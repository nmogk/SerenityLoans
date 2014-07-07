/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * 
 * File: PlayerManager.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This class performs mySQL database operations on the FinancialEntities
 * table. It adds entries to the table, reads information from the table,
 * and checks if a particular entity is in the table. This class also
 * performs white-list and black-list checking of added players.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Vector;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.google.common.io.Files;
import com.nwmogk.bukkit.loans.object.FinancialEntity;

public class PlayerManager {
	
	private SerenityLoans plugin;
	
	public PlayerManager(SerenityLoans plugin){
		this.plugin = plugin;
	}
	
	public FinancialEntity getFinancialEntity(String name){
		
		String entityNameSearch = "SELECT * from FinancialEntities WHERE Name=?;";
		ResultSet result = null;
		
		try {
			PreparedStatement stmt = plugin.conn.prepareStatement(entityNameSearch);
			stmt.setString(1, name);
			result = stmt.executeQuery();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return buildEntity(result);
	}
	
	public FinancialEntity getFinancialEntity(int userID){
		
		String entityIDSearch = "SELECT * from FinancialEntities WHERE UserID="+ userID +";";
		ResultSet result = null;
		
		try {
			Statement stmt = plugin.conn.createStatement();
			result = stmt.executeQuery(entityIDSearch);
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return buildEntity(result);
	}
	
	private FinancialEntity buildEntity(ResultSet successfulQuery){
		if(successfulQuery == null)
			return null;
		
		try {
			if(!successfulQuery.next())
				return null;
			
			int userID = successfulQuery.getInt("UserID");
			String name = successfulQuery.getString("Name");
			PlayerType pt = PlayerType.getFromString(successfulQuery.getString("Type"));
			double cash = successfulQuery.getDouble("Cash");
			int crScore = successfulQuery.getInt("CreditScore");
			
			int manager = successfulQuery.getInt("Manager");
			manager = manager == 0? userID : manager;
			
			return new FinancialEntity(userID, name, pt, manager, cash, crScore);
			
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return null;
	}

	public boolean inFinancialEntitiesTable(String entityName) {
		String entityNameSearch = "SELECT Name from FinancialEntities WHERE Name=?;";
		boolean result = false;
		
		try {
			PreparedStatement stmt = plugin.getConnection().prepareStatement(entityNameSearch);
			
			stmt.setString(1, entityName);
			
			ResultSet answer = stmt.executeQuery();
			
			result = answer.next();

			stmt.close();
			
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * 
	 * 
	 * 
	 * @param entityName
	 * @return true if the entity is in the FinancialEntities table
	 * by the end of the method.
	 */
	public boolean addToFinancialEntitiesTable(String playerName) {
		return addToFinancialEntitiesTable(playerName, null);
	}
	
	public void addToFinancialEntitiesTable(Player[] playerNames){
		
		String update = "INSERT INTO FinancialEntities (Name, Type, Cash, CreditScore) VALUES (?,?,?,?);";
		PreparedStatement stmt = null;
		
		try {
			stmt = plugin.getConnection().prepareStatement(update);
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		for(Player aPlayer : playerNames){
			addToFinancialEntitiesTable(aPlayer.getName(), stmt);
		}
		
	}
	
	
	private boolean addToFinancialEntitiesTable(String playerName, PreparedStatement statement){
		boolean white = false;
		boolean black = false;
		
		if(inFinancialEntitiesTable(playerName)){
			
			if(SerenityLoans.debugLevel >= 2)
				SerenityLoans.log.info(String.format("[%s] Player %s is already in system.", plugin.getDescription().getName(), playerName));
			
			
			return true;
		
		}
		
		Player player = plugin.getServer().getPlayer(playerName);
		if(player == null){
			if(SerenityLoans.debugLevel >= 2)
				SerenityLoans.log.info(String.format("[%s] Player %s is not online.", plugin.getDescription().getName(), playerName));

			return false;
		}
		
		if(plugin.getConfig().contains("options.use-whitelist"))
			white = plugin.getConfig().getBoolean("options.use-whitelist");
		else if(plugin.getConfig().contains("options.use-blacklist"))
			black = plugin.getConfig().getBoolean("options.use-blacklist");
		
		if(white || black) {
			if(SerenityLoans.debugLevel >= 2)
				SerenityLoans.log.info(String.format("[%s] Using a white/black list.", plugin.getDescription().getName()));
			
			
			Charset charset = Charset.forName("US-ASCII");
			
			String fname = white? "whitelist":"blacklist";
			File list = new File(plugin.getDataFolder().getAbsolutePath() + fname + ".txt");
			
			LinkedList<String> names = new LinkedList<String>();
				
			try {
				
				if(!list.exists()){
					
					list.createNewFile();
					return false;
					
				} else {
				
					BufferedReader reader = Files.newReader(list, charset);
			
					while(true){
						String name = reader.readLine();
						
						if(name == null)
							break;
						
						names.add(name);
					}
				
				}
		
			} catch (FileNotFoundException e) {
				SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
				e.printStackTrace();
			} catch (IOException e) {
				SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
				e.printStackTrace();
			}
			
			boolean nameFound = false;
			
			for(String candidate : names) {
				nameFound |= playerName.equals(candidate);
			}
			
			if(!(nameFound && white) && !(!nameFound && black))
				return false;
			
		}
		
		
		String update = "INSERT INTO FinancialEntities (Name, Type, Cash, CreditScore) VALUES (?,?,?,?);";
		int rowsUpdated = 0;
		
		try {
			
			PreparedStatement stmt = statement == null? plugin.getConnection().prepareStatement(update) : statement;
			
			stmt.setString(1, playerName);
			stmt.setString(2, "Player");
			
			double cash = 100;
			int crScore = 465;
			
			if(plugin.getConfig().contains("economy.initial-money"))
				cash = plugin.getConfig().getDouble("economy.initial-money");
			if(plugin.getConfig().contains("trust.credit-score.no-history-score"))
				crScore = plugin.getConfig().getInt("trust.credit-score.no-history-score");
			
			stmt.setDouble(3, cash);
			stmt.setInt(4, crScore);
			
			rowsUpdated = stmt.executeUpdate();
			
			stmt.close();
			
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			
		}
		
		if(rowsUpdated != 1){
			if(SerenityLoans.debugLevel >= 2)
				SerenityLoans.log.info(String.format("[%s] Adding player %s failed.", plugin.getDescription().getName(), playerName));
			
			return false;
		}
		
		buildFinancialEntityInitialOffers(playerName);
		
		if(SerenityLoans.debugLevel >= 2)
			SerenityLoans.log.info(String.format("[%s] Adding player %s succeeded.", plugin.getDescription().getName(), playerName));
		
		
		return true;
	}

	public void buildFinancialEntityInitialOffers(String playerName) {

		// Collect config info
		double value = 100;
		double interestRate = 0.05;
		
		double minPayment = 15;
		double lateFee = 0;
		double serviceFee = 0;
		
		long term = 2419200000L;
		long compoundingPeriod = 604800000L;
		long paymentTime = 604800000L;
		long paymentFrequency = 2419200000L;
		long gracePeriod  = 0L;
		long serviceFeeFrequency =  0L;
		String loanType = "'Bullet'";
		
		FileConfiguration config = plugin.getConfig();
		
		String valuePath = 		"loan.terms-constraints.principal-value.default";
		String interestPath = 	"loan.terms-constraints.interest.default";
		String compoundPath = 	"loan.terms-constraints.interest.compounding.default";
		String minPath = 		"loan.terms-constraints.min-payment.default";
		String termPath = 		"loan.terms-constraints.term.default";
		String paytimePath = 	"loan.terms-constraints.payment-time.default";
		String payfreqPath = 	"loan.terms-constraints.payment-frequency.default";
		String latePath = 		"loan.terms-constraints.fees.late-fee.default";
		String gracePath = 		"loan.terms-constraints.fees.grace-period.default";
		String servicePath = 	"loan.terms-constraints.fees.service-fee.default";
		String servfreqPath = 	"loan.terms-constraints.fees.service-fee-frequency.default";

		if(config.contains(valuePath) && config.isDouble(valuePath))
			value = Math.max(0, config.getDouble(valuePath));

		if(config.contains(interestPath) && config.isDouble(interestPath))
			interestRate = Math.max(0, config.getDouble(interestPath));
		
		if(config.contains(minPath) && config.isDouble(minPath))
			minPayment = Math.max(0, config.getDouble(minPath));
		
		if(config.contains(latePath) && config.isDouble(latePath))
			lateFee = Math.max(0, config.getDouble(latePath));
		
		if(config.contains(servicePath) && config.isDouble(servicePath))
			serviceFee = Math.max(0, config.getDouble(servicePath));
		
		if(config.contains(termPath) && config.isString(termPath)){
			long temp = Conf.parseTime(config.getString(termPath));
			term = temp == 0? term : temp;
		}
		
		if(config.contains(compoundPath) && config.isString(compoundPath)){
			long temp = Conf.parseTime(config.getString(compoundPath));
			compoundingPeriod = temp == 0? compoundingPeriod : temp;
		}
		
		if(config.contains(paytimePath) && config.isString(paytimePath)){
			long temp = Conf.parseTime(config.getString(paytimePath));
			paymentTime = temp == 0? paymentTime : temp;
		}
		
		if(config.contains(payfreqPath) && config.isString(payfreqPath)){
			long temp = Conf.parseTime(config.getString(payfreqPath));
			paymentFrequency = temp == 0? paymentFrequency : temp;
		}
		
		if(config.contains(gracePath) && config.isString(gracePath))
			gracePeriod = Conf.parseTime(config.getString(gracePath));
		
		if(config.contains(servfreqPath) && config.isString(servfreqPath))
			serviceFeeFrequency = Conf.parseTime(config.getString(servfreqPath));
		
		String columns = "LenderID, OfferName, Value, InterestRate, Term, CompoundingPeriod, GracePeriod, PaymentTime, PaymentFrequency, LateFee, MinPayment, ServiceFeeFrequency, ServiceFee, LoanType";
		
		String query1 = "SELECT OfferName FROM PreparedOffers WHERE LenderID=?";
		String query2 = "INSERT INTO PreparedOffers (" + columns + ") VALUES (?, ?, " + value + ", " + interestRate + ", " + term + ", " + compoundingPeriod + ", " + gracePeriod + ", " + paymentTime + ", " + paymentFrequency + ", " + lateFee + ", " + minPayment + ", " + serviceFeeFrequency + ", " + serviceFee + ", " + loanType + ");";
		
		try {
			
			// Collect info from FinancialEntities table
			
			
			int lenderID = getFinancialEntityID(playerName);
			
			if(lenderID == 0){
				return;
			}
			
			// build two PreparedOffers
			
			PreparedStatement stmt1 = plugin.conn.prepareStatement(query1);
			
			stmt1.setInt(1, lenderID);
			
			ResultSet existingOffers = stmt1.executeQuery();
			
			Set<String> searchSet = new HashSet<String>();
			
			while(existingOffers.next()){
				searchSet.add(existingOffers.getString("OfferName"));
			}
			
			PreparedStatement stmt2 = plugin.conn.prepareStatement(query2);
			
			stmt2.setInt(1, lenderID);
			
			
			if(searchSet.size() == 0 || !searchSet.contains("default")){
			
			stmt2.setString(2, "default");
			stmt2.executeUpdate();
			}
			
			if(searchSet.size() == 0 || !searchSet.contains("prepared")){
			stmt2.setString(2, "prepared");
			
			stmt2.executeUpdate();
			}
			
			stmt1.close();
			stmt2.close();
			
			
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}

				
	}

	/**
	 * Reads the FinancialEntities table, and returns the UserID associated
	 * with the given name or 0, if the name is not present. 
	 * 
	 * @param entityName
	 * @return
	 */
	public int getFinancialEntityID(String entityName) {
		
		String query = "SELECT UserID from FinancialEntities WHERE Name=?;";
		int result = 0;
		
		try {
			PreparedStatement stmt = plugin.getConnection().prepareStatement(query);
			
			stmt.setString(1, entityName);
			
			ResultSet candidates = stmt.executeQuery();
			
			if(!candidates.next()){
				
				if(SerenityLoans.debugLevel >=2){
					SerenityLoans.log.info(String.format("[%s] FinancialEntityID for " + entityName + " failed. Entity not found.", plugin.getDescription().getName()));
				}
				
				return result;
				
			}
			
			result = candidates.getInt("UserID");
			
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		if(SerenityLoans.debugLevel >=2){
			SerenityLoans.log.info(String.format("[%s] FinancialEntityID for " + entityName + "=" + result, plugin.getDescription().getName()));
		}

		return result;
	}

	/**
	 * This method returns a player object, given a financialEntityID.
	 * This method ensures that either a real player is given or the
	 * player who is the manager of a credit union if the given ID is
	 * a credit union.
	 * 
	 * If the given ID is not in the FinancialEntities table or the player
	 * is offline, this method returns null.
	 * 
	 * @param financialEntityID
	 * @return
	 */
	public Player getPlayer(int financialEntityID) {
		if(financialEntityID <= 0)
			return null;
		
		// This is injection safe since the input is guaranteed to be an int.
		String queryString = "SELECT Manager from FinancialEntities WHERE UserID="+ financialEntityID +";";
		int newID = financialEntityID;
		String userName = "";
		
		try {
			Statement stmt = plugin.getConnection().createStatement();
			
			ResultSet validResults = stmt.executeQuery(queryString);
			
			if(!validResults.next()){
				stmt.close();
				return null;
			}
			
			int potentialManager = validResults.getInt("Manager");
			newID = potentialManager + (1-Integer.signum(potentialManager)) * financialEntityID;
			
			String otherQueryString = "SELECT Name from FinancialEntities WHERE UserID=" + newID + ";";
			
			validResults = stmt.executeQuery(otherQueryString);
			
			if(!validResults.next())
				return null;
			
			userName = validResults.getString("Name");
			
			stmt.close();
			
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		
		
		return plugin.getServer().getPlayer(userName);
	}
	
	public Vector<Integer> getManagedEntities(int playerID){
		Vector<Integer> results = new Vector<Integer>();
		
		String query = "SELECT UserID FROM FinancialEntities WHERE Manager=?;";
		
		try {
			PreparedStatement stmt = plugin.conn.prepareStatement(query);
			
			stmt.setInt(1, playerID);
			
			ResultSet hits = stmt.executeQuery();
			
			while(hits.next()){
				results.add(hits.getInt("UserID"));
			}
			
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
		
		}
		
		return results;
	}
	
	public FinancialEntity getFinancialEntityRetryOnce(String name){
		
		FinancialEntity result = getFinancialEntity(name);
		
		if(result == null){
			if(!plugin.playerManager.addToFinancialEntitiesTable(name)){
						
				return null;
			}
			
			result = plugin.playerManager.getFinancialEntity(name);
		}
		
		return result;
	}

}
