package com.nwmogk.loans.jdo;

import java.math.BigDecimal;

import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
public class Garnishment {

	@Persistent
	@PrimaryKey
	protected FinancialEntity	entityId;

	@Persistent
	@PrimaryKey
	protected FinancialEntity	payTo;

	@Persistent
	protected BigDecimal		garnished	= new BigDecimal( 0 );

	@Persistent( nullValue = NullValue.EXCEPTION )
	protected Loan				applyTo;

	@Persistent
	protected BigDecimal		maximumAmount;

	@Persistent
	protected BigDecimal		absoluteGarnish;

	@Persistent
	protected double			percentGarnish;

	public Garnishment( FinancialEntity entityId, FinancialEntity payTo,
			Loan applyTo, BigDecimal maximumAmount, BigDecimal absoluteGarnish,
			double percentGarnish )
	{

		this.entityId = entityId;
		this.payTo = payTo;
		this.applyTo = applyTo;
		this.maximumAmount = maximumAmount;
		this.absoluteGarnish = absoluteGarnish;
		this.percentGarnish = percentGarnish;
	}

	public FinancialEntity getEntityId() {

		return this.entityId;
	}

	public FinancialEntity getPayTo() {

		return this.payTo;
	}

	public BigDecimal getGarnished() {

		return this.garnished;
	}

	public Loan getApplyTo() {

		return this.applyTo;
	}

	public BigDecimal getMaximumAmount() {

		return this.maximumAmount;
	}

	public BigDecimal getAbsoluteGarnish() {

		return this.absoluteGarnish;
	}

	public double getPercentGarnish() {

		return this.percentGarnish;
	}

	public void setGarnished( BigDecimal garnished ) {

		this.garnished = garnished;
	}

	public void setApplyTo( Loan applyTo ) {

		this.applyTo = applyTo;
	}

	public void setMaximumAmount( BigDecimal maximumAmount ) {

		this.maximumAmount = maximumAmount;
	}

	public void setAbsoluteGarnish( BigDecimal absoluteGarnish ) {

		this.absoluteGarnish = absoluteGarnish;
	}

	public void setPercentGarnish( double percentGarnish ) {

		this.percentGarnish = percentGarnish;
	}

}
