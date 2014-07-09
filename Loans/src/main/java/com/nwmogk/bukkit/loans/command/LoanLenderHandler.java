/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * 
 * File: LoanLenderHandler.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This class performs sub-command handling for the /loan command of the
 * SerenityLoans plugin. This class handles the lender related actions.
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
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.util.Date;
import java.util.HashMap;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.nwmogk.bukkit.loans.Conf;
import com.nwmogk.bukkit.loans.OfferManager.OfferExitStatus;
import com.nwmogk.bukkit.loans.SerenityLoans;
import com.nwmogk.bukkit.loans.command.LoanHandler.LoanSpec;
import com.nwmogk.bukkit.loans.exception.InvalidLoanTermsException;
import com.nwmogk.bukkit.loans.api.FinancialEntity;
import com.nwmogk.bukkit.loans.object.Loan;
import com.nwmogk.bukkit.loans.object.FinancialInstitution;

public class LoanLenderHandler {
	
	
	
	private class LoanSale{
		public Loan theLoan = null;
		public double amount = 0;
	}
	
	private SerenityLoans plugin;
	
	private HashMap<FinancialEntity, LoanSale> pendingSales;
	private String prfx;

	protected LoanLenderHandler(SerenityLoans plugin) {
		this.plugin = plugin;
		prfx = Conf.getMessageString();
	}
	
	/*
	 * Provides functionality for the /loan sendoffer command. The calling function ensures
	 * that the argument list is at least 2 long, and contains "sendoffer" in the first position.
	 * Permissions are also checked by the calling function. The behavior outline of this command 
	 * is as follows:
	 * 
	 * Check the sender's status in the loan system.
	 * Check the target's status in the loan system.
	 * Determine inputs for offer expiration time.
	 * Select terms from PreparedOffers table.
	 * Create unnamed entry in PreparedOffers table.
	 * Create Offer table entry with required info.
	 * If player is online, send them a message,
	 * otherwise terminate, message will be sent later.
	 */
	protected boolean sendOffer(CommandSender sender, Command cmd, String alias, String[] args, boolean isQuick){
		// Check ability of sender to use loan system
		// Sender already determined to be an online player
		
		FinancialEntity lender = plugin.playerManager.getFinancialEntityAdd(((Player)sender).getUniqueId());
		
		if(lender == null) {		
			sender.sendMessage(Conf.messageCenter("perm-generic-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias + args[0]}));
			return true;
		}
		
				
		//======================= Parse Inputs ========================

		// args was already determined to have at least length 2 before this command was called.
		if(args.length < 2)
			return false;
				
		// Collect financialEntity target info
		// This is potentially not safe input!!!
		String entityTarget = args[1];
				
		if(entityTarget.equalsIgnoreCase(sender.getName())){
			sender.sendMessage(Conf.messageCenter("meta-offer-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias + args[0]}));
			return true;
		}
				
		if(entityTarget.equalsIgnoreCase("CentralBank")){
			sender.sendMessage(Conf.messageCenter("offer-government", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias + args[0], entityTarget}));
			return true;
		}
			
		FinancialEntity borrower = plugin.playerManager.getFinancialEntityAdd(entityTarget);
		
		// Check if other entity is in FinancialEntities table
		if(borrower == null) {			
			sender.sendMessage(Conf.messageCenter("offer-send-fail", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias + args[0], entityTarget}));
			return true;
		}
		
		// Parse expiration time
		String timeString = "1w";
				
		// The expiration string is assumed to be the remainder of the line
		// Spaces are removed, and the string is parsed into a length
		if (args.length > 2) {
			for(int i = 2; i < args.length; i++)
				timeString += args[i];
		} else if(plugin.getConfig().contains("loan.default-offer-time"))
			timeString = plugin.getConfig().getString("loan.default-offer-time");
				
		// Parsing string
		long expirationTime = Conf.parseTime(timeString);
				
		if(expirationTime == 0){
			sender.sendMessage(Conf.messageCenter("bad-expiration", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias + args[0], entityTarget}));
			return true;
		}

		// Create expiration time
		Timestamp expDate = new Timestamp(new Date().getTime() + expirationTime);
				
		// Check if offer is in PreparedOffers table
		String offerName = isQuick? "default" : "prepared";
		
		OfferExitStatus exit = plugin.offerManager.createOffer(lender.getUserID(), borrower.getUserID(), offerName, expDate);
				
		switch(exit){
		case IGNORED:
			sender.sendMessage(Conf.messageCenter("talk-to-the-hand", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias + args[0], entityTarget}));
			return true;
		case OVERWRITE_FAIL:
			sender.sendMessage(Conf.messageCenter("overwrite-fail", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias + args[0], entityTarget}));
			return true;
		case SUCCESS:
			break;
		case UNKNOWN:
			sender.sendMessage(prfx + " No offer has been prepared. This is a bug. Please report.");
			return true;
		}
		
