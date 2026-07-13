package id.nationcore.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.configuration.file.YamlConfiguration;

import id.nationcore.NationCore;
import id.nationcore.models.CommunistGovernment;
import id.nationcore.models.CommunistGovernment.PolitburoMember;
import id.nationcore.models.CommunistGovernment.PolitburoPosition;
import id.nationcore.models.Government;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

public class BuffManager {

    private final NationCore plugin;



    public BuffManager(NationCore plugin) {
        this.plugin = plugin;
    }



    public void applyCabinetBuffs(Player player, Government.CabinetPosition position) {}
    public void removeCabinetBuffs(Player player) {}
    public void applyPolitburoBuffs(Player player, PolitburoPosition position) {}



    public void refreshAllBuffs() {}

    public void handlePlayerJoin(Player player) {}
    public void handlePlayerQuit(Player player) {}
    public void removeAllBuffs(Player player) {}
    public boolean hasCabinetBuffs(UUID playerId) { return false; }
    public boolean hasPolitburoBuffs(UUID playerId) { return false; }
    public PolitburoPosition getPolitburoPosition(UUID playerId) { return null; }
    public Government.CabinetPosition getCabinetPosition(UUID playerId) { return null; }
    public String getCabinetBuffDescription(Government.CabinetPosition position) { return "No buffs"; }
}
