package com.nwmogk.loans.jdo;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

import com.nwmogk.loans.api.EntityType;

@PersistenceCapable
public class FinancialInstitution extends FinancialEntity {

	@Persistent
	protected FinancialEntity				manager;

	@Persistent
	protected Map<FinancialEntity, Date>	members	= new HashMap<FinancialEntity, Date>();

	public FinancialInstitution( String name, EntityType type,
			FinancialEntity manager )
	{

		super( name, type );
		this.manager = manager;
	}

	public FinancialEntity getManager() {

		return this.manager;
	}

	public void setManager( FinancialEntity manager ) {

		this.manager = manager;
	}

	public Map<FinancialEntity, Date> getMembers() {

		return this.members;
	}

	public void setMembers( Map<FinancialEntity, Date> members ) {

		this.members = members;
	}

}
