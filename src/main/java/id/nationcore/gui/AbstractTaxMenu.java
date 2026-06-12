package id.nationcore.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import id.nationcore.NationCore;
import id.nationcore.managers.TaxManager;
import id.nationcore.models.Nation;
import id.nationcore.models.TaxInvoice;
import id.nationcore.models.TaxRecord.PlayerTaxProfile;
import id.nationcore.models.TaxRecord.TaxPayment;
import id.nationcore.utils.MessageUtils;

/**
 * Shared foundation for every per-nation Tax/Zakah menu.
 *
 * One uniquely named subclass exists per government type (each living in its
 * own UI package). All rendering and click logic for the invoice-based tax
 * system is centralized here; subclasses only supply theming (filler colour,
 * accent colour, menu title, terminology).
 *
 * The 54-slot layout is fixed by the specification:
 *   • Filler (per-nation stained glass) frames the menu.
 *   • Tax Statistics (glow_item_frame) on slot 4.
 *   • Pay Tax (emerald) on slot 21.
 *   • Toggle Auto Pay (redstone, glows while enabled) on slot 23.
 *   • Invoice List (knowledge_book) on slot 30.
 *   • Transaction Log (writable_book) on slot 32.
 *   • Back (spectral_arrow) on slot 43.
 */
@SuppressWarnings("deprecation")
public abstract class AbstractTaxMenu {

    protected final NationCore plugin;

    protected AbstractTaxMenu(NationCore plugin) {
        this.plugin = plugin;
    }

    // ── Theming hooks (implemented per nation) ──────────────────────────────

    /** Stained-glass-pane material used as filler, coloured per nation type. */
    protected abstract Material fillerMaterial();

    /** Accent colour code (e.g. "§b") used for headings. */
    protected abstract String accent();

    /** Unique inventory title for this menu. */
    public abstract String menuTitle();

    /** Terminology used in labels: "Tax" for most nations, "Zakah" for the Caliphate. */
    protected abstract String taxTerm();

    /** Name of the main menu this menu's Back button returns to. */
    protected abstract String backTargetName();

    // ── Layout ──────────────────────────────────────────────────────────────

