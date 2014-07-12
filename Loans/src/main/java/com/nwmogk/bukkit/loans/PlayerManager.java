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
 * This class also performs database calls on the FinancialInstitutions,
 * Memberships, CreditHistory, and Trust tables. All methods of this class
 * are thread-safe.
 * 
 * This class handles name and uuid lookups from the Mojang account service
 * using evilmidget38's fetchers. These methods may block, so should not
 * be run from the main thread. In addition, these methods rely on the
 * caller to handle potential exceptions.
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.nwmogk.bukkit.evilmidget38.UUIDFetcher;
import com.nwmogk.bukkit.evilmidget38.NameFetcher;
import com.nwmogk.bukkit.loans.api.FinancialEntity;
import com.nwmogk.bukkit.loans.api.PlayerType;
import com.nwmogk.bukkit.loans.object.FinancialInstitution;
import com.nwmogk.bukkit.loans.object.FinancialPlayer;

public class PlayerManager {
	
	private SerenityLoans plugin;
	
	// Each table that this class manages has a lock object.
	private final Object financialEntitiesLock = new Object();
	private final Object financialInstitutionsLock = new Object();
	@SuppressWarnings("unused")
	private final Object membershipsLock = new Object();
	private final Object trustLock = new Object();
	@SuppressWarnings("unused")
	private final Object creditHistoryLock = new Object();
	
	/**
	 * Creates a PlayerManager object with the specified plugin reference.
	 * 
	 * @param plugin SerenityLoans object with which this PlayerManager is associated.
	 */
	public PlayerManager(SerenityLoans plugin){
		this.plugin = plugin;
	}
	
	
	
	/**
	 * 
	 * Attempts to add a Player corresponding to the given UUID to the 
	 * FinancialEntities table. Will fail if given a UUID that does not
	 * represent an online Player unless the UUID is already present
	 * in the table. In this case, the method reports that the entity
	 * exists. 
	 * 
	 * @param playerID UUID of the player to be added.
	 * @return true if the entity is in the FinancialEntities table
	 * by the end of the method, false otherwise.
	 */
	public boolean addPlayer(UUID playerID){
		// Check to see if the entity is already in the table
		if(inFinancialEntitiesTable(playerID)){
			
			if(SerenityLoans.debugLevel >= 2)
				SerenityLoans.log.info(String.format("[%s] Player %s is already in system.", plugin.getDescription().getName(), playerID.toString()));

			return true;
		
		}
		
		// Check to ensure that the given ID represents an online player.
		Player player = plugin.getServer().getPlayer(playerID);
		if(player == null){
			if(SerenityLoans.debugLevel >= 2)
				SerenityLoans.log.info(String.format("[%s] Player %s is not online.", plugin.getDescription().getName(), playerID.toString()));
	
			return false;
		}
		
		return addPlayers(new Player[]{player});
		
	}


	/**
	 * 
	 * Attempts to add all Players given to the 
	 * FinancialEntities table. 
	 * 
	 * @param players Array of the players to be added.
	 * @return true if all players were added successfully, false otherwise.
	 */
	public boolean addPlayers(Player[] players){
		boolean result = true;
		
				
		// Loop through all of the players given
		for(Player aPlayer : players){
			
			// Check to see if the entity is already in the table
			if(inFinancialEntitiesTable(aPlayer.getUniqueId())){
				
				if(SerenityLoans.debugLevel >= 2)
					SerenityLoans.log.info(String.format("[%s] Player %s is already in system.", plugin.getDescription().getName(), aPlayer.getName()));

				continue;
			
			}
						
			String update = "INSERT INTO FinancialEntities (UserID, Type, Cash, CreditScore) VALUES (?,?,?,?);";
			int rowsUpdated = 0;
			
			try {
				
				PreparedStatement stmt = plugin.getConnection().prepareStatement(update);
				
				stmt.setString(1, aPlayer.getUniqueId().toString());
				stmt.setString(2, "Player");
				
				double cash = 100;
				int crScore = 465;
				
				// Collect configured defaults for initial values
				if(plugin.getConfig().contains("economy.initial-money"))
					cash = plugin.getConfig().getDouble("economy.initial-money");
				if(plugin.getConfig().contains("trust.credit-score.no-history-score"))
					crScore = plugin.getConfig().getInt("trust.credit-score.no-history-score");
				
				stmt.setDouble(3, cash);
				stmt.setInt(4, crScore);
				
				synchronized(financialEntitiesLock){
					rowsUpdated = stmt.executeUpdate();
				}
				
				stmt.close();
				
			} catch (SQLException e) {
				SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
				
			}
			
			// A single row should have been affected by the addition
			if(rowsUpdated != 1){
				if(SerenityLoans.debugLevel >= 2)
					SerenityLoans.log.info(String.format("[%s] Adding player %s failed.", plugin.getDescription().getName(), aPlayer.getName()));
				
				result = false;
			}
			
			// Add offers entries for this entity
			plugin.offerManager.buildFinancialEntityInitialOffers(aPlayer.getUniqueId());
			
			if(SerenityLoans.debugLevel >= 2)
				SerenityLoans.log.info(String.format("[%s] Adding player %s succeeded.", plugin.getDescription().getName(), aPlayer.getName()));
			
			
		}
		
		return result;
	}

