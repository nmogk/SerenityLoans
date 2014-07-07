/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * 
 * File: LoanInfo.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This interface defines what information needs to be extracted from an
 * object that contains information about a loan. Both loans and concrete
 * offers will implement this interface.
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

import com.nwmogk.bukkit.loans.object.FinancialEntity;



/**
 * 
 * This interface describes the methods used for determining the details of a particular loan
 * 
 * @author Nathan Mogk
 *
 */

public interface LoanInfo {
	
	/**
	 * @return The FinancialEntity object that represents the lender (debtor)
	 */
	public FinancialEntity getLender();
	
	/**
	 * @return The FinancialEntity object that represents the borrower (creditor)
	 */
	public FinancialEntity getBorrower();
	
	/**
	 * @return The face value balance of the original loan
	 */
	public double getValue();
	
	/**
	 * @return The interest rate of the loan
	 */
	public double getInterestRate();
	
	/**
	 * 
	 * Returns the length of time in weeks the loan is in effect until maturity.
	 * A value of zero indicates that there is no term.
	 * 
	 * @return The length of time in milliseconds before complete repayment is due
	 */
	public long getTerm();
	
	/**
	 * The time in weeks before interest is accrued. Negative values should
	 * not be accepted. A value of zero indicates continuous compounding.
	 * 
	 * @return The time in milliseconds before interest is accrued
	 */
	public long getCompoundingPeriod();
	
	/**
	 * The time in days that a borrower has after the payment is due to pay the minimum
	 * balance before accruing a late fee. Negative values should not be accepted.
	 * 
	 * @return The amount of time in milliseconds that the borrower has before incurring a late fee
	 */
	public long getGracePeriod();
	
	/**
	 * @return The amount of time in milliseconds that the payment statement will be sent before payment is due
	 */
	public long getPaymentTime();
	
	
	/**
	 * @return The number of milliseconds between payments
	 */
	public long getPaymentFrequency();
	
	/**
	 * @return The size of the fee assessed when the minimum payment has not been met
	 */
	public double getLateFee();
	
	/**
	 * @return The size of the minimum payment
	 */
	public double getMinPayment();
	
	
	
	/**
	 * @return The number of milliseconds between service fee charges
	 */
	public long getServiceFeeFrequency();

	/**
	 * @return The size of the service fee
	 */
	public double getServiceFee();
	
	/**
	 * This method returns the type of loan as defined in LoanType
	 */
	public LoanType getLoanType();
	
	public String[] toString(SerenityLoans plugin);

}
