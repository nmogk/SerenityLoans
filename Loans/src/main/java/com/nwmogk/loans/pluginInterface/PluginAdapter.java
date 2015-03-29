package com.nwmogk.loans.pluginInterface;

import java.util.UUID;


public interface PluginAdapter {
	
	String getPlayerName(UUID playerId);
	
	boolean isPlayerOnline(UUID playerId);

}
