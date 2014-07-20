/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * This file is part of the SerenityLoans Bukkit plugin project.
 * 
 * File: CreditHistory.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This class represents a credit history associated with a FinancialEntity
 * object. It stores CreditEvents as a result of calls to any one of the
 * record<_____> methods. This class maintains a credit score based on the
 * recorded history. The credit score is calculated iteratively according to 
 * an exponential moving average (EMA) algorithm. The influence of earlier
 * items decreases as time moves on, but never really disappear completely.
 * 
 * The new score S_{n+1} may be computed according to the following equation:
 * 
 * S_{n+1} = \alpha * Y_n + (1 - \alpha) * S_n
 * 
 * where Y_n is the new item to be added to the calculation sequence and \alpha
 * is the dissipation factor. Higher values of \alpha will cause older events
 * to be "forgotten" sooner.
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

import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.nwmogk.bukkit.loans.Conf.CreditScoreSettings;
import com.nwmogk.bukkit.loans.api.CreditEvent;
import com.nwmogk.bukkit.loans.api.CreditEventFactory;
import com.nwmogk.bukkit.loans.api.CreditEventType;
import com.nwmogk.bukkit.loans.api.CreditScore;
import com.nwmogk.bukkit.loans.api.FinancialEntity;
import com.nwmogk.bukkit.loans.api.Loanable;
import com.nwmogk.bukkit.loans.object.Loan;
import com.nwmogk.bukkit.loans.object.PaymentStatement;


/*
 * Algorithm:
 * 
 * Ignore changes if score range min==score range max
 * 
 * Collect previous score and setting information
 * Determine inactivity updates
 * Determine new score update
 * Apply all updates in order
 * 
 * Scores must be normalized before using them in the algorithm.
 * 
 * Main algorithm:
 * 
 * 	newScore = previousScore * (1 - alpha) + alpha * updateScore
 * 
 * Update scores:
 * 
 * 	payment made         	==> updateScore = 1
 * 	minimum payment made 	==> updateScore = subprimeLimitScore
 * 	payment missed     	 	==> updateScore = 0
 *  account inactivity	 	==> updateScore = max(no history score, inactivity penalty * current score )
 *  credit limit reached 	==> updateScore = credit limit penalty * current score
 *  credit utilization   	==> updateScore = (1 - utlization limit * abs((statement balance)/(credit limit) - credit utilization goal)) * current score
 *  final loan payment		==> updateScore = (1 - current score)(time taken)/term + current score
 *  overpayment penalty		==> updateScore = min(1, 1 - overpayment penalty * ( (amount paid - statement balance)/value)) * current score
 *  bankruptcy				==> newScore = bankruptcy score
 */

/*
 * To display correct sig figs:
 * 
 * double d1 = 1234500;
 * BigDecimal bd1 = new BigDecimal(d1);
 * bd1.round(new MathContext(7)).toPlainString();
 */


public class CreditHistoryManager {
	

	private SerenityLoans plugin;
	
	private Object creditHistoryLock = new Object();
	
	/**
	 * This enum defines all of the types of credit events that the credit history records
	 */
//	public enum CreditEventType{LOANOPEN, LOANCLOSE, PAID_BALANCE, PAID_MINIMUM, MISSED_PAYMENT, LOAN_UTILIZATION, INACTIVITY, CREDIT_UTILIZATION, CREDIT_LIMIT, BANKRUPT, LOAN_MODIFY};

