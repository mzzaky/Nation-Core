package id.nationcore.managers;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;
import id.nationcore.models.Government;
import id.nationcore.models.Government.CabinetMember;
import id.nationcore.models.Government.CabinetPosition;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.Nation;
import id.nationcore.models.PlayerData;
import id.nationcore.models.PresidentHistory.PresidentRecord;
import id.nationcore.models.Treasury.TransactionType;
import id.nationcore.utils.MessageUtils;

public class GovernmentManager {

    private final NationCore plugin;

    public GovernmentManager(NationCore plugin) {
        this.plugin = plugin;
    }

    public Government getGovernment() {
        return plugin.getDataManager().getGovernment();
    }

    /**
     * Resolve Government untuk konteks pemain — prioritas:
     *   1. Bila pemain di nation REPUBLIC → republicGovernment milik nation tsb.
     *   2. Bila tidak → Government global legacy.
     *
     * Mengembalikan null bila pemain berada di nation non-REPUBLIC (mis. KOMUNIS),
     * yang akan diserahkan ke handler komunis nantinya.
     */
    public Government getGovernment(UUID playerUUID) {
        Nation n = plugin.getNationManager().getNationOf(playerUUID);
        if (n == null) return getGovernment();
        return n.getType() == GovernmentType.REPUBLIC ? n.getRepublicGovernment() : null;
    }

    public Government getGovernment(Nation nation) {
        if (nation == null) return getGovernment();
        return nation.getType() == GovernmentType.REPUBLIC ? nation.getRepublicGovernment() : null;
    }

    public boolean isPresident(UUID uuid) {
        Government gov = getGovernment(uuid);
        return gov != null && gov.hasPresident() && gov.getPresidentUUID().equals(uuid);
    }

    public boolean isPresident(Nation nation, UUID uuid) {
        Government gov = getGovernment(nation);
        return gov != null && gov.hasPresident() && gov.getPresidentUUID().equals(uuid);
    }

    public boolean isCabinetMember(UUID uuid) {
        Government gov = getGovernment(uuid);
        return gov != null && gov.getCabinetMemberByUUID(uuid) != null;
    }

    public boolean isCabinetMember(Nation nation, UUID uuid) {
        Government gov = getGovernment(nation);
        return gov != null && gov.getCabinetMemberByUUID(uuid) != null;
    }

    public CabinetPosition getCabinetPosition(UUID uuid) {
        Government gov = getGovernment(uuid);
        return gov != null ? gov.getPositionByUUID(uuid) : null;
    }

    public CabinetPosition getCabinetPosition(Nation nation, UUID uuid) {
        Government gov = getGovernment(nation);
        return gov != null ? gov.getPositionByUUID(uuid) : null;
    }

