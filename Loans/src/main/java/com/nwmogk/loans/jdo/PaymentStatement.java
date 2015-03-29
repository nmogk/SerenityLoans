package com.nwmogk.loans.jdo;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
public class PaymentStatement {
	
	@Persistent( customValueStrategy = "uuid" )
	@PrimaryKey
	protected UUID statementId;
	
	@Persistent( nullValue = NullValue.EXCEPTION )
	protected Loan loan;
	
	@Persistent( nullValue = NullValue.EXCEPTION )
	protected BigDecimal billAmount;
	
	@Persistent( customValueStrategy = "timestamp" )
	protected Timestamp statementDate;
	
	@Persistent
	protected Date dueDate;
	
	@Persistent
	protected BigDecimal billAmountPaid = new BigDecimal(0);
	
	@Persistent
	protected BigDecimal extraPrincipalPaid = new BigDecimal(0);
	
	@Persistent
	protected BigDecimal extraInterestPaid = new BigDecimal(0);
	
	@Persistent
	protected BigDecimal extraFeesPaid = new BigDecimal(0);

	public PaymentStatement( Loan loan, BigDecimal billAmount, Date dueDate ) {

		this.loan = loan;
		this.billAmount = billAmount;
		this.dueDate = dueDate;
	}

	
	public UUID getStatementId() {
	
		return this.statementId;
	}

	
	public Loan getLoan() {
	
		return this.loan;
	}

	
	public BigDecimal getBillAmount() {
	
		return this.billAmount;
	}

	
	public Timestamp getStatementDate() {
	
		return this.statementDate;
	}

	
	public Date getDueDate() {
	
		return this.dueDate;
	}

	
	public BigDecimal getBillAmountPaid() {
	
		return this.billAmountPaid;
	}

	
	public BigDecimal getExtraPrincipalPaid() {
	
		return this.extraPrincipalPaid;
	}

	
	public BigDecimal getExtraInterestPaid() {
	
		return this.extraInterestPaid;
	}

	
	public BigDecimal getExtraFeesPaid() {
	
		return this.extraFeesPaid;
	}

	
	public void setLoan( Loan loan ) {
	
		this.loan = loan;
	}

	
	public void setBillAmount( BigDecimal billAmount ) {
	
		this.billAmount = billAmount;
	}

	
	public void setDueDate( Date dueDate ) {
	
		this.dueDate = dueDate;
	}

	
	public void setBillAmountPaid( BigDecimal billAmountPaid ) {
	
		this.billAmountPaid = billAmountPaid;
	}

	
	public void setExtraPrincipalPaid( BigDecimal extraPrincipalPaid ) {
	
		this.extraPrincipalPaid = extraPrincipalPaid;
	}

	
	public void setExtraInterestPaid( BigDecimal extraInterestPaid ) {
	
		this.extraInterestPaid = extraInterestPaid;
	}

	
	public void setExtraFeesPaid( BigDecimal extraFeesPaid ) {
	
		this.extraFeesPaid = extraFeesPaid;
	}
	
	

}
