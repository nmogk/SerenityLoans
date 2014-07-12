/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * 
 * File: PlayerLoginListener.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This class performs operations arising from a player logging onto the
 * server, including adding the player to the plugin system and checking if
 * the player has outstanding offers or new payments due.
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

package com.nwmogk.bukkit.loans.listener;

import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.nwmogk.bukkit.loans.Conf;
import com.nwmogk.bukkit.loans.SerenityLoans;
import com.nwmogk.bukkit.loans.api.FinancialEntity;
import com.nwmogk.bukkit.loans.object.FinancialInstitution;
import com.nwmogk.bukkit.loans.object.Loan;

public final class PlayerLoginListener implements Listener {
	
	private SerenityLoans plugin;
	private String prfx;
	
	public PlayerLoginListener(SerenityLoans plugin){
		this.plugin = plugin;
		prfx = Conf.getMessageString();
	}
	
	
	@EventHandler
	public void onLogin(final PlayerJoinEvent evt){
		
		if(SerenityLoans.debugLevel >= 2)
			SerenityLoans.log.info(String.format("[%s] Attempting to add player %s to system.", plugin.getDescription().getName(), evt.getPlayer().getName()));
		
		
		if(!plugin.playerManager.addPlayer(evt.getPlayer().getUniqueId()))
			return;
		
		if(!evt.getPlayer().hasPermission("serenityloans.loan.borrow"))
			return;
		
		
		plugin.threads.execute(new Runnable(){
			
			public void run(){
				
				UUID playerID = evt.getPlayer().getUniqueId();
				
				
				Vector<UUID> toCheck = plugin.playerManager.getManagedEntities(playerID);
				
				if(toCheck.size() != 0)
					for(UUID i : toCheck)
						toCheck.add(i);
				
				toCheck.add(0, playerID);
				
				
					
				boolean firstRun = true;
				
				for(UUID i : toCheck){
					
					List<Loan> loanSet = plugin.loanManager.getLoansWithOutstandingStatements(i);
					FinancialEntity currentEntity = plugin.playerManager.getFinancialEntity(i);
					Player recipient = evt.getPlayer();
					
					
					if(loanSet != null){
						
						plugin.scheduleMessage(recipient, String.format("%s %s an outstanding payment statement!", prfx, firstRun? "You have" : ((FinancialInstitution)currentEntity).getName() + " has"));
						plugin.scheduleMessage(recipient, String.format("%s Use %s to apply payment.", prfx, firstRun? "/loan": "/crunion"));
						plugin.scheduleMessage(recipient, String.format("%s Details are given below:", prfx));
						
						
						for(Loan theLoan : loanSet){

							
							if(recipient == null)
								continue;

							plugin.scheduleMessage(recipient, plugin.loanManager.getPaymentStatement(theLoan.getLoanID()).toString(plugin));
							plugin.scheduleMessage(recipient, String.format("%s Use %s statement to view this statement again.", prfx, firstRun? "/loan": "/crunion"));
							
						}
					}
					
					List<FinancialEntity> offerResults = plugin.offerManager.getOfferSendersTo(i, true);
					
					for(FinancialEntity fe : offerResults){
						
						String sender = null;
						try {
							sender = plugin.playerManager.entityNameLookup(fe);
						} catch (InterruptedException | ExecutionException | TimeoutException e) {
							// TODO add message to configuration
							plugin.scheduleMessage(recipient, prfx + " Problem during name lookup. Try again later.");
							continue;
						}
						
						if(sender == null)
							continue;
						
						if(firstRun){
							plugin.scheduleMessage(recipient, prfx + " You have a loan offer from " + sender + "!");
							plugin.scheduleMessage(recipient, prfx + " Type '/loan viewoffers' to view your offers.");
						} else {
							String recipientS = ((FinancialInstitution)currentEntity).getName();
							plugin.scheduleMessage(recipient, prfx + " " + recipientS + " has a loan offer from " + sender + "!");
							plugin.scheduleMessage(recipient, prfx + " Type '/crunion viewoffers' to view your offers.");
						}
						
						plugin.offerManager.registerOfferSend(fe.getUserID(), i);
						
						
					}
					
					firstRun = false;
				}
				
			}
			
		});
		
		
			
			
		
	}

}
