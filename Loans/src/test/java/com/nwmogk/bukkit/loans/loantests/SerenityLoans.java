package com.nwmogk.bukkit.loans.loantests;

import java.util.logging.Level;
import java.util.logging.Logger;


import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.*;

public final class SerenityLoans extends JavaPlugin{
	
	Logger log;
	
	public void onEnable(){
		this.saveDefaultConfig();
		
		
		log = Logger.getLogger("Minecraft");


		log.log(Level.INFO, "Loans: Message from Loans");
		
		if(this.getConfig().getBoolean("require-admin-approve"))
			log.log(Level.INFO, "Audit style: " + this.getConfig().getString("audit-style"));
	
		
	}
	
	public void onDisable(){
		
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		
		if(cmd.getName().equalsIgnoreCase("garble")){
			log.log(Level.INFO, "Command /garble typed");
			return true;
		}
		
		return false;
		
	}

}
