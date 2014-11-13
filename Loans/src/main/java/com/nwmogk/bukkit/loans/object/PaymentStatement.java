/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * 
 * File: PaymentStatement.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This class contains information relating to a payment statement. It is
 * intended to only be used as a way for classes to obtain information 
 * about the state of the mySQL database at a particular point in time. All
 * instances of this class are immutable, and cannot be changed after 
 * creation.
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

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.nwmogk.bukkit.loans.SerenityLoans;

public final class PaymentStatement {
	
	private Timestamp statementDate;
	private Timestamp dueDate;
	
	private final double principalAmount;
	private final double interestAmount;
	private final double feeAmount;
	private final double paid;
	private final double minPayment;
	private final int loanID;
	private final int statementID;
	
	public PaymentStatement(int statementID, int loan, double amountPrincipal, double intAmount, double feeAmount, double min, Timestamp statement, Timestamp due, double paid){
		this.statementID = statementID;
		this.loanID = loan;
		principalAmount = amountPrincipal;
		interestAmount = intAmount;
		this.feeAmount = feeAmount;
		minPayment = min;
		statementDate = statement;
		dueDate = due;
		this.paid = paid;
	}

	public Timestamp getPayDate() {
		return (Timestamp)dueDate.clone();
	}
	
	public int getStatementID() {
		return statementID;
	}
	
	public int getLoanID(){
		return loanID;
	}

	public double getActualPaid() {
		return paid;
	}

	public double getBillAmount() {
		return feeAmount + interestAmount + principalAmount;
	}
	
	public double getPaymentRemaining(){
		return Math.max(0.0, getBillAmount() - paid);
	}
	
	public double getPrincipalAmount(){
		return principalAmount;
	}
	
	public double getInterestAmount(){
		return interestAmount;
	}
	
	public double getFeeAmount(){
		return feeAmount;
	}
	
	public double getPrincipalRemaining(){
		if (paid <= interestAmount + feeAmount)
			return principalAmount;
		
		return Math.max(0.0, getBillAmount() - paid);
	}
	
	public double getFeesRemaining(){
		return Math.max(0.0, feeAmount - paid);
	}
	
	public double getInterestRemaining(){
		if(paid <= feeAmount)
			return interestAmount;
		
		return Math.max(0.0, feeAmount + interestAmount - paid);
	}
	
	public Timestamp getStatementDate() {
		return (Timestamp)statementDate.clone();
	}

	public double getMinimumPayment(){
		return minPayment;
	}
	
	public String[] toString(SerenityLoans plugin) throws InterruptedException, ExecutionException, TimeoutException, SQLException{
		
		String[] result = 
			{
				String.format("    Loan: %s", plugin.loanManager.getLoan(loanID).getShortDescription(plugin, true)),
				String.format("    Payment Remaining: %s", plugin.econ.format(getPaymentRemaining())),
				String.format("    Due date: %F", new Date(dueDate.getTime())),
				String.format("    Statement Date: %F", new Date(statementDate.getTime())),
				String.format("    %s", paid >= minPayment? "Original balance: " + plugin.econ.format(getBillAmount()) : "Please pay at least " + plugin.econ.format(minPayment) + ".")
			};
		
		return result;
	}

}
