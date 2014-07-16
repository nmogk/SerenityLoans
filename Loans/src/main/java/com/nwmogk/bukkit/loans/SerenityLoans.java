/**
 * ========================================================================
 *                               DESCRIPTION
 * ========================================================================
 * This file is part of the SerenityLoans Bukkit plugin project.
 * 
 * File: SerenityLoans.java
 * Contributing Authors: Nathan W Mogk
 * 
 * This class is the core of the SerenityLoans plugin. This class provides
 * the setup implementation for the rest of the plugin and is the entry
 * point for command handling. This class passes most operational functions
 * to other classes in the package. It facilitates communication between
 * these functional classes by provides protected objects. This class sets
 * up a mySQL database and builds the necessary tables.
 * 
 * 
 * ========================================================================
 *                            LICENSE INFORMATION
 * ========================================================================
 * 
 * Copyright 2014 Nathan W Mogk
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * 
 * ========================================================================
 *                                CHANGE LOG
 * ========================================================================
 *    Date          Name                  Description              Defect #
 * ----------  --------------  ----------------------------------  --------
 * 2014-xx-xx  nmogk           Initial release for v0.1
 * 
 * 
 */

package com.nwmogk.bukkit.loans;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.*;
import org.bukkit.scheduler.BukkitRunnable;