    public void setPresident(UUID uuid, String name, boolean isNewTerm) {
        Government gov = getGovernment();
        int maxTerms = plugin.getConfig().getInt("president.max-consecutive-terms", 2);
        int cooldownTerms = plugin.getConfig().getInt("president.cooldown-after-max-terms", 1);

        // Handle outgoing president - check if they reached max terms and set cooldown
        if (gov.hasPresident() && isNewTerm) {
            UUID outgoingPresident = gov.getPresidentUUID();
            if (outgoingPresident != null && gov.getConsecutiveTerms() >= maxTerms) {
                PlayerData outgoingData = plugin.getDataManager().getOrCreatePlayerData(outgoingPresident,
                        gov.getPresidentName());
                outgoingData.setPresidentCooldownTerms(cooldownTerms);
            }
        }

        // End previous president's term if exists
        if (gov.hasPresident()) {
            endPresidency("TERM_END");
        }

        // Decrease cooldown for all players with active cooldown (new term started)
        if (isNewTerm) {
            for (PlayerData data : plugin.getDataManager().getAllPlayerData()) {
                if (data.getPresidentCooldownTerms() > 0) {
                    data.decrementPresidentCooldown();
                }
            }
        }

        // Set new president
        gov.setPresidentUUID(uuid);
        gov.setPresidentName(name);
        // Important: Update nation-level leader UUID for systems that track leadership generically (like Research)
        Nation n = plugin.getNationManager().getNationOf(uuid);
        if (n != null && n.getType() == GovernmentType.REPUBLIC) {
            n.setLeaderUUID(uuid);
            n.setLeaderName(name);
        }
        gov.setTermStartTime(System.currentTimeMillis());
        gov.setLastPresidentActivity(System.currentTimeMillis());
        gov.setLastDailyReward(0);
        gov.setTotalSalaryPayouts(0.0); // Reset total salary payouts
        gov.getApprovalRatings().clear();
        gov.getCabinet().clear();

        // Track consecutive terms for new president
        if (isNewTerm) {
            PresidentRecord lastRecord = plugin.getDataManager().getPresidentHistory().getLatestRecord();
            if (lastRecord != null && lastRecord.getUuid().equals(uuid)) {
                gov.setConsecutiveTerms(gov.getConsecutiveTerms() + 1);
            } else {
                gov.setConsecutiveTerms(1);
            }
        }

        // Update player data
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(uuid, name);
        data.setTimesServedAsPresident(data.getTimesServedAsPresident() + 1);

        // Reset term counters
        plugin.getDataManager().setLastExecutiveOrderTime(0);
        plugin.getDataManager().setGamesThisTerm(0);
        // Create history record
        PresidentRecord record = new PresidentRecord(uuid, name, System.currentTimeMillis());
        plugin.getDataManager().getPresidentHistory().addRecord(record);

        // Broadcast inauguration
        MessageUtils.broadcastAnnouncement("PRESIDENTIAL INAUGURATION",
                "<gold>" + name + "</gold> <yellow>has been inaugurated as the new President!</yellow>");
        MessageUtils.broadcastTitle("<gold>🏛️ NEW PRESIDENT 🏛️</gold>",
                "<yellow>" + name + "</yellow>", 20, 100, 20);
        MessageUtils.broadcastSound(Sound.UI_TOAST_CHALLENGE_COMPLETE);


    }

    public void endPresidency(String reason) {
        Government gov = getGovernment();
        if (!gov.hasPresident())
            return;

        // Update history record
        PresidentRecord record = plugin.getDataManager().getPresidentHistory().getLatestRecord();
        if (record != null && record.getUuid().equals(gov.getPresidentUUID())) {
            record.setTermEnd(System.currentTimeMillis());
            record.setFinalApprovalRating(gov.getCurrentApprovalRating());
            record.setEndReason(reason);

            // Save cabinet members to record
            for (var entry : gov.getCabinet().entrySet()) {
                record.getCabinetMembers().put(entry.getKey(), entry.getValue().getName());
            }
        }



        // Remove buffs from cabinet
        for (CabinetMember member : gov.getCabinet().values()) {
            Player cabinetPlayer = Bukkit.getPlayer(member.getUuid());
            if (cabinetPlayer != null) {
                plugin.getBuffManager().removeCabinetBuffs(cabinetPlayer);
            }
        }

        // Clear government
        String presidentName = gov.getPresidentName();
        gov.setPresidentUUID(null);
        gov.setPresidentName(null);
        gov.getCabinet().clear();

        MessageUtils.broadcast("<yellow>President <gold>" + presidentName +
                "</gold>'s term has ended. Reason: <white>" + reason + "</white></yellow>");
    }

    public void appointCabinet(CabinetPosition position, UUID uuid, String name) {
        Government gov = getGovernment();

        // Remove existing member from position
        CabinetMember existing = gov.getCabinetMemberObject(position);
        if (existing != null) {
            Player existingPlayer = Bukkit.getPlayer(existing.getUuid());
            if (existingPlayer != null) {
                plugin.getBuffManager().removeCabinetBuffs(existingPlayer);
                MessageUtils.send(existingPlayer, "managers.government.removed");
            }
        }

        // Check if player already has a cabinet position
        CabinetPosition currentPos = gov.getPositionByUUID(uuid);
        if (currentPos != null) {
            gov.removeCabinet(currentPos);
        }

        // Appoint new member
        CabinetMember member = new CabinetMember(uuid, name, position);
        gov.appointCabinet(position, member);

        // Update player data
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(uuid, name);
        data.setTimesServedAsCabinet(data.getTimesServedAsCabinet() + 1);

        // Apply buffs if online
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            plugin.getBuffManager().applyCabinetBuffs(player, position);
            MessageUtils.send(player, "managers.government.appointed", "position", position.getDisplayName());
        }