	/**
	 * Attempts to create a FinancialInstitution with the given name and manager. The
	 * initial cash of the Institution is set to 0, and the credit score is set to that
	 * of the manager. The type of the Institution is assumed to be Credit Union.
	 * The manager must be a player.
	 * The method will give assign a UUID to the new FinancialInstitution and add it to
	 * the FinancialEntities table. If a FinancialInstitution already exists, this 
	 * method will return success if the manager is the same, or fail if the existing
	 * FinancialInstitution is managed by another entity.
	 * 
	 * @param desiredName The name of the new FinancialInstitution. Must be unique.
	 * @param manager The entity which manages this FinancialInstitution.
	 * @return true if the FinancialInstitution exists in the table at the end of the method,
	 * false otherwise.
	 */
	public boolean createFinancialInstitution(String desiredName, FinancialEntity manager){
		return createFinancialInstitution(desiredName, manager, PlayerType.CREDIT_UNION, 0.0, manager.getCreditScore());
	}

	/**
	 * 
	 * Attempts to create a FinancialInstitution with the given name, manager, entity type,
	 * initial cash, credit score. The manager must be a player.
	 * The method will give assign a UUID to the new FinancialInstitution and add it to
	 * the FinancialEntities table. If a FinancialInstitution already exists, this 
	 * method will return success if the manager is the same, or fail if the existing
	 * FinancialInstitution is managed by another entity.
	 * 
	 * @param desiredName The name of the new FinancialInstitution. Must be unique.
	 * @param manager The entity which manages this FinancialInstitution.
	 * @param type Type of Institution. Must be one of PlayerType excluding "Player".
	 * @param initialCash Initial account balance.
	 * @param crScore Initial creadit score.
	 * @return true if the FinancialInstitution exists in the table at the end of the method,
	 * false otherwise.
	 */
	public boolean createFinancialInstitution(String desiredName, FinancialEntity manager, PlayerType type, double initialCash, int crScore){
		
		// Manager must be a player (yeah yeah)
		if(manager.getPlayerType() != PlayerType.PLAYER)
			return false;
		
		// Institution must not be a player
		if(type.equals(PlayerType.PLAYER))
			return false;
		
		// Name check. Names must be unique
		UUID existingId = getFinancialInstituteID(desiredName);
		if(existingId != null){
			FinancialInstitution currentHolder = getFinancialInstitution(existingId);
			
			if(currentHolder.getResponsibleParty().equals(manager.getUserID()))
				return true;
			else
				return false;
		}
		
		UUID instituteId = null;
		
		// Pick an unoccupied UUID
		do {
			instituteId = UUID.randomUUID();
		} while(inFinancialEntitiesTable(instituteId));
		
		String fEntityString = String.format("INSERT INTO FinancialEntities (UserID, Type, Cash, CreditScore) VALUES (?, ?, %f, %d);", initialCash, crScore);		
		String fInstituteString = "INSERT INTO FinancialInstitutions (UserID, Name, Manager) VALUES (?, ?, ?);";
		
		boolean success = true;
		
		try {
			PreparedStatement ps1 = plugin.conn.prepareStatement(fEntityString);
			PreparedStatement ps2 = plugin.conn.prepareStatement(fInstituteString);
			
			ps1.setString(1, instituteId.toString());
			ps1.setString(2, type.toString());
			
			ps2.setString(1, instituteId.toString());
			ps2.setString(2, desiredName);
			ps2.setString(3, manager.getUserID().toString());
			
			// I should probably acquire both locks at the same time, but I don't think
			// a problem is likely.
			synchronized(financialEntitiesLock){
				success &= ps1.executeUpdate() == 1;
			}
			
			synchronized(financialInstitutionsLock){
				success &= ps2.executeUpdate() == 1;
			}
			
			ps1.close();
			ps2.close();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		// Populate the default offer tables.
		plugin.offerManager.buildFinancialEntityInitialOffers(instituteId);
		
		return success;
	}

	/**
	 * This method takes an name of a FinancialEntity and locates the appropriate uuid.
	 * This method performs the call with a Callable object running in a separate 
	 * thread. This method will block, and should be used with appropriate caution. 
	 * A check is first performed to see if the given name represents a FinancialInstitution,
	 * in which case, no unexpected behavior should result. This method will time out
	 * according to the configuration settings.
	 * 
	 * @param entityName The name of the entity to search for.
	 * @return A uuid corresponding to the given entity name.
	 * @throws InterruptedException 
	 * @throws ExecutionException When the Callable thread encounters an execution issue.
	 * @throws TimeoutException When the timeout has been reached without a response.
	 */
	public UUID entityIdLookup(String entityName) throws InterruptedException, ExecutionException, TimeoutException {
		
		// First perform the FinancialIstitution search
		UUID result = getFinancialInstituteID(entityName);
		
		if(result != null)
			return result;
		
		// Use evilmidget38's Fetcher class
		Callable<Map<String,UUID>> fetcher = new UUIDFetcher(Arrays.asList(entityName));
		Future<Map<String,UUID>> answer = plugin.threads.submit(fetcher);
		
		// Will either get the answer or throw an exception.
		// No need to return a special value.
		result = answer.get(Conf.getLookupTimeout(), TimeUnit.MILLISECONDS).get(entityName);
		
		return result;
	}

	/**
	 * This method takes a UUID that represents a financial entity and attempts to find a name.
	 * It first checks the list of FinancialInstitutions, and then it checks the online
	 * Player list. If neither of these searches return a value, then a call to the Mojang
	 * account service ensues. his method performs the call with a Callable object running in 
	 * a separate thread. This method will block, and should be used with appropriate caution.
	 * This method will time out according to the configuration settings.
	 * 
	 * @param entityID UUID of the entity to search for.
	 * @return The name associated with this UUID.
	 * @throws InterruptedException
	 * @throws ExecutionException When the Callable thread encounters an execution issue.
	 * @throws TimeoutException When the timeout has been reached without a response.
	 */
	public String entityNameLookup(UUID entityID) throws InterruptedException, ExecutionException, TimeoutException {
		
		// First search FinancialInstitutions
		FinancialInstitution bank = getFinancialInstitution(entityID);
		if(bank != null)
			return bank.getName();
		
		// Then search for online players
		Player player = plugin.getServer().getPlayer(entityID);
		
		if(player != null)
			return player.getName();
		
		// Having exhausted the fast calls, use evilmidget38's Fetcher class
		Callable<Map<UUID,String>> fetcher = new NameFetcher(Arrays.asList(entityID));
		Future<Map<UUID,String>> answer = plugin.threads.submit(fetcher);
		
		// Will give answer or throw exception
		String result = answer.get(Conf.getLookupTimeout(), TimeUnit.MILLISECONDS).get(entityID);
		
		return result;
	}

	/**
	 * This is a convenience method that performs the same function as entityNameLookup(UUID),
	 * but takes a FinancialEntity instead of a raw UUID. This method can be marginally
	 * faster than the other in the event that the FinancialEntity is a FinancialInstitution,
	 * since there is no SQL call to perform the first check.
	 * 
	 * @param entity FinancialEntity to search for.
	 * @return The name associated with this FinancialEntity.
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 */
	public String entityNameLookup(FinancialEntity entity) throws InterruptedException, ExecutionException, TimeoutException{
		if(entity instanceof FinancialInstitution)
			return ((FinancialInstitution) entity).getName();
		return entityNameLookup(entity.getUserID());
	}

	/**
	 * This method returns a FinancialEntity object given the UUID of the entity. If no entity
	 * with that UUID exists in the table, it returns null; Note that all FinancialEntities are
	 * immutable.
	 * 
	 * @param userID UUID used in the search.
	 * @return The FinancialEntity object represented by the UUID, or null.
	 */
	public FinancialEntity getFinancialEntity(UUID userID){
		return buildEntity(queryFinancialEntitiesTable(userID));
	}
	
	/**
	 * This method returns a FinancialEntity object given the name of the entity. If no entity
	 * with that name exists, it returns null; Note that all FinancialEntities are
	 * immutable. This method relies on a name lookup which can be potentially blocking, and
	 * so should be used with caution.
	 * 
	 * @param name FinancialEntity name.
	 * @return FinancialEntity object represented by the name.
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 */
	@Deprecated
	public FinancialEntity getFinancialEntity(String name) throws InterruptedException, ExecutionException, TimeoutException{
		return getFinancialEntity(entityIdLookup(name));
	}

	/**
	 * This method returns a FinancialEntity object given the UUID of the entity. If no entity
	 * with that UUID exists in the table, it attempts to add the entity as a player. If that
	 * still fails, it returns null; Note that all FinancialEntities are
	 * immutable.
	 * 
	 * @param userID UUID used in the search.
	 * @return The FinancialEntity object represented by the UUID, or null.
	 */
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



	/**
	 * This method returns a FinancialEntity object given the name of the entity. If no entity
	 * with that name exists, it attempts to add the entity as a player. If that
	 * still fails, it returns null; Note that all FinancialEntities are
	 * immutable. This method relies on a name lookup which can be potentially blocking, and
	 * so should be used with caution.
	 * 
	 * @param name FinancialEntity name.
	 * @return FinancialEntity object represented by the name.
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 */
	@Deprecated
	public FinancialEntity getFinancialEntityAdd(String name) throws InterruptedException, ExecutionException, TimeoutException{
		return getFinancialEntityAdd(entityIdLookup(name));
	}



	/**
	 * Reads the FinancialInstitutions table, and returns the UUID associated
	 * with the given name or null, if the name is not present. 
	 * 
	 * @param entityName
	 * @return UUID of the FinancialInstitution with the given name or null.
	 */
	public UUID getFinancialInstituteID(String entityName) {
		
		String query = "SELECT BankID from FinancialInstitutions WHERE Name=?;";
		String idString = null;
		UUID result = null;
		
		try {
			PreparedStatement stmt = plugin.getConnection().prepareStatement(query);
			
			stmt.setString(1, entityName);
			
			synchronized(financialInstitutionsLock){
				ResultSet candidates = stmt.executeQuery();
			
			
				if(!candidates.next()){
					
					if(SerenityLoans.debugLevel >=2){
						SerenityLoans.log.info(String.format("[%s] FinancialEntityID search for " + entityName + " failed. Institution not found.", plugin.getDescription().getName()));
					}
					
					return result;
					
				}
				
				idString = candidates.getString("BankID");
			}
			
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
	
	/**
	 * Reads the FinancialInstitutions table and returns a FinancialInstitution object
	 * representing the given ID or null if no entry exists. Note that FinancialInstitution
	 * objects are immutable.
	 * 
	 * @param bankId UUID of the relevant FinancialInstitution
	 * @return FinancialInstitution object representing the entry or null.
	 */
	public FinancialInstitution getFinancialInstitution(UUID bankId){
		return (FinancialInstitution) buildEntity(queryFinancialEntitiesTable(bankId));
	}
	
	/**
	 * Reads the FinancialInstitutions table and returns a FinancialInstitution object
	 * representing the given name or null if no entry exists. Note that FinancialInstitution
	 * objects are immutable.
	 * 
	 * @param bankName Name of the relevant FinancialInstitution
	 * @return FinancialInstitution object representing the entry or null.
	 */
	public FinancialInstitution getFinancialInstitution(String bankName){
		return getFinancialInstitution(getFinancialInstituteID(bankName));
	}

	/**
	 * This method takes a UUID of a player and returns a Vector of UUIDs representing
	 * the Entities that this user manages. If the user does not manage any entities, then
	 * it returns null. Note that a manager must be a player. 
	 * 
	 * @param playerID UUID of player to check.
	 * @return A vector of UUIDs of managed entities or null.
	 */
	public Vector<UUID> getManagedEntities(UUID playerID){
		Vector<UUID> results = new Vector<UUID>();
		
		String query = "SELECT BankID FROM FinancialInstitutions WHERE Manager=?;";
		
		try {
			PreparedStatement stmt = plugin.conn.prepareStatement(query);
			
			stmt.setString(1, playerID.toString());
			
			synchronized(financialInstitutionsLock){
				ResultSet hits = stmt.executeQuery();
				
				while(hits.next()){
					results.add(UUID.fromString(hits.getString("BankID")));
				}
			}
			
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		if(results.size() == 0)
			return null;
		
		return results;
	}

	/**
	 * This method returns an OfflinePlayer object given a FinancialEntity
	 * UUID. This method ensures that a real player is given. If the
	 * input UUID is a FinancialInstitution, then the manager will be
	 * returned. If the given UUID is not in the FinancialEntities table,
	 * or cannot be found in the server's player cache, then it returns
	 * null. Note this will return null for valid UUIDs that represent
	 * players that have never played on the server. This method will not
	 * handle large numbers of calls well, as it fetches the offline player
	 * list from the server and searches it by brute-force each time it is
	 * called.
	 * 
	 * @param financialEntityID
	 * @return OfflinePlayer object corresponding to the input ID or manager thereof, or null.
	 */
	public OfflinePlayer getOfflinePlayer(UUID financialEntityID){
		FinancialEntity entity = getFinancialEntity(financialEntityID);
		
		if(entity == null)
			return null;
		
		UUID playerID = entity.getUserID();
		
		// This only checks for management one level deep.
		// Cannot have institutions that manage other institutions
		if(entity instanceof FinancialInstitution)
			playerID = ((FinancialInstitution)entity).getResponsibleParty();
		
		// TODO look for ways to improve
		
		// Only finds OfflinePlayers who have played on the server
		OfflinePlayer[] allPlayers = plugin.getServer().getOfflinePlayers();
		
		OfflinePlayer result = null;
		
		// Currently does a brute-force linear approach. Not appropriate
		// for large number of calls.
		for(OfflinePlayer op : allPlayers){
			if(op.getUniqueId().equals(playerID)){
				result = op;
				break;
			}
		}
		
		// Bukkit claims this is inefficient, but it would be convenient.
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
	 * @return Player corresponding to the input ID or manager thereof, or null.
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
	
	/**
	 * Checks if the given UUID represents an entry in the FinancialEntities table.
	 * 
	 * @param entityID UUID to check
	 * @return true if the entry was found, false otherwise.
	 */
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

	/**
	 * Checks if the given name represents an entry in the FinancialEntities table.
	 * This method depends on a blocking name lookup, and should be used with
	 * caution.
	 * 
	 * @param entityName
	 * @return true if the entry was found, false otherwise.
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 */
	@Deprecated
	public boolean inFinancialEntitiesTable(String entityName) throws InterruptedException, ExecutionException, TimeoutException {
		return inFinancialEntitiesTable(entityIdLookup(entityName));
	}

	/**
	 * Checks if the given UUID represents an entry in the FinancialInstitutions
	 * table.
	 * 
	 * @param entityID UUID to check.
	 * @return true if the entry was found, false otherwise.
	 */
	public boolean inFinancialInstitutionsTable(UUID entityID){
		
		ResultSet result = queryFinancialInstitutionsTable(entityID);
		boolean answer = false;
		
		try {
			answer = result.next();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return answer;
	}

	/**
	 * This method checks is the first user is ignoring the second input user.
	 * If there is no entry for this user pair, then it returns false.
	 * 
	 * @param userId User to check ignore status.
	 * @param targetId Potentially ignored target.
	 * @return true if user is ignoring target, false otherwise.
	 */
	public boolean isIgnoring(UUID userId, UUID targetId){
		String ignoreQuery = "SELECT IgnoreOffers FROM Trust WHERE UserID=? AND TargetID=?;";
		
		try {
			PreparedStatement ps = plugin.conn.prepareStatement(ignoreQuery);
			
			ps.setString(1, userId.toString());
			ps.setString(2, targetId.toString());
			
			synchronized(trustLock){
				ResultSet ignoreResult = ps.executeQuery();
				
				if(ignoreResult.next() && Boolean.valueOf(ignoreResult.getString("IgnoreOffers"))){
					ps.close();
					return true;
				}
				
				else
					ps.close();
			}
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return false;
	}

	/**
	 * Toggles the state of the first user ignoring offers from the target user.
	 * If an entry does not already exist in the Trust table, one is created.
	 * The method returns the new state ignore state, as if the isIgnoring(...)
	 * method was called after the change.
	 * 
	 * @param playerId User to set ignore status.
	 * @param targetId Target of ignore status.
	 * @return true if user is now ignoring target, false otherwise.
	 */
	public boolean toggleIgnore(UUID playerId, UUID targetId){
		String querySQL = "SELECT IgnoreOffers FROM Trust WHERE UserID=? AND TargetID=?;";
		
		// Default if there isn't an entry already
		boolean setToIgnore = true;
		
		try {
			PreparedStatement ps = plugin.conn.prepareStatement(querySQL);
			
			ps.setString(1, playerId.toString());
			ps.setString(2, targetId.toString());
			
			synchronized(trustLock){
			
				ResultSet currentTrust = ps.executeQuery();
				
				String updateSQL;
				
				if(currentTrust.next()){
					// Already an entry
					
					// Switches current state
					setToIgnore = !Boolean.parseBoolean(currentTrust.getString("IgnoreOffers"));
					
					String ignoreString = setToIgnore? "'true'" : "'false'";
					updateSQL = String.format("UPDATE Trust SET IgnoreOffers=%s WHERE UserID=? AND TargetID=?;",  ignoreString);
					
				} else {
					// Need to create an entry
					updateSQL = "INSERT INTO Trust (UserID, TargetID, IgnoreOffers) VALUES (?, ?, 'true');";
				}
				
				PreparedStatement stmt = plugin.conn.prepareStatement(updateSQL);
				
				stmt.setString(1, playerId.toString());
				stmt.setString(2, targetId.toString());
				
				stmt.executeUpdate();
				
				ps.close();
				stmt.close();
			}
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return setToIgnore;
		
	}

	/*
	 * Performs entity construction, given a ResultSet of the desired entry
	 * in the FinancialEntities table. If the result is not a player, it attempts 
	 * to find the corresponding entry in the FinancialInstitutions table. If one is found, then a 
	 * FinancialInstitution object is created, if not, then it returns null.
	 * A Player type will produce a FinancialPlayer object.
	 */
	private FinancialEntity buildEntity(ResultSet successfulQuery){
		if(successfulQuery == null)
			return null;
		
		try {
			if(!successfulQuery.next())
				return null;
			
			// Collect FinancialEntity information.
			String userString = successfulQuery.getString("UserID");
			PlayerType pt = PlayerType.getFromString(successfulQuery.getString("Type"));
			double cash = successfulQuery.getDouble("Cash");
			int crScore = successfulQuery.getInt("CreditScore");
			
			UUID userID = UUID.fromString(userString);
			
			// Make FinancialPlayer object if that's what it is.
			if(pt.equals(PlayerType.PLAYER))
				return new FinancialPlayer(userID, pt, cash, crScore);
			
			// Get FinancialInstitution info if it exists.
			ResultSet instituteQuery = queryFinancialInstitutionsTable(userID);
				
			if(instituteQuery == null || !instituteQuery.next())
				return null;
				
			String name = instituteQuery.getString("Name");
			String managerString = instituteQuery.getString("Manager");
			
			UUID managerID = UUID.fromString(managerString);
			
			// Make FinancialInstitution object.
			return new FinancialInstitution(userID, name, pt, managerID, cash, crScore);
			
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return null;
	}

	/*
	 * Performs a SQL query on the FinancialEntities table for the given UUID
	 * are returns a result set containing every column of that entry. The
	 * ResultSet may be empty if it was not found.
	 */
	private ResultSet queryFinancialEntitiesTable(UUID userID){
		if(userID==null)
			return null;
		
		String entitySearch = "SELECT * from FinancialEntities WHERE UserID=?;";
		ResultSet result = null;
		
		try {
			PreparedStatement stmt = plugin.conn.prepareStatement(entitySearch);
			stmt.setString(1, userID.toString());
			synchronized(financialEntitiesLock){
				result = stmt.executeQuery();
			}
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return result;
	}

	/*
	 * Performs a SQL query on the FinancialInstitutions table for the given UUID
	 * are returns a result set containing every column of that entry. The
	 * ResultSet may be empty if it was not found.
	 */
	private ResultSet queryFinancialInstitutionsTable(UUID userID){
		if(userID==null)
			return null;
		
		String entitySearch = "SELECT * from FinancialInstitutions WHERE BankID=?;";
		ResultSet result = null;
		
		try {
			PreparedStatement stmt = plugin.conn.prepareStatement(entitySearch);
			stmt.setString(1, userID.toString());
			synchronized(financialInstitutionsLock){
				result = stmt.executeQuery();
			}
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return result;
	}

}
