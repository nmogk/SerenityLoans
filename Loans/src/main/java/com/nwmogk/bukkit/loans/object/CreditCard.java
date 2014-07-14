package com.nwmogk.bukkit.loans.object;

import java.sql.Timestamp;


public class CreditCard extends Loan{

	public CreditCard(int loanID, double balance, double interestBalance,
			double feeBalance, ImmutableOffer terms, Timestamp startDate,
			Timestamp lastUpdate) {
		super(loanID, balance, interestBalance, feeBalance, terms, startDate,
				lastUpdate, 0);
		
	};

	

}
