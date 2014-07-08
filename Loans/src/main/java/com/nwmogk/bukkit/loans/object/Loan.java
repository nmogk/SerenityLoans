/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * 
 * File: Loan.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This class provides information relating to loans as they are contained
 * in the Loans table of the mySQL database. Instances of this class are
 * immutable and cannot be changed after creation, even if the database has
 * been modified.
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

package com.nwmogk.bukkit.loans.object;

import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedList;

import org.apache.commons.lang.ArrayUtils;

import com.nwmogk.bukkit.loans.Conf;
import com.nwmogk.bukkit.loans.LoanType;
import com.nwmogk.bukkit.loans.Loanable;
import com.nwmogk.bukkit.loans.SerenityLoans;
import com.nwmogk.bukkit.loans.api.FinancialEntity;
import com.nwmogk.bukkit.loans.exception.InsufficientCashException;
import com.nwmogk.bukkit.loans.exception.LoanException;


public class Loan implements Loanable {
	
	private int loanID;
	private double balance;				// Outstanding principle unpaid (plus compounded interest)
	private double interestBalance;		// Balance of outstanding interest
	private double feeBalance;			// Balance of outstanding fees
	private ImmutableOffer terms;
	private Timestamp startDate;
	private Timestamp lastUpdate;

	private LinkedList<PaymentStatement> paymentQueue;
	
	//TODO finish making this class immutable
	
	public Loan(int loanID, double balance, double interestBalance, double feeBalance, ImmutableOffer terms, Timestamp startDate, Timestamp lastUpdate){
		this.loanID = loanID;
		this.balance = balance;
		this.interestBalance = interestBalance;
		this.feeBalance = feeBalance;
		this.terms = terms;
		this.startDate = startDate;
		this.lastUpdate = lastUpdate;
	}
	

	/**
	 * This method instructs the payment of the remainder of the loan. It returns whether the action
	 * was successful or not. If successful, this will close the loan.
	 * @throws InsufficientCashException 
	 */
	public boolean payoff() throws InsufficientCashException {
		// TODO do something about this method - remove it?
//		previousState = new LoanState(this);
		
		double interestAddition =  getInterestRate() * balance;
		
		double amount = balance + interestAddition + interestBalance + feeBalance;
		
		if(getBorrower() == null || getLender() == null)
			throw new LoanException(this);
		if(!SerenityLoans.econ.has(getBorrower(), amount).callSuccess){
			throw new InsufficientCashException(getBorrower(), "Borrower has insufficient cash to payoff.");
		}
		
		interestBalance += interestAddition;
		
		SerenityLoans.econ.withdraw(getBorrower(), amount);
		SerenityLoans.econ.deposit(getLender(), amount);
		
		balance = 0;
		interestBalance = 0;
		feeBalance = 0;
		

		
//		borrower.closeLoan(this);
//		lender.closeLoan(this);
		
//		newState = new LoanState(this);
		
		return true;
		
	}

	public FinancialEntity getLender() {return terms.getLender();}

	public FinancialEntity getBorrower() {return terms.getBorrower();}

	public double getValue() {return terms.getValue();}

	public double getInterestRate() {return terms.getInterestRate();}

	public double getFeesOutstanding() {return feeBalance;}

	public long getTerm() {return terms.getTerm();}

	public Timestamp getStartDate(){return (Timestamp)startDate.clone();}

	public long getCompoundingPeriod() {return terms.getCompoundingPeriod();}

	public long getGracePeriod() {return terms.getGracePeriod();}

	public long getPaymentTime() {return terms.getPaymentTime();}

	public long getPaymentFrequency() {return terms.getPaymentFrequency();}

	public double getLateFee() {return terms.getLateFee();}

	public double getMinPayment() {return terms.getMinPayment();}

//	public Timestamp getCloseDate() {return (Timestamp)endDate.clone();}

	public long getServiceFeeFrequency() {return terms.getServiceFeeFrequency();}

	public double getServiceFee() {return terms.getServiceFee();}
	
	public double getBalance() {return balance;}

	public PaymentStatement getOutstandingBill() {return paymentQueue.pollFirst();}

	public LoanType getLoanType() {return terms.getLoanType();}


	/**
	 * This allows knowing what the current interest balance is. It 
	 * is currently for the LoanState class to be able to recover
	 * the state of the loan. It is possible that this method may be
	 * made public in the future.
	 * 
	 * @return
	 */
	public double getInterestBalance(){
		return interestBalance;
	}
	