		Player recipient = plugin.playerManager.getPlayer(borrower.getUserID());
		
		if(recipient == null){
			sender.sendMessage(Conf.messageCenter("offline-send", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias + args[0], entityTarget}));
			return true;
		}
		
		if(!recipient.hasPermission("serenityloans.loan.borrow") && !recipient.hasPermission("serenityloans.crunion.borrow")){
			sender.sendMessage(Conf.messageCenter("no-can-borrow", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias + args[0], entityTarget}));
			return true;
		}
		
		// Send message
		
		String recipientName = recipient.getName().equals(entityTarget)? "You" : entityTarget;
		String commandName = recipient.getName().equals(entityTarget)? "/loan" : "/crunion";
					
		sender.sendMessage(Conf.messageCenter("offer-receipt", new String[]{"$$p", "$$c", "$$r", "$$m"}, new String[]{recipient.getName(), "/" + commandName + args[0], sender.getName(), recipientName}));
		sender.sendMessage(Conf.messageCenter("view-offers", new String[]{"$$p", "$$c", "$$r", "$$m"}, new String[]{recipient.getName(), "/" + commandName + args[0], sender.getName(), recipientName}));
					
		if(plugin.offerManager.registerOfferSend(lender.getUserID(), borrower.getUserID()))	
			sender.sendMessage(Conf.messageCenter("offer-send-success", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + commandName + args[0], entityTarget}));
				
		return true;
	}

	protected boolean retractOffer(CommandSender sender, Command cmd, String alias, String[] args){
		
		FinancialEntity borrower = plugin.playerManager.getFinancialEntity(args[1]);
		
		if(borrower != null && plugin.offerManager.removeOffer(((Player)sender).getUniqueId(), borrower.getUserID()))
			sender.sendMessage(prfx + " Operation successful.");
		else
			sender.sendMessage(Conf.messageCenter("generic-refuse", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias + " retractoffer", args[1]}));
		
		return true;
		
	}
	
	protected boolean loanOfferingCommand(CommandSender sender, Command cmd,
			String alias, String[] args, boolean isDefault) {
		
		FinancialEntity player = plugin.playerManager.getFinancialEntityAdd(((Player)sender).getUniqueId());
		
		if(player == null){
			sender.sendMessage(Conf.messageCenter("perm-generic-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias + " " + (isDefault? "default ": "") +  "offering"}));
			return true;
		}
		
		if(args.length == 1)
			return viewPreparedOffer(sender, player, isDefault);
		
		boolean success = true;
		for(int i = 1; i < args.length; i++){
			try {
				success &= plugin.offerManager.setTerms(player.getUserID(), isDefault, args[i]);
			} catch (InvalidLoanTermsException e) {
				sender.sendMessage(Conf.messageCenter("terms-constraint-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias + " " + (isDefault? "default ": "") +  "offering"}));
				sender.sendMessage(e.getMessage());
			}
		}
		
		if(success){
			sender.sendMessage(Conf.messageCenter("change-success", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias + " " + (isDefault? "default ": "") +  "offering"}));
			
		} else {
			sender.sendMessage(Conf.messageCenter("change-fail1", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias + " " + (isDefault? "default ": "") +  "offering"}));
			sender.sendMessage(Conf.messageCenter("change-fail2", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias + " " + (isDefault? "default ": "") +  "offering"}));
			
		}
		
		sender.sendMessage(Conf.messageCenter("new-values", new String[]{"$$s", "$$p", "$$c"}, new String[]{isDefault? "default ":"" ,sender.getName(), "/" + alias + " " + (isDefault? "default ": "") +  "offering"}));
		
		viewPreparedOffer(sender, player, isDefault);
	
		return true;
	}

	protected boolean forgiveLoan(CommandSender sender, Command cmd, String alias, String[] args){
		String borrowerName = args[1];
		
		FinancialEntity lender = plugin.playerManager.getFinancialEntityAdd(((Player)sender).getUniqueId());
		FinancialEntity borrower = plugin.playerManager.getFinancialEntity(borrowerName);
		
		if(lender == null){
			sender.sendMessage(Conf.messageCenter("generic-problem", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias + " forgive"}));
			
			return true;
		}
		
		LoanSpec loanSelection = LoanHandler.parseLoanArguments(lender, sender.getName(), args, true);
		
		
		if(loanSelection.errMessage != null){
			sender.sendMessage(Conf.parseMacros(loanSelection.errMessage, new String[]{"$$c"}, new String[]{"/" + alias + " forgive"}));
			return true;
		}
		
		if(loanSelection.multipleValues){
			sender.sendMessage(Conf.messageCenter("multiple-loans", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias + " forgive", borrowerName}));
			
			Loan[] allLoans = plugin.loanManager.getLoan(lender, borrower);
			for(int i = 0; i < allLoans.length ; i++){
				sender.sendMessage(String.format("    %d: %s", i, allLoans[i].getShortDescription(plugin, false) ));
			}
			
			return true;
		}
		
		double amount = 0;
		Loan theLoan = loanSelection.result;
		String[] toParse = loanSelection.remainingArgs;
		
		if(toParse.length >= 1){
			try{
				amount = Double.parseDouble(toParse[0]);
			} catch (NumberFormatException e){
				sender.sendMessage(Conf.messageCenter("number-parse-fail", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias + " forgive", borrowerName}));
				return true;
			}
		} else 
			amount = theLoan.getCloseValue();
		
		
		
		plugin.loanManager.applyPayment(theLoan, amount);
		
		sender.sendMessage(Conf.messageCenter("loan-forgive", new String[]{"$$p", "$$c", "$$r", "$$b"}, new String[]{sender.getName(), "/" + alias + " forgive", borrowerName, plugin.econ.format(amount)}));

		return true;
	}
	
	protected boolean sellLoan (CommandSender sender, Command cmd, String alias, String[] args){
		String borrowerName = args[1];
		FinancialEntity lender = plugin.playerManager.getFinancialEntityAdd(((Player)sender).getUniqueId());
		FinancialEntity borrower = plugin.playerManager.getFinancialEntity(borrowerName);
					
		if(lender == null){
			sender.sendMessage(Conf.messageCenter("generic-problem", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias + " sell"}));
			return true;
		}		
		
		LoanSpec loanSelection = LoanHandler.parseLoanArguments(lender, sender.getName(), args, true);
		
		
		if(loanSelection.errMessage != null){
			sender.sendMessage(Conf.parseMacros(loanSelection.errMessage, new String[]{"$$c"}, new String[]{"/" + alias + " forgive"}));
			return true;
		}
		
		if(loanSelection.multipleValues){
			sender.sendMessage(Conf.messageCenter("multiple-loans", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias + " sell", borrowerName}));
			Loan[] allLoans = plugin.loanManager.getLoan(lender, borrower);
			for(int i = 0; i < allLoans.length ; i++){
				sender.sendMessage(String.format("    %d: %s", i, allLoans[i].getShortDescription(plugin, false) ));
			}
			
			return true;
		}
		
		LoanSale sale = new LoanSale();
		
		double amount = 0;
		sale.theLoan = loanSelection.result;
		String[] toParse = loanSelection.remainingArgs;
		
		if(toParse.length < 2){
			sender.sendMessage(prfx + " Syntax incorrect. Must specify new lender and amount of sale.");
			return true;
		}
		
		FinancialEntity recipient = plugin.playerManager.getFinancialEntityAdd(toParse[0]);
		
		
		if(recipient == null){
			sender.sendMessage(String.format(prfx + " You cannot sell a loan to %s.", toParse[0]));
			return true;
		}
		
		Player newLender = plugin.playerManager.getPlayer(recipient.getUserID());
		
		
		if(newLender == null){
			sender.sendMessage(prfx + " New lender must be online to complete transaction.");
			return true;
		}
		
		if(!newLender.hasPermission("serenityloans.loan.lend") && !newLender.hasPermission("serenityloans.crunion.lend")){
			sender.sendMessage(String.format(prfx + " %s does not have permission to buy loan.", toParse[0]));
			return true;
		}
		
		try{
			amount = Double.parseDouble(toParse[1]);
		} catch (NumberFormatException e){
			sender.sendMessage(prfx + " Amount specified incorrectly.");
			return true;
		}
		
		
		sale.amount = amount;
		
		// Send message
		
		String recipientName = recipient.getUserID().equals(newLender.getUniqueId())? "You" : ((FinancialInstitution)recipient).getName();
		String commandName = recipient.getUserID().equals(newLender.getUniqueId())? "/loan" : "/crunion";
		
		newLender.sendMessage(String.format(prfx + " %s received a loan sale offer from %s for %s.", recipientName, sender.getName(), plugin.econ.format(amount)));
		newLender.sendMessage(String.format(prfx + "Type '%s viewsaleoffer' to view details.", commandName));
		newLender.sendMessage(String.format(prfx + "Type '%s buy' to purchase loan.", commandName));
	
		pendingSales.put(borrower, sale);
		
		sender.sendMessage(prfx + " Sale offer sent successfully.");
		
		return true;
	}
	
	protected boolean buyLoan(CommandSender sender, Command cmd, String alias, String[] args){
		FinancialEntity buyer = plugin.playerManager.getFinancialEntityAdd(((Player)sender).getUniqueId());			
		
		
		// Pending Sales are not persistent across restarts.
		if(!pendingSales.containsKey(buyer)){
			sender.sendMessage(prfx + " You do not have any outstanding offers to buy a loan.");
			return true;
		}
		
		LoanSale ls = pendingSales.get(buyer);
		
		if(!plugin.econ.has(buyer, ls.amount).callSuccess){
			sender.sendMessage(String.format(prfx + " You do not have enough money to buy this loan. Cost is %s.", plugin.econ.format(ls.amount)));
			return true;
		}
		
		plugin.econ.withdraw(buyer, ls.amount);
		plugin.econ.deposit(ls.theLoan.getLender(), ls.amount);
		
		if(plugin.loanManager.setLender(ls.theLoan.getLoanID(), buyer.getUserID()))
			sender.sendMessage(prfx + " Purchase processed successfully!");
		else
			sender.sendMessage(prfx + " Error processing purchase.");
		
		return true;
	}
	
	protected boolean viewSaleOffer(CommandSender sender, Command cmd, String alias,
			String[] args) {
	
		FinancialEntity buyer = plugin.playerManager.getFinancialEntityAdd(((Player)sender).getUniqueId());
		
		if(!pendingSales.containsKey(buyer)){
			sender.sendMessage(prfx + " You do not have any outstanding offers to buy a loan");
			return true;
		}
		
		LoanSale ls = pendingSales.get(buyer);
		
		sender.sendMessage(String.format(prfx + " You have an offer to buy a loan for %s.", plugin.econ.format(ls.amount)));
		sender.sendMessage(ls.theLoan.toString(plugin));
		
		
		return false;
	}


