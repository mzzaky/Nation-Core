package id.nationcore.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import id.nationcore.gui.GUIAction;

import org.bukkit.Bukkit;
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
import id.nationcore.models.RecallPetition;
import id.nationcore.models.Treasury;
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
            MessageUtils.send(sender, "<gray>Capital: <white>" + cap.getWorld() + " (" + (int) cap.getX() + ", " + (int) cap.getY() + ", " + (int) cap.getZ() + ")");
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
        MessageUtils.send(player, "<gray>Radius: <white>" + cap.getRadius() + " blocks");
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
                if (args.length > 2 && args[2].equalsIgnoreCase("president")) {
                    handleAdminSetPresident(sender, args);
                } else {
                    MessageUtils.send(sender, "admin.help_setpresident");
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
            case "addtreasury":
                handleAdminAddTreasury(sender, args);
                break;
            case "reload":
                handleAdminReload(sender);
                break;
            case "reset":
                handleAdminReset(sender, args);
                break;
            case "action":
                handleAdminAction(sender, args);
                break;
            case "npc":
                handleAdminNpc(sender, args);
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
        MessageUtils.send(sender, "admin.help_addtreasury");
        MessageUtils.send(sender, "admin.help_reload");
        MessageUtils.send(sender, "admin.help_reset");
        MessageUtils.send(sender, "<gold>/dc admin action <action_id> <player> <gray>- Execute a GUI action on a player");
        MessageUtils.send(sender, "<gold>/nationcore admin npc invite <nation_id> <name> <gray>- Invite a fake member to the nation");
        MessageUtils.send(sender, "<gold>/nationcore admin npc kick <nation_id> <name>   <gray>- Kick a fake member from the nation");
        MessageUtils.send(sender, "<gold>/nationcore admin npc role <nation_id> <name> <role> <gray>- Change a fake member's role");
        MessageUtils.send(sender, "<gold>/nationcore admin npc list <nation_id>           <gray>- View list of fake members");
    }

    private void handleAdminAction(CommandSender sender, String[] args) {
        if (args.length < 4) {
            MessageUtils.send(sender, "<gold>═══ ADMIN ACTION COMMAND ═══");
            MessageUtils.send(sender, "<white>/dc admin action <action_id> <player>");
            MessageUtils.send(sender, "");
            MessageUtils.send(sender, "<yellow>--- Open GUI Actions ---");
            for (GUIAction action : GUIAction.values()) {
                if (action == GUIAction.UNKNOWN)
                    continue;
                MessageUtils.send(sender, "<gray>  " + action.getConfigKey());
            }
            return;
        }

        String actionId = args[2].toLowerCase();
        String playerName = args[3];

        Player target = Bukkit.getPlayer(playerName);
        if (target == null || !target.isOnline()) {
            MessageUtils.send(sender, "<red>Player '" + playerName + "' is not online.");
            return;
        }

        GUIAction action = GUIAction.fromConfig(actionId);
        if (action == GUIAction.UNKNOWN) {
            MessageUtils.send(sender,
                    "<red>Unknown action: '" + actionId + "'. Use /dc admin action to see valid actions.");
            return;
        }

        try {
            plugin.getGUIListener().executeGUIAction(target, action, actionId, "", null);
            MessageUtils.send(sender, "<green>Executed action '<gold>" + actionId + "<green>' on player '<gold>"
                    + target.getName() + "<green>'.");
        } catch (Exception e) {
            MessageUtils.send(sender, "<red>Failed to execute action: " + e.getMessage());
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
        MessageUtils.send(sender, "<white>/nationcore admin npc invite <nation_id> <name>");
        MessageUtils.send(sender, "<white>/nationcore admin npc kick   <nation_id> <name>");
        MessageUtils.send(sender, "<white>/nationcore admin npc role   <nation_id> <name> <CITIZEN|OFFICER>");
        MessageUtils.send(sender, "<white>/nationcore admin npc list   <nation_id>");
    }

    private void handleAdminNpcInvite(CommandSender sender, String[] args) {
        // /nationcore admin npc invite <nation_id> <name>
        if (args.length < 5) {
            MessageUtils.send(sender, "<red>Usage: /nationcore admin npc invite <nation_id> <name>");
            return;
        }
        String nationId = args[3];
        String npcName  = args[4];

        var result = plugin.getFakeMemberManager().inviteNpc(nationId, npcName);
        MessageUtils.send(sender,
                (result.isSuccess() ? "<green>" : "<red>") + result.getMessage());
    }

    private void handleAdminNpcKick(CommandSender sender, String[] args) {
        // /nationcore admin npc kick <nation_id> <name>
        if (args.length < 5) {
            MessageUtils.send(sender, "<red>Usage: /nationcore admin npc kick <nation_id> <name>");
            return;
        }
        String nationId = args[3];
        String npcName  = args[4];

        var result = plugin.getFakeMemberManager().kickNpc(nationId, npcName);
        MessageUtils.send(sender,
                (result.isSuccess() ? "<green>" : "<red>") + result.getMessage());
    }

    private void handleAdminNpcRole(CommandSender sender, String[] args) {
        // /nationcore admin npc role <nation_id> <name> <CITIZEN|OFFICER>
        if (args.length < 6) {
            MessageUtils.send(sender, "<red>Usage: /nationcore admin npc role <nation_id> <name> <CITIZEN|OFFICER>");
            return;
        }
        String nationId = args[3];
        String npcName  = args[4];
        String roleStr  = args[5].toUpperCase();

        id.nationcore.models.Nation.NationRole role;
        try {
            role = id.nationcore.models.Nation.NationRole.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            MessageUtils.send(sender, "<red>Invalid role. Choose: CITIZEN or OFFICER.");
            return;
        }

        var result = plugin.getFakeMemberManager().setNpcRole(nationId, npcName, role);
        MessageUtils.send(sender,
                (result.isSuccess() ? "<green>" : "<red>") + result.getMessage());
    }

    private void handleAdminNpcList(CommandSender sender, String[] args) {
        // /nationcore admin npc list <nation_id>
        if (args.length < 4) {
            MessageUtils.send(sender, "<red>Usage: /nationcore admin npc list <nation_id>");
            return;
        }
        String nationId = args[3];
        id.nationcore.models.Nation nation = plugin.getNationManager().getNation(nationId);
        if (nation == null) {
            MessageUtils.send(sender, "<red>Nation with ID '" + nationId + "' not found.");
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
                    ? "<yellow>" : "<white>";
            MessageUtils.send(sender,
                    "<gray> • " + roleColor + npc.getName()
                    + " <gray>[" + npc.getRole().name() + "]  "
                    + "Balance: <gold>$" + MessageUtils.formatNumber(npc.getTaxBalance())
                    + " <gray>Debt: <red>$" + MessageUtils.formatNumber(npc.getTaxDebt()));
        }
    }

    private void handleAdminSetPresident(CommandSender sender, String[] args) {
        if (args.length < 4) {
            MessageUtils.send(sender, "admin.setpresident_usage");
            return;
        }

        String targetName = args[3];
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

        if (sender instanceof Player player) {
            pendingConfirmations.put(player.getUniqueId(), () -> {
                plugin.getGovernmentManager().setPresident(offlinePlayer.getUniqueId(), offlinePlayer.getName(), false);
                plugin.getElectionManager().endElection();
                plugin.getDataManager().saveElection();
                plugin.getDataManager().saveGovernment();
                MessageUtils.send(sender, "admin.setpresident_success", "player", offlinePlayer.getName());
            });
            MessageUtils.send(sender, "<yellow>Are you sure you want to set <gold>" + offlinePlayer.getName() +
                    "</gold> as president? This will end the current election.</yellow>");
            MessageUtils.send(sender, "<green>Type <gold>/dc admin confirm</gold> to proceed.</green>");
        } else {
            plugin.getGovernmentManager().setPresident(offlinePlayer.getUniqueId(), offlinePlayer.getName(), false);
            plugin.getElectionManager().endElection();
            plugin.getDataManager().saveElection();
            plugin.getDataManager().saveGovernment();
            MessageUtils.send(sender, "admin.setpresident_success", "player", offlinePlayer.getName());
        }
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



    private void handleAdminAddTreasury(CommandSender sender, String[] args) {
        if (args.length < 4) {
            MessageUtils.send(sender, "admin.addtreasury_usage");
            return;
        }

        String nationId = args[2];
        Nation nation = plugin.getNationManager().getNation(nationId);
        if (nation == null) {
            nation = plugin.getNationManager().getNationByName(nationId);
        }

        if (nation == null) {
            MessageUtils.send(sender, "<red>Nation with ID or name '" + nationId + "' not found.</red>");
            return;
        }

        try {
            long amount = Long.parseLong(args[3]);
            plugin.getTreasuryManager().deposit(nation, Treasury.TransactionType.MISC_EXPENSE, amount,
                    "Admin deposit by " + sender.getName(), null);
            MessageUtils.send(sender, "admin.addtreasury_success", "amount", MessageUtils.formatNumber(amount));
        } catch (NumberFormatException e) {
            MessageUtils.send(sender, "admin.addtreasury_invalid");
        }
    }

    private void handleAdminReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.reloadLanguage();
        plugin.reloadGUI();
        MessageUtils.send(sender, "general.config_reloaded");
    }

    private void handleAdminReset(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtils.send(sender, "admin.reset_usage");
            return;
        }

        String data = args[2].toLowerCase();

        switch (data) {
            case "government":
                plugin.getDataManager().resetGovernment();
                MessageUtils.send(sender, "general.data_reset", "data", "Government");
                break;
            case "election":
                plugin.getDataManager().resetElection();
                MessageUtils.send(sender, "general.data_reset", "data", "Election");
                break;
            case "treasury":
                plugin.getDataManager().resetTreasury();
                MessageUtils.send(sender, "general.data_reset", "data", "Treasury");
                break;
            case "all":
                plugin.getDataManager().resetGovernment();
                plugin.getDataManager().resetElection();
                plugin.getDataManager().resetTreasury();
                MessageUtils.send(sender, "general.all_data_reset");
                break;
            default:
                MessageUtils.send(sender, "admin.reset_invalid");
                break;
        }
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
                                "set", "remove", "addtreasury", "reload", "reset", "confirm", "action", "npc"));
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
                if (sub2.equals("set") || sub2.equals("remove")) {
                    completions.add("president");
                } else if (sub2.equals("reset")) {
                    completions.addAll(Arrays.asList("government", "election", "treasury", "all"));
                } else if (sub2.equals("action")) {
                    for (GUIAction action : GUIAction.values()) {
                        if (action != GUIAction.UNKNOWN) {
                            completions.add(action.getConfigKey());
                        }
                    }
                } else if (sub2.equals("addtreasury")) {
                    for (Nation n : plugin.getNationManager().getAllNations()) {
                        if (n.getId() != null) completions.add(n.getId());
                    }
                } else if (sub2.equals("npc")) {
                    completions.addAll(Arrays.asList("invite", "kick", "role", "list"));
                }
            }
        } else if (args.length == 4) {
            String sub = args[0].toLowerCase();
            String sub2 = args[1].toLowerCase();

            if (sub.equals("admin") && sub2.equals("set") && args[2].equalsIgnoreCase("president")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            } else if (sub.equals("admin") && sub2.equals("action")) {
                if (sender.hasPermission("nation.admin")) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        completions.add(p.getName());
                    }
                }
            } else if (sub.equals("admin") && sub2.equals("npc") && sender.hasPermission("nation.admin")) {
                // args[3] = nation_id
                for (Nation n : plugin.getNationManager().getAllNations()) {
                    if (n.getId() != null) completions.add(n.getId());
                }
            }
        } else if (args.length == 5) {
            String sub  = args[0].toLowerCase();
            String sub2 = args[1].toLowerCase();
            String sub3 = args[2].toLowerCase();

            if (sub.equals("admin") && sub2.equals("npc") && sender.hasPermission("nation.admin")) {
                // args[4] = npc name  (kick / role / list need the name)
                String nationId = args[3];
                Nation targetNation = plugin.getNationManager().getNation(nationId);
                if (targetNation != null) {
                    for (id.nationcore.models.FakeMember npc : targetNation.getAllFakeMembers()) {
                        completions.add(npc.getName());
                    }
                }
            }
        } else if (args.length == 6) {
            String sub  = args[0].toLowerCase();
            String sub2 = args[1].toLowerCase();
            String sub3 = args[2].toLowerCase();

            if (sub.equals("admin") && sub2.equals("npc") && sub3.equals("role")
                    && sender.hasPermission("nation.admin")) {
                completions.addAll(Arrays.asList("CITIZEN", "OFFICER"));
            }
        }

        String input = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
    }
}
