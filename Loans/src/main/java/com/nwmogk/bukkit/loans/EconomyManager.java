/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * 
 * File: EconomyManager.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This class accepts all economy-related calls from other classes in the 
 * SerenityLoans plugin and performs the operation on the proper economy
 * implementation, which is defined in the configuration. It provides most
 * of the functionality of a Vault economy with additional features, such
 * as currency symbols and percentage formatting.
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

package com.nwmogk.bukkit.loans;

import java.sql.SQLException;
import java.sql.Statement;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.plugin.RegisteredServiceProvider;

import com.nwmogk.bukkit.loans.api.EconResult;
import com.nwmogk.bukkit.loans.api.FinancialEntity;
import com.nwmogk.bukkit.loans.api.PlayerType;

public class EconomyManager {

	private enum EconType {VAULT, HYBRID, SERENE, INTERNAL};
	
	private SerenityLoans plugin;
	private Economy econ = null;
	
	private EconType config;
	
	public EconomyManager(SerenityLoans plug){
		plugin = plug;
		
		String setting = "hybrid";
		
		if(plugin.getConfig().contains("options.economy"))
			setting = plugin.getConfig().getString("options.economy");
		
		if(setting.equalsIgnoreCase("vault"))
			config = EconType.VAULT;
		else if(setting.equalsIgnoreCase("hybrid"))
			config = EconType.HYBRID;
		else if(setting.equalsIgnoreCase("serenecon"))
			config = EconType.SERENE;
		else if(setting.equalsIgnoreCase("internal"))
			config = EconType.INTERNAL;
		else
			config = null;
		
		setupVaultEconomy();
	}
	
	public boolean isInitialized(){
		if(config == null)
			return false;
		
		switch(config){
			
		case VAULT:
		case HYBRID:
			return econ!= null;
		case SERENE:
			return false;
		case INTERNAL:	
			return true;
			
		default:
				return false;
		}
		
	}
	
	private void setupVaultEconomy() {
	       if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) 
	           return;
	       
	       RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
	      
	       if (rsp == null) 
	           return;
	       
