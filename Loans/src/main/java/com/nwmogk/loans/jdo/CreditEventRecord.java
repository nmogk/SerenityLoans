package com.nwmogk.loans.jdo;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.nwmogk.bukkit.loans.api.CreditEvent;
import com.nwmogk.bukkit.loans.api.CreditEventType;

@PersistenceCapable
public class CreditEventRecord implements Comparable<CreditEventRecord>, CreditEvent {
	
	@Persistent( customValueStrategy = "uuid" )
	@PrimaryKey
	protected UUID creditEventId;
	
	@Persistent( nullValue = NullValue.EXCEPTION )
	protected CreditEventType type;
	
	@Persistent( nullValue = NullValue.EXCEPTION )
	protected FinancialEntity userId;
	
	@Persistent( nullValue = NullValue.EXCEPTION )
	protected BigDecimal scoreValue;
	
	@Persistent( nullValue = NullValue.EXCEPTION )
	protected double parameter;
	
	@Persistent( customValueStrategy = "timestamp" )
	protected Timestamp eventTime;
	

	public CreditEventRecord( CreditEventType type, FinancialEntity userId,
			BigDecimal scoreValue, double parameter )
	{

		this.type = type;
		this.userId = userId;
		this.scoreValue = scoreValue;
		this.parameter = parameter;
	}



	public int compareTo( CreditEventRecord o ) {

		return eventTime.compareTo( o.getEventTime() );
	}



	
	public CreditEventType getType() {
	
		return this.type;
	}



	
	public void setType( CreditEventType type ) {
	
		this.type = type;
	}



	
	public FinancialEntity getUserId() {
	
		return this.userId;
	}



	
	public void setUserId( FinancialEntity userId ) {
	
		this.userId = userId;
	}



	
	public BigDecimal getScoreValue() {
	
		return this.scoreValue;
	}



	
	public void setScoreValue( BigDecimal scoreValue ) {
	
		this.scoreValue = scoreValue;
	}



	
	public double getParameter() {
	
		return this.parameter;
	}



	
	public void setParameter( double parameter ) {
	
		this.parameter = parameter;
	}



	
	public Timestamp getEventTime() {
	
		return this.eventTime;
	}



	
	public void setEventTime( Timestamp eventTime ) {
	
		this.eventTime = eventTime;
	}



	
	public UUID getCreditEventId() {
	
		return this.creditEventId;
	}



	public double getUpdateScore( double currentScore ) {

		return scoreValue.doubleValue();
	}



	public double getDissipationFactor() {

		return parameter;
	}
	
	

}