	/*
	 * This class encapsulates all of the information associated with a credit
	 * event. Each event has a date, an applicable loan, a type and a score adjustment.
	 * 
	 * Not all events have an associated loan, that field can occasionally be null.
	 * 
	 * Not all events cause updates to the credit score. These usually input the current
	 * credit score in the score field. The toString() method for CreditEvent reflects
	 * the significance or not of the score field.
	 */
/*	private class GenericCreditEvent{
		
		Loanable loan;
		double score;
		Date date;
		CreditEventType type;
			
		GenericCreditEvent(CreditEventType type, Date date, double score, Loanable loan){
			this.loan = loan;
			this.score = score;
			this.date = date;
			this.type = type;
		}
		
		public String toString(){
			String result = "";
			
			result += DateFormat.getDateInstance().format(date) + ": ";
			
			switch(type){
			case LOANOPEN:
				result += "Opened loan " + System.identityHashCode(loan) + ";";
				break;
			case LOANCLOSE:
				result += "Closed loan " + System.identityHashCode(loan) + ";";
				break;
			case BANKRUPT:
				result += "Declared Bankruptcy. New Score: " + score + ";";
				break;
			case CREDIT_LIMIT:
				result += "Reached credit limit on loan " + System.identityHashCode(loan) + ". Score adjustment: " + score + ";";
				break;
			case CREDIT_UTILIZATION:
				result += "Credit utilization result on loan " + System.identityHashCode(loan) + ". Score adjustment: " + score + ";";
				break;
			case INACTIVITY:
				result += "Inactivity adjustment. Score adjustment: " + score + ";";
				break;
			case LOAN_MODIFY:
				result += "Loan modification;";
				break;
			case MISSED_PAYMENT:
				result += "Missed a payment on loan " + System.identityHashCode(loan) + ". Score adjustment: " + score + ";";
				break;
			case PAID_BALANCE:
				result += "Payment made in full on loan " + System.identityHashCode(loan) + ". Score adjustment: " + score + ";";
				break;
			case PAID_MINIMUM:
				result += "Made at least minimum payment on loan " + System.identityHashCode(loan) + ". Score adjustment: " + score + ";";
				break;
			
			}
			
			return result;
		}
	}
*/
	private class DefaultCreditModel implements CreditScore {

		public double updateScore(double previousScore, double measurement, double parameter, double covariance) {
			return parameter * measurement + (1 - parameter) * previousScore;
		}
		
	}
	
	private class Eventerator implements CreditEventFactory{

		private abstract class AbstractCreditEvent implements CreditEvent {
			private CreditEventType type;
			
			public AbstractCreditEvent(CreditEventType type){this.type = type;}
			
			public double getDissipationFactor(){return alpha;}
			public CreditEventType getType(){return type;}
		}
		
		private PaymentStatement ps = null;
		private Loan theLoan = null;
		
		public Eventerator(){}
		
		public Eventerator(Loan theLoan){this.theLoan = theLoan;}
		
		public Eventerator(PaymentStatement ps, Loan theLoan){this.ps = ps; this.theLoan = theLoan;}
		
		public CreditEvent getStatementlessOverpayment(final Loanable loan, final double amount) {
			return new AbstractCreditEvent(CreditEventType.OVERPAYMENT){
				public double getUpdateScore(double currentScore) {
					return Math.min(1, Conf.getCreditScoreSettings(CreditScoreSettings.OVERPAYMENT_PENALTY) * currentScore * amount/loan.getValue());
				}
			};
		}

		public CreditEvent getCreditEvent(CreditEventType type) {
			
			switch(type){
			
			case BANKRUPT:
				return new AbstractCreditEvent(type){
					public double getUpdateScore(double currentScore) {
						return normalizeScore(Conf.getCreditScoreSettings(CreditScoreSettings.BANKRUPT));
					}
					
					public double getDissipationFactor(){return 1;}
				};
				
			case CREDIT_LIMIT:
				return new AbstractCreditEvent(type){
					public double getUpdateScore(double currentScore) {
						return Conf.getCreditScoreSettings(CreditScoreSettings.CREDIT_LIMIT) * currentScore;
					}
				};
				
			case CREDIT_UTILIZATION:
				if(ps == null)
					return null;
				
				return new AbstractCreditEvent(type){
					public double getUpdateScore(double currentScore) {
						return currentScore * (1 - Conf.getCreditScoreSettings(CreditScoreSettings.UTIL_LIMIT) * Math.abs(ps.getActualPaid()/ps.getBillAmount() - Conf.getCreditScoreSettings(CreditScoreSettings.UTIL_GOAL)));
					}
				};
				
			case INACTIVITY:
				return new AbstractCreditEvent(type){
					public double getUpdateScore(double currentScore) {
						return Math.max(normalizeScore(Conf.getCreditScoreSettings(CreditScoreSettings.NO_HISTORY)), Conf.getCreditScoreSettings(CreditScoreSettings.INACTIVITY) * currentScore);
					}			
				};
				
			case MISSED_PAYMENT:
				return new AbstractCreditEvent(type){
					public double getUpdateScore(double currentScore) {return 0;}
				};
				
			case PAID_BALANCE:
				return new AbstractCreditEvent(type){
					public double getUpdateScore(double currentScore) {return 1;}
				};
				
			case PAID_MINIMUM:
				return new AbstractCreditEvent(type){
					public double getUpdateScore(double currentScore) {
						return normalizeScore(Conf.getCreditScoreSettings(CreditScoreSettings.SUBPRIME));
					}
				};
				
			case FINAL_PAYMENT:
				if(theLoan == null)
					return null;
			
				return new AbstractCreditEvent(type){
					public double getUpdateScore(double currentScore) {
						return (1 - currentScore) * ((double) (new Date().getTime() - theLoan.getStartTime().getTime()) ) / ((double)theLoan.getTerm()) + currentScore;
					}
				};
				
			case OVERPAYMENT:
				if(ps == null || theLoan == null)
					return null;
				
				return new AbstractCreditEvent(type){
					public double getUpdateScore(double currentScore) {
						return Math.min(1, Conf.getCreditScoreSettings(CreditScoreSettings.OVERPAYMENT_PENALTY) * currentScore * (ps.getActualPaid() - ps.getBillAmount())/theLoan.getValue());
					}
				};
				
			case LOANCLOSE:
			case LOANOPEN:
			case LOAN_MODIFY:
			default:
				return new AbstractCreditEvent(type){
					public double getUpdateScore(double currentScore) {return currentScore;}
					public double getDissipationFactor() {return 0;}
				};
			
			}
			
		}
		
	}
	
