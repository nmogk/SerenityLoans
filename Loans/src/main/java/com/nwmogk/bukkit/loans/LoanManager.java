/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * 
 * File: LoanManager.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This class performs operations on the Loans table of the mySQL database.
 * It allows calling classes to obtain information about a loan from the
 * database and to apply operations to the loan without having to interact
 * directly with the database. This class also implements the critical loan
 * update function, which checks the database for scheduled loan changes.
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

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;

import org.bukkit.entity.Player;

import com.nwmogk.bukkit.loans.api.FinancialEntity;
import com.nwmogk.bukkit.loans.object.ImmutableOffer;
import com.nwmogk.bukkit.loans.object.Loan;
import com.nwmogk.bukkit.loans.object.PaymentStatement;


public class LoanManager {
	
	/*
	 * This enum defines all of the event types for the loan model
	 */
	private enum LoanEventType {
		COMPOUND, INTERESTACCRUAL, SERVICEFEE, LATEFEE, PAYMENTDUE, PAYMENTMADE, STATEMENTOUT, OPEN, CLOSE, EXTRAPRINCIPALPAID, EXTRAINTERESTPAID, EXTRAFEESPAID;
		
		public static LoanEventType getFromString(String type){
		
			if(type.equalsIgnoreCase("CompoundInterest"))
				return COMPOUND;

			if(type.equalsIgnoreCase("Close"))
				return CLOSE;

			if(type.equalsIgnoreCase("ExtraFeesPaid"))
				return EXTRAFEESPAID;
	
			if(type.equalsIgnoreCase("ExtraInterestPaid"))
				return EXTRAINTERESTPAID;
	
			if(type.equalsIgnoreCase("ExtraPrincipalPaid"))
				return EXTRAPRINCIPALPAID;
		
			if(type.equalsIgnoreCase("AccrueInterest"))
				return INTERESTACCRUAL;
	
			if(type.equalsIgnoreCase("LateFee"))
				return LATEFEE;
		
			if(type.equalsIgnoreCase("Open"))
				return OPEN;
	
			if(type.equalsIgnoreCase("PaymentDue"))
				return PAYMENTDUE;
		
			if(type.equalsIgnoreCase("PaymentMade"))
				return PAYMENTMADE;
		
			if(type.equalsIgnoreCase("ServiceFee"))
				return SERVICEFEE;
		
			if(type.equalsIgnoreCase("StatementOut"))
				return STATEMENTOUT;

			return null;
		}
		
		public String toString(){
			switch(this){
			case COMPOUND:
				return "CompoundInterest";
			case CLOSE:
				return "Close";
			case EXTRAFEESPAID:
				return "ExtraFeesPaid";
			case EXTRAINTERESTPAID:
				return "ExtraInterestPaid";
			case EXTRAPRINCIPALPAID:
				return "ExtraPrincipalPaid";
			case INTERESTACCRUAL:
				return "AccrueInterest";
			case LATEFEE:
				return "LateFee";
			case OPEN:
				return "Open";
			case PAYMENTDUE:
				return "PaymentDue";
			case PAYMENTMADE:
				return "PaymentMade";
			case SERVICEFEE:
				return "ServiceFee";
			case STATEMENTOUT:
				return "StatementOut";
			default:
				return "";
			
			
			}
		}
	};
	
	/*
	 * This class defines an event in the loan model. These events are used to construct the balance of the
	 * loan and determine when events should happen, e.g. interest being accrued, a late fee being assessed.
	 * 
	 * The entire loan should be reconstructable from all of the events. The LoanEvent class is Comparable
	 * by date of event.
	 */
	private class LoanEvent implements Comparable<LoanEvent>{
		
		public Timestamp time;
		public LoanEventType action;
		public int loan;
		public double amount;
		public int loanEventID;
		
		public LoanEvent(Timestamp t, LoanEventType a, double amt, int loanID, int loanEventID ){
			time = t;
			action = a;
			amount = amt;
			loan = loanID;
			this.loanEventID = loanEventID;
		}
		
