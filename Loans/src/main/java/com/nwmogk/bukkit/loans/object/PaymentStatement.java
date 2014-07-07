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

import java.sql.Timestamp;
import java.util.Date;

import com.nwmogk.bukkit.loans.SerenityLoans;

public final class PaymentStatement {
	
	private Timestamp statementDate;
	private Timestamp dueDate;
	
	private final double billAmount;
	private final double paid;
	private final double minPayment;
	private final int loanID;
	private final int statementID;
	
	public PaymentStatement(int statementID, int loan, double amount, double min, Timestamp statement, Timestamp due, double paid){
		this.statementID = statementID;
		this.loanID = loan;
		billAmount = amount;
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
		return billAmount;
	}
	
	public double getPaymentRemaining(){
		return Math.max(0.0, billAmount - paid);
	}
	
	public Timestamp getStatementDate() {
		return (Timestamp)statementDate.clone();
	}

	public double getMinimumPayment(){
		return minPayment;
	}
	
	public String[] toString(SerenityLoans plugin){
		
		String[] result = 
			{
				String.format("    Loan: %s", plugin.loanManager.getLoan(loanID).getShortDescription(plugin, true)),
				String.format("    Payment Remaining: %s", SerenityLoans.econ.format(getPaymentRemaining())),
				String.format("    Due date: %F", new Date(dueDate.getTime())),
				String.format("    Statement Date: %F", new Date(statementDate.getTime())),
				String.format("    %s", paid >= minPayment? "Original balance: " + SerenityLoans.econ.format(billAmount) : "Please pay at least " + SerenityLoans.econ.format(minPayment) + ".")
			};
		
		return result;
	}

}
