/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * 
 * File: LoanHandler.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This class performs command handling for the /loan command of the
 * SerenityLoans plugin. For length purposes, this class splits lender and
 * borrower sub-commands into two other classes. This class handles the
 * help, viewoffers, and summary sub-commands directly. This class depends
 * on static fields provided in the com.nwmogk.bukkit.loans.SerenityLoans
 * class and configuration information from com.nwmogk.bukkit.loans.Conf
 * class. It interacts directly with a mySQL database.
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

//import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nwmogk.bukkit.loans.Conf;
import com.nwmogk.bukkit.loans.SerenityLoans;
import com.nwmogk.bukkit.loans.OfferManager.OfferExitStatus;
import com.nwmogk.bukkit.loans.api.FinancialEntity;
import com.nwmogk.bukkit.loans.exception.InvalidLoanTermsException;
import com.nwmogk.bukkit.loans.object.FinancialInstitution;
import com.nwmogk.bukkit.loans.object.ImmutableOffer;
import com.nwmogk.bukkit.loans.object.Loan;

public class LoanHandler implements CommandExecutor{

	public static class LoanSpec{
		public Loan result = null;
		public String[] remainingArgs = null;
		public boolean multipleValues = false;
		public String errMessage = null;
	}
	
	private class LoanSale{
		public Loan theLoan = null;
		public double amount = 0;
	}
	
	private SerenityLoans plugin;
	private HashMap<FinancialEntity, LoanSale> pendingSales;
	private LoanLenderHandler sub1;
	private LoanBorrowerHandler sub2;
	private static String prfx;
	
