package com.nwmogk.loans.jdo;

import java.util.Date;
import java.util.UUID;

import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
public class Offer {

	@Persistent( customValueStrategy = "uuid" )
	@PrimaryKey
	protected UUID				offerId;

	@Persistent( nullValue = NullValue.EXCEPTION )
	protected FinancialEntity	borrower;

	@Persistent( nullValue = NullValue.EXCEPTION )
	protected Date				expirationDate;

	@Persistent
	protected boolean			offerSent	= false;

	@Persistent( nullValue = NullValue.EXCEPTION )
	protected Terms				terms;

	public Offer( FinancialEntity borrower, Date expirationDate, Terms terms ) {

		this.borrower = borrower;
		this.expirationDate = expirationDate;
		this.terms = terms;
	}

	public FinancialEntity getBorrower() {

		return this.borrower;
	}

	public void setBorrower( FinancialEntity borrower ) {

		this.borrower = borrower;
	}

	public Date getExpirationDate() {

		return this.expirationDate;
	}

	public void setExpirationDate( Date expirationDate ) {

		this.expirationDate = expirationDate;
	}

	public boolean isOfferSent() {

		return this.offerSent;
	}

	public void setOfferSent( boolean offerSent ) {

		this.offerSent = offerSent;
	}

	public Terms getTerms() {

		return this.terms;
	}

	public void setTerms( Terms terms ) {

		this.terms = terms;
	}

	public UUID getOfferId() {

		return this.offerId;
	}

}
