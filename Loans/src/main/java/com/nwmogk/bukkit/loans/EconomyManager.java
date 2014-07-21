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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.plugin.RegisteredServiceProvider;

import com.nwmogk.bukkit.loans.api.EconResult;
import com.nwmogk.bukkit.loans.api.FinancialEntity;
import com.nwmogk.bukkit.loans.api.PlayerType;
import com.nwmogk.bukkit.loans.object.Loan;

public class EconomyManager {

	// TODO make thread safe + comments + handle different economies + update configuration fetches
	private enum EconType {VAULT, HYBRID, SERENE, INTERNAL};
	
	private SerenityLoans plugin;
	private Economy econ = null;
	
	private EconType config;
	
	public EconomyManager(SerenityLoans plug){
		plugin = plug;
		
		String setting = "hybrid";
		
		if(plugin.getConfig().contains("options.economy"))
			setting = plugin.getConfig().getString("options.economy");
		
		if(setting.equalsIgnoreCase("vault")){
			config = EconType.VAULT;
			setupVaultEconomy();
		} else if(setting.equalsIgnoreCase("hybrid"))
			config = EconType.HYBRID;
		else if(setting.equalsIgnoreCase("serenecon"))
			config = EconType.SERENE;
		else if(setting.equalsIgnoreCase("internal"))
			config = EconType.INTERNAL;
		else
			config = null;
		
		setupVaultEconomy();
	}
	
	public EconomyResponse convertEconResult(EconResult result){
		if(config != EconType.VAULT && config != EconType.HYBRID){
			return null;
		}
		
		return new EconomyResponse(result.amount, result.balance, result.callSuccess? EconomyResponse.ResponseType.SUCCESS : EconomyResponse.ResponseType.FAILURE, result.errMsg);
	}

	public String currencyNamePlural(){
		String result = "dollars";
		
		if(config.equals(EconType.VAULT) || config.equals(EconType.HYBRID))
			result = econ.currencyNamePlural();
		else if(plugin.getConfig().contains("economy.currency.currency-name.plural"))
			result = plugin.getConfig().getString("economy.currency.currency-name.plural");
	
		return result;
	}
	
	public String currencyNameSingular(){
		String result = "dollar";
		
		if(config.equals(EconType.VAULT) || config.equals(EconType.HYBRID))
			result = econ.currencyNameSingular();
		else if(plugin.getConfig().contains("economy.currency.currency-name.singular"))
			result = plugin.getConfig().getString("economy.currency.currency-name.singular");
	
		return result;
	}

	public String currencySymbol(){
		String result = "$";
		
		if(plugin.getConfig().contains("economy.currency.currency-symbol"))
			result = plugin.getConfig().getString("economy.currency.currency-symbol");
	
		return result;
	}
	
	public EconResult deposit(FinancialEntity entity, double amount){
		
		if(entity == null)
			return new EconResult(0, 0, false, "Entity is not recognized.");
		
		if(amount < 0)
			return new EconResult(0, 0, false, "Amount query is negative!");
		
		if(entity.getPlayerType().equals(PlayerType.CREDIT_UNION) && config.equals(EconType.VAULT))
			return new EconResult(0, 0, false, "CreditUnions are not supported by the economy.");
		
		if(config.equals(EconType.VAULT) || (config.equals(EconType.HYBRID) && entity.getPlayerType().equals(PlayerType.PLAYER)))
			return new EconResult(econ.depositPlayer(plugin.playerManager.getOfflinePlayer(entity.getUserID()), amount));
		
		return plugin.playerManager.depositCash(entity.getUserID(), amount);
		
	}

	@Deprecated
	public EconResult deposit(String name, double amount) throws InterruptedException, ExecutionException, TimeoutException{
		return deposit(plugin.playerManager.getFinancialEntity(name), amount);
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

	@Deprecated
	public EconResult getBalance(String name) throws InterruptedException, ExecutionException, TimeoutException{
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
	
	public EconResult getCash(FinancialEntity entity){
		// TODO differentiate between assets and cash
		return getBalance(entity);
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

	public EconResult getNetWorth(FinancialEntity entity){
		
		double netWorth = getCash(entity).balance;
		
		for(Loan loan : plugin.loanManager.getLoans(entity, false)){
			netWorth += loan.getCloseValue();
		}
		
		for(Loan loan : plugin.loanManager.getLoans(entity, true)){
			netWorth -= loan.getCloseValue();
		}
		
		return new EconResult(0, netWorth, true, null);
	}

	/**
	 * Returns an EconResult which contains the balance
	 * of the entity and the success of the amount query.
	 * Depends on the implementation of getBalance(FinancialEntity)
	 * to get the balance.
	 * 
	 * @param entity
	 * @param amount
	 * @return
	 */
	public EconResult has(FinancialEntity entity, double amount){
		boolean answer = true;
		
		if(amount < 0)
			return new EconResult(0, 0, false, "Amount query is negative!");
		
		EconResult result = getBalance(entity);
		
		if(! result.callSuccess)
			return result;
		
		double balance = result.balance;
		
		answer &= balance >= amount;
		
		
		return new EconResult(0, balance, answer, result.errMsg);
	}

	@Deprecated
	public EconResult has(String name, double amount) throws InterruptedException, ExecutionException, TimeoutException{
		return has(plugin.playerManager.getFinancialEntity(name), amount);
	}

	@Deprecated
	public boolean hasBalance(String financialEntity, double amount) throws InterruptedException, ExecutionException, TimeoutException{
		return has(financialEntity, amount).callSuccess;
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

	public EconResult recycleFunds(FinancialEntity toZero){
		double balance = getBalance(toZero).balance;
		withdraw(toZero, balance);
		deposit(plugin.playerManager.getFinancialInstitution("CentralBank"), balance);
		return new EconResult(balance, 0.0, true, null);
	}

	/**
	 * This method attempts to withdraw cash from a financial entity's 
	 * account. It will handle all of the various economy options available.
	 * Returns a EconResult, which has similar functionality to the Vault
	 * EconomyResponse.
	 * 
	 * @param entity
	 * @param amount
	 * @return
	 */
	public EconResult withdraw(FinancialEntity entity, double amount){
		
		if(entity == null)
			return new EconResult(0, 0, false, "Entity is not recognized.");
		
		if(amount < 0)
			return new EconResult(0, 0, false, "Amount query is negative!");
		
		if(entity.getPlayerType().equals(PlayerType.CREDIT_UNION) && config.equals(EconType.VAULT))
			return new EconResult(0, 0, false, "CreditUnions are not supported by the economy.");
		
		if(config.equals(EconType.VAULT) || (config.equals(EconType.HYBRID) && entity.getPlayerType().equals(PlayerType.PLAYER)))
			return new EconResult(econ.withdrawPlayer(plugin.playerManager.getOfflinePlayer(entity.getUserID()), amount));
		
		return plugin.playerManager.withdrawCash(entity.getUserID(), amount);
	}

	@Deprecated
	public EconResult withdraw(String name, double amount) throws InterruptedException, ExecutionException, TimeoutException{
		return withdraw(plugin.playerManager.getFinancialEntity(name), amount);
	}
	
	private void setupVaultEconomy() {
	       if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
	    	   plugin.getServer().getPluginManager().disablePlugin(plugin);
	           return;
	       }
	      
	       
	       RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
	      
	       if (rsp == null) {
	           plugin.getServer().getPluginManager().disablePlugin(plugin);
	           return;
	       }
	       econ = rsp.getProvider();
	       
	}
}
