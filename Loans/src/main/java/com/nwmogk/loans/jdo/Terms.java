package com.nwmogk.loans.jdo;

import java.math.BigDecimal;
import java.util.UUID;

import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.nwmogk.loans.api.TermsType;

@PersistenceCapable
public class Terms {

	@Persistent( customValueStrategy = "uuid" )
	@PrimaryKey
	protected UUID				termsId;

	@Persistent( nullValue = NullValue.EXCEPTION )
	protected FinancialEntity	lender;

	@Persistent( nullValue = NullValue.EXCEPTION )
	protected TermsType			type;

	@Persistent( nullValue = NullValue.EXCEPTION )
	protected BigDecimal		principalValue;

	@Persistent( nullValue = NullValue.EXCEPTION )
	protected BigDecimal		interestRate;

	@Persistent( nullValue = NullValue.EXCEPTION )
	protected BigDecimal		minimumPayment;

	@Persistent( nullValue = NullValue.EXCEPTION )
	protected long				term;

	@Persistent( nullValue = NullValue.EXCEPTION )
	protected long				paymentTime;

	@Persistent( nullValue = NullValue.EXCEPTION )
	protected long				paymentFrequency;

	@Persistent
	protected long				compoundingPeriod;

	@Persistent
	protected long				gracePeriod;

	@Persistent
	protected long				serviceFeeFrequency;

	@Persistent
	protected BigDecimal		lateFee;

	@Persistent
	protected BigDecimal		serviceFee;

	public Terms( FinancialEntity lender, TermsType type,
			BigDecimal principalValue, BigDecimal interestRate,
			BigDecimal minimumPayment, long term, long paymentTime,
			long paymentFrequency, long compoundingPeriod, long gracePeriod,
			long serviceFeeFrequency, BigDecimal lateFee, BigDecimal serviceFee )
	{

		this.lender = lender;
		this.type = type;
		this.principalValue = principalValue;
		this.interestRate = interestRate;
		this.minimumPayment = minimumPayment;
		this.term = term;
		this.paymentTime = paymentTime;
		this.paymentFrequency = paymentFrequency;
		this.compoundingPeriod = compoundingPeriod;
		this.gracePeriod = gracePeriod;
		this.serviceFeeFrequency = serviceFeeFrequency;
		this.lateFee = lateFee;
		this.serviceFee = serviceFee;
	}

	public FinancialEntity getLender() {

		return this.lender;
	}

	public void setLender( FinancialEntity lender ) {

		this.lender = lender;
	}

	public TermsType getType() {

		return this.type;
	}

	public void setType( TermsType type ) {

		this.type = type;
	}

	public BigDecimal getPrincipalValue() {

		return this.principalValue;
	}

	public void setPrincipalValue( BigDecimal principalValue ) {

		this.principalValue = principalValue;
	}

	public BigDecimal getInterestRate() {

		return this.interestRate;
	}

	public void setInterestRate( BigDecimal interestRate ) {

		this.interestRate = interestRate;
	}

	public BigDecimal getMinimumPayment() {

		return this.minimumPayment;
	}

	public void setMinimumPayment( BigDecimal minimumPayment ) {

		this.minimumPayment = minimumPayment;
	}

	public long getTerm() {

		return this.term;
	}

	public void setTerm( long term ) {

		this.term = term;
	}

	public long getPaymentTime() {

		return this.paymentTime;
	}

	public void setPaymentTime( long paymentTime ) {

		this.paymentTime = paymentTime;
	}

	public long getPaymentFrequency() {

		return this.paymentFrequency;
	}

	public void setPaymentFrequency( long paymentFrequency ) {

		this.paymentFrequency = paymentFrequency;
	}

	public long getCompoundingPeriod() {

		return this.compoundingPeriod;
	}

	public void setCompoundingPeriod( long compoundingPeriod ) {

		this.compoundingPeriod = compoundingPeriod;
	}

	public long getGracePeriod() {

		return this.gracePeriod;
	}

	public void setGracePeriod( long gracePeriod ) {

		this.gracePeriod = gracePeriod;
	}

	public long getServiceFeeFrequency() {

		return this.serviceFeeFrequency;
	}

	public void setServiceFeeFrequency( long serviceFeeFrequency ) {

		this.serviceFeeFrequency = serviceFeeFrequency;
	}

	public BigDecimal getLateFee() {

		return this.lateFee;
	}

	public void setLateFee( BigDecimal lateFee ) {

		this.lateFee = lateFee;
	}

	public BigDecimal getServiceFee() {

		return this.serviceFee;
	}

	public void setServiceFee( BigDecimal serviceFee ) {

		this.serviceFee = serviceFee;
	}

	public UUID getTermsID() {

		return this.termsId;
	}

}
