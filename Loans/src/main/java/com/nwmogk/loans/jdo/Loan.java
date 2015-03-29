package com.nwmogk.loans.jdo;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
public class Loan {

	@Persistent( customValueStrategy = "uuid" )
	@PrimaryKey
	protected UUID					loanId;

	@Persistent
	protected FinancialEntity		borrower;

	@Persistent
	protected Terms					terms;

	@Persistent
	protected BigDecimal			balance			= terms.getPrincipalValue();

	@Persistent
	protected BigDecimal			interestBalance	= new BigDecimal( 0 );

	@Persistent
	protected BigDecimal			feeBalance		= new BigDecimal( 0 );

	@Persistent( customValueStrategy = "timestamp" )
	protected Timestamp				startTime;

	@Persistent
	protected boolean				autoPay			= false;

	@Persistent
	protected List<LoanEventRecord>	loanHistory		= new LinkedList<LoanEventRecord>();

	public Loan( FinancialEntity borrower, Terms terms ) {

		this.borrower = borrower;
		this.terms = terms;
	}

	public UUID getLoanId() {

		return this.loanId;
	}

	public FinancialEntity getBorrower() {

		return this.borrower;
	}

	public Terms getTerms() {

		return this.terms;
	}

	public BigDecimal getBalance() {

		return this.balance;
	}

	public BigDecimal getInterestBalance() {

		return this.interestBalance;
	}

	public BigDecimal getFeeBalance() {

		return this.feeBalance;
	}

	public Timestamp getStartTime() {

		return this.startTime;
	}

	public boolean isAutoPay() {

		return this.autoPay;
	}

	public void setBorrower( FinancialEntity borrower ) {

		this.borrower = borrower;
	}

	public void setTerms( Terms terms ) {

		this.terms = terms;
	}

	public void setBalance( BigDecimal balance ) {

		this.balance = balance;
	}

	public void setInterestBalance( BigDecimal interestBalance ) {

		this.interestBalance = interestBalance;
	}

	public void setFeeBalance( BigDecimal feeBalance ) {

		this.feeBalance = feeBalance;
	}

	public void setAutoPay( boolean autoPay ) {

		this.autoPay = autoPay;
	}

	public List<LoanEventRecord> getLoanHistory() {

		return this.loanHistory;
	}

	public void setLoanHistory( List<LoanEventRecord> loanHistory ) {

		this.loanHistory = loanHistory;
	}

}
