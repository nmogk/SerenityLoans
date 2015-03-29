package com.nwmogk.loans.model;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;

import com.nwmogk.loans.pluginInterface.PluginAdapter;


public class Economy {
	
	PersistenceManager pm;
	PlayerManager playerManager;
	PluginAdapter plugin;
	Conf cfg;
	
	public Economy(String datastoreSettingsFile, String behaviorSettingsFile, PluginAdapter plugin) throws FileNotFoundException, IOException {
		Properties dataProperties = new Properties();
		dataProperties.load( new FileInputStream(datastoreSettingsFile) );
		
		pm = JDOHelper.getPersistenceManagerFactory( dataProperties ).getPersistenceManager();
		
		playerManager = new PlayerManager(this);
		
		cfg = new Conf(behaviorSettingsFile);
		
		this.plugin = plugin;
	}


}
