package com.nwmogk.bukkit.loans.obsolete;

import com.nwmogk.bukkit.loans.LoanInfo;
import com.nwmogk.bukkit.loans.LoanType;
import com.nwmogk.bukkit.loans.SerenityLoans;
import com.nwmogk.bukkit.loans.Terms;
import com.nwmogk.bukkit.loans.api.FinancialEntity;


/**
 * This class represents an offer to lend. It stores all of the terms which may
 * be accessed through the methods specified in @LoanInfo. The offer may be 
 * modified at any time. The current implementation does not have any input
 * validation, so classes that depend on objects of this class should perform
 * their own input validation. This class is @Cloneable.
 * 
 * 
 * @author Nathan W Mogk
 *
 */
public class Offer implements LoanInfo, Cloneable{
	
	/**
	 * This offer may be used as a blank placeholder offer. 
	 */
	public static final Offer impotentOffer = new Offer(null, null, 0, 0, 0);
	
	private FinancialEntity lender;
	private FinancialEntity borrower;
	
	private double value;
	private double interestRate;
	
	private double lateFee;
	private double minPayment;
	private double serviceFee;
	
	private long term;
	private long compoundingPeriod;
	private long gracePeriod;
	private long paymentTime;
	private long paymentFrequency;
	private long serviceFeeFrequency;
		
	private LoanType loanType;
	
	
	/**
	 * This constructor provides an offer that represents very basic loan terms. It supplies defaults
	 * for the remaining terms. The default terms are late fee:0, minPayment: 1% of the value, service
	 * fee: 0, compounding period: term, grace period: 0, payment frequency: term, payment time: 1 week.
	 * 
	 * @param lender
	 * @param borrower
	 * @param value
	 * @param interestRate
	 * @param term
	 */
	public Offer(FinancialEntity lender, FinancialEntity borrower, double value, double interestRate, int term){
		this(lender, borrower, value, interestRate, term, 0, 0.01 * value, 0, term, 0, 7, term, 0);
	}
	
	
	/**
	 * This constructor specifies inputs for all of the fields represented by this offer.
	 * 
	 * @param lender
	 * @param borrower
	 * @param value
	 * @param interestRate
	 * @param term
	 * @param lateFee
	 * @param minPayment
	 * @param serviceFee
	 * @param compoundingPeriod
	 * @param gracePeriod
	 * @param paymentTime
	 * @param paymentFrequency
	 * @param serviceFeeFrequency
	 */
	public Offer(FinancialEntity lender, FinancialEntity borrower, double value, double interestRate, 
			int term,  double lateFee, double minPayment, double serviceFee, int compoundingPeriod, 
			int gracePeriod, int paymentTime, int paymentFrequency, int serviceFeeFrequency){
		this.lender = lender;
		this.borrower = borrower;
		this.value = value;
		this.interestRate = interestRate;
		this.term = term;
		this.lateFee = lateFee;
		this.minPayment = minPayment;
		this.serviceFee = serviceFee;
		this.compoundingPeriod = compoundingPeriod;
		this.gracePeriod = gracePeriod;
		this.paymentTime = paymentTime;
		this.paymentFrequency = paymentFrequency;
		this.serviceFeeFrequency = serviceFeeFrequency;
	}
	
	public FinancialEntity getLender() {return lender;}

	public FinancialEntity getBorrower() {return borrower;}

	public double getValue() {return value;}

	public long getTerm() {return term;}

	public double getInterestRate() {return interestRate;}

	public long getCompoundingPeriod() {return compoundingPeriod;}

	public long getPaymentFrequency() {return paymentFrequency;}

	public long getPaymentTime() {return paymentTime;}

	public double getMinPayment() {return minPayment;}

	public long getGracePeriod() {return gracePeriod;}

	public double getLateFee() {return lateFee;}

	public long getServiceFeeFrequency() {return serviceFeeFrequency;}

	public double getServiceFee() {return serviceFee;}

	public LoanType getLoanType() {return loanType;}
	
	/**
	 * This method sets the fields of this offer. It accepts a field argument
	 * which specifies which term should be updated and an object which holds
	 * the value of the new field. This method checks the type of the value
	 * argument and will return false if there is a type mismatch with the
	 * specified field. 
	 * 
	 * @param field
	 * @param value
	 * @return
	 */
	public boolean set(Terms field, Object value){
		
		switch(field){
		case BORROWER:
			if(!(value instanceof FinancialEntity))
				return false;
			borrower = (FinancialEntity)value;
			break;
		case COMPOUNDINGPERIOD:
			if(!(value instanceof Integer))
				return false;
			compoundingPeriod = (Integer)value;
			break;
		case GRACEPERIOD:
			if(!(value instanceof Integer))
				return false;
			gracePeriod = (Integer)value;
			break;
		case INTERESTRATE:
			if(!(value instanceof Double))
				return false;
			interestRate = (Double)value;
			break;
		case LATEFEE:
			if(!(value instanceof Double))
				return false;
			lateFee = (Double)value;
			break;
		case LENDER:
			if(!(value instanceof FinancialEntity))
				return false;
			lender = (FinancialEntity)value;
			break;
		case LOANTYPE:
			if(!(value instanceof LoanType))
				return false;
			loanType = (LoanType)value;
			break;
		case MINPAYMENT:
			if(!(value instanceof Double))
				return false;
			minPayment = (Double)value;
			break;
		case PAYMENTFREQUENCY:
			if(!(value instanceof Integer))
				return false;
			paymentFrequency = (Integer)value;
			break;
		case PAYMENTTIME:
			if(!(value instanceof Integer))
				return false;
			paymentTime = (Integer)value;
			break;
		case SERVICEFEE:
			if(!(value instanceof Double))
				return false;
			serviceFee = (Double)value;
			break;
		case SERVICEFEEFREQUENCY:
			if(!(value instanceof Integer))
				return false;
			serviceFeeFrequency = (Integer)value;
			break;
		case TERM:
			if(!(value instanceof Integer))
				return false;
			term = (Integer)value;
			break;
		case VALUE:
			if(!(value instanceof Double))
				return false;
			this.value = (Double)value;
			break;
		default:
			return false;		
		}
		
		return true;
	}
	
	/**
	 * This method provides clone functionality for this class. It
	 * may return null if a @CloneNotSupportedException is raised.
	 */
	public Offer clone(){
		try {
			return (Offer)super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
public String toString(){
		
		String dollars = "dollars";
		//String $ = "$";
		
		//dollars = SerenityLoans.econ.currencyNamePlural();
		
		String result = "";
		result += "Lender: " + lender.getName() + "\n";
		result += "Borrower: " + borrower.getName() + "\n";
		result += "Loan value: " + value + " " + dollars + "\n";
		result += "Interest rate: " + interestRate + "% (weekly)\n";
		result += "Minimum payment: " + minPayment + " " + dollars + "\n";
		result += "Late fee: " + lateFee + " "+ dollars + "\n";
		result += "Service fee: " + serviceFee + " "  + dollars + "\n";
		result += "Term: " + term + " weeks\n";
		result += "Compounding period: " + compoundingPeriod + " weeks\n";
		result += "Grace period: " + gracePeriod + " days\n";
		result += "Payment time: " + paymentTime + " days\n";
		result += "Payment frequency: " + paymentFrequency + " weeks\n";
		result += "Service fee frequency: " + serviceFeeFrequency + " weeks\n";
		
		
		
		return result;
	}


@Override
public String[] toString(SerenityLoans plugin) {
	return null;
}

}
