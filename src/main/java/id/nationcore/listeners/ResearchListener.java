package id.nationcore.listeners;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import id.nationcore.NationCore;
import id.nationcore.managers.ResearchManager;
import id.nationcore.models.Nation;
import id.nationcore.models.ResearchType;

/**
 * Wires research effects into gameplay events.
 *
 * Effect mapping:
 *   • AGRICULTURE_PROGRAM_I  — BlockBreakEvent on crops:    chance for +1–2 drops
 *   • HUSBANDRY_BOOST_I      — EntityDeathEvent on animals: chance for +1–2 drops
 *   • MINING_LUCK_I          — BlockBreakEvent on mineables: chance for +1–2 drops
 *   • MAGIC_EDUCATION_I      — PlayerExpChangeEvent:        +X% XP gained
 *   • HEALTH_EXPANSION_I     — PlayerJoinEvent / Respawn:    apply MAX_HEALTH attribute
 *   • HUNTING_SKILL_I        — EntityDamageByEntityEvent vs hostile: +X% damage
 *   • ATTACK_EXERCISE_I      — EntityDamageByEntityEvent player→player: +X% damage
 *   • DEFENSIVE_TACTIC_I     — EntityDamageByEntityEvent player→player: -X% damage
 *   • ARCHERY_TRAINING_I     — EntityDamageByEntityEvent projectile→player|mob: +X%
 *
 * Damage modifiers run at EventPriority.HIGH so we layer cleanly on top of
 * {@link DamageListener} (also HIGH) — both adjust the same {@code damage}
 * field; order between them doesn't change the multiplicative outcome.
 */
public class ResearchListener implements Listener {

    private final NationCore plugin;

    public ResearchListener(NationCore plugin) {
        this.plugin = plugin;
    }

    private ResearchManager research() {
        return plugin.getResearchManager();
    }

    private Nation nationOf(Player p) {
        return plugin.getNationManager().getNationOf(p.getUniqueId());
    }

