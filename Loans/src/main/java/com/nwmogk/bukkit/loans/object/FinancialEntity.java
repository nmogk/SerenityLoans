/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * 
 * File: FinancialEntity.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This class contains information relating to a FinancialEntity as it is
 * represented in the mySQL table. Objects of this class are immutable and
 * cannot be changed once created.
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

import com.nwmogk.bukkit.loans.PlayerType;

public final class FinancialEntity implements Cloneable {

	private final int userID;
	//private final Timestamp ts;
	private final String name;
	private final int managerID; 
	private final double cash;
	private final int creditScore;
	private final PlayerType pt;
	
	public FinancialEntity(int userID, String name, PlayerType type,  double money, int crScore){
		this(userID, name, type, userID, money, crScore);
	}
	
	public FinancialEntity(int userID, String name, PlayerType type, int managerID, double money, int crScore){
		this.userID = userID;
		this.name = name;
		pt = type;
		this.managerID = managerID;
		cash = money;
		creditScore = crScore;
	}
	
	public int getUserID() {return userID;}

	public String getName() {return name;}

	public double getCash() {return cash;}

	public int getResponsibleParty() {return managerID;}
	
	public int getCreditScore() {return creditScore;}

	public PlayerType getPlayerType() {return pt;}
	
	public Timestamp getLastSystemUse() {return null;}//(Timestamp) ts.clone();}
	
	public FinancialEntity clone(){
		return (FinancialEntity) this.clone();
	}

}
