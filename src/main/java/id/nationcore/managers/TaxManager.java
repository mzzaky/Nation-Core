package id.nationcore.managers;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import id.nationcore.NationCore;
import id.nationcore.models.Nation;
import id.nationcore.models.PlayerData;
import id.nationcore.models.TaxInvoice;
import id.nationcore.models.TaxInvoice.InvoiceStatus;
import id.nationcore.models.TaxRecord;
import id.nationcore.models.TaxRecord.PaymentMethod;
import id.nationcore.models.TaxRecord.PlayerTaxProfile;
import id.nationcore.models.TaxRecord.TaxPayment;
import id.nationcore.models.Treasury.TransactionType;
import id.nationcore.utils.MessageUtils;

/**
 * Invoice-based nation tax engine.
 *
 * Billing: every billing cycle (1 real-life day) the system issues a $50
 * invoice to every active nation member. An invoice left unpaid for 1 day
 * doubles in value (penalty); left unpaid for 3 days from issue it becomes
 * permanent national debt which the state force-collects from any balance
 * the player ever holds — even after leaving the issuing nation.
 *
 * Paid invoices transfer the money to the issuing nation's treasury.
 */
public class TaxManager {

    /** Face value of one tax invoice per billing cycle. */
    public static final double INVOICE_AMOUNT = 50.0;

    /** Billing cycle length: 1 real-life day. */
    public static final long BILLING_CYCLE_MILLIS = 24L * 60 * 60 * 1000;

    /** Players offline longer than this are skipped at invoice generation. */
    private static final long INACTIVE_SKIP_MILLIS = 3L * 24 * 60 * 60 * 1000;

    private final NationCore plugin;
    private int taskId = -1;

    public TaxManager(NationCore plugin) {
        this.plugin = plugin;
    }

    public TaxRecord getTaxRecord() {
        return plugin.getDataManager().getTaxRecord();
    }

    // =====================================================================
    // Scheduler
    // =====================================================================