		public LoanEvent(Timestamp t, LoanEventType a, double amt, int loanID ){
			this(t, a, amt, loanID, 0);
		}
		
		/**
		 * This method causes LoanEvents to be sorted by date, then by LoanEventType
		 */
		public int compareTo(LoanEvent other) {
			return (int)(time.getTime() - other.time.getTime()) - (other.action.ordinal() - action.ordinal());
		}
		
//		public String toString(){
//			String dateStr = DateFormat.getDateInstance().format(time);
//			String actionStr = "";
//			
//			switch(action){
//			
//				case COMPOUND:
//					actionStr = "Compound Interest";
//					break;
//				case PAYMENTDUE:
//					actionStr = "Payment Due Date";
//					break;
//				case INTERESTACCRUAL:
//					actionStr = "Interest Accrued";
//					break;
//				case SERVICEFEE:
//					actionStr = "Service Fee Assessed";
//					break;
//				case STATEMENTOUT:
//					actionStr = "Statement Sent Out";
//					break;
//				case LATEFEE:
//					actionStr = "Late Fee Assessed";
//					break;
//				case PAYMENTMADE:
//					actionStr = "Payment of " + amount + " made to ";
//					switch(account){
//						case ALL:
//							actionStr += "all balances";
//							break;
//						case PRINCIPAL:
//							actionStr += "principal balance";
//							break;
//						case INTEREST:
//							actionStr += "interest balance";
//							break;
//						case FEES:
//							actionStr += "fee balance";
//							break;
//					}
//					break;
//			}
//			
//			return dateStr + ": " + actionStr + "\n";
//		}
	}
	
	private SerenityLoans plugin;
	private String prfx;
	
	public LoanManager(SerenityLoans plugin){
		this.plugin = plugin;
		prfx = Conf.getMessageString();
	}

	/*
	 * Can't return null. Must return an empty array if there are no results found.
	 */
	public Loan[] getLoan(FinancialEntity lender, FinancialEntity borrower) {
		
		Loan[] result = new Loan[]{};
		Vector<Loan> loansFound = new Vector<Loan>();
		
		String querySQL = String.format("SELECT LoanID FROM Loans WHERE LenderID=%d AND BorrowerID=%d ORDER BY LoanID;", lender.getUserID(), borrower.getUserID());
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			ResultSet rs = stmt.executeQuery(querySQL);
			
			while(rs.next()){
				Loan oneLoan = getLoan(rs.getInt("LoanID"));
				
				if(oneLoan != null)
					loansFound.add(oneLoan);
			}
			
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		if(loansFound.size() > 0)
			result = loansFound.toArray(result);
		
		return result;
	}
	
	public Loan getLoan(int loanID){
			
		String querySQL = String.format("SELECT * FROM Loans WHERE LoanID=%d;", loanID);
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			ResultSet rs  = stmt.executeQuery(querySQL);
			
			if(!rs.next()){
				stmt.close();
				return null;
			}
			
			int termsID = rs.getInt("Terms");
			double balance = rs.getDouble("Balance");
			double interestBal = rs.getDouble("InterestBalance");
			double feeBal = rs.getDouble("FeeBalance");
			Timestamp start = rs.getTimestamp("StartDate");
			Timestamp last = rs.getTimestamp("LastUpdate");
			FinancialEntity lender = plugin.playerManager.getFinancialEntity(rs.getInt("LenderID"));
			FinancialEntity borrower = plugin.playerManager.getFinancialEntity(rs.getInt("BorrowerID"));
			
			
			String offerQuery = String.format("SELECT * FROM PreparedOffers WHERE OfferID=%d;", termsID);
			
			ResultSet results = stmt.executeQuery(offerQuery);
			
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
			
			ImmutableOffer offer = new ImmutableOffer(lender, borrower, value, interestRate, lateFee, minPayment, serviceFee, term, compoundingPeriod, gracePeriod, paymentTime, paymentFrequency, serviceFeeFrequency, null);

			Loan theLoan = new Loan(loanID, balance, interestBal, feeBal, offer, start, last);
			
			stmt.close();
			
			return theLoan;
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return null;
	}

