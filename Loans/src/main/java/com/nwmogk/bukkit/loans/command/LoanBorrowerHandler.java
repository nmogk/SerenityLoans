/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * 
 * File: LoanBorrowerHandler.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This class performs sub-command handling for the /loan command of the
 * SerenityLoans plugin. This class handles the borrower related actions.
 * This class depends on static fields provided in the 
 * com.nwmogk.bukkit.loans.SerenityLoans class and configuration 
 * information from com.nwmogk.bukkit.loans.Conf class. It interacts 
 * directly with a mySQL database.
 * 
 * 
 * ========================================================================
 *                            LICENSE INFORMATION
 * ========================================================================
 * 
 * Copyright 2014 Nathan W Mogk
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * 
 * ========================================================================
 *                                CHANGE LOG
 * ========================================================================
 *    Date          Name                  Description              Defect #
 * ----------  --------------  ----------------------------------  --------
 * 2014-xx-xx  nmogk           Initial release for v0.1
 * 
 * 
 */

package com.nwmogk.bukkit.loans.command;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nwmogk.bukkit.loans.Conf;
import com.nwmogk.bukkit.loans.SerenityLoans;
import com.nwmogk.bukkit.loans.command.LoanHandler.LoanSpec;
import com.nwmogk.bukkit.loans.object.FinancialEntity;
import com.nwmogk.bukkit.loans.object.Loan;

public class LoanBorrowerHandler {
	
	private SerenityLoans plugin;
	private String prfx;
	
	protected LoanBorrowerHandler(SerenityLoans plugin){
		this.plugin = plugin;
		prfx = Conf.getMessageString();
	}
	
	protected boolean acceptOffer(CommandSender sender, Command cmd,  String alias, String[] args){
		
		FinancialEntity borrower = plugin.playerManager.getFinancialEntityRetryOnce(sender.getName());
		
		if(borrower == null){
			sender.sendMessage(prfx + " You are not able to use this commmand.");
			return true;
		}
		
		FinancialEntity lender = plugin.playerManager.getFinancialEntityRetryOnce(args[1]);
		
		if(lender == null) {
			sender.sendMessage(prfx + " Lender entity not found.");
			return true;
		}
		
		String offerQuery = String.format("SELECT * FROM Offers WHERE LenderID=%d AND BorrowerID=%d;", lender.getUserID(), borrower.getUserID());
		
		int loanID = 0;
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			ResultSet theOffer = stmt.executeQuery(offerQuery);
			
			if(!theOffer.next()){
				sender.sendMessage(String.format(prfx + " You do not have any outstanding offers from %s.", lender.getName()));
				stmt.close();
				return true;
			}
			
			Timestamp ts = theOffer.getTimestamp("ExpirationDate");
			
			if(ts.before(new Date())){
				sender.sendMessage(String.format(prfx + " You do not have any outstanding offers from %s.", lender.getName()));
				stmt.close();
				return true;
			}
			
			int termsID = theOffer.getInt("PreparedTerms");
			
			String loanBalanceQuery = String.format("SELECT Value FROM PreparedOffers WHERE OfferID=%d;", termsID);
			
			ResultSet balance = stmt.executeQuery(loanBalanceQuery);
			
			double value = balance.getDouble("Value");
			
			if(!SerenityLoans.econ.has(lender, value).callSuccess){
				
				sender.sendMessage(String.format(prfx + " %s does not have enough money to loan!", lender.getName()));
				return true;
			}
			
			SerenityLoans.econ.withdraw(lender, value);
			SerenityLoans.econ.deposit(borrower, value);
			
			
			String loanBuilder = String.format("INSERT INTO Loans(LenderID, BorrowerID, Terms, Balance, StartDate, LastUpdate) VALUES (%d, %d, %d, %f, ?, ?);", lender.getUserID(), borrower.getUserID(), termsID, value );
			
			PreparedStatement ps = plugin.conn.prepareStatement(loanBuilder);
			
			ps.setTimestamp(1, new Timestamp(new Date().getTime()));
			ps.setTimestamp(2, new Timestamp(new Date().getTime()));
			
			int returnCode = ps.executeUpdate();
			
			if(returnCode == 1){
				sender.sendMessage(prfx + " Successfully processed loan!");
			} else {
				sender.sendMessage(prfx + " Loan not processed!");
			}
			
			String offerDestruct = String.format("DELETE FROM Offers WHERE LenderID=%d AND BorrowerID=%d;", lender.getUserID(), borrower.getUserID());
			
			stmt.executeUpdate(offerDestruct);
			
			String whatsNew = String.format("SELECT LoanID FROM Loans WHERE Terms=%d;", termsID);
			
			ResultSet rs = stmt.executeQuery(whatsNew);
			rs.next();
			
			loanID = rs.getInt("LoanID");
			
			stmt.close();
			
			
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		plugin.loanManager.buildLoanEvents(loanID);
		
		return true;
	}
	
