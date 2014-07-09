/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * 
 * File: Terms.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This enum provides identification for every field that describes the 
 * terms of a loan. There is a 1:1 match with the terms in the mySQL 
 * database and the fields in this enum.
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
 * 2014-02-11  nmogk           Initial release for v0.1
 * 
 * 
 */

package com.nwmogk.bukkit.loans.api;

public enum Terms {

	LENDER, BORROWER, VALUE, INTERESTRATE, LATEFEE, 
	MINPAYMENT, SERVICEFEE, TERM, COMPOUNDINGPERIOD, GRACEPERIOD,
	PAYMENTTIME, PAYMENTFREQUENCY, SERVICEFEEFREQUENCY, LOANTYPE;

}