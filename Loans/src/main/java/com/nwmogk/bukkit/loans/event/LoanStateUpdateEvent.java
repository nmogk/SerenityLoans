package com.nwmogk.bukkit.loans.event;

import com.nwmogk.bukkit.loans.LoanState;
import com.nwmogk.bukkit.loans.object.Loan;

public class LoanStateUpdateEvent {

	public final Loan loan;
	public final LoanState previousState;
	public final LoanState newState;
	
	public LoanStateUpdateEvent(Loan loan, LoanState last, LoanState now) {
		this.loan = loan;
		previousState = last;
		newState = now;
	}

}
