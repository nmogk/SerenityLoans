/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * This file is part of the SerenityLoans Bukkit plugin project.
 * 
 * File: LoanHandler.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This class performs command handling for the /loan command of the
 * SerenityLoans plugin. Most subcommands run in their own thread.  This 
 * class depends on configuration information from the 
 * com.nwmogk.bukkit.loans.Conf class.
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
		public final Loan theLoan;
		public final double amount;
		
		public LoanSale(Loan theLoan, double amount){
			this.theLoan = theLoan;
			this.amount = amount;
		}
	}
	
	private SerenityLoans plugin;
	private ConcurrentHashMap<FinancialEntity, LoanSale> pendingSales;
	private static String prfx;
	
	public LoanHandler(SerenityLoans plugin){
		this.plugin = plugin;
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
		
		
		// Send to handler method
		if (subCommand.equalsIgnoreCase("help") || subCommand.equalsIgnoreCase("?"))
			return helpCommand(sender, alias, args);
		
		else if (subCommand.equalsIgnoreCase("offering") || subCommand.equalsIgnoreCase("defaultoffering")) 
			return loanOfferingCommand(sender, entity, alias + " " + subCommand, args, subCommand.equalsIgnoreCase("defaultoffering"));
			
		else if (subCommand.equalsIgnoreCase("sendoffer") || subCommand.equalsIgnoreCase("quickoffer")) 
			return sendOffer(sender, entity, alias + " " + subCommand, args, subCommand.equalsIgnoreCase("quickoffer"));
			
		else if (subCommand.equalsIgnoreCase("retractoffer")) 
			retractOffer(sender, entity, alias + " " + subCommand, args);

		else if (subCommand.equalsIgnoreCase("forgive"))
			return forgiveLoan(sender, entity, alias + " " + subCommand, args);
						
		else if (subCommand.equalsIgnoreCase("sell"))
			return sellLoan(sender, entity, alias + " " + subCommand, args);
			
		else if (subCommand.equalsIgnoreCase("buy"))
			return buyLoan(sender, entity, alias + " " + subCommand, args);
			
		else if (subCommand.equalsIgnoreCase("viewsaleoffer")) 
			return viewSaleOffer(sender, entity, alias + " " + subCommand, args);			
			
		else if (subCommand.equalsIgnoreCase("viewoffers") || subCommand.equalsIgnoreCase("viewoffer") || subCommand.equalsIgnoreCase("viewsentoffer") || subCommand.equalsIgnoreCase("viewsentoffers"))  
			return loanViewoffersCommand(sender, entity, alias + " " + subCommand, args, subCommand.equalsIgnoreCase("viewsentoffer") || subCommand.equalsIgnoreCase("viewsentoffers"));
		
		else if (subCommand.equalsIgnoreCase("accept") || subCommand.equalsIgnoreCase("acceptoffer")) {
			acceptOffer(sender, entity, alias + " " + subCommand, args);
			
		} else if (subCommand.equalsIgnoreCase("reject") || subCommand.equalsIgnoreCase("rejectoffer")) {
			
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
			rejectOffer(sender, entity, alias + " " + subCommand, args);
			
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

			ignoreOffers(sender, entity, alias + " " + subCommand, args);			
			
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
			
			payLoan(sender, entity, alias + " " + subCommand, args, false);
			
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
			
			payLoan(sender, entity, alias + " " + subCommand, args, true);
			
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
			
			setAutoPay(sender, entity, alias + " " + subCommand, args);
			
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
			
			viewStatement(sender, entity, alias + " " + subCommand, args);
			
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
		
		if(args.length == 0 || args.length <= 1 || !args[0].equalsIgnoreCase("help")){
			sender.sendMessage(basicHelp);
			
			if(sender.hasPermission("serenityloans.loan.lend"))
				sender.sendMessage(lendingCommands);
			if(sender.hasPermission("serenityloans.loan.borrow"))
				sender.sendMessage(borrowCommands);
			
			return true;
		}
		
		String subCommand = args[1];
		
		if (subCommand.equalsIgnoreCase("help") || subCommand.equalsIgnoreCase("?")) {
			sender.sendMessage(prfx + "Whoah, how meta. I think you have it figured out.");
		} else if (subCommand.equalsIgnoreCase("offering") || subCommand.equalsIgnoreCase("defaultoffering")) {
			sender.sendMessage(new String[]{
					"Command: /loan offering [param-list]",
					"         /loan defaultoffering [param-list]",
					"",
					"Permissions: serenityloans.loan.lend",
					"param-list = space delimited list of the form <termName>=<value>. Term names",
					"are not case sensitive. Note there must be no spaces next to the `='.",
					"",
					"If no arguments are given, then the command displays the current state of the",
					"offer. If param-list is given, then the command sets the parameter to the",
					"given value and reports success or failure. It then displays the new values.",
					"",
					"/loan offering updates the current prepared offer, /loan defaultoffering updates the",
					"default prepared offer."
			});
		} else if (subCommand.equalsIgnoreCase("sendoffer") || subCommand.equalsIgnoreCase("quickoffer")) {
			sender.sendMessage(new String[]{
					"Command: /loan sendoffer <borrower> [expiration time]",
					"         /loan quickoffer <borrower> [expiration time]",
					"",
					"Permissions: serenityloans.loan.lend",
					"",
					"borrower = name of a player or financial institution (bank)",
					"",
					"expiration time = length of time for loan to be active formatted",
					"as a list of integers followed by units. Available units are",
					"y (year), d (day), h (hour), m (minutes), s (seconds). Spaces",
					"are optional. ex - \"3d 4h 1 s\" represents 3 days 4 hours and 1 second.",
					"",
					"This command sends a loan offer to the given potential borrower.",
					"The borrower may be either a player or a financial institution and",
					"can be offline. However, the borrower must have the proper",
					"permissions to borrow for the command to work. ",
					"",
					"/loan sendoffer sends the current prepared offer",
					"/loan quickoffer sends the default prepared offer"
			});
		} else if (subCommand.equalsIgnoreCase("retractoffer")) {
			sender.sendMessage(new String[]{
					"Command: /loan retractoffer <borrower>",
					"",
					"Permissions: serenityloans.loan.lend",
					"",
					"borrower = name of a player or financial institution (bank)",
					"",
					"This command removes an offer from the given financial entity name.",
					"If there was no offer sent in the first place, this command will not",
					"give an error."
			});
		} else if (subCommand.equalsIgnoreCase("forgive")) {
			sender.sendMessage(new String[]{
					"Command: /loan forgive <borrower [account]> [amount]",
					"",
					"Permissions: serenityloans.loan.lend",
					"",
					"borrower = name of borrower of loan",
					"account = loan number if multiple loans with same borrower",
					"amount = dollar value to forgive. Default: {total balance}",
					"",
					"This command forgives the borrower loan by the amount given. If",
					"no amount argument is given, will forgive entire loan. If the",
					"borrower has multiple loans, a specific loan must be chosen from",
					"an index list. /loan forgive <borrower> 0 is a safe way to view",
					"the index list without accidentally forgiving anything." 
			});
		} else if (subCommand.equalsIgnoreCase("sell")) {
			sender.sendMessage(new String[]{
					"Command: /loan sell <borrower [account]> <new lender> <amount>",
					"",
					"Permissions: serenityloans.loan.lend",
					"",
					"borrower = name of borrower of loan",
					"account = loan number if multiple loans with same borrower",
					"new lender = entity to attempt to sell loan to",
					"amount = dollar value of sale",
					"",
					"This command sets up a loan to be sold to new lender for the",
					"given amount. If the borrower has multiple loans, a specific",
					"loan must be chosen from an index list. Sale offers will not",
					"persist across plugin resets, and the new lender must be ",
					"online. "
			});
		} else if (subCommand.equalsIgnoreCase("buy")) {
			sender.sendMessage(new String[]{
					"Command /loan buy",
					"",
					"Permissions: serenityloans.loan.lend",
					"",
					"This command executes the pending sale offer for the sender.",
					"It will fail if the sender does not have enough money. If",
					"the sale is successful, the old lender and the borrower are",
					"notified if they are online."
			});
		} else if (subCommand.equalsIgnoreCase("viewsaleoffer")) {
			sender.sendMessage(new String[]{
					"Command: /loan viewsaleoffer",
					"",
					"Permissions: serenityloans.loan.lend",
					"",
					"This command displays a received loan sale offer including",
					"the price and the loan in question."
			});
		} else if (subCommand.equalsIgnoreCase("viewoffers") || subCommand.equalsIgnoreCase("viewoffer") || subCommand.equalsIgnoreCase("viewsentoffer") || subCommand.equalsIgnoreCase("viewsentoffers")) { 			
			sender.sendMessage(new String[]{
					"Command: /loan viewoffer[s] [lender]",
					"         /loan viewsentoffer[s] [borrower]",
					"",
					"Permissions: serenityloans.loan.borrow",
					"          OR serenityloans.loan.lend",
					"",
					"lender = name of entity which sent the offer",
					"borrower = name of entity to which the offer was sent",
					"",
					"*The plural form of both of these commands are built-in",
					"aliases.",
					"",
					"This command shows outstanding offers. Giving a particular",
					"other entity to the command shows the detailed terms of an offer",
					"while leaving the argument blank will show the list of all",
					"relevant offers. Putting your own name as the optional argument",
					"has the same effect as /loan viewsentoffer with no arguments.",
					"",
					"/loan viewoffer shows offers which you have received",
					"/loan viewsentoffer shows offers which you have sent"
			});
		} else if (subCommand.equalsIgnoreCase("accept")) {
			sender.sendMessage(new String[]{
					"Command: /loan accept <lender>",
					"         /loan acceptoffer <lender>",
					"",
					"Permissions: serenityloans.loan.borrow",
					"",
					"lender = name of the entity which sent the offer",
					"",
					"This command will accept a loan offer from the given",
					"lender and create a new loan with the terms of the",
					"offer."
			});
		} else if (subCommand.equalsIgnoreCase("reject")) {
				
		} else if (subCommand.equalsIgnoreCase("ignore")) {

		} else if (subCommand.equalsIgnoreCase("pay")) {
			
		} else if (subCommand.equalsIgnoreCase("payoff")) {

		} else if (subCommand.equalsIgnoreCase("summary")) {
			
		} else if (subCommand.equalsIgnoreCase("setautopay")) {
			
		} else if (subCommand.equalsIgnoreCase("statement")) {
	
		} else {
			return false;
		}
		
		//TODO implement command documentation

		return true;
	}

	/*
	 * Command: /loan viewoffer[s] [lender]
	 *          /loan viewsentoffer[s] [borrower]
	 *          
	 * Permissions: serenityloans.loan.borrow
	 *           OR serenityloans.loan.lend
	 *           
	 * lender = name of entity which sent the offer
	 * borrower = name of entity to which the offer was sent
	 * 
	 * *The plural form of both of these commands are built-in
	 * aliases.
	 *           
	 * This command shows outstanding offers. Giving a particular
	 * other entity to the command shows the detailed terms of an offer
	 * while leaving the argument blank will show the list of all
	 * relevant offers. Putting your own name as the optional argument
	 * has the same effect as /loan viewsentoffer with no arguments.
	 * 
	 * /loan viewoffer shows offers which you have received
	 * /loan viewsentoffer shows offers which you have sent
	 */
	private boolean loanViewoffersCommand(final CommandSender sender, final FinancialEntity player, final String alias, final String[] args, final boolean sentOffers) {
		
		// Check perms			
		if(!sender.hasPermission("serenityloans.loan.borrow") || !sender.hasPermission("serenityloans.loan.lend")) {
			sender.sendMessage(Conf.messageCenter("perm-generic-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias}));
			return true;
		}
		
		// Check basic command syntax
		if(args.length > 2){
			sender.sendMessage(Conf.messageCenter("too-many-arguments", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias}));
			sender.sendMessage(Conf.messageCenter("command-help", new String[]{"$$p", "$$c", "$$h"}, new String[]{sender.getName(), "/" + alias, "/" + getHelpCommand(alias)}));
			return true;
		}
		
		plugin.threads.execute(new Runnable(){
			
			@SuppressWarnings("deprecation")
			public void run(){
				
				List<FinancialEntity> othersList = null;
				
				// Three cases
				if((sentOffers && (args.length == 1 || args[1].equalsIgnoreCase(sender.getName()))) || (args.length == 2 && args[1].equalsIgnoreCase(sender.getName()))){
					othersList = plugin.offerManager.getOfferRecipientsFrom(player.getUserID());
				
				} else if(args.length == 2){
					FinancialEntity other = null;
					String entityTarget = args[1];
					
					try {
						other = plugin.playerManager.getFinancialEntity(args[1]);
					} catch (InterruptedException | ExecutionException | TimeoutException e) {
						// TODO add message to configuration
						plugin.scheduleMessage(sender, prfx + " Problem during name lookup for " + entityTarget + ". Try again later.");
						return;
					}
					
					// Has to be generic because it could be a lender or borrower
					// Maybe add configuration message for this case.
					if(other == null){
						plugin.scheduleMessage(sender, Conf.messageCenter("generic-refuse", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias, entityTarget}));
						return;
					}
					
					ImmutableOffer offer = sentOffers? plugin.offerManager.getOffer(player.getUserID(), other.getUserID()) : plugin.offerManager.getOffer(other.getUserID(), player.getUserID());
				
					plugin.scheduleMessage(sender, String.format(prfx + " Details for offer %s %s.", sentOffers? "to":"from", args[1]));
					plugin.scheduleMessage(sender, offer.toString(plugin));
					
					return;
				} else {
					othersList = plugin.offerManager.getOfferSendersTo(player.getUserID());
				}
				
				if(othersList.size() == 0){
					plugin.scheduleMessage(sender, Conf.messageCenter("no-offers", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), alias, args.length == 2 && args[1].equalsIgnoreCase(sender.getName()) ? args[1] : ""}));
					return;
				}
				
				String lenderLabel = args.length == 1? "Lender" : "Recipient";
					
				// Table formatting
				String output = Conf.messageCenter(args.length == 1 && !sentOffers ? "received-offers" : "sent-offers", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), alias});
				output += "\n    " + lenderLabel + " --- Expires";
				
				// Collect information for each offer in the list.
				for(FinancialEntity fe : othersList){
					String name = null;
					try {
						name = plugin.playerManager.entityNameLookup(fe);
					} catch (InterruptedException | ExecutionException | TimeoutException e) {
						plugin.scheduleMessage(sender, String.format("%s Problem during name lookup for %s. Try again later.", prfx, fe.getUserID().toString()));
						continue;
					}
					
					ImmutableOffer offer = sentOffers? plugin.offerManager.getOffer(player.getUserID(), fe.getUserID()) : plugin.offerManager.getOffer(fe.getUserID(), player.getUserID());
				
					Timestamp exp = offer.getExpirationDate();
					
					output += String.format("\n    %s - %s", name, DateFormat.getDateInstance().format(exp));
					
				}
						
				plugin.scheduleMessage(sender, output.split("\n"));
				
			}
			
		});
				
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
	private LoanSpec parseLoanArguments(FinancialEntity sender, FinancialEntity target, String senderName, String[] args, boolean isSenderLender){
		
		LoanSpec result = new LoanSpec();
		result.result = null;
		result.remainingArgs = null;
		result.multipleValues = false;
		result.errMessage = null;
		
		int remainingIndex = 2;
		
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
				result.result = potentials[0];
				result.multipleValues = true;
				return result;
			}
			
			if (loanIndex >= potentials.length || loanIndex < 1){
				result.errMessage = Conf.messageCenter("unknown-loan-selection", new String[]{"$$p", "$$r", "$$c"}, new String[]{senderName, args[1], "$$c"});
				result.result = potentials[0];
				result.multipleValues = true;
				return result;
			}
			
			result.result = potentials[loanIndex - 1];
			remainingIndex++;
			
			
		}
		
		String[] remainingArgs = new String[args.length - remainingIndex];
		
		// Copy remaining args into new array
		for(int i = remainingIndex; i < args.length; i++){
			remainingArgs[i - remainingIndex] = args[i];
		}
		
		result.remainingArgs = remainingArgs;
		
		return result;
		
	}
	
	/*
	 * Command: /loan sendoffer <borrower> [expiration time]
	 * 			/loan quickoffer <borrower> [expiration time]
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
	 * /loan sendoffer sends the current prepared offer
	 * /loan quickoffer sends the default prepared offer
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
					plugin.scheduleMessage(sender, prfx + " Problem during name lookup for " + entityTarget + ". Try again later.");
					return;
				}
				
				if(borrower == null){	
					plugin.scheduleMessage(sender, Conf.messageCenter("offer-send-fail", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias, entityTarget}));
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
					plugin.scheduleMessage(sender, Conf.messageCenter("bad-expiration", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias, entityTarget}));
					return;
				}

				// Create expiration time
				Timestamp expDate = new Timestamp(new Date().getTime() + expirationTime);
						
				// Check if offer is in PreparedOffers table
				String offerName = isQuick? "default" : "prepared";
				
				OfferExitStatus exit = plugin.offerManager.createOffer(entity.getUserID(), borrower.getUserID(), offerName, expDate);
				
				switch(exit){
				case IGNORED:
					plugin.scheduleMessage(sender, Conf.messageCenter("talk-to-the-hand", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias, entityTarget}));
					return;
				case OVERWRITE_FAIL:
					plugin.scheduleMessage(sender, Conf.messageCenter("overwrite-fail", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias, entityTarget}));
					return;
				case SUCCESS:
					break;
				case UNKNOWN:
					plugin.scheduleMessage(sender, prfx + " No offer has been prepared. This is a bug. Please report.");
					return;
				}
				
				Player recipient = plugin.playerManager.getPlayer(borrower.getUserID());
				
				if(recipient == null){
					plugin.scheduleMessage(sender, Conf.messageCenter("offline-send", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias, entityTarget}));
					return;
				}
				
				if(!recipient.hasPermission("serenityloans.loan.borrow") && !recipient.hasPermission("serenityloans.crunion.borrow")){
					plugin.scheduleMessage(sender, Conf.messageCenter("no-can-borrow", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias, entityTarget}));
					return;
				}
				
				// Send message
				
				String recipientName = recipient.getName().equals(entityTarget)? "You" : entityTarget;
				String commandName = recipient.getName().equals(entityTarget)? "/loan " : "/crunion ";
							
				plugin.scheduleMessage(recipient, Conf.messageCenter("offer-receipt", new String[]{"$$p", "$$c", "$$r", "$$m"}, new String[]{recipient.getName(), "/" + commandName + args[0], sender.getName(), recipientName}));
				plugin.scheduleMessage(recipient, Conf.messageCenter("view-offers", new String[]{"$$p", "$$c", "$$r", "$$m"}, new String[]{recipient.getName(), "/" + commandName + args[0], sender.getName(), recipientName}));
							
				if(plugin.offerManager.registerOfferSend(entity.getUserID(), borrower.getUserID()))	
					plugin.scheduleMessage(sender, Conf.messageCenter("offer-send-success", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + commandName + args[0], entityTarget}));
				
			}
			
			
			
		});
			
				
		return true;
	}

	/*
	 * Command: /loan retractoffer <borrower>
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
					plugin.scheduleMessage(sender, prfx + " Problem during name lookup for " + args[1] + ". Try again later.");
					return;
				}
				
				// TODO replace with message center message
				if(plugin.offerManager.removeOffer(entity.getUserID(), borrowerId))
					plugin.scheduleMessage(sender, prfx + " Operation successful.");
				else
					plugin.scheduleMessage(sender, Conf.messageCenter("generic-refuse", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias, args[1]}));
				
			}
			
		});
		
		
		return true;
		
	}
	
	/*
	 * Command: /loan offering [param-list]
	 * 			/loan defaultoffering [param-list]
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
	 * /loan offering updates the current prepared offer, /loan defaultoffering updates the
	 * default prepared offer.
	 * 
	 * ======================================================================================
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

	/*
	 * Command: /loan forgive <borrower [account]> [amount]
	 * 
	 * Permissions: serenityloans.loan.lend
	 * 
	 * borrower = name of borrower of loan
	 * account = loan number if multiple loans with same borrower
	 * amount = dollar value to forgive. Default: {total balance}
	 * 
	 * This command forgives the borrower loan by the amount given. If
	 * no amount argument is given, will forgive entire loan. If the
	 * borrower has multiple loans, a specific loan must be chosen from
	 * an index list. /loan forgive <borrower> 0 is a safe way to view
	 * the index list without accidentally forgiving anything.
	 */
	private boolean forgiveLoan(final CommandSender sender, final FinancialEntity entity, final String alias, final String[] args){

		// -------------------- Permissions ------------------
				
		if(!sender.hasPermission("serenityloans.loan.lend")) {
			sender.sendMessage(Conf.messageCenter("perm-lend-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias}));
			return true;
		}

		
		// -------------------- Syntax ------------------
		
		if(args.length == 1){
			sender.sendMessage(Conf.messageCenter("missing-borrower-argument", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias}));
			sender.sendMessage(Conf.messageCenter("command-help", new String[]{"$$p", "$$c", "$$h"}, new String[]{sender.getName(), "/" + alias, "/" + getHelpCommand(alias)}));
			return true;
		}

		
		
		// -------------------- <Logic> ------------------
		
		plugin.threads.execute(new Runnable(){
			
			@SuppressWarnings("deprecation")
			public void run(){
				
				String borrowerName = args[1];
				
				
				FinancialEntity lender = entity;
				FinancialEntity borrower = null;
				try {
					
					// Parse borrower argument
					borrower = plugin.playerManager.getFinancialEntity(borrowerName);
				
				} catch (InterruptedException | ExecutionException | TimeoutException e) {
					// TODO add message to configuration
					plugin.scheduleMessage(sender, prfx + " Problem during name lookup for " + borrowerName + ". Try again later.");
					return;
				}
				
				// Parse loan selection
				LoanSpec loanSelection = parseLoanArguments(lender, borrower, sender.getName(), args, true);
				
				// Report error if found
				if(loanSelection.errMessage != null){
					plugin.scheduleMessage(sender, Conf.parseMacros(loanSelection.errMessage, new String[]{"$$c"}, new String[]{"/" + alias + " forgive"}));
					
					if(!loanSelection.multipleValues)
						return;
				}
				
				// If multiple loans were found, list them to the caller
				if(loanSelection.multipleValues){
					plugin.scheduleMessage(sender, Conf.messageCenter("multiple-loans", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias + " forgive", borrowerName}));
					
					Loan[] allLoans = plugin.loanManager.getLoan(lender, borrower);
					for(int i = 0; i < allLoans.length ; i++){
						plugin.scheduleMessage(sender, String.format("    %d: %s", i, allLoans[i].getShortDescription(plugin, false) ));
					}
					
					return;
				}
				
				double amount = 0;
				Loan theLoan = loanSelection.result;
				String[] toParse = loanSelection.remainingArgs;
				
				// Parse amount to forgive. Ignore arguments afterwards
				if(toParse.length >= 1){
					try{
						amount = Double.parseDouble(toParse[0]);
					} catch (NumberFormatException e){
						plugin.scheduleMessage(sender, Conf.messageCenter("number-parse-fail", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias + " forgive", borrowerName}));
						return;
					}
				} else 
					amount = theLoan.getCloseValue();
				
				// Apply the forgiveness
				plugin.loanManager.applyPayment(theLoan, amount);
				
				plugin.scheduleMessage(sender, Conf.messageCenter("loan-forgive", new String[]{"$$p", "$$c", "$$r", "$$b"}, new String[]{sender.getName(), "/" + alias + " forgive", borrowerName, plugin.econ.format(amount)}));
			}

		});
		
		return true;
	}
	
	/*
	 * Command: /loan sell <borrower [account]> <new lender> <amount>
	 * 
	 * Permissions: serenityloans.loan.lend
	 * 
	 * borrower = name of borrower of loan
	 * account = loan number if multiple loans with same borrower
	 * new lender = entity to attempt to sell loan to
	 * amount = dollar value of sale
	 * 
	 * This command sets up a loan to be sold to new lender for the
	 * given amount. If the borrower has multiple loans, a specific
	 * loan must be chosen from an index list. Sale offers will not
	 * persist across plugin resets, and the new lender must be 
	 * online. 
	 * 
	 */
	private boolean sellLoan (final CommandSender sender, final FinancialEntity entity, final String alias, final String[] args){

		//-------------------- Permissions --------------------
		
		if(!sender.hasPermission("serenityloans.loan.lend")) {
			sender.sendMessage(Conf.messageCenter("perm-lend-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias}));
			return true;
		}
		
		//-------------------- Syntax --------------------
		
		if(args.length == 1){
			sender.sendMessage(Conf.messageCenter("missing-borrower-argument", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias}));
			sender.sendMessage(Conf.messageCenter("command-help", new String[]{"$$p", "$$c", "$$h"}, new String[]{sender.getName(), "/" + alias, "/" + getHelpCommand(alias)}));
			return true;
		}

		//-------------------- Logic --------------------
		
		plugin.threads.execute(new Runnable(){
			
			@SuppressWarnings("deprecation")
			public void run(){
				
				String borrowerName = args[1];
				FinancialEntity lender = entity;
				FinancialEntity borrower = null;
				FinancialEntity recipient = null;
				
				try{
					
					borrower = plugin.playerManager.getFinancialEntity(borrowerName);
					
				} catch (InterruptedException | ExecutionException | TimeoutException e) {
					// TODO add message to configuration
					plugin.scheduleMessage(sender, prfx + " Problem during name lookup for " + borrowerName + ". Try again later.");
					return;
				}
			
				// Parse loan selection
				LoanSpec loanSelection = LoanHandler.parseLoanArguments(lender, borrower, sender.getName(), args, true);
				
				
				if(loanSelection.errMessage != null){
					plugin.scheduleMessage(sender, Conf.parseMacros(loanSelection.errMessage, new String[]{"$$c"}, new String[]{"/" + alias}));
					
					if(!loanSelection.multipleValues)
						return;
				}
				
				// List available loans to sender
				if(loanSelection.multipleValues){
					plugin.scheduleMessage(sender, Conf.messageCenter("multiple-loans", new String[]{"$$p", "$$c", "$$r"}, new String[]{sender.getName(), "/" + alias, borrowerName}));
					Loan[] allLoans = plugin.loanManager.getLoan(lender, borrower);
					for(int i = 0; i < allLoans.length ; i++){
						plugin.scheduleMessage(sender, String.format("    %d: %s", i, allLoans[i].getShortDescription(plugin, false) ));
					}
					
					return;
				}
				
				// New syntax checking now that loan selection has been taken care of
				// (variable argument length before this)
				String[] toParse = loanSelection.remainingArgs;
				
				if(toParse.length < 2){
					plugin.scheduleMessage(sender, prfx + " Syntax incorrect. Must specify new lender and amount of sale.");
					return;
				}
				
				String recipientName = toParse[0];
				
				// Get the new lender financial entity
				try{
					
					recipient = plugin.playerManager.getFinancialEntityAdd(recipientName);
					
				} catch (InterruptedException | ExecutionException | TimeoutException e) {
					// TODO add message to configuration
					plugin.scheduleMessage(sender, prfx + " Problem during name lookup for " + recipientName + ". Try again later.");
					return;
				}
				
				if(recipient == null){
					plugin.scheduleMessage(sender, String.format(prfx + " You cannot sell a loan to %s.", toParse[0]));
					return;
				}
				
				Player newLender = plugin.playerManager.getPlayer(recipient.getUserID());
				
				// New lender must be online
				if(newLender == null){
					plugin.scheduleMessage(sender, prfx + " New lender must be online to complete transaction.");
					return;
				}
				
				// Check permissions of new lender
				if(!newLender.hasPermission("serenityloans.loan.lend") && !newLender.hasPermission("serenityloans.crunion.lend")){
					plugin.scheduleMessage(sender, String.format(prfx + " %s does not have permission to buy loan.", toParse[0]));
					return;
				}
				
				double amount = 0;
				
				// Parse sale amount input
				try{
					amount = Double.parseDouble(toParse[1]);
				} catch (NumberFormatException e){
					plugin.scheduleMessage(sender, prfx + " Amount specified incorrectly.");
					return;
				}
				
				// Create sale record object
				LoanSale sale = new LoanSale(loanSelection.result, amount);
				// Each entity can only have a single loan offer at a time.
				pendingSales.put(recipient, sale);
				
				// Send message to new lender
				
				String recipientString = recipient.getUserID().equals(newLender.getUniqueId())? "You" : ((FinancialInstitution)recipient).getName();
				String commandName = recipient.getUserID().equals(newLender.getUniqueId())? "/loan" : "/crunion";
				
				plugin.scheduleMessage(newLender, String.format(prfx + " %s received a loan sale offer from %s for %s.", recipientString, sender.getName(), plugin.econ.format(amount)));
				plugin.scheduleMessage(newLender, String.format(prfx + "Type '%s viewsaleoffer' to view details.", commandName));
				plugin.scheduleMessage(newLender, String.format(prfx + "Type '%s buy' to purchase loan.", commandName));
			
				
				// TODO upgrade message
				plugin.scheduleMessage(sender, prfx + " Sale offer sent successfully.");
				
			}
			
		});

		return true;
	}
	
	/*
	 * Command /loan buy
	 * 
	 * Permissions: serenityloans.loan.lend
	 * 
	 * This command executes the pending sale offer for the sender.
	 * It will fail if the sender does not have enough money. If
	 * the sale is successful, the old lender and the borrower are
	 * notified if they are online.
	 * 
	 * =============================================================
	 * 
	 * This command does not use a name lookup, and so is on the main thread for now.
	 */
	private boolean buyLoan(CommandSender sender, FinancialEntity entity, String alias, String[] args){
		// TODO add configuration messages this whole method
		
		//------------------ Permissions --------------------
		
		if(!sender.hasPermission("serenityloans.loan.lend")){
			sender.sendMessage(Conf.messageCenter("perm-buy-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias}));
			return true;
		}
			
		FinancialEntity buyer = entity;
		
		
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
		
		FinancialEntity oldLender = ls.theLoan.getLender();
		
		
		plugin.econ.withdraw(buyer, ls.amount);
		plugin.econ.deposit(oldLender, ls.amount);
		
		if(plugin.loanManager.setLender(ls.theLoan.getLoanID(), buyer.getUserID()))
			sender.sendMessage(prfx + " Purchase processed successfully!");
		else {
			sender.sendMessage(prfx + " Error processing purchase.");
			return true;
		}
		
		Player oldLend = plugin.playerManager.getPlayer(oldLender.getUserID());
		Player borrower = plugin.playerManager.getPlayer(ls.theLoan.getBorrower().getUserID());
		
		if(oldLend != null)
			oldLend.sendMessage(prfx + " Loan sale succeeded!");
		if(borrower != null)
			borrower.sendMessage(prfx + " Your loan has been sold to another lender!");
		
		return true;
	}
	
	/*
	 * Command: /loan viewsaleoffer
	 * 
	 * Permissions: serenityloans.loan.lend
	 * 
	 * This command displays a received loan sale offer including
	 * the price and the loan in question.
	 * 
	 * =================================================================
	 * 
	 * This command does not use a name lookup, and so is on the main thread for now.
	 */
	private boolean viewSaleOffer(CommandSender sender, FinancialEntity entity, String alias,
			String[] args) {
	
		// TODO Message center
		
		if(!sender.hasPermission("serenityloans.loan.lend")){
			sender.sendMessage(Conf.messageCenter("perm-lend-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias}));
			return true;
		}

		if(!pendingSales.containsKey(entity)){
			sender.sendMessage(prfx + " You do not have any outstanding offers to buy a loan");
			return true;
		}
		
		LoanSale ls = pendingSales.get(entity);
		
		sender.sendMessage(String.format(prfx + " You have an offer to buy a loan for %s.", plugin.econ.format(ls.amount)));
		sender.sendMessage(ls.theLoan.toString(plugin));
		
		
		return false;
	}


	/*
	 * Displays a prepared offer of the given player provides all of the formatting
	 * in a String array, which it displays. Will automatically display or hide 
	 * information relating to service fees and late fees as necessary.
	 */
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
	
	/*
	 * Takes a command+subcommand and inserts the word 'help' between
	 * the two portions, forming the help function name.
	 */
	private String getHelpCommand(String commandSubcommand){
		String[] intermediate = commandSubcommand.split("\\s+");
		String result = intermediate[0];
		
		// Kindof an easter egg. Should never give more than one help.
		for(int i=1; i<intermediate.length; i++)
			result += " help " + intermediate[i];
		
		return result;
	}

	/*
	 * Command: /loan accept <lender>
	 *          /loan acceptoffer <lender>
	 *          
	 * Permissions: serenityloans.loan.borrow
	 * 
	 * lender = name of the entity which sent the offer
	 * 
	 * This command will accept a loan offer from the given
	 * lender and create a new loan with the terms of the
	 * offer.
	 */
	private boolean acceptOffer(final CommandSender sender, final FinancialEntity borrower, final String alias, final String[] args){
		
		// Check permissions
		
		if(!sender.hasPermission("serenityloans.loan.borrow")) {
			sender.sendMessage(Conf.messageCenter("perm-borrow-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias}));
			return true;
		}
		
		// Basic syntax
		
		if(args.length == 1){
			sender.sendMessage(Conf.messageCenter("missing-entity-argument", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias}));
			sender.sendMessage(Conf.messageCenter("command-help", new String[]{"$$p", "$$c", "$$h"}, new String[]{sender.getName(), "/" + alias, "/" + getHelpCommand(alias)}));
			return true;
		}
		
		plugin.threads.execute(new Runnable(){
			
			@SuppressWarnings("deprecation")
			public void run(){
				
				String lenderName = args[1];
				
				FinancialEntity lender = null;
				try {
					lender = plugin.playerManager.getFinancialEntityAdd(lenderName);
				} catch (InterruptedException | ExecutionException | TimeoutException e) {
					// TODO add message to configuration
					plugin.scheduleMessage(sender, prfx + " Problem during name lookup for " + lenderName + ". Try again later.");
					return;
				}
				
				if(lender == null) {
					plugin.scheduleMessage(sender, prfx + " Lender entity not found.");
					return;
				}
				
				ImmutableOffer theOffer = plugin.offerManager.getOffer(lender.getUserID(), borrower.getUserID());
				
				if(theOffer == null){
					plugin.scheduleMessage(sender, String.format(prfx + " You do not have any outstanding offers from %s.", lenderName));
					return;
				}
				
				Timestamp expires = theOffer.getExpirationDate();
				
				if(expires.before(new Date())){
					plugin.scheduleMessage(sender, String.format(prfx + " You do not have any outstanding offers from %s.", lenderName));
					return;
				}
				
				int termsID = theOffer.getPreparedTermsId();
				
				double value = plugin.offerManager.getTermsValue(termsID);
				
				if(!plugin.econ.has(lender, value).callSuccess){
					
					plugin.scheduleMessage(sender, String.format(prfx + " %s does not have enough money to loan!", lenderName));
					return;
				}
				
				plugin.econ.withdraw(lender, value);
				plugin.econ.deposit(borrower, value);
				
				boolean returnSuccess = plugin.loanManager.createLoan(lender.getUserID(), borrower.getUserID(), termsID, value);
				
				// TODO message center
				if(returnSuccess){
					plugin.scheduleMessage(sender, prfx + " Successfully processed loan!");
					
					Player newLender = plugin.playerManager.getPlayer(lender.getUserID());
					
					if(newLender != null)
						plugin.scheduleMessage(newLender, prfx + " Successfully processed loan!");
				} else {
					plugin.scheduleMessage(sender, prfx + " Loan not processed!");
				}
				
			}
		});

		return true;
	}
	
	protected boolean ignoreOffers(CommandSender sender, FinancialEntity entity,  String alias, String[] args){
		
		FinancialEntity requester = plugin.playerManager.getFinancialEntityAdd(((Player)sender).getUniqueId());
		FinancialEntity target = plugin.playerManager.getFinancialEntityAdd(args[1]);
		
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

	protected boolean rejectOffer(CommandSender sender, FinancialEntity entity, String alias, String[] args){
		boolean success = plugin.offerManager.removeOffer(((Player)sender).getUniqueId(), plugin.playerManager.entityIdLookup(args[1]));
		
		if(success)
			sender.sendMessage(prfx + " Offer removed successfully.");
		else
			sender.sendMessage(prfx + " Unable to complete request.");
		
		return true;
	}

	protected boolean payLoan(CommandSender sender, FinancialEntity entity, String alias, String[] args, boolean payOff) {

		FinancialEntity borrower = plugin.playerManager.getFinancialEntityAdd(((Player)sender).getUniqueId());
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

	protected boolean viewStatement(CommandSender sender, FinancialEntity entity, String alias, String[] args) {
		
		FinancialEntity borrower = plugin.playerManager.getFinancialEntityAdd(((Player)sender).getUniqueId());
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

	protected boolean setAutoPay(CommandSender sender, FinancialEntity entity, String alias, String[] args) {
		// TODO Auto-generated method stub
		return false;
	}
}
