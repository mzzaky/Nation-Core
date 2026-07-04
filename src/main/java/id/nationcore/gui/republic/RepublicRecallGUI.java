package id.nationcore.gui.republic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import id.nationcore.NationCore;
import id.nationcore.models.Government;
import id.nationcore.models.Nation;
import id.nationcore.models.RecallPetition;
import id.nationcore.utils.MessageUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Recall System GUI - Full interactive interface for the Recall system
 * Allows players to view petition status, sign petitions, vote on recalls, etc.
 */
public class RepublicRecallGUI {

    private final NationCore plugin;

    public static final String RECALL_MENU_TITLE = "§c§l⚠ RECALL SYSTEM ⚠";
    public static final String RECALL_CONFIRM_TITLE = "§4§l📜 CONFIRM PETITION 📜";
    public static final String RECALL_VOTE_TITLE = "§6§l🗳 RECALL VOTE 🗳";

    public RepublicRecallGUI(NationCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Open the main Recall System menu
     */
    public void openRecallMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, LegacyComponentSerializer.legacySection().deserialize(RECALL_MENU_TITLE));

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        Government gov = (nation != null) ? nation.getRepublicGovernment() : plugin.getDataManager().getGovernment();
        RecallPetition petition = (nation != null) ? nation.getRecallPetition() : plugin.getDataManager().getRecallPetition();
        
        boolean hasActivePetition = petition != null
                && petition.getPhase() != RecallPetition.RecallPhase.COMPLETED
                && petition.getPhase() != RecallPetition.RecallPhase.FAILED;
        
        boolean isMember = nation != null && nation.isMember(player.getUniqueId());

        // === ROW 1: Header ===

