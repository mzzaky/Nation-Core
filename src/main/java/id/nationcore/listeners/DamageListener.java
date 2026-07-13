package id.nationcore.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;

import id.nationcore.NationCore;
import id.nationcore.models.CommunistGovernment;
import id.nationcore.models.ExecutiveOrder;
import id.nationcore.models.Government;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.Nation;

public class DamageListener implements Listener {

    private final NationCore plugin;

    public DamageListener(NationCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Check for PvP
        if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof Player victim) {



            Nation nation = plugin.getNationManager().getNationOf(attacker.getUniqueId());
            if (nation != null) {
                // Modify damage for War Economy
                if (plugin.getExecutiveOrderManager().isOrderActive(nation, ExecutiveOrder.ExecutiveOrderType.WAR_ECONOMY)) {
                    // +50% PvP damage
                    event.setDamage(event.getDamage() * 1.5);
                }

                // Modify damage for Purge Protocol
                if (plugin.getExecutiveOrderManager().isOrderActive(nation, ExecutiveOrder.ExecutiveOrderType.PURGE_PROTOCOL)) {
                    // Full damage, no protection
                    event.setDamage(event.getDamage() * 1.25);
                }
            }

            // Leader damage bonus (additional on top of attribute modifiers)
            if (nation != null && nation.getLeaderUUID().equals(attacker.getUniqueId())) {
                // Leader bonus damage logic can be added here
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;

        // Check if in arena - arena has its own damage handling
        if (plugin.getArenaManager().isInArena(player.getUniqueId())) {
            return;
        }

        // Vaccination Drive (Komunis & Republic): cancel poison & wither damage untuk anggota
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation != null) {
            if (nation.getType() == GovernmentType.COMMUNIST) {
                CommunistGovernment cg = nation.getCommunistGovernment();
                if (cg != null && cg.isVaccinationActive()) {
                    EntityDamageEvent.DamageCause cause = event.getCause();
                    if (cause == EntityDamageEvent.DamageCause.POISON
                            || cause == EntityDamageEvent.DamageCause.WITHER) {
                        event.setCancelled(true);
                        return;
                    }
                    // Bersihkan efek poison/wither aktif (preventive)
                    if (player.hasPotionEffect(PotionEffectType.POISON)) {
                        player.removePotionEffect(PotionEffectType.POISON);
                    }
                    if (player.hasPotionEffect(PotionEffectType.WITHER)) {
                        player.removePotionEffect(PotionEffectType.WITHER);
                    }
                }
            } else if (nation.getType() == GovernmentType.REPUBLIC) {
                if (plugin.getCabinetManager().isDecisionActive(nation, id.nationcore.models.CabinetDecision.DecisionType.VACCINATION_DRIVE)) {
                    EntityDamageEvent.DamageCause cause = event.getCause();
                    if (cause == EntityDamageEvent.DamageCause.POISON
                            || cause == EntityDamageEvent.DamageCause.WITHER) {
                        event.setCancelled(true);
                        return;
                    }
                    if (player.hasPotionEffect(PotionEffectType.POISON)) {
                        player.removePotionEffect(PotionEffectType.POISON);
                    }
                    if (player.hasPotionEffect(PotionEffectType.WITHER)) {
                        player.removePotionEffect(PotionEffectType.WITHER);
                    }
                }
            }
        }




    }
}
