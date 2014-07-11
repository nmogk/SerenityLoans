/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * 
 * File: Conf.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This class provides configuration information and does input validation
 * checking for classes that require configuration data. It also provides
 * some utility methods that convert between time strings and long values.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Vector;
import java.util.regex.Matcher;

import org.bukkit.configuration.file.FileConfiguration;

public class Conf {
	
	private static SerenityLoans plugin = SerenityLoans.getPlugin();
	
	public static final String[] allMacros = {"$$c", "$$k", "$$p", "$$m", "$$r", "$$l", "$$b", "$$s", "$$h", "$$t"};
	
	// TODO make thread safe
	
	/**
	 * This method parses a string that contains time values in the form
	 * of numbers followed by units. Spaces and capitalization are removed
	 * from the string when starting. If no units are given, there must only
	 * be one number, and it will be assumed to be in minutes. Available units
	 * are y, w, d, h, m, s for week, day, hour, minute, second, respectively.
	 * 
	 * Invalid values will return 0;
	 * 
	 * @param timeString
	 * @return Time in miliseconds represented by the string.
	 */
	public static long parseTime(String timeString) {


		if(timeString.matches(".*[^0-9ywdhms].*"))
			return 0;
		
		Vector<String> parsed = new Vector<String>();
		String[] units = {"y", "w", "d", "h", "m", "s"};
		
		parsed.add(timeString.toLowerCase().replaceAll("[:space:]", ""));
		
		for(String regex : units){
			
			for(int i = 0; i < parsed.size(); i++) {
				String[] split = parsed.get(i).split(regex);
				
				if(split.length > 2 || split.length == 0)
					return 0;
				else if(split.length == 1 && parsed.get(i).equalsIgnoreCase(split[0]))
					continue;
				
				parsed.remove(i);
				
				if(split.length == 2)
					parsed.add(i, split[1]);
				
				parsed.add(i, regex);
				parsed.add(i, split[0]);
				
				i++;
			}	
		}
		
		long result = 0;
		
		if(parsed.size() == 1)
			parsed.add("m");
		else if(!parsed.lastElement().matches("[ywdhms]"))
			return 0;
		
		if(parsed.size() % 2 != 0)
			return 0;
		
		while(parsed.size() > 1){
			long multiplier = 1000;
			switch(parsed.remove(parsed.size() - 1).charAt(0)){
			case 'y':
				multiplier = (long)(365.2425 * 24 * 60 * 60 * 1000);
				break;
			case 'w':
				multiplier = 7 * 24 * 60 * 60 * 1000;
				break;
			case 'd':
				multiplier = 24 * 60 * 60 * 1000;
				break;
			case 'h':
				multiplier = 60 * 60 * 1000;
				break;
			case 'm':
				multiplier = 60 * 1000;
				break;
			case 's':
				multiplier = 1000;
				break;
			}
			
			result += multiplier * Long.valueOf(parsed.remove(parsed.size() - 1));
			
		}
		
		return result;
	}
	
	public static String buildTimeString(long value){
		
		if(value < 1000)
			return "0s";
		
		long counter = value;
		
		long years = counter/(long)(60 * 60 * 24 * 365.2425 * 1000);
		counter = counter % (long)(60 * 60 * 24 * 365.2425 * 1000);
		
		long weeks = counter/(60 * 60 * 24 * 7 * 1000);
		counter = counter % (60 * 60 * 24 * 7 * 1000);
		
		long days = counter/(60 * 60 * 24 * 1000);
		counter = counter % (60 * 60 * 24 * 1000);
		
		long hours = counter/(60 * 60 * 1000);
		counter = counter % (60 * 60 * 1000);
				
		long minutes = counter/(60 * 1000);
		long seconds = counter % (60 * 1000);
		
		String result = "";
		
		if(years > 0)
			result += years + "y";
		if(weeks > 0)
			result += weeks + "w";
		if(days > 0)
			result += days + "d";
		if(hours > 0)
			result += hours + "h";
		if(minutes > 0)
			result += minutes + "m";
		if(seconds > 0)
			result += seconds + "s";
		
		return result;
		
	}
	
	public static String getIntReportingString(){
		String reportingTime = "1w";
		String path = "loan.terms-constraints.interest.reporting-time";
		FileConfiguration config = plugin.getConfig();
		
		if(config.contains(path) && config.isString(path))
			reportingTime = plugin.getConfig().getString(path).replaceAll(" ", "");
		
		if(reportingTime.equalsIgnoreCase("1d")){
			reportingTime = "daily";
		} else if(reportingTime.equalsIgnoreCase("1w")){
			reportingTime = "weekly";
		} else if(reportingTime.equalsIgnoreCase("4w")){
			reportingTime = "monthly";
		} else if(reportingTime.equalsIgnoreCase("1y")){
			reportingTime = "yearly";
		}
		
		return reportingTime;
	}
	
	public static long getIntReportingTime(){
		String reportingTime = "1w";
		String path = "loan.terms-constraints.interest.reporting-time";
		FileConfiguration config = plugin.getConfig();
		
		if(config.contains(path) && config.isString(path))
			reportingTime = plugin.getConfig().getString(path).replaceAll(" ", "");
		
		return parseTime(reportingTime);
	}
	
	public static String getMessageString(){
		String message = "$loans$>";
		String path = "options.message-prefix";
		FileConfiguration config = plugin.getConfig();
		
		if(config.contains(path) && config.isString(path))
			message = plugin.getConfig().getString(path);
		
		return message;
	}
	
	public static double parseMinPayment(double loanValue, double minPayment){
		boolean percentRule = false;
		String path = "loan.terms-constraints.min-payment.percent-rule";
		FileConfiguration config = plugin.getConfig();
		
		if(config.contains(path) && config.isBoolean(path))
			percentRule = plugin.getConfig().getBoolean(path);
		
		return percentRule? loanValue * minPayment : minPayment;
	}
	
	public static String messageCenter(String messageName, String[] relevantMacros, String[] macroValues){
		String path = "options.messages." + messageName;
		String message = getMessageString() + " ";
		
		FileConfiguration config = plugin.getConfig();
		
		if(config.contains(path) && config.isString(path))
			message += plugin.getConfig().getString(path);
		
		
		return parseMacros(message, relevantMacros, macroValues);
	}
	
	public static String parseMacros(String message, String[] relevantMacros, String[] macroValues){
		String result = message;
		
		
		HashSet<String> macSet = new HashSet<String>();
		macSet.addAll(Arrays.asList(allMacros));
		macSet.removeAll(Arrays.asList(relevantMacros));
		
		String[] macrosRemaining = macSet.toArray(new String[0]);
		
		if(relevantMacros != null && macroValues != null){
			for(int i = 0; i < Math.min(relevantMacros.length, macroValues.length); i++){
				result = result.replaceAll(Matcher.quoteReplacement(relevantMacros[i]), macroValues[i]);
			}
			
			for(int i = 0; i < macrosRemaining.length; i++){
				result = result.replaceAll(Matcher.quoteReplacement(macrosRemaining[i]), "");
			}
		}
		
		if(SerenityLoans.debugLevel >= 2){
			SerenityLoans.log.info(String.format("Message before: %s", message));
			SerenityLoans.log.info(String.format("Message after: %s", result));
			
		}
		
		return result;
	}

}
