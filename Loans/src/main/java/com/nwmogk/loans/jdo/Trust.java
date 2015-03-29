package com.nwmogk.loans.jdo;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

@PersistenceCapable
public class Trust {
	
	@Persistent
	protected boolean ignoreOffers = false;
	
	@Persistent
	protected int trustLevel = 0;

	
	public boolean isIgnoreOffers() {
	
		return this.ignoreOffers;
	}

	
	public int getTrustLevel() {
	
		return this.trustLevel;
	}

	
	public void setIgnoreOffers( boolean ignoreOffers ) {
	
		this.ignoreOffers = ignoreOffers;
	}

	
	public void setTrustLevel( int trustLevel ) {
	
		this.trustLevel = trustLevel;
	}
	
	

}
