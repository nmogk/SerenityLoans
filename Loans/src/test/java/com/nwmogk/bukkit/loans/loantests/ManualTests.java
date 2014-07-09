package com.nwmogk.bukkit.loans.loantests;

import java.io.File;
import java.util.Vector;

import com.nwmogk.bukkit.loans.obsolete.Offer;

import org.bukkit.configuration.*;
import org.bukkit.configuration.file.*;

import com.nwmogk.bukkit.loans.*;
import com.nwmogk.bukkit.loans.api.Terms;
import com.nwmogk.bukkit.loans.exception.InsufficientCashException;
import com.nwmogk.bukkit.loans.exception.InvalidLoanTermsException;
import com.nwmogk.bukkit.loans.object.Loan;

import org.junit.Test;

import static org.junit.Assert.*;


@SuppressWarnings("unused")
public class ManualTests {

	public static void main(String[] args) {
		
		System.out.println(String.format("%#(,.2f", 5280.0));
		

	}
	
	public static long parseTime(String timeString) {
		
		if(timeString.matches(".*[^0-9wdhms].*"))
			return 0;
		
		Vector<String> parsed = new Vector<String>();
		String[] units = {"w", "d", "h", "m", "s"};
		
		parsed.add(timeString.toLowerCase());
		
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
		else if(!parsed.lastElement().matches("[wdhms]"))
			return 0;
		
		if(parsed.size() % 2 != 0)
			return 0;
		
		while(parsed.size() > 1){
			int multiplier = 1;
			switch(parsed.remove(parsed.size() - 1).charAt(0)){
			case 'w':
				multiplier = 7 * 24 * 60 * 60;
				break;
			case 'd':
				multiplier = 24 * 60 * 60;
				break;
			case 'h':
				multiplier = 60 * 60;
				break;
			case 'm':
				multiplier = 60;
				break;
			case 's':
				multiplier = 1;
				break;
			}
			
			result += multiplier * Long.valueOf(parsed.remove(parsed.size() - 1));
			
		}
		
		result *= 1000;
		
		return result;
	}
	
	private static void runConfigTest(){
		SerenityLoans plug = new SerenityLoans();
		YamlConfiguration y = YamlConfiguration.loadConfiguration(new File("config.yml"));
		String site = plug.getConfig().getString("options.disclaimer.tutorial-website");
		System.out.println(site);
	}
	
	private static void runLoansTest(){
		
		
	}
	
	@Test
	public void testSetOffer(){
		Offer offer = Offer.impotentOffer.clone();
		
		assertTrue(offer.set(Terms.VALUE, 100.0));
		assertTrue(offer.set(Terms.INTERESTRATE, 0.02));
		assertTrue(offer.set(Terms.TERM, 16));
		assertTrue(offer.set(Terms.COMPOUNDINGPERIOD, 4));
		assertTrue(offer.set(Terms.GRACEPERIOD, 3));
		assertTrue(offer.set(Terms.PAYMENTTIME, 7));
		assertTrue(offer.set(Terms.PAYMENTFREQUENCY, 8));
		assertTrue(offer.set(Terms.LATEFEE, 3.0));
		assertTrue(offer.set(Terms.MINPAYMENT, 1.0));
		assertTrue(offer.set(Terms.SERVICEFEEFREQUENCY, 8));
		assertTrue(offer.set(Terms.SERVICEFEE, 10.0));
	}

}
