package id.nationcore.utils;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class VaultHook {
    
    private final JavaPlugin plugin;
    private Economy economy;
    
    public VaultHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager()
            .getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }
    
    public Economy getEconomy() {
        return economy;
    }
    
    public double getBalance(UUID playerUUID) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        return economy.getBalance(player);
    }
    
    public boolean has(UUID playerUUID, double amount) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        return economy.has(player, amount);
    }
    
    public boolean withdraw(UUID playerUUID, double amount) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }
    
    public boolean deposit(UUID playerUUID, double amount) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        return economy.depositPlayer(player, amount).transactionSuccess();
    }
    
    public String format(double amount) {
        return economy.format(amount);
    }
}