	protected boolean ignoreOffers(CommandSender sender, Command cmd,  String alias, String[] args){
		
		FinancialEntity requester = plugin.playerManager.getFinancialEntityRetryOnce(sender.getName());
		FinancialEntity target = plugin.playerManager.getFinancialEntityRetryOnce(args[1]);
		
		if(requester == null){
			sender.sendMessage(prfx + " You are not able to use this command.");
			return true;
		}
		
		
		
		if(target == null){
			sender.sendMessage(prfx + " Cannot ignore this entity.");
			return true;
		}
		
		boolean setToIgnore = true;
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			String querySQL = String.format("SELECT IgnoreOffers FROM Trust WHERE UserID=%d AND TargetID=%d;", requester.getUserID(), target.getUserID());
			
			ResultSet status = stmt.executeQuery(querySQL);
			
			if(status.next()){
				setToIgnore = !Boolean.parseBoolean(status.getString("IgnoreOffers"));
				
				String ignoreString = setToIgnore? "'true'" : "'false'";
				String updateSQL = String.format("UPDATE Trust SET IgnoreOffers=%s WHERE UserID=%d AND TargetID=%d;",  ignoreString, requester.getUserID(), target.getUserID());
			
				stmt.executeUpdate(updateSQL);
			} else {
				
				String insertSQL = String.format("INSERT INTO Trust (UserID, TargetID, IgnoreOffers) VALUES (%d, %d, 'true');", requester.getUserID(), target.getUserID());
				
				stmt.executeUpdate(insertSQL);
			}
			
			
		} catch (SQLException e) {
			sender.sendMessage(prfx + " Unable to complete request.");
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
			return true;
		}
		
		sender.sendMessage(String.format(prfx + " %s ignoring %s.", setToIgnore? "Now" : "No longer",target.getName()));
		