	/*
	 * This method runs when payments are due and applies all of the payments
	 * made to the outstanding balances. 
	 */
	public double applyPayment(Loan theLoan, double amount) {
		
		double runningTotal = amount;
		double feeBalance = theLoan.getFeesOutstanding();
		double interestBalance = theLoan.getInterestBalance();
		double balance = theLoan.getBalance();
		
		if(runningTotal >= feeBalance){
			runningTotal -= feeBalance;
			feeBalance = 0;
		}
		else{
			feeBalance -= runningTotal;
			runningTotal = 0;
		}
		
		if(runningTotal >= interestBalance){
			runningTotal -= interestBalance;
			interestBalance = 0;
		}
		else {
			interestBalance -= runningTotal;
			runningTotal = 0;
		}
		
		if(runningTotal > 0){
			if(balance >= runningTotal)
				balance -= runningTotal;	
			else
				balance = 0;
		}
		
		String updateSQL = String.format("UPDATE Loans SET Balance=%f, InterestBalance=%f, FeeBalance=%f WHERE LoanID=%d;", balance, interestBalance, feeBalance, theLoan.getLoanID() );
		
		
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			stmt.executeUpdate(updateSQL);
			
			PaymentStatement ps = getPaymentStatement(theLoan.getLoanID());
			
			if(ps != null){
			
				String updateBill = String.format("UPDATE PaymentStatements SET BillAmountPaid=%f WHERE StatementID=%d;", ps.getActualPaid() + amount - runningTotal, ps.getStatementID());
				stmt.executeUpdate(updateBill);
			}
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		addLoanEvent(new LoanEvent(new Timestamp(new Date().getTime()), LoanEventType.PAYMENTMADE, amount - runningTotal, theLoan.getLoanID()), true);
		
		if(balance == 0){
			addLoanEvent(new LoanEvent(new Timestamp(new Date().getTime()), LoanEventType.CLOSE, theLoan.getValue(), theLoan.getLoanID()), true);
			closeLoan(theLoan.getLoanID());
		}
			
		return runningTotal;
		
	}

	private void closeLoan(int loanID) {
		
		Loan theLoan = getLoan(loanID);
		
		if(theLoan.getCloseValue() > 0)
			return;
		
		addLoanEvent(new LoanEvent(new Timestamp(new Date().getTime()), LoanEventType.CLOSE, 0.0, loanID), true);
		
		String closeSQL = String.format("UPDATE Loans SET Open='false' WHERE LoanID=%d;", loanID);
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			stmt.executeUpdate(closeSQL);
			
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
	}

