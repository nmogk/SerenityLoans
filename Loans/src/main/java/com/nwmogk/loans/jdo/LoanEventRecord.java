package com.nwmogk.loans.jdo;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.nwmogk.loans.api.LoanEventType;

@PersistenceCapable
public class LoanEventRecord implements Comparable<LoanEventRecord> {
	
	@Persistent(customValueStrategy = "uuid")
	@PrimaryKey
	protected UUID loanEventId;
	
	@Persistent( nullValue = NullValue.EXCEPTION )
	protected Loan loan;
	
	@Persistent( customValueStrategy = "timestamp" )
	protected Timestamp eventTime;
	
	@Persistent( nullValue = NullValue.EXCEPTION )
	protected LoanEventType type;
	
	@Persistent( nullValue = NullValue.EXCEPTION )
	protected BigDecimal value;

	public LoanEventRecord( Loan loan, LoanEventType type, BigDecimal value ) {

		this.loan = loan;
		this.type = type;
		this.value = value;
	}

	public int compareTo( LoanEventRecord o ) {

		return eventTime.compareTo( o.getEventTime() );
	}

	
	public UUID getLoanEventId() {
	
		return this.loanEventId;
	}

	
	public Loan getLoan() {
	
		return this.loan;
	}

	
	public Timestamp getEventTime() {
	
		return this.eventTime;
	}

	
	public LoanEventType getType() {
	
		return this.type;
	}

	
	public BigDecimal getValue() {
	
		return this.value;
	}

	
	public void setLoan( Loan loan ) {
	
		this.loan = loan;
	}

	
	public void setType( LoanEventType type ) {
	
		this.type = type;
	}

	
	public void setValue( BigDecimal value ) {
	
		this.value = value;
	}
	
	

}
