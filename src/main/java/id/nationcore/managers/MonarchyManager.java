package id.nationcore.managers;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import id.nationcore.NationCore;
import id.nationcore.models.CommunistDecisionType;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.MonarchyDecisionType;
import id.nationcore.models.MonarchyGovernment;
import id.nationcore.models.MonarchyGovernment.HighCouncilMember;
import id.nationcore.models.MonarchyGovernment.HighCouncilPosition;
import id.nationcore.models.Nation;
import id.nationcore.models.Treasury.TransactionType;
import id.nationcore.utils.MessageUtils;

/**
 * Specific operations for MONARCHY type nations.
 *
 * The monarchy is intentionally thin: it reuses Communist effect handlers
 * for the 20 royal decisions (the spec keeps the catalogue identical) while
 * enforcing its own rules:
 *
 *   • Absolute power — the King may execute any High Council decision and
 *     may issue executive orders without cooldown.
 *   • Lifelong rule — there is no election cycle, no recall mechanism, and
 *     no scheduled term reset; the King reigns until they abdicate or are
 *     forcibly replaced by an admin.
 *
 * The treasury, tax phase, and royal alms reuse the same scheduler hooks as
 * the Communist regime. The "Communist tax phase" in the config doubles as
 * the "royal tax phase" so server operators do not need to touch the YAML.
 */
public class MonarchyManager {

    public static class Result {
        private final boolean success;
        private final String message;

        private Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        public static Result ok(String msg) { return new Result(true, msg); }
        public static Result fail(String msg) { return new Result(false, msg); }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    private final NationCore plugin;

