/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * 
 * File: EconResult.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This class contains information regarding the outcome of an economic
 * call, independent of the economy implementation. This class is meant to
 * serve as an adapter to Vault-dependent plugins that will utilize this
 * plugin as an economy. Instances of this class are immutable, and all
 * fields are public access.
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

import net.milkbowl.vault.economy.EconomyResponse;

public final class EconResult {
	
	public final double amount;
	public final double balance;
	public final boolean callSuccess;
	public final String errMsg;
	
	public EconResult(double amount, double balance, boolean callSuccess, String errMsg){
		this.amount = amount;
		this.balance = balance;
		this.callSuccess = callSuccess;
		this.errMsg = errMsg;
	}
	
	public EconResult(EconomyResponse vaultResponse){
		amount = vaultResponse.amount;
		balance = vaultResponse.balance;
		callSuccess = vaultResponse.transactionSuccess();
		errMsg = vaultResponse.errorMessage;
	}

}
