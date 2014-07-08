/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * 
 * File: InsufficientCashException.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This exception provides context for when an attempt is made to withdraw
 * cash from a FinancialEntity that does not have enough to complete the
 * operation.
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

package com.nwmogk.bukkit.loans.exception;

import com.nwmogk.bukkit.loans.api.FinancialEntity;

public class InsufficientCashException extends Exception {

	private final FinancialEntity entity;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InsufficientCashException() {
		entity = null;
	}

	public InsufficientCashException(String message) {
		this(null, message);
	}
	
	public InsufficientCashException(FinancialEntity entity){
		this.entity = entity;
	}
	
	public InsufficientCashException(FinancialEntity entity, String message){
		super(message);
		this.entity = entity;
	}
	
	public FinancialEntity getFinancialEntity(){
		return entity;
	}

	

}