	public void buildLoanEvents(int loanID) {
		
		Loan theLoan = getLoan(loanID);
		
		if(theLoan == null){
			if(SerenityLoans.debugLevel >= 2)
				SerenityLoans.log.info(String.format("[%s] Loan not found when trying to populate events list.", plugin.getDescription().getName()));
			return;
		}
		
		addLoanEvent(new LoanEvent(new Timestamp(new Date().getTime()), LoanEventType.OPEN, theLoan.getValue(), theLoan.getLoanID()), true);
		
		double paymentAmount = calculatePaymentAmount(theLoan);
		
		//===================================Populate Events List=========================================
		
		// Calculate payment due dates, statement out dates, and latefee assessment times
		long payFrequency = Math.max(theLoan.getPaymentFrequency(), theLoan.getPaymentTime() + theLoan.getGracePeriod());
		
		for(int i = 1; i * payFrequency <= theLoan.getTerm(); i += 1){
			
			Date actionTime = new Date( theLoan.getStartDate().getTime() + i * payFrequency);
			Date feeTime = new Date(actionTime.getTime() + theLoan.getGracePeriod());
			Date statementTime = new Date(actionTime.getTime() - theLoan.getPaymentTime());
			
			addLoanEvent(new LoanEvent(new Timestamp(actionTime.getTime()), LoanEventType.PAYMENTDUE, paymentAmount, theLoan.getLoanID()), false);
			addLoanEvent(new LoanEvent(new Timestamp(feeTime.getTime()), LoanEventType.LATEFEE, theLoan.getLateFee(), theLoan.getLoanID()), false);
			addLoanEvent(new LoanEvent(new Timestamp(statementTime.getTime()), LoanEventType.STATEMENTOUT, paymentAmount, theLoan.getLoanID()), false);
			
			// If the next iteration is strictly greater than the term, add an event for final payoff
			if((i + 1) * payFrequency > theLoan.getTerm() && i * payFrequency != theLoan.getTerm()){
				
				Date actionTime2 = new Date(theLoan.getStartDate().getTime() + theLoan.getTerm());
				Date feeTime2 = new Date(actionTime2.getTime() + theLoan.getGracePeriod());
				Date statementTime2 = new Date(actionTime2.getTime() - theLoan.getPaymentTime());
				
				
				addLoanEvent(new LoanEvent(new Timestamp(actionTime2.getTime()), LoanEventType.PAYMENTDUE, paymentAmount, theLoan.getLoanID()), false);
				addLoanEvent(new LoanEvent(new Timestamp(feeTime2.getTime()), LoanEventType.LATEFEE, theLoan.getLateFee(), theLoan.getLoanID()), false);		
				addLoanEvent(new LoanEvent(new Timestamp(statementTime2.getTime()), LoanEventType.STATEMENTOUT, paymentAmount, theLoan.getLoanID()), false);
				
			}
					
		}
				
			
		// Calculate service fee times
		for(int i = 0; i * theLoan.getServiceFeeFrequency() < theLoan.getTerm() && theLoan.getServiceFeeFrequency() != 0 && theLoan.getServiceFee() != 0; i += 1){
			Date actionTime = new Date(theLoan.getStartDate().getTime() + i * theLoan.getServiceFeeFrequency());
						
			addLoanEvent(new LoanEvent(new Timestamp(actionTime.getTime()), LoanEventType.SERVICEFEE, theLoan.getServiceFee(), theLoan.getLoanID()), false);
		}
			
		//Calculate interest accrual/compounding times
		for(int i = 1; i * theLoan.getCompoundingPeriod() <= theLoan.getTerm() && theLoan.getCompoundingPeriod() != 0; i += 1){
					
			Date actionTime = new Date(theLoan.getStartDate().getTime() + i * theLoan.getCompoundingPeriod());
			
			addLoanEvent(new LoanEvent(new Timestamp(actionTime.getTime()), LoanEventType.COMPOUND, 0.0, theLoan.getLoanID()), false);
//			addLoanEvent(new LoanEvent(new Timestamp(actionTime.getTime() + 1l), LoanEventType.INTERESTACCRUAL, 0.0, theLoan.getLoanID()));
					
			if((i + 1) * theLoan.getCompoundingPeriod() > theLoan.getTerm() && i * theLoan.getCompoundingPeriod() != theLoan.getTerm()){
						
				Date actionTime2 = new Date(theLoan.getStartDate().getTime() + theLoan.getTerm());
				
				addLoanEvent(new LoanEvent(new Timestamp(actionTime2.getTime()), LoanEventType.COMPOUND, 0.0, theLoan.getLoanID()), false);
//				addLoanEvent(new LoanEvent(new Timestamp(actionTime2.getTime() + 1l), LoanEventType.INTERESTACCRUAL, 0.0, theLoan.getLoanID()));
	
			}					
		}
		
	}

