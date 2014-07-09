/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
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
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.nwmogk.bukkit.loans.api.FinancialEntity;
import com.nwmogk.bukkit.loans.object.ImmutableOffer;

public class OfferManager {
	
	private SerenityLoans plugin;
	
	public enum OfferExitStatus{SUCCESS, IGNORED, UNKNOWN, OVERWRITE_FAIL};
	
	
	public OfferManager(SerenityLoans plugin){
		this.plugin = plugin;
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
		String query = "SELECT * FROM offer_view WHERE LenderID=? AND BorrowerID=?;";
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
		String query = "SELECT LenderID FROM Offers WHERE BorrowerID=?;";
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
}
