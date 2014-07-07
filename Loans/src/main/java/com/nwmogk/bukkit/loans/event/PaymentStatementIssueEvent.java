package com.nwmogk.bukkit.loans.event;

import com.nwmogk.bukkit.loans.object.Loan;
import com.nwmogk.bukkit.loans.object.PaymentStatement;

public class PaymentStatementIssueEvent {
	
	public final Loan loan;
	public final PaymentStatement ps;
	
	private String message;

	public PaymentStatementIssueEvent(Loan loan, PaymentStatement ps) {
		this(loan, ps, null);
	}

	public PaymentStatementIssueEvent(Loan loan, PaymentStatement ps,
			String message) {
		this.loan = loan;
		this.ps = ps;
		this.message = message;
	}
	
	public String getMessage(){
		return message;
	}

}
