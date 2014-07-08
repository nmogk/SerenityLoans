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
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nwmogk.bukkit.loans.Conf;
import com.nwmogk.bukkit.loans.SerenityLoans;
import com.nwmogk.bukkit.loans.api.FinancialEntity;
import com.nwmogk.bukkit.loans.object.ImmutableOffer;
import com.nwmogk.bukkit.loans.object.Loan;

public class LoanHandler implements CommandExecutor{

	public static class LoanSpec{
		public Loan result = null;
		public String[] remainingArgs = null;
		public boolean multipleValues = false;
		public String errMessage = null;
	}
	
	private SerenityLoans plugin;
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

		if(!(sender instanceof Player)) {
			sender.sendMessage(prfx + " Only players may deal in personal loans.");
			return true;
		}
		
		if(args.length == 0)
			return helpCommand(sender, alias, args);
		
		String subCommand = args[0];
		
		if (subCommand.equalsIgnoreCase("help") || subCommand.equalsIgnoreCase("?")) {
			return helpCommand(sender, alias, args);
			
		} else if (subCommand.equalsIgnoreCase("offering") || subCommand.equalsIgnoreCase("defaultoffering")) {
			// Check perms			
			if(!sender.hasPermission("serenityloans.loan.lend")) {
				sender.sendMessage(Conf.messageCenter("perm-lend-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				return true;
			}
						
			return sub1.loanOfferingCommand(sender, cmd, alias, args, subCommand.equalsIgnoreCase("defaultoffering"));
			
		} else if (subCommand.equalsIgnoreCase("sendoffer") || subCommand.equalsIgnoreCase("quickoffer")) {
			// Check basic command syntax
			if(args.length == 1){
				sender.sendMessage(Conf.messageCenter("missing-entity-argument", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				sender.sendMessage(Conf.messageCenter("command-help", new String[]{"$$p", "$$c", "$$h"}, new String[]{sender.getName(), "/" + alias +" " + subCommand, "/" + alias +" help " + subCommand}));
				
				return true;
			}
			
			// Check perms			
			if(!sender.hasPermission("serenityloans.loan.lend")) {
				sender.sendMessage(Conf.messageCenter("perm-lend-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				return true;
			}
	
			// Send to specific handler method
			return sub1.sendOffer(sender, cmd, alias, args, subCommand.equalsIgnoreCase("quickoffer"));
			
		} else if (subCommand.equalsIgnoreCase("retractoffer")) {
			// Check basic command syntax
			if(args.length == 1){
				sender.sendMessage(Conf.messageCenter("missing-entity-argument", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				sender.sendMessage(Conf.messageCenter("command-help", new String[]{"$$p", "$$c", "$$h"}, new String[]{sender.getName(), "/" + alias +" " + subCommand, "/" + alias +" help " + subCommand}));
				return true;
			}
						
			// Check perms			
			if(!sender.hasPermission("serenityloans.loan.lend")) {
				sender.sendMessage(Conf.messageCenter("perm-lend-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/" + alias +" " + subCommand}));
				return true;
			}
			
			// Send to specific handler method
			sub1.retractOffer(sender, cmd, alias, args);

		} else if (subCommand.equalsIgnoreCase("forgive")) {			
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
		
		FinancialEntity player = plugin.playerManager.getFinancialEntityRetryOnce(((Player)sender).getUniqueId());
		
		if(player == null){
			sender.sendMessage(Conf.messageCenter("perm-generic-fail", new String[]{"$$p", "$$c"}, new String[]{sender.getName(), "/loan viewoffers"}));
			return true;
		}
		
		List<FinancialEntity> othersList = null;
		
		// Three cases
		if((sentOffers && args.length == 1) || (args.length == 2 && args[1].equalsIgnoreCase(sender.getName()))){
			othersList = plugin.offerManager.getOfferRecipientsFrom(player.getUserID());
		} else if(args.length == 2){
			FinancialEntity other = plugin.playerManager.getFinancialEntityRetryOnce(args[1]);
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


}
