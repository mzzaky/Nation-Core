package id.nationcore.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import id.nationcore.NationCore;
import id.nationcore.models.CaliphateGovernment;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.Nation;
import id.nationcore.models.Treasury.TransactionType;
import id.nationcore.utils.MessageUtils;

/**
 * Specific operations for CALIPHATE type nations.
 *
 * The caliphate is intentionally minimal: it has no cabinet/ministers and no
 * minister-level decisions. The Caliph's only governing tool is the standard
 * executive order (with regular cooldown — same as Republic / Communist;
 * unlike the King, the Caliph is not exempt from the cooldown).
 *
 * Tax / Bayt al-Mal scheduling reuses the same hooks as the Communist regime
 * so server operators do not need to add new YAML keys.
 */
public class CaliphateManager {

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

    public CaliphateManager(NationCore plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------
    // Resolver helpers
    // ---------------------------------------------------------------

    public CaliphateGovernment getGovernment(Nation nation) {
        if (nation == null || nation.getType() != GovernmentType.CALIPHATE) return null;
        return nation.getCaliphateGovernment();
    }

    public boolean isCaliph(Nation nation, UUID uuid) {
        CaliphateGovernment cg = getGovernment(nation);
        return cg != null && cg.hasCaliph() && cg.getCaliphUUID().equals(uuid);
    }

    public boolean isShuraMember(Nation nation, UUID uuid) {
        CaliphateGovernment cg = getGovernment(nation);
        return cg != null && cg.isShuraMember(uuid);
    }

    public boolean isStateScholar(Nation nation, UUID uuid) {
        CaliphateGovernment cg = getGovernment(nation);
        return cg != null && cg.isScholar(uuid);
    }

    // ---------------------------------------------------------------
    // Caliph lifecycle (admin / disband only — no election in a Caliphate)
    // ---------------------------------------------------------------

    public void setCaliph(Nation nation, UUID uuid, String name) {
        CaliphateGovernment cg = getGovernment(nation);
        if (cg == null) return;



        cg.setCaliphUUID(uuid);
        cg.setCaliphName(name);
        nation.setLeaderUUID(uuid);
        nation.setLeaderName(name);
        cg.setAscensionTime(System.currentTimeMillis());
        cg.setLastCaliphActivity(System.currentTimeMillis());

        broadcastToNation(nation, "<dark_green>" + name + " has ascended as the Caliph of " +
                nation.getName() + " — may justice prevail!</dark_green>");



        plugin.getDataManager().saveNations();
    }

    // ---------------------------------------------------------------
    // Shura Council membership (Caliph-only)
    // ---------------------------------------------------------------

    public Result appointShura(Nation nation, UUID actor, UUID targetUUID, String targetName) {
        CaliphateGovernment cg = getGovernment(nation);
        if (cg == null) return Result.fail("This nation is not a Caliphate.");
        if (!cg.hasCaliph()) return Result.fail("No Caliph currently leads this caliphate.");
        if (!isCaliph(nation, actor) && !hasAdminBypass(actor)) {
            return Result.fail("Only the Caliph may appoint Shura Council members.");
        }
        if (!nation.isMember(targetUUID)) {
            return Result.fail("Target is not a citizen of this caliphate.");
        }
        if (cg.isShuraMember(targetUUID)) {
            return Result.fail(targetName + " is already on the Shura Council.");
        }
        if (cg.getShuraCount() >= CaliphateGovernment.MAX_SHURA) {
            return Result.fail("The Shura Council is already at its maximum of "
                    + CaliphateGovernment.MAX_SHURA + " members.");
        }
        cg.addShuraMember(targetUUID);

        Player target = Bukkit.getPlayer(targetUUID);
        if (target != null) {
            MessageUtils.send(target, "<dark_green>You have been appointed to the Shura Council of "
                    + nation.getName() + ".</dark_green>");
        }
        broadcastToNation(nation, "<green><dark_green>" + targetName +
                "</dark_green> has joined the Shura Council of " + nation.getName() + ".</green>");
        plugin.getDataManager().saveNations();
        return Result.ok("Appointed " + targetName + " to the Shura Council.");
    }

    public Result removeShura(Nation nation, UUID actor, UUID targetUUID, String targetName) {
        CaliphateGovernment cg = getGovernment(nation);
        if (cg == null) return Result.fail("This nation is not a Caliphate.");
        if (!isCaliph(nation, actor) && !hasAdminBypass(actor)) {
            return Result.fail("Only the Caliph may dismiss Shura Council members.");
        }
        if (!cg.removeShuraMember(targetUUID)) {
            return Result.fail(targetName + " is not on the Shura Council.");
        }
        Player target = Bukkit.getPlayer(targetUUID);
        if (target != null) {
            MessageUtils.send(target, "<red>You have been dismissed from the Shura Council of "
                    + nation.getName() + ".</red>");
        }
        broadcastToNation(nation, "<yellow><gold>" + targetName +
                "</gold> has been dismissed from the Shura Council.</yellow>");
        plugin.getDataManager().saveNations();
        return Result.ok("Dismissed " + targetName + " from the Shura Council.");
    }

    // ---------------------------------------------------------------
    // State Scholars membership (Caliph-only)
    // ---------------------------------------------------------------

    public Result appointScholar(Nation nation, UUID actor, UUID targetUUID, String targetName) {
        CaliphateGovernment cg = getGovernment(nation);
        if (cg == null) return Result.fail("This nation is not a Caliphate.");
        if (!cg.hasCaliph()) return Result.fail("No Caliph currently leads this caliphate.");
        if (!isCaliph(nation, actor) && !hasAdminBypass(actor)) {
            return Result.fail("Only the Caliph may appoint State Scholars.");
        }
        if (!nation.isMember(targetUUID)) {
            return Result.fail("Target is not a citizen of this caliphate.");
        }
        if (cg.isScholar(targetUUID)) {
            return Result.fail(targetName + " is already a State Scholar.");
        }
        if (cg.getScholarCount() >= CaliphateGovernment.MAX_SCHOLARS) {
            return Result.fail("The State Scholars body is already at its maximum of "
                    + CaliphateGovernment.MAX_SCHOLARS + " members.");
        }
        cg.addScholar(targetUUID);

        Player target = Bukkit.getPlayer(targetUUID);
        if (target != null) {
            MessageUtils.send(target, "<dark_green>You have been ordained as a State Scholar of "
                    + nation.getName() + ".</dark_green>");
        }
        broadcastToNation(nation, "<green><dark_green>" + targetName +
                "</dark_green> has been ordained as a State Scholar.</green>");
        plugin.getDataManager().saveNations();
        return Result.ok("Ordained " + targetName + " as a State Scholar.");
    }

    public Result removeScholar(Nation nation, UUID actor, UUID targetUUID, String targetName) {
        CaliphateGovernment cg = getGovernment(nation);
        if (cg == null) return Result.fail("This nation is not a Caliphate.");
        if (!isCaliph(nation, actor) && !hasAdminBypass(actor)) {
            return Result.fail("Only the Caliph may remove State Scholars.");
        }
        if (!cg.removeScholar(targetUUID)) {
            return Result.fail(targetName + " is not a State Scholar.");
        }
        Player target = Bukkit.getPlayer(targetUUID);
        if (target != null) {
            MessageUtils.send(target, "<red>You have been removed from the State Scholars of "
                    + nation.getName() + ".</red>");
        }
        broadcastToNation(nation, "<yellow><gold>" + targetName +
                "</gold> has been removed from the State Scholars.</yellow>");
        plugin.getDataManager().saveNations();
        return Result.ok("Removed " + targetName + " from the State Scholars.");
    }

    // ---------------------------------------------------------------
    // Jizya tax & Zakat (free food / subsidy) schedulers
    // ---------------------------------------------------------------

    public void checkAllTaxPhases() {
        long phaseMinutes = plugin.getCommunistTaxPhaseMinutes();
        long phaseMs = phaseMinutes * 60 * 1000;
        double baseTaxAmount = plugin.getCommunistTaxAmount();

        for (Nation nation : plugin.getNationManager().getAllNations()) {
            if (nation.getType() != GovernmentType.CALIPHATE) continue;
            CaliphateGovernment cg = nation.getCaliphateGovernment();
            if (cg == null) continue;

            long elapsed = System.currentTimeMillis() - cg.getLastTaxPhase();
            if (elapsed < phaseMs && cg.getLastTaxPhase() > 0) continue;

            if (cg.getTaxReliefPhasesLeft() > 0) {
                cg.setTaxReliefPhasesLeft(cg.getTaxReliefPhasesLeft() - 1);
                broadcastToNation(nation, "<green>☪ Zakat Relief: Jizya suspended this phase. Remaining: " +
                        cg.getTaxReliefPhasesLeft() + "</green>");
                cg.setLastTaxPhase(System.currentTimeMillis());
                continue;
            }

            double effectiveTax = baseTaxAmount;
            if (cg.getTaxLevyPhasesLeft() > 0) {
                effectiveTax = baseTaxAmount * 2;
                cg.setTaxLevyPhasesLeft(cg.getTaxLevyPhasesLeft() - 1);
                broadcastToNation(nation, "<dark_red>⚖ Special Levy active: 200% Jizya. Remaining: " +
                        cg.getTaxLevyPhasesLeft() + "</dark_red>");
            }

            collectJizya(nation, effectiveTax);
            cg.setLastTaxPhase(System.currentTimeMillis());
        }
    }

    private void collectJizya(Nation nation, double taxAmount) {
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
                    "Jizya tax phase " + nation.getName(), null);
        }
        broadcastToNation(nation, "<dark_green>☪ Jizya Tax " + nation.getName() +
                "</dark_green> <gray>($" + MessageUtils.formatNumber(taxAmount) + "/citizen)</gray> " +
                "<gray>collected: <white>" + collected + "</white> citizens, total <green>$" +
                MessageUtils.formatNumber(totalTax) + "</green> to Bayt al-Mal.</gray>");
        plugin.getDataManager().saveNations();
    }

    public void checkAllZakatDistributions() {
        long intervalMs = plugin.getCommunistFreeFoodIntervalHours()
                * 60 * 60 * 1000;
        int breadCount = plugin.getCommunistFreeFoodBread();
        double costPerPlayer = plugin.getCommunistFreeFoodCostPerPlayer();

        for (Nation nation : plugin.getNationManager().getAllNations()) {
            if (nation.getType() != GovernmentType.CALIPHATE) continue;
            CaliphateGovernment cg = nation.getCaliphateGovernment();
            if (cg == null) continue;

            long elapsed = System.currentTimeMillis() - cg.getLastZakatDistribution();
            if (elapsed < intervalMs && cg.getLastZakatDistribution() > 0) continue;

            distributeZakat(nation, breadCount, costPerPlayer);
            cg.setLastZakatDistribution(System.currentTimeMillis());
        }
    }

    private void distributeZakat(Nation nation, int breadCount, double costPerPlayer) {
        List<Player> onlineMembers = new ArrayList<>();
        for (UUID memberUUID : nation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(memberUUID);
            if (p != null && p.isOnline()) onlineMembers.add(p);
        }
        if (onlineMembers.isEmpty()) return;

        double totalCost = costPerPlayer * onlineMembers.size();
        if (!plugin.getTreasuryManager().canAfford(nation, totalCost)) {
            broadcastToNation(nation, "<red>⚠ Bayt al-Mal cannot afford the Zakat distribution today ($" +
                    MessageUtils.formatNumber(totalCost) + ").</red>");
            return;
        }
        plugin.getTreasuryManager().withdraw(nation, TransactionType.STIMULUS, totalCost,
                "Zakat distribution — " + nation.getName(), null);

        org.bukkit.inventory.ItemStack bread = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BREAD, breadCount);
        for (Player member : onlineMembers) {
            var leftover = member.getInventory().addItem(bread.clone());
            for (var item : leftover.values()) {
                member.getWorld().dropItemNaturally(member.getLocation(), item);
            }
            MessageUtils.send(member, "<dark_green>☪ Zakat: <green>" + breadCount +
                    " bread</green> from Bayt al-Mal of " + nation.getName() + ".</dark_green>");
        }
        plugin.getDataManager().saveNations();
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private boolean hasAdminBypass(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        return p != null && p.hasPermission("nation.admin");
    }

    private void broadcastToNation(Nation nation, String message) {
        if (nation == null) return;
        for (UUID uuid : nation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) MessageUtils.send(p, message);
        }
    }

    public long getSalaryCooldown(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null || nation.getType() != GovernmentType.CALIPHATE) return -1;
        CaliphateGovernment cg = nation.getCaliphateGovernment();
        if (cg == null) return -1;

        if (cg.hasCaliph() && cg.getCaliphUUID().equals(player.getUniqueId())) {
            long now = System.currentTimeMillis();
            long dayMillis = 24 * 60 * 60 * 1000L;
            long diff = now - cg.getLastDailyReward();
            return (diff >= dayMillis) ? 0 : (dayMillis - diff);
        }
        return -1; // Not eligible
    }

    public void claimDailySalary(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null || nation.getType() != GovernmentType.CALIPHATE) {
            MessageUtils.send(player, "<red>You are not in a Caliphate nation!</red>");
            return;
        }
        CaliphateGovernment cg = nation.getCaliphateGovernment();
        if (cg == null) {
            MessageUtils.send(player, "<red>No active Caliphate government!</red>");
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

        if (cg.hasCaliph() && cg.getCaliphUUID().equals(player.getUniqueId())) {
            double vaultPoints = plugin.getConfig().getDouble("president.daily-rewards.vault-points", 50000);
            int diamondBlocks = plugin.getConfig().getInt("president.daily-rewards.diamond-blocks", 5);
            int netheriteIngots = plugin.getConfig().getInt("president.daily-rewards.netherite-ingots", 3);
            int goldenApples = plugin.getConfig().getInt("president.daily-rewards.enchanted-golden-apples", 10);

            if (plugin.getTreasuryManager().canAfford(nation, vaultPoints)) {
                plugin.getTreasuryManager().withdraw(nation, TransactionType.PRESIDENT_SALARY, vaultPoints,
                        "Daily salary for Caliph " + player.getName(), player.getUniqueId());
                plugin.getVaultHook().deposit(player.getUniqueId(), vaultPoints);
                cg.addSubsidyPayout(vaultPoints); // Track payout
                cg.setLastDailyReward(now);
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
        }
    }
}