    // =================================================================
    //  Health Expansion — apply MAX_HEALTH on join/respawn
    // =================================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Nation nation = nationOf(player);
        research().applyHealthAttribute(player, nation);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        // Run a tick later so vanilla respawn logic finishes first.
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Nation nation = nationOf(player);
            research().applyHealthAttribute(player, nation);
        }, 5L);
    }

    // =================================================================
    //  Magic Education — XP multiplier
    // =================================================================

    @EventHandler(priority = EventPriority.NORMAL)
    public void onExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        Nation nation = nationOf(player);
        if (nation == null) return;
        double bonus = research().getCumulativeEffect(nation, ResearchType.MAGIC_EDUCATION_I);
        if (bonus <= 0) return;
        int original = event.getAmount();
        if (original <= 0) return;
        int boosted = (int) Math.round(original * (1.0 + bonus));
        if (boosted == original) {
            // Avoid losing fractional XP for low-amount events.
            if (ThreadLocalRandom.current().nextDouble() < bonus) {
                boosted = original + 1;
            }
        }
        event.setAmount(boosted);
    }

    // =================================================================
    //  Bonus drops on crops & ores (Agriculture, Mining)
    // =================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        if (event.getExpToDrop() < 0 && event.getBlock() == null) return;

        Nation nation = nationOf(player);
        if (nation == null) return;

        Block block = event.getBlock();
        Material mat = block.getType();
        ResearchType applicable = pickFarmOrMineResearch(mat, block);
        if (applicable == null) return;

        double chance = research().getCumulativeEffect(nation, applicable);
        if (chance <= 0) return;
        if (ThreadLocalRandom.current().nextDouble() >= chance) return;

        int extra = ThreadLocalRandom.current().nextInt(1, 3); // 1 or 2
        // Use the natural drops of this block as templates so enchant effects
        // (e.g. Fortune) and tool requirements remain authoritative.
        var natural = block.getDrops(player.getInventory().getItemInMainHand());
        if (natural.isEmpty()) return;
        for (ItemStack drop : natural) {
            if (drop == null || drop.getType() == Material.AIR) continue;
            ItemStack bonus = drop.clone();
            bonus.setAmount(extra);
            block.getWorld().dropItemNaturally(block.getLocation(), bonus);
        }
    }

    private ResearchType pickFarmOrMineResearch(Material material, Block block) {
        if (isFarmBlock(material, block)) return ResearchType.AGRICULTURE_PROGRAM_I;
        if (isMiningBlock(material)) return ResearchType.MINING_LUCK_I;
        return null;
    }

    private boolean isFarmBlock(Material material, Block block) {
        // Mature crops: Wheat, Carrot, Potato, Beetroot — any block in Tag.CROPS
        if (Tag.CROPS.isTagged(material)) {
            // Honor maturity if Ageable to avoid free-bonus on freshly-planted seedlings.
            if (block.getBlockData() instanceof org.bukkit.block.data.Ageable ageable) {
                return ageable.getAge() >= ageable.getMaximumAge();
            }
            return true;
        }
        return switch (material) {
            case PUMPKIN, MELON, SUGAR_CANE, BAMBOO, CACTUS, KELP, KELP_PLANT,
                 SWEET_BERRY_BUSH, GLOW_BERRIES, NETHER_WART, COCOA -> true;
            default -> false;
        };
    }

    private boolean isMiningBlock(Material material) {
        if (Tag.COAL_ORES.isTagged(material)) return true;
        if (Tag.IRON_ORES.isTagged(material)) return true;
        if (Tag.GOLD_ORES.isTagged(material)) return true;
        if (Tag.DIAMOND_ORES.isTagged(material)) return true;
        if (Tag.EMERALD_ORES.isTagged(material)) return true;
        if (Tag.LAPIS_ORES.isTagged(material)) return true;
        if (Tag.REDSTONE_ORES.isTagged(material)) return true;
        if (Tag.COPPER_ORES.isTagged(material)) return true;
        return switch (material) {
            case ANCIENT_DEBRIS, NETHER_QUARTZ_ORE, NETHER_GOLD_ORE,
                 RAW_IRON_BLOCK, RAW_GOLD_BLOCK, RAW_COPPER_BLOCK -> true;
            default -> false;
        };
    }

    // =================================================================
    //  Husbandry Boost — bonus drops from livestock kills
    // =================================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead instanceof Animals)) return;
        Player killer = dead.getKiller();
        if (killer == null) return;
        Nation nation = nationOf(killer);
        if (nation == null) return;
        double chance = research().getCumulativeEffect(nation, ResearchType.HUSBANDRY_BOOST_I);
        if (chance <= 0) return;
        if (ThreadLocalRandom.current().nextDouble() >= chance) return;
        int extra = ThreadLocalRandom.current().nextInt(1, 3);
        for (ItemStack drop : event.getDrops()) {
            if (drop == null || drop.getType() == Material.AIR) continue;
            dead.getWorld().dropItemNaturally(dead.getLocation(),
                    new ItemStack(drop.getType(), extra));
        }
    }

    // =================================================================
    //  Combat & projectile damage modifiers
    // =================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;

        // Resolve the human source (direct player or projectile shooter).
        Player attacker = resolvePlayerSource(event.getDamager());
        boolean isProjectile = event.getDamager() instanceof Projectile
                || event.getDamager() instanceof AbstractArrow;
        Nation attackerNation = attacker != null ? nationOf(attacker) : null;

        double damage = event.getDamage();
        boolean modified = false;

        if (event.getEntity() instanceof Player victim) {
            // Skip same-nation friendly fire bonuses: Attack/Defensive/Archery only
            // apply to enemy-vs-enemy combat. Same-nation members get no bonus.
            Nation victimNation = nationOf(victim);
            boolean sameNation = attackerNation != null && victimNation != null
                    && attackerNation.getId() != null
                    && attackerNation.getId().equals(victimNation.getId());

            if (attackerNation != null && !sameNation) {
                double atkBonus = research().getCumulativeEffect(attackerNation,
                        ResearchType.ATTACK_EXERCISE_I);
                if (atkBonus > 0) {
                    damage *= 1.0 + atkBonus;
                    modified = true;
                }
                if (isProjectile) {
                    double archery = research().getCumulativeEffect(attackerNation,
                            ResearchType.ARCHERY_TRAINING_I);
                    if (archery > 0) {
                        damage *= 1.0 + archery;
                        modified = true;
                    }
                }
            }

            if (victimNation != null && !sameNation) {
                double defReduction = research().getCumulativeEffect(victimNation,
                        ResearchType.DEFENSIVE_TACTIC_I);
                if (defReduction > 0) {
                    // Cap reduction at 90% to avoid invulnerability via stacked tiers.
                    double capped = Math.min(0.9, defReduction);
                    damage *= 1.0 - capped;
                    modified = true;
                }
            }
        } else if (event.getEntity() instanceof Mob mob) {
            // Damage to mobs — apply Hunting Skill (hostile) and Archery (any mob).
            if (attackerNation != null) {
                if (mob instanceof Monster) {
                    double huntBonus = research().getCumulativeEffect(attackerNation,
                            ResearchType.HUNTING_SKILL_I);
                    if (huntBonus > 0) {
                        damage *= 1.0 + huntBonus;
                        modified = true;
                    }
                }
                if (isProjectile) {
                    double archery = research().getCumulativeEffect(attackerNation,
                            ResearchType.ARCHERY_TRAINING_I);
                    if (archery > 0) {
                        damage *= 1.0 + archery;
                        modified = true;
                    }
                }
            }
        }

        if (modified) {
            event.setDamage(damage);
        }
    }

    private Player resolvePlayerSource(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj) {
            ProjectileSource src = proj.getShooter();
            if (src instanceof Player p) return p;
        }
        return null;
    }
}
