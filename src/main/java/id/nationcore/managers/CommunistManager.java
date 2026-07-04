package id.nationcore.managers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import id.nationcore.NationCore;
import id.nationcore.models.CommunistDecisionType;
import id.nationcore.models.CommunistGovernment;
import id.nationcore.models.CommunistGovernment.PolitburoMember;
import id.nationcore.models.CommunistGovernment.PolitburoPosition;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.Nation;
import id.nationcore.models.Treasury.TransactionType;
import id.nationcore.utils.MessageUtils;

/**
 * Specific operations for COMMUNIST type nations.
 *
 * Responsible for: appointing/dismissing politburo, party membership,
 * (Phase 4B: progressive tax, daily subsidies, special executive order).
 *
 * Created parallel to {@link GovernmentManager} instead of as a sub-class to keep the
 * scope clear — Communism has a very different power structure and flow from
 * Republic (politburo vs cabinet, party-only voting vs popular vote).
 */
public class CommunistManager {

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

    public CommunistManager(NationCore plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------
    // Resolver helpers
    // ---------------------------------------------------------------

    public CommunistGovernment getGovernment(Nation nation) {
        if (nation == null || nation.getType() != GovernmentType.COMMUNIST) return null;
        return nation.getCommunistGovernment();
    }

    public boolean isSecretaryGeneral(Nation nation, UUID uuid) {
        CommunistGovernment cg = getGovernment(nation);
        return cg != null && cg.hasSecretaryGeneral() &&
                cg.getSecretaryGeneralUUID().equals(uuid);
    }

    public boolean isPolitburoMember(Nation nation, UUID uuid) {
        CommunistGovernment cg = getGovernment(nation);
        return cg != null && cg.getPolitburoMemberByUUID(uuid) != null;
    }

    public PolitburoPosition getPolitburoPosition(Nation nation, UUID uuid) {
        CommunistGovernment cg = getGovernment(nation);
        return cg != null ? cg.getPositionByUUID(uuid) : null;
    }

    // ---------------------------------------------------------------
    // Politbiro management
    // ---------------------------------------------------------------

    public Result appointPolitburo(Nation nation, PolitburoPosition position,
                                   UUID targetUUID, String targetName) {
        CommunistGovernment cg = getGovernment(nation);
        if (cg == null) return Result.fail("This nation is not of type Communist.");
        if (!cg.hasSecretaryGeneral()) {
            return Result.fail("No Secretary General is currently serving.");
        }
        if (!nation.isMember(targetUUID)) {
            return Result.fail("Target is not a member of this nation.");
        }
        // Must be a Party member first — Politburo only from the Party
        if (!cg.isPartyMember(targetUUID)) {
            if (cg.getPartyMemberCount() >= 5) {
                return Result.fail("Target is not a Party member and the Party is full (max 5). " +
                        "Admit them to the Party first if space becomes available.");
            }
            cg.addPartyMember(targetUUID);
        }

        // Remove from old position if already holding another slot
        PolitburoPosition currentPos = cg.getPositionByUUID(targetUUID);
        if (currentPos != null) cg.removePolitburo(currentPos);

        // Remove existing member in target position if present
        PolitburoMember existing = cg.getPolitburoMember(position);
        if (existing != null) {
            Player existingPlayer = Bukkit.getPlayer(existing.getUuid());
            if (existingPlayer != null) {
                plugin.getBuffManager().removeCabinetBuffs(existingPlayer);
                MessageUtils.send(existingPlayer, "<gray>You have been removed from the Politburo of " +
                        nation.getName() + ".</gray>");
            }
        }

        cg.appointPolitburo(position, new PolitburoMember(targetUUID, targetName, position));

        Player target = Bukkit.getPlayer(targetUUID);
        if (target != null) {
            // Reuse cabinet defense/treasury buff — Phase 4A does not
            // differentiate buffs; Phase 4B can tune itself.
            // Temporarily skip applying buff (requires mapping PolitburoPosition →
            // Government.CabinetPosition; will be handled in BuffManager).
            MessageUtils.send(target, "<gold>You have been appointed as " +
                    position.getDisplayName() + " in the Politburo of " + nation.getName() + "!</gold>");
        }

        broadcastToNation(nation, "<yellow><gold>" + targetName + "</gold> has been appointed as " +
                position.getDisplayName() + " in the Politburo of " + nation.getName() + ".</yellow>");

        plugin.getDataManager().saveNations();
        return Result.ok("Successfully appointed " + targetName + " as " +
                position.getDisplayName() + ".");
    }

    public Result removePolitburo(Nation nation, PolitburoPosition position) {
        CommunistGovernment cg = getGovernment(nation);
        if (cg == null) return Result.fail("This nation is not of type Communist.");

        PolitburoMember member = cg.getPolitburoMember(position);
        if (member == null) return Result.fail("Position " + position.getDisplayName() + " is empty.");

        Player target = Bukkit.getPlayer(member.getUuid());
        if (target != null) {
            plugin.getBuffManager().removeCabinetBuffs(target);
            MessageUtils.send(target, "<red>You have been removed from the Politburo of " + nation.getName() + ".</red>");
        }

        cg.removePolitburo(position);

        broadcastToNation(nation, "<yellow><gold>" + member.getName() +
                "</gold> was removed from the Politburo of " + nation.getName() + ".</yellow>");

        plugin.getDataManager().saveNations();
        return Result.ok("Position " + position.getDisplayName() + " is now empty.");
    }

    // ---------------------------------------------------------------
    // Party membership
    // ---------------------------------------------------------------

    public Result joinParty(Nation nation, Player player) {
        CommunistGovernment cg = getGovernment(nation);
        if (cg == null) return Result.fail("This nation is not of type Communist.");
        if (!nation.isMember(player.getUniqueId())) {
            return Result.fail("You are not a member of this nation.");
        }
        if (cg.isPartyMember(player.getUniqueId())) {
            return Result.fail("You are already a Party member.");
        }
        if (cg.getPartyMemberCount() >= 5) {
            return Result.fail("The Party has reached its maximal limit of 5 members.");
        }
        cg.addPartyMember(player.getUniqueId());
        plugin.getDataManager().saveNations();
        return Result.ok("Welcome to the Party! You are now eligible for internal voting.");
    }

    public Result leaveParty(Nation nation, Player player) {
        CommunistGovernment cg = getGovernment(nation);
        if (cg == null) return Result.fail("This nation is not of type Communist.");
        if (!cg.isPartyMember(player.getUniqueId())) {
            return Result.fail("You are not a member of the Party.");
        }
        // Secretary General cannot leave the Party (must step down first)
        if (cg.hasSecretaryGeneral() && cg.getSecretaryGeneralUUID().equals(player.getUniqueId())) {
            return Result.fail("The Secretary General cannot leave the Party. " +
                    "Step down first.");
        }
        // Politburo automatically removed when leaving the Party
        PolitburoPosition pos = cg.getPositionByUUID(player.getUniqueId());
        if (pos != null) {
            removePolitburo(nation, pos);
        }
        cg.removePartyMember(player.getUniqueId());
        plugin.getDataManager().saveNations();
        return Result.ok("You have left the Party.");
    }

    // ---------------------------------------------------------------
    // Sekjen lifecycle
    // ---------------------------------------------------------------

    /**
     * Set new Secretary General. Reset Politburo, term, and activity.
     * Party election will be implemented in Phase 4B; currently
     * called by founder flow or admin force.
     */
    public void setSecretaryGeneral(Nation nation, UUID uuid, String name, boolean isNewTerm) {
        CommunistGovernment cg = getGovernment(nation);
        if (cg == null) return;

        // Cleanup former Secretary General & Politburo
        if (cg.hasSecretaryGeneral()) {
            Player ex = Bukkit.getPlayer(cg.getSecretaryGeneralUUID());
            if (ex != null) plugin.getBuffManager().removePresidentBuffs(ex);
            for (PolitburoMember m : cg.getPolitburo().values()) {
                Player p = Bukkit.getPlayer(m.getUuid());
                if (p != null) plugin.getBuffManager().removeCabinetBuffs(p);
            }
        }

        UUID previousUUID = cg.getSecretaryGeneralUUID();
        int previousTerms = cg.getConsecutiveTerms();

        cg.setSecretaryGeneralUUID(uuid);
        cg.setSecretaryGeneralName(name);
        // Important: Update nation-level leader UUID for systems that track leadership generically (like Research)
        nation.setLeaderUUID(uuid);
        nation.setLeaderName(name);
        cg.setTermStartTime(System.currentTimeMillis());
        cg.setLastSecretaryActivity(System.currentTimeMillis());
        cg.getPolitburo().clear();
        cg.addPartyMember(uuid);

        if (isNewTerm) {
            if (previousUUID != null && previousUUID.equals(uuid)) {
                cg.setConsecutiveTerms(previousTerms + 1);
            } else {
                cg.setConsecutiveTerms(1);
            }
        } else if (cg.getConsecutiveTerms() <= 0) {
            cg.setConsecutiveTerms(1);
        }

        broadcastToNation(nation, "<gold>" + name + " now serves as the " +
                "Secretary General of " + nation.getName() + "!</gold>");

        Player sekjen = Bukkit.getPlayer(uuid);
        if (sekjen != null) plugin.getBuffManager().applyPresidentBuffs(sekjen);

        plugin.getDataManager().saveNations();
    }

    public void endSecretaryGeneral(Nation nation, String reason) {
        CommunistGovernment cg = getGovernment(nation);
        if (cg == null || !cg.hasSecretaryGeneral()) return;

        Player ex = Bukkit.getPlayer(cg.getSecretaryGeneralUUID());
        if (ex != null) plugin.getBuffManager().removePresidentBuffs(ex);
        for (PolitburoMember m : cg.getPolitburo().values()) {
            Player p = Bukkit.getPlayer(m.getUuid());
            if (p != null) plugin.getBuffManager().removeCabinetBuffs(p);
        }

        String name = cg.getSecretaryGeneralName();
        cg.setSecretaryGeneralUUID(null);
        cg.setSecretaryGeneralName(null);
        cg.getPolitburo().clear();

        broadcastToNation(nation, "<yellow>Secretary General <gold>" + name +
                "</gold> of " + nation.getName() + " stepped down. Reason: <white>" +
                reason + "</white></yellow>");

        plugin.getDataManager().saveNations();
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

    // ==========================================================
    // Sekretaris Jenderal weekly election (party-only voting)
    // ==========================================================

    /**
     * Party members cast vote for Secretary General candidate.
     * Voter can choose themselves or another Party member.
     * The next vote will replace the previous vote.
     */
    public Result castPartyVote(Nation nation, Player voter, UUID candidateUUID) {
        CommunistGovernment cg = getGovernment(nation);
        if (cg == null) return Result.fail("This nation is not of type Communist.");
        if (!cg.isPartyMember(voter.getUniqueId())) {
            return Result.fail("Only Party members can participate in the Secretary General election.");
        }
        if (!cg.isPartyMember(candidateUUID)) {
            return Result.fail("Candidates must be Party members.");
        }
        cg.getPartyVotes().put(voter.getUniqueId(), candidateUUID);
        plugin.getDataManager().saveNations();

        String candidateName = Bukkit.getOfflinePlayer(candidateUUID).getName();
        return Result.ok("Your vote for " + candidateName + " recorded. Vote can be changed " +
                "at any time before the cycle ends.");
    }

    /**
     * Iterate all COMMUNIST nations. If election cycle has ended
     * ({@code electionCycleStart + cycleDays >= now}), tally vote and promote
     * candidate with the most votes. If no votes, current Secretary General remains.
     */
    public void checkAllPartyElections() {
        long cycleDays = plugin.getConfig().getLong("nation.communist.election-cycle-days", 7);
        long cycleMs = cycleDays * 24L * 60 * 60 * 1000;

        for (Nation nation : plugin.getNationManager().getAllNations()) {
            if (nation.getType() != GovernmentType.COMMUNIST) continue;
            CommunistGovernment cg = nation.getCommunistGovernment();
            if (cg == null) continue;

            long elapsed = System.currentTimeMillis() - cg.getElectionCycleStart();
            if (elapsed < cycleMs) continue;

            tallyAndElect(nation);
        }
    }

    private void tallyAndElect(Nation nation) {
        CommunistGovernment cg = nation.getCommunistGovernment();
        if (cg == null) return;

        UUID winner = cg.getCurrentLeadCandidate();
        Map<UUID, Integer> counts = cg.getVoteCounts();

        if (winner == null) {
            // No votes — Secretary General remains, only restart cycle
            broadcastToNation(nation, "<gray>📜 Party election cycle for " + nation.getName() +
                    " ended without votes. The current Secretary General remains in office.</gray>");
            cg.clearPartyVotes();
            cg.setElectionCycleStart(System.currentTimeMillis());
            plugin.getDataManager().saveNations();
            return;
        }

        String winnerName = Bukkit.getOfflinePlayer(winner).getName();
        int winnerVotes = counts.getOrDefault(winner, 0);

        // If winner is same as current Secretary General → confirm/extend term
        boolean isReelection = cg.hasSecretaryGeneral() && cg.getSecretaryGeneralUUID().equals(winner);

        broadcastToNation(nation, "");
        broadcastToNation(nation, "<gold>═══════════════════════════════════════</gold>");
        broadcastToNation(nation, "<red>🚩 <bold>PARTY ELECTION RESULTS: " + nation.getName() +
                "</bold> <red>🚩");
        broadcastToNation(nation, "<gold>═══════════════════════════════════════</gold>");
        broadcastToNation(nation, "<yellow>Winner: <gold>" + winnerName +
                "</gold> <gray>(" + winnerVotes + " votes)</gray>");

        if (isReelection) {
            broadcastToNation(nation, "<gray>Secretary General continues their term.</gray>");
            cg.setConsecutiveTerms(cg.getConsecutiveTerms() + 1);
            cg.setTermStartTime(System.currentTimeMillis());
        } else {
            broadcastToNation(nation, "<gold>New Secretary General!</gold>");
            // Auto-promote: use setSecretaryGeneral (will reset Politburo)
            setSecretaryGeneral(nation, winner, winnerName, true);
        }
        broadcastToNation(nation, "<gold>═══════════════════════════════════════</gold>");

        cg.clearPartyVotes();
        cg.setElectionCycleStart(System.currentTimeMillis());
        plugin.getDataManager().saveNations();
    }

    public long getElectionCycleRemaining(Nation nation) {
        CommunistGovernment cg = getGovernment(nation);
        if (cg == null) return 0;
        long cycleDays = plugin.getConfig().getLong("nation.communist.election-cycle-days", 7);
        long cycleMs = cycleDays * 24L * 60 * 60 * 1000;
        long elapsed = System.currentTimeMillis() - cg.getElectionCycleStart();
        return Math.max(0, cycleMs - elapsed);
    }

    // ==========================================================
    // Weekly Communist Tax (50/member every 7 MC days)
    // ==========================================================

    /**
     * Iterate all COMMUNIST nations. If tax phase has passed
     * (default 7 Minecraft days = 140 real minutes), pull tax from
     * each online member & into nation treasury.
     *
     * Offline players are given leniency — billing to offline players is not
     * forced (no system debt). Phase 4C can add debt mechanism if needed.
     */
    public void checkAllTaxPhases() {
        long phaseMinutes = plugin.getConfig().getLong("nation.communist.tax-phase-minutes", 140);
        long phaseMs = phaseMinutes * 60 * 1000;
        double baseTaxAmount = plugin.getConfig().getDouble("nation.communist.tax-amount", 50);

        for (Nation nation : plugin.getNationManager().getAllNations()) {
            if (nation.getType() != GovernmentType.COMMUNIST) continue;
            CommunistGovernment cg = nation.getCommunistGovernment();
            if (cg == null) continue;

            long elapsed = System.currentTimeMillis() - cg.getLastTaxPhase();
            if (elapsed < phaseMs && cg.getLastTaxPhase() > 0) continue;

            // Distribution Program (Treasury Minister): skip total tax for N phases
            if (cg.getDistributionProgramPhasesLeft() > 0) {
                cg.setDistributionProgramPhasesLeft(cg.getDistributionProgramPhasesLeft() - 1);
                broadcastToNation(nation, "<green>💵 Distribution Program: tax for " +
                        nation.getName() + " exempted for this phase. Remaining free phases: " +
                        cg.getDistributionProgramPhasesLeft() + "</green>");
                cg.setLastTaxPhase(System.currentTimeMillis());
                continue;
            }

            // Tax Intensification (Treasury Minister): 200% tax for N phases
            double effectiveTax = baseTaxAmount;
            if (cg.getTaxIntensificationPhasesLeft() > 0) {
                effectiveTax = baseTaxAmount * 2;
                cg.setTaxIntensificationPhasesLeft(cg.getTaxIntensificationPhasesLeft() - 1);
                broadcastToNation(nation, "<dark_red>💰 Tax Intensification phase active: " +
                        "200% tax. Remaining intensification phases: " +
                        cg.getTaxIntensificationPhasesLeft() + "</dark_red>");
            }

            collectTax(nation, effectiveTax);
            cg.setLastTaxPhase(System.currentTimeMillis());
        }
    }

    private void collectTax(Nation nation, double taxAmount) {
        int collected = 0;
        double totalTax = 0;
        for (UUID memberUUID : nation.getMembers().keySet()) {
            double balance = plugin.getVaultHook().getBalance(memberUUID);
            if (balance < taxAmount) continue; // skip players with insufficient balance
            if (plugin.getVaultHook().withdraw(memberUUID, taxAmount)) {
                collected++;
                totalTax += taxAmount;
            }
        }
        if (totalTax > 0) {
            plugin.getTreasuryManager().deposit(nation, TransactionType.TAX_INCOME, totalTax,
                    "Communist tax phase " + nation.getName(), null);
        }
        broadcastToNation(nation, "<gold>💰 Communist Tax " + nation.getName() +
                "</gold> <gray>($" + MessageUtils.formatNumber(taxAmount) + "/member)</gray> " +
                "<gray>recorded: <white>" + collected + " members</white> total contribution <gold>$" +
                MessageUtils.formatNumber(totalTax) + "</gold> to treasury.</gray>");
        plugin.getDataManager().saveNations();
    }

    // ==========================================================
    // Free Food daily distribution (1000/pemain kas → 16 bread)
    // ==========================================================

    public void checkAllFreeFoodDistributions() {
        long intervalMs = plugin.getConfig().getLong("nation.communist.free-food-interval-hours", 24)
                * 60 * 60 * 1000;
        int breadCount = plugin.getConfig().getInt("nation.communist.free-food-bread", 16);
        double costPerPlayer = plugin.getConfig().getDouble("nation.communist.free-food-cost-per-player", 1000);

        for (Nation nation : plugin.getNationManager().getAllNations()) {
            if (nation.getType() != GovernmentType.COMMUNIST) continue;
            CommunistGovernment cg = nation.getCommunistGovernment();
            if (cg == null) continue;

            long elapsed = System.currentTimeMillis() - cg.getLastFreeFoodDistribution();
            if (elapsed < intervalMs && cg.getLastFreeFoodDistribution() > 0) continue;

            distributeFreeFood(nation, breadCount, costPerPlayer);
            cg.setLastFreeFoodDistribution(System.currentTimeMillis());
        }
    }

    private void distributeFreeFood(Nation nation, int breadCount, double costPerPlayer) {
        // Count online members who will receive distribution
        List<Player> onlineMembers = new ArrayList<>();
        for (UUID memberUUID : nation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(memberUUID);
            if (p != null && p.isOnline()) onlineMembers.add(p);
        }
        if (onlineMembers.isEmpty()) return;

        double totalCost = costPerPlayer * onlineMembers.size();
        if (!plugin.getTreasuryManager().canAfford(nation, totalCost)) {
            broadcastToNation(nation, "<red>⚠️ Treasury of " + nation.getName() +
                    " not enough for Free Food today ($" +
                    MessageUtils.formatNumber(totalCost) + " needed).</red>");
            return;
        }
        plugin.getTreasuryManager().withdraw(nation, TransactionType.STIMULUS, totalCost,
                "Free Food distribution — " + nation.getName(), null);

        // Distribute 16 bread per online player
        ItemStack bread = new ItemStack(Material.BREAD, breadCount);
        for (Player member : onlineMembers) {
            var leftover = member.getInventory().addItem(bread.clone());
            // Drop remainder if inventory is full
            for (var item : leftover.values()) {
                member.getWorld().dropItemNaturally(member.getLocation(), item);
            }
            MessageUtils.send(member, "<green>🍞 Free Food: <gold>" + breadCount +
                    " bread</gold> from treasury of " + nation.getName() + "!</green>");
        }
        broadcastToNation(nation, "<gold>🍞 Free Food " + nation.getName() +
                "</gold> <gray>distributed to <white>" + onlineMembers.size() +
                "</white> online members (treasury <red>-$" +
                MessageUtils.formatNumber(totalCost) + "</red>).</gray>");
        plugin.getDataManager().saveNations();
    }

    // ==========================================================
    // Executive Order: Nasionalisasi (Sekjen-only)
    // ==========================================================

    /**
     * Takes 10% from Vault balance of the 5 richest nation members, then
     * distributes 5% to all Party members (equally) and the remaining 95%
     * goes into the nation treasury.
     *
     * @return Result with execution summary
     */
    public Result executeNationalization(Nation nation, Player issuer) {
        CommunistGovernment cg = getGovernment(nation);
        if (cg == null) return Result.fail("This nation is not of type Communist.");
        if (!isSecretaryGeneral(nation, issuer.getUniqueId()) && !issuer.hasPermission("nation.admin")) {
            return Result.fail("Only the Secretary General can run Nationalization.");
        }

        // Cooldown via lastExecutiveOrderTime in Nation (shared with Republic EO)
        long cooldownDays = plugin.getConfig().getLong("executive-orders.cooldown-days", 7);
        long cooldownMs = cooldownDays * 24L * 60 * 60 * 1000;
        if (System.currentTimeMillis() - nation.getLastExecutiveOrderTime() < cooldownMs) {
            long remaining = cooldownMs - (System.currentTimeMillis() - nation.getLastExecutiveOrderTime());
            return Result.fail("Executive Order on cooldown — remaining " +
                    MessageUtils.formatTime(remaining) + ".");
        }

        // Sort members by Vault balance descending
        List<UUID> richest = new ArrayList<>(nation.getMembers().keySet());
        richest.sort(Comparator.comparingDouble(
                (UUID uuid) -> plugin.getVaultHook().getBalance(uuid)).reversed());

        int topN = Math.min(5, richest.size());
        if (topN == 0) return Result.fail("No members to nationalize.");

        double totalSeized = 0;
        StringBuilder seizureLog = new StringBuilder();
        for (int i = 0; i < topN; i++) {
            UUID memberUUID = richest.get(i);
            double balance = plugin.getVaultHook().getBalance(memberUUID);
            if (balance <= 0) continue;
            double take = Math.floor(balance * 0.10);
            if (take <= 0) continue;
            if (plugin.getVaultHook().withdraw(memberUUID, take)) {
                totalSeized += take;
                String memberName = Bukkit.getOfflinePlayer(memberUUID).getName();
                seizureLog.append("<gray>- <white>").append(memberName).append("</white> : -$")
                        .append(MessageUtils.formatNumber(take)).append("</gray>\n");
                Player p = Bukkit.getPlayer(memberUUID);
                if (p != null) {
                    MessageUtils.send(p, "<red>⚠️ Your property is nationalized! Seized: <gold>$" +
                            MessageUtils.formatNumber(take) + "</gold>");
                }
            }
        }

        if (totalSeized <= 0) return Result.fail("No property could be seized.");

        // 5% to party (equal), 95% to treasury
        double toParty = totalSeized * 0.05;
        double toTreasury = totalSeized - toParty;

        int partyCount = cg.getPartyMemberCount();
        double perParty = partyCount > 0 ? toParty / partyCount : 0;
        if (perParty > 0) {
            for (UUID partyUUID : cg.getPartyMembers()) {
                plugin.getVaultHook().deposit(partyUUID, perParty);
                Player p = Bukkit.getPlayer(partyUUID);
                if (p != null) {
                    MessageUtils.send(p, "<green>🚩 Nationalization Bonus: <gold>$" +
                            MessageUtils.formatNumber(perParty) + "</gold> for you as a Party member.</green>");
                }
            }
        } else {
            // No Party members — 100% to treasury
            toTreasury = totalSeized;
        }

        plugin.getTreasuryManager().deposit(nation, TransactionType.MISC_EXPENSE, toTreasury,
                "Nationalization — state treasury", issuer.getUniqueId());

        nation.setLastExecutiveOrderTime(System.currentTimeMillis());

        // Broadcast to nation
        broadcastToNation(nation, "");
        broadcastToNation(nation, "<dark_red>═══════════════════════════════════════</dark_red>");
        broadcastToNation(nation, "<red>⚡ <bold>NATIONALIZATION OF " + nation.getName() +
                "</bold> <red>⚡");
        broadcastToNation(nation, "<dark_red>═══════════════════════════════════════</dark_red>");
        broadcastToNation(nation, "<gray>Total seized: <gold>$" +
                MessageUtils.formatNumber(totalSeized) + "</gold>");
        broadcastToNation(nation, seizureLog.toString().trim());
        broadcastToNation(nation, "<gray>To Party (5%): <gold>$" +
                MessageUtils.formatNumber(toParty) + "</gold> (" + partyCount + " members)");
        broadcastToNation(nation, "<gray>To treasury (95%): <gold>$" +
                MessageUtils.formatNumber(toTreasury) + "</gold>");
        broadcastToNation(nation, "<dark_red>═══════════════════════════════════════</dark_red>");

        plugin.getDataManager().saveNations();
        return Result.ok("Nationalization successful. Total seized: $" +
                MessageUtils.formatNumber(totalSeized));
    }

    // ==========================================================
    // 20 Decisions Politbiro (Phase 4C)
    // ==========================================================

    /**
     * Check if player (as Politburo minister) can execute decision.
     * Validation: minister position matches, cooldown not ended, enough treasury.
     * Cooldown stored per-player in {@code cg.decisionCooldowns}.
     */
    public boolean canExecuteDecision(Nation nation, Player player, CommunistDecisionType type) {
        CommunistGovernment cg = getGovernment(nation);
        if (cg == null) return false;
        UUID uuid = player.getUniqueId();

        PolitburoPosition required = type.getPosition();
        PolitburoMember member = cg.getPolitburoMember(required);
        if (member == null || !member.getUuid().equals(uuid)) return false;

        long cooldownMs = plugin.getConfig().getLong("cabinet.decision-cooldown-hours", 48) * 3600000L;
        Map<String, Long> playerCooldowns = cg.getDecisionCooldowns()
                .computeIfAbsent(uuid, k -> new HashMap<>());
        Long lastUse = playerCooldowns.get(type.name());
        if (lastUse != null && System.currentTimeMillis() - lastUse < cooldownMs) return false;

        return plugin.getTreasuryManager().canAfford(nation, type.getCost());
    }

    public long getDecisionCooldownRemaining(Nation nation, UUID uuid, CommunistDecisionType type) {
        CommunistGovernment cg = getGovernment(nation);
        if (cg == null) return 0;
        long cooldownMs = plugin.getConfig().getLong("cabinet.decision-cooldown-hours", 48) * 3600000L;
        Map<String, Long> playerCooldowns = cg.getDecisionCooldowns().get(uuid);
        if (playerCooldowns == null) return 0;
        Long lastUse = playerCooldowns.get(type.name());
        if (lastUse == null) return 0;
        return Math.max(0, cooldownMs - (System.currentTimeMillis() - lastUse));
    }

    /**
     * Execute Politburo decision. Validates position, cooldown, and treasury;
     * then calls specific handler per type.
     */
    public Result executeDecision(Nation nation, Player player, CommunistDecisionType type) {
        CommunistGovernment cg = getGovernment(nation);
        if (cg == null) return Result.fail("This nation is not of type Communist.");

        PolitburoPosition required = type.getPosition();
        PolitburoMember member = cg.getPolitburoMember(required);
        if (member == null || !member.getUuid().equals(player.getUniqueId())) {
            // Secretary General has bypass — they can execute anything
            if (!isSecretaryGeneral(nation, player.getUniqueId()) &&
                    !player.hasPermission("nation.admin")) {
                return Result.fail("Only " + required.getDisplayName() +
                        " (or SecGen) can run " + type.getDisplayName() + ".");
            }
        }

        long remaining = getDecisionCooldownRemaining(nation, player.getUniqueId(), type);
        if (remaining > 0) {
            return Result.fail("Decision on cooldown — remaining " +
                    MessageUtils.formatTime(remaining));
        }

        if (!plugin.getTreasuryManager().canAfford(nation, type.getCost())) {
            return Result.fail("Treasury of " + nation.getName() + " not enough. Needs $" +
                    MessageUtils.formatNumber(type.getCost()) + ".");
        }

        // Tarik biaya
        plugin.getTreasuryManager().withdraw(nation, TransactionType.EXECUTIVE_ORDER,
                type.getCost(), "Decision: " + type.getDisplayName(), player.getUniqueId());

        // Set cooldown
        cg.getDecisionCooldowns().computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(type.name(), System.currentTimeMillis());

        // Dispatch handler
        Result handlerResult = dispatchDecision(nation, cg, player, type);

        if (handlerResult.isSuccess()) {
            cg.addOrderHistory(type.getEnglishName() + " (Minister - " + type.getPosition().getDisplayName().replace("Minister of ", "") + ")");
        }

        plugin.getDataManager().saveNations();
        return handlerResult;
    }

    private Result dispatchDecision(Nation nation, CommunistGovernment cg,
                                    Player player, CommunistDecisionType type) {
        return switch (type) {
            // --- PROPAGANDA ---
            case PROP_GLOBAL_BROADCAST    -> doGlobalBroadcast(nation, player);
            case PROP_NATIONAL_BROADCAST  -> doNationalBroadcast(nation, player);
            case PROP_LEADER_GLORIFICATION-> doLeaderGlorification(nation, cg, type);
            case PROP_MEDIA_CENSORSHIP    -> doMediaCensorship(nation, cg, type);
            case PROP_MOBILIZATION        -> doMobilization(nation, player);
            // --- DEFENSE ---
            case DEF_DECLARE_WAR          -> doDeclareWar(nation);
            case DEF_MILITARY_DRAFT       -> doMilitaryDraft(nation);
            case DEF_DEFENSE_PROTOCOL     -> doDefenseProtocol(nation, cg, type);
            case DEF_OFFENSE_PROTOCOL     -> doOffenseProtocol(nation, cg, type);
            case DEF_MILITARY_EMERGENCY   -> doMilitaryEmergency(nation, cg, type);
            // --- TREASURY ---
            case TRE_DISTRIBUTION_PROGRAM -> doDistributionProgram(nation, cg);
            case TRE_ECONOMIC_STIMULUS    -> doEconomicStimulus(nation);
            case TRE_EDUCATION_PROGRAM    -> doEducationProgram(nation);
            case TRE_TAX_INTENSIFICATION  -> doTaxIntensification(nation, cg);
            case TRE_MARKET_EVENT         -> doMarketEvent(nation, cg, type);
            // --- HEALTH ---
            case HEA_QUARANTINE_PROTOCOL  -> doQuarantineProtocol(nation, cg, type);
            case HEA_FIELD_MEDICINE       -> doFieldMedicine(nation, cg, type);
            case HEA_VACCINATION_DRIVE    -> doVaccinationDrive(nation, cg, type);
            case HEA_EMERGENCY_RATIONS    -> doEmergencyRations(nation);
            case HEA_PLAGUE               -> doPlague(nation, cg, type);
        };
    }

    // ============= PROPAGANDA HANDLERS =============

    private Result doGlobalBroadcast(Nation nation, Player issuer) {
        // Default message if arguments are not provided via separate command.
        // For simplicity, broadcast generic message; player can use
        // /nc broadcast <message> in the next Phase for custom message.
        Bukkit.broadcastMessage("§c§l[GLOBAL BROADCAST — " + nation.getName() +
                "§c§l] §c" + issuer.getName() + " broadcasting state message.");
        return Result.ok("Global Broadcast sent to the entire server.");
    }

    private Result doNationalBroadcast(Nation nation, Player issuer) {
        broadcastToNation(nation, "<gold>📢 <yellow>NATIONAL BROADCAST OF " + nation.getName() +
                "</yellow></gold>");
        broadcastToNation(nation, "<gray>Official message from " + issuer.getName() +
                ": <yellow>Stay loyal to the Party and the Secretary General!</yellow>");
        return Result.ok("National Broadcast sent to all nation members.");
    }

    private Result doLeaderGlorification(Nation nation, CommunistGovernment cg,
                                         CommunistDecisionType type) {
        cg.setGlorificationUntil(System.currentTimeMillis() + type.getDurationMillis());
        // Apply Strength I + Resistance I for 30 minutes to all online Party members
        int durationTicks = (int) (type.getDurationMillis() / 50);
        int applied = 0;
        for (UUID partyUUID : cg.getPartyMembers()) {
            Player p = Bukkit.getPlayer(partyUUID);
            if (p == null || !p.isOnline()) continue;
            p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, durationTicks, 0, false, true, true));
            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, durationTicks, 0, false, true, true));
            MessageUtils.send(p, "<gold>🚩 Leader Glorification: <yellow>Strength I + Resistance I 30 minutes.</yellow>");
            applied++;
        }
        broadcastToNation(nation, "<gold>🚩 Leader Glorification active! </gold><gray>" +
                applied + " Party members received buffs.</gray>");
        return Result.ok("Leader Glorification active for " + applied + " Party members.");
    }

    private Result doMediaCensorship(Nation nation, CommunistGovernment cg,
                                     CommunistDecisionType type) {
        cg.setSensorMediaUntil(System.currentTimeMillis() + type.getDurationMillis());
        cg.setCensorshipReplacement("§c[CENSORED BY MINISTRY OF PROPAGANDA]");
        cg.getCensorshipUsedOn().clear();
        broadcastToNation(nation, "<dark_red>📢 Media Censorship active!</dark_red> " +
                "<gray>Each target member will receive 1x censorship on global chat.</gray>");
        return Result.ok("Media Censorship active for 24 hours.");
    }

    private Result doMobilization(Nation nation, Player issuer) {
        // Teleport all online members to minister location
        int teleported = 0;
        for (UUID memberUUID : nation.getMembers().keySet()) {
            if (memberUUID.equals(issuer.getUniqueId())) continue;
            Player p = Bukkit.getPlayer(memberUUID);
            if (p == null || !p.isOnline()) continue;
            p.teleport(issuer.getLocation());
            MessageUtils.send(p, "<red>📢 PROPAGANDA MOBILIZATION!</red> " +
                    "<gray>You are called to the Ministry of Propaganda's location.</gray>");
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.5f);
            teleported++;
        }
        broadcastToNation(nation, "<red>📢 Mobilization: <gold>" + teleported +
                "</gold> members called to minister's location.");
        return Result.ok("Mobilization: " + teleported + " members gathered.");
    }

    // ============= DEFENSE HANDLERS =============

    private Result doDeclareWar(Nation nation) {
        broadcastToNation(nation, "<dark_red>⚔ <bold>WAR DECLARATION!</bold>");
        broadcastToNation(nation, "<gray>Full war system will be available in Phase 5 (Diplomacy).</gray>");
        return Result.ok("War declaration recorded (Phase 5 placeholder).");
    }

    private Result doMilitaryDraft(Nation nation) {
        // Give basic war equipment (iron set + sword + bow + arrow)
        int draftedCount = 0;
        for (UUID memberUUID : nation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(memberUUID);
            if (p == null || !p.isOnline()) continue;
            ItemStack[] kit = {
                    new ItemStack(Material.IRON_HELMET),
                    new ItemStack(Material.IRON_CHESTPLATE),
                    new ItemStack(Material.IRON_LEGGINGS),
                    new ItemStack(Material.IRON_BOOTS),
                    new ItemStack(Material.IRON_SWORD),
                    new ItemStack(Material.BOW),
                    new ItemStack(Material.ARROW, 32),
                    new ItemStack(Material.GOLDEN_APPLE, 3)
            };
            for (ItemStack item : kit) {
                var leftover = p.getInventory().addItem(item);
                for (var dropItem : leftover.values()) {
                    p.getWorld().dropItemNaturally(p.getLocation(), dropItem);
                }
            }
            MessageUtils.send(p, "<red>⚔ Military Draft: <gold>war equipment</gold> distributed!");
            draftedCount++;
        }
        broadcastToNation(nation, "<red>⚔ Military Draft: <gold>" + draftedCount +
                "</gold> members equipped with war gear.");
        return Result.ok("Military Draft: " + draftedCount + " members equipped.");
    }

    private Result doDefenseProtocol(Nation nation, CommunistGovernment cg,
                                     CommunistDecisionType type) {
        cg.setDefenseProtocolUntil(System.currentTimeMillis() + type.getDurationMillis());
        int durationTicks = (int) (type.getDurationMillis() / 50);
        int affected = 0;
        for (UUID memberUUID : nation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(memberUUID);
            if (p == null || !p.isOnline()) continue;
            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, durationTicks, 0, false, true, true));
            MessageUtils.send(p, "<aqua>🛡 Defense Protocol: <gray>Resistance I for 30 minutes.</gray>");
            affected++;
        }
        broadcastToNation(nation, "<aqua>🛡 Defense Protocol active!</aqua> <gray>(-20% damage taken, " +
                affected + " online members).</gray>");
        return Result.ok("Defense Protocol active for " + affected + " members.");
    }

    private Result doOffenseProtocol(Nation nation, CommunistGovernment cg,
                                     CommunistDecisionType type) {
        cg.setOffenseProtocolUntil(System.currentTimeMillis() + type.getDurationMillis());
        int durationTicks = (int) (type.getDurationMillis() / 50);
        int affected = 0;
        for (UUID memberUUID : nation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(memberUUID);
            if (p == null || !p.isOnline()) continue;
            p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, durationTicks, 0, false, true, true));
            MessageUtils.send(p, "<red>⚔ Offense Protocol: <gray>Strength I for 30 minutes.</gray>");
            affected++;
        }
        broadcastToNation(nation, "<red>⚔ Offense Protocol active!</red> <gray>(+20% damage dealt, " +
                affected + " online members).</gray>");
        return Result.ok("Offense Protocol active for " + affected + " members.");
    }

    private Result doMilitaryEmergency(Nation nation, CommunistGovernment cg,
                                       CommunistDecisionType type) {
        cg.setMilitaryEmergencyUntil(System.currentTimeMillis() + type.getDurationMillis());
        if (!nation.hasCapital()) {
            return Result.fail("Your nation does not have a capital yet — effect cannot target territory.");
        }
        // Reveal & debuff non-members in territory (glow + Weakness II)
        int durationTicks = (int) (type.getDurationMillis() / 50);
        int targets = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (nation.isMember(p.getUniqueId())) continue;
            // Cek di teritori nation ini
            id.nationcore.models.Nation atLoc = plugin.getTerritoryManager().getNationAt(p.getLocation());
            if (atLoc == null || !atLoc.getId().equals(nation.getId())) continue;
            p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, durationTicks, 0, false, true, true));
            p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, durationTicks, 1, false, true, true));
            targets++;
        }
        broadcastToNation(nation, "<red>🚨 Military Emergency!</red> <gray>" + targets +
                " intruders detected in territory — glow + Weakness II for 5 minutes.</gray>");
        return Result.ok("Military Emergency: " + targets + " intruders flagged.");
    }

    // ============= TREASURY HANDLERS =============

    private Result doDistributionProgram(Nation nation, CommunistGovernment cg) {
        cg.setDistributionProgramPhasesLeft(3);
        broadcastToNation(nation, "<green>💵 Distribution Program active!</green> <gray>Communist " +
                "tax exempted for the next 3 phases.</gray>");
        return Result.ok("Distribution Program: tax-free 3 phases.");
    }

    private Result doEconomicStimulus(Nation nation) {
        double amount = 10_000;
        int recipients = 0;
        for (UUID memberUUID : nation.getMembers().keySet()) {
            plugin.getVaultHook().deposit(memberUUID, amount);
            Player p = Bukkit.getPlayer(memberUUID);
            if (p != null) {
                MessageUtils.send(p, "<green>💵 Economic Stimulus: <gold>+$" +
                        MessageUtils.formatNumber(amount) + "</gold>!");
            }
            recipients++;
        }
        broadcastToNation(nation, "<green>💵 Economic Stimulus distributed to " +
                recipients + " nation members ($" + MessageUtils.formatNumber(amount) + " each).");
        return Result.ok("Economic Stimulus: " + recipients + " members receive $" +
                MessageUtils.formatNumber(amount) + ".");
    }

    private Result doEducationProgram(Nation nation) {
        int xpLevels = 10;
        int recipients = 0;
        for (UUID memberUUID : nation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(memberUUID);
            if (p == null || !p.isOnline()) continue;
            p.giveExpLevels(xpLevels);
            MessageUtils.send(p, "<aqua>📚 Education Program: <gold>+" + xpLevels + " XP levels</gold>!");
            recipients++;
        }
        broadcastToNation(nation, "<aqua>📚 Education Program: <gold>" + recipients +
                "</gold> online members receive +" + xpLevels + " XP levels.");
        return Result.ok("Education Program: " + recipients + " members receive +" + xpLevels + " XP levels.");
    }

    private Result doTaxIntensification(Nation nation, CommunistGovernment cg) {
        cg.setTaxIntensificationPhasesLeft(3);
        broadcastToNation(nation, "<dark_red>💰 Tax Intensification active!</dark_red> <gray>Communist " +
                "tax becomes 200% for the next 3 phases.</gray>");
        return Result.ok("Tax Intensification: 200% tax for 3 phases.");
    }

    private Result doMarketEvent(Nation nation, CommunistGovernment cg,
                                 CommunistDecisionType type) {
        cg.setMarketEventUntil(System.currentTimeMillis() + type.getDurationMillis());
        broadcastToNation(nation, "<gold>🛒 Market Event active!</gold> <gray>Every villager " +
                "transaction gives members +$25 for 30 minutes.</gray>");
        return Result.ok("Market Event active for 30 minutes.");
    }

    // ============= HEALTH HANDLERS =============

    private Result doQuarantineProtocol(Nation nation, CommunistGovernment cg,
                                        CommunistDecisionType type) {
        cg.setQuarantineUntil(System.currentTimeMillis() + type.getDurationMillis());
        broadcastToNation(nation, "<aqua>💉 Quarantine Protocol active!</aqua> <gray>Non-members " +
                "cannot enter territory for 10 minutes.</gray>");
        return Result.ok("Quarantine Protocol active for 10 minutes.");
    }

    private Result doFieldMedicine(Nation nation, CommunistGovernment cg,
                                   CommunistDecisionType type) {
        if (cg.isFieldMedicineOnCooldown()) {
            return Result.fail("Field Medicine on cooldown — remaining " +
                    MessageUtils.formatTime(cg.getFieldMedicineCooldownUntil() - System.currentTimeMillis()));
        }
        long cooldownMs = 2L * 60 * 60 * 1000; // 2 hours
        cg.setFieldMedicineCooldownUntil(System.currentTimeMillis() + cooldownMs);
        int durationTicks = (int) (type.getDurationMillis() / 50);
        int affected = 0;
        for (UUID memberUUID : nation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(memberUUID);
            if (p == null || !p.isOnline()) continue;
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, durationTicks, 1, false, true, true));
            MessageUtils.send(p, "<green>💉 Field Medicine: <gray>Regeneration II for 5 minutes.</gray>");
            affected++;
        }
        broadcastToNation(nation, "<green>💉 Field Medicine!</green> <gray>" + affected +
                " online members receive Regeneration II for 5 minutes.</gray>");
        return Result.ok("Field Medicine active for " + affected + " members.");
    }

    private Result doVaccinationDrive(Nation nation, CommunistGovernment cg,
                                      CommunistDecisionType type) {
        cg.setVaccinationUntil(System.currentTimeMillis() + type.getDurationMillis());
        broadcastToNation(nation, "<green>💉 Vaccination Drive aktif!</green> <gray>Anggota kebal " +
                "poison & wither selama 1 jam.</gray>");
        return Result.ok("Vaccination Drive aktif selama 1 jam.");
    }

    private Result doEmergencyRations(Nation nation) {
        int distributed = 0;
        for (UUID memberUUID : nation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(memberUUID);
            if (p == null || !p.isOnline()) continue;
            // Hunger bar 0..20; <50% means <10
            if (p.getFoodLevel() >= 10) continue;
            ItemStack bread = new ItemStack(Material.BREAD, 8);
            var leftover = p.getInventory().addItem(bread);
            for (var dropItem : leftover.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), dropItem);
            }
            MessageUtils.send(p, "<gold>🍞 Emergency Rations: <gray>8 bread for you.</gray>");
            distributed++;
        }
        broadcastToNation(nation, "<gold>🍞 Emergency Rations distributed to " +
                distributed + " members with low hunger.");
        return Result.ok("Emergency Rations: " + distributed + " members receive food.");
    }

    private Result doPlague(Nation nation, CommunistGovernment cg,
                            CommunistDecisionType type) {
        cg.setPlagueUntil(System.currentTimeMillis() + type.getDurationMillis());
        broadcastToNation(nation, "<dark_red>☠ Plague active!</dark_red> <gray>Enemies entering territory " +
                "will receive Weakness II + Hunger for 30 seconds. Active for 10 minutes.</gray>");
        return Result.ok("Plague active for 10 minutes.");
    }

    // ==========================================================
    // Salary scheduler (10,000 vault/week to Politburo)
    // ==========================================================

    /**
     * Pay weekly salary to all Politburo from nation treasury. Called
     * scheduler every 1 real day; tracking lastDailyReward in each
     * PolitburoMember determines if 7 days have passed since the last payment.
     */
    public void checkPolitburoSalaries() {
        long weekMs = 7L * 24 * 60 * 60 * 1000;
        double weeklySalary = plugin.getConfig().getDouble("nation.communist.minister-weekly-salary", 10_000);

        for (Nation nation : plugin.getNationManager().getAllNations()) {
            if (nation.getType() != GovernmentType.COMMUNIST) continue;
            CommunistGovernment cg = nation.getCommunistGovernment();
            if (cg == null) continue;

            for (PolitburoMember member : cg.getPolitburo().values()) {
                long elapsed = System.currentTimeMillis() - member.getLastDailyReward();
                if (elapsed < weekMs && member.getLastDailyReward() > 0) continue;

                if (!plugin.getTreasuryManager().canAfford(nation, weeklySalary)) continue;
                plugin.getTreasuryManager().withdraw(nation, TransactionType.CABINET_SALARY,
                        weeklySalary, "Politbiro weekly salary — " + member.getPosition().name(),
                        member.getUuid());
                plugin.getVaultHook().deposit(member.getUuid(), weeklySalary);
                member.setLastDailyReward(System.currentTimeMillis());

                Player p = Bukkit.getPlayer(member.getUuid());
                if (p != null) {
                    MessageUtils.send(p, "<gold>💰 Politburo Salary: <green>+$" +
                            MessageUtils.formatNumber(weeklySalary) + "</green> from treasury of " +
                            nation.getName() + ".");
                }
            }
            plugin.getDataManager().saveNations();
        }
    }

    public long getSalaryCooldown(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null || nation.getType() != GovernmentType.COMMUNIST) return -1;
        CommunistGovernment cg = nation.getCommunistGovernment();
        if (cg == null) return -1;

        if (cg.hasSecretaryGeneral() && cg.getSecretaryGeneralUUID().equals(player.getUniqueId())) {
            long now = System.currentTimeMillis();
            long dayMillis = 24 * 60 * 60 * 1000L;
            long diff = now - cg.getLastDailyReward();
            return (diff >= dayMillis) ? 0 : (dayMillis - diff);
        }

        CommunistGovernment.PolitburoMember member = cg.getPolitburoMemberByUUID(player.getUniqueId());
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
        if (nation == null || nation.getType() != GovernmentType.COMMUNIST) {
            MessageUtils.send(player, "<red>You are not in a Communist nation!</red>");
            return;
        }
        CommunistGovernment cg = nation.getCommunistGovernment();
        if (cg == null) {
            MessageUtils.send(player, "<red>No active Communist government!</red>");
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

        if (cg.hasSecretaryGeneral() && cg.getSecretaryGeneralUUID().equals(player.getUniqueId())) {
            double vaultPoints = plugin.getConfig().getDouble("president.daily-rewards.vault-points", 50000);
            int diamondBlocks = plugin.getConfig().getInt("president.daily-rewards.diamond-blocks", 5);
            int netheriteIngots = plugin.getConfig().getInt("president.daily-rewards.netherite-ingots", 3);
            int goldenApples = plugin.getConfig().getInt("president.daily-rewards.enchanted-golden-apples", 10);

            if (plugin.getTreasuryManager().canAfford(nation, vaultPoints)) {
                plugin.getTreasuryManager().withdraw(nation, TransactionType.PRESIDENT_SALARY, vaultPoints,
                        "Daily salary for Secretary General " + player.getName(), player.getUniqueId());
                plugin.getVaultHook().deposit(player.getUniqueId(), vaultPoints);
                cg.addSubsidyPayout(vaultPoints); // Track payout
                cg.setLastDailyReward(now);
                MessageUtils.send(player, "managers.government.daily_reward");
            } else {
                MessageUtils.send(player, "<red>The Treasury cannot afford your daily salary!</red>");
                return;
            }

            var leftover1 = player.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.DIAMOND_BLOCK, diamondBlocks));
            for (var item : leftover1.values()) { player.getWorld().dropItemNaturally(player.getLocation(), item); }
            var leftover2 = player.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.NETHERITE_INGOT, netheriteIngots));
            for (var item : leftover2.values()) { player.getWorld().dropItemNaturally(player.getLocation(), item); }
            var leftover3 = player.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.ENCHANTED_GOLDEN_APPLE, goldenApples));
            for (var item : leftover3.values()) { player.getWorld().dropItemNaturally(player.getLocation(), item); }

            MessageUtils.playSound(player, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP);
            plugin.getDataManager().saveNations();
            return;
        }

        CommunistGovernment.PolitburoMember member = cg.getPolitburoMemberByUUID(player.getUniqueId());
        if (member != null) {
            CommunistGovernment.PolitburoPosition position = member.getPosition();
            String configPath = switch (position) {
                case DEFENSE -> "cabinet.defense.daily-vault";
                case TREASURY -> "cabinet.treasury-minister.daily-vault";
                default -> "cabinet.daily-salary";
            };

            double vaultPoints = plugin.getConfig().getDouble(configPath, 30000);
            double salary = plugin.getConfig().getDouble("cabinet.daily-salary", 20000);
            double totalPay = vaultPoints + salary;

            if (plugin.getTreasuryManager().canAfford(nation, totalPay)) {
                plugin.getTreasuryManager().withdraw(nation, TransactionType.CABINET_SALARY, totalPay,
                        "Daily salary for " + position.getDisplayName() + " " + player.getName(), player.getUniqueId());
                plugin.getVaultHook().deposit(player.getUniqueId(), totalPay);
                cg.addSubsidyPayout(totalPay); // Track payout
                member.setLastDailyReward(now);
                MessageUtils.send(player, "managers.government.cabinet_daily");
            } else {
                MessageUtils.send(player, "<red>The Treasury cannot afford your daily salary!</red>");
                return;
            }

            switch (position) {
                case DEFENSE -> {
                    var leftoverA = player.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.DIAMOND, 3));
                    for (var item : leftoverA.values()) { player.getWorld().dropItemNaturally(player.getLocation(), item); }
                    var leftoverB = player.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.GOLDEN_APPLE, 5));
                    for (var item : leftoverB.values()) { player.getWorld().dropItemNaturally(player.getLocation(), item); }
                }
                case TREASURY -> {
                    var leftoverC = player.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.EMERALD_BLOCK, 10));
                    for (var item : leftoverC.values()) { player.getWorld().dropItemNaturally(player.getLocation(), item); }
                }
                default -> {}
            }

            MessageUtils.playSound(player, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP);
            plugin.getDataManager().saveNations();
        }
    }
}
