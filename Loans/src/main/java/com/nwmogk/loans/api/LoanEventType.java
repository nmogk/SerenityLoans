package com.nwmogk.loans.api;

public enum LoanEventType {

	COMPOUND( "CompoundInterest" ), INTERESTACCRUAL( "AccrueInterest" ), SERVICEFEE(
			"ServiceFee" ), LATEFEE( "LateFee" ), PAYMENTMADE( "PaymentMade" ), PAYMENTDUE(
			"PaymentDue" ), STATEMENTOUT( "StatementOut" ), OPEN( "Open" ), CLOSE(
			"Close" ), EXTRAPRINCIPALPAID( "ExtraPrincipalPaid" ), EXTRAINTERESTPAID(
			"ExtraInterestPaid" ), EXTRAFEESPAID( "ExtraFeesPaid" );

	private String	text;

	private LoanEventType( String text ) {

		this.text = text;
	}

	public String toString() {

		return text;
	}
	
	public static LoanEventType getFromString(String type) {
		
		for( LoanEventType t : LoanEventType.values() )
			if(type.equalsIgnoreCase( t.toString() ))
				return t;
		
		return null;
	}
}
