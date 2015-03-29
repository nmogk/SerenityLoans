package com.nwmogk.loans.jdo;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Unique;

import com.nwmogk.bukkit.loans.Conf;
import com.nwmogk.bukkit.loans.Conf.CreditScoreSettings;
import com.nwmogk.loans.api.EntityType;

@PersistenceCapable
public class FinancialEntity {

	@Persistent( customValueStrategy = "uuid" )
	@PrimaryKey
	protected UUID							entityId;

	@Persistent( nullValue = NullValue.EXCEPTION )
	@Unique( name = "TYPE_NAME_UNIQ" )
	protected String						name;

	@Persistent( nullValue = NullValue.EXCEPTION )
	@Unique( name = "TYPE_NAME_UNIQ" )
	protected EntityType					type;

	@Persistent
	protected BigDecimal					cash			= new BigDecimal( 0 );

	@Persistent
	protected BigDecimal					creditScore		= new BigDecimal(
																	Conf.getCreditScoreSettings( CreditScoreSettings.NO_HISTORY ) );

	@Persistent( customValueStrategy = "timestamp" )
	protected Timestamp						lastSystemUse;

	@Persistent
	protected List<CreditEventRecord>		creditHistory	= new LinkedList<CreditEventRecord>();

	@Persistent
	protected Map<FinancialEntity, Trust>	trustMap		= new HashMap<FinancialEntity, Trust>();

	public FinancialEntity( String name, EntityType type ) {

		this.name = name;
		this.type = type;
	}

	public String getName() {

		return this.name;
	}

	public void setName( String name ) {

		this.name = name;
	}

	public BigDecimal getCash() {

		return this.cash;
	}

	public void setCash( BigDecimal cash ) {

		this.cash = cash;
	}

	public BigDecimal getCreditScore() {

		return this.creditScore;
	}

	public void setCreditScore( BigDecimal creditScore ) {

		this.creditScore = creditScore;
	}

	public UUID getEntityId() {

		return this.entityId;
	}

	public EntityType getType() {

		return this.type;
	}

	public Timestamp getLastSystemUse() {

		return this.lastSystemUse;
	}

	public List<CreditEventRecord> getCreditHistory() {

		return this.creditHistory;
	}

	public void setCreditHistory( List<CreditEventRecord> creditHistory ) {

		this.creditHistory = creditHistory;
	}

	
	public Map<FinancialEntity, Trust> getTrustMap() {
	
		return this.trustMap;
	}

	
	public void setTrustMap( Map<FinancialEntity, Trust> trustMap ) {
	
		this.trustMap = trustMap;
	}

}
