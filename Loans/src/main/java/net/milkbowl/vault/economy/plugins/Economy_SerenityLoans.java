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

import com.nwmogk.bukkit.loans.Conf;
import com.nwmogk.bukkit.loans.EconomyManager;
import com.nwmogk.bukkit.loans.SerenityLoans;
import com.nwmogk.bukkit.loans.api.EconResult;
import com.nwmogk.bukkit.loans.api.FinancialEntity;
import com.nwmogk.bukkit.loans.object.FinancialInstitution;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;

public class Economy_SerenityLoans implements Economy {
	
	// TODO Do something with name lookup calls
	//      Implement bank memberships
	//      Add response messages to failed EconomyResponse objects
	
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

	public EconomyResponse bankBalance(String bankName) {
		FinancialInstitution entity = slPlug.playerManager.getFinancialInstitution(bankName);
		EconResult result = economy.getBalance(entity);
		
		return economy.convertEconResult(result);
	}

	public EconomyResponse bankDeposit(String bankName, double amount) {
		FinancialInstitution entity = slPlug.playerManager.getFinancialInstitution(bankName);
		EconResult result = economy.deposit(entity, amount);
		
		return economy.convertEconResult(result);
	}

	public EconomyResponse bankHas(String bankName, double amount) {
		FinancialInstitution entity = slPlug.playerManager.getFinancialInstitution(bankName);
		EconResult result = economy.has(entity, amount);
		
		return economy.convertEconResult(result);
	}

	public EconomyResponse bankWithdraw(String bankName, double amount) {
		FinancialInstitution entity = slPlug.playerManager.getFinancialInstitution(bankName);
		EconResult result = economy.withdraw(entity, amount);
		
		return economy.convertEconResult(result);
	}

	@Deprecated
	public EconomyResponse createBank(String bankName, String playerName) {
		return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, null);
	}

	public EconomyResponse createBank(String bankName, OfflinePlayer player) {
		return economy.convertEconResult(economy.createBank(bankName, player));
	}

	@Deprecated
	public boolean createPlayerAccount(String arg0) {
		return false;
	}

	public boolean createPlayerAccount(OfflinePlayer player) {
		return economy.createPlayerAccount(player);
	}

	@Deprecated
	public boolean createPlayerAccount(String playerName, String world) {
		return createPlayerAccount(playerName);
	}

	public boolean createPlayerAccount(OfflinePlayer player, String world) {
		return createPlayerAccount(player);
	}

	public String currencyNamePlural() {
		return economy.currencyNamePlural();
	}

	public String currencyNameSingular() {
		return economy.currencyNameSingular();
	}

	public EconomyResponse deleteBank(String arg0) {
		return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, null);
	}

	@Deprecated
	public EconomyResponse depositPlayer(String playerName, double amount) {
		return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, null);
	}

	public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
		FinancialEntity entity = slPlug.playerManager.getFinancialEntity(player.getUniqueId());
		EconResult result = economy.deposit(entity, amount);
		
		return economy.convertEconResult(result);
	}

	@Deprecated
	public EconomyResponse depositPlayer(String playerName, String world, double amount) {
		return depositPlayer(playerName, amount);
	}

	public EconomyResponse depositPlayer(OfflinePlayer player, String world, double amount) {
		return depositPlayer(player, amount);
	}

	public String format(double amount) {
		return economy.format(amount);
	}

	public int fractionalDigits() {
		return Conf.getFractionalDigits();
	}

	@Deprecated
	public double getBalance(String playerName) {
		return 0;
	}

	public double getBalance(OfflinePlayer player) {
		FinancialEntity entity = slPlug.playerManager.getFinancialEntity(player.getUniqueId());
		
		return economy.getBalance(entity).balance;
	}

	@Deprecated
	public double getBalance(String playerName, String world) {
		return getBalance(playerName);
	}

	public double getBalance(OfflinePlayer player, String world) {
		return getBalance(player);
	}

	public List<String> getBanks() {
		return economy.getBanks();
	}

	public String getName() {
		return name;
	}

	@Deprecated
	public boolean has(String playerName, double amount) {
		return false;
	}

	public boolean has(OfflinePlayer player, double amount) {
		FinancialEntity entity = slPlug.playerManager.getFinancialEntity(player.getUniqueId());
		
		return economy.has(entity, amount).callSuccess;
	}

	@Deprecated
	public boolean has(String playerName, String world, double amount) {
		return has(playerName, amount);
	}

	public boolean has(OfflinePlayer player, String world, double amount) {
		return has(player, amount);
	}

	@Deprecated
	public boolean hasAccount(String playerName) {
		return false;
	}

	public boolean hasAccount(OfflinePlayer player) {
		return economy.hasAccount(player);
	}

	@Deprecated
	public boolean hasAccount(String playerName, String world) {
		return hasAccount(playerName);
	}

	public boolean hasAccount(OfflinePlayer player, String world) {
		return hasAccount(player);
	}

	public boolean hasBankSupport() {
		return economy.hasBankSupport();
	}

	@Deprecated
	public EconomyResponse isBankMember(String bankName, String playerName) {
		return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, null);
	}

	public EconomyResponse isBankMember(String bankName, OfflinePlayer player) {
		return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, null);
	}

	@Deprecated
	public EconomyResponse isBankOwner(String bankName, String playerName) {
		return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, null);
	}

	public EconomyResponse isBankOwner(String bankName, OfflinePlayer player) {
		return economy.convertEconResult(economy.isBankOwner(bankName, player));
	}

	public boolean isEnabled() {
		if(economy == null)
			return false;
		
		return economy.isInitialized();
	}

	@Deprecated
	public EconomyResponse withdrawPlayer(String arg0, double arg1) {
		return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, null);
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