        // Info item (center)
        ItemStack headerItem = createItem(Material.REDSTONE_TORCH, "§c§l⚠ RECALL SYSTEM ⚠",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7The Recall System is used to",
                "§7remove the President from office",
                "§7through a democratic petition.",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(4, headerItem);

        // === ROW 2: President Info & Status ===

        // President Head (Slot 10)
        if (gov.hasPresident()) {
            ItemStack presidentHead = createPresidentHead(gov);
            inv.setItem(10, presidentHead);
        } else {
            ItemStack noPresident = createItem(Material.BARRIER, "§c§lNo President",
                    "§7There is no president",
                    "§7to recall currently.");
            inv.setItem(10, noPresident);
        }

        // Petition Status (Slot 13)
        ItemStack statusItem = createPetitionStatusItem(petition, hasActivePetition);
        inv.setItem(13, statusItem);

        // Config Info (Slot 16)
        ItemStack configItem = createConfigInfoItem();
        inv.setItem(16, configItem);

        // === ROW 3: Petition Details / Progress ===

        if (hasActivePetition) {
            if (petition.getPhase() == RecallPetition.RecallPhase.COLLECTING) {
                // Signature collection phase
                createCollectingPhaseItems(inv, petition, player, isMember);
            } else if (petition.getPhase() == RecallPetition.RecallPhase.VOTING) {
                // Voting phase
                createVotingPhaseItems(inv, petition, player, isMember);
            }
        } else {
            // No active petition - show start petition option
            createNoPetitionItems(inv, player, gov, isMember);
        }

        // === ROW 5: Action Buttons ===

        if (hasActivePetition) {
            if (petition.getPhase() == RecallPetition.RecallPhase.COLLECTING) {
                // Sign Petition Button (Slot 38)
                if (isMember && !petition.hasSigned(player.getUniqueId())
                        && !petition.getTargetId().equals(player.getUniqueId())) {

                    double deposit = plugin.getRecallSignatureDeposit();
                    double balance = plugin.getVaultHook().getBalance(player.getUniqueId());
                    boolean canAfford = balance >= deposit;

                    ItemStack signItem = createItem(
                            canAfford ? Material.LIME_CONCRETE : Material.RED_CONCRETE,
                            canAfford ? "§a§l✍ SIGN PETITION" : "§c§l✍ SIGN PETITION",
                            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            "§7Deposit required: §6" + MessageUtils.formatNumber(deposit),
                            "§7Your balance: §f" + MessageUtils.formatNumber(balance),
                            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            canAfford ? "§aClick to sign the petition!" : "§cNot enough money!",
                            canAfford ? "" : "§cNeed §6" + MessageUtils.formatNumber(deposit - balance) + " §cmore");
                    if (canAfford)
                        addGlow(signItem);
                    inv.setItem(38, signItem);

                } else if (isMember && petition.hasSigned(player.getUniqueId())
                        && !petition.getInitiatorId().equals(player.getUniqueId())) {

                    // Withdraw Signature Button (Slot 38)
                    double deposit = petition.getDeposit(player.getUniqueId());
                    double refund = deposit * 0.5;
                    ItemStack withdrawItem = createItem(Material.ORANGE_CONCRETE,
                            "§6§l↩ WITHDRAW SIGNATURE",
                            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            "§7Your deposit: §6" + MessageUtils.formatNumber(deposit),
                            "§7Refund (50%): §e" + MessageUtils.formatNumber(refund),
                            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            "§c⚠ You will only get 50% back!",
                            "",
                            "§6Click to withdraw");
                    inv.setItem(38, withdrawItem);

                } else if (isMember && petition.hasSigned(player.getUniqueId())
                        && petition.getInitiatorId().equals(player.getUniqueId())) {
                    // Initiator - cannot withdraw
                    ItemStack initiatorItem = createItem(Material.GREEN_CONCRETE,
                            "§a§l✔ YOU STARTED THIS PETITION",
                            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            "§7You are the initiator",
                            "§7of this petition.",
                            "§7You cannot withdraw.",
                            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                    inv.setItem(38, initiatorItem);
                } else if (!isMember) {
                    // Not a member - cannot participate
                    ItemStack noMemberItem = createItem(Material.BARRIER, "§c§l✗ NOT A MEMBER",
                            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            "§7Only registered members of",
                            "§7this nation can sign the",
                            "§7recall petition.",
                            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                    inv.setItem(38, noMemberItem);
                }

                // Status Command Info (Slot 40)
                ItemStack statusCmd = createItem(Material.PAPER, "§e§l📋 VIEW STATUS",
                        "§7Click to see detailed status",
                        "§7in chat.");
                inv.setItem(40, statusCmd);

            } else if (petition.getPhase() == RecallPetition.RecallPhase.VOTING) {
                if (isMember && !petition.hasVoted(player.getUniqueId())) {
                    // Vote REMOVE Button (Slot 38)
                    ItemStack removeVote = createItem(Material.RED_CONCRETE, "§c§l✗ VOTE: REMOVE",
                            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            "§7Vote to §cREMOVE§7 the",
                            "§7president from office.",
                            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            "§c⚠ This cannot be undone!",
                            "",
                            "§cClick to vote REMOVE");
                    inv.setItem(38, removeVote);

                    // Vote KEEP Button (Slot 42)
                    ItemStack keepVote = createItem(Material.LIME_CONCRETE, "§a§l✔ VOTE: KEEP",
                            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            "§7Vote to §aKEEP§7 the",
                            "§7president in office.",
                            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            "§c⚠ This cannot be undone!",
                            "",
                            "§aClick to vote KEEP");
                    inv.setItem(42, keepVote);
                } else if (isMember && petition.hasVoted(player.getUniqueId())) {
                    // Already voted
                    boolean votedRemove = petition.getRecallVotes().get(player.getUniqueId());
                    ItemStack alreadyVoted = createItem(Material.ENCHANTED_BOOK,
                            "§e§l✔ YOU HAVE VOTED",
                            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            "§7Your vote: " + (votedRemove ? "§cREMOVE" : "§aKEEP"),
                            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            "§7Thank you for voting!");
                    addGlow(alreadyVoted);
                    inv.setItem(40, alreadyVoted);
                } else if (!isMember) {
                    // Not a member - cannot vote
                    ItemStack noMemberItem = createItem(Material.BARRIER, "§c§l✗ NOT A MEMBER",
                            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            "§7Only registered members of",
                            "§7this nation can participate",
                            "§7in the recall vote.",
                            "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                    inv.setItem(40, noMemberItem);
                }
            }
        }

        // === ROW 6: Footer ===

        // Back to Main Menu
        ItemStack backItem = createItem(Material.ARROW, "§7§l◀ Back", "§7Back to Main Menu");
        inv.setItem(45, backItem);

        // Refresh
        ItemStack refreshItem = createItem(Material.CLOCK, "§e§l🔄 Refresh",
                "§7Click to refresh",
                "§7petition data");
        inv.setItem(49, refreshItem);

        // Close Button
        ItemStack closeItem = createItem(Material.BARRIER, "§c§lClose Menu", "§7Click to close");
        inv.setItem(53, closeItem);

        // Fill empty slots with glass
        fillGlass(inv);

        // Decorative corners
        ItemStack redCorner = createItem(Material.RED_STAINED_GLASS_PANE, " ");
        int[] corners = { 0, 8, 45, 53 };
        // Only set corners that aren't already set
        for (int c : corners) {
            if (inv.getItem(c) == null || inv.getItem(c).getType() == Material.GRAY_STAINED_GLASS_PANE) {
                inv.setItem(c, redCorner);
            }
        }

        player.openInventory(inv);
    }

    /**
     * Open the Confirm Petition Start GUI
     */
    public void openConfirmPetition(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, LegacyComponentSerializer.legacySection().deserialize(RECALL_CONFIRM_TITLE));

        Government gov = plugin.getDataManager().getGovernment();
        double deposit = plugin.getRecallSignatureDeposit();
        int requiredSignatures = plugin.getRecallManager().getRequiredSignatures();
        long collectionDays = plugin.getRecallCollectionDays();

        // Warning Header (Slot 4)
        ItemStack warningItem = createItem(Material.TNT, "§4§l⚠ START RECALL PETITION ⚠",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7You are about to start a",
                "§7recall petition against:",
                "§f" + (gov.hasPresident() ? Bukkit.getOfflinePlayer(gov.getPresidentUUID()).getName()
                        : "No President"),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "",
                "§7Deposit: §6" + MessageUtils.formatNumber(deposit),
                "§7Signatures needed: §f" + requiredSignatures,
                "§7Collection period: §f" + collectionDays + " days",
                "",
                "§c⚠ Your deposit will be LOST",
                "§c  if the petition fails!");
        inv.setItem(4, warningItem);

        // Confirm Button (Slot 11)
        ItemStack confirmItem = createItem(Material.LIME_CONCRETE, "§a§l✔ CONFIRM",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Click to start the",
                "§7recall petition.",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Deposit: §6" + MessageUtils.formatNumber(deposit),
                "",
                "§aClick to confirm!");
        addGlow(confirmItem);
        inv.setItem(11, confirmItem);

        // Cancel Button (Slot 15)
        ItemStack cancelItem = createItem(Material.RED_CONCRETE, "§c§l✗ CANCEL",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Go back to the",
                "§7recall menu.",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "",
                "§cClick to cancel");
        inv.setItem(15, cancelItem);

        // Reason input info (Slot 22)
        ItemStack reasonItem = createItem(Material.WRITABLE_BOOK, "§e§l📝 REASON",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7A default reason will be used:",
                "§f\"Player-initiated recall\"",
                "",
                "§7For custom reason, use command:",
                "§f/dc recall start <reason>",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(22, reasonItem);

        // Fill glass
        fillGlass(inv);

        player.openInventory(inv);
    }

    // === Private Helper Methods for Building Inventory Content ===

    @SuppressWarnings("deprecation")
    private ItemStack createPresidentHead(Government gov) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        var offlinePlayer = Bukkit.getOfflinePlayer(gov.getPresidentUUID());
        meta.setOwningPlayer(offlinePlayer);
        meta.setDisplayName("§6§l👑 PRESIDENT: " + offlinePlayer.getName());

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Term #" + gov.getCurrentTerm());
        lore.add("§7Time left: §f" + MessageUtils.formatTime(gov.getTermEndTime() - System.currentTimeMillis()));
        lore.add("§7Approval: §e" + String.format("%.1f", gov.getApprovalRating()) + "/5.0 ⭐");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createPetitionStatusItem(RecallPetition petition, boolean hasActivePetition) {
        if (!hasActivePetition) {
            return createItem(Material.GRAY_DYE, "§7§l📋 PETITION STATUS",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Status: §fNo Active Petition",
                    "",
                    "§7There is no active recall",
                    "§7petition at the moment.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        }

        Material mat;
        String phaseColor;
        switch (petition.getPhase()) {
            case COLLECTING:
                mat = Material.ORANGE_DYE;
                phaseColor = "§6";
                break;
            case VOTING:
                mat = Material.RED_DYE;
                phaseColor = "§c";
                break;
            default:
                mat = Material.GRAY_DYE;
                phaseColor = "§7";
                break;
        }

        String initiatorName = Bukkit.getOfflinePlayer(petition.getInitiatorId()).getName();
        String targetName = Bukkit.getOfflinePlayer(petition.getTargetId()).getName();

        List<String> lore = new ArrayList<>();
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Phase: " + phaseColor + petition.getPhase().getDisplayName());
        lore.add("§7Target: §f" + targetName);
        lore.add("§7Initiated by: §f" + initiatorName);
        lore.add("§7Reason: §f" + petition.getReason());
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        if (petition.getPhase() == RecallPetition.RecallPhase.COLLECTING) {
            long remaining = petition.getCollectionDeadline() - System.currentTimeMillis();
            lore.add("§7Time remaining: §f" + MessageUtils.formatTime(Math.max(0, remaining)));
        } else if (petition.getPhase() == RecallPetition.RecallPhase.VOTING) {
            long remaining = petition.getVotingEndTime() - System.currentTimeMillis();
            lore.add("§7Voting ends in: §f" + MessageUtils.formatTime(Math.max(0, remaining)));
        }

        ItemStack item = createItem(mat, phaseColor + "§l📋 PETITION STATUS", lore.toArray(new String[0]));
        addGlow(item);
        return item;
    }

    private ItemStack createConfigInfoItem() {
        double deposit = plugin.getRecallSignatureDeposit();
        double requiredPercentage = plugin.getRecallRequiredPercentage();
        double signaturePercentage = plugin.getRecallRequiredSignaturePercentage();
        long collectionDays = plugin.getRecallCollectionDays();
        long votingDays = plugin.getRecallVotingDays();
        long cooldownDays = plugin.getRecallCooldownDays();

        return createItem(Material.BOOK, "§e§l📖 RECALL RULES",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Signature Deposit: §6" + MessageUtils.formatNumber(deposit),
                "§7Signatures Needed: §f" + String.format("%.0f", signaturePercentage) + "% of active players",
                "§7Collection Period: §f" + collectionDays + " days",
                "§7Voting Period: §f" + votingDays + " days",
                "§7Removal Threshold: §f" + String.format("%.0f", requiredPercentage) + "% votes",
                "§7Cooldown after fail: §f" + cooldownDays + " days",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "",
                "§7§oDeposits are refunded if",
                "§7§orecall is successful.",
                "§7§oDeposits go to treasury",
                "§7§oif recall fails.");
    }

    private void createCollectingPhaseItems(Inventory inv, RecallPetition petition, Player player, boolean isMember) {
        int currentSigs = petition.getSignatureCount();
        int requiredSigs = plugin.getRecallManager().getRequiredSignatures();
        double progress = Math.min(1.0, (double) currentSigs / requiredSigs);

        // Signature Progress Bar (Slots 19-25)
        ItemStack progressHeader = createItem(Material.WRITABLE_BOOK, "§6§l📊 SIGNATURE PROGRESS",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Signatures: §f" + currentSigs + " / " + requiredSigs,
                "§7Progress: §f" + String.format("%.1f", progress * 100) + "%",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(19, progressHeader);

        // Visual progress bar (Slots 20-24)
        int barSlots = 5;
        int filledSlots = (int) Math.ceil(progress * barSlots);
        for (int i = 0; i < barSlots; i++) {
            Material barMat = i < filledSlots ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
            String barName = i < filledSlots ? "§a█ Filled" : "§c░ Empty";
            ItemStack barItem = createItem(barMat, barName,
                    "§7" + currentSigs + "/" + requiredSigs + " signatures");
            inv.setItem(20 + i, barItem);
        }

        // Deposit Info (Slot 25)
        double totalDeposits = petition.getTotalDeposits();
        ItemStack depositItem = createItem(Material.GOLD_INGOT, "§6§l💰 TOTAL DEPOSITS",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Total deposited: §6" + MessageUtils.formatNumber(totalDeposits),
                "§7Signers: §f" + currentSigs,
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7§oDeposits go to treasury",
                "§7§oif recall fails.");
        inv.setItem(25, depositItem);

        // Time Remaining (Slot 31)
        long remaining = petition.getCollectionDeadline() - System.currentTimeMillis();
        Material timeMat = remaining > 86400000L ? Material.LIME_DYE
                : remaining > 43200000L ? Material.YELLOW_DYE : Material.RED_DYE;
        String timeColor = remaining > 86400000L ? "§a" : remaining > 43200000L ? "§e" : "§c";
        ItemStack timeItem = createItem(timeMat, timeColor + "§l⏰ TIME REMAINING",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Time left: " + timeColor + MessageUtils.formatTime(Math.max(0, remaining)),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                remaining <= 0 ? "§c⚠ Petition has expired!" : "§7Collect enough signatures in time!");
        inv.setItem(31, timeItem);

        // Player's Status (Slot 29)
        if (!isMember) {
            ItemStack myStatusItem = createItem(Material.BARRIER, "§c§l✗ NOT A MEMBER",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7You are not a registered",
                    "§7member of this nation.",
                    "§7Only members can participate.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            inv.setItem(29, myStatusItem);
        } else if (petition.hasSigned(player.getUniqueId())) {
            double playerDeposit = petition.getDeposit(player.getUniqueId());
            boolean isInitiator = petition.getInitiatorId().equals(player.getUniqueId());
            ItemStack myStatusItem = createItem(Material.LIME_DYE, "§a§l✔ YOUR STATUS",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7You §ahave signed§7 this petition.",
                    "§7Your deposit: §6" + MessageUtils.formatNumber(playerDeposit),
                    isInitiator ? "§6You are the initiator!" : "",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            addGlow(myStatusItem);
            inv.setItem(29, myStatusItem);
        } else if (petition.getTargetId().equals(player.getUniqueId())) {
            ItemStack myStatusItem = createItem(Material.RED_DYE, "§c§l⚠ YOUR STATUS",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§cYou are the TARGET of",
                    "§cthis recall petition!",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            inv.setItem(29, myStatusItem);
        } else {
            ItemStack myStatusItem = createItem(Material.GRAY_DYE, "§7§l✗ YOUR STATUS",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7You §chave not signed§7 yet.",
                    "§7Sign below to support the recall!",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            inv.setItem(29, myStatusItem);
        }

        // Signer count icon (Slot 33)
        ItemStack signerItem = createItem(Material.PLAYER_HEAD, "§b§l👥 SIGNERS",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Total signers: §f" + currentSigs,
                "§7Required: §f" + requiredSigs,
                "§7Need §e" + Math.max(0, requiredSigs - currentSigs) + "§7 more",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(33, signerItem);
    }

    private void createVotingPhaseItems(Inventory inv, RecallPetition petition, Player player, boolean isMember) {
        int removeVotes = petition.getRemoveVotes();
        int keepVotes = petition.getKeepVotes();
        int totalVotes = removeVotes + keepVotes;
        double removePercent = totalVotes > 0 ? (double) removeVotes / totalVotes * 100 : 0;
        double keepPercent = totalVotes > 0 ? (double) keepVotes / totalVotes * 100 : 0;
        double requiredPercentage = plugin.getRecallRequiredPercentage();

        // Vote Stats Header (Slot 19)
        ItemStack voteHeader = createItem(Material.NETHER_STAR, "§6§l🗳 VOTE RESULTS",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Total votes: §f" + totalVotes,
                "§7Removal threshold: §f" + String.format("%.0f", requiredPercentage) + "%",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        addGlow(voteHeader);
        inv.setItem(19, voteHeader);

        // Remove Votes (Slot 21)
        ItemStack removeItem = createItem(Material.RED_WOOL, "§c§l✗ REMOVE VOTES",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Votes: §c" + removeVotes,
                "§7Percentage: §c" + String.format("%.1f", removePercent) + "%",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                removePercent >= requiredPercentage ? "§c⚠ Recall will PASS!"
                        : "§7Need " + String.format("%.1f", requiredPercentage) + "% to pass");
        inv.setItem(21, removeItem);

        // VS indicator (Slot 22)
        ItemStack vsItem = createItem(Material.IRON_SWORD, "§e§lVS",
                "§7Remove vs Keep");
        ItemMeta vsMeta = vsItem.getItemMeta();
        vsMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        vsItem.setItemMeta(vsMeta);
        inv.setItem(22, vsItem);

        // Keep Votes (Slot 23)
        ItemStack keepItem = createItem(Material.LIME_WOOL, "§a§l✔ KEEP VOTES",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Votes: §a" + keepVotes,
                "§7Percentage: §a" + String.format("%.1f", keepPercent) + "%",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(23, keepItem);

        // Visual vote bar (Slots 28-34)
        int barSlots = 7;
        int removeBar = totalVotes > 0 ? (int) Math.round((double) removeVotes / totalVotes * barSlots) : 0;
        for (int i = 0; i < barSlots; i++) {
            boolean isRemove = i < removeBar;
            Material barMat = isRemove ? Material.RED_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE;
            String barName = isRemove ? "§c█ Remove" : "§a█ Keep";
            ItemStack barItem = createItem(barMat, barName,
                    "§cRemove: " + String.format("%.1f", removePercent) + "%",
                    "§aKeep: " + String.format("%.1f", keepPercent) + "%");
            inv.setItem(28 + i, barItem);
        }

        // Time remaining (Slot 25)
        long remaining = petition.getVotingEndTime() - System.currentTimeMillis();
        ItemStack timeItem = createItem(Material.CLOCK, "§e§l⏰ VOTING ENDS IN",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7" + MessageUtils.formatTime(Math.max(0, remaining)),
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(25, timeItem);

        // Player vote status (Slot 40 is handled in action buttons section)
    }

    private void createNoPetitionItems(Inventory inv, Player player, Government gov, boolean isMember) {
        if (!gov.hasPresident()) {
            // No president to recall
            ItemStack noPresItem = createItem(Material.BARRIER, "§c§l✗ NO PRESIDENT",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7There is no president",
                    "§7to recall at this time.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "",
                    "§7Wait for an election",
                    "§7to elect a president first!");
            inv.setItem(22, noPresItem);
            return;
        }

        // Check cooldown
        long cooldownRemaining = plugin.getRecallManager().getCooldownRemaining();
        if (cooldownRemaining > 0) {
            ItemStack cooldownItem = createItem(Material.ICE, "§b§l❄ COOLDOWN ACTIVE",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7A recall petition recently failed.",
                    "§7You must wait before starting",
                    "§7a new petition.",
                    "",
                    "§7Cooldown remaining:",
                    "§f" + MessageUtils.formatTime(cooldownRemaining),
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            inv.setItem(22, cooldownItem);
            return;
        }

        // Can potentially start a petition
        boolean isPresident = gov.getPresidentUUID().equals(player.getUniqueId());
        double deposit = plugin.getRecallSignatureDeposit();
        double balance = plugin.getVaultHook().getBalance(player.getUniqueId());
        boolean canAfford = balance >= deposit;

        if (isPresident) {
            ItemStack selfItem = createItem(Material.BARRIER, "§c§l✗ CANNOT RECALL YOURSELF",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7You are the President!",
                    "§7You cannot start a recall",
                    "§7petition against yourself.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            inv.setItem(22, selfItem);
        } else if (!isMember) {
            ItemStack memberItem = createItem(Material.BARRIER, "§c§l✗ NOT A MEMBER",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7You are not a registered",
                    "§7member of this nation.",
                    "§7Only members can start a recall.",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            inv.setItem(22, memberItem);
        } else {
            String presName = Bukkit.getOfflinePlayer(gov.getPresidentUUID()).getName();
            ItemStack startItem = createItem(
                    canAfford ? Material.REDSTONE_BLOCK : Material.GRAY_CONCRETE,
                    canAfford ? "§c§l📜 START RECALL PETITION" : "§8§l📜 START RECALL PETITION",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Start a petition to recall",
                    "§7President §f" + presName + "§7!",
                    "",
                    "§7Deposit required: §6" + MessageUtils.formatNumber(deposit),
                    "§7Your balance: §f" + MessageUtils.formatNumber(balance),
                    canAfford ? "" : "§cNeed §6" + MessageUtils.formatNumber(deposit - balance) + "§c more!",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    canAfford ? "§cClick to start petition!" : "§cNot enough money!");
            if (canAfford)
                addGlow(startItem);
            inv.setItem(22, startItem);
        }

        // How it works info (Slot 20)
        ItemStack howItem = createItem(Material.OAK_SIGN, "§e§l📋 HOW IT WORKS",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§71. §fStart a petition (deposit required)",
                "§72. §fCollect signatures from players",
                "§73. §fOnce enough signatures, voting starts",
                "§74. §fAll players vote: Remove or Keep",
                "§75. §fIf 60%+ vote Remove → Recall!",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "",
                "§7§oSuccessful recall = deposits refunded",
                "§7§oFailed recall = deposits to treasury");
        inv.setItem(20, howItem);

        // Requirements (Slot 24)
        int requiredSigs = plugin.getRecallManager().getRequiredSignatures();
        ItemStack reqItem = createItem(Material.DIAMOND, "§b§l📊 REQUIREMENTS",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7Signatures needed: §f" + requiredSigs,
                "§7Deposit per signer: §6" + MessageUtils.formatNumber(deposit),
                "§7Collection time: §f" + plugin.getRecallCollectionDays() + " days",
                "§7Voting time: §f" + plugin.getRecallVotingDays() + " days",
                "§7Removal threshold: §f" + String.format("%.0f", plugin.getRecallRequiredPercentage()) + "% votes",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        inv.setItem(24, reqItem);
    }

    // === Utility Methods ===

    @SuppressWarnings("deprecation")
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) {
            // Filter out completely empty strings at the end
            List<String> loreList = new ArrayList<>(Arrays.asList(lore));
            meta.setLore(loreList);
        }
        item.setItemMeta(meta);
        return item;
    }

    private void addGlow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    private void fillGlass(Inventory inv) {
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, glass);
            }
        }
    }
}