private boolean viewPreparedOffer(CommandSender sender, FinancialEntity player, boolean isDefault) {

	String querySQL = String.format("SELECT * FROM PreparedOffers WHERE LenderID=%d AND OfferName='%s'", player.getUserID(), isDefault? "default" : "prepared" );
	
	try {
		Statement stmt = plugin.conn.createStatement();
		
		ResultSet results = stmt.executeQuery(querySQL);
		
		if(!results.next()){
			sender.sendMessage(prfx + " No applicable offers to view.");
			stmt.close();
			return true;
		}
		
		double value = results.getDouble("Value");
		double interestRate = results.getDouble("InterestRate");
		double lateFee = results.getDouble("LateFee");
		double minPayment = results.getDouble("MinPayment");
		double serviceFee = results.getDouble("ServiceFee");
		long term = results.getLong("Term");
		long compoundingPeriod = results.getLong("CompoundingPeriod");
		long gracePeriod = results.getLong("GracePeriod");
		long paymentTime = results.getLong("PaymentTime");
		long paymentFrequency = results.getLong("PaymentFrequency");
		long serviceFeeFrequency = results.getLong("ServiceFeeFrequency");
		String loanType = results.getString("LoanType");
		
		String[] result =  
			{String.format("    Loan value: %s", plugin.econ.format(value)),
			 String.format("    Interest rate: %s (%s)",  plugin.econ.formatPercent(interestRate), Conf.getIntReportingString()),
			 String.format("    Minimum payment: %s", plugin.econ.format(minPayment)),
			 String.format("    Term: %s", Conf.buildTimeString(term)),
			 String.format("    Compounding period: %s", Conf.buildTimeString(compoundingPeriod)),
			 String.format("    Payment time: %s", Conf.buildTimeString(paymentTime)),
			 String.format("    Payment frequency: %s", Conf.buildTimeString(paymentFrequency)),
			 String.format("    Loan type: %s", loanType)};

		String[] lateFeeRelated = 
			{String.format("    Late fee: %s", plugin.econ.format(lateFee)),
			 String.format("    Grace period: %s", Conf.buildTimeString(gracePeriod))};
		
		String[] serviceFeeRelated = 
			{String.format("    Service fee: %s", plugin.econ.format(serviceFee)),
			 String.format("    Service fee frequency: %s", Conf.buildTimeString(serviceFeeFrequency))};
		
		sender.sendMessage(String.format(prfx + " Details for %soffering.", isDefault? "default ":""));
		sender.sendMessage(result);
		
		if(lateFee != 0)
			sender.sendMessage(lateFeeRelated);
		if(serviceFee != 0)
			sender.sendMessage(serviceFeeRelated);
		
		stmt.close();
		return true;
	} catch (SQLException e) {
		SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
		e.printStackTrace();
	}
	
	return false;
}




}