	       econ = rsp.getProvider();
	       
	}
	

	public String currencyNameSingular(){
		String result = "dollar";
		
		if(config.equals(EconType.VAULT) || config.equals(EconType.HYBRID))
			result = econ.currencyNameSingular();
		else if(plugin.getConfig().contains("economy.currency.currency-name.singular"))
			result = plugin.getConfig().getString("economy.currency.currency-name.singular");
	
		return result;
	}
	
	public String currencyNamePlural(){
		String result = "dollars";
		
		if(config.equals(EconType.VAULT) || config.equals(EconType.HYBRID))
			result = econ.currencyNamePlural();
		else if(plugin.getConfig().contains("economy.currency.currency-name.plural"))
			result = plugin.getConfig().getString("economy.currency.currency-name.plural");
	
		return result;
	}
	
	public String currencySymbol(){
		String result = "$";
		
		if(plugin.getConfig().contains("economy.currency.currency-symbol"))
			result = plugin.getConfig().getString("economy.currency.currency-symbol");
	
		return result;
	}
	
	public EconResult has(String name, double amount){
		boolean answer = true;
		
		if(amount < 0)
			return new EconResult(0, 0, false, "Amount query is negative!");
		
		EconResult result = getBalance(name);
		
		double balance = result.balance;
		
		answer &= balance >= amount;
		
		
		return new EconResult(0, balance, answer, result.errMsg);
	}
	
	public EconResult has(FinancialEntity entity, double amount){
		boolean answer = true;
		
		if(amount < 0)
			return new EconResult(0, 0, false, "Amount query is negative!");
		
		EconResult result = getBalance(entity);
		
		double balance = result.balance;
		
		answer &= balance >= amount;
		
		
		return new EconResult(0, balance, answer, result.errMsg);
	}
	
	public boolean hasBalance(String financialEntity, double amount){
		return has(financialEntity, amount).callSuccess;
	}
	
	
	public EconResult getBalance(String name){
		return getBalance(plugin.playerManager.getFinancialEntity(name));
	}
		
	public EconResult getBalance(FinancialEntity entity){
		
		if(entity == null)
			return new EconResult(0, 0, false, "Entity is not recognized.");
		
		
		boolean answer = true;
		String message = null;
		double balance = 0;
		
		if(entity.getPlayerType().equals(PlayerType.CREDIT_UNION) && config.equals(EconType.VAULT))
			return new EconResult(0, 0, false, "CreditUnions are not supported by the economy.");
		
		if(entity.getPlayerType().equals(PlayerType.CREDIT_UNION)){
			
			balance = entity.getCash();
			
		} else if(entity.getPlayerType().equals(PlayerType.PLAYER)){
			
			if(config.equals(EconType.VAULT) || config.equals(EconType.HYBRID)) 
				balance = econ.getBalance(plugin.playerManager.getOfflinePlayer(entity.getUserID()));
			else
				balance = entity.getCash();
		}
		
		
		return new EconResult(0, balance, answer, message);
	}
	
	public String format(double amount){
		
		boolean useSym = true;
		
		if(plugin.getConfig().contains("economy.currency.prefer-symbol"))
			useSym = plugin.getConfig().getBoolean("economy.currency.prefer-symbol");
		
		String symbol = useSym? currencySymbol() : "";
		String dollars = useSym? "" : " ";
		
		if(!useSym)
			dollars += amount == 1? currencyNameSingular() : currencyNamePlural();
			
		int decimals = 2;
		
		if(plugin.getConfig().contains("economy.currency.fractional-digits"))
			decimals = plugin.getConfig().getInt("economy.currency.fractional-digits");
				
		return String.format("%s%#(,." + decimals + "f%s", symbol, amount, dollars);
	}
	
	public String formatPercent(double value){
		return String.format("%#.3f%%", value * 100);
	}

	public EconResult deposit(String name, double amount){
		return deposit(plugin.playerManager.getFinancialEntity(name), amount);
	}
	
	public EconResult deposit(FinancialEntity entity, double amount){
		
		if(entity == null)
			return new EconResult(0, 0, false, "Entity is not recognized.");
		
		if(amount < 0)
			return new EconResult(0, 0, false, "Amount query is negative!");
		
		
		if(entity.getPlayerType().equals(PlayerType.CREDIT_UNION) && config.equals(EconType.VAULT))
			return new EconResult(0, 0, false, "CreditUnions are not supported by the economy.");
		
		if(config.equals(EconType.VAULT) || config.equals(EconType.HYBRID))
			return new EconResult(econ.depositPlayer(plugin.playerManager.getOfflinePlayer(entity.getUserID()), amount));
		
		String updateSQL = String.format("UPDATE FinancialEntities SET Cash=%f WHERE UserID=%d", Math.max(entity.getCash(), 0.0) + amount, entity.getUserID());
		
		int result = 0;
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			result = stmt.executeUpdate(updateSQL);
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		if(result == 1)
			return new EconResult(amount, Math.max(entity.getCash(),  0.0) + amount, true, null);
		else
			return new EconResult(0, entity.getCash(), false, "Upate not completed!");
	}
	
	public EconResult withdraw(String name, double amount){
		return withdraw(plugin.playerManager.getFinancialEntity(name), amount);
	}
	
	public EconResult withdraw(FinancialEntity entity, double amount){
		
		if(entity == null)
			return new EconResult(0, 0, false, "Entity is not recognized.");
		
		if(amount < 0)
			return new EconResult(0, 0, false, "Amount query is negative!");
		
		
		if(entity.getPlayerType().equals(PlayerType.CREDIT_UNION) && config.equals(EconType.VAULT))
			return new EconResult(0, 0, false, "CreditUnions are not supported by the economy.");
		
		if(config.equals(EconType.VAULT) || config.equals(EconType.HYBRID))
			return new EconResult(econ.withdrawPlayer(plugin.playerManager.getOfflinePlayer(entity.getUserID()), amount));
		
		if(amount > entity.getCash())
			return new EconResult(0, entity.getCash(), false, "Entity does not have sufficient funds.");
		
		String updateSQL = String.format("UPDATE FinancialEntities SET Cash=%f WHERE UserID=%d", Math.max(entity.getCash() - amount, 0.0), entity.getUserID());
		
		int result = 0;
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			result = stmt.executeUpdate(updateSQL);
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		if(result == 1)
			return new EconResult(amount, Math.max(entity.getCash() - amount,  0.0), true, null);
		else
			return new EconResult(0, entity.getCash(), false, "Upate not completed!");
	}
	
	public String getName(){
		switch(config){
		case VAULT:
		case HYBRID:
			return econ.getName();
		case SERENE:
			return "Serenecon";
		case INTERNAL:
			return "SerenityLoans";
		}
		
		return "";
	}
}
