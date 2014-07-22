package net.milkbowl.vault.economy.plugins;

import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

import com.nwmogk.bukkit.loans.EconomyManager;
import com.nwmogk.bukkit.loans.SerenityLoans;
import com.nwmogk.bukkit.loans.api.EconResult;
import com.nwmogk.bukkit.loans.api.FinancialEntity;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

public class Economy_SerenityLoans implements Economy {
	
	// TODO Implement all methods
	
	private static final Logger log = Logger.getLogger("Minecraft");

	private final String name = "SerenityLoans Economy";
    private Plugin plugin = null;
    private EconomyManager economy = null;
    private SerenityLoans slPlug = null;
    
    public class EconomyServerListener implements Listener {
        Economy_SerenityLoans economy = null;

        public EconomyServerListener(Economy_SerenityLoans economy) {
            this.economy = economy;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPluginEnable(PluginEnableEvent event) {
            if (economy.economy == null) {
                Plugin serenityLoans = event.getPlugin();

                if (serenityLoans.getDescription().getName().equals("SerenityLoans")) {
                	economy.slPlug = (SerenityLoans) serenityLoans;
                    economy.economy = ((SerenityLoans) serenityLoans).econ;
                    log.info(String.format("[%s][Economy] %s hooked.", plugin.getDescription().getName(), economy.name));
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPluginDisable(PluginDisableEvent event) {
            if (event.getPlugin().getDescription().getName().equals("SerenityLoans")) {
                economy.economy = null;
                economy.slPlug = null;
                log.info(String.format("[%s][Economy] %s unhooked.", plugin.getDescription().getName(), economy.name));
            }
            
        }
    }
    
	public Economy_SerenityLoans(Plugin plugin){
		this.plugin = plugin;
		Bukkit.getServer().getPluginManager().registerEvents(new EconomyServerListener(this), plugin);

		if (economy == null) {
            Plugin serenityLoans = plugin.getServer().getPluginManager().getPlugin("SerenityLoans");
            
            if (serenityLoans != null && serenityLoans.isEnabled()) {
                slPlug = (SerenityLoans) serenityLoans;
            	economy = slPlug.econ;
                log.info(String.format("[%s][Economy] %s hooked.", plugin.getDescription().getName(), name));
            }
        }
	}

	public EconomyResponse bankBalance(String arg0) {
		return null;
	}

	public EconomyResponse bankDeposit(String arg0, double arg1) {
		return null;
	}

	public EconomyResponse bankHas(String arg0, double arg1) {
		return null;
	}

	public EconomyResponse bankWithdraw(String arg0, double arg1) {
		return null;
	}

	public EconomyResponse createBank(String arg0, String arg1) {
		return null;
	}

	public EconomyResponse createBank(String arg0, OfflinePlayer arg1) {
		return null;
	}

	public boolean createPlayerAccount(String arg0) {
		return false;
	}

	public boolean createPlayerAccount(OfflinePlayer arg0) {
		return false;
	}

	public boolean createPlayerAccount(String arg0, String arg1) {
		return false;
	}

	public boolean createPlayerAccount(OfflinePlayer arg0, String arg1) {
		return false;
	}

	public String currencyNamePlural() {
		return null;
	}

	public String currencyNameSingular() {
		return null;
	}

	public EconomyResponse deleteBank(String arg0) {
		return null;
	}

	public EconomyResponse depositPlayer(String arg0, double arg1) {
		return null;
	}

	public EconomyResponse depositPlayer(OfflinePlayer arg0, double arg1) {
		return null;
	}

	public EconomyResponse depositPlayer(String arg0, String arg1, double arg2) {
		return null;
	}

	public EconomyResponse depositPlayer(OfflinePlayer arg0, String arg1,
			double arg2) {
		return null;
	}

	public String format(double arg0) {
		return null;
	}

	public int fractionalDigits() {
		return 0;
	}

	public double getBalance(String arg0) {
		return 0;
	}

	public double getBalance(OfflinePlayer arg0) {
		return 0;
	}

	public double getBalance(String arg0, String arg1) {
		return 0;
	}

	public double getBalance(OfflinePlayer arg0, String arg1) {
		return 0;
	}

	public List<String> getBanks() {
		return null;
	}

	public String getName() {
		return null;
	}

	public boolean has(String arg0, double arg1) {
		return false;
	}

	public boolean has(OfflinePlayer arg0, double arg1) {
		return false;
	}

	public boolean has(String arg0, String arg1, double arg2) {
		return false;
	}

	public boolean has(OfflinePlayer arg0, String arg1, double arg2) {
		return false;
	}

	public boolean hasAccount(String arg0) {
		return false;
	}

	public boolean hasAccount(OfflinePlayer arg0) {
		return false;
	}

	public boolean hasAccount(String arg0, String arg1) {
		return false;
	}

	public boolean hasAccount(OfflinePlayer arg0, String arg1) {
		return false;
	}

	public boolean hasBankSupport() {
		return false;
	}

	public EconomyResponse isBankMember(String arg0, String arg1) {
		return null;
	}

	public EconomyResponse isBankMember(String arg0, OfflinePlayer arg1) {
		return null;
	}

	public EconomyResponse isBankOwner(String arg0, String arg1) {
		return null;
	}

	public EconomyResponse isBankOwner(String arg0, OfflinePlayer arg1) {
		return null;
	}

	public boolean isEnabled() {
		if(economy == null)
			return false;
		
		return economy.isInitialized();
	}

	@Deprecated
	public EconomyResponse withdrawPlayer(String arg0, double arg1) {
		return null;
	}

	public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
		FinancialEntity entity = slPlug.playerManager.getFinancialEntity(player.getUniqueId());
		EconResult result = economy.withdraw(entity, amount);
		
		return economy.convertEconResult(result);
	}

	@Deprecated
	public EconomyResponse withdrawPlayer(String playerName, String world, double amount) {
		return withdrawPlayer(playerName, amount);
	}

	public EconomyResponse withdrawPlayer(OfflinePlayer player, String world, double amount) {
		return withdrawPlayer(player, amount);
	}

	

}
