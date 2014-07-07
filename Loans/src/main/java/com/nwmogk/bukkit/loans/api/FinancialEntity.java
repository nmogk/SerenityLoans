/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * 
 * File: FinancialEntity.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This interface contains methods relating to a FinancialEntity as it is
 * represented in the mySQL table.
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

package com.nwmogk.bukkit.loans.api;

import java.sql.Timestamp;
import java.util.UUID;

import com.nwmogk.bukkit.loans.PlayerType;

public interface FinancialEntity extends Cloneable{
	
	public UUID getUserID();

	public double getCash();
	
	public int getCreditScore();

	public PlayerType getPlayerType();
	
	public Timestamp getLastSystemUse();

}
