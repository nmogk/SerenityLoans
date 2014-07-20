package com.nwmogk.bukkit.loans.command;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nwmogk.bukkit.loans.Conf;
import com.nwmogk.bukkit.loans.SerenityLoans;
import com.nwmogk.bukkit.loans.api.FinancialEntity;

public class EconomyHandler  implements CommandExecutor {
	
	private SerenityLoans plugin;
	private String prfx;
	
	private enum BalanceType{CASH, ASSETS, NET_WORTH};
	
	public EconomyHandler(SerenityLoans plugin){
		prfx = Conf.getMessageString();
		this.plugin = plugin;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
		
		boolean payPerms = sender.hasPermission("serenityloans.pay");
		boolean balancePerms = sender.hasPermission("serenityloans.balance");
		boolean ecoPerms = sender.hasPermission("serenityloans.admin");
		
		if(cmd.getName().equalsIgnoreCase("sl-pay") && payPerms){
			
			
			
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
					plugin.scheduleMessage(sender, prfx + "Player argument required from console.");
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
	
	

}
