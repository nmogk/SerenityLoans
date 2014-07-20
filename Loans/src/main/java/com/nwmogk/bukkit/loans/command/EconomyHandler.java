package com.nwmogk.bukkit.loans.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.nwmogk.bukkit.loans.Conf;
import com.nwmogk.bukkit.loans.SerenityLoans;

public class EconomyHandler  implements CommandExecutor {
	
	private SerenityLoans plugin;
	private String prfx;
	
	public EconomyHandler(SerenityLoans plugin){
		prfx = Conf.getMessageString();
		this.plugin = plugin;
	}

	public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		
		

		return false;
	}
	
	

}
