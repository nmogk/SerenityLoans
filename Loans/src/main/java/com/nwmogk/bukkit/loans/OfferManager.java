/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * This file is part of the SerenityLoans Bukkit plugin project.
 * 
 * File: OfferManager.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This class performs mySQL database operations on the PreparedOffers and
 * Offers tables. It reads information from the table and builds Offer
 * objects to give to callers.
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
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import org.bukkit.configuration.file.FileConfiguration;

import com.nwmogk.bukkit.loans.api.FinancialEntity;
import com.nwmogk.bukkit.loans.api.LoanType;
import com.nwmogk.bukkit.loans.exception.InvalidLoanTermsException;
import com.nwmogk.bukkit.loans.object.ImmutableOffer;

public class OfferManager {
	
	private SerenityLoans plugin;

	public enum OfferExitStatus{SUCCESS, IGNORED, UNKNOWN, OVERWRITE_FAIL};
	
	private Object offerTableLock = new Object();
	private Object preparedLock = new Object();
	
	// To shorten subsequent strings
	private String columns = "LenderID, OfferName, Value, InterestRate, Term, CompoundingPeriod, GracePeriod, PaymentTime, PaymentFrequency, LateFee, MinPayment, ServiceFeeFrequency, ServiceFee, LoanType";
	private String copyColumns = "copy.Value, copy.InterestRate, copy.Term, copy.CompoundingPeriod, copy.GracePeriod, copy.PaymentTime, copy.PaymentFrequency, copy.LateFee, copy.MinPayment, copy.ServiceFeeFrequency, copy.ServiceFee, copy.LoanType";
			
	
	public OfferManager(SerenityLoans plugin){
		this.plugin = plugin;
	}
	