	private double scoreMax;
	private double scoreMin;
	
	private double alpha;
	
	private CreditScore scoreModel;
	
	/**
	 * This method is the default constructor for a CreditHistory object. It
	 * initializes the list array and applies a default credit score.
	 */
	public CreditHistoryManager(SerenityLoans plugin){
		
		this.plugin = plugin;
		scoreModel = new DefaultCreditModel();
		
		String getInfo = "SELECT CRscore_max, CRscore_min FROM Info;";
		double lastScoreMax = 0;
		double lastScoreMin = 0;
		
		try {
			Statement stmt = this.plugin.conn.createStatement();
			
			ResultSet rs = stmt.executeQuery(getInfo);
			
			if(rs.next()){
				lastScoreMax = rs.getDouble(1);
				lastScoreMin = rs.getDouble(2);
			}
			
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.logFail(e.getMessage());
			e.printStackTrace();
		}
		
		// Gather parameters from config file
		scoreMax = Conf.getCreditScoreSettings(CreditScoreSettings.RANGE_MAX);
		scoreMin = Conf.getCreditScoreSettings(CreditScoreSettings.RANGE_MIN);
		alpha = Conf.getCreditScoreSettings(CreditScoreSettings.ALPHA);
		
		if(lastScoreMax == 0 && lastScoreMin == 0){
			
			String writeScoreSQL = String.format("UPDATE Info SET CRscore_max=%f, CRscore_min=%f;", scoreMax, scoreMin);
			
			try {
				Statement stmt = this.plugin.conn.createStatement();
				stmt.executeUpdate(writeScoreSQL);
				stmt.close();
			} catch (SQLException e) {
				SerenityLoans.logFail(e.getMessage());
				e.printStackTrace();
			}
	
		} else if (lastScoreMax != scoreMax || lastScoreMin != scoreMin)
			migrateScores(lastScoreMax, lastScoreMin);
		
		
		

	}
	
	/**
	 * This method returns a numeric representation of the credit score. The credit
	 * score is stored internally as a double between the values of 0 and 1. The 
	 * range of the credit score is specified in the config.yml file. This method
	 * reports the result after having converted it to the specified range.
	 * 
	 * The config.yml file also specifies reported significant figures. This method
	 * respects this setting and only reports the relevant significant figures. For
	 * the purposes of this method, trailing zeros before the decimal sign are
	 * considered significant.
	 * 
	 * @return
	 */
	public String formatCreditScore(double scaledScore) {
		
		int sigFigs = (int)Conf.getCreditScoreSettings(CreditScoreSettings.SIG_FIGS);
		
		BigDecimal inputScore = new BigDecimal(scaledScore);
		
		return inputScore.round(new MathContext(sigFigs)).toString();
	}
	
	public CreditEvent getInactivityEvent(){
		return new Eventerator().getCreditEvent(CreditEventType.INACTIVITY);
	}
	
	/**
	 * This method returns a string stating whether the credit score is "good"
	 * or "bad." In the terms of finance, this translates to "Prime" and "Subprime."
	 * The score at which the transition is made is set in the config file.
	 * 
	 * @return
	 */
	public String getInterpretation(FinancialEntity entity){
		
		if(entity.getCreditScore() < Conf.getCreditScoreSettings(CreditScoreSettings.SUBPRIME))
			return "Subprime";
		else
			return "Prime";
		
	}

