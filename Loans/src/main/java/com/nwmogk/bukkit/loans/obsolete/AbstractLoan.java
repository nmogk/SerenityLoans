package com.nwmogk.bukkit.loans.obsolete;

import java.util.LinkedList;

import com.nwmogk.bukkit.loans.api.Loanable;
import com.nwmogk.bukkit.loans.event.LoanStateUpdateEvent;
import com.nwmogk.bukkit.loans.event.LoanStateUpdateListener;
import com.nwmogk.bukkit.loans.event.PaymentStatementIssueEvent;
import com.nwmogk.bukkit.loans.event.PaymentStatementIssueListener;

/**
 * This class provides an implementation of the event based duties of
 * loans. It allows registration and notification of listeners for 
 * subclasses.
 * 
 * @author Nathan Mogk
 *
 */

public abstract class AbstractLoan implements Loanable {

	// Registered listeners
		private LinkedList<LoanStateUpdateListener> LSULs = new LinkedList<LoanStateUpdateListener>(); 
		private LinkedList<PaymentStatementIssueListener> PSILs = new LinkedList<PaymentStatementIssueListener>();
		
		/**
		 * This method adds a LoanStateUpdateListener to the list of listeners
		 * that will be notified when the loan state is changed.
		 * 
		 * @param lsul
		 */
		public void registerLoanStateUpdateListener(LoanStateUpdateListener lsul){
			if(lsul == null)
				return;
			LSULs.add(lsul);
		}
		
		/**
		 * This method adds a PaymentStatementIssueListener to the list of listeners
		 * that will be notified when a payment statement is issued.
		 * 
		 * @param lsue
		 */
		public void registerPaymentStatementIssueListener(PaymentStatementIssueListener psil){
			if(psil == null)
				return;
			PSILs.add(psil);
		}
		
		/**
		 * This method removes the specified LoanStateUpdateListener from the listener
		 * list. The listener will no longer be notified of changes.
		 * 
		 * @param lsul
		 */
		public void unregisterLoanStateUpdateListener(LoanStateUpdateListener lsul){
			if(lsul == null)
				return;
			LSULs.remove(lsul);
		}
		
		/**
		 * This method removes a PaymentStatementIssueListener to the list of listeners
		 * that will be notified when a payment statement is issued.
		 * 
		 * @param lsue
		 */
		public void unregisterPaymentStatementIssueListener(PaymentStatementIssueListener psil){
			if(psil == null)
				return;
			PSILs.remove(psil);
		}
		
		/*
		 * This method takes the registered listeners and calls the loanStateUpdated(lsue)
		 * method on them. Calling methods should produce the loanStateUpdateEvent.
		 * 
		 * @param lsue
		 */
		protected void notifyLoanStateUpdateListeners(LoanStateUpdateEvent lsue){
			for(LoanStateUpdateListener lsul : LSULs)
				lsul.loanStateUpdated(lsue);
		}
		
		/*
		 * This method takes the registered listeners and calls the paymentStatementIssued(lsue)
		 * method on them. 
		 */
		protected void notifyPaymentStatementIssueListeners(PaymentStatementIssueEvent psie){
			for(PaymentStatementIssueListener psil : PSILs)
				psil.paymentStatementIssued(psie);
		}
}