    public MonarchyManager(NationCore plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------
    // Resolver helpers
    // ---------------------------------------------------------------

    public MonarchyGovernment getGovernment(Nation nation) {
        if (nation == null || nation.getType() != GovernmentType.MONARCHY) return null;
        return nation.getMonarchyGovernment();
    }

    public boolean isKing(Nation nation, UUID uuid) {
        MonarchyGovernment mg = getGovernment(nation);
        return mg != null && mg.hasKing() && mg.getKingUUID().equals(uuid);
    }

    public boolean isCouncilMember(Nation nation, UUID uuid) {
        MonarchyGovernment mg = getGovernment(nation);
        return mg != null && mg.getCouncilMemberByUUID(uuid) != null;
    }

    public HighCouncilPosition getCouncilPosition(Nation nation, UUID uuid) {
        MonarchyGovernment mg = getGovernment(nation);
        return mg != null ? mg.getPositionByUUID(uuid) : null;
    }

    // ---------------------------------------------------------------
    // High Council appointments (King-only)
    // ---------------------------------------------------------------

    public Result appointCouncil(Nation nation, HighCouncilPosition position,
                                 UUID targetUUID, String targetName) {
        MonarchyGovernment mg = getGovernment(nation);
        if (mg == null) return Result.fail("This nation is not a Monarchy.");
        if (!mg.hasKing()) return Result.fail("No King currently rules this kingdom.");
        if (!nation.isMember(targetUUID)) {
            return Result.fail("Target is not a member of this kingdom.");
        }

        // Royal Soldiers tracking (subset of nation members) — appointed
        // council members are automatically promoted to Royal Soldier rank.
        if (!mg.isRoyalSoldier(targetUUID)) {
            mg.addRoyalSoldier(targetUUID);
        }

        HighCouncilPosition currentPos = mg.getPositionByUUID(targetUUID);
        if (currentPos != null) mg.removeCouncil(currentPos);

        HighCouncilMember existing = mg.getCouncilMember(position);
        if (existing != null) {
            Player existingPlayer = Bukkit.getPlayer(existing.getUuid());
            if (existingPlayer != null) {
                plugin.getBuffManager().removeCabinetBuffs(existingPlayer);
                MessageUtils.send(existingPlayer, "<gray>You have been dismissed from the High Council of " +
                        nation.getName() + ".</gray>");
            }
        }

        mg.appointCouncil(position, new HighCouncilMember(targetUUID, targetName, position));

        Player target = Bukkit.getPlayer(targetUUID);
        if (target != null) {
            MessageUtils.send(target, "<gold>You have been appointed as " +
                    position.getDisplayName() + " of the High Council of " + nation.getName() + "!</gold>");
        }

        broadcastToNation(nation, "<yellow><gold>" + targetName + "</gold> has been appointed as " +
                position.getDisplayName() + " of the High Council of " + nation.getName() + ".</yellow>");

        plugin.getDataManager().saveNations();
        return Result.ok("Appointed " + targetName + " as " + position.getDisplayName() + ".");
    }

    public Result removeCouncil(Nation nation, HighCouncilPosition position) {
        MonarchyGovernment mg = getGovernment(nation);
        if (mg == null) return Result.fail("This nation is not a Monarchy.");

        HighCouncilMember member = mg.getCouncilMember(position);
        if (member == null) return Result.fail("Position " + position.getDisplayName() + " is empty.");

        Player target = Bukkit.getPlayer(member.getUuid());
        if (target != null) {
            plugin.getBuffManager().removeCabinetBuffs(target);
            MessageUtils.send(target, "<red>You have been removed from the High Council of " + nation.getName() + ".</red>");
        }

        mg.removeCouncil(position);

        broadcastToNation(nation, "<yellow><gold>" + member.getName() +
                "</gold> was removed from the High Council of " + nation.getName() + ".</yellow>");

        plugin.getDataManager().saveNations();
        return Result.ok("Position " + position.getDisplayName() + " is now vacant.");
    }

    // ---------------------------------------------------------------
    // King lifecycle (admin-only — there is no election in a Monarchy)
    // ---------------------------------------------------------------

    public void setKing(Nation nation, UUID uuid, String name) {
        MonarchyGovernment mg = getGovernment(nation);
        if (mg == null) return;

        if (mg.hasKing()) {
            Player ex = Bukkit.getPlayer(mg.getKingUUID());
            if (ex != null) plugin.getBuffManager().removePresidentBuffs(ex);
            for (HighCouncilMember m : mg.getHighCouncil().values()) {
                Player p = Bukkit.getPlayer(m.getUuid());
                if (p != null) plugin.getBuffManager().removeCabinetBuffs(p);
            }
        }

        mg.setKingUUID(uuid);
        mg.setKingName(name);
        nation.setLeaderUUID(uuid);
        nation.setLeaderName(name);
        mg.setCoronationTime(System.currentTimeMillis());
        mg.setLastKingActivity(System.currentTimeMillis());
        mg.getHighCouncil().clear();
        mg.addRoyalSoldier(uuid);

        broadcastToNation(nation, "<gold>" + name + " has ascended the throne of " +
                nation.getName() + " — Long live the King!</gold>");

        Player king = Bukkit.getPlayer(uuid);
        if (king != null) plugin.getBuffManager().applyPresidentBuffs(king);

        plugin.getDataManager().saveNations();
    }

    // ---------------------------------------------------------------
    // 20 High Council Decisions (delegates to Communist effect handlers)
    // ---------------------------------------------------------------

    /**
     * Cooldown lookup for council members. The King is exempt — they may
     * execute every decision freely under absolute power.
     */
    /** Decision cost from order.yaml (fallback to the enum default). */
    public int getDecisionCost(MonarchyDecisionType type) {
        return (int) plugin.getExecutiveOrderManager().getOrderCost(type.name().toLowerCase(), type.getCost());
    }

    /** Decision cooldown (ms) from order.yaml days, with a per-position hour fallback. */
    private long getDecisionCooldownMs(MonarchyDecisionType type) {
        YamlConfiguration nationConfig = plugin.getNationConfig(GovernmentType.MONARCHY);
        long fallbackHours = nationConfig != null ? nationConfig.getLong("cabinet.decision-cooldown-hours", 48) : 48;
        long days = plugin.getExecutiveOrderManager().getOrderCooldownDays(type.name().toLowerCase(), -1);
        if (days >= 0) return days * 24L * 3600000L;
        return fallbackHours * 3600000L;
    }

    private boolean isDecisionEnabled(MonarchyDecisionType type) {
        return plugin.getExecutiveOrderManager().isOrderEnabled(type.name().toLowerCase());
    }

    public long getDecisionCooldownRemaining(Nation nation, UUID uuid, MonarchyDecisionType type) {
        MonarchyGovernment mg = getGovernment(nation);
        if (mg == null) return 0;
        if (mg.hasKing() && mg.getKingUUID().equals(uuid)) return 0;
        long cooldownMs = getDecisionCooldownMs(type);
        Map<String, Long> playerCooldowns = mg.getDecisionCooldowns().get(uuid);
        if (playerCooldowns == null) return 0;
        Long lastUse = playerCooldowns.get(type.name());
        if (lastUse == null) return 0;
        return Math.max(0, cooldownMs - (System.currentTimeMillis() - lastUse));
    }

    /**
     * Execute a royal decision. The King may execute any decision regardless
     * of position; council members are limited to their own seat. The
     * Communist effect handler is reused after the cooldown/treasury checks
     * pass so the gameplay outcome is identical to the equivalent
     * Politburo decision.
     */
    public Result executeDecision(Nation nation, Player player, MonarchyDecisionType type) {
        MonarchyGovernment mg = getGovernment(nation);
        if (mg == null) return Result.fail("This nation is not a Monarchy.");

        boolean isKing = isKing(nation, player.getUniqueId());
        boolean isAdmin = player.hasPermission("nation.admin");

        if (!isKing && !isAdmin) {
            HighCouncilPosition required = type.getPosition();
            HighCouncilMember holder = mg.getCouncilMember(required);
            if (holder == null || !holder.getUuid().equals(player.getUniqueId())) {
                return Result.fail("Only the " + required.getDisplayName() +
                        " (or the King) may issue " + type.getDisplayName() + ".");
            }
        }

        if (!isDecisionEnabled(type)) {
            return Result.fail(type.getDisplayName() + " is currently disabled.");
        }

        long remaining = getDecisionCooldownRemaining(nation, player.getUniqueId(), type);
        if (remaining > 0) {
            return Result.fail("Decision on cooldown — remaining " + MessageUtils.formatTime(remaining));
        }

        int decisionCost = getDecisionCost(type);
        if (!plugin.getTreasuryManager().canAfford(nation, decisionCost)) {
            return Result.fail("Royal Treasury of " + nation.getName() + " cannot afford this. Needs $" +
                    MessageUtils.formatNumber(decisionCost) + ".");
        }

        plugin.getTreasuryManager().withdraw(nation, TransactionType.EXECUTIVE_ORDER,
                decisionCost, "Royal decision: " + type.getDisplayName(), player.getUniqueId());

        // The King is exempt from cooldown tracking
        if (!isKing) {
            mg.getDecisionCooldowns().computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                    .put(type.name(), System.currentTimeMillis());
        }

        applyDecisionEffect(nation, mg, type);

        plugin.getDataManager().saveNations();
        return Result.ok("Royal decision '" + type.getDisplayName() + "' issued.");
    }

    /**
     * Apply the gameplay effect of a royal decision. Reuses the existing
     * Communist effect mechanics by mirroring their state changes onto the
     * MonarchyGovernment fields, so the buffs/timers shown in the GUI stay
     * accurate.
     */
    private void applyDecisionEffect(Nation nation, MonarchyGovernment mg, MonarchyDecisionType type) {
        long now = System.currentTimeMillis();
        long dur = type.getDurationMillis();

        switch (type) {
            // ─ HERALD ─────────────────────────────────────────────
            case HER_ROYAL_PROCLAMATION -> {
                Bukkit.broadcastMessage("§e§l[ROYAL PROCLAMATION — " + nation.getName() +
                        "§e§l] §6The Crown speaks. Long live the King!");
            }
            case HER_ROYAL_DECREE -> {
                broadcastToNation(nation, "<gold>📜 <yellow>ROYAL DECREE OF " + nation.getName() +
                        "</yellow></gold>");
                broadcastToNation(nation,
                        "<gray>By order of the Crown: <yellow>Loyalty and service to the King!</yellow>");
            }
            case HER_GLORIFY_KING -> {
                mg.setGlorificationUntil(now + dur);
                int ticks = (int) (dur / 50);
                int applied = 0;
                for (UUID s : mg.getRoyalSoldiers()) {
                    Player p = Bukkit.getPlayer(s);
                    if (p == null || !p.isOnline()) continue;
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.STRENGTH, ticks, 0, false, true, true));
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.RESISTANCE, ticks, 0, false, true, true));
                    applied++;
                }
                broadcastToNation(nation, "<gold>👑 Glorify the King!</gold> <gray>" +
                        applied + " Royal Soldiers receive Strength I + Resistance I.</gray>");
            }
            case HER_CENSOR_PRESS -> {
                mg.setSensorMediaUntil(now + dur);
                mg.setCensorshipReplacement("§6[CENSORED BY ROYAL HERALD]");
                mg.getCensorshipUsedOn().clear();
                broadcastToNation(nation, "<gold>📜 Royal censorship in effect!</gold>");
            }
            case HER_ROYAL_MOBILISATION -> {
                Player issuer = Bukkit.getPlayer(nation.getLeaderUUID());
                if (issuer != null) {
                    int n = 0;
                    for (UUID memberUUID : nation.getMembers().keySet()) {
                        Player p = Bukkit.getPlayer(memberUUID);
                        if (p == null || !p.isOnline() || p.equals(issuer)) continue;
                        p.teleport(issuer.getLocation());
                        n++;
                    }
                    broadcastToNation(nation, "<gold>📯 Royal Mobilisation: <yellow>" + n +
                            "</yellow> subjects answer the summons.</gold>");
                }
            }