	/**
	 * This method records a bankruptcy event in the credit history and updates
	 * the credit score to match the bankruptcy score in the config file. It
	 * checks to see if bankruptcies are allowed first.
	 */
	public void recordBankruptcy(FinancialEntity entity){

		if(entity == null)
			return;
		
		LinkedList<CreditEvent> eventsList = new LinkedList<CreditEvent>();
		Eventerator evtr = new Eventerator();
		
		String historySQL = String.format("SELECT EventTime FROM CreditHistory WHERE UserID='%s' ORDER BY EventTime DESC;", entity.getUserID().toString());
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			ResultSet rs = null;
			synchronized(creditHistoryLock){
				rs = stmt.executeQuery(historySQL);
			}
			
			if(rs.next()){
				Timestamp ts = rs.getTimestamp(1);
				
				long timeAgo = new Date().getTime() - ts.getTime();
				
				int numWeeks = (int) Math.floor(((double)timeAgo) / Conf.getCreditScoreSettings(CreditScoreSettings.TAU));
				
				for(int i = 0; i < numWeeks; i++)
					eventsList.add(evtr.getCreditEvent(CreditEventType.INACTIVITY));
			}
				
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.logFail(e.getMessage());
			e.printStackTrace();
		}
		
		CreditEvent event = evtr.getCreditEvent(CreditEventType.BANKRUPT);
		double currentScore = updateScore(entity, eventsList);
		
		
		eventsList.clear();
		eventsList.add(event);
		
		String insertSQL = String.format("INSERT INTO CreditHistory (UserID, EventType, ScoreValue, Parameter) VALUES ('%s', '%s', %f, %f)", entity.getUserID().toString(), event.getType().toString(), event.getUpdateScore(currentScore), event.getDissipationFactor());
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			stmt.executeUpdate(insertSQL);
			
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.logFail(e.getMessage());
			e.printStackTrace();
		}
		
		updateScore(entity, eventsList);
	}

	/**
	 * This method records a loan opening event
	 * 
	 * @param loan
	 */
	public void recordBorrow(FinancialEntity entity, Loanable loan){


		if(entity == null || loan == null)
			return;
		
		LinkedList<CreditEvent> eventsList = new LinkedList<CreditEvent>();
		Eventerator evtr = new Eventerator();
		
		String historySQL = String.format("SELECT EventTime FROM CreditHistory WHERE UserID='%s' ORDER BY EventTime DESC;", entity.getUserID().toString());
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			ResultSet rs = null;
			synchronized(creditHistoryLock){
				rs = stmt.executeQuery(historySQL);
			}
			
			if(rs.next()){
				Timestamp ts = rs.getTimestamp(1);
				
				long timeAgo = new Date().getTime() - ts.getTime();
				
				int numWeeks = (int) Math.floor(((double)timeAgo) / Conf.getCreditScoreSettings(CreditScoreSettings.TAU));
				
				for(int i = 0; i < numWeeks; i++)
					eventsList.add(evtr.getCreditEvent(CreditEventType.INACTIVITY));
			}
				
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.logFail(e.getMessage());
			e.printStackTrace();
		}
		
		CreditEvent event = evtr.getCreditEvent(CreditEventType.LOANOPEN);
		double currentScore = updateScore(entity, eventsList);
		
		
		eventsList.clear();
		eventsList.add(event);
		
		String insertSQL = String.format("INSERT INTO CreditHistory (UserID, EventType, ScoreValue, Parameter) VALUES ('%s', '%s', %f, %f)", entity.getUserID().toString(), event.getType().toString(), event.getUpdateScore(currentScore), event.getDissipationFactor());
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			stmt.executeUpdate(insertSQL);
			
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.logFail(e.getMessage());
			e.printStackTrace();
		}
		
		updateScore(entity, eventsList);
		
	}
	
	/**
	 * This method records a loan modification event. Currently, nothing is done
	 * with the inputs, and no attempt is made to show a connection between old
	 * and new loans. An event will be recorded in history, but only shown as a
	 * generic modification.
	 * 
	 * @param oldLoan
	 * @param newLoan
	 */
	public void recordLoanModification(FinancialEntity entity, Loanable loan){
		if(entity == null || loan == null)
			return;
		
		LinkedList<CreditEvent> eventsList = new LinkedList<CreditEvent>();
		Eventerator evtr = new Eventerator();
		
		String historySQL = String.format("SELECT EventTime FROM CreditHistory WHERE UserID='%s' ORDER BY EventTime DESC;", entity.getUserID().toString());
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			ResultSet rs = null;
			synchronized(creditHistoryLock){
				rs = stmt.executeQuery(historySQL);
			}
			
			if(rs.next()){
				Timestamp ts = rs.getTimestamp(1);
				
				long timeAgo = new Date().getTime() - ts.getTime();
				
				int numWeeks = (int) Math.floor(((double)timeAgo) / Conf.getCreditScoreSettings(CreditScoreSettings.TAU));
				
				for(int i = 0; i < numWeeks; i++)
					eventsList.add(evtr.getCreditEvent(CreditEventType.INACTIVITY));
			}
				
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.logFail(e.getMessage());
			e.printStackTrace();
		}
		
		CreditEvent event = evtr.getCreditEvent(CreditEventType.LOAN_MODIFY);
		double currentScore = updateScore(entity, eventsList);
		
		
		eventsList.clear();
		eventsList.add(event);
		
		String insertSQL = String.format("INSERT INTO CreditHistory (UserID, EventType, ScoreValue, Parameter) VALUES ('%s', '%s', %f, %f)", entity.getUserID().toString(), event.getType().toString(), event.getUpdateScore(currentScore), event.getDissipationFactor());
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			stmt.executeUpdate(insertSQL);
			
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.logFail(e.getMessage());
			e.printStackTrace();
		}
		
		updateScore(entity, eventsList);
	}
	
	/**
	 * This method records the missing of a payment. It is equivalent to
	 * calling recordPayment(bill, 0.0)
	 * 
	 * @param bill
	 */