	public LoanHandler(SerenityLoans plugin){
		this.plugin = plugin;
		sub1 = new LoanLenderHandler(plugin);
		sub2 = new LoanBorrowerHandler(plugin);
		prfx = Conf.getMessageString();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {

		// The /loan command is specific to individual players. Console senders may
		// issue loans through the CentralBank.
		if(!(sender instanceof Player)) {
			sender.sendMessage(prfx + " Only players may deal in personal loans.");
			return true;
		}
		
		// All sub-commands require the sender to be in the system.
		FinancialEntity entity = plugin.playerManager.getFinancialEntityAdd(((Player)sender).getUniqueId());
		
		if(entity == null){
			sender.sendMessage(prfx + " You are not able to use this command.");
			return true;
		}
		
		// The specific sub-command must appear after the main command. If not, display help.
		if(args.length == 0)
			return helpCommand(sender, alias, args);
		
		String subCommand = args[0];
		
		if (subCommand.equalsIgnoreCase("help") || subCommand.equalsIgnoreCase("?"))
			return helpCommand(sender, alias, args);
		
		else if (subCommand.equalsIgnoreCase("offering") || subCommand.equalsIgnoreCase("defaultoffering")) 
			return loanOfferingCommand(sender, entity, alias + " " + subCommand, args, subCommand.equalsIgnoreCase("defaultoffering"));
			
		else if (subCommand.equalsIgnoreCase("sendoffer") || subCommand.equalsIgnoreCase("quickoffer")) 
			return sendOffer(sender, entity, alias + " " + subCommand, args, subCommand.equalsIgnoreCase("quickoffer"));
			
		else if (subCommand.equalsIgnoreCase("retractoffer")) 
			retractOffer(sender, entity, alias + " " + subCommand, args);

		else if (subCommand.equalsIgnoreCase("forgive")) {			
			// Check basic command syntax
			if(args.length == 1){
				sender.sendMessage(Conf.messageCenter("missing-borrower-argument", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				sender.sendMessage(Conf.messageCenter("command-help", new String[]{"$$p", "$$c", "$$h"}, new String[]{sender.getName(), "/" + alias +" " + subCommand, "/" + alias +" help " + subCommand}));
				return true;
			}
									
			// Check perms			
			if(!sender.hasPermission("serenityloans.loan.lend")) {
				sender.sendMessage(Conf.messageCenter("perm-lend-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				return true;
			}
			
			return sub1.forgiveLoan(sender, cmd, alias, args);
						
		} else if (subCommand.equalsIgnoreCase("sell")) {
			// Check basic command syntax
			if(args.length == 1){
				sender.sendMessage(Conf.messageCenter("missing-borrower-argument", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				sender.sendMessage(Conf.messageCenter("command-help", new String[]{"$$p", "$$c", "$$h"}, new String[]{sender.getName(), "/" + alias +" " + subCommand, "/" + alias +" help " + subCommand}));
				return true;
			}
									
			// Check perms			
			if(!sender.hasPermission("serenityloans.loan.lend")) {
				sender.sendMessage(Conf.messageCenter("perm-lend-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				return true;
			}
			
			return sub1.sellLoan(sender, cmd, alias, args);
			
		} else if (subCommand.equalsIgnoreCase("buy")) {
			
			if(!sender.hasPermission("serenityloans.loan.lend")){
				sender.sendMessage(Conf.messageCenter("perm-buy-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				return true;
			}
			
			return sub1.buyLoan(sender, cmd, alias, args);
		} else if (subCommand.equalsIgnoreCase("viewsaleoffer")) {
			
			if(!sender.hasPermission("serenityloans.loan.lend")){
				sender.sendMessage(Conf.messageCenter("perm-lend-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				return true;
			}
			
			return sub1.viewSaleOffer(sender, cmd, alias, args);			
			
		} else if (subCommand.equalsIgnoreCase("viewoffers") || subCommand.equalsIgnoreCase("viewoffer") || subCommand.equalsIgnoreCase("viewsentoffer") || subCommand.equalsIgnoreCase("viewsentoffers")) { 
			/*
			 * Behavior: 
			 * 	/loan viewoffers
			 * 		Shows all offers given to the calling player
			 * 	/loan viewoffers <otherentity>
			 * 		Shows offer detail from given entity
			 * 	/loan viewoffers <self>
			 * 		Shows all offers outstanding
			 */
			
			// Check basic command syntax
			if(args.length > 2){
				sender.sendMessage(Conf.messageCenter("too-many-arguments", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				sender.sendMessage(Conf.messageCenter("command-help", new String[]{"$$p", "$$c", "$$h"}, new String[]{sender.getName(), "/" + alias +" " + subCommand, "/" + alias +" help " + subCommand}));
				return true;
			}
			
			// Check perms			
			if(!sender.hasPermission("serenityloans.loan.borrow")) {
				sender.sendMessage(Conf.messageCenter("perm-borrow-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				return true;
			}
			
			// Send to specific handler method
			return loanViewoffersCommand(sender, args, subCommand.equalsIgnoreCase("viewsentoffer") || subCommand.equalsIgnoreCase("viewsentoffers"));
						
			
		} else if (subCommand.equalsIgnoreCase("accept")) {
			
			if(args.length == 1){
				sender.sendMessage(Conf.messageCenter("missing-entity-argument", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				sender.sendMessage(Conf.messageCenter("command-help", new String[]{"$$p", "$$c", "$$h"}, new String[]{sender.getName(), "/" + alias +" " + subCommand, "/" + alias +" help " + subCommand}));
				return true;
			}
						
			// Check perms			
			if(!sender.hasPermission("serenityloans.loan.borrow")) {
				sender.sendMessage(Conf.messageCenter("perm-borrow-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				return true;
			}
			
			return sub2.acceptOffer(sender, cmd, alias, args);
			
		} else if (subCommand.equalsIgnoreCase("reject")) {
			
			if(args.length == 1){
				sender.sendMessage(Conf.messageCenter("missing-entity-argument", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				sender.sendMessage(Conf.messageCenter("command-help", new String[]{"$$p", "$$c", "$$h"}, new String[]{sender.getName(), "/" + alias +" " + subCommand, "/" + alias +" help " + subCommand}));
				return true;
			}
						
			// Check perms			
			if(!sender.hasPermission("serenityloans.loan.borrow")) {
				sender.sendMessage(Conf.messageCenter("perm-borrow-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				return true;
			}
			
			// Send to specific handler method
			sub2.rejectOffer(sender, cmd, alias, args);
			
		} else if (subCommand.equalsIgnoreCase("ignore")) {
			// Behavior: toggle ignore status
			// Check current status
			// Change
			
			if(args.length == 1){
				sender.sendMessage(Conf.messageCenter("missing-entity-argument", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				sender.sendMessage(Conf.messageCenter("command-help", new String[]{"$$p", "$$c", "$$h"}, new String[]{sender.getName(), "/" + alias +" " + subCommand, "/" + alias +" help " + subCommand}));
				return true;
			}
						
			// Check perms			
			if(!sender.hasPermission("serenityloans.loan.borrow")) {
				sender.sendMessage(Conf.messageCenter("perm-borrow-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				return true;
			}
			
			// Send to specific handler method

			return sub2.ignoreOffers(sender, cmd, alias, args);			
			
		} else if (subCommand.equalsIgnoreCase("pay")) {
			
			if(args.length == 1){
				sender.sendMessage(Conf.messageCenter("missing-lender-argument", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				sender.sendMessage(Conf.messageCenter("command-help", new String[]{"$$p", "$$c", "$$h"}, new String[]{sender.getName(), "/" + alias +" " + subCommand, "/" + alias +" help " + subCommand}));
				return true;
			}
						
			// Check perms			
			if(!sender.hasPermission("serenityloans.loan.borrow")) {
				sender.sendMessage(Conf.messageCenter("perm-borrow-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				return true;
			}
			
			return sub2.payLoan(sender, cmd, alias, args, false);
			
		} else if (subCommand.equalsIgnoreCase("payoff")) {

			if(args.length == 1){
				sender.sendMessage(Conf.messageCenter("missing-lender-argument", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				sender.sendMessage(Conf.messageCenter("command-help", new String[]{"$$p", "$$c", "$$h"}, new String[]{sender.getName(), "/" + alias +" " + subCommand, "/" + alias +" help " + subCommand}));
				return true;
			}
						
			// Check perms			
			if(!sender.hasPermission("serenityloans.loan.borrow")) {
				sender.sendMessage(Conf.messageCenter("perm-borrow-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				return true;
			}
			
			return sub2.payLoan(sender, cmd, alias, args, true);
			
		} else if (subCommand.equalsIgnoreCase("summary")) {
			//TODO
		} else if (subCommand.equalsIgnoreCase("setautopay")) {
			
			if(args.length < 3){
				sender.sendMessage(Conf.messageCenter("too-few-arguments", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				sender.sendMessage(Conf.messageCenter("command-help", new String[]{"$$p", "$$c", "$$h"}, new String[]{sender.getName(), "/" + alias +" " + subCommand, "/" + alias +" help " + subCommand}));
				return true;
			}
						
			// Check perms			
			if(!sender.hasPermission("serenityloans.loan.borrow")) {
				sender.sendMessage(Conf.messageCenter("perm-borrow-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				return true;
			}
			
			return sub2.setAutoPay(sender, cmd, alias, args);
			
		} else if (subCommand.equalsIgnoreCase("statement")) {
			
			if(args.length == 1){
				sender.sendMessage(Conf.messageCenter("missing-lender-argument", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				sender.sendMessage(Conf.messageCenter("command-help", new String[]{"$$p", "$$c", "$$h"}, new String[]{sender.getName(), "/" + alias +" " + subCommand, "/" + alias +" help " + subCommand}));
				return true;
			}
						
			// Check perms			
			if(!sender.hasPermission("serenityloans.loan.borrow")) {
				sender.sendMessage(Conf.messageCenter("perm-borrow-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				return true;
			}
			
			return sub2.viewStatement(sender, cmd, alias, args);
			
		} else {
			return false;
		}
		
		return false;
	}
	
	

	private boolean helpCommand(CommandSender sender, String alias, String[] args) {

		String[] basicHelp = {prfx + " The '/" + alias + "' command allows players to offer and accept loans.",
								prfx + " The specific commands are listed below.",
								prfx + " Use '/" + alias + " help <commandname>' for more info on a command."};
		
		String[] lendingCommands = {"    /" + alias + " offering",
									"    /" + alias + " sendoffer",
									"    /" + alias + " defaultoffering",
									"    /" + alias + " quickoffer",
									"    /" + alias + " retractoffer",
									"    /" + alias + " viewsentoffer",
									"    /" + alias + " viewsentoffers",
									"    /" + alias + " forgive",
									"    /" + alias + " sell",
									"    /" + alias + " buy",};
		
		String[] borrowCommands =  {"    /" + alias + " viewoffer",
									"    /" + alias + " viewoffers",
									"    /" + alias + " accept",
									"    /" + alias + " reject",
									"    /" + alias + " ignore",
									"    /" + alias + " pay",
									"    /" + alias + " payoff",
									"    /" + alias + " summary",
									"    /" + alias + " setautopay",};
		
		if(args.length <= 1){
			sender.sendMessage(basicHelp);
			
			if(sender.hasPermission("serenityloans.loan.lend"))
				sender.sendMessage(lendingCommands);
			if(sender.hasPermission("serenityloans.loan.borrow"))
				sender.sendMessage(borrowCommands);
			
			return true;
		}
		
		//TODO implement command documentation

		return false;
	}
	
	


	private boolean loanViewoffersCommand(CommandSender sender, String[] args, boolean sentOffers) {
		
		// Attempt to add sender to FinancialEntities table
		
		FinancialEntity player = plugin.playerManager.getFinancialEntityAdd(((Player)sender).getUniqueId());
		
		if(player == null){
			sender.sendMessage(Conf.messageCenter("perm-generic-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/loan viewoffers"}));
			return true;
		}
		
		List<FinancialEntity> othersList = null;
		
		// Three cases
		if((sentOffers && args.length == 1) || (args.length == 2 && args[1].equalsIgnoreCase(sender.getName()))){
			othersList = plugin.offerManager.getOfferRecipientsFrom(player.getUserID());
		} else if(args.length == 2){
			FinancialEntity other = plugin.playerManager.getFinancialEntityAdd(args[1]);
			ImmutableOffer offer = sentOffers? plugin.offerManager.getOffer(player.getUserID(), other.getUserID()) : plugin.offerManager.getOffer(other.getUserID(), player.getUserID());
		
			sender.sendMessage(String.format(prfx + " Details for offer %s %s.", sentOffers? "to":"from", args[1]));
			sender.sendMessage(offer.toString(plugin));
			
			return true;
		} else {
			othersList = plugin.offerManager.getOfferSendersTo(player.getUserID());
		}
		
		if(othersList.size() == 0){
			sender.sendMessage(Conf.messageCenter("no-offers", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/loan viewoffers", args.length == 2 && args[1].equalsIgnoreCase(sender.getName()) ? args[1] : ""}));
			return true;
		}
		
		String lenderLabel = args.length == 1? "Lender" : "Recipient";
				
		String output = Conf.messageCenter(args.length == 1 && !sentOffers ? "received-offers" : "sent-offers", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/loan viewoffers"});
		output += "\n    " + lenderLabel + " --- Expires";
		
		for(FinancialEntity fe : othersList){
			String name = plugin.playerManager.entityNameLookup(fe);
			
			ImmutableOffer offer = sentOffers? plugin.offerManager.getOffer(player.getUserID(), fe.getUserID()) : plugin.offerManager.getOffer(fe.getUserID(), player.getUserID());
		
			Timestamp exp = offer.getExpirationDate();
			
			output += String.format("\n    %s - %s", name, DateFormat.getDateInstance().format(exp));
			
		}
				
		sender.sendMessage(output.split("\n"));
				
		return true;
				
	}
	
	
	/*
	 * Parses a loan specification from arguments given a FinancialEntity.
	 * errMessage should be null unless there is an error. As long as errMessage is 
	 * null, the loan should be not null. If the flag multipleValues is true, then
	 * the query returned multiple matches, and the loan will be the first one found.
	 * As long as there is no error and multipleValues is false, then the remaining
	 * args will be not null, and contain only the arguments following the last loan
	 * specification argument.
	 */
	protected static LoanSpec parseLoanArguments(FinancialEntity sender, String senderName, String[] args, boolean isSenderLender){
		
		LoanSpec result = new LoanSpec();
		result.result = null;
		result.remainingArgs = null;
		result.multipleValues = false;
		result.errMessage = null;
		
		int remainingIndex = 2;
		
		if(args.length < 2){
			result.errMessage = Conf.messageCenter("missing-entity-argument", new String[]{"$$p", "$$c"}, new String[]{senderName, "$$c"});
			return result;
		}
			
		FinancialEntity target = SerenityLoans.getPlugin().playerManager.getFinancialEntity(args[1]);
		
		if(target == null){
			
			result.errMessage = Conf.messageCenter("no-loan", new String[]{"$$p", "$$r", "$$c"}, new String[]{senderName, args[1], "$$c"});
			
			return result;
			
		}
		
		FinancialEntity lender = isSenderLender? sender : target;
		FinancialEntity borrower = isSenderLender? target : sender;
		
		Loan[] potentials = SerenityLoans.getPlugin().loanManager.getLoan(lender, borrower);
		
		if(potentials == null){
			result.errMessage = Conf.messageCenter("no-loan", new String[]{"$$p", "$$r", "$$c"}, new String[]{senderName, args[1], "$$c"});
			
			return result;
			
		}
		
		if(potentials.length == 1){
			
			result.result = potentials[0];
			
		} else {
			
			int loanIndex;
			
			if(args.length < 3){
				result.result = potentials[0];
				result.multipleValues = true;
				return result;
			}
			
			try {
				loanIndex = Integer.parseInt(args[2]);
			}
			catch (NumberFormatException e){
				result.errMessage = Conf.messageCenter("unknown-loan-selection", new String[]{"$$p", "$$r", "$$c"}, new String[]{senderName, args[1], "$$c"});
				
				return result;
			}
			
			if (loanIndex >= potentials.length){
				result.errMessage = Conf.messageCenter("unknown-loan-selection", new String[]{"$$p", "$$r", "$$c"}, new String[]{senderName, args[1], "$$c"});
				return result;
			}
			
			result.result = potentials[loanIndex - 1];
			remainingIndex++;
			
			
		}
		
		String[] remainingArgs = new String[args.length - remainingIndex];
		
		for(int i = remainingIndex; i < args.length; i++){
			remainingArgs[i - remainingIndex] = args[i];
		}
		
		result.remainingArgs = remainingArgs;
		
		return result;
		
	}
	
	/*
	 * Command: /sendoffer <borrower> [expiration time]
	 * 			/quickoffer <borrower> [expiration time]
	 * 
	 * Permissions: serenityloans.loan.lend
	 * 
	 * borrower = name of a player or financial institution (bank)
	 * 
	 * expiration time = length of time for loan to be active formatted
	 * as a list of integers followed by units. Available units are
	 * y (year), d (day), h (hour), m (minutes), s (seconds). Spaces
	 * are optional. ex - "3d 4h 1 s" represents 3 days 4 hours and 1 second.
	 * 
	 * This command sends a loan offer to the given potential borrower.
	 * The borrower may be either a player or a financial institution and
	 * can be offline. However, the borrower must have the proper
	 * permissions to borrow for the command to work. 
	 * 
	 * /sendoffer sends the current prepared offer
	 * /quickoffer sends the default prepared offer
	 * 
	 * ==========================================================
	 * 
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
	private boolean sendOffer(final CommandSender sender, final FinancialEntity entity, final String alias, final String[] args, final boolean isQuick){
		
		// -------------------- <Permissions> ------------------
				
		if(!sender.hasPermission("serenityloans.loan.lend")) {
			sender.sendMessage(Conf.messageCenter("perm-lend-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias}));
			return true;
		}
		
		// -------------------- </Permissions> ------------------
			
		
		
		// -------------------- <Syntax> ------------------
		
		if(args.length == 1){
			sender.sendMessage(Conf.messageCenter("missing-entity-argument", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias}));
			sender.sendMessage(Conf.messageCenter("command-help", new String[]{"$$p", "$$c", "$$h"}, new String[]{sender.getName(), "/" + alias, "/" + getHelpCommand(alias)}));
					
			return true;
		}
		
		// -------------------- </Syntax> ------------------
		
					
		
		// -------------------- <Parse Inputs> --------------------

				
		// Collect financialEntity target info
		// This is potentially not safe input!!!
		final String entityTarget = args[1];
				
		if(entityTarget.equalsIgnoreCase(sender.getName())){
			sender.sendMessage(Conf.messageCenter("meta-offer-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias + args[0]}));
			return true;
		}
				
		if(entityTarget.equalsIgnoreCase("CentralBank")){
			sender.sendMessage(Conf.messageCenter("offer-government", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias + args[0], entityTarget}));
			return true;
		}
		
		// -------------------- </Parse Inputs> --------------------
		
		
		plugin.threads.execute(new Runnable(){

			@SuppressWarnings("deprecation")
			public void run() {
				
				// Check if other entity is in FinancialEntities table
				FinancialEntity borrower = null;
				
				try {
					borrower = plugin.playerManager.getFinancialEntityAdd(entityTarget);
				} catch (InterruptedException | ExecutionException | TimeoutException e) {
					// TODO add message to configuration
					scheduleMessage(prfx + " Problem during name lookup for " + entityTarget + ". Try again later.");
					return;
				}
				
				if(borrower == null){	
					scheduleMessage(Conf.messageCenter("offer-send-fail", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias, entityTarget}));
					return;
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
					scheduleMessage(Conf.messageCenter("bad-expiration", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias, entityTarget}));
					return;
				}

				// Create expiration time
				Timestamp expDate = new Timestamp(new Date().getTime() + expirationTime);
						
				// Check if offer is in PreparedOffers table
				String offerName = isQuick? "default" : "prepared";
				
				OfferExitStatus exit = plugin.offerManager.createOffer(entity.getUserID(), borrower.getUserID(), offerName, expDate);
				
				switch(exit){
				case IGNORED:
					scheduleMessage(Conf.messageCenter("talk-to-the-hand", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias, entityTarget}));
					return;
				case OVERWRITE_FAIL:
					scheduleMessage(Conf.messageCenter("overwrite-fail", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias, entityTarget}));
					return;
				case SUCCESS:
					break;
				case UNKNOWN:
					scheduleMessage(prfx + " No offer has been prepared. This is a bug. Please report.");
					return;
				}
				
				Player recipient = plugin.playerManager.getPlayer(borrower.getUserID());
				
				if(recipient == null){
					scheduleMessage(Conf.messageCenter("offline-send", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias + args[0], entityTarget}));
					return;
				}
				
				if(!recipient.hasPermission("serenityloans.loan.borrow") && !recipient.hasPermission("serenityloans.crunion.borrow")){
					scheduleMessage(Conf.messageCenter("no-can-borrow", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias + args[0], entityTarget}));
					return;
				}
				
				// Send message
				
				String recipientName = recipient.getName().equals(entityTarget)? "You" : entityTarget;
				String commandName = recipient.getName().equals(entityTarget)? "/loan " : "/crunion ";
							
				scheduleMessage(Conf.messageCenter("offer-receipt", new String[]{"$$p", "$$c", "$$r", "$$m"}, new String[]{recipient.getName(), "/" + commandName + args[0], sender.getName(), recipientName}), recipient);
				scheduleMessage(Conf.messageCenter("view-offers", new String[]{"$$p", "$$c", "$$r", "$$m"}, new String[]{recipient.getName(), "/" + commandName + args[0], sender.getName(), recipientName}), recipient);
							
				if(plugin.offerManager.registerOfferSend(entity.getUserID(), borrower.getUserID()))	
					sender.sendMessage(Conf.messageCenter("offer-send-success", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + commandName + args[0], entityTarget}));
				
			}
			
			
			private void scheduleMessage(String message){
				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, plugin.new MessageSender(sender, message));
			}
			
			private void scheduleMessage(String message, CommandSender sender){
				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, plugin.new MessageSender(sender, message));
			}
			
		});
			
				
		return true;
	}

	/*
	 * Command: /retractoffer <borrower>
	 * 
	 * Permissions: serenityloans.loan.lend
	 * 
	 * borrower = name of a player or financial institution (bank)
	 * 
	 * This command removes an offer from the given financial entity name.
	 * If there was no offer sent in the first place, this command will not
	 * give an error.
	 */
	private boolean retractOffer(final CommandSender sender, final FinancialEntity entity, final String alias, final String[] args){
		
		// -------------------- <Permissions> ------------------
		
		// Check perms			
		if(!sender.hasPermission("serenityloans.loan.lend")) {
			sender.sendMessage(Conf.messageCenter("perm-lend-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias}));
			return true;
		}
				
		// -------------------- <Syntax> ------------------
		
		
		// Check basic command syntax
		if(args.length == 1){
			sender.sendMessage(Conf.messageCenter("missing-entity-argument", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias}));
			sender.sendMessage(Conf.messageCenter("command-help", new String[]{"$$p", "$$c", "$$h"}, new String[]{sender.getName(), "/" + alias, "/" + getHelpCommand(alias)}));
			return true;
		}
								
		
		// -------------------- <Logic> ------------------
		
		
		plugin.threads.execute(new Runnable(){

			public void run() {
				UUID borrowerId = null;
				
				try {
					borrowerId = plugin.playerManager.entityIdLookup(args[1]);
				} catch (InterruptedException | ExecutionException	| TimeoutException e) {
					// TODO add message to configuration
					scheduleMessage(prfx + " Problem during name lookup for " + args[1] + ". Try again later.");
					return;
				}
				
				// TODO replace with message center message
				if(plugin.offerManager.removeOffer(entity.getUserID(), borrowerId))
					scheduleMessage(prfx + " Operation successful.");
				else
					scheduleMessage(Conf.messageCenter("generic-refuse", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias, args[1]}));
				
			}
			
			private void scheduleMessage(String message){
				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, plugin.new MessageSender(sender, message));
			}
			
		});
		
		
		return true;
		
	}
	
	/*
	 * Command: /offering [param-list]
	 * 			/defaultoffering [param-list]
	 * 
	 * Permissions: serenityloans.loan.lend
	 * 
	 * param-list = space delimited list of the form <termName>=<value>. Term names
	 * are not case sensitive. Note there must be no spaces next to the `='.
	 * 
	 * If no arguments are given, then the command displays the current state of the
	 * offer. If param-list is given, then the command sets the parameter to the 
	 * given value and reports success or failure. It then displays the new values.
	 * 
	 * /offering updates the current prepared offer, /defaultoffering updates the
	 * default prepared offer.
	 * 
	 * This command does not make any name queries, so currently runs on the main thread.
	 */
	private boolean loanOfferingCommand(CommandSender sender, FinancialEntity entity, String alias, String[] args, boolean isDefault) {
		
		// -------------------- <Permissions> ------------------
		
		// Check perms			
		if(!sender.hasPermission("serenityloans.loan.lend")) {
			sender.sendMessage(Conf.messageCenter("perm-lend-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias}));
			return true;
		}
		
		// -------------------- </Permissions> ------------------

		// If there are no arguments, then display the current values.
		if(args.length == 1)
			return viewPreparedOffer(sender, entity, isDefault);
		
		boolean success = true;
		for(int i = 1; i < args.length; i++){
			try {
				success &= plugin.offerManager.setTerms(entity.getUserID(), isDefault, args[i]);
			} catch (InvalidLoanTermsException e) {
				sender.sendMessage(Conf.messageCenter("terms-constraint-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias}));
				sender.sendMessage(e.getMessage());
			}
		}
		
		if(success){
			sender.sendMessage(Conf.messageCenter("change-success", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias}));
			
		} else {
			sender.sendMessage(Conf.messageCenter("change-fail1", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias}));
			sender.sendMessage(Conf.messageCenter("change-fail2", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias}));
			
		}
		
		sender.sendMessage(Conf.messageCenter("new-values", new String[]{"$$s", "$$p", "$$c"}, new String[]{isDefault? "default ":"" ,sender.getName(), "/" + alias + " " + (isDefault? "default ": "") +  "offering"}));
		
		// View the new state of the offer.
		viewPreparedOffer(sender, entity, isDefault);
	
		return true;
	}

	protected boolean forgiveLoan(CommandSender sender, FinancialEntity entity, String alias, String[] args){
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
	
	protected boolean sellLoan (CommandSender sender, FinancialEntity entity, String alias, String[] args){
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
	
	protected boolean buyLoan(CommandSender sender, FinancialEntity entity, String alias, String[] args){
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
	
	protected boolean viewSaleOffer(CommandSender sender, FinancialEntity entity, String alias,
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

		ImmutableOffer results = plugin.offerManager.getPreparedOffer(player.getUserID(), isDefault? "default" : "prepared");
	
		if(results == null)
			return false;
	
		double value = results.getValue();
		double interestRate = results.getInterestRate();
		double lateFee = results.getLateFee();
		double minPayment = results.getMinPayment();
		double serviceFee = results.getServiceFee();
		long term = results.getTerm();
		long compoundingPeriod = results.getCompoundingPeriod();
		long gracePeriod = results.getGracePeriod();
		long paymentTime = results.getPaymentTime();
		long paymentFrequency = results.getPaymentFrequency();
		long serviceFeeFrequency = results.getServiceFeeFrequency();
		String loanType = results.getLoanType().toString();
	
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
			
		return true;
		
		
	}
	
	private String getHelpCommand(String commandSubcommand){
		String[] intermediate = commandSubcommand.split("\\s+");
		String result = intermediate[0];
		
		// Kindof an easter egg. Should never give more than one help.
		for(int i=1; i<intermediate.length; i++)
			result += " help " + intermediate[i];
		
		return result;
	}


}