	/**
	 * This method inserts the entries for the two prepared offers
	 * that each FinancialEntity possesses. The two offers have the
	 * names 'prepared' and 'default'. This method first detects if
	 * the offers have already been created. It populates the offers
	 * with information in the configuration file.
	 * 
	 * @param playerID UUID of the entity to build tables for. UUID
	 * need not represent a FinancialEntity.
	 */
	public void buildFinancialEntityInitialOffers(UUID playerID) {
		if(SerenityLoans.debugLevel >= 3)
			SerenityLoans.logInfo(String.format("Entering %s method. %s", "buildFinancialEntityInitialOffers(UUID)", SerenityLoans.debugLevel >= 4? "Thread: " + Thread.currentThread().getId() : "."));
		
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
	
		// Set values from settings
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
		
		if(SerenityLoans.debugLevel >= 2)
			SerenityLoans.logInfo("Default terms loaded from config.");
		
		String columns = "LenderID, OfferName, Value, InterestRate, Term, CompoundingPeriod, GracePeriod, PaymentTime, PaymentFrequency, LateFee, MinPayment, ServiceFeeFrequency, ServiceFee, LoanType";
		
		String query1 = "SELECT OfferName FROM PreparedOffers WHERE LenderID=?";
		String query2 = "INSERT INTO PreparedOffers (" + columns + ") VALUES (?, ?, " + value + ", " + interestRate + ", " + term + ", " + compoundingPeriod + ", " + gracePeriod + ", " + paymentTime + ", " + paymentFrequency + ", " + lateFee + ", " + minPayment + ", " + serviceFeeFrequency + ", " + serviceFee + ", " + loanType + ");";

		try {
			
			// build two PreparedOffers
			
			PreparedStatement stmt1 = plugin.conn.prepareStatement(query1);
			PreparedStatement stmt2 = plugin.conn.prepareStatement(query2);
			
			synchronized(preparedLock){
				stmt1.setString(1, playerID.toString());
				
				ResultSet existingOffers = stmt1.executeQuery();
				
				Set<String> searchSet = new HashSet<String>();
				
				// Add current offers to the search set
				while(existingOffers.next()){
					searchSet.add(existingOffers.getString("OfferName"));
				}
				
				
				stmt2.setString(1, playerID.toString());
				
				// Only add offers if they don't already exist.
				if(searchSet.size() == 0 || !searchSet.contains("default")){
				
					if(SerenityLoans.debugLevel >= 2)
						SerenityLoans.logInfo(String.format("Adding \'default\' prepared offer for %s.", playerID.toString()));
					
					stmt2.setString(2, "default");
					stmt2.executeUpdate();
				}
				
				if(searchSet.size() == 0 || !searchSet.contains("prepared")){
					
					if(SerenityLoans.debugLevel >= 2)
						SerenityLoans.logInfo(String.format("Adding \'prepared\' prepared offer for %s.", playerID.toString()));
					
					stmt2.setString(2, "prepared");
					stmt2.executeUpdate();
				}
			
			}
			
			stmt1.close();
			stmt2.close();
			
			
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
	
		if(SerenityLoans.debugLevel >= 4)
			SerenityLoans.logInfo(String.format("Exiting %s method. %s", "buildFinancialEntityInitialOffers(UUID)",  "Thread: " + Thread.currentThread().getId() + "."));
			
	}
	
	/**
	 * This method attempts to create an offer from the given lender to
	 * the given borrower with the given expiration date. The lenderId
	 * and the offer name specify which terms should be associated with
	 * the offer. This method copies the preparedTerms to a new entry
	 * so changes to the player's default and prepared offers will not
	 * affect the state of the offer. The possible exit values are
	 * IGNORED, SUCCESS, OVERWRITE_FAIL, and UNKNOWN.
	 * 
	 * @param lenderID ID of the lender (sender) of the offer.
	 * @param borrowerID ID of the target.
	 * @param preparedOfferName Name of the offer to copy, either 'prepared' or 'default'.
	 * @param offerExpiry Timestamp representing the date and time of the offer expiration.
	 * @return OfferExitStatus with the result of the offer write attempt.
	 */
	public OfferExitStatus createOffer(UUID lenderID, UUID borrowerID, String preparedOfferName, Timestamp offerExpiry){
		if(SerenityLoans.debugLevel >= 3)
			SerenityLoans.logInfo(String.format("Entering %s method. %s", "createOffer(UUID, UUID, String, Timestamp)", SerenityLoans.debugLevel >= 4? "Thread: " + Thread.currentThread().getId() : "."));
		
		// Check for ignoring status first
		if(plugin.playerManager.isIgnoring(borrowerID, lenderID)){
			
			if(SerenityLoans.debugLevel >= 2)
				SerenityLoans.logInfo(String.format("Offers from %s are being ignored by %s.", lenderID.toString(), borrowerID.toString()));
			
			return OfferExitStatus.IGNORED;
		}
		
		
		// Four major tasks in this method
		// - Copy the relevant prepared offer to a new PreparedOffer entry which will be
		// permanently associated with the offer and the loan it represents.
		// - Get the offerID of the copy
		// - Find and remove any existing offers between the lender and borrower (2 SQL statements)
		// - Insert the new Offer (string for this defined later)
		String offerCopy = String.format("INSERT INTO PreparedOffers (%s) SELECT ?, 'inprogress', %s FROM PreparedOffers copy WHERE LenderID=? AND OfferName=?;", columns, copyColumns);
		String offerQuery = "SELECT OfferID from PreparedOffers WHERE LenderID=? AND OfferName='inprogress';";
		String offerNameUpdate = "UPDATE PreparedOffers SET OfferName='' WHERE LenderID=? AND OfferName='inprogress';";
		String deleteOldOffer = "DELETE FROM Offers WHERE LenderID=? AND BorrowerID=?;";
		String checkDeleted = "SELECT * FROM Offers WHERE LenderID=? AND BorrowerID=?;";
		
		
		try {
			PreparedStatement offerTermsCopier = plugin.conn.prepareStatement(offerCopy);
			
			offerTermsCopier.setString(1, lenderID.toString());
			offerTermsCopier.setString(2, lenderID.toString());
			offerTermsCopier.setString(3, preparedOfferName);
			
			int offerId;
			
			// This synchronized block has to cover the entire copying process
			// until we have the id of the permanent prepared offer with no name.
			// Otherwise, the update step to change names could interfere with
			// another thread creating an offer at the same time.
			synchronized(preparedLock){
				int output = offerTermsCopier.executeUpdate();
				
				offerTermsCopier.close();
				
				if(output != 1){
					if(SerenityLoans.debugLevel >= 2)
						SerenityLoans.logInfo("Problem during terms copying.");
					return OfferExitStatus.UNKNOWN;
				}
				
				if(SerenityLoans.debugLevel >= 2)
					SerenityLoans.logInfo("Offer terms copied.");
				
				
				PreparedStatement offerIdFinder = plugin.conn.prepareStatement(offerQuery);
				
				offerIdFinder.setString(1, lenderID.toString());
				
				ResultSet newOfferId = null;
				
				
				newOfferId = offerIdFinder.executeQuery();
				
				offerIdFinder.close();
				
				if(!newOfferId.next()){
					return OfferExitStatus.UNKNOWN;
				}
				
				
				
				offerId = newOfferId.getInt(1);
				
				if(SerenityLoans.debugLevel >= 2)
					SerenityLoans.logInfo(String.format("Offer terms copy ID located: ID:%d.", offerId));
				
				
				PreparedStatement renameOffer = plugin.conn.prepareStatement(offerNameUpdate);
				
				renameOffer.setString(1, lenderID.toString());
			
			
				output = renameOffer.executeUpdate();
				
				renameOffer.close();
				
			
				if(output != 1)
					return OfferExitStatus.UNKNOWN;
				
				if(SerenityLoans.debugLevel >= 2)
					SerenityLoans.logInfo(String.format("Offer terms renamed successfully.", offerId));
				
			}
			
			PreparedStatement deleteOldOfferSQL = plugin.conn.prepareStatement(deleteOldOffer);
			PreparedStatement checkDeletedSQL = plugin.conn.prepareStatement(checkDeleted);
			
			deleteOldOfferSQL.setString(1, lenderID.toString());
			deleteOldOfferSQL.setString(2, borrowerID.toString());
			checkDeletedSQL.setString(1, lenderID.toString());
			checkDeletedSQL.setString(2, borrowerID.toString());
			
			synchronized(offerTableLock){
				int output = deleteOldOfferSQL.executeUpdate();
				ResultSet shouldBeEmpty = checkDeletedSQL.executeQuery();
			
				if(!(output == 1 || output == 0) || shouldBeEmpty.next())
					return OfferExitStatus.OVERWRITE_FAIL;
			}
			
			// Prepare SQL for inserting the new Offer.
			String sentOfferString = String.format("INSERT INTO Offers (LenderID, BorrowerID, ExpirationDate, PreparedTerms) VALUES (?, ?, ?, %d);", offerId);
			if(SerenityLoans.debugLevel >= 3)
				SerenityLoans.logInfo(sentOfferString);
			
			PreparedStatement buildOffer = plugin.conn.prepareStatement(sentOfferString);
			
			buildOffer.setString(1, lenderID.toString());
			buildOffer.setString(2, borrowerID.toString());
			buildOffer.setTimestamp(3, offerExpiry);
			
			if(SerenityLoans.debugLevel >= 3)
				SerenityLoans.logInfo(offerExpiry.toString());
			
			synchronized(offerTableLock){
				int output = buildOffer.executeUpdate();
			
				if(output != 1)
					return OfferExitStatus.UNKNOWN;
			}
			
			deleteOldOfferSQL.close();
			checkDeletedSQL.close();
		} catch (SQLException e) {
			SerenityLoans.logFail(e.getMessage());
			e.printStackTrace();
		}
		
		if(SerenityLoans.debugLevel >= 3)
			SerenityLoans.logInfo("Offer created successfully.");
		
		if(SerenityLoans.debugLevel >= 4)
			SerenityLoans.logInfo(String.format("Exiting %s method. %s", "createOffer(UUID, UUID, String, Timestamp)",  "Thread: " + Thread.currentThread().getId() + "."));
		
		
		return OfferExitStatus.SUCCESS;
	}
	
	/**
	 * This method copies the default prepared offer to the working prepared
	 * offer of the given lender. Returns the success status of the method.
	 * 
	 * @param lenderId
	 * @return
	 */
	public boolean copyOffer(UUID lenderId){
		String deleteOffer = String.format("DELETE FROM PreparedOffers WHERE LenderID='%s' AND OfferName='prepared';", lenderId.toString());
		String copyOffer = String.format("INSERT INTO PreparedOffers (colums) SELECT '%s', copyColumns FROM PreparedOffers copy WHERE LenderID='%s' AND OfferName='default';", lenderId.toString(), lenderId.toString());
		
		try {
			Statement stmt = plugin.conn.createStatement();
			int result;
			
			synchronized(preparedLock){
				result = stmt.executeUpdate(deleteOffer);
				result *= stmt.executeUpdate(copyOffer);
			}
			
			stmt.close();
			return result == 1;
			
		} catch (SQLException e) {
			SerenityLoans.logFail(e.getMessage());
			e.printStackTrace();
		}
		
		return false;
	
	}
	
	/**
	 * Returns an ImmutableOffer object representing the offer
	 * including all of the relevant terms information or null
	 * if the offer doesn't exist.
	 * 
	 * @param lenderID 
	 * @param borrowerID
	 * @return Object representing the offer or null.
	 */
	public ImmutableOffer getOffer(UUID lenderID, UUID borrowerID){
		if(SerenityLoans.debugLevel >= 3)
			SerenityLoans.logInfo(String.format("Entering %s method. %s", "getOffer(UUID, UUID)", SerenityLoans.debugLevel >= 4? "Thread: " + Thread.currentThread().getId() : "."));
		
		return getOffer(lenderID, borrowerID, false);
	}
	
	/**
	 * Returns an ImmutableOffer object representing the offer
	 * if it exists or null otherwise. Will filter out offers
	 * that have been sent if the boolean flag is set to true.
	 * If no offers have been found after the filter, it 
	 * returns null.
	 * 
	 * @param lenderID
	 * @param borrowerID
	 * @param filterSent Whether or not to filter out sent offers.
	 * @return Object representing the offer or null;
	 */
	public ImmutableOffer getOffer(UUID lenderID, UUID borrowerID, boolean filterSent){
		if(SerenityLoans.debugLevel >= 3)
			SerenityLoans.logInfo(String.format("Entering %s method. %s", "getOffer(UUID, UUID, boolean)", SerenityLoans.debugLevel >= 4? "Thread: " + Thread.currentThread().getId() : "."));
		
		String query = String.format("SELECT * FROM offer_view WHERE LenderID=? AND BorrowerID=?%s;", filterSent? " AND Sent='false'" : "");
		String query2 = "SELECT PreparedTerms FROM Offers WHERE LenderID=? AND BorrowerID=?;";
		ImmutableOffer offer = null;
		
		try {
			PreparedStatement stmt = plugin.conn.prepareStatement(query);
			PreparedStatement stmt2 = plugin.conn.prepareStatement(query2);
			
			stmt.setString(1, lenderID.toString());
			stmt.setString(2, borrowerID.toString());
			
			stmt2.setString(1, lenderID.toString());
			stmt2.setString(2, borrowerID.toString());
			
			ResultSet results = stmt.executeQuery();
			
			if(!results.next())
				return null;
			
			FinancialEntity lender = plugin.playerManager.getFinancialEntity(UUID.fromString(results.getString("LenderID")));
			FinancialEntity borrower = plugin.playerManager.getFinancialEntity(UUID.fromString(results.getString("BorrowerID")));
			double value = results.getDouble("Value");
			double interestRate = results.getDouble("InterestRate");
			double lateFee = results.getDouble("LateFee");
			double minPayment = results.getDouble("MinPayment");
			double serviceFee = results.getDouble("ServiceFee");
			long term = results.getLong("Term");
			long compoundingPeriod = results.getLong("CompoundingPeriod");
			long gracePeriod = results.getLong("GracePeriod");
			long paymentTime = results.getLong("PaymentTime");
			long paymentFrequency = results.getLong("PaymentFrequency");
			long serviceFeeFrequency = results.getLong("ServiceFeeFrequency");
			Timestamp expDate = results.getTimestamp("ExpirationDate");
			LoanType lt = LoanType.getFromString(results.getString("LoanType"));
			
			synchronized(offerTableLock){
				results = stmt2.executeQuery();
			}
			
			if(!results.next())
				return null;
			
			int termsID = results.getInt(1);
			
			offer = new ImmutableOffer(lender, borrower, value, interestRate, lateFee, minPayment, serviceFee, term, compoundingPeriod, gracePeriod, paymentTime, paymentFrequency, serviceFeeFrequency, lt, expDate, termsID);

			
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		if(SerenityLoans.debugLevel >= 4)
			SerenityLoans.logInfo(String.format("Exiting %s method. %s", "getOffer(UUID, UUID, boolean)",  "Thread: " + Thread.currentThread().getId() + "."));
		
		
		return offer;
	}
	
	/**
	 * Returns a list of FinancialEntities who have been sent
	 * offers from the given lender. The returned list is of
	 * type LinkedList.
	 * 
	 * @param lenderID
	 * @return
	 */
	public List<FinancialEntity> getOfferRecipientsFrom(UUID lenderID){
		if(SerenityLoans.debugLevel >= 3)
			SerenityLoans.logInfo(String.format("Entering %s method. %s", "getOfferRecipientsFrom(UUID)", SerenityLoans.debugLevel >= 4? "Thread: " + Thread.currentThread().getId() : "."));
		
		String query = "SELECT BorrowerID FROM Offers WHERE LenderID=?;";
		LinkedList<FinancialEntity> list = new LinkedList<FinancialEntity>();
		
		try {
			PreparedStatement stmt = plugin.conn.prepareStatement(query);
			
			stmt.setString(1, lenderID.toString());
			
			ResultSet results = null;
			
			synchronized(offerTableLock){
				results = stmt.executeQuery();
			}
			
			while(results.next())
				list.add(plugin.playerManager.getFinancialEntity(UUID.fromString(results.getString("BorrowerID"))));
			
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		if(SerenityLoans.debugLevel >= 4)
			SerenityLoans.logInfo(String.format("Exiting %s method. %s", "getOfferRecipientsFrom(UUID)",  "Thread: " + Thread.currentThread().getId() + "."));
		
		return list;
		
	}
	
	/**
	 * Returns a list of FinancialEntities who have sent offers
	 * to the given borrower. The list returned is of type
	 * LinkedList.
	 * 
	 * @param borrowerID
	 * @return
	 */
	public List<FinancialEntity> getOfferSendersTo(UUID borrowerID){
		if(SerenityLoans.debugLevel >= 3)
			SerenityLoans.logInfo(String.format("Entering %s method. %s", "getOfferSendersTo(UUID)", SerenityLoans.debugLevel >= 4? "Thread: " + Thread.currentThread().getId() : "."));
		
		return getOfferSendersTo(borrowerID, false);
	}
	
	/**
	 * Returns a list of FinancialEntities who have sent offers
	 * to the given borrower. The list will be filtered so that
	 * only offers which have not been sent will be included.
	 * 
	 * @param borrowerID
	 * @param filterSent
	 * @return
	 */
	public List<FinancialEntity> getOfferSendersTo(UUID borrowerID, boolean filterSent){
		if(SerenityLoans.debugLevel >= 3)
			SerenityLoans.logInfo(String.format("Entering %s method. %s", "getOfferSendersTo(UUID, boolean)", SerenityLoans.debugLevel >= 4? "Thread: " + Thread.currentThread().getId() : "."));
		
		String query = String.format("SELECT LenderID FROM Offers WHERE BorrowerID=?%s;", filterSent? " AND Sent='false'" : "");
		LinkedList<FinancialEntity> list = new LinkedList<FinancialEntity>();
		
		try {
			PreparedStatement stmt = plugin.conn.prepareStatement(query);
			
			stmt.setString(1, borrowerID.toString());
			
			ResultSet results = null;
			
			synchronized(offerTableLock){
				results = stmt.executeQuery();
			}
			
			while(results.next())
				list.add(plugin.playerManager.getFinancialEntity(UUID.fromString(results.getString("LenderID"))));
			
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		if(SerenityLoans.debugLevel >= 4)
			SerenityLoans.logInfo(String.format("Exiting %s method. %s", "getOfferSendersTo(UUID, boolean)",  "Thread: " + Thread.currentThread().getId() + "."));
		
		return list;
	}
	
	/**
	 * Builds an ImmutableOffer object which has the given name and belongs to 
	 * the given lender. The immutable offer has a null borrower and expiration
	 * date, as that information is only available on a full offer. Will
	 * return null if there was a problem or the offer cannot be found.
	 * 
	 * @param lenderId
	 * @param offerName
	 * @return
	 */
	public ImmutableOffer getPreparedOffer(UUID lenderId, String offerName){
		if(SerenityLoans.debugLevel >= 3)
			SerenityLoans.logInfo(String.format("Entering %s method. %s", "getPreparedOffer(UUID, String)", SerenityLoans.debugLevel >= 4? "Thread: " + Thread.currentThread().getId() : "."));
		
		String query = "SELECT * FROM PreparedOffers WHERE LenderID=? AND OfferName=?;";
		ImmutableOffer offer = null;
		
		try {
			PreparedStatement stmt = plugin.conn.prepareStatement(query);
			
			stmt.setString(1, lenderId.toString());
			stmt.setString(2, offerName);
			
			ResultSet results = null;
			
			synchronized(preparedLock){
				results = stmt.executeQuery();
			}
			
			if(!results.next()){
				stmt.close();
				return null;
			}
			
			FinancialEntity lender = plugin.playerManager.getFinancialEntity(lenderId);
			FinancialEntity borrower = null;
			double value = results.getDouble("Value");
			double interestRate = results.getDouble("InterestRate");
			double lateFee = results.getDouble("LateFee");
			double minPayment = results.getDouble("MinPayment");
			double serviceFee = results.getDouble("ServiceFee");
			long term = results.getLong("Term");
			long compoundingPeriod = results.getLong("CompoundingPeriod");
			long gracePeriod = results.getLong("GracePeriod");
			long paymentTime = results.getLong("PaymentTime");
			long paymentFrequency = results.getLong("PaymentFrequency");
			long serviceFeeFrequency = results.getLong("ServiceFeeFrequency");
			Timestamp expDate = null;
			int termsID = results.getInt("OfferID");
			LoanType lt = LoanType.getFromString(results.getString("LoanType"));
			
			offer = new ImmutableOffer(lender, borrower, value, interestRate, lateFee, minPayment, serviceFee, term, compoundingPeriod, gracePeriod, paymentTime, paymentFrequency, serviceFeeFrequency, lt, expDate, termsID);

			
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		if(SerenityLoans.debugLevel >= 4)
			SerenityLoans.logInfo(String.format("Exiting %s method. %s", "getPreparedOffer(UUID, String)",  "Thread: " + Thread.currentThread().getId() + "."));
		
		return offer;
	}
	
	/**
	 * Returns an ImmutableOffer from the given offerId. It includes the
	 * given lender and borrower for completeness. The expiration date will
	 * be null. Will return null if the offer cannot be found.
	 * 
	 * @param offerId
	 * @param lender
	 * @param borrower
	 * @return
	 */
	public ImmutableOffer getPreparedOffer(int offerId, FinancialEntity lender, FinancialEntity borrower){
		if(SerenityLoans.debugLevel >= 3)
			SerenityLoans.logInfo(String.format("Entering %s method. %s", "getPreparedOffer(int, FinancialEntity, FinancialEntity)", SerenityLoans.debugLevel >= 4? "Thread: " + Thread.currentThread().getId() : "."));
		
		String query = String.format("SELECT * FROM PreparedOffers WHERE OfferID=%d;", offerId);
		ImmutableOffer offer = null;
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			ResultSet results = null;
			
			synchronized(preparedLock){
				results = stmt.executeQuery(query);
			}
			
			if(!results.next()){
				stmt.close();
				return null;
			}
			
			double value = results.getDouble("Value");
			double interestRate = results.getDouble("InterestRate");
			double lateFee = results.getDouble("LateFee");
			double minPayment = results.getDouble("MinPayment");
			double serviceFee = results.getDouble("ServiceFee");
			long term = results.getLong("Term");
			long compoundingPeriod = results.getLong("CompoundingPeriod");
			long gracePeriod = results.getLong("GracePeriod");
			long paymentTime = results.getLong("PaymentTime");
			long paymentFrequency = results.getLong("PaymentFrequency");
			long serviceFeeFrequency = results.getLong("ServiceFeeFrequency");
			Timestamp expDate = null;
			int termsID = results.getInt("OfferID");
			
			offer = new ImmutableOffer(lender, borrower, value, interestRate, lateFee, minPayment, serviceFee, term, compoundingPeriod, gracePeriod, paymentTime, paymentFrequency, serviceFeeFrequency, null, expDate, termsID);

			
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		if(SerenityLoans.debugLevel >= 4)
			SerenityLoans.logInfo(String.format("Exiting %s method. %s", "getPreparedOffer(int, FinancialEntity, FinancialEntity)",  "Thread: " + Thread.currentThread().getId() + "."));
		
		return offer;
	}
	
	/**
	 * This method takes the termsId and returns the value of
	 * the loan represented by the terms. If there is a problem
	 * then the result will be -1.
	 * 
	 * @param preparedTermsId
	 * @return
	 */
	public double getTermsValue(int preparedTermsId){
		if(SerenityLoans.debugLevel >= 3)
			SerenityLoans.logInfo(String.format("Entering %s method. %s", "getTermsValue(int)", SerenityLoans.debugLevel >= 4? "Thread: " + Thread.currentThread().getId() : "."));
		
		String query = String.format("SELECT Value FROM PreparedOffers WHERE OfferID=%d;", preparedTermsId);
		double result = -1;
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			ResultSet res = null;
			
			synchronized(preparedLock){
				res = stmt.executeQuery(query);
			}
			
			if(!res.next())
				return result;
			
			result = res.getDouble(1);
			
			stmt.close();
			
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		if(SerenityLoans.debugLevel >= 4)
			SerenityLoans.logInfo(String.format("Exiting %s method. %s", "getTermsValue(int)",  "Thread: " + Thread.currentThread().getId() + "."));
		
		
		return result;
		
	}
	
	/**
	 * This method marks an offer as having been sent to the potential borrower.
	 * It returns the success of the call.
	 * 
	 * @param lenderId
	 * @param borrowerId
	 * @return
	 */
	public boolean registerOfferSend(UUID lenderId, UUID borrowerId){
		if(SerenityLoans.debugLevel >= 3)
			SerenityLoans.logInfo(String.format("Entering %s method. %s", "registerOfferSend(UUID, UUID)", SerenityLoans.debugLevel >= 4? "Thread: " + Thread.currentThread().getId() : "."));
		
		String sentUpdate = "UPDATE Offers SET Sent='true' WHERE LenderID=? AND BorrowerID=?;";

		try {
			
			PreparedStatement stmt = plugin.conn.prepareStatement(sentUpdate);
			
			stmt.setString(1, lenderId.toString());
			stmt.setString(2, borrowerId.toString());
			
			synchronized(offerTableLock){
				int result = stmt.executeUpdate();
			
				if(result != 1){
					stmt.close();
					return false;
				}
			}
			
			stmt.close();
					
			return true;
					
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		if(SerenityLoans.debugLevel >= 4)
			SerenityLoans.logInfo(String.format("Exiting %s method. %s", "registerOfferSend(UUID, UUID)",  "Thread: " + Thread.currentThread().getId() + "."));
		
		
		return false;
	}
	
	/**
	 * Removes an offer from the Offers table. This method
	 * will also delete the underlying item from the PreparedOffers table.
	 * If this is not desired (for example, processing the offer), then
	 * use removeOffer(lenderId, borrowerId, false) instead.
	 * 
	 * @param lenderId
	 * @param borrowerId
	 * @return
	 */
	public boolean removeOffer(UUID lenderId, UUID borrowerId){
		return removeOffer(lenderId, borrowerId, true);
	}

	/**
	 * 
	 * Removes an offer from the Offers table according to the given 
	 * lender and borrower. The boolean parameter controls whether or
	 * not to remove the PreparedOffer entry representing the terms.
	 * The return value is the success of the method.
	 * 
	 * @param lenderId
	 * @param borrowerId
	 * @param cleanPrepared
	 * @return
	 */
	public boolean removeOffer(UUID lenderId, UUID borrowerId, boolean cleanPrepared){
		if(SerenityLoans.debugLevel >= 3)
			SerenityLoans.logInfo(String.format("Entering %s method. %s", "removeOffer(UUID, UUID)", SerenityLoans.debugLevel >= 4? "Thread: " + Thread.currentThread().getId() : "."));
		
		String update = "DELETE FROM Offers WHERE LenderID=? AND BorrowerID=?;";
		String selectOfferNumber = String.format("SELECT PreparedTerms FROM Offers WHERE LenderID='%s' AND BorrowerID='%s';", lenderId.toString(), borrowerId.toString());
		int exit = -1;
		
		try {
			
			Statement stmt = null;
			int offerId;
			String deletePrepared = null;
			
			
			// Get ID of prepared offer if instructed
			if(cleanPrepared){
				stmt = plugin.conn.createStatement();
				ResultSet offerResult;
				
				synchronized(preparedLock){
					offerResult = stmt.executeQuery(selectOfferNumber);
				}
				
				offerResult.next();
				
				offerId = offerResult.getInt(1);
				deletePrepared = String.format("DELETE FROM PreparedOffers WHERE OfferID=%d;", offerId);
				
			}
			
			
			// Delete the offer
			PreparedStatement ps = plugin.conn.prepareStatement(update);
			
			ps.setString(1, lenderId.toString());
			ps.setString(2, borrowerId.toString());
			
			synchronized(offerTableLock){
				exit = ps.executeUpdate();
			}
			
			// Delete the prepared offer now
			if(cleanPrepared){
				
				synchronized(preparedLock){
					exit *= stmt.executeUpdate(deletePrepared);
				}
				
				stmt.close();
			}
			
			ps.close();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
			return false;
		}
		
		if(SerenityLoans.debugLevel >= 4)
			SerenityLoans.logInfo(String.format("Exiting %s method. %s", "removeOffer(UUID, UUID)",  "Thread: " + Thread.currentThread().getId() + "."));
		
		
		return exit == 0 || exit == 1;
		
	}
	
	/**
	 * This method takes a lenderId, a boolean representing which of the prepared offers
	 * should be written to. The argument string should be in the form of a parameter
	 * name followed by an equals sign followed by the desired parameter value, properly
	 * formatted. There must be no spaces in the argument. The method will return false
	 * in the event of improperly formatted input, including parameter values that do
	 * not parse correctly. Values that are outside of the allowed configured range
	 * will throw an InvalidLoanTermsException, which contains a message detailing the
	 * problem with the value.
	 * 
	 * @param lenderId
	 * @param isDefault
	 * @param argument
	 * @return
	 * @throws InvalidLoanTermsException
	 */
	public boolean setTerms(UUID lenderId, boolean isDefault, String argument) throws InvalidLoanTermsException{
		if(SerenityLoans.debugLevel >= 3)
			SerenityLoans.logInfo(String.format("Entering %s method. %s", "setTerms(UUID, boolean, String)", SerenityLoans.debugLevel >= 4? "Thread: " + Thread.currentThread().getId() : "."));
		
		
		String[] parsedArg = argument.split("=");
		
		if(parsedArg.length != 2)
			return false;
		
		String updateColumn = null;
		String objective = null;
		
		FileConfiguration config = plugin.getConfig();
		
		try{
			
			
			// The following statements parse the input
			// The basic form is as follows:
			// * Set default (generous) minimum and maximum allowed configuration values
			// * Read the configured minimum and maximum values from the config values
			// * Process actual minimum and maximum values
			// * Parse parameter value from string
			// * Set column name string and value update string.
			if(parsedArg[0].equalsIgnoreCase("Value")){
				updateColumn = "Value";
				
				double minValue = 0.0;
				double maxValue = Double.MAX_VALUE;
				
				String minValuePath = "loan.terms-constraints.principal-value.min";
				String maxValuePath = "loan.terms-constraints.principal-value.max";
				
				if(config.contains(minValuePath) && config.isDouble(minValuePath))
					minValue = Math.max(0, config.getDouble(minValuePath));
				if(config.contains(maxValuePath) && config.isDouble(maxValuePath))
					maxValue = Math.max(minValue, config.getDouble(maxValuePath));
				
				Double value = Double.parseDouble(parsedArg[1]);
				
				if(value < minValue)
					throw new InvalidLoanTermsException(String.format("Value below configured minimum of %s", plugin.econ.format(minValue)));
				if(value > maxValue)
					throw new InvalidLoanTermsException(String.format("Value above configured minimum of %s", plugin.econ.format(maxValue)));
				
				objective = value.toString();
				
			} else if(parsedArg[0].equalsIgnoreCase("InterestRate") || parsedArg[0].equalsIgnoreCase("Interest") || parsedArg[0].equalsIgnoreCase("Int")){
				updateColumn = "InterestRate";
				
				double minValue = 0.0;
				double maxValue = Double.MAX_VALUE;
				
				String minValuePath = "loan.terms-constraints.interest.minrate";
				String maxValuePath = "loan.terms-constraints.interest.maxrate";
				
				if(config.contains(minValuePath) && config.isDouble(minValuePath))
					minValue = Math.max(0, config.getDouble(minValuePath));
				if(config.contains(maxValuePath) && config.isDouble(maxValuePath))
					maxValue = Math.max(minValue, config.getDouble(maxValuePath));
				
				DecimalFormat parser = new DecimalFormat();
				Number perc = parser.parse(parsedArg[1], new ParsePosition(0));
	
				if(perc == null)
					return false;
				
				Double value = perc.doubleValue();
				
				if(value < minValue)
					throw new InvalidLoanTermsException(String.format("InterestRate below configured minimum of %s", plugin.econ.formatPercent(minValue)));
				if(value > maxValue)
					throw new InvalidLoanTermsException(String.format("InterestRate above configured minimum of %s", plugin.econ.formatPercent(maxValue)));
				
				objective = value.toString();
				
			} else if(parsedArg[0].equalsIgnoreCase("Term")){
				updateColumn = "Term";
				
				long minValue = 0l;
				long maxValue = Long.MAX_VALUE;
				
				String minValuePath = "loan.terms-constraints.term.min";
				String maxValuePath = "loan.terms-constraints.term.max";
				
				if(config.contains(minValuePath))
					minValue = Conf.parseTime(config.getString(minValuePath));
				if(plugin.getConfig().contains(maxValuePath))
					maxValue = Math.max(minValue, Conf.parseTime(config.getString(maxValuePath)));
				
				Long value = Conf.parseTime(parsedArg[1]);
				if(value == 0)
					return false;
	
				if(value < minValue)
					throw new InvalidLoanTermsException(String.format("Term below configured minimum of %s", Conf.buildTimeString(minValue)));
				if(value > maxValue)
					throw new InvalidLoanTermsException(String.format("Term above configured minimum of %s", Conf.buildTimeString(maxValue)));
				
				objective = value.toString();
				
			} else if(parsedArg[0].equalsIgnoreCase("CompoundingPeriod")){
				updateColumn = "CompoundingPeriod";
				
				long minValue = 0l;
				long maxValue = Long.MAX_VALUE;
				
				String minPath = "loan.terms-constraints.interest.compounding.min-time";
				String maxPath = "loan.terms-constraints.interest.compounding.max-time";
				
				if(config.contains(minPath))
					minValue = Conf.parseTime(config.getString(minPath));
				if(config.contains(maxPath))
					maxValue = Math.max(minValue, Conf.parseTime(config.getString(maxPath)));
				
				Long value = Conf.parseTime(parsedArg[1]);
				
				if(value < minValue)
					throw new InvalidLoanTermsException(String.format("CompoundingPeriod below configured minimum of %s", Conf.buildTimeString(minValue)));
				if(value > maxValue)
					throw new InvalidLoanTermsException(String.format("CompoundingPeriod above configured minimum of %s", Conf.buildTimeString(maxValue)));
				
				boolean allow = true;
				String contCompoundPath = "loan.terms-constraints.interest.compounding.allow-continuous";
				
				if(config.contains(contCompoundPath) && config.isBoolean(contCompoundPath))
					allow = config.getBoolean(contCompoundPath);
				if(value == 0 && !allow)
					throw new InvalidLoanTermsException("Administrator does not allow continuously compounding interest.");
				
				objective = value.toString();
				
			} else if(parsedArg[0].equalsIgnoreCase("GracePeriod")){
				updateColumn = "GracePeriod";
				
				long minValue = 0l;
				long maxValue = Long.MAX_VALUE;
				
				String minPath = "loan.terms-constraints.fees.grace-period.min";
				String maxPath = "loan.terms-constraints.fees.grace-period.max";
				
				if(config.contains(minPath))
					minValue = Conf.parseTime(config.getString(minPath));
				if(config.contains(maxPath))
					maxValue = Math.max(minValue, Conf.parseTime(config.getString(maxPath)));
						
				Long value = Conf.parseTime(parsedArg[1]);
	
				if(value < minValue)
					throw new InvalidLoanTermsException(String.format("GracePeriod below configured minimum of %s", Conf.buildTimeString(minValue)));
				if(value > maxValue)
					throw new InvalidLoanTermsException(String.format("GracePeriod above configured minimum of %s", Conf.buildTimeString(maxValue)));
				
				objective = value.toString();
				
			} else if(parsedArg[0].equalsIgnoreCase("PaymentTime") || parsedArg[0].equalsIgnoreCase("paytime")){
				updateColumn = "PaymentTime";
				
				long minValue = 0l;
				long maxValue = Long.MAX_VALUE;
				
				String minPath = "loan.terms-constraints.payment-time.min";
				String maxPath = "loan.terms-constraints.payment-time.max";
				
				if(config.contains(minPath))
					minValue = Conf.parseTime(config.getString(minPath));
				if(config.contains(maxPath))
					maxValue = Math.max(minValue, Conf.parseTime(config.getString(maxPath)));
				
				Long value = Conf.parseTime(parsedArg[1]);
				if(value == 0)
					return false;
	
				if(value < minValue)
					throw new InvalidLoanTermsException(String.format("PaymentTime below configured minimum of %s", Conf.buildTimeString(minValue)));
				if(value > maxValue)
					throw new InvalidLoanTermsException(String.format("PaymentTime above configured minimum of %s", Conf.buildTimeString(maxValue)));
				
				objective = value.toString();
				
			} else if(parsedArg[0].equalsIgnoreCase("PaymentFrequency")){
				updateColumn = "PaymentFrequency";
				
				long minValue = 0l;
				long maxValue = Long.MAX_VALUE;
				
				String minPath = "loan.terms-constraints.payment-frequency.min";
				String maxPath = "loan.terms-constraints.payment-frequency.max";
				
				if(config.contains(minPath))
					minValue = Conf.parseTime(config.getString(minPath));
				if(config.contains(maxPath))
					maxValue = Math.max(minValue, Conf.parseTime(config.getString(maxPath)));
				
				Long value = Conf.parseTime(parsedArg[1]);
				if(value == 0)
					return false;
	
				if(value < minValue)
					throw new InvalidLoanTermsException(String.format("PaymentFrequency below configured minimum of %s", Conf.buildTimeString(minValue)));
				if(value > maxValue)
					throw new InvalidLoanTermsException(String.format("PaymentFrequency above configured minimum of %s", Conf.buildTimeString(maxValue)));
				
				objective = value.toString();
				
			} else if(parsedArg[0].equalsIgnoreCase("LateFee")){
				updateColumn = "LateFee";
				
				boolean allow = true;
				String allowPath = "loan.terms-constraints.fees.late-fee.allow-change";
				
				if(config.contains(allowPath) && config.isBoolean(allowPath))
					allow = config.getBoolean(allowPath);
				if(!allow)
					throw new InvalidLoanTermsException("Administrator does not allow changes to the late fee.");
				
				double minValue = 0.0;
				double maxValue = Double.MAX_VALUE;
				
				String minPath = "loan.terms-constraints.fees.late-fee.min";
				String maxPath = "loan.terms-constraints.fees.late-fee.max";
				
				if(config.contains(minPath) && config.isDouble(minPath))
					minValue = Math.max(0, config.getDouble(minPath));
				if(config.contains(maxPath) && config.isDouble(maxPath))
					maxValue = Math.max(minValue, config.getDouble(maxPath));
				
				Double value = Double.parseDouble(parsedArg[1]);
				
				if(value < minValue)
					throw new InvalidLoanTermsException(String.format("LateFee below configured minimum of %s", plugin.econ.format(minValue)));
				if(value > maxValue)
					throw new InvalidLoanTermsException(String.format("LateFee above configured minimum of %s", plugin.econ.format(maxValue)));
				
				objective = value.toString();
				
			} else if(parsedArg[0].equalsIgnoreCase("MinPayment") || parsedArg[0].equalsIgnoreCase("minimumpayment")){
				updateColumn = "MinPayment";
				
				double minValue = 0.0;
				double maxValue = Double.MAX_VALUE;
				
				String minPath = "loan.terms-constraints.min-payment.min";
				String maxPath = "loan.terms-constraints.min-payment.max";
				
				boolean percentageRule = false;
				String rulePath = "loan.terms-constraints.min-payment.percent-rule";
				if(config.contains(rulePath) && config.isBoolean(rulePath))
					percentageRule = config.getBoolean(rulePath);
				
				
				if(config.contains(minPath) && config.isDouble(minPath))
					minValue = Math.max(0, config.getDouble(minPath));
				if(config.contains(maxPath) && config.isDouble(maxPath))
					maxValue = Math.max(minValue, config.getDouble(maxPath));
				
				Double value = Double.parseDouble(parsedArg[1]);
				
				if(value < minValue)
					throw new InvalidLoanTermsException(String.format("MinPayment below configured minimum of %s", percentageRule? plugin.econ.formatPercent(minValue) : plugin.econ.format(minValue)));
				if(value > maxValue)
					throw new InvalidLoanTermsException(String.format("MinPayment above configured minimum of %s", percentageRule? plugin.econ.formatPercent(maxValue) : plugin.econ.format(maxValue)));
				
				objective = value.toString();
				
			} else if(parsedArg[0].equalsIgnoreCase("ServiceFeeFrequency")){
				updateColumn = "ServiceFeeFrequency";
				
				boolean allow = true;
				String allowPath = "loan.terms-constraints.fees.service-fee.allow-change";
				
				if(config.contains(allowPath) && config.isBoolean(allowPath))
					allow = config.getBoolean(allowPath);
				if(!allow)
					throw new InvalidLoanTermsException("Administrator does not allow changes to the service fee.");
				
				long minValue = 0l;
				long maxValue = Long.MAX_VALUE;
				
				String minPath = "loan.terms-constraints.fees.service-fee-frequency.min";
				String maxPath = "loan.terms-constraints.fees.service-fee-frequency.max";
				
				if(config.contains(minPath))
					minValue = Conf.parseTime(config.getString(minPath));
				if(config.contains(maxPath))
					maxValue = Math.max(minValue, Conf.parseTime(config.getString(maxPath)));
				
				Long value = Conf.parseTime(parsedArg[1]);
				
				if(value < minValue)
					throw new InvalidLoanTermsException(String.format("ServiceFeeFrequency below configured minimum of %s", Conf.buildTimeString(minValue)));
				if(value > maxValue)
					throw new InvalidLoanTermsException(String.format("ServiceFeeFrequency above configured minimum of %s", Conf.buildTimeString(maxValue)));
				
				objective = value.toString();
				
			} else if(parsedArg[0].equalsIgnoreCase("ServiceFee")){
				updateColumn = "ServiceFee";
				
				boolean allow = true;
				String allowPath = "loan.terms-constraints.fees.service-fee.allow-change";
				
				if(config.contains(allowPath) && config.isBoolean(allowPath))
					allow = config.getBoolean(allowPath);
				if(!allow)
					throw new InvalidLoanTermsException("Administrator does not allow changes to the service fee.");
				
				double minValue = 0.0;
				double maxValue = Double.MAX_VALUE;
	
				String minPath = "loan.terms-constraints.fees.service-fee.min";
				String maxPath = "loan.terms-constraints.fees.service-fee.max";
				
				if(config.contains(minPath) && config.isDouble(minPath))
					minValue = Math.max(0, config.getDouble(minPath));
				if(config.contains(maxPath) && config.isDouble(maxPath))
					maxValue = Math.max(minValue, config.getDouble(maxPath));
				
				Double value = Double.parseDouble(parsedArg[1]);
				
				if(value < minValue)
					throw new InvalidLoanTermsException(String.format("ServiceFee below configured minimum of %s", plugin.econ.format(minValue)));
				if(value > maxValue)
					throw new InvalidLoanTermsException(String.format("ServiceFee above configured minimum of %s", plugin.econ.format(maxValue)));
				
				objective = value.toString();
				
			} else if(parsedArg[0].equalsIgnoreCase("LoanType")){
				updateColumn = "LoanType";
	
				String s  = parsedArg[1];
				if(s.equalsIgnoreCase("Amortizing") || s.equalsIgnoreCase("InterestOnly") || s.equalsIgnoreCase("FixedFee") || s.equalsIgnoreCase("Bullet") || s.equalsIgnoreCase("Credit") || s.equalsIgnoreCase("Gift") || s.equalsIgnoreCase("Bond") || s.equalsIgnoreCase("Deposit") || s.equalsIgnoreCase("Salary"))
					objective = "'" + s + "'";
				else
					return false;
				
			} else
				return false;
		

		// Improperly formatted values will return false
		} catch(NumberFormatException e){
			// This will catch improperly formatted input
			return false;
		}
		
		if(objective == null)
			return false;
		
		// Because the values can be formatted very differently, we must insert them
		// as strings. This is insecure except with the very careful preparation in
		// the preceeding code. DO NOT COPY WITHOUT THINKING ABOUT SECURITY!
		String updateSQL = String.format("UPDATE PreparedOffers SET %s=%s WHERE LenderID=? AND OfferName='%s';", updateColumn, objective,  isDefault? "default":"prepared" );
		
		
		try {
			PreparedStatement stmt = plugin.conn.prepareStatement(updateSQL);
			
			stmt.setString(1, lenderId.toString());
			
			int result;
			synchronized(preparedLock){
				result = stmt.executeUpdate();
			}
			
			stmt.close();
			
			return result == 1;
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		if(SerenityLoans.debugLevel >= 4)
			SerenityLoans.logInfo(String.format("Exiting %s method. %s", "setTerms(UUID, boolean, String)",  "Thread: " + Thread.currentThread().getId() + "."));
		
		return false;
	}
	
	/**
	 * Updates all of the existing offers and removes offers which have expired.
	 */
	public synchronized void updateAll(){
		if(SerenityLoans.debugLevel >= 3)
			SerenityLoans.logInfo(String.format("Entering %s method. %s", "updateAll()", SerenityLoans.debugLevel >= 4? "Thread: " + Thread.currentThread().getId() : "."));
		
		// Selects only offers which are expired
		String query = "SELECT LenderID, BorrowerID FROM Offers WHERE ExpirationDate < NOW();";
		LinkedList<Vector<UUID>> expOffers = new LinkedList<Vector<UUID>>();
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			ResultSet rs = null;
			
			synchronized(offerTableLock){
				rs = stmt.executeQuery(query);
			}
			
			// Put offers into a list
			// A vector is used to hold the pair
			while(rs.next()){
				UUID lenderId = UUID.fromString(rs.getString("LenderID"));
				UUID borrowerId = UUID.fromString(rs.getString("BorrowerID"));
				Vector<UUID> thePair = new Vector<UUID>(2);
				thePair.add(lenderId);
				thePair.add(borrowerId);
				expOffers.add(thePair);
			}
			
		} catch (SQLException e) {
			SerenityLoans.log.severe(e.getMessage());
			e.printStackTrace();
		}
		
		for(Vector<UUID> parties : expOffers){
			
			// Remove the offer and the associated terms
			removeOffer(parties.firstElement(), parties.lastElement());
			
			try {
				Thread.sleep((int)Math.floor(Math.random() * 200));
			} catch (InterruptedException e) {
				return;
			}
			
		}
		
		if(SerenityLoans.debugLevel >= 4)
			SerenityLoans.logInfo(String.format("Exiting %s method. %s", "updateAll()",  "Thread: " + Thread.currentThread().getId() + "."));
		
		
	}
	
}