/*	public void recordMissedPayment(PaymentStatement bill){
		recordPayment(bill, 0.0);
	}
*/
	
	/**
	 * This method records a payment made to a loan. This method may generate
	 * several different CreditEvent types including those that represent payment
	 * in full, partial payment, or no payment. In addition to the payment events
	 * it will also produce credit score events relating to credit utilization
	 * and credit limit. 
	 * 
	 * This method overloads recordPayment(PaymentStatement, double) by simply
	 * calling the other method with bill.getActualPaid() as the amount. This
	 * method will simply return if bill is null. If there is no applicable
	 * PaymentStatement, recordPayment(double, Loanable) should be used instead.
	 * This is the preferred method for all other calls.
	 * 
	 * 
	 * @param bill
	 */
	public void recordPayment(PaymentStatement bill){
		if(bill == null)
			return;
		
		// Possibilities: overpayment, final payment, partial payment, missed payment, full payment
		// Outline:
		//      1) determine case
		//	    2) write CreditHistory entry
		//		3) Update score
		
		LinkedList<CreditEvent> eventsList = new LinkedList<CreditEvent>();
		Eventerator evtr = new Eventerator(bill, plugin.loanManager.getLoan(bill.getLoanID()));
		
		Loan loan = plugin.loanManager.getLoan(bill.getLoanID());
		FinancialEntity borrower = loan.getBorrower();
		String historySQL = String.format("SELECT EventTime FROM CreditHistory WHERE UserID='%s' ORDER BY EventTime DESC;", borrower.getUserID().toString());
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			ResultSet rs = null;
			synchronized(creditHistoryLock){
				rs = stmt.executeQuery(historySQL);
			}
			
			if(rs.next()){
				Timestamp ts = rs.getTimestamp(1);
				
				long timeAgo = new Date().getTime() - ts.getTime();
				
				int numWeeks = (int) Math.floor(((double)timeAgo) / Conf.getCreditScoreSettings(CreditScoreSettings.TAU));
				
				for(int i = 0; i < numWeeks; i++)
					eventsList.add(evtr.getCreditEvent(CreditEventType.INACTIVITY));
			}
				
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.logFail(e.getMessage());
			e.printStackTrace();
		}
		
		CreditEvent event = null;
		double currentScore = updateScore(borrower, eventsList);
		
		if (loan.getBalance() <= 0)
			event = evtr.getCreditEvent(CreditEventType.FINAL_PAYMENT);
		else if (bill.getActualPaid() > bill.getBillAmount())
			event = evtr.getCreditEvent(CreditEventType.OVERPAYMENT);
		else if (bill.getActualPaid() == bill.getBillAmount())
			event = evtr.getCreditEvent(CreditEventType.PAID_BALANCE);
		else if(bill.getActualPaid() >= bill.getMinimumPayment())
			event = evtr.getCreditEvent(CreditEventType.PAID_MINIMUM);
		else
			event = evtr.getCreditEvent(CreditEventType.MISSED_PAYMENT);
		
		eventsList.clear();
		eventsList.add(event);
		
		String insertSQL = String.format("INSERT INTO CreditHistory (UserID, EventType, ScoreValue, Parameter) VALUES ('%s', '%s', %f, %f)", borrower.getUserID().toString(), event.getType().toString(), event.getUpdateScore(currentScore), event.getDissipationFactor());
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			stmt.executeUpdate(insertSQL);
			
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.logFail(e.getMessage());
			e.printStackTrace();
		}
		
		updateScore(borrower, eventsList);
	
	}
	

	/**
	 * This method records a payment that is not associated with a statement. 
	 * It should not be used if a statement is available. It will apply the
	 * overpayment penalty factor to the history. This method should also not
	 * be applied to credit cards.
	 * 
	 * @param amount
	 * @param loan
	 */
	public void recordPayment(double amount, Loan loan){
		// If null nothing to do
		if(loan == null || amount < 0)
			return;
		
		LinkedList<CreditEvent> eventsList = new LinkedList<CreditEvent>();
		Eventerator evtr = new Eventerator(loan);
		
		FinancialEntity borrower = loan.getBorrower();
		String historySQL = String.format("SELECT EventTime FROM CreditHistory WHERE UserID='%s' ORDER BY EventTime DESC;", borrower.getUserID().toString());
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			ResultSet rs = null;
			synchronized(creditHistoryLock){
				rs = stmt.executeQuery(historySQL);
			}
			
			if(rs.next()){
				Timestamp ts = rs.getTimestamp(1);
				
				long timeAgo = new Date().getTime() - ts.getTime();
				
				int numWeeks = (int) Math.floor(((double)timeAgo) / Conf.getCreditScoreSettings(CreditScoreSettings.TAU));
				
				for(int i = 0; i < numWeeks; i++)
					eventsList.add(evtr.getCreditEvent(CreditEventType.INACTIVITY));
			}
				
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.logFail(e.getMessage());
			e.printStackTrace();
		}
		
		CreditEvent event = null;
		double currentScore = updateScore(borrower, eventsList);
		
		if (loan.getBalance() <= 0)
			event = evtr.getCreditEvent(CreditEventType.FINAL_PAYMENT);
		else
			event = evtr.getStatementlessOverpayment(loan, amount);
		
		eventsList.clear();
		eventsList.add(event);
		
		String insertSQL = String.format("INSERT INTO CreditHistory (UserID, EventType, ScoreValue, Parameter) VALUES ('%s', '%s', %f, %f)", borrower.getUserID().toString(), event.getType().toString(), event.getUpdateScore(currentScore), event.getDissipationFactor());
		
		try {
			Statement stmt = plugin.conn.createStatement();
			
			stmt.executeUpdate(insertSQL);
			
			stmt.close();
		} catch (SQLException e) {
			SerenityLoans.logFail(e.getMessage());
			e.printStackTrace();
		}
		
		updateScore(borrower, eventsList);
	}
	
	
	
	private void migrateScores(double lastScoreMax, double lastScoreMin) {
		FinancialEntity[] list = plugin.playerManager.getFinancialEntities();
		
		for(FinancialEntity fe : list){
			double oldScore = fe.getCreditScore();
			double normScore = (oldScore - lastScoreMin)/(lastScoreMax - lastScoreMin);
			double newScore = normScore * (scoreMax - scoreMin) + scoreMin;
			
			plugin.playerManager.setCreditScore(fe, newScore);
		}
	}

	private double normalizeScore(double rawScore){
		return (rawScore - scoreMin)/(scoreMax - scoreMin);
	}
	
	private double scaleScore(double normalScore){
		return normalScore * (scoreMax - scoreMin) + scoreMin;
	}

	/*
	 * This method applies the exponential moving average function to the credit score.
	 * It computes the results iteratively from the previous score and the new element
	 * to be added to the average. 
	 * 
	 * It is the responsibility of the caller to set up the list of credit events 
	 * correctly.
	 */
	private double updateScore(FinancialEntity entity, List<CreditEvent> newScoreItems){
		FinancialEntity toCheck = plugin.playerManager.getFinancialEntity(entity.getUserID());
		
		double score = normalizeScore(toCheck.getCreditScore());
		double dissipationFactor = alpha;
		
		for(CreditEvent ce : newScoreItems){
			dissipationFactor = ce.getDissipationFactor();
			score = scoreModel.updateScore(score, ce.getUpdateScore(score), dissipationFactor, 0);
		}
		
		plugin.playerManager.setCreditScore(toCheck, scaleScore(score));
		
		return score;
	}

}