	/**
	 * This method returns the date in which the loan was last updated.
	 * It is primarily intended to reconstruct the state of the loan
	 * from a LoanState object. 
	 * 
	 * @return
	 */
	public Date getLastUpdate(){
		return lastUpdate;
	}
	
	
//	/**
//	 * This method causes the loan to be simulated, ignoring the actual timing of events. It returns an array
//	 * of event messages with updates to the balances of the loan. This method restores everything to its original
//	 * state after the test. For debugging purposes only.
//	 * 
//	 * @return
//	 */
//	@SuppressWarnings("unchecked")
//	public String[] simulateLoan(){
//		if(!debug)
//			return null;
//		
//		double bal = balance;
//		double intBal = interestBalance;
//		double fBal = feeBalance;
//		
//		LinkedList<LoanEvent> chkl = (LinkedList<LoanEvent>) checklist.clone();
//		LinkedList<LoanEvent> evnts = (LinkedList<LoanEvent>) events.clone();
//		checklist = (LinkedList<LoanEvent>) events.clone();
//		
//		
//		Vector<String> result = new Vector<String>(events.size());
//		
//		
//		while(checklist.size() > 0 ){
//			LoanEvent le = checklist.peek();
//			
////			Date updateTime = new Date(le.time.getTime() + 1);
//			
////			update(updateTime);
//			
//			result.add(le.toString() + "balance: " + balance + "\ninterest: " + interestBalance + "\nfees: " + feeBalance);
//				
//		}
//		
//		System.out.println(this);
//		
//		balance = bal;
//		interestBalance = intBal;
//		feeBalance = fBal;
//		
//		checklist = chkl;
//		events = evnts;
//		
//		return result.toArray(new String[result.size()]);
//	}
	
	/*
	 * This method steps through one update of the loan. It is meant to 
	 * simulate the loan from another class. This does not reset anytihng
	 * at the end like simulateLoan() does. Use with caution. Returns false
	 * when there is nothing left to simulate.
	 */
//	public boolean simulateStep(int numSteps){
//		if(!debug)
//			return false;
//		
//		for(int i = 0; i < numSteps; i++){
//			if(checklist.size() == 0)
//				return false;
//			
//			LoanEvent le = checklist.peek();
//			
////			update(le.time);
//		}
		
//		return true;
//	}

	public double getCloseValue() {
		return balance + feeBalance + interestBalance;
	}

	public String getShortDescription(SerenityLoans plugin, boolean nameByLender) {
		return String.format("%s: %s %F", nameByLender? plugin.playerManager.entityNameLookup(getLender()) : plugin.playerManager.entityNameLookup(getBorrower()), plugin.econ.format(getValue()), new Date(startDate.getTime()));
	}

	public int getLoanID() {
		return loanID;
	}

	public String[] toString(SerenityLoans plugin) {
		String[] result =  
			{String.format("    Balance: %s", plugin.econ.format(balance)),
			 String.format("    Interest balance: %s", plugin.econ.format(interestBalance)),
			 String.format("    Fee balance: %s", plugin.econ.format(feeBalance)),
			 String.format("    Open date: %F", new Date(startDate.getTime())),
			 "",
			 String.format("    Lender: %s", plugin.playerManager.entityNameLookup(getLender())),
			 String.format("    Borrower: %s", plugin.playerManager.entityNameLookup(getBorrower())),
			 String.format("    Loan value: %s", plugin.econ.format(getValue())),
			 String.format("    Interest rate: %s (%s)",  plugin.econ.formatPercent(getInterestRate()), Conf.getIntReportingString()),
			 String.format("    Minimum payment: %s", plugin.econ.format(getMinPayment())),
			 String.format("    Term: %s", Conf.buildTimeString(getTerm())),
			 String.format("    Compounding period: %s", Conf.buildTimeString(getCompoundingPeriod())),
			 String.format("    Payment time: %s", Conf.buildTimeString(getPaymentTime())),
			 String.format("    Payment frequency: %s", Conf.buildTimeString(getPaymentFrequency())),
			 String.format("    Loan type: %s", getLoanType())};

		String[] lateFeeRelated = 
			{String.format("    Late fee: %s", plugin.econ.format(getLateFee())),
			 String.format("    Grace period: %s", Conf.buildTimeString(getGracePeriod()))};
		
		String[] serviceFeeRelated = 
			{String.format("    Service fee: %s", plugin.econ.format(getServiceFee())),
			 String.format("    Service fee frequency: %s", Conf.buildTimeString(getServiceFeeFrequency()))};
		
		if(getLateFee() != 0)
			result = (String[])ArrayUtils.addAll(result, lateFeeRelated);
		if(getServiceFee() != 0)
			result = (String[]) ArrayUtils.addAll(result, serviceFeeRelated);
		
		return result;
	}

	public Timestamp getStartTime() {
		return (Timestamp) startDate.clone();
	}
	
}
