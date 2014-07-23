package com.nwmogk.bukkit.loans.object;

import java.sql.Date;

import com.nwmogk.bukkit.loans.api.FinancialEntity;

public class MembershipRecord {
	
	private final FinancialInstitution institute;
	private final FinancialEntity member;
	private final Date joinDate;
	private final String roles;
	private final int shares;
	
	public MembershipRecord(FinancialInstitution institute, FinancialEntity member, Date joinDate, String roles, int shares){
		this.institute = institute;
		this.member = member;
		this.joinDate = joinDate;
		this.roles = roles;
		this.shares = shares;
	}
	
	public FinancialInstitution getMemberOf(){
		return institute;
	}
	
	public FinancialEntity getEntity(){
		return member;
	}
	
	public Date getJoinDate(){
		return joinDate;
	}
	
	public String getRoles(){
		return roles;
	}
	
	public int getShares(){
		return shares;
	}

}
