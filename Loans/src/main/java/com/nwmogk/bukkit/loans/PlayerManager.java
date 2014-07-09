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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.google.common.io.Files;
import com.nwmogk.bukkit.evilmidget38.UUIDFetcher;
import com.nwmogk.bukkit.evilmidget38.NameFetcher;
import com.nwmogk.bukkit.loans.api.FinancialEntity;
import com.nwmogk.bukkit.loans.api.PlayerType;
import com.nwmogk.bukkit.loans.object.FinancialInstitution;
import com.nwmogk.bukkit.loans.object.FinancialPlayer;

public class PlayerManager {
	
	private SerenityLoans plugin;
	
	public PlayerManager(SerenityLoans plugin){
		this.plugin = plugin;
	}
	
	/**
	 * 
	 * 
	 * 
	 * @param entityName
	 * @return true if the entity is in the FinancialEntities table
	 * by the end of the method.
	 */
	public boolean addPlayer(UUID playerID) {
		return addPlayer(playerID, null);
	}

	public void addPlayers(Player[] players){
		
		String update = "INSERT INTO FinancialEntities (Name, Type, Cash, CreditScore) VALUES (?,?,?,?);";
		PreparedStatement stmt = null;
		
		try {
			stmt = plugin.getConnection().prepareStatement(update);
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		for(Player aPlayer : players){
			addPlayer(aPlayer.getUniqueId(), stmt);
		}
		
	}

	public boolean createFinancialInstitution(String desiredName, FinancialEntity manager){
		//TODO default value inputs
		
		return createFinancialInstitution(desiredName, manager, PlayerType.CREDIT_UNION, 0.0, 435);
	}

	public boolean createFinancialInstitution(String desiredName, FinancialEntity manager, PlayerType type, double initialCash, int crScore){
		//TODO Implement FinancialInstitution creation.
		
		// from SerenityLoans must remember to do this in this method.
		//playerManager.buildFinancialEntityInitialOffers("CentralBank");
		return false;
	}

	public UUID entityIdLookup(String entityName) throws InterruptedException, ExecutionException, TimeoutException {
		
		UUID result = getFinancialInstituteID(entityName);
		
		if(result != null)
			return result;
		
		Callable<Map<String,UUID>> fetcher = new UUIDFetcher(Arrays.asList(entityName));
		Future<Map<String,UUID>> answer = plugin.threads.submit(fetcher);
		
		//TODO configure timeout settings
		result = answer.get(10L, TimeUnit.SECONDS).get(entityName);
		
		return result;
	}

	public String entityNameLookup(UUID entityID) throws InterruptedException, ExecutionException, TimeoutException {
		
		FinancialInstitution bank = getFinancialInstitution(entityID);
		if(bank != null)
			return bank.getName();
		
		Player player = plugin.getServer().getPlayer(entityID);
		
		if(player != null)
			return player.getName();
		
		Callable<Map<UUID,String>> fetcher = new NameFetcher(Arrays.asList(entityID));
		Future<Map<UUID,String>> answer = plugin.threads.submit(fetcher);
		
		String result = answer.get(10L, TimeUnit.SECONDS).get(entityID);
		
		return result;
	}

	public String entityNameLookup(FinancialEntity entity) throws InterruptedException, ExecutionException, TimeoutException{
		if(entity instanceof FinancialInstitution)
			return ((FinancialInstitution) entity).getName();
		return entityNameLookup(entity.getUserID());
	}

	public FinancialEntity getFinancialEntity(UUID userID){
		return buildEntity(queryFinancialEntitiesTable(userID));
	}
	
	@Deprecated
	public FinancialEntity getFinancialEntity(String name) throws InterruptedException, ExecutionException, TimeoutException{
		return getFinancialEntity(entityIdLookup(name));
	}

	/**
	 * Reads the FinancialInstitutions table, and returns the UUID associated
	 * with the given name or null, if the name is not present. 
	 * 
	 * @param entityName
	 * @return
	 */
	public UUID getFinancialInstituteID(String entityName) {
		
		String query = "SELECT BankID from FinancialInstitutions WHERE Name=?;";
		String idString = null;
		UUID result = null;
		
		try {
			PreparedStatement stmt = plugin.getConnection().prepareStatement(query);
			
			stmt.setString(1, entityName);
			
			ResultSet candidates = stmt.executeQuery();
			
			if(!candidates.next()){
				
				if(SerenityLoans.debugLevel >=2){
					SerenityLoans.log.info(String.format("[%s] FinancialEntityID search for " + entityName + " failed. Institution not found.", plugin.getDescription().getName()));
				}
				
				return result;
				
			}
			
			idString = candidates.getString("BankID");
			
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		result = UUID.fromString(idString);
		
		if(SerenityLoans.debugLevel >=2){
			SerenityLoans.log.info(String.format("[%s] FinancialEntityID for " + entityName + "=" + result, plugin.getDescription().getName()));
		}

		return result;
	}
	
	public FinancialInstitution getFinancialInstitution(UUID bankId){
		return (FinancialInstitution) buildEntity(queryFinancialInstitutionsTable(bankId));
	}

	public Vector<UUID> getManagedEntities(UUID playerID){
		Vector<UUID> results = new Vector<UUID>();
		
		String query = "SELECT BankID FROM FinancialInstitutions WHERE Manager=?;";
		
		try {
			PreparedStatement stmt = plugin.conn.prepareStatement(query);
			
			stmt.setString(1, playerID.toString());
			
			ResultSet hits = stmt.executeQuery();
			
			while(hits.next()){
				results.add(UUID.fromString(hits.getString("BankID")));
			}
			
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
		
		}
		
		return results;
	}

	public OfflinePlayer getOfflinePlayer(UUID financialEntityID){
		FinancialEntity entity = getFinancialEntity(financialEntityID);
		
		if(entity == null)
			return null;
		
		UUID playerID = entity.getUserID();
		
		if(entity instanceof FinancialInstitution)
			playerID = ((FinancialInstitution)entity).getResponsibleParty();
		
		// TODO look for ways to improve
		
		OfflinePlayer[] allPlayers = plugin.getServer().getOfflinePlayers();
		
		OfflinePlayer result = null;
		
		for(OfflinePlayer op : allPlayers){
			if(op.getUniqueId().equals(playerID)){
				result = op;
				break;
			}
		}
		
		// Bukkit claims this is inefficient, but it would be
		// convenient.
		//return plugin.getServer().getOfflinePlayer(playerID);
		
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
	public Player getPlayer(UUID financialEntityID) {
		
		FinancialEntity entity = getFinancialEntity(financialEntityID);
		
		if(entity == null)
			return null;
		
		UUID playerID = entity.getUserID();
		
		if(entity instanceof FinancialInstitution)
			playerID = ((FinancialInstitution)entity).getResponsibleParty();
		
		return plugin.getServer().getPlayer(playerID);
		
	}
	
	public FinancialEntity getFinancialEntityAdd(UUID userID){
		
		FinancialEntity result = getFinancialEntity(userID);
		
		if(result == null){
			if(!plugin.playerManager.addPlayer(userID)){
						
				return null;
			}
			
			result = plugin.playerManager.getFinancialEntity(userID);
		}
		
		return result;
	}
	
	@Deprecated
	public FinancialEntity getFinancialEntityAdd(String name) throws InterruptedException, ExecutionException, TimeoutException{
		return getFinancialEntityAdd(entityIdLookup(name));
	}

	public boolean inFinancialEntitiesTable(UUID entityID){
		
		ResultSet result = queryFinancialEntitiesTable(entityID);
		boolean answer = false;
		
		try {
			answer = result.next();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return answer;
	}

	@Deprecated
	public boolean inFinancialEntitiesTable(String entityName) throws InterruptedException, ExecutionException, TimeoutException {
		return inFinancialEntitiesTable(entityIdLookup(entityName));
	}
	
	public boolean isIgnoring(UUID userId, UUID targetId){
		String ignoreQuery = "SELECT IgnoreOffers FROM Trust WHERE UserID=? AND TargetID=?;";
		
		try {
			PreparedStatement ps = plugin.conn.prepareStatement(ignoreQuery);
			
			ps.setString(1, userId.toString());
			ps.setString(2, targetId.toString());
			
			ResultSet ignoreResult = ps.executeQuery();
			
			if(ignoreResult.next() && Boolean.valueOf(ignoreResult.getString("IgnoreOffers"))){
				ps.close();
				return true;
			}
			else
				ps.close();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return false;
	}

	public boolean toggleIgnore(UUID playerId, UUID targetId){
		String querySQL = "SELECT IgnoreOffers FROM Trust WHERE UserID=? AND TargetID=?;";
		
		boolean setToIgnore = true;
		
		try {
			PreparedStatement ps = plugin.conn.prepareStatement(querySQL);
			
			ps.setString(1, playerId.toString());
			ps.setString(2, targetId.toString());
			
			ResultSet currentTrust = ps.executeQuery();
			
			String updateSQL;
			
			if(currentTrust.next()){
				setToIgnore = !Boolean.parseBoolean(currentTrust.getString("IgnoreOffers"));
				
				String ignoreString = setToIgnore? "'true'" : "'false'";
				updateSQL = String.format("UPDATE Trust SET IgnoreOffers=%s WHERE UserID=? AND TargetID=?;",  ignoreString);
				
			} else {
				updateSQL = "INSERT INTO Trust (UserID, TargetID, IgnoreOffers) VALUES (?, ?, 'true');";
			}
			
			PreparedStatement stmt = plugin.conn.prepareStatement(updateSQL);
			
			stmt.setString(1, playerId.toString());
			stmt.setString(2, targetId.toString());
			
			stmt.executeUpdate();
			
			ps.close();
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return setToIgnore;
		
	}

	private boolean addPlayer(UUID playerID, PreparedStatement statement){
		boolean white = false;
		boolean black = false;
		
		if(inFinancialEntitiesTable(playerID)){
			
			if(SerenityLoans.debugLevel >= 2)
				SerenityLoans.log.info(String.format("[%s] Player %s is already in system.", plugin.getDescription().getName(), playerID.toString()));
			
			
			return true;
		
		}
		
		Player player = plugin.getServer().getPlayer(playerID);
		if(player == null){
			if(SerenityLoans.debugLevel >= 2)
				SerenityLoans.log.info(String.format("[%s] Player %s is not online.", plugin.getDescription().getName(), playerID.toString()));
	
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
				nameFound |= playerID.toString().equals(candidate);
			}
			
			if(!(nameFound && white) && !(!nameFound && black))
				return false;
			
		}
		
		
		String update = "INSERT INTO FinancialEntities (UserID, Type, Cash, CreditScore) VALUES (?,?,?,?);";
		int rowsUpdated = 0;
		
		try {
			
			PreparedStatement stmt = statement == null? plugin.getConnection().prepareStatement(update) : statement;
			
			stmt.setString(1, playerID.toString());
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
				SerenityLoans.log.info(String.format("[%s] Adding player %s failed.", plugin.getDescription().getName(), playerID.toString()));
			
			return false;
		}
		
		buildFinancialEntityInitialOffers(playerID);
		
		if(SerenityLoans.debugLevel >= 2)
			SerenityLoans.log.info(String.format("[%s] Adding player %s succeeded.", plugin.getDescription().getName(), playerID.toString()));
		
		
		return true;
	}

	private FinancialEntity buildEntity(ResultSet successfulQuery){
		if(successfulQuery == null)
			return null;
		
		try {
			if(!successfulQuery.next())
				return null;
			
			String userString = successfulQuery.getString("UserID");
			PlayerType pt = PlayerType.getFromString(successfulQuery.getString("Type"));
			double cash = successfulQuery.getDouble("Cash");
			int crScore = successfulQuery.getInt("CreditScore");
			
			UUID userID = UUID.fromString(userString);
			
			if(pt.equals(PlayerType.PLAYER))
				return new FinancialPlayer(userID, pt, cash, crScore);
			
				
			ResultSet instituteQuery = queryFinancialInstitutionsTable(userID);
				
			if(instituteQuery == null || !instituteQuery.next())
				return null;
				
			String name = instituteQuery.getString("Name");
			String managerString = instituteQuery.getString("Manager");
			
			UUID managerID = UUID.fromString(managerString);
			
			return new FinancialInstitution(userID, name, pt, managerID, cash, crScore);
			
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return null;
	}

	private void buildFinancialEntityInitialOffers(UUID playerID) {
	
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
			
			// build two PreparedOffers
			
			PreparedStatement stmt1 = plugin.conn.prepareStatement(query1);
			
			stmt1.setString(1, playerID.toString());
			
			ResultSet existingOffers = stmt1.executeQuery();
			
			Set<String> searchSet = new HashSet<String>();
			
			while(existingOffers.next()){
				searchSet.add(existingOffers.getString("OfferName"));
			}
			
			PreparedStatement stmt2 = plugin.conn.prepareStatement(query2);
			
			stmt2.setString(1, playerID.toString());
			
			
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

	private ResultSet queryFinancialEntitiesTable(UUID userID){
		
		String entitySearch = "SELECT * from FinancialEntities WHERE UserID=?;";
		ResultSet result = null;
		
		try {
			PreparedStatement stmt = plugin.conn.prepareStatement(entitySearch);
			stmt.setString(1, userID.toString());
			result = stmt.executeQuery();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return result;
	}

	private ResultSet queryFinancialInstitutionsTable(UUID userID){
		
		String entitySearch = "SELECT * from FinancialInstitutions WHERE BankID=?;";
		ResultSet result = null;
		
		try {
			PreparedStatement stmt = plugin.conn.prepareStatement(entitySearch);
			stmt.setString(1, userID.toString());
			result = stmt.executeQuery();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return result;
	}

}
