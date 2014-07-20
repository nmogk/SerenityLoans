package com.nwmogk.bukkit.loans.api;

public interface CreditScore {
	
	public double updateScore(double previousScore, double measurement, double parameter, double covariance);

}
