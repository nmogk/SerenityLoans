/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * 
 * File: LoanType.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This enum provides identifiers for the different types of loans. It also
 * provides string parsing to get the proper identifier.
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

/*
 * I am not sure if this enum even matters.
 */
public enum LoanType {
	
	INTERESTONLY, FIXEDFEE, CREDIT, GIFT, DEPOSIT, BULLET, AMORTIZING, BOND, SALARY;
	
	public static LoanType getFromString(String input){
		if(input.equalsIgnoreCase("InterestOnly"))
			return INTERESTONLY;
		else if(input.equalsIgnoreCase("FixedFee"))
			return FIXEDFEE;
		else if(input.equalsIgnoreCase("Credit"))
			return CREDIT;
		else if(input.equalsIgnoreCase("Gift"))
			return GIFT;
		else if(input.equalsIgnoreCase("Deposit"))
			return DEPOSIT;
		else if(input.equalsIgnoreCase("Bond"))
			return BOND;
		else if(input.equalsIgnoreCase("Salary"))
			return SALARY;
		else if(input.equalsIgnoreCase("Amortizing"))
			return AMORTIZING;
		else
			return BULLET;
	}
	
}
