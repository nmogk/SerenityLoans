package com.nwmogk.bukkit.loans.event;

import java.util.EventListener;

public interface LoanStateUpdateListener extends EventListener {

	public void loanStateUpdated(LoanStateUpdateEvent lsue);
	
}
