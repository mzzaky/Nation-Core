package id.nationcore.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import id.nationcore.NationCore;
import id.nationcore.models.Election;
import id.nationcore.utils.MessageUtils;

public class VotingGUI {

    private final NationCore plugin;
    public static final String VOTING_GUI_TITLE = "§6§l🗳 ELECTION VOTING 🗳";
    public static final String CANDIDATE_GUI_TITLE = "§e§lCandidate: ";

    public VotingGUI(NationCore plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    public void openVotingMenu(Player player) {
        Election election = plugin.getDataManager().getElection();

        // Check if voting is allowed
        if (election.getPhase() != Election.ElectionPhase.VOTING) {
            MessageUtils.send(player, "<red>Voting is only available during the voting phase!");
            return;
        }

        if (election.hasVoted(player.getUniqueId())) {
            MessageUtils.send(player, "<red>You have already voted!");
            return;
        }

        List<UUID> candidates = new ArrayList<>(election.getCandidates().keySet());
        int size = Math.min(54, ((candidates.size() / 9) + 1) * 9);
        size = Math.max(27, size);

        Inventory inv = Bukkit.createInventory(null, size, VOTING_GUI_TITLE);

        // Add candidates
        int slot = 10;
        for (UUID candidateUUID : candidates) {
            if (slot >= size - 9)
                break;

            ItemStack head = createCandidateHead(candidateUUID, election);
            inv.setItem(slot, head);

            slot++;
            if ((slot + 1) % 9 == 0)
                slot += 2; // Skip borders
        }

        // Info item
        ItemStack infoItem = createItem(Material.BOOK, "§e§lElection Info",
                "§7Phase: §f" + election.getPhase().getDisplayName(),
                "§7Total Candidates: §f" + candidates.size(),
                "§7Total Votes: §f" + election.getTotalVotes(),
                "",
                "§aClick a candidate's head to vote!");
        inv.setItem(4, infoItem);

        // Close button
        ItemStack closeItem = createItem(Material.BARRIER, "§c§lClose", "§7Click to close");
        inv.setItem(size - 5, closeItem);

        // Fill empty with glass
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < size; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, glass);
            }
        }

        player.openInventory(inv);
    }

    @SuppressWarnings("deprecation")
    public void openCandidateInfo(Player player, UUID candidateUUID) {
        Election election = plugin.getDataManager().getElection();
        String candidateName = Bukkit.getOfflinePlayer(candidateUUID).getName();

        Inventory inv = Bukkit.createInventory(null, 27, CANDIDATE_GUI_TITLE + candidateName);

        // Candidate head in center
        ItemStack head = createCandidateHead(candidateUUID, election);
        inv.setItem(13, head);

        // Stats
        int endorsements = election.getEndorsementCount(candidateUUID);
        double votes = election.getVoteCount(candidateUUID);
        var playerData = plugin.getDataManager().getPlayerData(candidateUUID);
        var viewerData = plugin.getDataManager().getPlayerData(player.getUniqueId());

        ItemStack statsItem = createItem(Material.PAPER, "§e§lCandidate Statistics",
                "§7Endorsements: §f" + endorsements,
                "§7Votes: §f" + String.format("%.1f", votes),
                "§7Playtime: §f" + MessageUtils.formatTime(playerData.getTotalPlaytime()),
                "§7Times served as President: §f" + playerData.getTimesServedAsPresident() + "x");
        inv.setItem(11, statsItem);

        // Vote button
        if (election.getPhase() == Election.ElectionPhase.VOTING && !election.hasVoted(player.getUniqueId())) {
            ItemStack voteItem = createItem(Material.LIME_WOOL, "§a§lVOTE " + candidateName.toUpperCase(),
                    "§7Click to cast your vote",
                    "",
                    "§c⚠ Cannot be undone!");
            inv.setItem(15, voteItem);
        } else if (election.hasVoted(player.getUniqueId())) {
            ItemStack votedItem = createItem(Material.RED_WOOL, "§c§lAlready Voted",
                    "§7You have already voted");
            inv.setItem(15, votedItem);
        }

        // Endorse button (during registration/campaign)
        if (election.getPhase() == Election.ElectionPhase.REGISTRATION ||
                election.getPhase() == Election.ElectionPhase.CAMPAIGN) {

            boolean alreadyEndorsed = viewerData.hasEndorsed(candidateUUID);
            if (!alreadyEndorsed && !candidateUUID.equals(player.getUniqueId())) {
                ItemStack endorseItem = createItem(Material.GOLDEN_APPLE, "§6§lEndorse " + candidateName,
                        "§7Provide support to this candidate",
                        "",
                        "§eClick to endorse!");
                inv.setItem(16, endorseItem);
            }
        }

        // Back button
        ItemStack backItem = createItem(Material.ARROW, "§7§lBack", "§7Back to candidate list");
        inv.setItem(18, backItem);

        // Fill glass
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, glass);
            }
        }

        player.openInventory(inv);
    }

    @SuppressWarnings("deprecation")
    private ItemStack createCandidateHead(UUID candidateUUID, Election election) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        var offlinePlayer = Bukkit.getOfflinePlayer(candidateUUID);
        meta.setOwningPlayer(offlinePlayer);
        meta.setDisplayName("§e§l" + offlinePlayer.getName());

        int endorsements = election.getEndorsementCount(candidateUUID);
        double votes = election.getVoteCount(candidateUUID);

        List<String> lore = new ArrayList<>();
        lore.add("§7Endorsements: §f" + endorsements);

        if (election.getPhase() == Election.ElectionPhase.VOTING ||
                election.getPhase() == Election.ElectionPhase.INAUGURATION) {
            lore.add("§7Votes: §f" + String.format("%.1f", votes));
        }

        lore.add("");
        lore.add("§aClick for details");

        meta.setLore(lore);
        head.setItemMeta(meta);

        return head;
    }

    @SuppressWarnings("deprecation")
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) {
            meta.setLore(Arrays.asList(lore));
        }
        item.setItemMeta(meta);
        return item;
    }
}
