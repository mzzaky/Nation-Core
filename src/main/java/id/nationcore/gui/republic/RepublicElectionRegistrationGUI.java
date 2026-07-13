package id.nationcore.gui.republic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import id.nationcore.NationCore;
import id.nationcore.gui.NationMenuBase;
import id.nationcore.managers.ElectionManager;
import id.nationcore.models.Election;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.Nation;
import id.nationcore.models.PlayerData;
import id.nationcore.utils.MessageUtils;

/**
 * Presidential candidacy registration menu ("Pendaftaran Berkas") for REPUBLIC
 * nations. Only reachable while the nation's election is in the REGISTRATION
 * phase. The player assembles six documents (berkas) — playtime, level, fee,
 * clean record, campaign message, and the candidacy agreement — then files
 * their candidacy with the Submit button.
 *
 * <p>All requirement checks shown here are cosmetic/realtime; the authoritative
 * validation lives in {@link ElectionManager#submitCandidacy}. Every empty slot
 * is filled and every click is cancelled by the router so items can never be
 * inserted or lost.
 */
public class RepublicElectionRegistrationGUI extends NationMenuBase {

    public static final String TITLE = "§8§lPresidential Registration";

    private static final Material FILLER = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
    private static final String BAR = "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";

    // Layout slots
    public static final int SLOT_INFO = 4;
    public static final int SLOT_PLAYTIME = 21;
    public static final int SLOT_LEVEL = 22;
    public static final int SLOT_FEE = 23;
    public static final int SLOT_RECORD = 30;
    public static final int SLOT_MESSAGE = 31;
    public static final int SLOT_AGREEMENT = 32;
    public static final int SLOT_BACK = 43;
    public static final int SLOT_SUBMIT = 49;