            // ─ MARSHAL ───────────────────────────────────────────
            case MAR_DECLARE_WAR -> broadcastToNation(nation,
                    "<dark_red>⚔ <bold>WAR DECLARED IN THE KING'S NAME!</bold>");
            case MAR_ROYAL_CONSCRIPTION -> {
                int draftedCount = 0;
                for (UUID memberUUID : nation.getMembers().keySet()) {
                    Player p = Bukkit.getPlayer(memberUUID);
                    if (p == null || !p.isOnline()) continue;
                    org.bukkit.inventory.ItemStack[] kit = {
                            new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_HELMET),
                            new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_CHESTPLATE),
                            new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_LEGGINGS),
                            new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_BOOTS),
                            new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_SWORD),
                            new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOW),
                            new org.bukkit.inventory.ItemStack(org.bukkit.Material.ARROW, 32),
                            new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLDEN_APPLE, 3)
                    };
                    for (org.bukkit.inventory.ItemStack item : kit) {
                        var leftover = p.getInventory().addItem(item);
                        for (var dropItem : leftover.values()) {
                            p.getWorld().dropItemNaturally(p.getLocation(), dropItem);
                        }
                    }
                    draftedCount++;
                }
                broadcastToNation(nation, "<red>⚔ Royal Conscription: <gold>" + draftedCount +
                        "</gold> subjects equipped.");
            }
            case MAR_DEFENSE_ORDER -> {
                mg.setDefenseProtocolUntil(now + dur);
                int ticks = (int) (dur / 50);
                int affected = 0;
                for (UUID memberUUID : nation.getMembers().keySet()) {
                    Player p = Bukkit.getPlayer(memberUUID);
                    if (p == null || !p.isOnline()) continue;
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.RESISTANCE, ticks, 0, false, true, true));
                    affected++;
                }
                broadcastToNation(nation, "<aqua>🛡 Royal Defense Order active!</aqua> (" + affected + " online)");
            }
            case MAR_OFFENSE_ORDER -> {
                mg.setOffenseProtocolUntil(now + dur);
                int ticks = (int) (dur / 50);
                int affected = 0;
                for (UUID memberUUID : nation.getMembers().keySet()) {
                    Player p = Bukkit.getPlayer(memberUUID);
                    if (p == null || !p.isOnline()) continue;
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.STRENGTH, ticks, 0, false, true, true));
                    affected++;
                }
                broadcastToNation(nation, "<red>⚔ Royal Offense Order active!</red> (" + affected + " online)");
            }
            case MAR_MILITARY_VIGILANCE -> {
                mg.setMilitaryEmergencyUntil(now + dur);
                if (!nation.hasCapital()) break;
                int ticks = (int) (dur / 50);
                int targets = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (nation.isMember(p.getUniqueId())) continue;
                    Nation atLoc = plugin.getTerritoryManager().getNationAt(p.getLocation());
                    if (atLoc == null || !atLoc.getId().equals(nation.getId())) continue;
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.GLOWING, ticks, 0, false, true, true));
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.WEAKNESS, ticks, 1, false, true, true));
                    targets++;
                }
                broadcastToNation(nation, "<red>👁 Military Vigilance — " + targets + " trespassers exposed.</red>");
            }

            // ─ CHANCELLOR ───────────────────────────────────────
            case CHA_ROYAL_ALMSGIVING -> {
                mg.setDistributionProgramPhasesLeft(3);
                broadcastToNation(nation, "<green>💛 Royal Almsgiving — taxes suspended for 3 phases.</green>");
            }
            case CHA_ROYAL_STIMULUS -> {
                double amount = 10_000;
                int n = 0;
                for (UUID memberUUID : nation.getMembers().keySet()) {
                    plugin.getVaultHook().deposit(memberUUID, amount);
                    n++;
                }
                broadcastToNation(nation, "<green>💰 Royal Stimulus: " + n + " subjects receive $" +
                        MessageUtils.formatNumber(amount) + " each.");
            }
            case CHA_ROYAL_TUTELAGE -> {
                int xp = 10;
                int n = 0;
                for (UUID memberUUID : nation.getMembers().keySet()) {
                    Player p = Bukkit.getPlayer(memberUUID);
                    if (p == null || !p.isOnline()) continue;
                    p.giveExpLevels(xp);
                    n++;
                }
                broadcastToNation(nation, "<aqua>📚 Royal Tutelage: <gold>" + n + "</gold> subjects gain +" + xp + " XP.</aqua>");
            }
            case CHA_TAX_INTENSIFICATION -> {
                mg.setTaxIntensificationPhasesLeft(3);
                broadcastToNation(nation, "<dark_red>💎 Tax Intensification — 200% tax for 3 phases.</dark_red>");
            }
            case CHA_ROYAL_MARKET -> {
                mg.setMarketEventUntil(now + dur);
                broadcastToNation(nation, "<gold>🛒 Royal Market Festival active for 30 minutes.</gold>");
            }

            // ─ SAINT ────────────────────────────────────────────
            case SAI_ROYAL_QUARANTINE -> {
                mg.setQuarantineUntil(now + dur);
                broadcastToNation(nation, "<aqua>⛔ Royal Quarantine sealed for 10 minutes.</aqua>");
            }
            case SAI_ROYAL_HEALING -> {
                if (mg.isFieldMedicineOnCooldown()) break;
                mg.setFieldMedicineCooldownUntil(now + 2L * 60 * 60 * 1000);
                int ticks = (int) (dur / 50);
                int affected = 0;
                for (UUID memberUUID : nation.getMembers().keySet()) {
                    Player p = Bukkit.getPlayer(memberUUID);
                    if (p == null || !p.isOnline()) continue;
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.REGENERATION, ticks, 1, false, true, true));
                    affected++;
                }
                broadcastToNation(nation, "<green>✚ Royal Healing — " + affected + " subjects gain Regeneration II.</green>");
            }
            case SAI_HOLY_VACCINATION -> {
                mg.setVaccinationUntil(now + dur);
                broadcastToNation(nation, "<green>✚ Holy Vaccination — immunity to poison & wither for 1 hour.</green>");
            }
            case SAI_ROYAL_PROVISIONS -> {
                int n = 0;
                for (UUID memberUUID : nation.getMembers().keySet()) {
                    Player p = Bukkit.getPlayer(memberUUID);
                    if (p == null || !p.isOnline()) continue;
                    if (p.getFoodLevel() >= 10) continue;
                    org.bukkit.inventory.ItemStack bread = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BREAD, 8);
                    var leftover = p.getInventory().addItem(bread);
                    for (var dropItem : leftover.values()) {
                        p.getWorld().dropItemNaturally(p.getLocation(), dropItem);
                    }
                    n++;
                }
                broadcastToNation(nation, "<gold>🍞 Royal Provisions distributed to " + n + " hungry subjects.</gold>");
            }
            case SAI_CURSED_PLAGUE -> {
                mg.setPlagueUntil(now + dur);
                broadcastToNation(nation, "<dark_red>☠ Cursed Plague — territory cursed for 10 minutes.</dark_red>");
            }
        }
    }

    /** Used by the GUI to colour the decision card based on its current state. */
    public boolean isDecisionStateActive(MonarchyGovernment mg, MonarchyDecisionType type) {
        return switch (type) {
            case HER_GLORIFY_KING        -> mg.isGlorificationActive();
            case HER_CENSOR_PRESS        -> mg.isSensorMediaActive();
            case MAR_DEFENSE_ORDER       -> mg.isDefenseProtocolActive();
            case MAR_OFFENSE_ORDER       -> mg.isOffenseProtocolActive();
            case MAR_MILITARY_VIGILANCE  -> mg.isMilitaryEmergencyActive();
            case CHA_ROYAL_MARKET        -> mg.isMarketEventActive();
            case CHA_ROYAL_ALMSGIVING    -> mg.getDistributionProgramPhasesLeft() > 0;
            case CHA_TAX_INTENSIFICATION -> mg.getTaxIntensificationPhasesLeft() > 0;
            case SAI_ROYAL_QUARANTINE    -> mg.isQuarantineActive();
            case SAI_HOLY_VACCINATION    -> mg.isVaccinationActive();
            case SAI_CURSED_PLAGUE       -> mg.isPlagueActive();
            default -> false;
        };
    }

    /** Convenience accessor used by the executive-orders GUI. */
    public CommunistDecisionType asCommunistEquivalent(MonarchyDecisionType type) {
        return type.asCommunistEquivalent();
    }

    // ---------------------------------------------------------------
    // Royal Tax & subsidy schedulers
    // ---------------------------------------------------------------

    public void checkAllTaxPhases() {
        long phaseMinutes = plugin.getCommunistTaxPhaseMinutes();
        long phaseMs = phaseMinutes * 60 * 1000;
        double baseTaxAmount = plugin.getCommunistTaxAmount();

        for (Nation nation : plugin.getNationManager().getAllNations()) {
            if (nation.getType() != GovernmentType.MONARCHY) continue;
            MonarchyGovernment mg = nation.getMonarchyGovernment();
            if (mg == null) continue;

            long elapsed = System.currentTimeMillis() - mg.getLastTaxPhase();
            if (elapsed < phaseMs && mg.getLastTaxPhase() > 0) continue;

            if (mg.getDistributionProgramPhasesLeft() > 0) {
                mg.setDistributionProgramPhasesLeft(mg.getDistributionProgramPhasesLeft() - 1);
                broadcastToNation(nation, "<green>💛 Royal Almsgiving: tax suspended this phase. Remaining: " +
                        mg.getDistributionProgramPhasesLeft() + "</green>");
                mg.setLastTaxPhase(System.currentTimeMillis());
                continue;
            }

            double effectiveTax = baseTaxAmount;
            if (mg.getTaxIntensificationPhasesLeft() > 0) {
                effectiveTax = baseTaxAmount * 2;
                mg.setTaxIntensificationPhasesLeft(mg.getTaxIntensificationPhasesLeft() - 1);
                broadcastToNation(nation, "<dark_red>💎 Tax Intensification active: 200%. Remaining: " +
                        mg.getTaxIntensificationPhasesLeft() + "</dark_red>");
            }

            collectRoyalTax(nation, effectiveTax);
            mg.setLastTaxPhase(System.currentTimeMillis());
        }
    }

    private void collectRoyalTax(Nation nation, double taxAmount) {
        int collected = 0;
        double totalTax = 0;
        for (UUID memberUUID : nation.getMembers().keySet()) {
            double balance = plugin.getVaultHook().getBalance(memberUUID);
            if (balance < taxAmount) continue;
            if (plugin.getVaultHook().withdraw(memberUUID, taxAmount)) {
                collected++;
                totalTax += taxAmount;
            }
        }
        if (totalTax > 0) {
            plugin.getTreasuryManager().deposit(nation, TransactionType.TAX_INCOME, totalTax,
                    "Royal tax phase " + nation.getName(), null);
        }
        broadcastToNation(nation, "<gold>👑 Royal Tax " + nation.getName() +
                "</gold> <gray>($" + MessageUtils.formatNumber(taxAmount) + "/subject)</gray> " +
                "<gray>collected: <white>" + collected + "</white> subjects, total <gold>$" +
                MessageUtils.formatNumber(totalTax) + "</gold> to the treasury.</gray>");
        plugin.getDataManager().saveNations();
    }

    public void checkAllAlmsDistributions() {
        long intervalMs = plugin.getCommunistFreeFoodIntervalHours()
                * 60 * 60 * 1000;
        int breadCount = plugin.getCommunistFreeFoodBread();
        double costPerPlayer = plugin.getCommunistFreeFoodCostPerPlayer();

        for (Nation nation : plugin.getNationManager().getAllNations()) {
            if (nation.getType() != GovernmentType.MONARCHY) continue;
            MonarchyGovernment mg = nation.getMonarchyGovernment();
            if (mg == null) continue;

            long elapsed = System.currentTimeMillis() - mg.getLastAlmsDistribution();
            if (elapsed < intervalMs && mg.getLastAlmsDistribution() > 0) continue;

            distributeRoyalAlms(nation, breadCount, costPerPlayer);
            mg.setLastAlmsDistribution(System.currentTimeMillis());
        }
    }

    private void distributeRoyalAlms(Nation nation, int breadCount, double costPerPlayer) {
        List<Player> onlineMembers = new ArrayList<>();
        for (UUID memberUUID : nation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(memberUUID);
            if (p != null && p.isOnline()) onlineMembers.add(p);
        }
        if (onlineMembers.isEmpty()) return;

        double totalCost = costPerPlayer * onlineMembers.size();
        if (!plugin.getTreasuryManager().canAfford(nation, totalCost)) {
            broadcastToNation(nation, "<red>⚠ Royal Treasury cannot afford Royal Alms today ($" +
                    MessageUtils.formatNumber(totalCost) + ").</red>");
            return;
        }
        plugin.getTreasuryManager().withdraw(nation, TransactionType.STIMULUS, totalCost,
                "Royal Alms — " + nation.getName(), null);

        org.bukkit.inventory.ItemStack bread = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BREAD, breadCount);
        for (Player member : onlineMembers) {
            var leftover = member.getInventory().addItem(bread.clone());
            for (var item : leftover.values()) {
                member.getWorld().dropItemNaturally(member.getLocation(), item);
            }
            MessageUtils.send(member, "<gold>🍞 Royal Alms: <yellow>" + breadCount +
                    " bread</yellow> from the Crown of " + nation.getName() + "!</gold>");
        }
        plugin.getDataManager().saveNations();
    }

    public void checkCouncilSalaries() {
        long weekMs = 7L * 24 * 60 * 60 * 1000;
        double weeklySalary = plugin.getCommunistMinisterWeeklySalary();

        for (Nation nation : plugin.getNationManager().getAllNations()) {
            if (nation.getType() != GovernmentType.MONARCHY) continue;
            MonarchyGovernment mg = nation.getMonarchyGovernment();
            if (mg == null) continue;

            for (HighCouncilMember member : mg.getHighCouncil().values()) {
                long elapsed = System.currentTimeMillis() - member.getLastDailyReward();
                if (elapsed < weekMs && member.getLastDailyReward() > 0) continue;

                if (!plugin.getTreasuryManager().canAfford(nation, weeklySalary)) continue;
                plugin.getTreasuryManager().withdraw(nation, TransactionType.CABINET_SALARY,
                        weeklySalary, "High Council weekly stipend — " + member.getPosition().name(),
                        member.getUuid());
                plugin.getVaultHook().deposit(member.getUuid(), weeklySalary);
                member.setLastDailyReward(System.currentTimeMillis());

                Player p = Bukkit.getPlayer(member.getUuid());
                if (p != null) {
                    MessageUtils.send(p, "<gold>👑 Royal Stipend: <green>+$" +
                            MessageUtils.formatNumber(weeklySalary) + "</green> from the Crown of " +
                            nation.getName() + ".");
                }
            }
            plugin.getDataManager().saveNations();
        }
    }

    // ---------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------

    private void broadcastToNation(Nation nation, String message) {
        if (nation == null) return;
        for (UUID uuid : nation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) MessageUtils.send(p, message);
        }
    }

    public long getSalaryCooldown(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null || nation.getType() != GovernmentType.MONARCHY) return -1;
        MonarchyGovernment mg = nation.getMonarchyGovernment();
        if (mg == null) return -1;

        if (mg.hasKing() && mg.getKingUUID().equals(player.getUniqueId())) {
            long now = System.currentTimeMillis();
            long dayMillis = 24 * 60 * 60 * 1000L;
            long diff = now - mg.getLastDailyReward();
            return (diff >= dayMillis) ? 0 : (dayMillis - diff);
        }

        MonarchyGovernment.HighCouncilMember member = mg.getCouncilMemberByUUID(player.getUniqueId());
        if (member != null) {
            long now = System.currentTimeMillis();
            long dayMillis = 24 * 60 * 60 * 1000L;
            long diff = now - member.getLastDailyReward();
            return (diff >= dayMillis) ? 0 : (dayMillis - diff);
        }
        return -1; // Not eligible
    }

    public void claimDailySalary(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null || nation.getType() != GovernmentType.MONARCHY) {
            MessageUtils.send(player, "<red>You are not in a Monarchy kingdom!</red>");
            return;
        }
        MonarchyGovernment mg = nation.getMonarchyGovernment();
        if (mg == null) {
            MessageUtils.send(player, "<red>No active Monarchy government!</red>");
            return;
        }

        long cooldown = getSalaryCooldown(player);
        if (cooldown > 0) {
            MessageUtils.send(player, "<red>You can claim your salary in: " + MessageUtils.formatTime(cooldown) + "</red>");
            return;
        } else if (cooldown == -1) {
            MessageUtils.send(player, "<red>You are not eligible for a government salary!</red>");
            return;
        }

        long now = System.currentTimeMillis();

        if (mg.hasKing() && mg.getKingUUID().equals(player.getUniqueId())) {
            double vaultPoints = plugin.getConfig().getDouble("president.daily-rewards.vault-points", 50000);
            int diamondBlocks = plugin.getConfig().getInt("president.daily-rewards.diamond-blocks", 5);
            int netheriteIngots = plugin.getConfig().getInt("president.daily-rewards.netherite-ingots", 3);
            int goldenApples = plugin.getConfig().getInt("president.daily-rewards.enchanted-golden-apples", 10);

            if (plugin.getTreasuryManager().canAfford(nation, vaultPoints)) {
                plugin.getTreasuryManager().withdraw(nation, TransactionType.PRESIDENT_SALARY, vaultPoints,
                        "Daily salary for King " + player.getName(), player.getUniqueId());
                plugin.getVaultHook().deposit(player.getUniqueId(), vaultPoints);
                mg.addSubsidyPayout(vaultPoints); // Track payout
                mg.setLastDailyReward(now);
                MessageUtils.send(player, "managers.government.daily_reward");
            } else {
                MessageUtils.send(player, "<red>The Treasury cannot afford your daily salary!</red>");
                return;
            }

            var leftover1 = player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_BLOCK, diamondBlocks));
            for (var item : leftover1.values()) { player.getWorld().dropItemNaturally(player.getLocation(), item); }
            var leftover2 = player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHERITE_INGOT, netheriteIngots));
            for (var item : leftover2.values()) { player.getWorld().dropItemNaturally(player.getLocation(), item); }
            var leftover3 = player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.ENCHANTED_GOLDEN_APPLE, goldenApples));
            for (var item : leftover3.values()) { player.getWorld().dropItemNaturally(player.getLocation(), item); }

            MessageUtils.playSound(player, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP);
            plugin.getDataManager().saveNations();
            return;
        }

        MonarchyGovernment.HighCouncilMember member = mg.getCouncilMemberByUUID(player.getUniqueId());
        if (member != null) {
            MonarchyGovernment.HighCouncilPosition position = member.getPosition();
            String configPath = switch (position) {
                case MARSHAL -> "cabinet.defense.daily-vault";
                case CHANCELLOR -> "cabinet.treasury.daily-vault";
                case SAINT -> "cabinet.health.daily-vault";
                default -> "cabinet.daily-salary";
            };

            YamlConfiguration nationConfig = plugin.getNationConfig(GovernmentType.MONARCHY);
            double vaultPoints = nationConfig != null ? nationConfig.getDouble(configPath, 30000) : 30000;
            double salary = nationConfig != null ? nationConfig.getDouble("cabinet.daily-salary", 20000) : 20000;
            double totalPay = vaultPoints + salary;

            if (plugin.getTreasuryManager().canAfford(nation, totalPay)) {
                plugin.getTreasuryManager().withdraw(nation, TransactionType.CABINET_SALARY, totalPay,
                        "Daily salary for " + position.getDisplayName() + " " + player.getName(), player.getUniqueId());
                plugin.getVaultHook().deposit(player.getUniqueId(), totalPay);
                mg.addSubsidyPayout(totalPay); // Track payout
                member.setLastDailyReward(now);
                MessageUtils.send(player, "managers.government.cabinet_daily");
            } else {
                MessageUtils.send(player, "<red>The Treasury cannot afford your daily salary!</red>");
                return;
            }

            switch (position) {
                case MARSHAL -> {
                    var leftoverA = player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND, 3));
                    for (var item : leftoverA.values()) { player.getWorld().dropItemNaturally(player.getLocation(), item); }
                    var leftoverB = player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLDEN_APPLE, 5));
                    for (var item : leftoverB.values()) { player.getWorld().dropItemNaturally(player.getLocation(), item); }
                }
                case CHANCELLOR -> {
                    var leftoverC = player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.EMERALD_BLOCK, 10));
                    for (var item : leftoverC.values()) { player.getWorld().dropItemNaturally(player.getLocation(), item); }
                }
                default -> {}
            }

            MessageUtils.playSound(player, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP);
            plugin.getDataManager().saveNations();
        }
    }

    /** Sort comparator helper to mirror Communist nationalisation if needed later. */
    @SuppressWarnings("unused")
    private static final Comparator<UUID> RICHEST_FIRST = null;
}
