/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * 
 * File: LoanState.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This class contains the information that defines the state of a loan.
 * Objects of this class are immutable, so they will not change after 
 * initial creation. This class may be redundant with the Loan class.
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

import java.util.Date;

import com.nwmogk.bukkit.loans.object.ImmutableOffer;
import com.nwmogk.bukkit.loans.object.Loan;

/*
 * May be redundant with the Loan class
 */

public class LoanState{
	
	public final double balance;
	public final double interestBalance;
	public final double feeBalance;
	public final LoanInfo terms;
	public final long startDate;
	public final long lastUpdate;

	public LoanState(double balance, double interestBalance, double feeBalance, 
			LoanInfo terms, Date startDate, Date lastUpdate) {
		this.balance = balance;
		this.interestBalance = interestBalance;
		this.feeBalance = feeBalance;
		this.terms = terms;
		this.startDate = startDate.getTime();
		this.lastUpdate = lastUpdate.getTime();
	}
	
	public LoanState(Loan loan){
		this(loan.getBalance(), loan.getInterestBalance(), loan.getFeesOutstanding(), 
				new ImmutableOffer(loan), loan.getStartDate(), loan.getLastUpdate());
	}

}
