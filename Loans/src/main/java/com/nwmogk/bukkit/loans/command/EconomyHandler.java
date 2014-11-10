package com.nwmogk.bukkit.loans.command;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.nwmogk.bukkit.loans.Conf;
import com.nwmogk.bukkit.loans.SerenityLoans;
import com.nwmogk.bukkit.loans.api.EconResult;
import com.nwmogk.bukkit.loans.api.FinancialEntity;

public class EconomyHandler  implements CommandExecutor {
	
	private SerenityLoans plugin;
	private String prfx;
	
	private enum BalanceType{CASH, ASSETS, NET_WORTH};
	
	private enum EcoAction{ADD, SUBTRACT, SET};
	
	public EconomyHandler(SerenityLoans plugin){
		prfx = Conf.getMessageString();
		this.plugin = plugin;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
		
		boolean payPerms = sender.hasPermission("serenityloans.pay");
		boolean balancePerms = sender.hasPermission("serenityloans.balance");
		boolean ecoPerms = sender.hasPermission("serenityloans.admin");
		
		if(cmd.getName().equalsIgnoreCase("sl-pay") && payPerms){
			if(sender instanceof ConsoleCommandSender){
				magicMoney(sender, cmd, args);
				return true;
			}
			
			pay(sender, args);
			
			return true;
		} else if (cmd.getName().equalsIgnoreCase("sl-cash") && balancePerms) {
			balanceLookup(sender, args, BalanceType.CASH);
			return true;
		} else if (cmd.getName().equalsIgnoreCase("sl-balance") && balancePerms) {
			balanceLookup(sender, args, BalanceType.ASSETS);
			return true;
		} else if (cmd.getName().equalsIgnoreCase("sl-networth") && balancePerms) {
			balanceLookup(sender, args, BalanceType.NET_WORTH);
			return true;
		} else if (cmd.getName().equalsIgnoreCase("sl-eco") && ecoPerms) {
			
			
		}
		
		sender.sendMessage(Conf.messageCenter("perm-generic-fail", null, null));

		return false;
	}
	
	private void balanceLookup(final CommandSender sender, final String[] args, final BalanceType bt){
		
		plugin.threads.execute(new Runnable(){
			@SuppressWarnings("deprecation")
			public void run(){
				
				FinancialEntity toCheck;
				String checkName;
				
				if (args.length >= 1){
					try {
						toCheck = plugin.playerManager.getFinancialEntityAdd(args[0]);
						checkName = args[0];
					} catch (InterruptedException | ExecutionException	| TimeoutException e) {
						plugin.scheduleMessage(sender, prfx + " Problem during name lookup for " + args[0] + ". Try again later.");
						return;
					}
					
				} else if (! (sender instanceof Player)){
					plugin.scheduleMessage(sender, prfx + " Player argument required from console.");
					return;
				} else {
					toCheck = plugin.playerManager.getFinancialEntityAdd(((Player) sender).getUniqueId());
					checkName = sender.getName();
				}
				double balance = 0;
				String type = "";
				
				switch(bt){
				case ASSETS:
					balance = plugin.econ.getBalance(toCheck).balance;
					type = "Assets";
					break;
				case CASH:
					balance = plugin.econ.getCash(toCheck).balance;
					type = "Cash";
					break;
				case NET_WORTH:
					balance = plugin.econ.getNetWorth(toCheck).balance;
					type = "Net worth";
					break;
				
				}
				
				plugin.scheduleMessage(sender, String.format("%s %s of %s: %s", prfx, type, checkName, plugin.econ.format(balance)));
				
			}
		});
		
	}
	
	
	private void magicMoney(final CommandSender sender, final Command cmd, final String[] args){
		
		
		if(args.length < 2){
			Conf.messageCenter("too-few-arguments", null, null);
			return;
		}
		
		
		
		plugin.threads.execute(new Runnable(){
			
			@SuppressWarnings("deprecation")
			public void run(){
				
				EcoAction action = null;
				
				int entityIndex = 1;
				
				if(cmd.getName().equalsIgnoreCase("sl-pay")){
					action = EcoAction.ADD;
					entityIndex = 0;
				} else if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("pay")){
					action = EcoAction.ADD;
				} else if (args[0].equalsIgnoreCase("subtract") || args[0].equalsIgnoreCase("take")){
					action = EcoAction.SUBTRACT;
				} else if (args[0].equalsIgnoreCase("set"))
					action = EcoAction.SET;
				else {
					plugin.scheduleMessage(sender, prfx + " Unknown action.");
					return;
				}
				
				FinancialEntity target = null;
				
				try {
					target = plugin.playerManager.getFinancialEntityAdd(args[entityIndex]);
				} catch (InterruptedException | ExecutionException | TimeoutException e) {
					plugin.scheduleMessage(sender, Conf.messageCenter("name-lookup-fail", new String[]{"$$r", "$$p"},  new String[]{args[entityIndex], sender.getName()}));
					return;
				}
				
				if(target == null){
					plugin.scheduleMessage(sender, prfx + " Entity not found!");
					return;
				}
				
				if(args.length < entityIndex + 2){
					plugin.scheduleMessage(sender, Conf.messageCenter("too-few-arguments", null, null));
					return;
				}
				
				double amount = Double.parseDouble(args[entityIndex + 1]);
				
				EconResult result = null;
				
				switch(action){
				case ADD:
					result = plugin.econ.deposit(target, amount);
					break;
				case SET:
					double balance = plugin.econ.getBalance(target).balance;
					plugin.econ.withdraw(target, balance);
					result = plugin.econ.deposit(target, amount);
					break;
				case SUBTRACT:
					result = plugin.econ.withdraw(target, amount);
					break;
				
				}
				
				if(result.callSuccess){
					plugin.scheduleMessage(sender, Conf.messageCenter("generic-success", null, null));
				} else {
					plugin.scheduleMessage(sender, Conf.messageCenter("generic-refuse", null, null));
					plugin.scheduleMessage(sender, prfx + result.errMsg);
				}
				
			}
		});
			
		
	}
	

	private void pay(final CommandSender sender, final String[] args){
		
		
		if(args.length < 2){
			Conf.messageCenter("too-few-arguments", null, null);
			return;
		}
		
		
		
		plugin.threads.execute(new Runnable(){
			
			@SuppressWarnings("deprecation")
			public void run(){
				
				FinancialEntity source = plugin.playerManager.getFinancialEntity(((Player) sender).getUniqueId());
				FinancialEntity target = null;
				
				try {
					target = plugin.playerManager.getFinancialEntityAdd(args[0]);
				} catch (InterruptedException | ExecutionException | TimeoutException e) {
					plugin.scheduleMessage(sender, Conf.messageCenter("name-lookup-fail", new String[]{"$$r", "$$p"},  new String[]{args[0], sender.getName()}));
					return;
				}
				
				if(target == null){
					plugin.scheduleMessage(sender, prfx + " Entity not found!");
					return;
				}
				
				double amount = Double.parseDouble(args[1]);
				
				EconResult result = null;
				
				if(!plugin.econ.has(source, amount).callSuccess){
					plugin.scheduleMessage(sender, prfx + " You do not have enough money!");
					return;
				}
				
				plugin.econ.withdraw(source, amount);
				result = plugin.econ.deposit(target, amount);
					
				
				if(result.callSuccess){
					plugin.scheduleMessage(sender, Conf.messageCenter("generic-success", null, null));
				} else {
					plugin.scheduleMessage(sender, Conf.messageCenter("generic-refuse", null, null));
					plugin.scheduleMessage(sender, prfx + result.errMsg);
				}
				
			}
		});
			
		
	}
}