	private void addLoanEvent(LoanEvent loanEvent, Boolean executed) {


		String insertSQL = String.format("INSERT INTO LoanEvents(LoanID, EventTime, EventType, Amount, Executed) VALUES (%d, ?, '%s', %f, '%s');", loanEvent.loan, loanEvent.action.toString(), loanEvent.amount, executed.toString());
		
		try {
			PreparedStatement stmt = plugin.conn.prepareStatement(insertSQL);
			
			stmt.setTimestamp(1, loanEvent.time);
			
			stmt.executeUpdate();
			
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
	}

	/*
	 * This method calculates a fixed payment amount so that an amortizing loan
	 * will be paid off with interest by the end of the loan. For all other loan
	 * types, it divides the principal into the number of expected payments.
	 * 
	 * If the payment schedule does not perfectly align with the term, then this 
	 * method assumes there will be one final payment in addition to the regularly
	 * scheduled ones.
	 */
	private double calculatePaymentAmount(Loan theLoan) {
		double r = theLoan.getInterestRate();
		double n = Math.ceil(((double)  theLoan.getTerm())/((double) theLoan.getPaymentFrequency()));
		
		if(theLoan.getLoanType() == LoanType.AMORTIZING)
			return theLoan.getBalance() * (r + (r/(Math.pow(1 + r,n) - 1)));
		else if(theLoan.getLoanType() == LoanType.BULLET)
			return theLoan.getBalance() * (1 + theLoan.getInterestRate() * theLoan.getTerm()/Conf.getIntReportingTime());
		else
			return theLoan.getBalance()/n;
	}

	/**
	 * This method causes Loan to check all of its scheduled events. If it finds a scheduled
	 * event that requires action, it causes the loan to be updated with that action. It
	 * assumes that the current time is the time that should be updated for.
	 */
	public void update(int loanID) {
		Timestamp now = new Timestamp(new Date().getTime());
		
		String toDoQuery = String.format("SELECT * FROM LoanEvents WHERE LoanID=%d AND Executed='false' ORDER BY EventTime;", loanID);
		
		LinkedList<LoanEvent> checklist = new LinkedList<LoanEvent>();
		
		try{
			Statement stmt = plugin.conn.createStatement();
		
			ResultSet events = stmt.executeQuery(toDoQuery);
			
			while(events.next()){
				
				Timestamp ts = events.getTimestamp("EventTime");
				int loanEventID = events.getInt("LoanEventID");
				String eventType = events.getString("EventType");
				double amount = events.getDouble("Amount");
				
				checklist.add(new LoanEvent(ts, LoanEventType.getFromString(eventType), amount, loanID, loanEventID));
				
			}
		
		
//		Collections.sort(checklist);
		
		ListIterator<LoanEvent> it = checklist.listIterator();
		
		while(it.hasNext()){
			
			LoanEvent le = it.next();
			
			if(now.before(le.time))
				break;
			
			accrueInterest(loanID);
			
			switch(le.action) {
				case COMPOUND: 			
					compoundInterest(le);
					break;
										
				case PAYMENTDUE:		
					attemptAutoPay(le);
					creditScoreUpdate(le);
					break;
					
				case LATEFEE:
				case SERVICEFEE:		
					assessFee(le);		
					break;
					
				case STATEMENTOUT:		
					sendOutStatement(le);
					break;
							
				default:
					break;
			}	
		}
		
		// Update Last Updated Time
			String updateTime = String.format("UPDATE Loans SET LastUpdate=NOW() WHERE LoanID=%d;", loanID);
			stmt.executeUpdate(updateTime);
			
			stmt.close();
		
		} catch (SQLException e){
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
	}

	private void attemptAutoPay(LoanEvent le) {
		
		String allowedSQL = String.format("SELECT AutoPay FROM Loans WHERE LoanID=%d;", le.loan);
		boolean doAutoPay = false;
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			ResultSet rs = stmt.executeQuery(allowedSQL);
			
			if(rs.next())
				doAutoPay = Boolean.parseBoolean(rs.getString("AutoPay"));
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		if(!doAutoPay)
			return;
		
		Loan theLoan = getLoan(le.loan);
		PaymentStatement ps = getPaymentStatement(le.loan);
		
		if(! SerenityLoans.econ.has(theLoan.getBorrower(), ps.getMinimumPayment()).callSuccess)
			return;
		
		SerenityLoans.econ.withdraw(theLoan.getBorrower(), ps.getMinimumPayment());
		SerenityLoans.econ.deposit(theLoan.getLender(), ps.getMinimumPayment());
		
		applyPayment(theLoan, ps.getMinimumPayment());
		
	}

	private void sendOutStatement(LoanEvent le) {
		
		Loan theLoan = getLoan(le.loan);
		PaymentStatement ps = getPaymentStatement(le.loan);
		
		boolean percentageRule = false;
		String rulePath = "loan.terms-constraints.min-payment.percent-rule";
		if(plugin.getConfig().contains(rulePath) && plugin.getConfig().isBoolean(rulePath))
			percentageRule = plugin.getConfig().getBoolean(rulePath);

		double statementAmount = Math.min(theLoan.getCloseValue() , le.amount) + theLoan.getFeesOutstanding() + ps.getPaymentRemaining();
		double minPayment = theLoan.getMinPayment() * (percentageRule? le.amount : 1);
		
		Timestamp due = new Timestamp(le.time.getTime() + theLoan.getPaymentTime());
		
		String insertSQL = String.format("INSERT INTO PaymentStatements (LoanID, BillAmount, Minimum, StatementDate, DueDate) VALUES (%d, $f, $f, ?, ?);", le.loan, statementAmount, minPayment);
	
		try {
			
			PreparedStatement prep = plugin.conn.prepareStatement(insertSQL);
			
			prep.setTimestamp(1, le.time);
			prep.setTimestamp(2, due);
			
			prep.executeUpdate();
			
			String updateLE = String.format("UPDATE LoanEvents SET Amount=%f, Executed='true' WHERE LoanEventID=%d;", statementAmount, le.loanEventID);

			Statement stmt = plugin.conn.createStatement();
			
			stmt.executeUpdate(updateLE);
			
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		Player recipient = plugin.playerManager.getPlayer(theLoan.getBorrower().getUserID());
		
		if(recipient == null)
			return;
		
		boolean isPlayer = recipient.getName().equalsIgnoreCase(theLoan.getBorrower().getName());
	
		recipient.sendMessage(String.format("%s %s received a payment statement!", prfx, isPlayer? "You have" : theLoan.getBorrower().getName() + " has"));
		recipient.sendMessage(String.format("%s Use %s to apply payment.", prfx, isPlayer? "/loan": "/crunion"));
		recipient.sendMessage(String.format("%s Details are given below:", prfx));
		recipient.sendMessage(getPaymentStatement(theLoan.getLoanID()).toString(plugin));
		recipient.sendMessage(String.format("%s Use %s statement %s to view this statement again.", prfx, isPlayer? "/loan": "/crunion", theLoan.getLender().getName()));
	}

	private void creditScoreUpdate(LoanEvent le) {
		// TODO Auto-generated method stub
		
	}

	/*
	 * This method handles assessing of fees. It determines the fee type from
	 * the input argument and will dismiss a late fee if the minimum payment
	 * has been made.
	 */
	private void assessFee(LoanEvent le) {
		
		try {
			Statement stmt = plugin.conn.createStatement();
		
			Loan theLoan = getLoan(le.loan);
		
			String updateLE = String.format("UPDATE LoanEvents SET Executed='true' WHERE LoanEventID=%d;", le.loanEventID);
			double newFeeBalance = theLoan.getFeesOutstanding();
			
			if(le.action == LoanEventType.SERVICEFEE){
			
				newFeeBalance += le.amount;
			
				String updateFees = String.format("UPDATE Loans SET FeeBalance=%f WHERE LoanID=%d;", newFeeBalance, theLoan.getLoanID());
				
				stmt.executeUpdate(updateFees);
			} else {
				
				PaymentStatement ps = getPaymentStatement(theLoan.getLoanID());
				
				
				if(ps.getActualPaid() < theLoan.getMinPayment()){
					newFeeBalance += le.amount;
					
					String updateFees = String.format("UPDATE Loans SET FeeBalance=%f WHERE LoanID=%d;", newFeeBalance, theLoan.getLoanID());
					
					stmt.executeUpdate(updateFees);
					
				} else {
					updateLE = String.format("UPDATE LoanEvents SET Amount=0.0, Executed='true' WHERE LoanEventID=%d;", le.loanEventID);
				}
				
			}
			

			stmt.executeUpdate(updateLE);
		
			stmt.close();
			
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
	}

	public PaymentStatement getPaymentStatement(int loanID) {

		String selectSQL = String.format("SELECT * FROM PaymentStatements WHERE LoanID=%d ORDER BY StatementDate DESC;", loanID);
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			ResultSet rs = stmt.executeQuery(selectSQL);
			
			if(!rs.next())
				return null;
			
			int statementID = rs.getInt("StatementID");
			double billAmount = rs.getDouble("BillAmount");
			double minimum = rs.getDouble("Minimum");
			Timestamp statementDate = rs.getTimestamp("StatementDate");
			Timestamp dueDate = rs.getTimestamp("DueDate");
			double amountPaid = rs.getDouble("BillAmountPaid");
			
			return new PaymentStatement(statementID, loanID, billAmount, minimum, statementDate, dueDate, amountPaid);
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
		return null;
	}

	/*
	 * This method compounds the outstanding interest balance. That is, it moves the
	 * interest to the principal, so it will accrue interest. This method is only
	 * used for discreet compounding.
	 */
	private void compoundInterest(LoanEvent le) {
		
		Loan theLoan = getLoan(le.loan);
		
		double compounded = theLoan.getInterestBalance();
		double newBalance = theLoan.getBalance() + compounded;
		
		String updateLoan = String.format("UPDATE Loans SET Balance=%f, InterestBalance=%f WHERE LoanID=%d;", newBalance, 0.0, theLoan.getLoanID());
		String updateLE = String.format("UPDATE LoanEvents SET Amount=%f, Executed='true' WHERE LoanEventID=%d;", compounded, le.loanEventID);
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			stmt.executeUpdate(updateLoan);
			stmt.executeUpdate(updateLE);
			
			stmt.close();
			
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
	}

	private void accrueInterest(int loanID) {
		
		Loan theLoan = getLoan(loanID);
		
		// Calculate time since last event
		
		String eventQuery = String.format("SELECT EventTime FROM LoanEvents WHERE LoanID=%d AND Executed='true' ORDER BY EventTime DESC;", theLoan.getLoanID());
		Timestamp lastTime = theLoan.getStartTime();
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			ResultSet eventsList = stmt.executeQuery(eventQuery);
			
			if(eventsList.next())
				lastTime = eventsList.getTimestamp("EventTime");
			
		
		
		double prorateFactor = ((double)((new Date().getTime()) - lastTime.getTime()))/((double) Conf.getIntReportingTime());
		
		
		double interest;
		
		// Determine continuous or periodic
		
		
		if(theLoan.getCompoundingPeriod() == 0){
			double balance = theLoan.getBalance() * Math.exp(theLoan.getInterestRate() * prorateFactor);
			interest = balance - theLoan.getBalance();
			
			String balanceUpdate = String.format("UPDATE Loan SET Balance=%f WHERE LoanID=%d;", balance, theLoan.getLoanID());
			
			// Update balance directly
			stmt.executeUpdate(balanceUpdate);
		} else {
			interest = theLoan.getBalance() * theLoan.getInterestRate() * prorateFactor;
			double newInterestBalance = interest + theLoan.getInterestBalance();
			
			String interestUpdate = String.format("UPDATE Loan SET InterestBalance=%f WHERE LoanID=%d;", newInterestBalance, theLoan.getLoanID());
			
			// Add to interest balance
			stmt.executeUpdate(interestUpdate);
		}
		
		// Add loanEvent
		
		addLoanEvent(new LoanEvent(new Timestamp(new Date().getTime()), LoanEventType.INTERESTACCRUAL, interest, theLoan.getLoanID()), true);
		
		stmt.close();
		
		} catch (SQLException e) {
			SerenityLoans.log.severe(String.format("[%s] " + e.getMessage(), plugin.getDescription().getName()));
			e.printStackTrace();
		}
		
	}

}
