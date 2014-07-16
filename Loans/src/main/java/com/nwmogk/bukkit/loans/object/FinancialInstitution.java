/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * 
 * File: FinancialEntity.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This class contains information relating to a FinancialInstitution. It 
 * contains additional information over and above that of a FinancialEntity
 * representing all of the information contained about the entry in the
 * FinancialEntities and FinancialInstitutions tables. Objects of this 
 * class are immutable and cannot be changed once created.
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
import java.util.UUID;

import com.nwmogk.bukkit.loans.api.FinancialEntity;
import com.nwmogk.bukkit.loans.api.PlayerType;

public final class FinancialInstitution implements FinancialEntity {

	private final UUID userID;
	//private final Timestamp ts;
	private final String name;
	private final UUID managerID; 
	private final double cash;
	private final double creditScore;
	private final PlayerType pt;
	
	public FinancialInstitution(UUID userID, String name, PlayerType type, UUID managerID, double money, double crScore){
		this.userID = userID;
		this.name = name;
		pt = type;
		this.managerID = managerID;
		cash = money;
		creditScore = crScore;
	}
	
	public UUID getUserID() {return userID;}

	public String getName() {return name;}

	public double getCash() {return cash;}

	public UUID getResponsibleParty() {return managerID;}
	
	public double getCreditScore() {return creditScore;}

	public PlayerType getPlayerType() {return pt;}
	
	public Timestamp getLastSystemUse() {return null;}//(Timestamp) ts.clone();}
	
	public FinancialEntity clone(){
		return (FinancialEntity) this.clone();
	}

}