import com.nwmogk.bukkit.loans.api.PlayerType;
import com.nwmogk.bukkit.loans.command.LoanHandler;
import com.nwmogk.bukkit.loans.exception.DatabaseVersionMismatchException;
import com.nwmogk.bukkit.loans.listener.PlayerLoginListener;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public final class SerenityLoans extends JavaPlugin {
	
	public static Logger log;
	
    // Incrementing these numbers will force a rebuild of the database.
    public static final int dbMajorVersion = 0;
    public static final int dbMinorVersion = 7;
    
    private static SerenityLoans plugin;
	
    public static int debugLevel;
    
    public Connection conn = null;
    
    public PlayerManager playerManager;
    public EconomyManager econ;
	public LoanManager loanManager;
	public OfferManager offerManager;
	
	public ExecutorService threads;
    
	public void onEnable(){
		
		log = Logger.getLogger("Minecraft." + getDescription().getName());
		
		this.threads = Executors.newCachedThreadPool();
		
		this.saveDefaultConfig();
		plugin = this;
		
		if(getConfig().contains("options.logging.debug-level"))
			debugLevel = getConfig().getInt("options.logging.debug-level");
		else
			debugLevel = 2;
		
		if(debugLevel >= 4)
			logInfo("Main thread ID: " + Thread.currentThread().getId() + ".");
		
		String squrl = "jdbc:mysql://";
		
		if(getConfig().contains("mysql.host") && getConfig().contains("mysql.port") && getConfig().contains("mysql.databasename")){
			squrl += getConfig().getString("mysql.host");
			squrl += ":" + getConfig().getString("mysql.port");
			squrl += "/" + getConfig().getString("mysql.databasename");
		}
		else {
			logFail("Database configuration info not found, disabling...");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		if(getConfig().contains("mysql.username") || getConfig().contains("mysql.password")){
			squrl += "?";
			if(debugLevel >= 1)
				logInfo("Using username and password info.");
		}
		
		if(getConfig().contains("mysql.username")){
			squrl += "user=" + getConfig().getString("mysql.username") + (getConfig().contains("mysql.password")?"&":"");
			if(debugLevel >= 2)
				logInfo("Username given.");
		}
		if(getConfig().contains("mysql.password")){
			squrl += "password=" + getConfig().getString("mysql.password");
			if(debugLevel >= 2)
				logInfo("Password given.");
		}
		
		if(debugLevel >= 2)
			logInfo("Database configuration loaded. Setting up...");
		
		try {
			conn = DriverManager.getConnection(squrl);
			
			if(conn == null)
				throw new SQLException("conn null.");
		} catch (SQLException e) {
			if(debugLevel >=2)
				logFail(e.getMessage());
			
			logFail("Unable to connect to database! Disabling...");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		playerManager = new PlayerManager(this);
		offerManager = new OfferManager(this);
		
		try {
			if(buildRequired())
				setupTables();
		} catch (SQLException e) {
			if(debugLevel >=2)
				logFail(e.getMessage());
			
			logFail("Unable to build database! Disabling...");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}	catch (DatabaseVersionMismatchException e) {
			logFail(String.format("%s Disabling...", e.getMessage()));
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		
		// Attempt to add online players to the loan system.
		if(debugLevel >= 1)
			logInfo("Attempting to add players to the system...");
		
		playerManager.addPlayers(getServer().getOnlinePlayers());
		
		getServer().getPluginManager().registerEvents(new PlayerLoginListener(this), this);
		
		/*
		 * TODO next:
		 * Setup listeners/handlers
		 * 
		 * Remove references to the regular configuration pathway in other classes. Use only the Conf object
		 * 
		 * Work out economy stuff
		 */
		
		if(debugLevel >=1)
			logInfo("Setting up economy functions.");
		
		econ = new EconomyManager(this);
		
		if (!econ.isInitialized() ) {
            logFail("Disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
		
		loanManager = new LoanManager(this);
//        setupPermissions();
//        setupChat();
		
		// Populate tables with online users
		
		if(debugLevel >= 3)
			logInfo("Setting command handlers.");
		
		getCommand("loan").setExecutor(new LoanHandler(this));
		
		if(debugLevel >= 2)
			logInfo("Scheduling repeating upates.");

        getServer().getScheduler().runTaskTimerAsynchronously(this, new BukkitRunnable(){public void run(){loanManager.updateAll();offerManager.updateAll();}}, 0, Conf.getUpdateTime());
	}
	
	public void onDisable(){
		
		threads.shutdown();
		this.getServer().getScheduler().cancelTasks(this);
		
		try {
			if(conn != null)
				conn.close();
		} catch (SQLException e) {
			logFail(e.getMessage());
		}
		
	}
	


//	 private boolean setupChat() {
//	     RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
//	     chat = rsp.getProvider();
//	     return chat != null;
//	 }

//	 private boolean setupPermissions() {
//	     RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
//	     perms = rsp.getProvider();
//	     return perms != null;
//	 }
	 
	 private boolean buildRequired() throws SQLException, DatabaseVersionMismatchException{
		 Statement statement = null;
		 
		 try{
			statement = conn.createStatement();
			
			ResultSet tables = statement.executeQuery("SHOW Tables;");
			
			if(!tables.next()){
				return true;
			}
			
			boolean hasInfo = false;
			
			hasInfo |= tables.getString(1).equalsIgnoreCase("Info");
			while(tables.next())
				hasInfo |= tables.getString(1).equalsIgnoreCase("Info");
			
			if(!hasInfo)
				throw new DatabaseVersionMismatchException("Info table missing!");
			
			ResultSet version = statement.executeQuery("SELECT * FROM Info;");
			
			version.next();
			
			if(version.getInt("DBMajor") != dbMajorVersion || version.getInt("DBMinor") != dbMinorVersion){
				String ruhroh = "DB Version mismatch. V" + version.getInt("DBMajor") + "." + version.getInt("DBMinor")
						+ " needs V" + dbMajorVersion + "." + dbMinorVersion + ".";
				throw new DatabaseVersionMismatchException(ruhroh);
			}
			
		 } catch (SQLException e) {
			 if(debugLevel >=2)
					logFail(e.getMessage());
				
			 throw e;
		 } finally {
			 if(statement != null){statement.close();}
		 }
		 
		 return false;
	 }
	 
	 private void setupTables() throws SQLException{
		 
		 boolean success = true;
		 
		 int defaultCreditScore = 465;
		 int decimals = 2;
		 double dissipationFactor = 0.15;
		 
		 if(getConfig().contains("trust.credit-score.no-history-score")){
			 defaultCreditScore = getConfig().getInt("trust.credit-score.no-history-score");
			 if(debugLevel >=3)
				logInfo("Loaded default credit score.");
		 }
		 
		 if(getConfig().contains("trust.credit-score.dissipation-factor")){
			 dissipationFactor = getConfig().getInt("trust.credit-score.dissipation-factor");
			 if(debugLevel >=3)
				logInfo("Loaded dissipation factor.");
		 }
		 
		 if(getConfig().contains("economy.currency.fractional-digits")){
			 decimals = getConfig().getInt("economy.currency.fractional-digits");
			 if(debugLevel >=3)
				logInfo("Loaded currency digits.");
		 }
		 
		 String financialEntityTable = 
				 "CREATE TABLE FinancialEntities"
					+ "(" 
					+ "UserID varchar(36) NOT NULL,"
					+ "Type ENUM('Player','Bank','CreditUnion','Town/Faction','Employer') NOT NULL DEFAULT 'Player',"
					+ "Cash DECIMAL(10," + decimals +"),"
					+ "CreditScore double DEFAULT "+ defaultCreditScore + ","
					+ "LastSystemUse TIMESTAMP NOT NULL DEFAULT NOW() ON UPDATE NOW(),"
					+ "PRIMARY KEY (UserID)"
					+ ");";
		 
		 String financialInstitutionsTable = 
				 "CREATE TABLE FinancialInstitutions"
				 	+ "("
				 	+ "BankID varchar(36) NOT NULL,"
				 	+ "Name varchar(255) NOT NULL,"
				 	+ "Manager varchar(36) NOT NULL,"
				 	+ "UNIQUE (Name),"
				 	+ "PRIMARY KEY (BankID),"
				 	+ "FOREIGN KEY (BankID) REFERENCES FinancialEntities(UserID),"
				 	+ "FOREIGN KEY (Manager) REFERENCES FinancialEntities(UserID)"
				 	+ ");";
		 
		
		 String trustTable = 
				 "CREATE TABLE Trust"
				 	+ "("
				 	+ "UserID varchar(36) NOT NULL,"
				 	+ "TargetID varchar(36) NOT NULL,"
				 	+ "TrustLevel int NOT NULL DEFAULT 0,"
				 	+ "IgnoreOffers ENUM('true','false') NOT NULL DEFAULt 'false',"
				 	+ "CONSTRAINT uc_relationID PRIMARY KEY (UserID,TargetID),"
				 	+ "FOREIGN KEY (UserID) REFERENCES FinancialEntities(UserID),"
				 	+ "FOREIGN KEY (TargetID) REFERENCES FinancialEntities(UserID)"
				 	+ ");";
		 	 
		 
		 String creditHistoryTable = 
				 "CREATE TABLE CreditHistory"
				 	+ "("
				 	+ "ItemID int NOT NULL AUTO_INCREMENT,"
				 	+ "UserID varchar(36) NOT NULL,"
				 	+ "EventType ENUM('Bankruptcy','Payment','MinPayment','MissedPayment','Payoff','LoanStart', 'LoanClose', 'CreditLimitReached', 'LoanModified') NOT NULL,"
				 	+ "ScoreValue double NOT NULL,"
				 	+ "Parameter double NOT NULL DEFAULT " + dissipationFactor + ","
				 	+ "Notes TEXT,"
				 	+ "EventTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
				 	+ "PRIMARY KEY (ItemID),"
				 	+ "FOREIGN KEY (UserID) REFERENCES FinancialEntities(UserID)"
				 	+ ");";
		 
		 String membershipTable = 
				 "CREATE TABLE Memberships"
				 	+ "("
				 	+ "UserID varchar(36) NOT NULL,"
				 	+ "MemberOf varchar(36) NOT NULL,"
				 	+ "JoinDate DATE NOT NULL,"
					+ "CONSTRAINT uc_memberID PRIMARY KEY (UserID,MemberOf),"
					+ "FOREIGN KEY (UserID) REFERENCES FinancialEntities(UserID),"
					+ "FOREIGN KEY (MemberOf) REFERENCES FinancialEntities(UserID)"
	 				+ ");";
		 
		 String loanTable = 
				 "CREATE TABLE Loans"
				 	+ "("
				 	+ "LoanID int NOT NULL AUTO_INCREMENT,"
				 	+ "LenderID varchar(36) NOT NULL,"
				 	+ "BorrowerID varchar(36) NOT NULL,"
				 	+ "Terms int NOT NULL,"
				 	+ "AutoPay ENUM('true','false') NOT NULL DEFAULT 'false',"
					+ "Balance DECIMAL(9," + decimals + ") NOT NULL,"
				 	+ "InterestBalance DECIMAL(9," + decimals + ") DEFAULT 0.0,"
				 	+ "FeeBalance DECIMAL(9," + decimals + ") DEFAULT 0.0,"
				 	+ "StartDate TIMESTAMP NOT NULL DEFAULT NOW(),"
				 	+ "LastUpdate TIMESTAMP NULL,"
				 	+ "Open ENUM('true', 'false') NOT NULL DEFAULT 'true',"
				 	+ "PRIMARY KEY (LoanID),"
				 	+ "FOREIGN KEY (LenderID) REFERENCES FinancialEntities (UserID),"
				 	+ "FOREIGN KEY (BorrowerID) REFERENCES FinancialEntities (UserID),"
				 	+ "FOREIGN KEY (Terms) REFERENCES PreparedOffers (OfferID)"
				 	+ ");";
		 
		 String loanEventsTable = 
				 "CREATE TABLE LoanEvents"
				 	+ "("
				 	+ "LoanEventID int NOT NULL AUTO_INCREMENT,"
				 	+ "LoanID int NOT NULL,"
				 	+ "EventTime TIMESTAMP NOT NULL DEFAULT NOW(),"
				 	+ "EventType ENUM('AccrueInterest','CompoundInterest','ServiceFee','LateFee','PaymentDue','PaymentMade','StatementOut','Open','Close','ExtraPrincipalPaid','ExtraInterestPaid','ExtraFeesPaid') NOT NULL,"
				 	+ "Amount DECIMAL(9,"+ decimals + "),"
				 	+ "Executed ENUM('true', 'false') NOT NULL DEFAULT 'false',"
				 	+ "PRIMARY KEY (LoanEventID),"
				 	+ "FOREIGN KEY (LoanID) REFERENCES Loans (LoanID)"
				 	+ ");";
		 
		 String paymentStatementsTable = 
				 "CREATE TABLE PaymentStatements"
				 	+ "("
				 	+ "StatementID int NOT NULL AUTO_INCREMENT,"
				 	+ "LoanID int NOT NULL,"
				 	+ "BillAmount DECIMAL(9," + decimals + ") NOT NULL,"
				 	+ "Minimum DECIMAL(9," + decimals + ") NOT NULL,"
				 	+ "StatementDate TIMESTAMP DEFAULT 0,"
				 	+ "DueDate TIMESTAMP DEFAULT 0,"
				 	+ "BillAmountPaid DECIMAL(9," + decimals + ") NOT NULL DEFAULT 0,"
				 	+ "AdditionalPrincipal DECIMAL(9," + decimals + ") NOT NULL DEFAULT 0,"
				 	+ "AdditionalInterest DECIMAL(9," + decimals + ") NOT NULL DEFAULT 0,"
				 	+ "AdditionalFees DECIMAL(9," + decimals + ") NOT NULL DEFAULT 0,"
				 	+ "PRIMARY KEY (StatementID),"
				 	+ "FOREIGN KEY (LoanID) REFERENCES Loans (LoanID)"
				 	+ ");";
		 
		 String preparedOffersTable = 
				 "CREATE TABLE PreparedOffers"
				 	+ "("
				 	+ "OfferID int NOT NULL AUTO_INCREMENT,"
				 	+ "LenderID varchar(36) NOT NULL,"
				 	+ "OfferName varchar(255),"
				 	+ "Value DECIMAL(9," + decimals + ") NOT NULL,"
				 	+ "InterestRate DECIMAL(6,3) NOT NULL,"
				 	+ "Term BIGINT NOT NULL,"
				 	+ "CompoundingPeriod BIGINT,"
				 	+ "GracePeriod BIGINT,"
				 	+ "PaymentTime BIGINT NOT NULL,"
				 	+ "PaymentFrequency BIGINT NOT NULL,"
				 	+ "LateFee DECIMAL(7," + decimals + "),"
				 	+ "MinPayment DECIMAL(9," + decimals + ") NOT NULL,"
				 	+ "ServiceFeeFrequency BIGINT,"
				 	+ "ServiceFee DECIMAL(7," + decimals + "),"
				 	+ "LoanType ENUM('Amortizing','Bullet','FixedFee','InterestOnly','Credit','Gift','Deposit','Bond','Salary') NOT NULL,"
				 	+ "PRIMARY KEY (OfferID),"
				 	+ "FOREIGN KEY (LenderID) REFERENCES FinancialEntities (UserID)"
				 	+ ");";

		 String offersTable = 
				 "CREATE TABLE Offers"
				 	+ "("
				 	+ "LenderID varchar(36) NOT NULL,"
				 	+ "BorrowerID varchar(36) NOT NULL,"
				 	+ "ExpirationDate TIMESTAMP DEFAULT 0,"
				 	+ "PreparedTerms int NOT NULL,"
				 	+ "Sent ENUM('true','false') NOT NULL DEFAULT 'false',"
				 	+ "CONSTRAINT uc_offerID PRIMARY KEY (LenderID,BorrowerID),"
				 	+ "FOREIGN KEY (LenderID) REFERENCES FinancialEntities (UserID),"
				 	+ "FOREIGN KEY (BorrowerID) REFERENCES FinancialEntities (UserID),"
				 	+ "FOREIGN KEY (PreparedTerms) REFERENCES PreparedOffers (OfferID)" 
				 	+ ");";

		 String signShopsTable = 
				 "CREATE TABLE SignShops"
				 	+ "("
				 	+ "X int NOT NULL,"
				 	+ "Y int NOT NULL,"
				 	+ "Z int NOT NULL,"
				 	+ "StoredOffer int NOT NULL,"
				 	+ "CONSTRAINT uc_shopID PRIMARY KEY (X,Y,Z),"
				 	+ "FOREIGN KEY (StoredOffer) REFERENCES PreparedOffers (OfferID)"
				 	+ ");";
		 
		 String loanView = 
				 "CREATE VIEW loans_all AS "
				 	+ "SELECT Loans.LoanID, Loans.LenderID, Loans.BorrowerID, Loans.StartDate, Loans.Balance, Loans.InterestBalance, Loans.FeeBalance, Loans.AutoPay, Loans.LastUpdate, PreparedOffers.Value, PreparedOffers.InterestRate, PreparedOffers.Term, PreparedOffers.CompoundingPeriod, PreparedOffers.GracePeriod, PreparedOffers.PaymentTime, PreparedOffers.PaymentFrequency, PreparedOffers.LateFee, PreparedOffers.MinPayment, PreparedOffers.ServiceFeeFrequency, PreparedOffers.ServiceFee " 
				 	+ "FROM Loans JOIN (PreparedOffers) "
		 			+ "ON Loans.Terms = PreparedOffers.OfferID;";
		 
		 String offerView = 
				"CREATE VIEW offer_view AS "
				 	+ "SELECT Offers.LenderID, Offers.BorrowerID,  Offers.ExpirationDate, PreparedOffers.Value, PreparedOffers.InterestRate, PreparedOffers.Term, PreparedOffers.CompoundingPeriod, PreparedOffers.GracePeriod, PreparedOffers.PaymentTime, PreparedOffers.PaymentFrequency, PreparedOffers.LateFee, PreparedOffers.MinPayment, PreparedOffers.ServiceFeeFrequency, PreparedOffers.ServiceFee, PreparedOffers.LoanType " 
				 	+ "FROM Offers JOIN (PreparedOffers) "
				 	+ "ON Offers.PreparedTerms = PreparedOffers.OfferID;";
		 
		 String debtorView =
				 "CREATE VIEW debtors AS "
				 	+ "SELECT DISTINCT UserID "
				 	+ "From Loans JOIN FinancialEntities "
				 	+ "ON Loans.BorrowerID=FinancialEntities.UserID;";
		 
		 String infoTable = 
				 "CREATE TABLE Info"
				 	+ "("
				 	+ "DBmajor int NOT NULL,"
				 	+ "DBminor int NOT NULL,"
				 	+ "CRscore_max double,"
				 	+ "CRscore_min double,"
				 	+ "CONSTRAINT uc_relationID PRIMARY KEY (DBmajor,DBminor)"
				 	+ ");";
		 
		 String writeVersion = "INSERT INTO Info VALUES(" + dbMajorVersion + "," + dbMinorVersion + ");";

		 Statement statement = null;
		 
		 try {
			statement = conn.createStatement();
			
			if(debugLevel >=2)
				logInfo("Statement created successfully.");
			
			statement.executeUpdate(financialEntityTable);
			
			if(debugLevel >=2)
				logInfo("Built FinancialEntities table successfully.");
			
			statement.executeUpdate(financialInstitutionsTable);
			
			if(debugLevel >=2)
				logInfo("Built FinancialInstitutions table successfully.");
			
			statement.executeUpdate(trustTable);
			
			if(debugLevel >=2)
				logInfo("Built Trust table successfully.");
			
			statement.executeUpdate(creditHistoryTable);
			
			if(debugLevel >=2)
				logInfo("Built CreditHistory table successfully.");
			
			statement.executeUpdate(membershipTable);
			
			if(debugLevel >=2)
				logInfo("Built Memberships table successfully.");
			
			statement.executeUpdate(preparedOffersTable);
			
			if(debugLevel >=2)
				logInfo("Built PreparedOffers table successfully.");
			
			statement.executeUpdate(loanTable);
			
			if(debugLevel >=2)
				logInfo("Built Loans table successfully.");
			
			statement.executeUpdate(loanEventsTable);
			
			if(debugLevel >=2)
				logInfo("Built LoanEvents table successfully.");
			
			statement.executeUpdate(paymentStatementsTable);
			
			if(debugLevel >=2)
				logInfo("Built PaymentStatements table successfully.");
			
			statement.executeUpdate(offersTable);
			
			if(debugLevel >=2)
				logInfo("Built Offers table successfully.");

			statement.executeUpdate(signShopsTable);
			
			if(debugLevel >=2)
				logInfo("Built SignShops table successfully.");
			
			statement.execute(loanView);
			
			if(debugLevel >=2)
				logInfo("Built Loans view successfully.");

			statement.executeUpdate(offerView);
			
			if(debugLevel >=2)
				logInfo("Built Offers view successfully.");
			
			statement.executeUpdate(debtorView);
			
			if(debugLevel >=2)
				logInfo("Built Debtors view successfully.");
		 
			statement.executeUpdate(infoTable);
			
			if(debugLevel >=2)
				logInfo("Built Info table successfully.");
		 
			statement.executeUpdate(writeVersion);
			
			if(debugLevel >=2)
				logInfo("Wrote version info successfully.");
			
			
			
		 } catch (SQLException e) {
			if(debugLevel >=2)
				logFail(e.getMessage());
			success = false;
		 } finally {
			 if(statement != null){statement.close();};
		 }
		 
		 if(!success){
			 logFail("Unable to build database tables! Disabling...");
			 getServer().getPluginManager().disablePlugin(this);
			 return;
		 }
		 
		 if(success && debugLevel >= 1)
			 logInfo("Database tables built successfully.");
		 
		 
		 double centralBankCash = 0;
		 if(getConfig().contains("economy.central-bank-balance"))
			 centralBankCash = getConfig().getDouble("economy.central-bank-balance");
			
		 int centralBankScore = 850;
		 if(getConfig().contains("trust.credit-score.score-range.max"))
			 centralBankScore = getConfig().getInt("trust.credit-score.score-range.max");
		 
		 boolean cbResult = playerManager.createFinancialInstitution("CentralBank", null, PlayerType.CREDIT_UNION, centralBankCash, centralBankScore);
		 
		 if(debugLevel >= 1){
			 if (cbResult)
				 logInfo("Central bank entry added.");
			 else
				 logWarn("Central bank entry failed.");
					
			}
	 }
	 
	 public static SerenityLoans getPlugin(){
		 return plugin;
	 }
	    
	    	 
	 public Connection getConnection(){
		 return conn;
	 }
	 
	 public EconomyManager getEcon(){
		 return econ;
	 }
	 
	 public static void logInfo(String message){
		 log.info(String.format("[%s] %s", "SerenityLoans", message));
	 }
	 
	 public static void logWarn(String message){
		 log.warning(String.format("[%s] %s", "SerenityLoans", message));
	 }
	 
	 public static void logFail(String message){
		 log.severe(String.format("[%s] %s", "SerenityLoans", message));
	 }
	 
	 public void scheduleMessage(CommandSender sender, String message){
			getServer().getScheduler().scheduleSyncDelayedTask(plugin, plugin.new MessageSender(sender, new String[]{message}));
	 }
	 
	 public void scheduleMessage(CommandSender sender, String[] message){
		 if(debugLevel >= 3)
				logInfo(String.format("Entering %s method. %s", "scheduleMessage(CommandSender, String[])", SerenityLoans.debugLevel >= 4? "Thread: " + Thread.currentThread().getId() : ""));
			getServer().getScheduler().scheduleSyncDelayedTask(plugin, plugin.new MessageSender(sender, message));
	 }
	 
	 
	 private class MessageSender extends BukkitRunnable {
		 
		 CommandSender sendTo;
		 String[] message;
		 
		 private MessageSender(CommandSender sendTo, String[] message){
			 this.sendTo = sendTo;
			 this.message = message;
		 }
		 
		 public void run(){
			 if(SerenityLoans.debugLevel >= 3)
					SerenityLoans.logInfo(String.format("MessageSender run() method. %s", SerenityLoans.debugLevel >= 4? "Thread: " + Thread.currentThread().getId() : "."));
				
			 
			 sendTo.sendMessage(message);
		 }
		 
		 
		 
	 }

	 
	 

}
