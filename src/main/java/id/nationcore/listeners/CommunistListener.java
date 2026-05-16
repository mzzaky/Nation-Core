package id.nationcore.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;
import id.nationcore.models.CommunistGovernment;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

/**
 * Listener khusus untuk efek-efek pemerintahan Komunis yang tidak cocok di
 * listener existing. Saat ini menangani Market Event (bonus trade villager).
 *
 * Sensor Media replacement diatur di {@link ChatListener} karena sangat
 * spesifik chat event flow. Plague & Quarantine diatur di {@link CapitalListener}
 * karena terkait teritori.
 */
public class CommunistListener implements Listener {

    private final NationCore plugin;

    /** Throttle: tidak boleh lebih dari 1 bonus per pemain per detik. */
    private final Map<UUID, Long> lastTradeBonus = new HashMap<>();

    public CommunistListener(NationCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Market Event: setiap kali anggota nation Komunis ambil hasil dari
     * Merchant inventory (villager trade), beri bonus +$25.
     *
     * Triggered hanya saat result slot (slot 2) di-klik dengan ada item
     * (artinya trade akan terjadi). Throttle 1 detik untuk hindari abuse
     * via shift-click rapid.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMerchantTrade(InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.MERCHANT) return;
        if (event.getRawSlot() != 2) return; // result slot
        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null || nation.getType() != GovernmentType.COMMUNIST) return;
        CommunistGovernment cg = nation.getCommunistGovernment();
        if (cg == null || !cg.isMarketEventActive()) return;

        long now = System.currentTimeMillis();
        Long last = lastTradeBonus.get(player.getUniqueId());
        if (last != null && now - last < 1000) return;
        lastTradeBonus.put(player.getUniqueId(), now);

        double bonus = plugin.getConfig().getDouble("nation.communist.market-event-bonus", 25);
        plugin.getVaultHook().deposit(player.getUniqueId(), bonus);
        MessageUtils.send(player, "<gold>🛒 Market Event: <green>+$" +
                MessageUtils.formatNumber(bonus) + "</green> dari trade.");
    }
}
