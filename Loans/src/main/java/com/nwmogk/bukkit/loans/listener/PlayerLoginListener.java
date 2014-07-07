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
import java.util.Vector;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.nwmogk.bukkit.loans.Conf;
import com.nwmogk.bukkit.loans.SerenityLoans;
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
		
		
		if(!plugin.playerManager.addToFinancialEntitiesTable(evt.getPlayer().getName()))
			return;
		
		if(!evt.getPlayer().hasPermission("serenityloans.loan.borrow"))
			return;
		
		int playerID = plugin.playerManager.getFinancialEntityID(evt.getPlayer().getName());
		
		if(playerID == 0)
			return;
		
		Vector<Integer> toCheck = plugin.playerManager.getManagedEntities(playerID);
		
		if(toCheck.size() != 0)
			for(int i : toCheck)
				toCheck.add(i);
		
		toCheck.add(0, playerID);
		
		try{
			
			String offerSQL = "SELECT DISTINCT LenderID FROM Offers WHERE Sent='false' AND BorrowerID=?;";
			String nameSQL = "SELECT Name FROM FinancialEntities WHERE UserID=?;";
			String setTrue = "UPDATE Offers SET Sent='true' WHERE BorrowerID=? AND LenderID=?;";
			String psQuery = "SELECT DISTINCT LoanID FROM PaymentStatements WHERE BillAmountPaid < BillAmount;";
			
			PreparedStatement offerQuery = plugin.conn.prepareStatement(offerSQL);
			PreparedStatement nameQuery = plugin.conn.prepareStatement(nameSQL);
			PreparedStatement setUpdate = plugin.conn.prepareStatement(setTrue);
			Statement paymentStatements = plugin.conn.createStatement();
			
			ResultSet loansWithStatements = paymentStatements.executeQuery(psQuery);
			HashMap<Integer, Loan> loanSet = new HashMap<Integer, Loan>();
			
			while(loansWithStatements.next()){
				Loan theLoan = plugin.loanManager.getLoan(loansWithStatements.getInt("LoanID"));
				
				loanSet.put(theLoan.getBorrower().getUserID(), theLoan);
			}
			
			for(int i : toCheck){
				
				if(loanSet.containsKey(i)){
					Loan theLoan = loanSet.get(i);
					
					Player recipient = plugin.playerManager.getPlayer(i);
					
					if(recipient != null){
						
					
					boolean isPlayer = recipient.getName().equalsIgnoreCase(theLoan.getBorrower().getName());
				
					recipient.sendMessage(String.format("%s %s an outstanding payment statement!", prfx, isPlayer? "You have" : theLoan.getBorrower().getName() + " has"));
					recipient.sendMessage(String.format("%s Use %s to apply payment.", prfx, isPlayer? "/loan": "/crunion"));
					recipient.sendMessage(String.format("%s Details are given below:", prfx));
					recipient.sendMessage(plugin.loanManager.getPaymentStatement(theLoan.getLoanID()).toString(plugin));
					recipient.sendMessage(String.format("%s Use %s statement to view this statement again.", prfx, isPlayer? "/loan": "/crunion"));
					}
				}
				
				offerQuery.setInt(1, i);
				
				ResultSet offerResults = offerQuery.executeQuery();
				
				while(offerResults.next()){
					
					int offerer = offerResults.getInt("LenderID");
					
					nameQuery.setInt(1, offerer);
					
					ResultSet whoOffered = nameQuery.executeQuery();
					
					if(!whoOffered.next())
						continue;
					
					String sender = whoOffered.getString("Name");
					
					nameQuery.setInt(1, i);
					
					ResultSet whoAmI = nameQuery.executeQuery();
					
					if(!whoAmI.next())
						continue;
					
					String recipient = whoAmI.getString("Name");
					
					if(i == 0){
						evt.getPlayer().sendMessage(prfx + " You have a loan offer from " + sender + "!");
					} else {
						evt.getPlayer().sendMessage(prfx + " " + recipient + " has a loan offer from " + sender + "!");
					}
					
					setUpdate.setInt(1, i);
					setUpdate.setInt(2, offerer);
					
					setUpdate.executeUpdate();
				}
				
				if(i == 0)
					evt.getPlayer().sendMessage(prfx + " Type '/loan viewoffers' to view your offers.");
				
			}
			
			offerQuery.close();
			nameQuery.close();
			setUpdate.close();
			
		} catch(SQLException e){
			
		}
	}

}
