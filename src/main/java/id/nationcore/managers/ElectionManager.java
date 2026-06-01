package id.nationcore.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import id.nationcore.NationCore;
import id.nationcore.models.Election;
import id.nationcore.models.Election.Candidate;
import id.nationcore.models.Election.ElectionPhase;
import id.nationcore.models.Election.Vote;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.Nation;
import id.nationcore.models.PlayerData;
import id.nationcore.utils.MessageUtils;

public class ElectionManager {

    private final NationCore plugin;

    public ElectionManager(NationCore plugin) {
        this.plugin = plugin;
    }

    public Election getElection() {
        return plugin.getDataManager().getElection();
    }

    public boolean isElectionActive() {
        return getElection().isActive();
    }

    public void startElection() {
        Election election = getElection();
        if (election.isActive())
            return;

        election.startElection();

        MessageUtils.broadcastAnnouncement("ELECTION BEGINS",
                "<yellow>A new presidential election has begun!</yellow>\n" +
                        "<gray>Registration phase: " + getPhaseDuration(ElectionPhase.REGISTRATION) + " days</gray>\n" +
                        "<green>Use </green><gold>/dc register [slogan]</gold><green> to run for president!</green>");

        MessageUtils.broadcastTitle("<gold>🗳️ ELECTION TIME 🗳️</gold>",
                "<yellow>Registration is now open!</yellow>", 20, 100, 20);
    }

    public void startEmergencyElection() {
        MessageUtils.broadcast("<red>An emergency election has been called!</red>");
        startElection();
    }

    public void checkPhaseTransitions() {
        Election election = getElection();
        if (!election.isActive()) {
            // Check if it's time for a new election
            checkAutoElectionStart();
            return;
        }

        long elapsed = System.currentTimeMillis() - election.getPhaseStartTime();
        long phaseDuration = getPhaseDuration(election.getCurrentPhase()) * 24L * 60 * 60 * 1000;

        if (elapsed >= phaseDuration) {
            advancePhase();
        }
    }

    private void checkAutoElectionStart() {
        // Start election 5 days before term ends
        long remaining = plugin.getGovernmentManager().getTermRemainingTime();
        int daysBeforeTermEnd = 5;
        long threshold = daysBeforeTermEnd * 24L * 60 * 60 * 1000;

        if (remaining > 0 && remaining <= threshold && !getElection().isActive()) {
            startElection();
        }

        // Start election if no president
        if (!plugin.getGovernmentManager().getGovernment().hasPresident() && !getElection().isActive()) {
            startElection();
        }
    }

    private void advancePhase() {
        Election election = getElection();
        ElectionPhase previousPhase = election.getCurrentPhase();

        switch (previousPhase) {
            case REGISTRATION -> {
                if (election.getCandidates().isEmpty()) {
                    MessageUtils.broadcast("<red>No candidates registered. Registration phase restarted.</red>");
                    // Restart registration phase
                    election.setPhaseStartTime(System.currentTimeMillis());

                    MessageUtils.broadcastAnnouncement("ELECTION RESTARTED",
                            "<yellow>The election registration has restarted due to lack of candidates!</yellow>\n" +
                                    "<gray>Registration phase: " + getPhaseDuration(ElectionPhase.REGISTRATION)
                                    + " days</gray>\n" +
                                    "<green>Use </green><gold>/dc register [slogan]</gold><green> to run for president!</green>");
                    return;
                }
                election.nextPhase();
                MessageUtils.broadcastAnnouncement("CAMPAIGN PHASE",
                        "<yellow>Campaign phase has begun!</yellow>\n" +
                                "<gray>Duration: " + getPhaseDuration(ElectionPhase.CAMPAIGN) + " days</gray>\n" +
                                "<green>Candidates can now campaign for votes!</green>");
            }
            case CAMPAIGN -> {
                election.nextPhase();
                MessageUtils.broadcastAnnouncement("VOTING PHASE",
                        "<yellow>Voting has begun!</yellow>\n" +
                                "<gray>Duration: " + getPhaseDuration(ElectionPhase.VOTING) + " days</gray>\n" +
                                "<green>Use </green><gold>/dc vote</gold><green> to cast your vote!</green>");
                MessageUtils.broadcastTitle("<gold>🗳️ VOTE NOW 🗳️</gold>",
                        "<yellow>Make your voice heard!</yellow>", 20, 100, 20);
            }
            case VOTING -> {
                election.nextPhase();
                concludeVoting();
            }
            case INAUGURATION -> {
                election.endElection();
                // Clear player election data
                for (PlayerData data : plugin.getDataManager().getAllPlayerData()) {
                    data.clearElectionData();
                }
            }
            default -> {
            }
        }
    }