    public void startTaxScheduler() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L * 60, 20L * 60).getTaskId();
    }

    public void stopTaxScheduler() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    /** Runs every minute: escalate invoices, seize national debt, bill new cycle. */
    public void tick() {
        if (!isEnabled()) return;
        updateInvoiceStatuses();
        collectAllNationalDebts();
        checkBillingCycle();
    }

    // =====================================================================
    // Invoice generation (billing cycle)
    // =====================================================================

    private void checkBillingCycle() {
        TaxRecord record = getTaxRecord();
        long now = System.currentTimeMillis();

        // First-time initialization: anchor the cycle, bill one cycle later.
        if (record.getLastCycleTime() == 0) {
            record.setLastCycleTime(now);
            return;
        }

        if (now - record.getLastCycleTime() >= BILLING_CYCLE_MILLIS) {
            runBillingCycle();
        }
    }

    /** Issues one $50 invoice to every active nation member, then bills NPCs. */
    public void runBillingCycle() {
        TaxRecord record = getTaxRecord();
        long inactiveThreshold = System.currentTimeMillis() - INACTIVE_SKIP_MILLIS;

        int issuedCount = 0;
        int autoPaidCount = 0;
        double autoPaidTotal = 0;

        for (PlayerData playerData : plugin.getDataManager().getAllPlayerData()) {
            UUID playerUUID = playerData.getUuid();

            // Skip long-offline players so bills don't pile up while they are gone.
            if (playerData.getLastSeen() < inactiveThreshold) {
                continue;
            }

            Nation nation = plugin.getNationManager().getNationOf(playerUUID);
            if (nation == null) {
                continue;
            }

            PlayerTaxProfile profile = record.getOrCreateProfile(playerUUID.toString(), playerData.getName());
            TaxInvoice invoice = new TaxInvoice(record.nextInvoiceId(), nation.getId(), nation.getName(),
                    INVOICE_AMOUNT);
            profile.addInvoice(invoice);
            issuedCount++;

            boolean autoPaid = false;
            if (profile.isAutoPay()) {
                autoPaid = tryAutoPay(playerUUID, profile, invoice);
                if (autoPaid) {
                    autoPaidCount++;
                    autoPaidTotal += invoice.getTotalDue();
                }
            }

            Player onlinePlayer = Bukkit.getPlayer(playerUUID);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                if (autoPaid) {
                    MessageUtils.send(onlinePlayer, "<green>📑 Tax invoice <white>" + invoice.getId()
                            + "</white> of <gold>$" + MessageUtils.formatNumber(INVOICE_AMOUNT)
                            + "</gold> was paid automatically (Auto Pay).");
                    MessageUtils.playSound(onlinePlayer, Sound.BLOCK_NOTE_BLOCK_PLING);
                } else {
                    MessageUtils.send(onlinePlayer, "<yellow>📑 New tax invoice <white>" + invoice.getId()
                            + "</white> of <gold>$" + MessageUtils.formatNumber(INVOICE_AMOUNT)
                            + "</gold> issued by <white>" + nation.getName()
                            + "</white>. Pay within <white>1 day</white> or the bill doubles.");
                    MessageUtils.playSound(onlinePlayer, Sound.BLOCK_NOTE_BLOCK_BELL);
                }
            }
        }

        // === Fake Member (NPC) billing — collected directly each cycle ===
        int npcTaxedCount = 0;
        int npcDebtCount = 0;
        double npcCollected = 0;

        for (Nation nation : plugin.getNationManager().getAllNations()) {
            if (nation.getAllFakeMembers().isEmpty()) continue;

            for (id.nationcore.models.FakeMember npc : nation.getAllFakeMembers()) {
                double paid = plugin.getFakeMemberManager().collectTax(npc, nation, INVOICE_AMOUNT);
                npcCollected += paid;
                if (paid < INVOICE_AMOUNT) {
                    npcDebtCount++;
                } else {
                    npcTaxedCount++;
                }
            }
        }

        if (npcTaxedCount > 0 || npcDebtCount > 0) {
            plugin.getDataManager().saveNations();
        }

        record.incrementCycle();

        if (issuedCount > 0 || npcTaxedCount > 0 || npcDebtCount > 0) {
            MessageUtils.broadcast("<gold>=======================================================");
            MessageUtils.broadcast("<yellow>          NATION TAX — INVOICE BILLING");
            MessageUtils.broadcast("<gold>=======================================================");
            MessageUtils.broadcast("<gray>Invoice Amount: <gold>$" + MessageUtils.formatNumber(INVOICE_AMOUNT)
                    + " <gray>per citizen");
            MessageUtils.broadcast("<gray>Invoices Issued: <green>" + issuedCount);
            if (autoPaidCount > 0) {
                MessageUtils.broadcast("<gray>Auto-Paid: <aqua>" + autoPaidCount + " <gray>(<gold>$"
                        + MessageUtils.formatNumber(autoPaidTotal) + "</gold>)");
            }
            if (npcTaxedCount > 0 || npcDebtCount > 0) {
                MessageUtils.broadcast("<gray>NPC Members Taxed: <green>" + npcTaxedCount
                        + " <gray>| NPC Debt: <red>" + npcDebtCount);
                MessageUtils.broadcast("<gray>NPC Total Paid: <gold>$" + MessageUtils.formatNumber(npcCollected));
            }
            MessageUtils.broadcast("<gray>Due Time: <white>1 day</white> <gray>— after that the bill doubles!");
            MessageUtils.broadcast("<gold>=======================================================");
        }

        plugin.getLogger().info("Tax billing cycle #" + record.getTotalCycles() + " completed: "
                + issuedCount + " invoices issued, " + autoPaidCount + " auto-paid. "
                + "NPC: " + npcTaxedCount + " taxed, " + npcDebtCount + " in debt.");
    }

    /** Attempts to settle a freshly issued invoice from the player's balance. */
    private boolean tryAutoPay(UUID playerUUID, PlayerTaxProfile profile, TaxInvoice invoice) {
        double due = invoice.getRemaining();
        if (due <= 0) return false;
        if (!plugin.getVaultHook().has(playerUUID, due)) {
            return false;
        }
        plugin.getVaultHook().withdraw(playerUUID, due);
        applyPayment(playerUUID, profile, invoice, due, PaymentMethod.AUTO_PAY);
        return true;
    }

    // =====================================================================
    // Status escalation: ACTIVE → OVERDUE (2x) → NATIONAL_DEBT (permanent)
    // =====================================================================

    private void updateInvoiceStatuses() {
        TaxRecord record = getTaxRecord();
        long now = System.currentTimeMillis();

        for (Map.Entry<String, PlayerTaxProfile> entry : record.getPlayerProfiles().entrySet()) {
            PlayerTaxProfile profile = entry.getValue();
            UUID playerUUID = parseUUID(entry.getKey());

            for (TaxInvoice invoice : profile.getInvoices()) {
                if (!invoice.isOutstanding()) continue;

                // Invoices of dissolved nations are voided — there is no
                // treasury left to receive the money.
                if (plugin.getNationManager().getNation(invoice.getNationId()) == null) {
                    invoice.setStatus(InvoiceStatus.PAID);
                    invoice.setPaidAt(now);
                    plugin.getLogger().info("Tax invoice " + invoice.getId() + " voided — nation "
                            + invoice.getNationName() + " no longer exists.");
                    continue;
                }

                long age = now - invoice.getCreatedAt();

                if (invoice.getStatus() == InvoiceStatus.ACTIVE && age >= TaxInvoice.DUE_PERIOD_MILLIS) {
                    invoice.setStatus(InvoiceStatus.OVERDUE);
                    invoice.setPenaltyApplied(true);
                    notifyOverdue(playerUUID, invoice);
                }

                if (invoice.getStatus() == InvoiceStatus.OVERDUE && age >= TaxInvoice.DEBT_PERIOD_MILLIS) {
                    invoice.setStatus(InvoiceStatus.NATIONAL_DEBT);
                    profile.setInvoicesDefaulted(profile.getInvoicesDefaulted() + 1);
                    notifyNationalDebt(playerUUID, invoice);
                }
            }
        }
    }

    private void notifyOverdue(UUID playerUUID, TaxInvoice invoice) {
        if (playerUUID == null) return;
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) return;
        MessageUtils.send(player, "<red>⚠ Tax invoice <white>" + invoice.getId()
                + "</white> is overdue! The bill has <bold>doubled</bold> to <gold>$"
                + MessageUtils.formatNumber(invoice.getTotalDue())
                + "</gold>. <red>Settle it within <white>"
                + MessageUtils.formatTime(invoice.getMillisUntilDebt())
                + "</white> or it becomes permanent national debt.");
        MessageUtils.playSound(player, Sound.ENTITY_VILLAGER_NO);
    }

    private void notifyNationalDebt(UUID playerUUID, TaxInvoice invoice) {
        if (playerUUID == null) return;
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) return;
        MessageUtils.send(player, "<dark_red><bold>NATIONAL DEBT!</bold></dark_red> <red>Tax invoice <white>"
                + invoice.getId() + "</white> of <gold>$" + MessageUtils.formatNumber(invoice.getTotalDue())
                + "</gold> has been permanently recorded as national debt. The state will seize any balance "
                + "you hold until it is settled — even if you leave the nation.");
        MessageUtils.sendTitle(player,
                "<dark_red><bold>NATIONAL DEBT",
                "<red>Invoice " + invoice.getId() + " will be force-collected!",
                10, 60, 20);
        MessageUtils.playSound(player, Sound.ENTITY_WITHER_SPAWN);
    }

    // =====================================================================
    // National debt — forced bypass collection
    // =====================================================================

    private void collectAllNationalDebts() {
        TaxRecord record = getTaxRecord();
        for (String uuidStr : new ArrayList<>(record.getPlayerProfiles().keySet())) {
            UUID playerUUID = parseUUID(uuidStr);
            if (playerUUID != null) {
                collectNationalDebt(playerUUID);
            }
        }
    }

    /**
     * Force-collects outstanding national debt from whatever balance the
     * player currently holds (bypass — partial seizure allowed). Works for
     * offline players and players who already left the issuing nation.
     * Also called on player join so freshly earned money is seized promptly.
     */
    public void collectNationalDebt(UUID playerUUID) {
        TaxRecord record = getTaxRecord();
        PlayerTaxProfile profile = record.getProfile(playerUUID.toString());
        if (profile == null || !profile.hasNationalDebt()) return;

        double balance = plugin.getVaultHook().getBalance(playerUUID);
        if (balance < 0.01) return;

        double seizedTotal = 0;
        for (TaxInvoice invoice : profile.getOutstandingInvoices()) {
            if (invoice.getStatus() != InvoiceStatus.NATIONAL_DEBT) continue;
            if (balance < 0.01) break;

            double seize = Math.min(balance, invoice.getRemaining());
            if (seize < 0.01) continue;

            if (!plugin.getVaultHook().withdraw(playerUUID, seize)) {
                continue;
            }
            balance -= seize;
            seizedTotal += seize;
            applyPayment(playerUUID, profile, invoice, seize, PaymentMethod.FORCED);
        }

        if (seizedTotal > 0) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null && player.isOnline()) {
                double remaining = profile.getNationalDebtRemaining();
                MessageUtils.send(player, "<dark_red>⚖ The state seized <gold>$"
                        + MessageUtils.formatNumber(seizedTotal) + "</gold> from your balance toward national debt."
                        + (remaining > 0
                                ? " <red>Remaining debt: <gold>$" + MessageUtils.formatNumber(remaining) + "</gold>"
                                : " <green>Your national debt is now fully settled!"));
                MessageUtils.playSound(player, remaining > 0 ? Sound.ENTITY_VILLAGER_NO : Sound.ENTITY_PLAYER_LEVELUP);
            }
        }
    }

    // =====================================================================
    // Payments
    // =====================================================================

    /** Outcome of a "pay all invoices" request, consumed by the tax menus. */
    public static class PaymentResult {
        public final int invoicesPaid;
        public final double amountPaid;
        public final int invoicesRemaining;
        public final double remainingDue;

        public PaymentResult(int invoicesPaid, double amountPaid, int invoicesRemaining, double remainingDue) {
            this.invoicesPaid = invoicesPaid;
            this.amountPaid = amountPaid;
            this.invoicesRemaining = invoicesRemaining;
            this.remainingDue = remainingDue;
        }

        public boolean nothingToPay() {
            return invoicesPaid == 0 && invoicesRemaining == 0;
        }
    }

    /**
     * Pays every outstanding invoice the player can afford, oldest first.
     * Money is withdrawn from the player's Vault balance and deposited into
     * the issuing nation's treasury.
     */
    public PaymentResult payAllInvoices(Player player) {
        PlayerTaxProfile profile = getOrCreateProfile(player);
        List<TaxInvoice> outstanding = profile.getOutstandingInvoices();

        if (outstanding.isEmpty()) {
            return new PaymentResult(0, 0, 0, 0);
        }

        int paidCount = 0;
        double paidTotal = 0;

        for (TaxInvoice invoice : outstanding) {
            double remaining = invoice.getRemaining();
            if (remaining <= 0) continue;
            if (!plugin.getVaultHook().has(player.getUniqueId(), remaining)) {
                break; // oldest-first; stop at the first bill the player cannot afford
            }
            plugin.getVaultHook().withdraw(player.getUniqueId(), remaining);
            applyPayment(player.getUniqueId(), profile, invoice, remaining, PaymentMethod.MANUAL);
            paidCount++;
            paidTotal += remaining;
        }

        return new PaymentResult(paidCount, paidTotal,
                profile.getOutstandingInvoices().size(), profile.getOutstandingTotal());
    }

    /**
     * Books a successful Vault withdrawal against an invoice: deposits the
     * money into the issuing nation's treasury, updates invoice state,
     * statistics and the player's transaction log.
     */
    private void applyPayment(UUID playerUUID, PlayerTaxProfile profile, TaxInvoice invoice,
                              double amount, PaymentMethod method) {
        TaxRecord record = getTaxRecord();
        InvoiceStatus statusBefore = invoice.getStatus();

        Nation nation = plugin.getNationManager().getNation(invoice.getNationId());
        if (nation != null) {
            String description = switch (method) {
                case FORCED -> "National debt seizure (" + invoice.getId() + ") from " + profile.getPlayerName();
                case AUTO_PAY -> "Auto-paid tax invoice " + invoice.getId() + " from " + profile.getPlayerName();
                default -> "Tax invoice " + invoice.getId() + " paid by " + profile.getPlayerName();
            };
            plugin.getTreasuryManager().deposit(nation, TransactionType.TAX_INCOME, amount, description, playerUUID);
        }

        invoice.setPaidAmount(invoice.getPaidAmount() + amount);
        boolean settled = invoice.getRemaining() <= 0.0001;
        if (settled) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(System.currentTimeMillis());
        }

        profile.setTotalAmountPaid(profile.getTotalAmountPaid() + amount);

        if (method == PaymentMethod.FORCED || statusBefore == InvoiceStatus.NATIONAL_DEBT) {
            profile.setTotalDebtRepaid(profile.getTotalDebtRepaid() + amount);
            record.addDebtCollected(amount);
        } else {
            double penaltyPortion = invoice.isPenaltyApplied() ? Math.max(0, amount - invoice.getBaseAmount()) : 0;
            record.addTaxCollected(amount - penaltyPortion);
            if (penaltyPortion > 0) {
                record.addPenaltyCollected(penaltyPortion);
                profile.setTotalPenaltyPaid(profile.getTotalPenaltyPaid() + penaltyPortion);
            }
            if (settled) {
                profile.setInvoicesPaid(profile.getInvoicesPaid() + 1);
                if (invoice.isPenaltyApplied()) {
                    profile.setInvoicesPaidLate(profile.getInvoicesPaidLate() + 1);
                }
            }
        }

        String logNote = switch (method) {
            case FORCED -> settled ? "Debt seized in full" : "Partial debt seizure";
            case AUTO_PAY -> "Paid automatically";
            default -> statusBefore == InvoiceStatus.NATIONAL_DEBT ? "Debt settled manually"
                    : (invoice.isPenaltyApplied() ? "Paid with penalty" : "Paid on time");
        };
        profile.recordPayment(new TaxPayment(invoice.getId(), amount, method, logNote));
    }

    // =====================================================================
    // Auto Pay
    // =====================================================================

    /** Flips the player's Auto Pay preference and returns the new state. */
    public boolean toggleAutoPay(Player player) {
        PlayerTaxProfile profile = getOrCreateProfile(player);
        profile.setAutoPay(!profile.isAutoPay());
        return profile.isAutoPay();
    }

    // =====================================================================
    // Accessors / helpers
    // =====================================================================

    public PlayerTaxProfile getOrCreateProfile(Player player) {
        return getTaxRecord().getOrCreateProfile(player.getUniqueId().toString(), player.getName());
    }

    public PlayerTaxProfile getProfile(UUID playerUUID) {
        return getTaxRecord().getProfile(playerUUID.toString());
    }

    public boolean isEnabled() {
        return getTaxRecord().isEnabled();
    }

    public double getInvoiceAmount() {
        return INVOICE_AMOUNT;
    }

    public long getNextCycleTime() {
        TaxRecord record = getTaxRecord();
        if (record.getLastCycleTime() == 0) return 0;
        return record.getLastCycleTime() + BILLING_CYCLE_MILLIS;
    }

    public long getTimeUntilNextCycle() {
        long next = getNextCycleTime();
        if (next == 0) return 0;
        return Math.max(0, next - System.currentTimeMillis());
    }

    /** Total national debt still owed across all players. */
    public double getTotalNationalDebt() {
        double total = 0;
        for (PlayerTaxProfile profile : getTaxRecord().getPlayerProfiles().values()) {
            total += profile.getNationalDebtRemaining();
        }
        return total;
    }

    /** Number of players carrying unsettled national debt. */
    public int getDebtorCount() {
        int count = 0;
        for (PlayerTaxProfile profile : getTaxRecord().getPlayerProfiles().values()) {
            if (profile.hasNationalDebt()) count++;
        }
        return count;
    }

    private UUID parseUUID(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
