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
	
	public OfferManager(SerenityLoans plugin){
		this.plugin = plugin;
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
			
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return result;
		
	}
}
