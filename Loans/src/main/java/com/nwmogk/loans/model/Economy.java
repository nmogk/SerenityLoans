package com.nwmogk.loans.model;

import java.io.File;
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
	
	public Economy(String propertiesFile, PluginAdapter plugin) throws FileNotFoundException, IOException {
		Properties properties = new Properties();
		properties.load( new FileInputStream(propertiesFile) );
		
		pm = JDOHelper.getPersistenceManagerFactory( new File(propertiesFile) ).getPersistenceManager();
		
		playerManager = new PlayerManager(this);
		
		this.plugin = plugin;
	}


}