    /** Border filler slots as specified by the layout. */
    private static final int[] BORDER_SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 50, 51, 52, 53
    };

    public RepublicElectionRegistrationGUI(NationCore plugin) {
        super(plugin);
    }

    /**
     * Open the registration menu. Guards ensure it is only ever shown to a member
     * of a REPUBLIC nation whose election is currently in the registration phase.
     */
    public void open(Player player, Nation nation) {
        if (nation == null || nation.getType() != GovernmentType.REPUBLIC) {
            MessageUtils.send(player, "<red>Presidential registration is exclusive to Republic nations.</red>");
            return;
        }
        if (!nation.isMember(player.getUniqueId())) {
            MessageUtils.send(player, "<red>You are not a member of this nation.</red>");
            return;
        }
        Election election = nation.getElection();
        if (election == null || election.getCurrentPhase() != Election.ElectionPhase.REGISTRATION) {
            MessageUtils.send(player, "<red>The registration phase is not currently open.</red>");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54,
                LegacyComponentSerializer.legacySection().deserialize(TITLE));

        // Border + fill every remaining slot so nothing can be inserted.
        ItemStack filler = pane(FILLER);
        for (int slot : BORDER_SLOTS) {
            inv.setItem(slot, filler);
        }

        ElectionManager em = plugin.getElectionManager();
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        UUID uuid = player.getUniqueId();

        boolean alreadyFiled = election.getCandidates().containsKey(uuid);
        boolean agreementAccepted = isAgreementAccepted(uuid);

        boolean okPlaytime = em.meetsPlaytimeRequirement(player);
        boolean okLevel = em.meetsLevelRequirement(player);
        boolean okFee = em.meetsFeeRequirement(player);
        boolean okRecord = em.meetsCleanRecordRequirement(player);
        boolean okMessage = em.hasValidCampaignMessage(player);

        inv.setItem(SLOT_INFO, buildInfoCard(nation, election, alreadyFiled));
        inv.setItem(SLOT_PLAYTIME, buildPlaytimeCard(em, data, okPlaytime));
        inv.setItem(SLOT_LEVEL, buildLevelCard(em, player, okLevel));
        inv.setItem(SLOT_FEE, buildFeeCard(em, player, okFee));
        inv.setItem(SLOT_RECORD, buildRecordCard(em, okRecord));
        inv.setItem(SLOT_MESSAGE, buildMessageCard(data, okMessage));
        inv.setItem(SLOT_AGREEMENT, buildAgreementCard(em, agreementAccepted));
        inv.setItem(SLOT_SUBMIT, buildSubmitCard(okPlaytime, okLevel, okFee, okRecord, okMessage,
                agreementAccepted, alreadyFiled));
        inv.setItem(SLOT_BACK, buildBackCard());

        // Fill any interior gap left over (security: no empty, insertable slots).
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }

        player.openInventory(inv);
    }

    // ===== Card builders =====

    private ItemStack buildInfoCard(Nation nation, Election election, boolean alreadyFiled) {
        long remaining = plugin.getElectionManager().getPhaseRemainingTime(nation);
        int candidates = election.getCandidates().size();

        List<String> lore = new ArrayList<>();
        lore.add(BAR);
        lore.add("&7File your candidacy documents to run");
        lore.add("&7for &bPresident &7of &f" + nation.getName() + "&7.");
        lore.add(BAR);
        lore.add("&b&lPHASE STATUS");
        lore.add("&8• &7Phase: &aRegistration &7(open)");
        lore.add("&8• &7Time Left: &f" + formatRemaining(remaining));
        lore.add("&8• &7Candidates: &f" + candidates + " &8/ &f" + ElectionManager.REGISTRATION_CANDIDATE_CAP);
        lore.add(BAR);
        if (alreadyFiled) {
            lore.add("&a✔ You have already filed your candidacy.");
            lore.add("&7Await the campaign phase to begin.");
        } else {
            lore.add("&7Complete all &f6 documents &7below, then");
            lore.add("&7submit them with the &fNether Star&7.");
            lore.add("&7Registration closes automatically once");
            lore.add("&f" + ElectionManager.REGISTRATION_CANDIDATE_CAP + " members &7have filed.");
        }
        lore.add(BAR);
        return buildIcon(Material.GLOW_ITEM_FRAME, "&b&l📋 Presidential Registration", lore);
    }

    private ItemStack buildPlaytimeCard(ElectionManager em, PlayerData data, boolean met) {
        List<String> lore = new ArrayList<>();
        lore.add(BAR);
        lore.add("&7Proof of dedication — hours spent");
        lore.add("&7serving the community.");
        lore.add(BAR);
        lore.add("&7Required : &f" + (int) em.getRequiredPlaytimeHours() + " hours");
        lore.add("&7Current  : &f" + String.format("%.1f", data.getPlaytimeHours()) + " hours");
        lore.add(BAR);
        lore.add(statusLine(met));
        return berkas(met, "&bDocument · Playtime", lore);
    }

    private ItemStack buildLevelCard(ElectionManager em, Player player, boolean met) {
        List<String> lore = new ArrayList<>();
        lore.add(BAR);
        lore.add("&7Proof of experience — your total");
        lore.add("&7accumulated experience level.");
        lore.add(BAR);
        lore.add("&7Required : &fLevel " + em.getRequiredLevel());
        lore.add("&7Current  : &fLevel " + player.getLevel());
        lore.add(BAR);
        lore.add(statusLine(met));
        return berkas(met, "&bDocument · Experience Level", lore);
    }

    private ItemStack buildFeeCard(ElectionManager em, Player player, boolean met) {
        double fee = em.getRegistrationFee();
        List<String> lore = new ArrayList<>();
        lore.add(BAR);
        lore.add("&7The registration fee, deducted from");
        lore.add("&7your Vault balance upon submission.");
        lore.add(BAR);
        lore.add("&7Fee      : &6$" + formatMoney(fee));
        lore.add("&7Your Bal : " + (met ? "&a" : "&c") + "$"
                + formatMoney(plugin.getVaultHook().getBalance(player.getUniqueId())));
        lore.add(BAR);
        lore.add(statusLine(met));
        return berkas(met, "&bDocument · Registration Fee", lore);
    }

    private ItemStack buildRecordCard(ElectionManager em, boolean met) {
        List<String> lore = new ArrayList<>();
        lore.add(BAR);
        lore.add("&7A clean record — no server");
        lore.add("&7punishments (ban, mute, etc.)");
        lore.add("&7within the review window.");
        lore.add(BAR);
        lore.add("&7Review window : &f" + em.getNoPunishmentDays() + " days");
        lore.add("&7Record        : " + (met ? "&aClean" : "&cRecent punishment found"));
        lore.add(BAR);
        lore.add(statusLine(met));
        return berkas(met, "&bDocument · Clean Record", lore);
    }

    private ItemStack buildMessageCard(PlayerData data, boolean met) {
        List<String> lore = new ArrayList<>();
        lore.add(BAR);
        lore.add("&7Your official campaign speech,");
        lore.add("&7broadcast to citizens during the");
        lore.add("&7campaign phase.");
        lore.add(BAR);
        lore.add("&7Length     : &f" + plugin.getElectionManager().getCampaignMessageMinChars() + " - "
                + plugin.getElectionManager().getCampaignMessageMaxChars() + " characters");
        if (data.hasPresidentCampaignMessage()) {
            int len = ElectionManager.messageLength(data.getPresidentCampaignMessage());
            lore.add("&7Saved      : " + (met ? "&a" : "&c") + len + " characters");
            lore.add(BAR);
            lore.add("&7Preview:");
            lore.add("&f\"" + preview(data.getPresidentCampaignMessage()) + "&f\"");
        } else {
            lore.add("&7Saved      : &cEmpty");
        }
        lore.add(BAR);
        if (met) {
            lore.add(statusLine(true));
            lore.add("&e➜ Click to rewrite your message");
        } else {
            lore.add("&c✖ DOCUMENT INCOMPLETE");
            lore.add("&e➜ Click to write your campaign message");
        }
        return berkas(met, "&bDocument · Campaign Message", lore);
    }

    private ItemStack buildAgreementCard(ElectionManager em, boolean accepted) {
        double fee = em.getRegistrationFee();
        List<String> lore = new ArrayList<>();
        lore.add(BAR);
        lore.add("&7The Presidential Candidate's Oath.");
        lore.add("&7Read carefully before you accept.");
        lore.add(BAR);
        lore.add("&e① Victory Clause");
        lore.add("&7  Win the election and your &6$" + formatMoney(fee));
        lore.add("&7  fee is transferred to the &fstate treasury&7.");
        lore.add("");
        lore.add("&e② Defeat Clause");
        lore.add("&7  Lose and the fee is &cnot refunded &7— it");
        lore.add("&7  still flows into the &fstate treasury&7.");
        lore.add("");
        lore.add("&e③ Oath of Office");
        lore.add("&7  You vow to uphold every national law");
        lore.add("&7  and serve as an honorable President.");
        lore.add(BAR);
        if (accepted) {
            lore.add("&a✔ AGREEMENT ACCEPTED");
            lore.add("&e➜ Click again to withdraw your consent");
        } else {
            lore.add("&c✖ NOT YET ACCEPTED");
            lore.add("&e➜ Click to swear the oath and accept");
        }
        return berkas(accepted, "&bDocument · Candidacy Agreement", lore);
    }

    private ItemStack buildSubmitCard(boolean okPlaytime, boolean okLevel, boolean okFee,
                                      boolean okRecord, boolean okMessage, boolean okAgreement,
                                      boolean alreadyFiled) {
        int ready = (okPlaytime ? 1 : 0) + (okLevel ? 1 : 0) + (okFee ? 1 : 0)
                + (okRecord ? 1 : 0) + (okMessage ? 1 : 0) + (okAgreement ? 1 : 0);
        boolean allReady = ready == 6 && !alreadyFiled;

        List<String> lore = new ArrayList<>();
        lore.add(BAR);
        lore.add("&7Submit your six documents to enter");
        lore.add("&7the race as a presidential candidate.");
        lore.add(BAR);
        lore.add("&7Documents ready: &f" + ready + " &8/ &f6");
        lore.add(check(okPlaytime) + " Playtime");
        lore.add(check(okLevel) + " Experience Level");
        lore.add(check(okFee) + " Registration Fee");
        lore.add(check(okRecord) + " Clean Record");
        lore.add(check(okMessage) + " Campaign Message");
        lore.add(check(okAgreement) + " Candidacy Agreement");
        lore.add(BAR);
        if (alreadyFiled) {
            lore.add("&a✔ Candidacy already filed.");
        } else if (allReady) {
            lore.add("&a⚡ READY — Click to file your candidacy!");
        } else {
            lore.add("&c✖ Complete all six documents first.");
        }
        ItemStack item = buildIcon(Material.NETHER_STAR,
                (allReady ? "&a&l✦ Submit Candidacy" : "&7&l✦ Submit Candidacy"), lore);
        return allReady ? glowing(item) : item;
    }

    private ItemStack buildBackCard() {
        return buildIcon(Material.SPECTRAL_ARROW,
                "&e&l⏴ Back",
                BAR,
                "&7Return to the Republic Council.",
                BAR,
                "&eClick &7→ Back to main menu");
    }

    // ===== Helpers =====

    /** A document icon: enchanted (glowing) book when satisfied, plain book otherwise. */
    private ItemStack berkas(boolean met, String name, List<String> lore) {
        Material material = met ? Material.ENCHANTED_BOOK : Material.BOOK;
        String prefix = met ? "&a✔ " : "&c✖ ";
        return buildIcon(material, prefix + name, lore);
    }

    private String statusLine(boolean met) {
        return met ? "&a✔ REQUIREMENT MET" : "&c✖ REQUIREMENT NOT MET";
    }

    private String check(boolean met) {
        return met ? "&a  ✔" : "&c  ✖";
    }

    private String preview(String message) {
        if (message == null) return "";
        String clean = message.trim();
        int max = 42;
        if (clean.length() <= max) {
            return clean;
        }
        return clean.substring(0, max).trim() + "...";
    }

    private boolean isAgreementAccepted(UUID uuid) {
        return plugin.getGUIListener() != null
                && plugin.getGUIListener().registrationAgreementAccepted.contains(uuid);
    }
}
