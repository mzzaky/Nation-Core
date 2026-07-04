package id.nationcore.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import id.nationcore.NationCore;
import id.nationcore.models.Government;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.Nation;
import id.nationcore.models.PlayerData;
import id.nationcore.models.Treasury;
import id.nationcore.models.TaxInvoice;
import id.nationcore.models.TaxRecord;
import id.nationcore.managers.TaxManager;
import id.nationcore.utils.MessageUtils;

public class NationCommand implements CommandExecutor, TabCompleter {

    private final NationCore plugin;
    private final Map<UUID, Runnable> pendingConfirmations = new HashMap<>();

    public NationCommand(NationCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "menu":
            case "gui":
                handleMainMenu(sender);
                break;
            case "help":
                showHelp(sender);
                break;
            case "info":
            case "status":
                showGovernmentInfo(sender);
                break;
            case "treasury":
                handleTreasury(sender, args);
                break;
            case "stats":
                showPlayerStats(sender, args);
                break;
            case "admin":
                handleAdmin(sender, args);
                break;
            case "hub":
                handleHub(sender);
                break;
            case "create":
                handleCreateNation(sender, args);
                break;
            case "nation":
                handleNationInfo(sender, args);
                break;
            case "leave":
                handleNationLeave(sender);
                break;
            case "disband":
                handleNationDisband(sender);
                break;
            case "confirm":
                handleConfirm(sender);
                break;
            case "capital":
                handleCapital(sender, args);
                break;
            case "diplomacy":
            case "diplo":
                handleDiplomacy(sender, args);
                break;

            default:
                MessageUtils.send(sender, "general.unknown_command");
                break;
        }

