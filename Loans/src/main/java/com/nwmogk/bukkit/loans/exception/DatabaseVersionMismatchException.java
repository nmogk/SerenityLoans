/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * 
 * File: DatabaseVersionMismatchException.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This exception provides context for when a current version of the data
 * base is different than the current version of the plugin.
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

public class DatabaseVersionMismatchException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DatabaseVersionMismatchException(String msg){
		super(msg);
	}

}
