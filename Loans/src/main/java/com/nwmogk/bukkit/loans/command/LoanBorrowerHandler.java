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
import com.nwmogk.bukkit.loans.api.FinancialEntity;
import com.nwmogk.bukkit.loans.object.FinancialInstitution;
import com.nwmogk.bukkit.loans.object.ImmutableOffer;
import com.nwmogk.bukkit.loans.object.Loan;

public class LoanBorrowerHandler {
	
	private SerenityLoans plugin;
	private String prfx;
	
	protected LoanBorrowerHandler(SerenityLoans plugin){
		this.plugin = plugin;
		prfx = Conf.getMessageString();
	}
	
	protected boolean acceptOffer(CommandSender sender, Command cmd,  String alias, String[] args){
		
		FinancialEntity borrower = plugin.playerManager.getFinancialEntityRetryOnce(((Player)sender).getUniqueId());
		
		if(borrower == null){
			sender.sendMessage(prfx + " You are not able to use this commmand.");
			return true;
		}
		
		String lenderName = args[1];
		
		FinancialEntity lender = plugin.playerManager.getFinancialEntityRetryOnce(lenderName);
		
		if(lender == null) {
			sender.sendMessage(prfx + " Lender entity not found.");
			return true;
		}
		
		
		ImmutableOffer theOffer = plugin.offerManager.getOffer(lender.getUserID(), borrower.getUserID());
		
		if(theOffer == null){
			sender.sendMessage(String.format(prfx + " You do not have any outstanding offers from %s.", lenderName));
			return true;
		}
		
		Timestamp expires = theOffer.getExpirationDate();
		
		if(expires.before(new Date())){
			sender.sendMessage(String.format(prfx + " You do not have any outstanding offers from %s.", lenderName));
			return true;
		}
		
		int termsID = theOffer.getPreparedTermsId();
		
		double value = plugin.offerManager.getTermsValue(termsID);
		
		if(!plugin.econ.has(lender, value).callSuccess){
			
			sender.sendMessage(String.format(prfx + " %s does not have enough money to loan!", lenderName));
			return true;
		}
		
		plugin.econ.withdraw(lender, value);
		plugin.econ.deposit(borrower, value);
		
		boolean returnSuccess = plugin.loanManager.createLoan(lender.getUserID(), borrower.getUserID(), termsID, value);
		
		if(returnSuccess){
			sender.sendMessage(prfx + " Successfully processed loan!");
		} else {
			sender.sendMessage(prfx + " Loan not processed!");
		}
		
		return true;
	}
	
	protected boolean ignoreOffers(CommandSender sender, Command cmd,  String alias, String[] args){
		
		FinancialEntity requester = plugin.playerManager.getFinancialEntityRetryOnce(((Player)sender).getUniqueId());
		FinancialEntity target = plugin.playerManager.getFinancialEntityRetryOnce(args[1]);
		
		if(requester == null){
			sender.sendMessage(prfx + " You are not able to use this command.");
			return true;
		}
		
		if(target == null){
			sender.sendMessage(prfx + " Cannot ignore this entity.");
			return true;
		}
		
		boolean setToIgnore = plugin.playerManager.toggleIgnore(requester.getUserID(), target.getUserID());
		
		sender.sendMessage(String.format(prfx + " %s ignoring %s.", setToIgnore? "Now" : "No longer", args[1]));
		
		return true;
	}

	protected boolean rejectOffer(CommandSender sender, Command cmd, String alias, String[] args){
		boolean success = plugin.offerManager.removeOffer(((Player)sender).getUniqueId(), plugin.playerManager.entityIdLookup(args[1]));
		
		if(success)
			sender.sendMessage(prfx + " Offer removed successfully.");
		else
			sender.sendMessage(prfx + " Unable to complete request.");
		
		return true;
	}

	protected boolean payLoan(CommandSender sender, Command cmd, String alias, String[] args, boolean payOff) {

		FinancialEntity borrower = plugin.playerManager.getFinancialEntityRetryOnce(((Player)sender).getUniqueId());
		FinancialEntity lender = plugin.playerManager.getFinancialEntity(args[1]);
		
		
		if(borrower == null){
			sender.sendMessage(prfx + " You are not able to use this command.");
			return true;
		}		
		
		LoanSpec loanSelection = LoanHandler.parseLoanArguments(borrower, sender.getName(), args, false);
		
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
		
		if(!plugin.econ.has(borrower, payAmount).callSuccess){
			sender.sendMessage(String.format("%s You do not have enough money!", prfx));
			return true;
		}
		
		plugin.econ.withdraw(borrower, payAmount);
		plugin.econ.deposit(lender, payAmount);
		
		plugin.loanManager.applyPayment(loanSelection.result, payAmount);
		
		sender.sendMessage(String.format("%s Payment of %s successfully applied to loan, %s.", prfx, plugin.econ.format(payAmount), loanSelection.result.getShortDescription(	plugin, true)));
		
		return false;
	}

	protected boolean viewStatement(CommandSender sender, Command cmd, String alias, String[] args) {
		
		FinancialEntity borrower = plugin.playerManager.getFinancialEntityRetryOnce(((Player)sender).getUniqueId());
		FinancialEntity lender = plugin.playerManager.getFinancialEntity(args[1]);
		
		
		if(borrower == null){
			sender.sendMessage(prfx + " You are not able to use this command.");
			return true;
		}		
		
		LoanSpec loanSelection = LoanHandler.parseLoanArguments(borrower, sender.getName(), args, false);
		
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
		
		boolean isPlayer = recipient.getUniqueId().equals(loanSelection.result.getBorrower().getUserID());
	
		recipient.sendMessage(String.format("%s %s an outstanding payment statement!", prfx, isPlayer? "You have" : ((FinancialInstitution)loanSelection.result.getBorrower()).getName() + " has"));
		recipient.sendMessage(String.format("%s Use %s to apply payment.", prfx, isPlayer? "/loan": "/crunion"));
		recipient.sendMessage(String.format("%s Details are given below:", prfx));
		recipient.sendMessage(plugin.loanManager.getPaymentStatement(loanSelection.result.getLoanID()).toString(plugin));
		recipient.sendMessage(String.format("%s Use %s statement %s to view this statement again.", prfx, isPlayer? "/loan": "/crunion", plugin.playerManager.entityNameLookup(loanSelection.result.getLender())));
	
		return false;
	}

	protected boolean setAutoPay(CommandSender sender, Command cmd, String alias,
			String[] args) {
		// TODO Auto-generated method stub
		return false;
	}
}
