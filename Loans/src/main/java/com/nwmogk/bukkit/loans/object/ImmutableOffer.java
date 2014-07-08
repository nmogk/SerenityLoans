/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * 
 * File: ImmutableOffer.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This class provides information relating to an offer. It implements the
 * LoanInfo interface. Instances of this class are immutable.
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

import org.apache.commons.lang.ArrayUtils;

import com.nwmogk.bukkit.loans.Conf;
import com.nwmogk.bukkit.loans.EconomyManager;
import com.nwmogk.bukkit.loans.LoanInfo;
import com.nwmogk.bukkit.loans.LoanType;
import com.nwmogk.bukkit.loans.SerenityLoans;
import com.nwmogk.bukkit.loans.api.FinancialEntity;

public final class ImmutableOffer implements LoanInfo {
	
	private final FinancialEntity lender;
	private final FinancialEntity borrower;
	
	private final double value;
	private final double interestRate;
	
	private final double lateFee;
	private final double minPayment;
	private final double serviceFee;
	
	private final long term;
	private final long compoundingPeriod;
	private final long gracePeriod;
	private final long paymentTime;
	private final long paymentFrequency;
	private final long serviceFeeFrequency;
		
	private LoanType loanType;

	public ImmutableOffer(Loan loan) {
		lender = loan.getLender();
		borrower = loan.getBorrower();
		value = loan.getValue();
		interestRate = loan.getInterestRate();
		lateFee = loan.getLateFee();
		minPayment = loan.getMinPayment();
		serviceFee = loan.getServiceFee();
		term = loan.getTerm();
		compoundingPeriod = loan.getCompoundingPeriod();
		gracePeriod = loan.getGracePeriod();
		paymentTime = loan.getPaymentTime();
		paymentFrequency = loan.getPaymentFrequency();
		serviceFeeFrequency = loan.getServiceFeeFrequency();
		loanType = loan.getLoanType();
	}
	
	public ImmutableOffer(FinancialEntity lender, FinancialEntity borrower, double value, double interestRate, double lateFee, double minPayment, double serviceFee, long term, long compoundingPeriod, long gracePeriod, long paymentTime, long paymentFrequency, long serviceFeeFrequency, LoanType loanType){
		this.lender = lender;
		this.borrower = borrower;
		this.value = value;
		this.interestRate = interestRate;
		this.lateFee = lateFee;
		this.minPayment = minPayment;
		this.serviceFee = serviceFee;
		this.term = term;
		this.compoundingPeriod = compoundingPeriod;
		this.gracePeriod = gracePeriod;
		this.paymentTime = paymentTime;
		this.paymentFrequency = paymentFrequency;
		this.serviceFeeFrequency = serviceFeeFrequency;
		this.loanType = loanType;
	}
	
	
	public String[] toString(SerenityLoans plugin){
		
		EconomyManager econ = plugin.getEcon();
		
		String[] result =  
			{String.format("    Lender: %s", plugin.playerManager.entityNameLookup(lender)),
			 String.format("    Borrower: %s",  plugin.playerManager.entityNameLookup(borrower)),
			 String.format("    Loan value: %s", econ.format(value)),
			 String.format("    Interest rate: %s (%s)",  econ.formatPercent(interestRate), Conf.getIntReportingString()),
			 String.format("    Minimum payment: %s", econ.format(minPayment)),
			 String.format("    Term: %s", Conf.buildTimeString(term)),
			 String.format("    Compounding period: %s", Conf.buildTimeString(compoundingPeriod)),
			 String.format("    Payment time: %s", Conf.buildTimeString(paymentTime)),
			 String.format("    Payment frequency: %s", Conf.buildTimeString(paymentFrequency)),
			 String.format("    Loan type: %s", loanType)};

		String[] lateFeeRelated = 
			{String.format("    Late fee: %s", econ.format(lateFee)),
			 String.format("    Grace period: %s", Conf.buildTimeString(gracePeriod))};
		
		String[] serviceFeeRelated = 
			{String.format("    Service fee: %s", econ.format(serviceFee)),
			 String.format("    Service fee frequency: %s", Conf.buildTimeString(serviceFeeFrequency))};
		
		if(lateFee != 0)
			result = (String[])ArrayUtils.addAll(result, lateFeeRelated);
		if(serviceFee != 0)
			result = (String[]) ArrayUtils.addAll(result, serviceFeeRelated);
		
		return result;
	}
	
	public FinancialEntity getLender() {return lender.clone();}

	public FinancialEntity getBorrower() {return borrower.clone();}

	public double getValue() {return value;}

	public long getTerm() {return term;}

	public double getInterestRate() {return interestRate;}

	public long getCompoundingPeriod() {return compoundingPeriod;}

	public long getPaymentFrequency() {return paymentFrequency;}

	public long getPaymentTime() {return paymentTime;}

	public double getMinPayment() {return minPayment;}

	public long getGracePeriod() {return gracePeriod;}

	public double getLateFee() {return lateFee;}

	public long getServiceFeeFrequency() {return serviceFeeFrequency;}

	public double getServiceFee() {return serviceFee;}

	public LoanType getLoanType() {return loanType;}
}
