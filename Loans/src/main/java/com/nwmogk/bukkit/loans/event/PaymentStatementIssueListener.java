package com.nwmogk.bukkit.loans.event;

import java.util.EventListener;

public interface PaymentStatementIssueListener extends EventListener {

	public void paymentStatementIssued(PaymentStatementIssueEvent psie);

}