        return true;
    }

    // === MULTI-NATION SYSTEM (since v1.5) ===

    private void handleHub(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtils.send(sender, "general.player_only");
            return;
        }
        plugin.getGUIListener().openHubGUI(player);
    }

    private void handleCreateNation(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.send(sender, "general.player_only");
            return;
        }
        if (args.length < 2) {
            plugin.getGUIListener().openCreateNationGUI(player);
            return;
        }
        GovernmentType type = GovernmentType.fromString(args[1]);
        if (type == null) {
            MessageUtils.send(player,
                    "<red>Government type not recognized. Options: <yellow>republic, communist</yellow>");
            return;
        }
        if (args.length < 3) {
            plugin.getNationManager().setPendingCreation(player.getUniqueId(), type);
            MessageUtils.send(player, "<yellow>You chose " + type.getColoredName() + "</yellow>");
            MessageUtils.send(player, "<gold>Please type your nation name in chat now.</gold>");
            return;
        }

        String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        var res = plugin.getNationManager().createNation(player, name, type);
        MessageUtils.send(player, (res.isSuccess() ? "<green>" : "<red>") + res.getMessage() +
                (res.isSuccess() ? "</green>" : "</red>"));
    }

    private void handleNationInfo(CommandSender sender, String[] args) {
        Nation nation;
        if (args.length >= 2) {
            String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            nation = plugin.getNationManager().getNationByName(name);
            if (nation == null) {
                MessageUtils.send(sender, "<red>Nation '" + name + "' not found.</red>");
                return;
            }
        } else if (sender instanceof Player p) {
            nation = plugin.getNationManager().getNationOf(p.getUniqueId());
            if (nation == null) {
                MessageUtils.send(sender, "<red>You are not in a nation. Use: /nc nation <name></red>");
                return;
            }
        } else {
            MessageUtils.send(sender, "<red>Usage: /nc nation <name></red>");
            return;
        }

        MessageUtils.send(sender, "<gold>═══ Nation Info: " + nation.getName() + " ═══</gold>");
        MessageUtils.send(sender, "<gray>Type: " + nation.getType().getColoredName());
        String leaderName = nation.getLeaderUUID() != null ? Bukkit.getOfflinePlayer(nation.getLeaderUUID()).getName() : "None";
        MessageUtils.send(sender, "<gray>Leader: <white>" + leaderName);
        MessageUtils.send(sender, "<gray>Members: <white>" + nation.getMemberCount());
        MessageUtils.send(sender, "<gray>Treasury Balance: <green>$" + MessageUtils.formatNumber(nation.getTreasury().getBalance()));
        if (nation.hasCapital()) {
            var cap = nation.getCapital();
            MessageUtils.send(sender, "<gray>Capital Chunk: <white>" + cap.getWorld() + " (" + (((int) cap.getX()) >> 4) + ", " + (((int) cap.getZ()) >> 4) + ")");
        } else {
            MessageUtils.send(sender, "<gray>Capital: <yellow>Not claimed</yellow>");
        }
    }

    private void handleNationLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtils.send(sender, "general.player_only");
            return;
        }
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) {
            MessageUtils.send(player, "<red>You are not in a nation.</red>");
            return;
        }

        if (player.getUniqueId().equals(nation.getLeaderUUID())) {
            pendingConfirmations.put(player.getUniqueId(), () -> {
                plugin.getNationManager().disbandNation(player);
                MessageUtils.send(player, "<green>You disbanded your nation because you were the leader.</green>");
            });
            MessageUtils.send(player, "<yellow>WARNING: You are the leader of " + nation.getName() + ". Leaving will DISBAND the nation!</yellow>");
            MessageUtils.send(player, "<green>Type <gold>/dc confirm</gold> to proceed.</green>");
            return;
        }

        var res = plugin.getNationManager().leaveNation(player);
        MessageUtils.send(player, (res.isSuccess() ? "<green>" : "<red>") + res.getMessage() +
                (res.isSuccess() ? "</green>" : "</red>"));
    }

    private void handleNationDisband(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtils.send(sender, "general.player_only");
            return;
        }
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) {
            MessageUtils.send(player, "<red>You are not in a nation.</red>");
            return;
        }
        if (!player.getUniqueId().equals(nation.getLeaderUUID()) && !player.hasPermission("nation.admin")) {
            MessageUtils.send(player, "<red>Only the nation leader or an admin can disband the nation.</red>");
            return;
        }

        pendingConfirmations.put(player.getUniqueId(), () -> {
            plugin.getNationManager().disbandNation(player);
            MessageUtils.send(player, "<green>Nation successfully disbanded.</green>");
        });
        MessageUtils.send(player, "<yellow>Are you sure you want to DISBAND " + nation.getName() + "? This is irreversible!</yellow>");
        MessageUtils.send(player, "<green>Type <gold>/dc confirm</gold> to proceed.</green>");
    }

    private void handleConfirm(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtils.send(sender, "general.player_only");
            return;
        }
        Runnable action = pendingConfirmations.remove(player.getUniqueId());
        if (action != null) {
            action.run();
        } else {
            MessageUtils.send(player, "<red>You have no pending confirmations.</red>");
        }
    }

    private void handleCapital(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.send(sender, "general.player_only");
            return;
        }
        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) {
            MessageUtils.send(player, "<red>You are not in any nation.</red>");
            return;
        }

        if (args.length < 2 || args[1].equalsIgnoreCase("info")) {
            showCapitalInfo(player, nation);
            return;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "claim": {
                if (!player.getUniqueId().equals(nation.getLeaderUUID()) && !player.hasPermission("nation.admin")) {
                    MessageUtils.send(player, "<red>Only the nation leader can claim the capital territory.</red>");
                    return;
                }
                var res = plugin.getTerritoryManager().claimCapital(player, player.getLocation());
                MessageUtils.send(player, (res.isSuccess() ? "<green>" : "<red>") + res.getMessage() +
                        (res.isSuccess() ? "</green>" : "</red>"));
                break;
            }
            case "spawn":
            case "tp": {
                if (!nation.hasCapital()) {
                    MessageUtils.send(player, "<red>Your nation has not claimed a capital yet.</red>");
                    return;
                }
                var loc = plugin.getTerritoryManager().toBukkitLocation(nation.getCapital());
                if (loc != null) {
                    player.teleport(loc);
                    MessageUtils.send(player, "<green>Teleported to the Capital of " + nation.getName() + ".</green>");
                } else {
                    MessageUtils.send(player, "<red>Could not find the Capital world. Is it loaded?</red>");
                }
                break;
            }
            default:
                showCapitalInfo(player, nation);
                break;
        }
    }

    private void showCapitalInfo(Player player, Nation nation) {
        MessageUtils.send(player, "<gold>═══ Capital Info: " + nation.getName() + " ═══</gold>");
        if (!nation.hasCapital()) {
            MessageUtils.send(player, "<yellow>Not set.</yellow>");
            if (nation.getLeaderUUID() != null && nation.getLeaderUUID().equals(player.getUniqueId())) {
                MessageUtils.send(player, "<gray>Use <yellow>/nc capital claim</yellow> at the location you desire.</gray>");
            }
            return;
        }
        var cap = nation.getCapital();
        MessageUtils.send(player, "<gray>World: <white>" + cap.getWorld());
        MessageUtils.send(player, "<gray>Position: <white>(" +
                (int) cap.getX() + ", " + (int) cap.getY() + ", " + (int) cap.getZ() + ")");
        MessageUtils.send(player, "<gray>Chunk: <white>" + (((int) cap.getX()) >> 4) + ", " + (((int) cap.getZ()) >> 4));
    }

    // === HELP ===

    private void showHelp(CommandSender sender) {
        MessageUtils.send(sender, "help.header");
        MessageUtils.send(sender, "help.title");
        MessageUtils.send(sender, "help.footer");
        MessageUtils.send(sender, "help.commands.info");
        MessageUtils.send(sender, "help.commands.treasury");
        MessageUtils.send(sender, "help.commands.stats");

        if (sender.hasPermission("nation.admin")) {
            MessageUtils.send(sender, "help.admin_section");
            MessageUtils.send(sender, "help.admin_commands.admin");
        }
    }

    // === GOVERNMENT INFO ===

    private void showGovernmentInfo(CommandSender sender) {
        Government gov;
        Nation contextNation = null;
        if (sender instanceof Player p) {
            contextNation = plugin.getNationManager().getNationOf(p.getUniqueId());
            gov = plugin.getGovernmentManager().getGovernment(p.getUniqueId());
            if (gov == null) gov = plugin.getDataManager().getGovernment();
        } else {
            gov = plugin.getDataManager().getGovernment();
        }

        MessageUtils.send(sender, "government.header");
        MessageUtils.send(sender, "government.title");
        MessageUtils.send(sender, "government.footer");
        if (contextNation != null) {
            MessageUtils.send(sender, "<gray>Nation: <gold>" + contextNation.getName() +
                    "</gold> <gray>(" + contextNation.getType().getDisplayName() + ")</gray>");
        }

        if (gov.hasPresident()) {
            String presName = Bukkit.getOfflinePlayer(gov.getPresidentUUID()).getName();
            MessageUtils.send(sender, "government.president", "name", presName);
            MessageUtils.send(sender, "government.president_term", "term", gov.getCurrentTerm(), "time",
                    MessageUtils.formatTime(gov.getTermEndTime() - System.currentTimeMillis()));
        } else {
            MessageUtils.send(sender, "government.no_president");
        }
    }

    // === MAIN MENU ===

    private void handleMainMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtils.send(sender, "general.player_only");
            return;
        }
        plugin.getGUIListener().openMainMenu(player);
    }

    // === TREASURY ===

    private void handleTreasury(CommandSender sender, String[] args) {
        Nation contextNation = null;
        Treasury treasury;
        if (sender instanceof Player p) {
            contextNation = plugin.getNationManager().getNationOf(p.getUniqueId());
            treasury = contextNation != null ? contextNation.getTreasury()
                    : plugin.getDataManager().getTreasury();
        } else {
            treasury = plugin.getDataManager().getTreasury();
        }

        MessageUtils.send(sender, "treasury.header");
        MessageUtils.send(sender, "treasury.title");
        MessageUtils.send(sender, "treasury.footer");
        if (contextNation != null) {
            MessageUtils.send(sender, "<gray>Nation: <gold>" + contextNation.getName() + "</gold>");
        }
        MessageUtils.send(sender, "treasury.balance", "balance", MessageUtils.formatNumber(treasury.getBalance()));
        MessageUtils.send(sender, "treasury.income", "income",
                MessageUtils.formatNumber(treasury.getTotalIncome()));
        MessageUtils.send(sender, "treasury.expenses", "expenses",
                MessageUtils.formatNumber(treasury.getTotalExpenses()));

        MessageUtils.send(sender, "");
        MessageUtils.send(sender, "treasury.transactions_title");
        var transactions = treasury.getRecentTransactions(5);
        for (Treasury.Transaction tx : transactions) {
            String type = tx.getType().name().contains("INCOME") || tx.getType().name().contains("DONATION")
                    || tx.getType().name().contains("FUND") || tx.getType().name().contains("REFUND")
                            ? "treasury.transaction_income"
                            : "treasury.transaction_expense";
            MessageUtils.send(sender, type, "amount", MessageUtils.formatNumber(tx.getAmount()), "description",
                    tx.getDescription());
        }

        if (sender instanceof Player && args.length >= 3 && args[1].equalsIgnoreCase("donate")) {
            try {
                long amount = Long.parseLong(args[2]);
                Player player = (Player) sender;

                if (plugin.getVaultHook().getBalance(player.getUniqueId()) < amount) {
                    MessageUtils.send(player, "treasury.donate_insufficient");
                    return;
                }

                plugin.getVaultHook().withdraw(player.getUniqueId(), amount);
                if (contextNation != null) {
                    plugin.getTreasuryManager().deposit(contextNation, Treasury.TransactionType.DONATION,
                            amount, "Donation from " + player.getName(), player.getUniqueId());
                    plugin.getDataManager().saveNations();
                } else {
                    plugin.getTreasuryManager().deposit(Treasury.TransactionType.DONATION, amount,
                            "Donation from " + player.getName(), player.getUniqueId());
                }
                MessageUtils.send(player, "treasury.donate_success", "amount", MessageUtils.formatNumber(amount));
                MessageUtils.broadcast("treasury.donate_broadcast", "player", player.getName(), "amount",
                        MessageUtils.formatNumber(amount));
            } catch (NumberFormatException e) {
                MessageUtils.send(sender, "treasury.donate_invalid");
            }
        }

        MessageUtils.send(sender, "");
        MessageUtils.send(sender, "treasury.donate_usage");
    }

    // === STATS ===

    private void showPlayerStats(CommandSender sender, String[] args) {
        UUID targetUUID;
        String targetName;

        if (args.length >= 2) {
            var offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
            if (!offlinePlayer.hasPlayedBefore()) {
                MessageUtils.send(sender, "general.player_not_found");
                return;
            }
            targetUUID = offlinePlayer.getUniqueId();
            targetName = offlinePlayer.getName();
        } else if (sender instanceof Player player) {
            targetUUID = player.getUniqueId();
            targetName = player.getName();
        } else {
            MessageUtils.send(sender, "stats.usage");
            return;
        }

        PlayerData data = plugin.getDataManager().getPlayerData(targetUUID);

        MessageUtils.send(sender, "stats.header");
        MessageUtils.send(sender, "stats.title", "name", targetName.toUpperCase());
        MessageUtils.send(sender, "stats.footer");
        MessageUtils.send(sender, "stats.playtime", "time", MessageUtils.formatTime(data.getTotalPlaytime()));
        MessageUtils.send(sender, "stats.level", "level", calculateLevel(data));
        MessageUtils.send(sender, "stats.votes", "votes", data.getTotalVotesCast());
        MessageUtils.send(sender, "stats.endorsements", "endorsements", data.getEndorsementsGiven());
        MessageUtils.send(sender, "stats.president_times", "times", data.getTimesServedAsPresident());
        MessageUtils.send(sender, "stats.minister_times", "times", data.getTimesServedAsCabinet());
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, "stats.arena_title");
        MessageUtils.send(sender, "stats.arena_kills", "kills", data.getArenaKills(), "deaths", data.getArenaDeaths());
        MessageUtils.send(sender, "stats.arena_streak", "streak", data.getBestKillstreak());
    }

    private int calculateLevel(PlayerData data) {
        double hours = data.getTotalPlaytime() / (1000.0 * 60 * 60);
        return (int) (hours / 10) + 1;
    }

    // === ADMIN ===

    private void handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nation.admin")) {
            MessageUtils.send(sender, "general.no_permission");
            return;
        }

        if (args.length < 2) {
            showAdminHelp(sender);
            return;
        }

        String subCmd = args[1].toLowerCase();

        switch (subCmd) {
            case "set":
                if (args.length > 2 && args[2].equalsIgnoreCase("leader")) {
                    handleAdminSetLeader(sender, args);
                } else if (args.length > 2 && args[2].equalsIgnoreCase("announcement")) {
                    handleAdminSetAnnouncement(sender, args);
                } else {
                    MessageUtils.send(sender, "<red>Usage: /nationcore admin set <leader|announcement> ...</red>");
                }
                break;
            case "remove":
                if (args.length > 2 && args[2].equalsIgnoreCase("president")) {
                    handleAdminRemovePresident(sender);
                } else {
                    MessageUtils.send(sender, "admin.help_removepresident");
                }
                break;
            case "confirm":
                handleAdminConfirm(sender);
                break;
            case "treasury":
                handleAdminTreasury(sender, args);
                break;
            case "reload":
                handleAdminReload(sender);
                break;
            case "npc":
                handleAdminNpc(sender, args);
                break;
            case "invoice":
                handleAdminInvoice(sender, args);
                break;
            case "research":
                handleAdminResearch(sender, args);
                break;
            default:
                showAdminHelp(sender);
                break;
        }
    }
    private void showAdminHelp(CommandSender sender) {
        MessageUtils.send(sender, "admin.help_header");
        MessageUtils.send(sender, "admin.help_setpresident");
        MessageUtils.send(sender, "admin.help_removepresident");
        MessageUtils.send(sender, "<gold>/dc admin treasury give <nation> <amount> <gray>- Give nation treasury balance");
        MessageUtils.send(sender, "<gold>/dc admin treasury take <nation> <amount> <gray>- Take nation treasury balance");
        MessageUtils.send(sender, "<gold>/dc admin treasury set <nation> <amount> <gray>- Set nation treasury balance");
        MessageUtils.send(sender, "admin.help_reload");
        MessageUtils.send(sender, "<gold>/dc admin invoice add <player> <gray>- Add a tax invoice to a player");
        MessageUtils.send(sender, "<gold>/dc admin invoice remove <player> <gray>- Settle all active invoices for a player");
        MessageUtils.send(sender, "<gold>/nationcore admin npc invite <nation_name> <name> <gray>- Invite a fake member to the nation");
        MessageUtils.send(sender, "<gold>/nationcore admin npc kick <nation_name> <name>   <gray>- Kick a fake member from the nation");
        MessageUtils.send(sender, "<gold>/nationcore admin npc role <nation_name> <name> <role> <gray>- Change a fake member's role");
        MessageUtils.send(sender, "<gold>/nationcore admin npc list <nation_name>           <gray>- View list of fake members");
        MessageUtils.send(sender, "<gold>/nationcore admin research set <nation_name> <research_id> <level> <gray>- Set a nation's research level");
        MessageUtils.send(sender, "<gold>/nationcore admin research reset <nation_name>                       <gray>- Reset all research data for a nation");
    }

    private void handleAdminInvoice(CommandSender sender, String[] args) {
        if (args.length < 4) {
            MessageUtils.send(sender, "<red>Usage: /nationcore admin invoice <add|remove> <player_name></red>");
            return;
        }

        String action = args[2].toLowerCase();
        String targetName = args[3];

        org.bukkit.OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
        if (targetPlayer == null || (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline())) {
            MessageUtils.send(sender, "general.player_not_found");
            return;
        }

        UUID playerUUID = targetPlayer.getUniqueId();

        if (action.equals("add")) {
            Nation nation = plugin.getNationManager().getNationOf(playerUUID);
            if (nation == null) {
                MessageUtils.send(sender, "<red>Player " + targetPlayer.getName() + " is not in a nation.</red>");
                return;
            }

            var taxRecord = plugin.getTaxManager().getTaxRecord();
            var profile = plugin.getTaxManager().getTaxRecord().getOrCreateProfile(playerUUID.toString(), targetPlayer.getName());
            
            // Generate next sequential invoice ID
            String invId = taxRecord.nextInvoiceId();
            
            // Create the invoice
            TaxInvoice invoice = new TaxInvoice(invId, nation.getId(), nation.getName(), TaxManager.INVOICE_AMOUNT);
            profile.addInvoice(invoice);
            
            plugin.getDataManager().saveTaxRecord();

            MessageUtils.send(sender, "<green>Successfully generated new tax invoice <white>" + invId + "</white> of <gold>$" + MessageUtils.formatNumber(TaxManager.INVOICE_AMOUNT) + "</gold> for player " + targetPlayer.getName() + ".</green>");
            
            Player onlineTarget = targetPlayer.getPlayer();
            if (onlineTarget != null && onlineTarget.isOnline()) {
                MessageUtils.send(onlineTarget, "<yellow>📑 New tax invoice <white>" + invId + "</white> of <gold>$" + MessageUtils.formatNumber(TaxManager.INVOICE_AMOUNT) + "</gold> issued by admin.");
                MessageUtils.playSound(onlineTarget, Sound.BLOCK_NOTE_BLOCK_BELL);
            }
        } else if (action.equals("remove")) {
            var profile = plugin.getTaxManager().getProfile(playerUUID);
            if (profile == null || profile.getOutstandingInvoices().isEmpty()) {
                MessageUtils.send(sender, "<red>Player " + targetPlayer.getName() + " has no active tax invoices.</red>");
                return;
            }

            List<TaxInvoice> outstanding = new ArrayList<>(profile.getOutstandingInvoices());
            double totalPaid = 0;
            int count = 0;

            for (TaxInvoice invoice : outstanding) {
                double remaining = invoice.getRemaining();
                if (remaining <= 0) continue;

                // Withdraw from player's balance (force payment)
                plugin.getVaultHook().withdraw(playerUUID, remaining);
                
                // applyPayment deposits the money to the nation's treasury and updates state
                plugin.getTaxManager().applyPayment(playerUUID, profile, invoice, remaining, TaxRecord.PaymentMethod.MANUAL);
                
                totalPaid += remaining;
                count++;
            }

            plugin.getDataManager().saveTaxRecord();

            MessageUtils.send(sender, "<green>Successfully settled " + count + " active invoices for player " + targetPlayer.getName() + ", transferring <gold>$" + MessageUtils.formatNumber(totalPaid) + "</gold> to their respective nation treasury.</green>");
            
            Player onlineTarget = targetPlayer.getPlayer();
            if (onlineTarget != null && onlineTarget.isOnline()) {
                MessageUtils.send(onlineTarget, "<green>All active tax invoices (<gold>$" + MessageUtils.formatNumber(totalPaid) + "</gold>) have been settled by admin.</green>");
                MessageUtils.playSound(onlineTarget, Sound.ENTITY_PLAYER_LEVELUP);
            }
        } else {
            MessageUtils.send(sender, "<red>Usage: /nationcore admin invoice <add|remove> <player_name></red>");
        }
    }


    // === ADMIN NPC ===

    private void handleAdminNpc(CommandSender sender, String[] args) {
        // /nationcore admin npc <sub> <nation_id> [name] [role]
        if (args.length < 3) {
            sendNpcHelp(sender);
            return;
        }

        String sub = args[2].toLowerCase();

        switch (sub) {
            case "invite" -> handleAdminNpcInvite(sender, args);
            case "kick"   -> handleAdminNpcKick(sender, args);
            case "role"   -> handleAdminNpcRole(sender, args);
            case "list"   -> handleAdminNpcList(sender, args);
            default       -> sendNpcHelp(sender);
        }
    }

    private void sendNpcHelp(CommandSender sender) {
        MessageUtils.send(sender, "<gold>═══ ADMIN NPC COMMANDS ═══");
        MessageUtils.send(sender, "<white>/nationcore admin npc invite <nation_name> <name>");
        MessageUtils.send(sender, "<white>/nationcore admin npc kick   <nation_name> <name>");
        MessageUtils.send(sender, "<white>/nationcore admin npc role   <nation_name> <name> <CITIZEN|OFFICER>");
        MessageUtils.send(sender, "<white>/nationcore admin npc list   <nation_name>");
    }

    private void handleAdminNpcInvite(CommandSender sender, String[] args) {
        // /nationcore admin npc invite <nation_name> <name>
        if (args.length < 5) {
            MessageUtils.send(sender, "<red>Usage: /nationcore admin npc invite <nation_name> <name>");
            return;
        }

        Nation nation = null;
        StringBuilder currentName = new StringBuilder();
        for (int i = 3; i < args.length - 1; i++) {
            if (i > 3) currentName.append(" ");
            currentName.append(args[i]);
            Nation found = plugin.getNationManager().getNationByName(currentName.toString());
            if (found != null) {
                nation = found;
            }
        }

        if (nation == null) {
            nation = plugin.getNationManager().getNation(args[3]);
        }

        if (nation == null) {
            MessageUtils.send(sender, "<red>Nation not found.</red>");
            return;
        }

        String npcName  = args[args.length - 1];

        var result = plugin.getFakeMemberManager().inviteNpc(nation.getId(), npcName);
        MessageUtils.send(sender,
                (result.isSuccess() ? "<green>" : "<red>") + result.getMessage());
    }

    private void handleAdminNpcKick(CommandSender sender, String[] args) {
        // /nationcore admin npc kick <nation_name> <name>
        if (args.length < 5) {
            MessageUtils.send(sender, "<red>Usage: /nationcore admin npc kick <nation_name> <name>");
            return;
        }

        Nation nation = null;
        StringBuilder currentName = new StringBuilder();
        for (int i = 3; i < args.length - 1; i++) {
            if (i > 3) currentName.append(" ");
            currentName.append(args[i]);
            Nation found = plugin.getNationManager().getNationByName(currentName.toString());
            if (found != null) {
                nation = found;
            }
        }

        if (nation == null) {
            nation = plugin.getNationManager().getNation(args[3]);
        }

        if (nation == null) {
            MessageUtils.send(sender, "<red>Nation not found.</red>");
            return;
        }

        String npcName  = args[args.length - 1];

        var result = plugin.getFakeMemberManager().kickNpc(nation.getId(), npcName);
        MessageUtils.send(sender,
                (result.isSuccess() ? "<green>" : "<red>") + result.getMessage());
    }

    private void handleAdminNpcRole(CommandSender sender, String[] args) {
        // /nationcore admin npc role <nation_name> <name> <CITIZEN|OFFICER>
        if (args.length < 6) {
            MessageUtils.send(sender, "<red>Usage: /nationcore admin npc role <nation_name> <name> <CITIZEN|OFFICER>");
            return;
        }

        String roleStr  = args[args.length - 1].toUpperCase();
        id.nationcore.models.Nation.NationRole role;
        try {
            role = id.nationcore.models.Nation.NationRole.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            MessageUtils.send(sender, "<red>Invalid role. Choose: CITIZEN or OFFICER.");
            return;
        }

        String npcName  = args[args.length - 2];

        Nation nation = null;
        StringBuilder currentName = new StringBuilder();
        for (int i = 3; i < args.length - 2; i++) {
            if (i > 3) currentName.append(" ");
            currentName.append(args[i]);
            Nation found = plugin.getNationManager().getNationByName(currentName.toString());
            if (found != null) {
                nation = found;
            }
        }

        if (nation == null) {
            nation = plugin.getNationManager().getNation(args[3]);
        }

        if (nation == null) {
            MessageUtils.send(sender, "<red>Nation not found.</red>");
            return;
        }

        var result = plugin.getFakeMemberManager().setNpcRole(nation.getId(), npcName, role);
        MessageUtils.send(sender,
                (result.isSuccess() ? "<green>" : "<red>") + result.getMessage());
    }

    private void handleAdminNpcList(CommandSender sender, String[] args) {
        // /nationcore admin npc list <nation_name>
        if (args.length < 4) {
            MessageUtils.send(sender, "<red>Usage: /nationcore admin npc list <nation_name>");
            return;
        }

        Nation nation = null;
        StringBuilder currentName = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            if (i > 3) currentName.append(" ");
            currentName.append(args[i]);
            Nation found = plugin.getNationManager().getNationByName(currentName.toString());
            if (found != null) {
                nation = found;
            }
        }

        if (nation == null) {
            nation = plugin.getNationManager().getNation(args[3]);
        }

        if (nation == null) {
            MessageUtils.send(sender, "<red>Nation not found.</red>");
            return;
        }

        var npcs = plugin.getFakeMemberManager().getAllNpcsInNation(nation);
        MessageUtils.send(sender, "<gold>═══ Fake Members: " + nation.getName()
                + " (" + npcs.size() + " NPC) ═══");
        if (npcs.isEmpty()) {
            MessageUtils.send(sender, "<gray>No fake members in this nation.");
            return;
        }
        for (id.nationcore.models.FakeMember npc : npcs) {
            String roleColor = npc.getRole() == id.nationcore.models.Nation.NationRole.OFFICER
                    ? "<gold>"
                    : "<white>";
            MessageUtils.send(sender,
                    "<gray> • " + roleColor + npc.getName()
                    + " <gray>[" + npc.getRole().name() + "]  "
                    + "Balance: <gold>$" + MessageUtils.formatNumber(npc.getTaxBalance())
                    + " <gray>Debt: <red>$" + MessageUtils.formatNumber(npc.getTaxDebt()));
        }
    }

    private void handleAdminSetLeader(CommandSender sender, String[] args) {
        if (args.length < 5) {
            MessageUtils.send(sender, "admin.setpresident_usage");
            return;
        }

        String targetName = args[args.length - 1];
        String nationName = String.join(" ", Arrays.copyOfRange(args, 3, args.length - 1));

        Nation nation = plugin.getNationManager().getNationByName(nationName);
        if (nation == null) {
            nation = plugin.getNationManager().getNation(nationName);
        }

        if (nation == null) {
            MessageUtils.send(sender, "<red>Nation '" + nationName + "' not found.</red>");
            return;
        }

        Player onlinePlayer = Bukkit.getPlayer(targetName);
        org.bukkit.OfflinePlayer targetPlayer;
        if (onlinePlayer != null) {
            targetPlayer = onlinePlayer;
        } else {
            targetPlayer = Bukkit.getOfflinePlayer(targetName);
            if (!targetPlayer.hasPlayedBefore()) {
                MessageUtils.send(sender, "general.player_not_found");
                return;
            }
        }

        final org.bukkit.OfflinePlayer offlinePlayer = targetPlayer;
        final Nation finalNation = nation;

        if (sender instanceof Player player) {
            pendingConfirmations.put(player.getUniqueId(), () -> {
                executeSetLeader(finalNation, offlinePlayer);
                MessageUtils.send(sender, "admin.setpresident_success", "player", offlinePlayer.getName(), "nation", finalNation.getName());
            });
            MessageUtils.send(sender, "<yellow>Are you sure you want to set <gold>" + offlinePlayer.getName() +
                    "</gold> as leader of <gold>" + finalNation.getName() + "</gold>? This will replace the current leader.</yellow>");
            MessageUtils.send(sender, "<green>Type <gold>/dc admin confirm</gold> to proceed.</green>");
        } else {
            executeSetLeader(finalNation, offlinePlayer);
            MessageUtils.send(sender, "admin.setpresident_success", "player", offlinePlayer.getName(), "nation", finalNation.getName());
        }
    }

    private void executeSetLeader(Nation nation, org.bukkit.OfflinePlayer offlinePlayer) {
        // Demote old leader in the nation and player data
        UUID oldLeaderUUID = nation.getLeaderUUID();
        if (oldLeaderUUID != null) {
            Nation.NationMember oldMember = nation.getMember(oldLeaderUUID);
            if (oldMember != null) {
                oldMember.setRole(Nation.NationRole.CITIZEN);
            }
            PlayerData oldData = plugin.getDataManager().getPlayerData(oldLeaderUUID);
            if (oldData != null) {
                oldData.setNationRole(Nation.NationRole.CITIZEN);
            }
        }

        // Promote new leader in the nation and player data
        UUID newLeaderUUID = offlinePlayer.getUniqueId();
        Nation.NationMember newMember = nation.getMember(newLeaderUUID);
        if (newMember == null) {
            newMember = new Nation.NationMember(newLeaderUUID, offlinePlayer.getName(), Nation.NationRole.LEADER);
            nation.addMember(newMember);
        } else {
            newMember.setRole(Nation.NationRole.LEADER);
        }
        PlayerData newData = plugin.getDataManager().getOrCreatePlayerData(newLeaderUUID, offlinePlayer.getName());
        newData.setNationId(nation.getId());
        newData.setNationRole(Nation.NationRole.LEADER);
        newData.setNationJoinedAt(System.currentTimeMillis());

        // Set specific government type fields and logic
        if (nation.getType() == GovernmentType.REPUBLIC) {
            plugin.getGovernmentManager().setPresident(nation, newLeaderUUID, offlinePlayer.getName(), false);
            if (nation.getElection() != null) {
                nation.getElection().endElection();
            }
        } else if (nation.getType() == GovernmentType.COMMUNIST) {
            plugin.getCommunistManager().setSecretaryGeneral(nation, newLeaderUUID, offlinePlayer.getName(), false);
        } else if (nation.getType() == GovernmentType.MONARCHY) {
            plugin.getMonarchyManager().setKing(nation, newLeaderUUID, offlinePlayer.getName());
        } else if (nation.getType() == GovernmentType.CALIPHATE) {
            plugin.getCaliphateManager().setCaliph(nation, newLeaderUUID, offlinePlayer.getName());
        }

        // Save changes
        plugin.getDataManager().saveNations();
        plugin.getDataManager().savePlayerData();
    }

    private void handleAdminSetAnnouncement(CommandSender sender, String[] args) {
        if (args.length < 5) {
            MessageUtils.send(sender, "<red>Usage: /nationcore admin set announcement <nation_name> <custom_message_value></red>");
            return;
        }

        Nation nation = null;
        int nationArgsCount = 0;

        StringBuilder currentName = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            if (i > 3) currentName.append(" ");
            currentName.append(args[i]);
            Nation found = plugin.getNationManager().getNationByName(currentName.toString());
            if (found != null) {
                nation = found;
                nationArgsCount = i - 2;
            }
        }

        if (nation == null) {
            // Check if args[3] is nation ID (UUID string) as a fallback
            nation = plugin.getNationManager().getNation(args[3]);
            if (nation != null) {
                nationArgsCount = 1;
            }
        }

        if (nation == null) {
            MessageUtils.send(sender, "<red>Nation with name or ID '" + args[3] + "' not found.</red>");
            return;
        }

        // The remaining arguments after the nation name form the custom message
        if (3 + nationArgsCount >= args.length) {
            MessageUtils.send(sender, "<red>Usage: /nationcore admin set announcement <nation_name> <custom_message_value></red>");
            return;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 3 + nationArgsCount, args.length));
        nation.setAnnouncementMessage(message);
        nation.setAnnouncementCreatedAt(System.currentTimeMillis());
        plugin.getDataManager().saveNations();

        MessageUtils.send(sender, "<green>Announcement message for nation '<gold>" + nation.getName() + "</gold>' has been set manually.</green>");
        sender.sendMessage(MessageUtils.parseLegacy("&7Preview: &f" + message));
    }

    private void handleAdminConfirm(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtils.send(sender, "general.player_only");
            return;
        }

        if (pendingConfirmations.containsKey(player.getUniqueId())) {
            pendingConfirmations.remove(player.getUniqueId()).run();
        } else {
            MessageUtils.send(player, "<red>You have no pending confirmations.</red>");
        }
    }

    private void handleAdminRemovePresident(CommandSender sender) {
        Government gov = plugin.getDataManager().getGovernment();
        if (!gov.hasPresident()) {
            MessageUtils.send(sender, "<red>There is no active president to remove.</red>");
            return;
        }

        if (sender instanceof Player player) {
            pendingConfirmations.put(player.getUniqueId(), () -> {
                plugin.getGovernmentManager().endTerm("ADMIN_REMOVAL");
                plugin.getElectionManager().startElection();
                MessageUtils.send(sender, "admin.removepresident_success");
                MessageUtils.broadcast(
                        "<yellow>A new election cycle has been initiated following the removal of the president.</yellow>");
            });
            MessageUtils.send(sender,
                    "<yellow>Are you sure you want to remove the current president? This will start a new election.</yellow>");
            MessageUtils.send(sender, "<green>Type <gold>/dc admin confirm</gold> to proceed.</green>");
        } else {
            plugin.getGovernmentManager().endTerm("ADMIN_REMOVAL");
            plugin.getElectionManager().startElection();
            MessageUtils.send(sender, "admin.removepresident_success");
        }
    }



    private void handleAdminTreasury(CommandSender sender, String[] args) {
        if (args.length < 5) {
            MessageUtils.send(sender, "<red>Usage: /dc admin treasury <give|take|set> <nation_name> <amount></red>");
            return;
        }

        String action = args[2].toLowerCase();
        if (!action.equals("give") && !action.equals("take") && !action.equals("set")) {
            MessageUtils.send(sender, "<red>Usage: /dc admin treasury <give|take|set> <nation_name> <amount></red>");
            return;
        }

        String amountStr = args[args.length - 1];
        long amount;
        try {
            amount = Long.parseLong(amountStr);
            if (amount < 0) {
                MessageUtils.send(sender, "<red>Amount must be non-negative!</red>");
                return;
            }
        } catch (NumberFormatException e) {
            MessageUtils.send(sender, "<red>Invalid amount value!</red>");
            return;
        }

        Nation nation = null;

        StringBuilder currentName = new StringBuilder();
        for (int i = 3; i < args.length - 1; i++) {
            if (i > 3) currentName.append(" ");
            currentName.append(args[i]);
            Nation found = plugin.getNationManager().getNationByName(currentName.toString());
            if (found != null) {
                nation = found;
            }
        }

        if (nation == null) {
            nation = plugin.getNationManager().getNation(args[3]);
        }

        if (nation == null) {
            MessageUtils.send(sender, "<red>Nation not found.</red>");
            return;
        }

        switch (action) {
            case "give":
                plugin.getTreasuryManager().deposit(nation, Treasury.TransactionType.MISC_EXPENSE, amount,
                        "Admin give by " + sender.getName(), null);
                plugin.getDataManager().saveNations();
                MessageUtils.send(sender, "<green>Gave <gold>" + MessageUtils.formatNumber(amount) + "</gold> to nation '<gold>" + nation.getName() + "</gold>' treasury.</green>");
                break;
            case "take":
                double balance = plugin.getTreasuryManager().getBalance(nation);
                if (amount > balance) {
                    MessageUtils.send(sender, "<red>Nation treasury only has " + MessageUtils.formatNumber(balance) + ". Cannot take " + MessageUtils.formatNumber(amount) + ".</red>");
                    return;
                }
                plugin.getTreasuryManager().withdraw(nation, Treasury.TransactionType.MISC_EXPENSE, amount,
                        "Admin take by " + sender.getName(), null);
                plugin.getDataManager().saveNations();
                MessageUtils.send(sender, "<green>Took <gold>" + MessageUtils.formatNumber(amount) + "</gold> from nation '<gold>" + nation.getName() + "</gold>' treasury.</green>");
                break;
            case "set":
                plugin.getTreasuryManager().getTreasury(nation).setBalance(amount);
                plugin.getDataManager().saveNations();
                MessageUtils.send(sender, "<green>Set nation '<gold>" + nation.getName() + "</gold>' treasury balance to <gold>" + MessageUtils.formatNumber(amount) + "</gold>.</green>");
                break;
        }
    }

    private void handleAdminReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.reloadLanguage();
        plugin.reloadGUI();
        MessageUtils.send(sender, "general.config_reloaded");
    }

    // === ADMIN RESEARCH ===

    private void handleAdminResearch(CommandSender sender, String[] args) {
        // /nationcore admin research <set|reset> ...
        if (args.length < 3) {
            sendResearchAdminHelp(sender);
            return;
        }

        String sub = args[2].toLowerCase();
        switch (sub) {
            case "set"   -> handleAdminResearchSet(sender, args);
            case "reset" -> handleAdminResearchReset(sender, args);
            default      -> sendResearchAdminHelp(sender);
        }
    }

    private void sendResearchAdminHelp(CommandSender sender) {
        MessageUtils.send(sender, "<gold>═══ ADMIN RESEARCH COMMANDS ═══");
        MessageUtils.send(sender, "<white>/nationcore admin research set <nation_name> <research_id> <level>");
        MessageUtils.send(sender, "<white>/nationcore admin research reset <nation_name>");
    }

    /**
     * /nationcore admin research set <nation_name> <research_id> <level>
     *
     * Sets a specific research type to the given level for a nation.
     * The level is clamped between 0 and the configured max level.
     */
    private void handleAdminResearchSet(CommandSender sender, String[] args) {
        // Minimum: admin research set <nation> <research_id> <level>  → args[0..5]
        if (args.length < 6) {
            MessageUtils.send(sender, "<red>Usage: /nationcore admin research set <nation_name> <research_id> <level></red>");
            return;
        }

        // Last arg is the level, second-to-last is research_id.
        // Everything between args[3] and args[length-3] is the nation name.
        String levelStr    = args[args.length - 1];
        String researchId  = args[args.length - 2];
        String nationName  = String.join(" ", Arrays.copyOfRange(args, 3, args.length - 2));

        // Resolve nation
        Nation nation = plugin.getNationManager().getNationByName(nationName);
        if (nation == null) nation = plugin.getNationManager().getNation(nationName);
        if (nation == null) {
            MessageUtils.send(sender, "<red>Nation '" + nationName + "' not found.</red>");
            return;
        }

        // Resolve research type
        id.nationcore.models.ResearchType type = id.nationcore.models.ResearchType.fromId(researchId);
        if (type == null) {
            MessageUtils.send(sender, "<red>Unknown research ID: '" + researchId + "'. Use tab-complete to see valid IDs.</red>");
            return;
        }

        // Parse level
        int level;
        try {
            level = Integer.parseInt(levelStr);
        } catch (NumberFormatException e) {
            MessageUtils.send(sender, "<red>Invalid level value. Must be an integer.</red>");
            return;
        }

        int maxLevel = plugin.getResearchManager().getMaxLevel(type);
        if (level < 0 || level > maxLevel) {
            MessageUtils.send(sender, "<red>Level must be between 0 and " + maxLevel + " for '" + type.getDisplayName() + "'.</red>");
            return;
        }

        nation.getResearchData().setLevel(type, level);
        plugin.getDataManager().saveNations();

        MessageUtils.send(sender, "<green>Set research '<gold>" + type.getDisplayName()
                + "<green>' to level <white>" + level
                + "<green> for nation '<gold>" + nation.getName() + "<green>'.</green>");
    }

    /**
     * /nationcore admin research reset <nation_name>
     *
     * Clears all research level data for the nation and cancels any active research.
     */
    private void handleAdminResearchReset(CommandSender sender, String[] args) {
        if (args.length < 4) {
            MessageUtils.send(sender, "<red>Usage: /nationcore admin research reset <nation_name></red>");
            return;
        }

        String nationName = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

        Nation nation = plugin.getNationManager().getNationByName(nationName);
        if (nation == null) nation = plugin.getNationManager().getNation(nationName);
        if (nation == null) {
            MessageUtils.send(sender, "<red>Nation '" + nationName + "' not found.</red>");
            return;
        }

        id.nationcore.models.NationResearchData researchData = nation.getResearchData();
        researchData.getLevels().clear();
        researchData.setActive(null);
        researchData.setLastCompletedAt(0);
        researchData.setTotalProjectsCompleted(0);
        researchData.setTotalVaultSpent(0);
        plugin.getDataManager().saveNations();

        MessageUtils.send(sender, "<green>All research data for nation '<gold>" + nation.getName() + "<green>' has been reset.</green>");
    }


    private void handleDiplomacy(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.send(sender, "general.player_only");
            return;
        }

        Nation nation = plugin.getNationManager().getNationOf(player.getUniqueId());
        if (nation == null) {
            MessageUtils.send(player, "<red>You are not in any nation.</red>");
            return;
        }

        if (args.length < 2) {
            MessageUtils.send(player, "<gold>═══ Diplomacy Menu ═══</gold>");
            MessageUtils.send(player, "<yellow>/nc diplomacy propose <nation> <status></yellow> <gray>- Propose diplomacy</gray>");
            MessageUtils.send(player, "<yellow>/nc diplomacy accept <nation></yellow> <gray>- Accept diplomacy invitation</gray>");
            MessageUtils.send(player, "<yellow>/nc diplomacy reject <nation></yellow> <gray>- Reject diplomacy invitation</gray>");
            MessageUtils.send(player, "<gray>Status: PEACE, ALLIANCE, TRUCE, WAR</gray>");
            return;
        }

        String sub = args[1].toLowerCase();
        
        switch (sub) {
            case "propose":
                if (args.length < 4) {
                    MessageUtils.send(player, "<yellow>Usage: /nc diplomacy propose <nation> <status></yellow>");
                    return;
                }
                Nation targetNation = plugin.getNationManager().getNationByName(args[2]);
                if (targetNation == null) {
                    MessageUtils.send(player, "<red>Nation '" + args[2] + "' not found.</red>");
                    return;
                }
                id.nationcore.models.DiplomacyStatus status = id.nationcore.models.DiplomacyStatus.fromString(args[3]);
                if (status == null) {
                    MessageUtils.send(player, "<red>Invalid status. Use: PEACE, ALLIANCE, TRUCE, WAR.</red>");
                    return;
                }
                plugin.getDiplomacyManager().proposeDiplomacy(player, nation, targetNation, status);
                break;
            case "accept":
                if (args.length < 3) {
                    MessageUtils.send(player, "<yellow>Usage: /nc diplomacy accept <nation></yellow>");
                    return;
                }
                Nation senderNationA = plugin.getNationManager().getNationByName(args[2]);
                if (senderNationA == null) {
                    MessageUtils.send(player, "<red>Nation '" + args[2] + "' not found.</red>");
                    return;
                }
                plugin.getDiplomacyManager().acceptDiplomacy(player, nation, senderNationA.getId());
                break;
            case "reject":
                if (args.length < 3) {
                    MessageUtils.send(player, "<yellow>Usage: /nc diplomacy reject <nation></yellow>");
                    return;
                }
                Nation senderNationR = plugin.getNationManager().getNationByName(args[2]);
                if (senderNationR == null) {
                    MessageUtils.send(player, "<red>Nation '" + args[2] + "' not found.</red>");
                    return;
                }
                plugin.getDiplomacyManager().rejectDiplomacy(player, nation, senderNationR.getId());
                break;
            default:
                MessageUtils.send(player, "<red>Diplomacy command not recognized. Use: propose, accept, reject.</red>");
                break;
        }
    }

    // === TAB COMPLETE ===

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList(
                    "menu", "help", "info", "treasury", "stats", "hub", "create", 
                    "nation", "leave", "disband", "confirm", "capital", "diplomacy"));
            if (sender.hasPermission("nation.admin")) {
                completions.add("admin");
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();

            switch (sub) {
                case "stats":
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        completions.add(p.getName());
                    }
                    break;
                case "admin":
                    if (sender.hasPermission("nation.admin")) {
                        completions.addAll(Arrays.asList(
                                "set", "remove", "treasury", "reload", "confirm", "npc", "invoice", "research"));
                    }
                    break;
                case "treasury":
                    completions.add("donate");
                    break;
                case "create":
                    completions.addAll(Arrays.asList("republic", "communist"));
                    break;
                case "nation":
                    for (Nation n : plugin.getNationManager().getAllNations()) {
                        if (n.getName() != null) completions.add(n.getName());
                    }
                    break;
                case "capital":
                    completions.addAll(Arrays.asList("info", "claim", "tp", "spawn"));
                    break;
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            String sub2 = args[1].toLowerCase();
            if (sub.equals("admin")) {
                if (sub2.equals("set")) {
                    completions.addAll(Arrays.asList("leader", "announcement"));
                } else if (sub2.equals("remove")) {
                    completions.add("president");
                } else if (sub2.equals("treasury")) {
                    completions.addAll(Arrays.asList("give", "take", "set"));
                } else if (sub2.equals("npc")) {
                    completions.addAll(Arrays.asList("invite", "kick", "role", "list"));
                } else if (sub2.equals("invoice")) {
                    completions.addAll(Arrays.asList("add", "remove"));
                } else if (sub2.equals("research")) {
                    completions.addAll(Arrays.asList("set", "reset"));
                }
            }
        } else if (args.length >= 4) {
            String sub = args[0].toLowerCase();
            String sub2 = args[1].toLowerCase();

            // admin research set / reset — nation name completions
            if (sub.equals("admin") && sub2.equals("research") && sender.hasPermission("nation.admin")) {
                String researchSub = args[2].toLowerCase();
                if (researchSub.equals("reset")) {
                    // /nationcore admin research reset <nation_name...>
                    for (Nation n : plugin.getNationManager().getAllNations()) {
                        if (n.getName() != null) completions.add(n.getName());
                    }
                } else if (researchSub.equals("set")) {
                    // /nationcore admin research set <nation_name...> <research_id> <level>
                    // args[3..] — we try to find how much of the tail is already a valid nation name.
                    // Once we have a nation match, the next arg is research_id, then level.
                    Nation foundNation = null;
                    int nationEndIndex = -1; // index of the last nation-name token (0-based in args)

                    StringBuilder currentName = new StringBuilder();
                    for (int i = 3; i < args.length; i++) {
                        if (i > 3) currentName.append(" ");
                        currentName.append(args[i]);
                        Nation n = plugin.getNationManager().getNationByName(currentName.toString());
                        if (n == null) n = plugin.getNationManager().getNation(args[i]);
                        if (n != null) {
                            foundNation = n;
                            nationEndIndex = i;
                        }
                    }

                    if (foundNation == null) {
                        // Still typing the nation name
                        for (Nation n : plugin.getNationManager().getAllNations()) {
                            if (n.getName() != null) completions.add(n.getName());
                        }
                    } else {
                        int researchIdIndex = nationEndIndex + 1; // 0-based in args
                        int levelIndex      = nationEndIndex + 2;
                        if (args.length == researchIdIndex + 1) {
                            // Suggest research IDs
                            for (id.nationcore.models.ResearchType rt : id.nationcore.models.ResearchType.values()) {
                                completions.add(rt.getId());
                            }
                        } else if (args.length == levelIndex + 1) {
                            // Suggest level numbers 0..maxLevel for the chosen research type
                            id.nationcore.models.ResearchType rt =
                                    id.nationcore.models.ResearchType.fromId(args[researchIdIndex]);
                            if (rt != null) {
                                int max = plugin.getResearchManager().getMaxLevel(rt);
                                for (int lvl = 0; lvl <= max; lvl++) {
                                    completions.add(String.valueOf(lvl));
                                }
                            }
                        }
                    }
                }
            } else if (sub.equals("admin") && sub2.equals("set") && args[2].equalsIgnoreCase("leader")) {
                boolean isNationNameComplete = false;
                if (args.length > 4) {
                    String potentialNationName = String.join(" ", Arrays.copyOfRange(args, 3, args.length - 1));
                    if (plugin.getNationManager().getNationByName(potentialNationName) != null ||
                        plugin.getNationManager().getNation(potentialNationName) != null) {
                        isNationNameComplete = true;
                    }
                }
                if (isNationNameComplete) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        completions.add(p.getName());
                    }
                } else {
                    for (Nation n : plugin.getNationManager().getAllNations()) {
                        if (n.getName() != null) completions.add(n.getName());
                    }
                }
            } else if (sub.equals("admin") && sub2.equals("invoice")) {
                if (args.length == 4) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        completions.add(p.getName());
                    }
                }
            } else if (sub.equals("admin") && sub2.equals("set") && args[2].equalsIgnoreCase("announcement")) {
                boolean isNationNameComplete = false;
                if (args.length > 4) {
                    String potentialNationName = String.join(" ", Arrays.copyOfRange(args, 3, args.length - 1));
                    if (plugin.getNationManager().getNationByName(potentialNationName) != null ||
                        plugin.getNationManager().getNation(potentialNationName) != null) {
                        isNationNameComplete = true;
                    }
                }
                if (!isNationNameComplete) {
                    for (Nation n : plugin.getNationManager().getAllNations()) {
                        if (n.getName() != null) completions.add(n.getName());
                    }
                }
            } else if (sub.equals("admin") && sub2.equals("treasury") &&
                    (args[2].equalsIgnoreCase("give") || args[2].equalsIgnoreCase("take") || args[2].equalsIgnoreCase("set"))) {
                boolean isNationNameComplete = false;
                if (args.length > 4) {
                    String potentialNationName = String.join(" ", Arrays.copyOfRange(args, 3, args.length - 1));
                    if (plugin.getNationManager().getNationByName(potentialNationName) != null ||
                        plugin.getNationManager().getNation(potentialNationName) != null) {
                        isNationNameComplete = true;
                    }
                }
                if (!isNationNameComplete) {
                    for (Nation n : plugin.getNationManager().getAllNations()) {
                        if (n.getName() != null) completions.add(n.getName());
                    }
                }
            } else if (sub.equals("admin") && sub2.equals("npc") && sender.hasPermission("nation.admin")) {
                String action = args[2].toLowerCase();
                
                Nation foundNation = null;
                int nameStartIndex = -1;
                
                StringBuilder currentName = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    if (i > 3) currentName.append(" ");
                    currentName.append(args[i]);
                    Nation n = plugin.getNationManager().getNationByName(currentName.toString());
                    if (n == null) {
                        n = plugin.getNationManager().getNation(args[i]);
                    }
                    if (n != null) {
                        foundNation = n;
                        nameStartIndex = i + 1;
                        break;
                    }
                }
                
                if (foundNation == null) {
                    for (Nation n : plugin.getNationManager().getAllNations()) {
                        if (n.getName() != null) completions.add(n.getName());
                    }
                } else {
                    if (action.equals("list")) {
                        // list only takes nation name
                    } else if (action.equals("invite") || action.equals("kick")) {
                        if (args.length == nameStartIndex + 1) {
                            if (action.equals("kick")) {
                                for (id.nationcore.models.FakeMember npc : foundNation.getAllFakeMembers()) {
                                    completions.add(npc.getName());
                                }
                            }
                        }
                    } else if (action.equals("role")) {
                        if (args.length == nameStartIndex + 1) {
                            for (id.nationcore.models.FakeMember npc : foundNation.getAllFakeMembers()) {
                                completions.add(npc.getName());
                            }
                        } else if (args.length == nameStartIndex + 2) {
                            completions.addAll(Arrays.asList("CITIZEN", "OFFICER"));
                        }
                    }
                }
            }
        }

        String input = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
    }
}