		return true;
		
	}

	protected boolean rejectOffer(CommandSender sender, Command cmd, String alias, String[] args){
		String updateSQL = "DELETE FROM Offers WHERE LenderID=? AND BorrowerID=?;";
		
		try {
			PreparedStatement stmt = plugin.conn.prepareStatement(updateSQL);
			
			stmt.setInt(2, plugin.playerManager.getFinancialEntityID(sender.getName()));
			stmt.setInt(1, plugin.playerManager.getFinancialEntityID(args[1]));
			
			stmt.executeUpdate();
		} catch (SQLException e) {
			sender.sendMessage(prfx + " Unable to complete request.");
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
			return true;
		}
		
		sender.sendMessage(prfx + " Offer removed successfully.");
		
		return true;
	}

	protected boolean payLoan(CommandSender sender, Command cmd, String alias, String[] args, boolean payOff) {

		FinancialEntity borrower = plugin.playerManager.getFinancialEntityRetryOnce(sender.getName());
		FinancialEntity lender = plugin.playerManager.getFinancialEntity(args[1]);
		
		
		if(borrower == null){
			sender.sendMessage(prfx + " You are not able to use this command.");
			return true;
		}		
		
		LoanSpec loanSelection = LoanHandler.parseLoanArguments(borrower, args, false);
		
		if(loanSelection.errMessage != null){
			sender.sendMessage(Conf.parseMacros(loanSelection.errMessage, new String[]{"$$c"}, new String[]{"/" + alias + " forgive"}));
			return true;
		}
		
		if(loanSelection.multipleValues){
			sender.sendMessage(prfx + " You have multiple loans with this entity. Select one of the following.");
			Loan[] allLoans = plugin.loanManager.getLoan(lender, borrower);
			for(int i = 0; i < allLoans.length ; i++){
				sender.sendMessage(String.format("    %d: %s", i, allLoans[i].getShortDescription(plugin, true) ));
			}
			
			return true;
		}
		
		double payAmount = 0;
		
		try{

			payAmount = loanSelection.remainingArgs.length != 0? Double.parseDouble(loanSelection.remainingArgs[0]) : (payOff? loanSelection.result.getCloseValue() : plugin.loanManager.getPaymentStatement(loanSelection.result.getLoanID()).getPaymentRemaining());
				
		} catch(NumberFormatException e){
			sender.sendMessage(String.format("%s Value specified incorrectly.", prfx));
			return true;
		}
		
		if(!SerenityLoans.econ.has(borrower, payAmount).callSuccess){
			sender.sendMessage(String.format("%s You do not have enough money!", prfx));
			return true;
		}
		
		SerenityLoans.econ.withdraw(borrower, payAmount);
		SerenityLoans.econ.deposit(lender, payAmount);
		
		plugin.loanManager.applyPayment(loanSelection.result, payAmount);
		
		sender.sendMessage(String.format("%s Payment of %s successfully applied to loan, %s.", prfx, SerenityLoans.econ.format(payAmount), loanSelection.result.getShortDescription(	plugin, true)));
		
		return false;
	}

	protected boolean viewStatement(CommandSender sender, Command cmd, String alias, String[] args) {
		
		FinancialEntity borrower = plugin.playerManager.getFinancialEntityRetryOnce(sender.getName());
		FinancialEntity lender = plugin.playerManager.getFinancialEntity(args[1]);
		
		
		if(borrower == null){
			sender.sendMessage(prfx + " You are not able to use this command.");
			return true;
		}		
		
		LoanSpec loanSelection = LoanHandler.parseLoanArguments(borrower, args, false);
		
		if(loanSelection.errMessage != null){
			sender.sendMessage(Conf.parseMacros(loanSelection.errMessage, new String[]{"$$c"}, new String[]{"/" + alias + " forgive"}));
			return true;
		}
		
		if(loanSelection.multipleValues){
			sender.sendMessage(prfx + " You have multiple loans with this entity. Select one of the following.");
			Loan[] allLoans = plugin.loanManager.getLoan(lender, borrower);
			for(int i = 0; i < allLoans.length ; i++){
				sender.sendMessage(String.format("    %d: %s", i, allLoans[i].getShortDescription(plugin, true) ));
			}
			
			return true;
		}
		
		Player recipient = plugin.playerManager.getPlayer(borrower.getUserID());
		
		if(recipient == null)
			return true;
		
		boolean isPlayer = recipient.getName().equalsIgnoreCase(loanSelection.result.getBorrower().getName());
	
		recipient.sendMessage(String.format("%s %s an outstanding payment statement!", prfx, isPlayer? "You have" : loanSelection.result.getBorrower().getName() + " has"));
		recipient.sendMessage(String.format("%s Use %s to apply payment.", prfx, isPlayer? "/loan": "/crunion"));
		recipient.sendMessage(String.format("%s Details are given below:", prfx));
		recipient.sendMessage(plugin.loanManager.getPaymentStatement(loanSelection.result.getLoanID()).toString(plugin));
		recipient.sendMessage(String.format("%s Use %s statement %s to view this statement again.", prfx, isPlayer? "/loan": "/crunion", loanSelection.result.getLender().getName()));
	
		return false;
	}

	protected boolean setAutoPay(CommandSender sender, Command cmd, String alias,
			String[] args) {
		// TODO Auto-generated method stub
		return false;
	}
}
