package com.nwmogk.loans.jdo;

import java.sql.Timestamp;
import java.util.UUID;

import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
public class CashTransaction {

	@Persistent( customValueStrategy = "uuid" )
	@PrimaryKey
	protected UUID				transactionId;

	@Persistent( nullValue = NullValue.EXCEPTION )
	protected FinancialEntity	from;

	@Persistent( nullValue = NullValue.EXCEPTION )
	protected FinancialEntity	to;

	@Persistent( nullValue = NullValue.EXCEPTION )
	protected Timestamp			transactionTime;

	public CashTransaction( FinancialEntity from, FinancialEntity to,
			Timestamp transactionTime )
	{

		this.from = from;
		this.to = to;
		this.transactionTime = transactionTime;
	}

	public UUID getTransactionId() {

		return this.transactionId;
	}

	public FinancialEntity getFrom() {

		return this.from;
	}

	public FinancialEntity getTo() {

		return this.to;
	}

	public Timestamp getTransactionTime() {

		return this.transactionTime;
	}

}
