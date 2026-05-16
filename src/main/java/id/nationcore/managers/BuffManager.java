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

    // Modifier keys
    private static final String PRESIDENT_DAMAGE_KEY = "nation_president_damage";
    private static final String PRESIDENT_DEFENSE_KEY = "nation_president_defense";
    private static final String PRESIDENT_HEALTH_KEY = "nation_president_health";
    private static final String CABINET_DAMAGE_KEY = "nation_cabinet_damage";
    private static final String CABINET_DEFENSE_KEY = "nation_cabinet_defense";
    private static final String CABINET_HEALTH_KEY = "nation_cabinet_health";

    // Track applied buffs
    private final Map<UUID, Boolean> presidentBuffsApplied = new HashMap<>();
    private final Map<UUID, Government.CabinetPosition> cabinetBuffsApplied = new HashMap<>();

    public BuffManager(NationCore plugin) {
        this.plugin = plugin;
    }

    public void applyPresidentBuffs(Player player) {
        if (player == null || !player.isOnline())
            return;

        UUID playerId = player.getUniqueId();

        // Remove any existing buffs first
        removePresidentBuffs(player);

        // Get buff values from config (adjusted for multiplier logic)
        double damageBonus = plugin.getConfig().getDouble("president.buffs.damage-multiplier", 1.15) - 1.0;
        double defenseBonus = plugin.getConfig().getDouble("president.buffs.defense-multiplier", 1.12) - 1.0;
        double extraHeartsBase = plugin.getConfig().getDouble("president.buffs.extra-hearts", 2.0);
        int extraHearts = (int) extraHeartsBase;

        // Apply damage buff (attack damage)
        AttributeInstance attackDamage = player.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackDamage != null) {
            AttributeModifier damageModifier = new AttributeModifier(
                    new NamespacedKey(plugin, PRESIDENT_DAMAGE_KEY),
                    damageBonus,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1);
            attackDamage.addModifier(damageModifier);
        }

        // Apply defense buff (armor toughness)
        AttributeInstance armorToughness = player.getAttribute(Attribute.ARMOR_TOUGHNESS);
        if (armorToughness != null) {
            AttributeModifier defenseModifier = new AttributeModifier(
                    new NamespacedKey(plugin, PRESIDENT_DEFENSE_KEY),
                    defenseBonus * 10, // Convert to armor toughness points
                    AttributeModifier.Operation.ADD_NUMBER);
            armorToughness.addModifier(defenseModifier);
        }

        // Apply extra health
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            AttributeModifier healthModifier = new AttributeModifier(
                    new NamespacedKey(plugin, PRESIDENT_HEALTH_KEY),
                    extraHearts * 2, // Hearts to health points
                    AttributeModifier.Operation.ADD_NUMBER);
            maxHealth.addModifier(healthModifier);
        }

        // Apply potion effects
        applyPresidentPotionEffects(player);

        presidentBuffsApplied.put(playerId, true);
    }

    public void removePresidentBuffs(Player player) {
        if (player == null)
            return;

        UUID playerId = player.getUniqueId();

        // Remove attribute modifiers
        removeModifier(player, Attribute.ATTACK_DAMAGE, PRESIDENT_DAMAGE_KEY);
        removeModifier(player, Attribute.ARMOR_TOUGHNESS, PRESIDENT_DEFENSE_KEY);
        removeModifier(player, Attribute.MAX_HEALTH, PRESIDENT_HEALTH_KEY);

        // Remove potion effects
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        player.removePotionEffect(PotionEffectType.SATURATION);

        presidentBuffsApplied.remove(playerId);

        // Ensure health doesn't exceed new max
        if (player.getHealth() > player.getAttribute(Attribute.MAX_HEALTH).getValue()) {
            player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        }
    }

    public void applyPresidentPotionEffects(Player player) {
        if (player == null || !player.isOnline())
            return;

        // Night vision (permanent while president)
        if (plugin.getConfig().getBoolean("president.buffs.night-vision", true)) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.NIGHT_VISION,
                    Integer.MAX_VALUE,
                    0,
                    false,
                    false,
                    true));
        }

        // Hunger immunity via saturation
        if (plugin.getConfig().getBoolean("president.buffs.hunger-immunity", true)) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SATURATION,
                    Integer.MAX_VALUE,
                    0,
                    false,
                    false,
                    false));
        }
    }

    public void applyCabinetBuffs(Player player, Government.CabinetPosition position) {
        if (player == null || !player.isOnline() || position == null)
            return;

        UUID playerId = player.getUniqueId();

        // Remove any existing cabinet buffs first
        removeCabinetBuffs(player);

        // Get position-specific buffs from config
        String configKey = position == Government.CabinetPosition.TREASURY ? "treasury-minister"
                : position.name().toLowerCase();
        String basePath = "cabinet." + configKey + ".";

        double damageBonus = plugin.getConfig().getDouble(basePath + "damage-multiplier", 1.0) - 1.0;
        double defenseBonus = plugin.getConfig().getDouble(basePath + "defense-multiplier", 1.0) - 1.0;
        double extraHeartsBase = plugin.getConfig().getDouble(basePath + "extra-hearts", 0.0);
        int extraHearts = (int) extraHeartsBase;

        // Apply damage buff if any
        if (damageBonus > 0) {
            AttributeInstance attackDamage = player.getAttribute(Attribute.ATTACK_DAMAGE);
            if (attackDamage != null) {
                AttributeModifier damageModifier = new AttributeModifier(
                        new NamespacedKey(plugin, CABINET_DAMAGE_KEY),
                        damageBonus,
                        AttributeModifier.Operation.MULTIPLY_SCALAR_1);
                attackDamage.addModifier(damageModifier);
            }
        }

        // Apply defense buff if any
        if (defenseBonus > 0) {
            AttributeInstance armorToughness = player.getAttribute(Attribute.ARMOR_TOUGHNESS);
            if (armorToughness != null) {
                AttributeModifier defenseModifier = new AttributeModifier(
                        new NamespacedKey(plugin, CABINET_DEFENSE_KEY),
                        defenseBonus * 10,
                        AttributeModifier.Operation.ADD_NUMBER);
                armorToughness.addModifier(defenseModifier);
            }
        }

        // Apply extra health if any
        if (extraHearts > 0) {
            AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                AttributeModifier healthModifier = new AttributeModifier(
                        new NamespacedKey(plugin, CABINET_HEALTH_KEY),
                        extraHearts * 2,
                        AttributeModifier.Operation.ADD_NUMBER);
                maxHealth.addModifier(healthModifier);
            }
        }

        // Apply position-specific potion effects
        applyCabinetPotionEffects(player, position);

        cabinetBuffsApplied.put(playerId, position);
    }

    public void removeCabinetBuffs(Player player) {
        if (player == null)
            return;

        UUID playerId = player.getUniqueId();
        Government.CabinetPosition position = cabinetBuffsApplied.get(playerId);

        // Remove attribute modifiers
        removeModifier(player, Attribute.ATTACK_DAMAGE, CABINET_DAMAGE_KEY);
        removeModifier(player, Attribute.ARMOR_TOUGHNESS, CABINET_DEFENSE_KEY);
        removeModifier(player, Attribute.MAX_HEALTH, CABINET_HEALTH_KEY);

        // Remove position-specific potion effects
        if (position != null) {
            removeCabinetPotionEffects(player, position);
        }

        cabinetBuffsApplied.remove(playerId);

        // Ensure health doesn't exceed new max
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null && player.getHealth() > maxHealth.getValue()) {
            player.setHealth(maxHealth.getValue());
        }
    }

    private void applyCabinetPotionEffects(Player player, Government.CabinetPosition position) {
        switch (position) {
            case DEFENSE -> {
                // Defense minister gets combat buffs
                player.addPotionEffect(
                        new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0, false, false, true));
                player.addPotionEffect(
                        new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, false, false, true));
            }
            case TREASURY -> {
                // Treasury minister gets luck for better drops
                player.addPotionEffect(
                        new PotionEffect(PotionEffectType.LUCK, Integer.MAX_VALUE, 1, false, false, true));
            }
            default -> {}
        }
    }

    private void removeCabinetPotionEffects(Player player, Government.CabinetPosition position) {
        switch (position) {
            case DEFENSE -> {
                player.removePotionEffect(PotionEffectType.STRENGTH);
                player.removePotionEffect(PotionEffectType.RESISTANCE);
            }
            case TREASURY -> {
                player.removePotionEffect(PotionEffectType.LUCK);
            }
            default -> {}
        }
    }

    private void removeModifier(Player player, Attribute attribute, String key) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null)
            return;

        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (modifier.getKey().equals(namespacedKey)) {
                instance.removeModifier(modifier);
            }
        }
    }

    public void refreshAllBuffs() {
        // Legacy global government — masih dipakai oleh pemain tanpa nation
        refreshGovernmentBuffs(plugin.getDataManager().getGovernment());

        // Per-nation: iterate semua nation REPUBLIC & COMMUNIST
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            if (nation.getType() == GovernmentType.REPUBLIC) {
                Government gov = nation.getRepublicGovernment();
                if (gov != null) refreshGovernmentBuffs(gov);
            } else if (nation.getType() == GovernmentType.COMMUNIST) {
                CommunistGovernment cg = nation.getCommunistGovernment();
                if (cg != null) refreshCommunistBuffs(cg);
            }
        }
    }

    /** Refresh buff Sekjen & Politbiro untuk satu CommunistGovernment instance. */
    private void refreshCommunistBuffs(CommunistGovernment cg) {
        if (cg == null) return;

        if (cg.hasSecretaryGeneral()) {
            Player sekjen = Bukkit.getPlayer(cg.getSecretaryGeneralUUID());
            if (sekjen != null && sekjen.isOnline()) {
                if (!presidentBuffsApplied.containsKey(sekjen.getUniqueId())) {
                    applyPresidentBuffs(sekjen);
                } else {
                    applyPresidentPotionEffects(sekjen);
                }
            }
        }

        for (PolitburoMember member : cg.getPolitburo().values()) {
            Player p = Bukkit.getPlayer(member.getUuid());
            if (p == null || !p.isOnline()) continue;
            Government.CabinetPosition mapped = mapPolitburoToCabinet(member.getPosition());
            if (!cabinetBuffsApplied.containsKey(p.getUniqueId())) {
                applyCabinetBuffs(p, mapped);
            } else {
                applyCabinetPotionEffects(p, mapped);
            }
        }
    }

    /**
     * Mapping PolitburoPosition (Komunis) → Government.CabinetPosition (Republik)
     * untuk reuse buff config existing. Mapping pakai cabinet slot terdekat
     * secara tema:
     *   Propaganda → Treasury       (manipulasi sosial, glowing/dolphin's grace)
     *   Defense    → Defense        (sama persis — combat buffs)
     *   Treasury   → Treasury       (sama persis — vault/luck)
     *   Health     → Defense (haste — medic kerja cepat, regen feel)
     */
    public static Government.CabinetPosition mapPolitburoToCabinet(PolitburoPosition pos) {
        return switch (pos) {
            case PROPAGANDA -> Government.CabinetPosition.TREASURY;
            case DEFENSE    -> Government.CabinetPosition.DEFENSE;
            case TREASURY   -> Government.CabinetPosition.TREASURY;
            case HEALTH     -> Government.CabinetPosition.DEFENSE;
        };
    }

    /** Refresh buff president & cabinet untuk satu Government instance. */
    private void refreshGovernmentBuffs(Government gov) {
        if (gov == null) return;

        if (gov.hasPresident()) {
            Player president = Bukkit.getPlayer(gov.getPresidentUUID());
            if (president != null && president.isOnline()) {
                if (!presidentBuffsApplied.containsKey(president.getUniqueId())) {
                    applyPresidentBuffs(president);
                } else {
                    applyPresidentPotionEffects(president);
                }
            }
        }

        for (Government.CabinetPosition position : Government.CabinetPosition.values()) {
            UUID memberId = gov.getCabinetMember(position);
            if (memberId == null) continue;
            Player member = Bukkit.getPlayer(memberId);
            if (member == null || !member.isOnline()) continue;
            if (!cabinetBuffsApplied.containsKey(member.getUniqueId())) {
                applyCabinetBuffs(member, position);
            } else {
                applyCabinetPotionEffects(member, position);
            }
        }
    }

    public void handlePlayerJoin(Player player) {
        UUID playerId = player.getUniqueId();

        // 1. Cek di nation pemain (jika ada). REPUBLIC: president→cabinet.
        // COMMUNIST: Sekjen→Politbiro.
        Nation nation = plugin.getNationManager().getNationOf(playerId);
        if (nation != null && nation.getType() == GovernmentType.REPUBLIC) {
            Government natGov = nation.getRepublicGovernment();
            if (natGov != null) {
                if (natGov.hasPresident() && natGov.getPresidentUUID().equals(playerId)) {
                    applyPresidentBuffs(player);
                    MessageUtils.send(player, "<gold>🎖 Buffs presiden " + nation.getName() + " aktif!");
                    return;
                }
                for (Government.CabinetPosition position : Government.CabinetPosition.values()) {
                    UUID memberId = natGov.getCabinetMember(position);
                    if (memberId != null && memberId.equals(playerId)) {
                        applyCabinetBuffs(player, position);
                        MessageUtils.send(player, "<gold>🎖 Buffs kabinet " + nation.getName() + " aktif!");
                        return;
                    }
                }
            }
        } else if (nation != null && nation.getType() == GovernmentType.COMMUNIST) {
            CommunistGovernment cg = nation.getCommunistGovernment();
            if (cg != null) {
                if (cg.hasSecretaryGeneral() && cg.getSecretaryGeneralUUID().equals(playerId)) {
                    applyPresidentBuffs(player);
                    MessageUtils.send(player, "<gold>🎖 Buffs Sekretaris Jenderal " + nation.getName() + " aktif!");
                    return;
                }
                PolitburoPosition pos = cg.getPositionByUUID(playerId);
                if (pos != null) {
                    applyCabinetBuffs(player, mapPolitburoToCabinet(pos));
                    MessageUtils.send(player, "<gold>🎖 Buffs Politbiro " + nation.getName() + " aktif!");
                    return;
                }
            }
        }

        // 2. Fallback ke legacy global Government untuk pemain tanpa nation.
        Government gov = plugin.getDataManager().getGovernment();
        if (gov.hasPresident() && gov.getPresidentUUID().equals(playerId)) {
            applyPresidentBuffs(player);
            MessageUtils.send(player, "<gold>🎖 Presidential buffs applied!");
            return;
        }
        for (Government.CabinetPosition position : Government.CabinetPosition.values()) {
            UUID memberId = gov.getCabinetMember(position);
            if (memberId != null && memberId.equals(playerId)) {
                applyCabinetBuffs(player, position);
                MessageUtils.send(player, "<gold>🎖 Cabinet buffs applied!");
                return;
            }
        }
    }

    public void handlePlayerQuit(Player player) {
        UUID playerId = player.getUniqueId();

        // Clear tracking (buffs will be re-applied on join)
        presidentBuffsApplied.remove(playerId);
        cabinetBuffsApplied.remove(playerId);
    }

    public void removeAllBuffs(Player player) {
        removePresidentBuffs(player);
        removeCabinetBuffs(player);
    }

    public boolean hasPresidentBuffs(UUID playerId) {
        return presidentBuffsApplied.containsKey(playerId);
    }

    public boolean hasCabinetBuffs(UUID playerId) {
        return cabinetBuffsApplied.containsKey(playerId);
    }

    public Government.CabinetPosition getCabinetPosition(UUID playerId) {
        return cabinetBuffsApplied.get(playerId);
    }

    public String getPresidentBuffDescription() {
        double damageBonus = (plugin.getConfig().getDouble("president.buffs.damage-multiplier", 1.15) - 1.0) * 100;
        double defenseBonus = (plugin.getConfig().getDouble("president.buffs.defense-multiplier", 1.12) - 1.0) * 100;
        double extraHeartsBase = plugin.getConfig().getDouble("president.buffs.extra-hearts", 2.0);
        int extraHearts = (int) extraHeartsBase;
        double vaultBonus = (plugin.getConfig().getDouble("president.buffs.vault-multiplier", 1.20) - 1.0) * 100;
        double xpBonus = (plugin.getConfig().getDouble("president.buffs.xp-multiplier", 1.10) - 1.0) * 100;

        return String.format(
                "+" + "%.0f%% Damage, +%.0f%% Defense, +%d Hearts, +%.0f%% Vault, +%.0f%% XP, Night Vision, Hunger Immunity",
                damageBonus, defenseBonus, extraHearts, vaultBonus, xpBonus);
    }

    public String getCabinetBuffDescription(Government.CabinetPosition position) {
        return switch (position) {
            case DEFENSE -> "+10% Damage, +8% Defense, Strength I, Resistance I";
            case TREASURY -> "+15% Vault Bonus, Luck II";
            default -> "No buffs";
        };
    }
}