    private static final int[] FILLER_SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36,
            44, 45, 46, 47, 48, 49, 50, 51, 52, 53
    };
    private static final int SLOT_STATS = 4;
    private static final int SLOT_PAY = 21;
    private static final int SLOT_AUTOPAY = 23;
    private static final int SLOT_INVOICES = 30;
    private static final int SLOT_LOG = 32;
    private static final int SLOT_BACK = 43;

    private static final String DIVIDER = "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";

    /** Maximum invoices rendered in the Invoice List lore before folding. */
    private static final int MAX_LISTED_INVOICES = 8;

    // ── Rendering ────────────────────────────────────────────────────────────

    public void open(Player player) {
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) {
            MessageUtils.send(player, "<red>You are not a member of any nation.</red>");
            player.closeInventory();
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, menuTitle());

        ItemStack filler = createItem(fillerMaterial(), " ", new ArrayList<>());
        for (int slot : FILLER_SLOTS) {
            inv.setItem(slot, filler);
        }

        PlayerTaxProfile profile = plugin.getTaxManager().getOrCreateProfile(player);

        inv.setItem(SLOT_STATS, buildStatsItem(player, nation, profile));
        inv.setItem(SLOT_PAY, buildPayItem(player, profile));
        inv.setItem(SLOT_AUTOPAY, buildAutoPayItem(profile));
        inv.setItem(SLOT_INVOICES, buildInvoiceListItem(profile));
        inv.setItem(SLOT_LOG, buildTransactionLogItem(profile));
        inv.setItem(SLOT_BACK, buildBackItem());

        player.openInventory(inv);
    }

    // ── Slot 4: Tax Statistics ───────────────────────────────────────────────

    private ItemStack buildStatsItem(Player player, Nation nation, PlayerTaxProfile profile) {
        double outstanding = profile.getOutstandingTotal();
        double nationalDebt = profile.getNationalDebtRemaining();
        int paidOnTime = Math.max(0, profile.getInvoicesPaid() - profile.getInvoicesPaidLate());

        List<String> lore = new ArrayList<>();
        lore.add(DIVIDER);
        lore.add("§7 Citizen §8: §f" + player.getName());
        lore.add("§7 Nation §8: §f" + nation.getName() + " §8[§f" + nation.getTag() + "§8]");
        lore.add("");
        lore.add("§7 Invoices Issued §8: §f" + profile.getInvoicesIssued());
        lore.add("§7 Paid On Time §8: §a" + paidOnTime);
        lore.add("§7 Paid With Penalty §8: §e" + profile.getInvoicesPaidLate());
        lore.add("§7 Turned Into Debt §8: §c" + profile.getInvoicesDefaulted());
        lore.add("");
        lore.add("§6 Total " + taxTerm() + " Paid §8: §a$" + MessageUtils.formatNumber(profile.getTotalAmountPaid()));
        lore.add("§7 Penalties Paid §8: §e$" + MessageUtils.formatNumber(profile.getTotalPenaltyPaid()));
        lore.add("§7 Debt Repaid §8: §c$" + MessageUtils.formatNumber(profile.getTotalDebtRepaid()));
        lore.add("");
        lore.add("§7 Outstanding §8: " + (outstanding > 0
                ? "§c$" + MessageUtils.formatNumber(outstanding)
                : "§a$0"));
        if (nationalDebt > 0) {
            lore.add("§4 National Debt §8: §4$" + MessageUtils.formatNumber(nationalDebt));
        }
        lore.add("§7 Auto Pay §8: " + (profile.isAutoPay() ? "§aENABLED" : "§cDISABLED"));
        lore.add("§7 Next Billing §8: §f" + formatNextBilling());
        lore.add(DIVIDER);

        ItemStack item = createItem(Material.GLOW_ITEM_FRAME,
                accent() + "§l📊 " + taxTerm() + " Statistics", lore);
        addGlow(item);
        return item;
    }

    // ── Slot 21: Pay Tax ─────────────────────────────────────────────────────

    private ItemStack buildPayItem(Player player, PlayerTaxProfile profile) {
        List<TaxInvoice> outstanding = profile.getOutstandingInvoices();
        double due = profile.getOutstandingTotal();
        double balance = plugin.getVaultHook().getBalance(player.getUniqueId());

        List<String> lore = new ArrayList<>();
        lore.add(DIVIDER);
        lore.add("§7Settle every outstanding " + taxTerm().toLowerCase() + " invoice");
        lore.add("§7in a single payment. Funds go to your");
        lore.add("§7nation's treasury.");
        lore.add("");
        lore.add("§7 Outstanding Invoices §8: §f" + outstanding.size());
        lore.add("§7 Total Due §8: " + (due > 0 ? "§c$" + MessageUtils.formatNumber(due) : "§a$0"));
        lore.add("§7 Your Balance §8: §f$" + MessageUtils.formatNumber(balance));
        lore.add(DIVIDER);
        if (outstanding.isEmpty()) {
            lore.add("§aAll invoices settled — nothing to pay!");
        } else {
            lore.add("§eClick §7→ Pay all invoices");
        }

        return createItem(Material.EMERALD, "§a§l💰 Pay " + taxTerm(), lore);
    }

    // ── Slot 23: Toggle Auto Pay ─────────────────────────────────────────────

    private ItemStack buildAutoPayItem(PlayerTaxProfile profile) {
        boolean on = profile.isAutoPay();

        List<String> lore = new ArrayList<>();
        lore.add(DIVIDER);
        lore.add("§7While enabled, every future invoice is");
        lore.add("§7paid automatically from your Vault");
        lore.add("§7balance the moment it is issued.");
        lore.add("");
        lore.add("§7 Status §8: " + (on ? "§a§lENABLED" : "§c§lDISABLED"));
        lore.add(DIVIDER);
        lore.add("§eClick §7→ Turn Auto Pay " + (on ? "§cOFF" : "§aON"));

        ItemStack item = createItem(Material.REDSTONE,
                "§c§l⚡ Auto Pay §8[" + (on ? "§aON" : "§cOFF") + "§8]", lore);
        if (on) {
            addGlow(item);
        }
        return item;
    }

    // ── Slot 30: Invoice List ────────────────────────────────────────────────

    private ItemStack buildInvoiceListItem(PlayerTaxProfile profile) {
        List<TaxInvoice> outstanding = profile.getOutstandingInvoices();

        List<String> lore = new ArrayList<>();
        lore.add(DIVIDER);

        if (outstanding.isEmpty()) {
            lore.add("§aNo active invoices.");
            lore.add("§7You are all caught up!");
        } else {
            lore.add("§7Active invoices awaiting payment:");
            lore.add("");
            int shown = 0;
            for (TaxInvoice invoice : outstanding) {
                if (shown >= MAX_LISTED_INVOICES) break;
                lore.add("§f " + invoice.getId() + " §8• "
                        + invoice.getStatus().getColor() + "§l" + invoice.getStatus().getDisplayName());
                lore.add("§8   └ §7Due §8: §6$" + MessageUtils.formatNumber(invoice.getRemaining())
                        + " §8• " + describeEscalation(invoice));
                shown++;
            }
            if (outstanding.size() > shown) {
                lore.add("§8   ... and §f" + (outstanding.size() - shown) + " §8more invoice(s)");
            }
            lore.add("");
            lore.add("§7 Total Due §8: §c$" + MessageUtils.formatNumber(profile.getOutstandingTotal()));
        }
        lore.add(DIVIDER);

        return createItem(Material.KNOWLEDGE_BOOK, accent() + "§l📑 Invoice List", lore);
    }

    private String describeEscalation(TaxInvoice invoice) {
        return switch (invoice.getStatus()) {
            case ACTIVE -> "§7doubles in §e" + MessageUtils.formatTime(invoice.getMillisUntilOverdue());
            case OVERDUE -> "§7debt in §c" + MessageUtils.formatTime(invoice.getMillisUntilDebt());
            case NATIONAL_DEBT -> "§4forced collection active";
            default -> "§asettled";
        };
    }

    // ── Slot 32: Transaction Log ─────────────────────────────────────────────

    private ItemStack buildTransactionLogItem(PlayerTaxProfile profile) {
        List<TaxPayment> recent = profile.getRecentPayments(5);

        List<String> lore = new ArrayList<>();
        lore.add(DIVIDER);
        lore.add("§7Your latest §f5 §7" + taxTerm().toLowerCase() + " payments:");
        lore.add("");

        if (recent.isEmpty()) {
            lore.add("§8(no payments recorded yet)");
        } else {
            for (TaxPayment payment : recent) {
                lore.add("§a +$" + MessageUtils.formatNumber(payment.getAmount())
                        + " §8• §f" + payment.getInvoiceId());
                lore.add("§8   └ §7" + payment.getMethod().getDisplayName()
                        + " §8• §f" + MessageUtils.formatTime(
                                System.currentTimeMillis() - payment.getTimestamp()) + " ago");
            }
        }
        lore.add(DIVIDER);

        return createItem(Material.WRITABLE_BOOK, accent() + "§l📜 Transaction Log", lore);
    }

    // ── Slot 43: Back ────────────────────────────────────────────────────────

    private ItemStack buildBackItem() {
        List<String> lore = new ArrayList<>();
        lore.add(DIVIDER);
        lore.add("§7Return to the " + backTargetName() + ".");
        lore.add(DIVIDER);
        lore.add("§eClick §7→ Main Menu");
        return createItem(Material.SPECTRAL_ARROW, "§e§l⚐ Back", lore);
    }

    // ── Click handling ───────────────────────────────────────────────────────

    /**
     * Handles a click inside this menu. Raw slots are used so clicks in the
     * player's own inventory (raw ≥ 54) can never trigger menu actions.
     */
    public void handleClick(GUIListener gui, Player player, ItemStack clicked, int rawSlot) {
        if (rawSlot < 0 || rawSlot >= 54) return;
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == fillerMaterial()) return;

        MessageUtils.playSound(player, Sound.UI_BUTTON_CLICK);

        if (rawSlot == SLOT_BACK && clicked.getType() == Material.SPECTRAL_ARROW) {
            gui.mainMenuRouter.openFor(player);
            return;
        }

        if (rawSlot == SLOT_PAY && clicked.getType() == Material.EMERALD) {
            handlePayAll(player);
            return;
        }

        if (rawSlot == SLOT_AUTOPAY && clicked.getType() == Material.REDSTONE) {
            boolean on = plugin.getTaxManager().toggleAutoPay(player);
            if (on) {
                MessageUtils.send(player, "<green>⚡ Auto Pay <bold>enabled</bold> — future invoices will be "
                        + "paid automatically from your balance.");
                MessageUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);
            } else {
                MessageUtils.send(player, "<red>⚡ Auto Pay <bold>disabled</bold> — you must settle future "
                        + "invoices manually.");
                MessageUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS);
            }
            open(player); // refresh
        }
    }

    private void handlePayAll(Player player) {
        TaxManager.PaymentResult result = plugin.getTaxManager().payAllInvoices(player);

        if (result.nothingToPay()) {
            MessageUtils.send(player, "<green>You have no outstanding " + taxTerm().toLowerCase()
                    + " invoices. Nothing to pay!");
            MessageUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);
        } else if (result.invoicesPaid == 0) {
            MessageUtils.send(player, "<red>Insufficient balance! You need <gold>$"
                    + MessageUtils.formatNumber(result.remainingDue) + "</gold> to settle <white>"
                    + result.invoicesRemaining + "</white> invoice(s).");
            MessageUtils.playSound(player, Sound.ENTITY_VILLAGER_NO);
        } else if (result.invoicesRemaining > 0) {
            MessageUtils.send(player, "<yellow>Paid <white>" + result.invoicesPaid
                    + "</white> invoice(s) for <gold>$" + MessageUtils.formatNumber(result.amountPaid)
                    + "</gold>. <red>Still owing <gold>$" + MessageUtils.formatNumber(result.remainingDue)
                    + "</gold> on <white>" + result.invoicesRemaining + "</white> invoice(s).");
            MessageUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL);
        } else {
            MessageUtils.send(player, "<green>✔ Paid <white>" + result.invoicesPaid
                    + "</white> invoice(s) totaling <gold>$" + MessageUtils.formatNumber(result.amountPaid)
                    + "</gold>. All " + taxTerm().toLowerCase() + " settled — thank you, citizen!");
            MessageUtils.playSound(player, Sound.ENTITY_PLAYER_LEVELUP);
        }

        open(player); // refresh
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    private String formatNextBilling() {
        long remaining = plugin.getTaxManager().getTimeUntilNextCycle();
        if (remaining <= 0) {
            return "pending...";
        }
        return MessageUtils.formatTime(remaining);
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void addGlow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
    }
}