    private void concludeVoting() {
        Election election = getElection();
        Candidate winner = election.getWinner();

        if (winner == null) {
            MessageUtils.broadcast("<red>No valid votes were cast. Election inconclusive.</red>");
            election.endElection();
            return;
        }

        // Announce results
        StringBuilder results = new StringBuilder();
        results.append("<yellow>Election Results:</yellow>\n");

        List<Candidate> sortedCandidates = new ArrayList<>(election.getCandidates().values());
        sortedCandidates.sort((a, b) -> Double.compare(
                election.getCandidateVotes(b.getUuid()),
                election.getCandidateVotes(a.getUuid())));

        int rank = 1;
        for (Candidate candidate : sortedCandidates) {
            double votes = election.getCandidateVotes(candidate.getUuid());
            results.append("<gray>").append(rank).append(". </gray>");
            results.append(rank == 1 ? "<gold>" : "<white>");
            results.append(candidate.getName());
            results.append(rank == 1 ? "</gold>" : "</white>");
            results.append(" <gray>- ").append(String.format("%.1f", votes)).append(" votes</gray>\n");
            rank++;
        }

        MessageUtils.broadcastAnnouncement("ELECTION RESULTS", results.toString());

        // Refund deposits
        for (Candidate candidate : election.getCandidates().values()) {
            double refundRate = candidate.getUuid().equals(winner.getUuid())
                    ? plugin.getConfig().getDouble("election.registration-refund-winner", 1.0)
                    : plugin.getConfig().getDouble("election.registration-refund-loser", 0.5);

            double refund = candidate.getDepositPaid() * refundRate;
            if (refund > 0) {
                plugin.getVaultHook().deposit(candidate.getUuid(), refund);
                Player player = Bukkit.getPlayer(candidate.getUuid());
                if (player != null) {
                    MessageUtils.send(player, "managers.election.refund", "amount",
                            plugin.getVaultHook().format(refund));
                }
            }
        }

        // Give voter rewards
        giveVoterRewards();

        // Inaugurate winner
        MessageUtils.broadcastTitle("<gold>🎉 WINNER 🎉</gold>",
                "<yellow>" + winner.getName() + "</yellow>", 20, 100, 20);
        MessageUtils.broadcastSound(Sound.UI_TOAST_CHALLENGE_COMPLETE);

        // Schedule inauguration
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getGovernmentManager().setPresident(winner.getUuid(), winner.getName(), true);
        }, 20L * 5); // 5 seconds delay for dramatic effect
    }

    private void giveVoterRewards() {
        Election election = getElection();
        double participationReward = plugin.getConfig().getDouble("election.voter-rewards.participation", 10000);
        double lotteryBonus = plugin.getConfig().getDouble("election.voter-rewards.lottery-bonus", 50000);
        int lotteryWinners = plugin.getConfig().getInt("election.voter-rewards.lottery-winners", 10);

        List<UUID> voters = new ArrayList<>(election.getVotes().keySet());

        // Participation reward
        for (UUID voterUUID : voters) {
            if (!election.getVoterRewardsClaimed().contains(voterUUID)) {
                plugin.getVaultHook().deposit(voterUUID, participationReward);
                election.getVoterRewardsClaimed().add(voterUUID);

                Player player = Bukkit.getPlayer(voterUUID);
                if (player != null) {
                    MessageUtils.send(player, "<green>Thank you for voting! You received <gold>" +
                            plugin.getVaultHook().format(participationReward) + "</gold>!</green>");
                }
            }
        }

        // Lottery winners
        Collections.shuffle(voters);
        int winnersCount = Math.min(lotteryWinners, voters.size());
        for (int i = 0; i < winnersCount; i++) {
            UUID winnerUUID = voters.get(i);
            plugin.getVaultHook().deposit(winnerUUID, lotteryBonus);
            election.getLotteryWinners().add(winnerUUID);

            Player player = Bukkit.getPlayer(winnerUUID);
            if (player != null) {
                MessageUtils.send(player, "managers.election.lottery_win", "amount",
                        plugin.getVaultHook().format(lotteryBonus));
                MessageUtils.playSound(player, Sound.ENTITY_PLAYER_LEVELUP);
            }
        }

        if (winnersCount > 0) {
            MessageUtils.broadcast("<yellow>" + winnersCount + " lucky voters won the bonus lottery!</yellow>");
        }
    }

    public boolean registerCandidate(Player player, String slogan) {
        Election election = getElection();
        UUID uuid = player.getUniqueId();

        // Check phase
        if (election.getCurrentPhase() != ElectionPhase.REGISTRATION) {
            MessageUtils.send(player, "register.not_open");
            return false;
        }

        // Check already registered
        if (election.getCandidates().containsKey(uuid)) {
            MessageUtils.send(player, "<red>You are already registered as a candidate.</red>");
            return false;
        }

        // Check max candidates
        int maxCandidates = plugin.getConfig().getInt("election.max-candidates", 3);
        if (election.getCandidates().size() >= maxCandidates) {
            MessageUtils.send(player,
                    "<red>The maximum number of candidates (" + maxCandidates + ") has been reached.</red>");
            return false;
        }

        // Check requirements
        if (!meetsRequirements(player)) {
            return false;
        }

        // Check consecutive terms and cooldown
        if (!plugin.getGovernmentManager().canRunForPresident(uuid)) {
            int remainingCooldown = plugin.getGovernmentManager().getRemainingCooldownTerms(uuid);
            if (remainingCooldown > 0) {
                MessageUtils.send(player,
                        "<red>You must wait " + remainingCooldown
                                + " more term(s) before running for president again.</red>");
            } else {
                MessageUtils.send(player,
                        "<red>You have reached the maximum consecutive terms. Wait one term before running again.</red>");
            }
            return false;
        }

        // Check fee
        double registrationFee = plugin.getConfig().getDouble("election.registration-fee", 500000);
        if (!plugin.getVaultHook().has(uuid, registrationFee)) {
            MessageUtils.send(player, "<red>You need <gold>" + plugin.getVaultHook().format(registrationFee) +
                    "</gold> to register as a candidate.</red>");
            return false;
        }

        // Withdraw fee
        plugin.getVaultHook().withdraw(uuid, registrationFee);

        // Register candidate
        Candidate candidate = new Candidate(uuid, player.getName(), slogan, registrationFee);
        election.registerCandidate(candidate);

        // Update player data
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(uuid, player.getName());
        data.setTimesRanForPresident(data.getTimesRanForPresident() + 1);

        MessageUtils.broadcast("<yellow><gold>" + player.getName() +
                "</gold> has registered as a presidential candidate!</yellow>");
        MessageUtils.send(player, "register.success", "slogan", slogan);

        return true;
    }

    public boolean meetsRequirements(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(uuid, player.getName());

        // Level requirement
        int minLevel = plugin.getConfig().getInt("president.requirements.min-level", 100);
        if (player.getLevel() < minLevel) {
            MessageUtils.send(player, "<red>You need level " + minLevel + " to run for president. (Current: "
                    + player.getLevel() + ")</red>");
            return false;
        }

        // Playtime requirement
        double minPlaytime = plugin.getConfig().getDouble("president.requirements.min-playtime-hours", 100);
        if (data.getPlaytimeHours() < minPlaytime) {
            MessageUtils.send(player, "<red>You need " + minPlaytime + " hours of playtime. (Current: " +
                    String.format("%.1f", data.getPlaytimeHours()) + ")</red>");
            return false;
        }

        // Balance requirement
        double minBalance = plugin.getConfig().getDouble("president.requirements.min-vault-balance", 500000);
        if (!plugin.getVaultHook().has(uuid, minBalance)) {
            MessageUtils.send(player, "<red>You need " + plugin.getVaultHook().format(minBalance) +
                    " balance to run.</red>");
            return false;
        }

        // Endorsement requirement (check if already in campaign phase)
        int minEndorsements = plugin.getConfig().getInt("president.requirements.min-endorsements", 10);
        Candidate existing = getElection().getCandidate(uuid);
        if (existing != null && existing.getEndorsementCount() < minEndorsements) {
            // This is checked during campaign phase
        }

        // Punishment check
        int noPunishmentDays = plugin.getConfig().getInt("president.requirements.no-punishment-days", 30);
        if (data.hasRecentPunishment(noPunishmentDays)) {
            MessageUtils.send(player, "<red>You have a recent punishment record. Wait " +
                    noPunishmentDays + " days.</red>");
            return false;
        }

        return true;
    }

    public boolean endorseCandidate(Player endorser, UUID candidateUUID) {
        Election election = getElection();
        UUID endorserUUID = endorser.getUniqueId();

        // Check phase
        if (election.getCurrentPhase() != ElectionPhase.REGISTRATION &&
                election.getCurrentPhase() != ElectionPhase.CAMPAIGN) {
            MessageUtils.send(endorser, "<red>Endorsements are not currently accepted.</red>");
            return false;
        }

        // Check candidate exists
        Candidate candidate = election.getCandidate(candidateUUID);
        if (candidate == null) {
            MessageUtils.send(endorser, "<red>Candidate not found.</red>");
            return false;
        }

        // Check not endorsing self
        if (endorserUUID.equals(candidateUUID)) {
            MessageUtils.send(endorser, "<red>You cannot endorse yourself.</red>");
            return false;
        }

        // Check player data
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(endorserUUID, endorser.getName());

        // Check if already endorsed this candidate
        if (candidate.getEndorsements().contains(endorserUUID)) {
            MessageUtils.send(endorser, "<red>You have already endorsed this candidate.</red>");
            return false;
        }

        // Add endorsement
        candidate.addEndorsement(endorserUUID);
        data.addEndorsement(candidateUUID);

        // Update candidate's player data
        PlayerData candidateData = plugin.getDataManager().getPlayerData(candidateUUID);
        if (candidateData != null) {
            candidateData.setEndorsementsReceived(candidateData.getEndorsementsReceived() + 1);
        }

        MessageUtils.send(endorser, "<green>You endorsed <gold>" + candidate.getName() +
                "</gold>! (+10 campaign points)</green>");

        Player candidatePlayer = Bukkit.getPlayer(candidateUUID);
        if (candidatePlayer != null) {
            MessageUtils.send(candidatePlayer, "<green><gold>" + endorser.getName() +
                    "</gold> endorsed you! (+10 campaign points)</green>");
        }

        return true;
    }

    public boolean castVote(Player voter, UUID candidateUUID) {
        Election election = getElection();
        UUID voterUUID = voter.getUniqueId();

        // Check phase
        if (election.getCurrentPhase() != ElectionPhase.VOTING) {
            MessageUtils.send(voter, "<red>Voting is not currently open.</red>");
            return false;
        }

        // Check already voted
        if (election.hasVoted(voterUUID)) {
            MessageUtils.send(voter, "<red>You have already voted.</red>");
            return false;
        }

        // Check candidate exists
        Candidate candidate = election.getCandidate(candidateUUID);
        if (candidate == null) {
            MessageUtils.send(voter, "<red>Invalid candidate.</red>");
            return false;
        }

        // Calculate vote weight
        double weight = calculateVoteWeight(voter);

        // Cast vote
        Vote vote = new Vote(voterUUID, candidateUUID, weight);
        election.castVote(vote);

        // Update player data
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(voterUUID, voter.getName());
        data.setTotalVotesCast(data.getTotalVotesCast() + 1);

        MessageUtils.send(voter, "<green>You voted for <gold>" + candidate.getName() +
                "</gold>! (Vote weight: " + String.format("%.1f", weight) + ")</green>");
        MessageUtils.playSound(voter, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);

        return true;
    }

    public double calculateVoteWeight(Player player) {
        double weight = plugin.getConfig().getDouble("election.vote-weights.base", 1.0);
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());

        // Playtime bonus
        double playtimeThreshold = plugin.getConfig().getDouble("election.vote-weights.playtime-threshold-hours", 200);
        if (data.getPlaytimeHours() >= playtimeThreshold) {
            weight += plugin.getConfig().getDouble("election.vote-weights.playtime-bonus", 0.5);
        }

        // Level bonus
        int levelThreshold = plugin.getConfig().getInt("election.vote-weights.level-threshold", 75);
        if (player.getLevel() >= levelThreshold) {
            weight += plugin.getConfig().getDouble("election.vote-weights.level-bonus", 0.5);
        }

        // Balance bonus
        double balanceThreshold = plugin.getConfig().getDouble("election.vote-weights.balance-threshold", 1000000);
        if (plugin.getVaultHook().getBalance(player.getUniqueId()) >= balanceThreshold) {
            weight += plugin.getConfig().getDouble("election.vote-weights.balance-bonus", 0.5);
        }

        // Cap at max weight
        double maxWeight = plugin.getConfig().getDouble("election.vote-weights.max-weight", 2.5);
        return Math.min(weight, maxWeight);
    }

    public boolean setCampaignMessage(Player player, String message) {
        Election election = getElection();
        Candidate candidate = election.getCandidate(player.getUniqueId());

        if (candidate == null) {
            MessageUtils.send(player, "<red>You are not a registered candidate.</red>");
            return false;
        }

        if (election.getCurrentPhase() != ElectionPhase.CAMPAIGN) {
            MessageUtils.send(player, "<red>Campaign messages can only be set during campaign phase.</red>");
            return false;
        }

        // Check cooldown
        long cooldownHours = plugin.getConfig().getLong("election.campaign.broadcast-cooldown-hours", 6);
        long cooldownMillis = cooldownHours * 60 * 60 * 1000;
        if (System.currentTimeMillis() - candidate.getLastCampaignBroadcast() < cooldownMillis) {
            long remaining = cooldownMillis - (System.currentTimeMillis() - candidate.getLastCampaignBroadcast());
            MessageUtils.send(player, "<red>Campaign broadcast on cooldown. Remaining: " +
                    MessageUtils.formatTime(remaining) + "</red>");
            return false;
        }

        // Check fee
        double cost = plugin.getConfig().getDouble("election.campaign.broadcast-cost", 10000);
        if (!plugin.getVaultHook().has(player.getUniqueId(), cost)) {
            MessageUtils.send(player, "<red>Campaign broadcast costs " + plugin.getVaultHook().format(cost) + "</red>");
            return false;
        }

        plugin.getVaultHook().withdraw(player.getUniqueId(), cost);
        candidate.setCampaignMessage(message);
        candidate.setLastCampaignBroadcast(System.currentTimeMillis());

        MessageUtils.send(player, "<green>Campaign message set! It will be broadcast periodically.</green>");
        return true;
    }

    public void broadcastCampaignMessages() {
        Election election = getElection();
        if (election.getCurrentPhase() != ElectionPhase.CAMPAIGN)
            return;

        for (Candidate candidate : election.getCandidates().values()) {
            if (candidate.getCampaignMessage() != null && !candidate.getCampaignMessage().isEmpty()) {
                MessageUtils.broadcastRaw("<gold>[Campaign]</gold> <yellow>" + candidate.getName() +
                        ":</yellow> <white>" + candidate.getCampaignMessage() + "</white>");
            }
        }
    }

    private int getPhaseDuration(ElectionPhase phase) {
        return switch (phase) {
            case REGISTRATION -> plugin.getConfig().getInt("election.registration-days", 3);
            case CAMPAIGN -> plugin.getConfig().getInt("election.campaign-days", 7);
            case VOTING -> plugin.getConfig().getInt("election.voting-days", 3);
            case INAUGURATION -> plugin.getConfig().getInt("election.inauguration-days", 1);
            default -> 0;
        };
    }

    public long getPhaseRemainingTime() {
        Election election = getElection();
        if (!election.isActive())
            return 0;

        long phaseDuration = getPhaseDuration(election.getCurrentPhase()) * 24L * 60 * 60 * 1000;
        long elapsed = System.currentTimeMillis() - election.getPhaseStartTime();

        return Math.max(0, phaseDuration - elapsed);
    }

    public void forceStartElection() {
        getElection().endElection();
        startElection();
    }

    public void forceNextPhase() {
        if (!isElectionActive())
            return;
        advancePhase();
    }

    // Additional method for NationCommand
    public void endElection() {
        getElection().endElection();
    }

    // ==========================================================
    // Context-aware (per-nation) overloads — Phase 2B
    // ==========================================================

    /** Resolve Election dari nation atau global legacy bila nation null. */
    public Election getElection(Nation nation) {
        return nation != null ? nation.getElection() : getElection();
    }

    public boolean isElectionActive(Nation nation) {
        Election e = getElection(nation);
        return e != null && e.isActive();
    }

    public void startElection(Nation nation) {
        if (nation == null || nation.getType() != GovernmentType.REPUBLIC) return;
        Election election = nation.getElection();
        if (election == null || election.isActive()) return;

        election.startElection();
        plugin.getDataManager().saveNations();

        MessageUtils.broadcastAnnouncement("ELECTION BEGINS — " + nation.getName(),
                "<yellow>Pemilu presiden " + nation.getName() + " telah dimulai!</yellow>\n" +
                        "<gray>Fase registrasi: " + getPhaseDuration(ElectionPhase.REGISTRATION) + " hari</gray>\n" +
                        "<green>Anggota nation gunakan </green><gold>/dc register</gold><green> untuk maju!</green>");
    }

    public void startEmergencyElection(Nation nation) {
        if (nation == null) return;
        MessageUtils.broadcast("<red>Emergency election dipanggil di " + nation.getName() + "!</red>");
        startElection(nation);
    }

    public void endElection(Nation nation) {
        Election e = getElection(nation);
        if (e != null) e.endElection();
        plugin.getDataManager().saveNations();
    }

    public void forceNextPhase(Nation nation) {
        if (!isElectionActive(nation)) return;
        advancePhase(nation);
    }

    public long getPhaseRemainingTime(Nation nation) {
        Election election = getElection(nation);
        if (election == null || !election.isActive()) return 0;
        long phaseDuration = getPhaseDuration(election.getCurrentPhase()) * 24L * 60 * 60 * 1000;
        long elapsed = System.currentTimeMillis() - election.getPhaseStartTime();
        return Math.max(0, phaseDuration - elapsed);
    }

    /**
     * Iterasi seluruh nation REPUBLIC dan global legacy. Dipanggil scheduler
     * setiap menit menggantikan {@link #checkPhaseTransitions()} yang lama.
     */
    public void checkAllPhaseTransitions() {
        // Per-nation
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            if (nation.getType() != GovernmentType.REPUBLIC) continue;
            checkPhaseTransitions(nation);
        }
    }

    public void checkPhaseTransitions(Nation nation) {
        Election election = getElection(nation);
        if (election == null) return;
        if (!election.isActive()) {
            checkAutoElectionStart(nation);
            return;
        }
        long elapsed = System.currentTimeMillis() - election.getPhaseStartTime();
        long phaseDuration = getPhaseDuration(election.getCurrentPhase()) * 24L * 60 * 60 * 1000;
        if (elapsed >= phaseDuration) advancePhase(nation);
    }

    private void checkAutoElectionStart(Nation nation) {
        long remaining = plugin.getGovernmentManager().getTermRemainingTime(nation);
        long threshold = 5L * 24 * 60 * 60 * 1000;

        var gov = plugin.getGovernmentManager().getGovernment(nation);
        if (gov == null) return;

        // Mulai pemilu 5 hari sebelum term berakhir, atau bila tidak ada presiden
        if ((remaining > 0 && remaining <= threshold) || !gov.hasPresident()) {
            if (!nation.getElection().isActive()) startElection(nation);
        }
    }

    private void advancePhase(Nation nation) {
        Election election = getElection(nation);
        if (election == null) return;
        ElectionPhase previousPhase = election.getCurrentPhase();

        switch (previousPhase) {
            case REGISTRATION -> {
                if (election.getCandidates().isEmpty()) {
                    MessageUtils.broadcast("<red>Tidak ada kandidat di " + nation.getName() +
                            ". Fase registrasi diulang.</red>");
                    election.setPhaseStartTime(System.currentTimeMillis());
                    return;
                }
                election.nextPhase();
                MessageUtils.broadcastAnnouncement("CAMPAIGN — " + nation.getName(),
                        "<yellow>Fase kampanye dimulai untuk " + nation.getName() + "!</yellow>");
            }
            case CAMPAIGN -> {
                election.nextPhase();
                MessageUtils.broadcastAnnouncement("VOTING — " + nation.getName(),
                        "<yellow>Fase voting dimulai untuk " + nation.getName() + "!</yellow>");
            }
            case VOTING -> {
                election.nextPhase();
                concludeVoting(nation);
            }
            case INAUGURATION -> {
                election.endElection();
                for (PlayerData data : plugin.getDataManager().getAllPlayerData()) {
                    if (nation.isMember(data.getUuid())) data.clearElectionData();
                }
            }
            default -> { }
        }
        plugin.getDataManager().saveNations();
    }

    private void concludeVoting(Nation nation) {
        Election election = nation.getElection();
        Candidate winner = election.getWinner();

        if (winner == null) {
            MessageUtils.broadcast("<red>Tidak ada suara sah di pemilu " + nation.getName() +
                    ". Hasil tidak konklusif.</red>");
            election.endElection();
            return;
        }

        StringBuilder results = new StringBuilder();
        results.append("<yellow>Hasil Pemilu ").append(nation.getName()).append(":</yellow>\n");
        List<Candidate> sorted = new ArrayList<>(election.getCandidates().values());
        sorted.sort((a, b) -> Double.compare(
                election.getCandidateVotes(b.getUuid()),
                election.getCandidateVotes(a.getUuid())));
        int rank = 1;
        for (Candidate c : sorted) {
            double votes = election.getCandidateVotes(c.getUuid());
            results.append("<gray>").append(rank).append(". </gray>")
                    .append(rank == 1 ? "<gold>" : "<white>")
                    .append(c.getName())
                    .append(rank == 1 ? "</gold>" : "</white>")
                    .append(" <gray>- ").append(String.format("%.1f", votes)).append(" suara</gray>\n");
            rank++;
        }
        MessageUtils.broadcastAnnouncement("HASIL PEMILU — " + nation.getName(), results.toString());

        // Refund deposits
        for (Candidate candidate : election.getCandidates().values()) {
            double rate = candidate.getUuid().equals(winner.getUuid())
                    ? plugin.getConfig().getDouble("election.registration-refund-winner", 1.0)
                    : plugin.getConfig().getDouble("election.registration-refund-loser", 0.5);
            double refund = candidate.getDepositPaid() * rate;
            if (refund > 0) {
                plugin.getVaultHook().deposit(candidate.getUuid(), refund);
            }
        }

        // Voter rewards (per-nation, hanya untuk member nation)
        giveVoterRewards(nation, election);

        // Inaugurate winner
        MessageUtils.broadcastTitle("<gold>🎉 PRESIDEN " + nation.getName().toUpperCase() + " 🎉</gold>",
                "<yellow>" + winner.getName() + "</yellow>", 20, 100, 20);
        MessageUtils.broadcastSound(Sound.UI_TOAST_CHALLENGE_COMPLETE);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getGovernmentManager().setPresident(nation, winner.getUuid(), winner.getName(), true);
            plugin.getDataManager().saveNations();
        }, 20L * 5);
    }

    private void giveVoterRewards(Nation nation, Election election) {
        double participation = plugin.getConfig().getDouble("election.voter-rewards.participation", 10000);
        double lottery = plugin.getConfig().getDouble("election.voter-rewards.lottery-bonus", 50000);
        int lotteryWinners = plugin.getConfig().getInt("election.voter-rewards.lottery-winners", 10);

        List<UUID> voters = new ArrayList<>(election.getVotes().keySet());
        for (UUID uuid : voters) {
            if (!election.getVoterRewardsClaimed().contains(uuid)) {
                plugin.getVaultHook().deposit(uuid, participation);
                election.getVoterRewardsClaimed().add(uuid);
            }
        }
        Collections.shuffle(voters);
        int n = Math.min(lotteryWinners, voters.size());
        for (int i = 0; i < n; i++) {
            UUID winner = voters.get(i);
            plugin.getVaultHook().deposit(winner, lottery);
            election.getLotteryWinners().add(winner);
        }
    }

    public boolean registerCandidate(Player player, Nation nation, String slogan) {
        if (nation == null) return registerCandidate(player, slogan);
        if (nation.getType() != GovernmentType.REPUBLIC) {
            MessageUtils.send(player, "<red>Pemilu hanya untuk pemerintahan Republik.</red>");
            return false;
        }
        if (!nation.isMember(player.getUniqueId())) {
            MessageUtils.send(player, "<red>Anda bukan anggota nation ini.</red>");
            return false;
        }
        Election election = nation.getElection();
        UUID uuid = player.getUniqueId();

        if (election.getCurrentPhase() != ElectionPhase.REGISTRATION) {
            MessageUtils.send(player, "register.not_open");
            return false;
        }
        if (election.getCandidates().containsKey(uuid)) {
            MessageUtils.send(player, "<red>Anda sudah terdaftar sebagai kandidat.</red>");
            return false;
        }
        int max = plugin.getConfig().getInt("election.max-candidates", 3);
        if (election.getCandidates().size() >= max) {
            MessageUtils.send(player, "<red>Slot kandidat penuh (" + max + ").</red>");
            return false;
        }
        if (!meetsRequirements(player)) return false;

        double fee = plugin.getConfig().getDouble("election.registration-fee", 500_000);
        if (!plugin.getVaultHook().has(uuid, fee)) {
            MessageUtils.send(player, "<red>Anda butuh " + plugin.getVaultHook().format(fee) +
                    " untuk mendaftar.</red>");
            return false;
        }
        plugin.getVaultHook().withdraw(uuid, fee);
        election.registerCandidate(new Candidate(uuid, player.getName(), slogan, fee));

        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(uuid, player.getName());
        data.setTimesRanForPresident(data.getTimesRanForPresident() + 1);

        MessageUtils.broadcast("<yellow><gold>" + player.getName() +
                "</gold> mendaftar sebagai kandidat presiden " + nation.getName() + "!</yellow>");
        plugin.getDataManager().saveNations();
        return true;
    }

    public boolean endorseCandidate(Player endorser, Nation nation, UUID candidateUUID) {
        if (nation == null) return endorseCandidate(endorser, candidateUUID);
        if (!nation.isMember(endorser.getUniqueId())) {
            MessageUtils.send(endorser, "<red>Anda bukan anggota nation ini.</red>");
            return false;
        }
        Election election = nation.getElection();
        UUID endorserUUID = endorser.getUniqueId();

        if (election.getCurrentPhase() != ElectionPhase.REGISTRATION &&
                election.getCurrentPhase() != ElectionPhase.CAMPAIGN) {
            MessageUtils.send(endorser, "<red>Endorsement tidak diterima saat ini.</red>");
            return false;
        }
        Candidate candidate = election.getCandidate(candidateUUID);
        if (candidate == null) {
            MessageUtils.send(endorser, "<red>Kandidat tidak ditemukan.</red>");
            return false;
        }
        if (endorserUUID.equals(candidateUUID)) {
            MessageUtils.send(endorser, "<red>Tidak bisa mengendorse diri sendiri.</red>");
            return false;
        }
        if (candidate.getEndorsements().contains(endorserUUID)) {
            MessageUtils.send(endorser, "<red>Anda sudah mengendorse kandidat ini.</red>");
            return false;
        }
        candidate.addEndorsement(endorserUUID);
        plugin.getDataManager().getOrCreatePlayerData(endorserUUID, endorser.getName()).addEndorsement(candidateUUID);
        PlayerData candidateData = plugin.getDataManager().getPlayerData(candidateUUID);
        if (candidateData != null) {
            candidateData.setEndorsementsReceived(candidateData.getEndorsementsReceived() + 1);
        }
        MessageUtils.send(endorser, "<green>Anda mengendorse <gold>" + candidate.getName() + "</gold>!</green>");
        plugin.getDataManager().saveNations();
        return true;
    }

    public boolean castVote(Player voter, Nation nation, UUID candidateUUID) {
        if (nation == null) return castVote(voter, candidateUUID);
        if (!nation.isMember(voter.getUniqueId())) {
            MessageUtils.send(voter, "<red>Anda bukan anggota nation ini.</red>");
            return false;
        }
        Election election = nation.getElection();
        UUID voterUUID = voter.getUniqueId();

        if (election.getCurrentPhase() != ElectionPhase.VOTING) {
            MessageUtils.send(voter, "<red>Voting belum dibuka.</red>");
            return false;
        }
        if (election.hasVoted(voterUUID)) {
            MessageUtils.send(voter, "<red>Anda sudah memilih.</red>");
            return false;
        }
        Candidate candidate = election.getCandidate(candidateUUID);
        if (candidate == null) {
            MessageUtils.send(voter, "<red>Kandidat tidak valid.</red>");
            return false;
        }

        double weight = calculateVoteWeight(voter);
        election.castVote(new Vote(voterUUID, candidateUUID, weight));

        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(voterUUID, voter.getName());
        data.setTotalVotesCast(data.getTotalVotesCast() + 1);

        MessageUtils.send(voter, "<green>Anda memilih <gold>" + candidate.getName() +
                "</gold>! (Bobot: " + String.format("%.1f", weight) + ")</green>");
        MessageUtils.playSound(voter, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        plugin.getDataManager().saveNations();
        return true;
    }

    public boolean setCampaignMessage(Player player, Nation nation, String message) {
        if (nation == null) return setCampaignMessage(player, message);
        Election election = nation.getElection();
        Candidate candidate = election.getCandidate(player.getUniqueId());
        if (candidate == null) {
            MessageUtils.send(player, "<red>Anda bukan kandidat di nation ini.</red>");
            return false;
        }
        if (election.getCurrentPhase() != ElectionPhase.CAMPAIGN) {
            MessageUtils.send(player, "<red>Pesan kampanye hanya bisa di fase kampanye.</red>");
            return false;
        }
        long cooldownHours = plugin.getConfig().getLong("election.campaign.broadcast-cooldown-hours", 6);
        long cooldownMs = cooldownHours * 60 * 60 * 1000;
        if (System.currentTimeMillis() - candidate.getLastCampaignBroadcast() < cooldownMs) {
            long remaining = cooldownMs - (System.currentTimeMillis() - candidate.getLastCampaignBroadcast());
            MessageUtils.send(player, "<red>Cooldown broadcast: " +
                    MessageUtils.formatTime(remaining) + "</red>");
            return false;
        }
        double cost = plugin.getConfig().getDouble("election.campaign.broadcast-cost", 10000);
        if (!plugin.getVaultHook().has(player.getUniqueId(), cost)) {
            MessageUtils.send(player, "<red>Biaya broadcast: " + plugin.getVaultHook().format(cost) + "</red>");
            return false;
        }
        plugin.getVaultHook().withdraw(player.getUniqueId(), cost);
        candidate.setCampaignMessage(message);
        candidate.setLastCampaignBroadcast(System.currentTimeMillis());
        plugin.getDataManager().saveNations();
        return true;
    }

    public void broadcastCampaignMessages(Nation nation) {
        if (nation == null) return;
        Election election = nation.getElection();
        if (election == null || election.getCurrentPhase() != ElectionPhase.CAMPAIGN) return;
        for (Candidate c : election.getCandidates().values()) {
            if (c.getCampaignMessage() != null && !c.getCampaignMessage().isEmpty()) {
                MessageUtils.broadcastRaw("<gold>[Campaign " + nation.getName() + "]</gold> <yellow>" +
                        c.getName() + ":</yellow> <white>" + c.getCampaignMessage() + "</white>");
            }
        }
    }

    public void broadcastAllCampaignMessages() {
        broadcastCampaignMessages(); // legacy
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            if (nation.getType() == GovernmentType.REPUBLIC) broadcastCampaignMessages(nation);
        }
    }
}