        MessageUtils.broadcast("<yellow><gold>" + name + "</gold> has been appointed as <gold>" +
                position.getDisplayName() + "</gold>!</yellow>");
    }

    public void removeCabinet(CabinetPosition position) {
        Government gov = getGovernment();
        CabinetMember member = gov.getCabinetMemberObject(position);
        if (member == null)
            return;

        Player player = Bukkit.getPlayer(member.getUuid());
        if (player != null) {
            plugin.getBuffManager().removeCabinetBuffs(player);
            MessageUtils.send(player, "<red>You have been removed from your cabinet position.</red>");
        }

        gov.removeCabinet(position);
        MessageUtils.broadcast("<yellow><gold>" + member.getName() +
                "</gold> has been removed from the cabinet.</yellow>");
    }

    public long getSalaryCooldown(Player player) {
        Government gov = getGovernment(player.getUniqueId());
        if (gov == null) return -1;
        if (gov.hasPresident() && gov.getPresidentUUID().equals(player.getUniqueId())) {
            long now = System.currentTimeMillis();
            long dayMillis = 24 * 60 * 60 * 1000L;
            long diff = now - gov.getLastDailyReward();
            return (diff >= dayMillis) ? 0 : (dayMillis - diff);
        }

        CabinetMember member = gov.getCabinetMemberByUUID(player.getUniqueId());
        if (member != null) {
            long now = System.currentTimeMillis();
            long dayMillis = 24 * 60 * 60 * 1000L;
            long diff = now - member.getLastDailyReward();
            return (diff >= dayMillis) ? 0 : (dayMillis - diff);
        }
        return -1; // Not eligible
    }

    public void claimDailySalary(Player player) {
        Government gov = getGovernment(player.getUniqueId());
        if (gov == null) {
            MessageUtils.send(player, "<red>No active government!</red>");
            return;
        }
        if (!gov.hasPresident()) {
            MessageUtils.send(player, "<red>No active government!</red>");
            return;
        }

        long cooldown = getSalaryCooldown(player);
        if (cooldown > 0) {
            MessageUtils.send(player,
                    "<red>You can claim your salary in: " + MessageUtils.formatTime(cooldown) + "</red>");
            return;
        } else if (cooldown == -1) {
            MessageUtils.send(player, "<red>You are not eligible for a government salary!</red>");
            return;
        }

        long now = System.currentTimeMillis();

        // President daily rewards
        if (gov.getPresidentUUID().equals(player.getUniqueId())) {
            givePresidentDailyRewards(player, gov);
            gov.setLastDailyReward(now);
            return;
        }

        // Cabinet daily rewards
        CabinetMember member = gov.getCabinetMemberByUUID(player.getUniqueId());
        if (member != null) {
            giveCabinetDailyRewards(player, member.getPosition(), gov);
            member.setLastDailyReward(now);
        }
    }

    /*
     * Deprecated: Moved to manual claimDailySalary
     * public void checkDailyRewards() {
     * Government gov = getGovernment();
     * if (!gov.hasPresident()) return;
     * 
     * long now = System.currentTimeMillis();
     * long dayMillis = 24 * 60 * 60 * 1000L;
     * 
     * // President daily rewards
     * if (now - gov.getLastDailyReward() >= dayMillis) {
     * Player president = Bukkit.getPlayer(gov.getPresidentUUID());
     * if (president != null) {
     * givePresidentDailyRewards(president);
     * gov.setLastDailyReward(now);
     * }
     * }
     * 
     * // Cabinet daily rewards and salary
     * for (CabinetMember member : gov.getCabinet().values()) {
     * if (now - member.getLastDailyReward() >= dayMillis) {
     * Player cabinetPlayer = Bukkit.getPlayer(member.getUuid());
     * if (cabinetPlayer != null) {
     * giveCabinetDailyRewards(cabinetPlayer, member.getPosition());
     * member.setLastDailyReward(now);
     * }
     * 
     * // Pay cabinet salary from treasury
     * double salary = plugin.getConfig().getDouble("cabinet.daily-salary", 20000);
     * if (plugin.getTreasuryManager().canAfford(salary)) {
     * plugin.getTreasuryManager().withdraw(TransactionType.CABINET_SALARY, salary,
     * "Daily salary for " + member.getPosition().getDisplayName(),
     * member.getUuid());
     * plugin.getVaultHook().deposit(member.getUuid(), salary);
     * }
     * }
     * }
     * }
     */

    private void givePresidentDailyRewards(Player president, Government gov) {
        double vaultPoints = plugin.getConfig().getDouble("president.daily-rewards.vault-points", 50000);
        int diamondBlocks = plugin.getConfig().getInt("president.daily-rewards.diamond-blocks", 5);
        int netheriteIngots = plugin.getConfig().getInt("president.daily-rewards.netherite-ingots", 3);
        int goldenApples = plugin.getConfig().getInt("president.daily-rewards.enchanted-golden-apples", 10);

        Nation nation = plugin.getNationManager().getNationOf(president.getUniqueId());

        // Salary payment from Treasury
        if (plugin.getTreasuryManager().canAfford(nation, vaultPoints)) {
            plugin.getTreasuryManager().withdraw(nation, TransactionType.PRESIDENT_SALARY, vaultPoints,
                    "Daily salary for President " + president.getName(), president.getUniqueId());
            plugin.getVaultHook().deposit(president.getUniqueId(), vaultPoints);
            gov.addSalaryPayout(vaultPoints); // Track payout
            MessageUtils.send(president, "managers.government.daily_reward");
        } else {
            MessageUtils.send(president, "<red>The Treasury cannot afford your daily salary!</red>");
        }

        president.getInventory().addItem(new ItemStack(Material.DIAMOND_BLOCK, diamondBlocks));
        president.getInventory().addItem(new ItemStack(Material.NETHERITE_INGOT, netheriteIngots));
        president.getInventory().addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, goldenApples));

        MessageUtils.playSound(president, Sound.ENTITY_PLAYER_LEVELUP);
    }

    private void giveCabinetDailyRewards(Player player, CabinetPosition position, Government gov) {
        String configPath = switch (position) {
            case DEFENSE -> "cabinet.defense.daily-vault";
            case TREASURY -> "cabinet.treasury.daily-vault";
            case HEALTH -> "cabinet.health.daily-vault";
            default -> "cabinet.daily-salary";
        };

        YamlConfiguration nationConfig = plugin.getNationConfig(GovernmentType.REPUBLIC);
        double vaultPoints = nationConfig != null ? nationConfig.getDouble(configPath, 30000) : 30000;
        double salary = nationConfig != null ? nationConfig.getDouble("cabinet.daily-salary", 20000) : 20000;
        double totalPay = vaultPoints + salary;

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());

        // Pay cabinet salary from treasury
        if (plugin.getTreasuryManager().canAfford(nation, totalPay)) {
            plugin.getTreasuryManager().withdraw(nation, TransactionType.CABINET_SALARY, totalPay,
                    "Daily salary for " + position.getDisplayName(), player.getUniqueId());
            plugin.getVaultHook().deposit(player.getUniqueId(), totalPay);
            gov.addSalaryPayout(totalPay); // Track payout
            MessageUtils.send(player, "managers.government.cabinet_daily");
        } else {
            MessageUtils.send(player, "<red>The Treasury cannot afford your daily salary!</red>");
        }

        // Additional rewards based on position
        switch (position) {
            case DEFENSE -> {
                player.getInventory().addItem(new ItemStack(Material.DIAMOND, 3));
                player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 5));
            }
            case TREASURY -> player.getInventory().addItem(new ItemStack(Material.EMERALD_BLOCK, 10));
        }
    }



    public long getTermRemainingTime() {
        Government gov = getGovernment();
        if (!gov.hasPresident())
            return 0;

        int termLengthDays = plugin.getConfig().getInt("president.term-length-days", 30);
        long termLengthMillis = termLengthDays * 24L * 60 * 60 * 1000;
        long termEnd = gov.getTermStartTime() + termLengthMillis;

        return Math.max(0, termEnd - System.currentTimeMillis());
    }

    public boolean canRunForPresident(UUID uuid) {
        Government gov = getGovernment();
        PlayerData playerData = plugin.getDataManager().getPlayerData(uuid);

        // Check if player has an active cooldown
        if (playerData != null && playerData.getPresidentCooldownTerms() > 0) {
            return false;
        }

        // Check max consecutive terms
        int maxTerms = plugin.getConfig().getInt("president.max-consecutive-terms", 2);
        if (gov.getPresidentUUID() != null && gov.getPresidentUUID().equals(uuid)
                && gov.getConsecutiveTerms() >= maxTerms) {
            return false;
        }

        return true;
    }

    public int getRemainingCooldownTerms(UUID uuid) {
        PlayerData playerData = plugin.getDataManager().getPlayerData(uuid);
        return playerData != null ? playerData.getPresidentCooldownTerms() : 0;
    }

    public void rateApproval(UUID voterUUID, int rating) {
        Government gov = getGovernment();
        if (!gov.hasPresident())
            return;

        // Find existing rating
        Government.ApprovalRating existingRating = null;
        for (Government.ApprovalRating r : gov.getApprovalRatings()) {
            if (r.getVoterUUID().equals(voterUUID)) {
                existingRating = r;
                break;
            }
        }

        if (existingRating != null) {
            existingRating.setRating(rating);
        } else {
            gov.getApprovalRatings().add(new Government.ApprovalRating(voterUUID, rating));
        }

        gov.calculateApprovalRating();
    }

    public String getPresidentPrefix() {
        return "<gold>[President]</gold> ";
    }

    public String getCabinetPrefix(CabinetPosition position) {
        return position.getPrefix() + " ";
    }

    // Additional methods for NationCommand
    public void appointCabinetMember(CabinetPosition position, UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        if (name != null) {
            appointCabinet(position, uuid, name);
        }
    }

    public void removeCabinetMember(CabinetPosition position) {
        removeCabinet(position);
    }

    public void endTerm(String reason) {
        endPresidency(reason);
    }

    // ==========================================================
    // Context-aware (per-nation) overloads — Phase 2
    // ==========================================================

    /**
     * Appoint kabinet untuk nation tertentu (REPUBLIC). Tidak ada efek bila
     * nation null atau bukan REPUBLIC.
     */
    public void appointCabinet(Nation nation, CabinetPosition position, UUID uuid, String name) {
        if (nation == null || nation.getType() != GovernmentType.REPUBLIC) return;
        Government gov = nation.getRepublicGovernment();
        if (gov == null) return;

        // Penanggalan posisi yang sama bila sudah terisi
        CabinetMember existing = gov.getCabinetMemberObject(position);
        if (existing != null) {
            Player existingPlayer = Bukkit.getPlayer(existing.getUuid());
            if (existingPlayer != null) {
                plugin.getBuffManager().removeCabinetBuffs(existingPlayer);
                MessageUtils.send(existingPlayer, "managers.government.removed");
            }
        }

        // Bila pemain sudah memegang posisi lain di nation ini, copot dulu
        CabinetPosition currentPos = gov.getPositionByUUID(uuid);
        if (currentPos != null) gov.removeCabinet(currentPos);

        gov.appointCabinet(position, new CabinetMember(uuid, name, position));

        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(uuid, name);
        data.setTimesServedAsCabinet(data.getTimesServedAsCabinet() + 1);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            plugin.getBuffManager().applyCabinetBuffs(player, position);
            MessageUtils.send(player, "managers.government.appointed",
                    "position", position.getDisplayName());
        }

        broadcastToNation(nation, "<yellow><gold>" + name + "</gold> has been appointed as <gold>" +
                position.getDisplayName() + "</gold> of " + nation.getName() + "!</yellow>");
    }

    public void appointCabinetMember(Nation nation, CabinetPosition position, UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        if (name != null) appointCabinet(nation, position, uuid, name);
    }

    public void removeCabinet(Nation nation, CabinetPosition position) {
        if (nation == null || nation.getType() != GovernmentType.REPUBLIC) return;
        Government gov = nation.getRepublicGovernment();
        if (gov == null) return;

        CabinetMember member = gov.getCabinetMemberObject(position);
        if (member == null) return;

        Player player = Bukkit.getPlayer(member.getUuid());
        if (player != null) {
            plugin.getBuffManager().removeCabinetBuffs(player);
            MessageUtils.send(player, "<red>You have been removed from your cabinet position.</red>");
        }

        gov.removeCabinet(position);
        broadcastToNation(nation, "<yellow><gold>" + member.getName() +
                "</gold> has been removed from the cabinet of " + nation.getName() + ".</yellow>");
    }

    public void removeCabinetMember(Nation nation, CabinetPosition position) {
        removeCabinet(nation, position);
    }



    public long getTermRemainingTime(Nation nation) {
        Government gov = getGovernment(nation);
        if (gov == null || !gov.hasPresident()) return 0;
        int termLengthDays = plugin.getConfig().getInt("president.term-length-days", 30);
        long termLengthMillis = termLengthDays * 24L * 60 * 60 * 1000;
        long termEnd = gov.getTermStartTime() + termLengthMillis;
        return Math.max(0, termEnd - System.currentTimeMillis());
    }

    /**
     * Akhiri masa jabatan presiden suatu nation. Tidak menghapus nation —
     * hanya mengosongkan slot presiden dan kabinet.
     *
     * Catatan: pemilihan presiden baru per-nation belum diimplementasi
     * (akan menyusul di Phase 2B bersama ElectionManager refactor).
     * Sementara ini leader nation otomatis dipromosikan kembali.
     */
    public void endPresidency(Nation nation, String reason) {
        if (nation == null || nation.getType() != GovernmentType.REPUBLIC) return;
        Government gov = nation.getRepublicGovernment();
        if (gov == null || !gov.hasPresident()) return;



        for (CabinetMember member : gov.getCabinet().values()) {
            Player cabinetPlayer = Bukkit.getPlayer(member.getUuid());
            if (cabinetPlayer != null) plugin.getBuffManager().removeCabinetBuffs(cabinetPlayer);
        }

        String presidentName = gov.getPresidentName();
        gov.setPresidentUUID(null);
        gov.setPresidentName(null);
        gov.getCabinet().clear();

        broadcastToNation(nation, "<yellow>President <gold>" + presidentName +
                "</gold> of " + nation.getName() + " has stepped down. Reason: <white>" +
                reason + "</white></yellow>");
    }

    private void broadcastToNation(Nation nation, String message) {
        if (nation == null) return;
        for (UUID uuid : nation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) MessageUtils.send(p, message);
        }
    }

    /**
     * Set presiden baru untuk nation REPUBLIC tertentu, mereset cabinet,
     * dan memberi modal awal kas dari config (nation.election.term-fund atau
     * fallback treasury.starting-fund). Tidak menyentuh PresidentHistory global
     * — itu masih dilakukan oleh callsite legacy bila perlu.
     *
     * @param nation     nation REPUBLIC
     * @param uuid       UUID presiden baru
     * @param name       nama presiden baru (saat itu)
     * @param isNewTerm  true bila ini hasil pemilu baru (akan increment
     *                   consecutive terms bila pemain yang sama menang lagi).
     */
    public void setPresident(Nation nation, UUID uuid, String name, boolean isNewTerm) {
        if (nation == null || nation.getType() != GovernmentType.REPUBLIC) return;
        Government gov = nation.getRepublicGovernment();
        if (gov == null) {
            gov = new Government();
            nation.setRepublicGovernment(gov);
        }

        // Cleanup mantan presiden bila ada
        if (gov.hasPresident()) {
            Player exPresident = Bukkit.getPlayer(gov.getPresidentUUID());
            for (CabinetMember member : gov.getCabinet().values()) {
                Player cabinetPlayer = Bukkit.getPlayer(member.getUuid());
                if (cabinetPlayer != null) plugin.getBuffManager().removeCabinetBuffs(cabinetPlayer);
            }
        }

        UUID previousUUID = gov.getPresidentUUID();
        int previousTerms = gov.getConsecutiveTerms();

        gov.setPresidentUUID(uuid);
        gov.setPresidentName(name);
        // Important: Update nation-level leader UUID for systems that track leadership generically (like Research)
        nation.setLeaderUUID(uuid);
        nation.setLeaderName(name);
        gov.setTermStartTime(System.currentTimeMillis());
        gov.setLastPresidentActivity(System.currentTimeMillis());
        gov.setLastDailyReward(0);
        gov.setTotalSalaryPayouts(0.0);
        gov.getApprovalRatings().clear();
        gov.getCabinet().clear();

        if (isNewTerm) {
            if (previousUUID != null && previousUUID.equals(uuid)) {
                gov.setConsecutiveTerms(previousTerms + 1);
            } else {
                gov.setConsecutiveTerms(1);
            }
        } else if (gov.getConsecutiveTerms() <= 0) {
            gov.setConsecutiveTerms(1);
        }

        // Update PlayerData untuk presiden baru
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(uuid, name);
        data.setTimesServedAsPresident(data.getTimesServedAsPresident() + 1);
        broadcastToNation(nation, "<gold>" + name + " has been inaugurated as the new President of " +
                nation.getName() + "!</gold>");


    }


}
