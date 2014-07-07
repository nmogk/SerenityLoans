/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * 
 * File: PlayerType.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This enum provides labels for all of the different types of 
 * FinancialEntities. The allowed labels match the allowed values in the
 * mySQL table for the PlayerType column. This enum provides a method to
 * obtain the proper label from a text string.
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

package com.nwmogk.bukkit.loans;

public enum PlayerType {
	
	PLAYER, BANK, CREDIT_UNION, TOWN, EMPLOYER;
	
	public static PlayerType getFromString(String type){
		if(type.equalsIgnoreCase("Player"))
			return PLAYER;
		else if(type.equalsIgnoreCase("Bank"))
			return BANK;
		else if(type.equalsIgnoreCase("CreditUnion"))
			return CREDIT_UNION;
		else if(type.equalsIgnoreCase("Town/Faction"))
			return TOWN;
		else if(type.equalsIgnoreCase("Employer"))
			return EMPLOYER;
		
		return PLAYER;
	}
}
