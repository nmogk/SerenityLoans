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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;

import com.nwmogk.bukkit.loans.api.FinancialEntity;
import com.nwmogk.bukkit.loans.exception.InvalidLoanTermsException;
import com.nwmogk.bukkit.loans.object.ImmutableOffer;

public class OfferManager {
	
	private SerenityLoans plugin;
	// TODO make thread-safe, + comments
	public enum OfferExitStatus{SUCCESS, IGNORED, UNKNOWN, OVERWRITE_FAIL};
	
	private Object offerTableLock = new Object();
	
	public OfferManager(SerenityLoans plugin){
		this.plugin = plugin;
	}
	
	public void buildFinancialEntityInitialOffers(UUID playerID) {
		
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
	
	public OfferExitStatus createOffer(UUID lenderID, UUID borrowerID, String preparedOfferName, Timestamp offerExpiry){
		if(plugin.playerManager.isIgnoring(borrowerID, lenderID))
			return OfferExitStatus.IGNORED;
		
		String columns = "LenderID, OfferName, Value, InterestRate, Term, CompoundingPeriod, GracePeriod, PaymentTime, PaymentFrequency, LateFee, MinPayment, ServiceFeeFrequency, ServiceFee, LoanType";
		String copyColumns = "copy.Value, copy.InterestRate, copy.Term, copy.CompoundingPeriod, copy.GracePeriod, copy.PaymentTime, copy.PaymentFrequency, copy.LateFee, copy.MinPayment, copy.ServiceFeeFrequency, copy.ServiceFee, copy.LoanType";
		
		
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
			
			int output = offerTermsCopier.executeUpdate();
			
			if(output != 1)
				return OfferExitStatus.UNKNOWN;
			
			PreparedStatement offerIdFinder = plugin.conn.prepareStatement(offerQuery);
			
			offerIdFinder.setString(1, lenderID.toString());
			
			ResultSet newOfferId = offerIdFinder.executeQuery();
			
			if(!newOfferId.next()){
				return OfferExitStatus.UNKNOWN;
			}
			
			int offerId = newOfferId.getInt(1);
			
			PreparedStatement renameOffer = plugin.conn.prepareStatement(offerNameUpdate);
			
			renameOffer.setString(1, lenderID.toString());
			
			output = renameOffer.executeUpdate();
			
			if(output != 1)
				return OfferExitStatus.UNKNOWN;
			
			PreparedStatement deleteOldOfferSQL = plugin.conn.prepareStatement(deleteOldOffer);
			PreparedStatement checkDeletedSQL = plugin.conn.prepareStatement(checkDeleted);
			
			deleteOldOfferSQL.setString(1, lenderID.toString());
			deleteOldOfferSQL.setString(2, borrowerID.toString());
			checkDeletedSQL.setString(1, lenderID.toString());
			checkDeletedSQL.setString(2, borrowerID.toString());
			
			output = deleteOldOfferSQL.executeUpdate();
			ResultSet shouldBeEmpty = checkDeletedSQL.executeQuery();
			
			if(output != 1 || output != 0 || shouldBeEmpty.next())
				return OfferExitStatus.OVERWRITE_FAIL;
			
			String sentOfferString = String.format("INSERT INTO Offers (LenderID, BorrowerID, ExpirationDate, PreparedTerms) VALUES (?, ?, ?, %d);", offerId);
			
			PreparedStatement buildOffer = plugin.conn.prepareStatement(sentOfferString);
			
			buildOffer.setString(1, lenderID.toString());
			buildOffer.setString(2, borrowerID.toString());
			buildOffer.setTimestamp(3, offerExpiry);
			
			output = buildOffer.executeUpdate();
			
			if(output != 1)
				return OfferExitStatus.UNKNOWN;
			
			offerTermsCopier.close();
			offerIdFinder.close();
			renameOffer.close();
			deleteOldOfferSQL.close();
			checkDeletedSQL.close();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return OfferExitStatus.SUCCESS;
	}
	
	public ImmutableOffer getOffer(UUID lenderID, UUID borrowerID){
		return getOffer(lenderID, borrowerID, false);
	}
	
	public ImmutableOffer getOffer(UUID lenderID, UUID borrowerID, boolean filterSent){
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
			
			results = stmt2.executeQuery();
			
			int termsID = results.getInt(1);
			
			offer = new ImmutableOffer(lender, borrower, value, interestRate, lateFee, minPayment, serviceFee, term, compoundingPeriod, gracePeriod, paymentTime, paymentFrequency, serviceFeeFrequency, null, expDate, termsID);

			
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return offer;
	}
	
	public List<FinancialEntity> getOfferRecipientsFrom(UUID lenderID){
		String query = "SELECT BorrowerID FROM Offers WHERE LenderID=?;";
		LinkedList<FinancialEntity> list = new LinkedList<FinancialEntity>();
		
		try {
			PreparedStatement stmt = plugin.conn.prepareStatement(query);
			
			stmt.setString(1, lenderID.toString());
			
			ResultSet results = stmt.executeQuery();
			
			while(results.next())
				list.add(plugin.playerManager.getFinancialEntity(UUID.fromString(results.getString("BorrowerID"))));
			
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return list;
		
	}
	
	public List<FinancialEntity> getOfferSendersTo(UUID borrowerID){
		return getOfferSendersTo(borrowerID, false);
	}
	
	public List<FinancialEntity> getOfferSendersTo(UUID borrowerID, boolean filterSent){
		String query = String.format("SELECT LenderID FROM Offers WHERE BorrowerID=?%s;", filterSent? " AND Sent='false'" : "");
		LinkedList<FinancialEntity> list = new LinkedList<FinancialEntity>();
		
		try {
			PreparedStatement stmt = plugin.conn.prepareStatement(query);
			
			stmt.setString(1, borrowerID.toString());
			
			ResultSet results = stmt.executeQuery();
			
			while(results.next())
				list.add(plugin.playerManager.getFinancialEntity(UUID.fromString(results.getString("LenderID"))));
			
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return list;
	}
	
	public ImmutableOffer getPreparedOffer(UUID lenderId, String offerName){
		String query = "SELECT * FROM PreparedOffers WHERE LenderID=? AND OfferName=?;";
		ImmutableOffer offer = null;
		
		try {
			PreparedStatement stmt = plugin.conn.prepareStatement(query);
			
			stmt.setString(1, lenderId.toString());
			stmt.setString(2, offerName);
			
			ResultSet results = stmt.executeQuery();
			
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
			
			offer = new ImmutableOffer(lender, borrower, value, interestRate, lateFee, minPayment, serviceFee, term, compoundingPeriod, gracePeriod, paymentTime, paymentFrequency, serviceFeeFrequency, null, expDate, termsID);

			
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return offer;
	}
	
	public ImmutableOffer getPreparedOffer(int offerId, FinancialEntity lender, FinancialEntity borrower){
		String query = String.format("SELECT * FROM PreparedOffers WHERE OfferID=%d;", offerId);
		ImmutableOffer offer = null;
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			ResultSet results = stmt.executeQuery(query);
			
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
		
		return offer;
	}
	

	public double getTermsValue(int preparedTermsId){
		String query = String.format("SELECT Value FROM PreparedOffers WHERE OfferID=%d;", preparedTermsId);
		double result = -1;
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			ResultSet res = stmt.executeQuery(query);
			
			if(!res.next())
				return result;
			
			result = res.getDouble(1);
			
			stmt.close();
			
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return result;
		
	}
	
	public boolean registerOfferSend(UUID lenderId, UUID borrowerId){
		String sentUpdate = "UPDATE Offers SET Sent='true' WHERE LenderID=? AND BorrowerID=?;";

		try {
			
			PreparedStatement stmt = plugin.conn.prepareStatement(sentUpdate);
			
			stmt.setString(1, lenderId.toString());
			stmt.setString(2, borrowerId.toString());
			
			int result = stmt.executeUpdate();
			
			if(result != 1){
				stmt.close();
				return false;
			}
			
			stmt.close();
					
			return true;
					
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return false;
	}
	
	public boolean removeOffer(UUID lenderId, UUID borrowerId){
		String update = "DELETE FROM Offers WHERE LenderID=? AND BorrowerID=?;";
		int exit = -1;
		
		try {
			PreparedStatement ps = plugin.conn.prepareStatement(update);
			
			ps.setString(1, lenderId.toString());
			ps.setString(2, borrowerId.toString());
			
			exit = ps.executeUpdate();
			
			ps.close();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
			return false;
		}
		
		return exit == 0 || exit == 1;
		
	}
	
	public boolean setTerms(UUID lenderId, boolean isDefault, String argument) throws InvalidLoanTermsException{
		String[] parsedArg = argument.split("=");
		
		if(parsedArg.length != 2)
			return false;
		
		String updateColumn = null;
		String objective = null;
		
		FileConfiguration config = plugin.getConfig();
		
		try{
			
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
			
		} else if(parsedArg[0].equalsIgnoreCase("InterestRate")){
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
			
		} else if(parsedArg[0].equalsIgnoreCase("PaymentTime")){
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
			
		} else if(parsedArg[0].equalsIgnoreCase("MinPayment")){
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
			if(s.equals("Amortizing") || s.equals("InterestOnly") || s.equals("FixedFee") || s.equals("Bullet") || s.equals("Credit") || s.equals("Gift") || s.equals("Bond") || s.equals("Deposit") || s.equals("Salary"))
				objective = "'" + s + "'";
			else
				return false;
			
		} else
			return false;
		
		
	
		
		
		} catch(NumberFormatException e){
			// This will catch improperly formatted input
			return false;
		}
		
		if(objective == null)
			return false;
		
		String updateSQL = String.format("UPDATE PreparedOffers SET %s=%s WHERE LenderID=? AND OfferName='%s';", updateColumn, objective,  isDefault? "default":"prepared" );
		
		
		try {
			PreparedStatement stmt = plugin.conn.prepareStatement(updateSQL);
			
			stmt.setString(1, lenderId.toString());
			
			int result = stmt.executeUpdate();
			
			stmt.close();
			
			return result == 1;
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return false;
	}
	
	public void updateAll(){
		
		String query = "SELECT LenderID, BorrowerID FROM Offers WHERE ExpirationDate < NOW();";
		HashMap<UUID, UUID> expiredOffers = new HashMap<UUID, UUID>();
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			ResultSet rs = null;
			
			synchronized(offerTableLock){
				rs = stmt.executeQuery(query);
			}
			
			while(rs.next()){
				UUID lenderId = UUID.fromString(rs.getString("LenderID"));
				UUID borrowerId = UUID.fromString(rs.getString("BorrowerID"));
				expiredOffers.put(lenderId, borrowerId);
			}
			
		} catch (SQLException e) {
			SerenityLoans.log.severe(e.getMessage());
			e.printStackTrace();
		}
		
		for(UUID lenderId : expiredOffers.keySet()){
			
			removeOffer(lenderId, expiredOffers.get(lenderId));
			
			try {
				Thread.sleep((int)Math.floor(Math.random() * 200));
			} catch (InterruptedException e) {
				return;
			}
			
		}
		
	}
	
}
