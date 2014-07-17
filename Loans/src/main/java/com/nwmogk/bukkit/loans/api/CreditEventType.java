package com.nwmogk.bukkit.loans.api;

public enum CreditEventType {
	
	LOANOPEN, LOANCLOSE, PAID_BALANCE, PAID_MINIMUM, MISSED_PAYMENT, INACTIVITY, CREDIT_UTILIZATION, CREDIT_LIMIT, BANKRUPT, LOAN_MODIFY, OVERPAYMENT, FINAL_PAYMENT;
	
	public String toString(){
		switch(this){
		case BANKRUPT:
			return "Bankruptcy";
		case CREDIT_LIMIT:
			return "CreditLimitReached";
		case CREDIT_UTILIZATION:
			return "CreditUtilization";
		case FINAL_PAYMENT:
			return "Payoff";
		case INACTIVITY:
			return "Inactivity";
		case LOANCLOSE:
			return "LoanCloase";
		case LOANOPEN:
			return "LoanStart";
		case LOAN_MODIFY:
			return "LoanModified";
		case MISSED_PAYMENT:
			return "MissedPayment";
		case OVERPAYMENT:
			return "Overpayment";
		case PAID_BALANCE:
			return "Payment";
		case PAID_MINIMUM:
			return "MinPayment";
		default:
			return null;
		}
	}
}
