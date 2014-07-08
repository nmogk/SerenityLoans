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

import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;
import java.util.Vector;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.nwmogk.bukkit.loans.Conf;
import com.nwmogk.bukkit.loans.SerenityLoans;
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
	public void onLogin(PlayerJoinEvent evt){
		
		if(SerenityLoans.debugLevel >= 2)
			SerenityLoans.log.info(String.format("[%s] Attempting to add player %s to system.", plugin.getDescription().getName(), evt.getPlayer().getName()));
		
		
		if(!plugin.playerManager.addToFinancialEntitiesTable(evt.getPlayer().getUniqueId()))
			return;
		
		if(!evt.getPlayer().hasPermission("serenityloans.loan.borrow"))
			return;
		
		UUID playerID = evt.getPlayer().getUniqueId();
		
		Vector<UUID> toCheck = plugin.playerManager.getManagedEntities(playerID);
		
		if(toCheck.size() != 0)
			for(UUID i : toCheck)
				toCheck.add(i);
		
		toCheck.add(0, playerID);
		
		try{
			
			String offerSQL = "SELECT DISTINCT LenderID FROM Offers WHERE Sent='false' AND BorrowerID=?;";
			//String nameSQL = "SELECT Name FROM FinancialEntities WHERE UserID=?;";
			String setTrue = "UPDATE Offers SET Sent='true' WHERE BorrowerID=? AND LenderID=?;";
			String psQuery = "SELECT DISTINCT LoanID FROM PaymentStatements WHERE BillAmountPaid < BillAmount;";
			
			PreparedStatement offerQuery = plugin.conn.prepareStatement(offerSQL);
			//PreparedStatement nameQuery = plugin.conn.prepareStatement(nameSQL);
			PreparedStatement setUpdate = plugin.conn.prepareStatement(setTrue);
			Statement paymentStatements = plugin.conn.createStatement();
			
			ResultSet loansWithStatements = paymentStatements.executeQuery(psQuery);
			HashMap<UUID, Loan> loanSet = new HashMap<UUID, Loan>();
			
			while(loansWithStatements.next()){
				Loan theLoan = plugin.loanManager.getLoan(loansWithStatements.getInt("LoanID"));
				
				loanSet.put(theLoan.getBorrower().getUserID(), theLoan);
			}
			
			boolean firstRun = true;
			
			for(UUID i : toCheck){
				
				if(loanSet.containsKey(i)){
					Loan theLoan = loanSet.get(i);
					
					boolean isPlayer = i.equals(theLoan.getBorrower().getUserID());
					
					Player recipient = plugin.playerManager.getPlayer(i);
					
					if(recipient != null){
						
					
					
					recipient.sendMessage(String.format("%s %s an outstanding payment statement!", prfx, isPlayer? "You have" : ((FinancialInstitution)theLoan.getBorrower()).getName() + " has"));
					recipient.sendMessage(String.format("%s Use %s to apply payment.", prfx, isPlayer? "/loan": "/crunion"));
					recipient.sendMessage(String.format("%s Details are given below:", prfx));
					recipient.sendMessage(plugin.loanManager.getPaymentStatement(theLoan.getLoanID()).toString(plugin));
					recipient.sendMessage(String.format("%s Use %s statement to view this statement again.", prfx, isPlayer? "/loan": "/crunion"));
					}
				}
				
				offerQuery.setString(1, i.toString());
				
				ResultSet offerResults = offerQuery.executeQuery();
				
				while(offerResults.next()){
					
					UUID offerer = UUID.fromString(offerResults.getString("LenderID"));
					
					String sender = plugin.playerManager.entityNameLookup(plugin.playerManager.getFinancialEntity(offerer));
					
					
					if(firstRun){
						evt.getPlayer().sendMessage(prfx + " You have a loan offer from " + sender + "!");
					} else {
						String recipient = ((FinancialInstitution)plugin.playerManager.getFinancialEntity(i)).getName();
						evt.getPlayer().sendMessage(prfx + " " + recipient + " has a loan offer from " + sender + "!");
					}
					
					setUpdate.setString(1, i.toString());
					setUpdate.setString(2, offerer.toString());
					
					setUpdate.executeUpdate();
				}
				
//				if(i == 0)
//					evt.getPlayer().sendMessage(prfx + " Type '/loan viewoffers' to view your offers.");
				
				firstRun = false;
			}
			
			offerQuery.close();
			setUpdate.close();
			
		} catch(SQLException e){
			
		}
	}

}
