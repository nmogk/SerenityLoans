/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * 
 * File: Loanable.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This interface defines the information that actual loanable objects 
 * should supply. This includes the outstanding balance and the current
 * amount of payment that is due, as well as the opening date.
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

import java.sql.Timestamp;

//import com.nwmogk.bukkit.loans.exception.InsufficientCashException;
//import com.nwmogk.bukkit.loans.exception.InvalidLoanTermsException;
//import com.nwmogk.bukkit.loans.object.FinancialEntity;
import com.nwmogk.bukkit.loans.object.PaymentStatement;

/**
 * This interface describes the methods needed for an object to be loanable. 
 * All methods in this interface must be completely implemented for an
 * implementing class to be in compliance.
 * 
 * @author Nathan Mogk
 * @extends LoanInfo
 *
 */

public interface Loanable extends LoanInfo{
	
	
	
//	/**
//	 * This method should modify the terms of the loan to comply with the
//	 * new terms. If the modifications are performed, a true value should
//	 * be returned. There is no guarantee that any part of the new terms
//	 * will match up with the old. If some changes fail, then all of the
//	 * old terms should be restored. The current balance should be modified
//	 * to become the loan value indicated in the newTerms.
//	 * 
//	 * @param newTerms Updated loan terms
//	 * @return True if modifications succeeded, false otherwise
//	 * @throws InvalidLoanTermsException 
//	 */
//	public boolean modifyLoan(LoanInfo newTerms) throws InvalidLoanTermsException;
	
	/**
	 * This method returns the current outstanding balance.
	 * That is, the entire remaining principal of the loan.
	 */
	public double getBalance();
	
	/**
	 * This allows callers to obtain a copy of the current payment
	 * due. If there is no outstanding bill, then this method should
	 * return null. This method should not be construed to constrain
	 * when a payment by makePayment(double) may be made.
	 * 
	 * @return Current outstanding PaymentStatement or null
	 */
	public PaymentStatement getOutstandingBill();

	/**
	 * @return The balance of fees on the loan
	 */
	public double getFeesOutstanding();

//	/**
//	 * This method should pay the entire outstanding bill. This is
//	 * equivalent to makePayment(getOutstandingBill().getPaymentRemaining())
//	 * 
//	 * This method does require getOutstandingBill() to be non null.
//	 * 
//	 * The return value is the success of the payment. If the payment does
//	 * not succeed, so money should be drawn.
//	 */
//	public boolean makePayment(PaymentStatement bill);
//
//	/**
//	 * This method typifies making a payment of a specified amount
//	 * towards the loan. The money should be transferred from the 
//	 * borrower to the lender.
//	 * 
//	 * The return value is the success of the payment. If the payment does
//	 * not succeed, so money should be drawn.
//	 * 
//	 * @param amount The amount of money to pay
//	 * @return the success or failure of the payment
//	 */
//	public boolean makePayment(double amount, PaymentStatement bill);
//	
//	/**
//	 * This method causes the entire principal balance to be paid in
//	 * full. Outstanding fees and interest should also be paid.
//	 * 
//	 * The return value is the success of the payment. If the payment does
//	 * not succeed, so money should be drawn.
//	 * 
//	 * @return The success of the payment
//	 * @throws InsufficientCashException 
//	 */
//	public boolean payoff() throws InsufficientCashException;
//	
//	/**
//	 * This method changes the borrower provided the new borrower
//	 * is authorized to borrow.
//	 * 
//	 * @param newDebtor The new borrower
//	 * @return The success of the transfer
//	 */
//	public boolean transferDebt(FinancialEntity newDebtor);
//	
//	/**
//	 * This method changes the creditor provided the new creditor is
//	 * able to offer the loan.
//	 * 
//	 * @param newCreditor
//	 * @return
//	 */
//	public boolean transferInvestment(FinancialEntity newCreditor);
	
	/**
	 * @return The actual expected date when the loan will be repaid
	 */
//	public Date getCloseDate();
	
	/**
	 * @return The actual date of loan opening
	 */
	public Timestamp getStartDate();

}
