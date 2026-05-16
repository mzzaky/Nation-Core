package id.nationcore.listeners;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import id.nationcore.NationCore;
import id.nationcore.managers.NationManager;
import id.nationcore.models.ArenaSession;
import id.nationcore.models.CommunistGovernment;
import id.nationcore.models.CommunistGovernment.PolitburoPosition;
import id.nationcore.models.Government;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.MonarchyGovernment;
import id.nationcore.models.MonarchyGovernment.HighCouncilPosition;
import id.nationcore.models.CaliphateGovernment;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class ChatListener implements Listener {

    private final NationCore plugin;
    public static final java.util.Set<UUID> pendingBroadcasts = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public ChatListener(NationCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Media Censorship (Communist Propaganda Minister decision): check if player
        // is a member of a COMMUNIST nation with active Media Censorship AND
        // has not been censored yet. If so, replace message with template & mark.
        Nation senderNation = plugin.getNationManager().getNationOf(uuid);
        if (senderNation != null && senderNation.getType() == GovernmentType.COMMUNIST) {
            CommunistGovernment cg = senderNation.getCommunistGovernment();
            if (cg != null && cg.isSensorMediaActive()
                    && !cg.getCensorshipUsedOn().contains(uuid)
                    && !player.hasPermission("nation.admin")) {
                cg.getCensorshipUsedOn().add(uuid);
                String replacement = cg.getCensorshipReplacement() != null
                        ? cg.getCensorshipReplacement()
                        : "§c[CENSORED]";
                event.message(net.kyori.adventure.text.Component.text(replacement));
            }
        }

        // Royal Press Censorship — same mechanic, MONARCHY variant.
        if (senderNation != null && senderNation.getType() == GovernmentType.MONARCHY) {
            MonarchyGovernment mg = senderNation.getMonarchyGovernment();
            if (mg != null && mg.isSensorMediaActive()
                    && !mg.getCensorshipUsedOn().contains(uuid)
                    && !player.hasPermission("nation.admin")) {
                mg.getCensorshipUsedOn().add(uuid);
                String replacement = mg.getCensorshipReplacement() != null
                        ? mg.getCensorshipReplacement()
                        : "§6[CENSORED BY ROYAL HERALD]";
                event.message(net.kyori.adventure.text.Component.text(replacement));
            }
        }

        // Capture nation name when player is in pending creation mode.
        NationManager nationMgr = plugin.getNationManager();
        if (nationMgr != null && nationMgr.hasPendingCreation(uuid)) {
            event.setCancelled(true);
            String input = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(event.message()).trim();

            if (input.equalsIgnoreCase("cancel") || input.equalsIgnoreCase("batal")) {
                nationMgr.consumePendingCreation(uuid);
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () ->
                        MessageUtils.send(player, "<yellow>Nation creation cancelled.</yellow>"));
                return;
            }

            String validation = NationManager.validateNationName(input);
            if (validation != null) {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () ->
                        MessageUtils.send(player, "<red>" + validation + " Try again or type 'cancel'.</red>"));
                return;
            }

            GovernmentType type = nationMgr.consumePendingCreation(uuid);
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                NationManager.Result result = nationMgr.createNation(player, input, type);
                if (result.isSuccess()) {
                    MessageUtils.send(player, "");
                    MessageUtils.send(player, "<gold>═══════════════════════════════════════</gold>");
                    MessageUtils.send(player, "<green><b>" + result.getMessage() + "</b></green>");
                    MessageUtils.send(player, "<yellow>You are now the " +
                            type.getLeaderTitle() + " of " + result.getNation().getName() + ".</yellow>");
                    MessageUtils.send(player, "<gold>═══════════════════════════════════════</gold>");
                    org.bukkit.Bukkit.broadcastMessage("§eNew nation established: §6" +
                            result.getNation().getName() + " §e(" + type.getDisplayName() +
                            ") §7led by §f" + player.getName());
                } else {
                    MessageUtils.send(player, "<red>" + result.getMessage() + "</red>");
                }
            });
            return;
        }

        // Capture new name when player is in pending rename mode.
        if (nationMgr != null && nationMgr.hasPendingRename(uuid)) {
            event.setCancelled(true);
            String input = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(event.message()).trim();

            if (input.equalsIgnoreCase("cancel") || input.equalsIgnoreCase("batal")) {
                nationMgr.consumePendingRename(uuid);
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () ->
                        MessageUtils.send(player, "<yellow>Nation rename cancelled.</yellow>"));
                return;
            }

            String validation = NationManager.validateNationName(input);
            if (validation != null) {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () ->
                        MessageUtils.send(player, "<red>" + validation + " Try again or type 'cancel'.</red>"));
                return;
            }

            Nation nation = nationMgr.consumePendingRename(uuid);
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                String oldName = nation.getName();
                nation.setName(input);
                plugin.getDataManager().saveNations();
                MessageUtils.send(player, "<green>Nation name successfully changed to '" + input + "'.</green>");
                org.bukkit.Bukkit.broadcastMessage("§eNation §6" + oldName + " §ehas changed name to §6" + input + "§e.");
            });
            return;
        }

        if (pendingBroadcasts.remove(uuid)) {
            event.setCancelled(true);
            String messageStr = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(event.message());

            plugin.getDataManager().getGovernment().setLastBroadcastTime(System.currentTimeMillis());

            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                id.nationcore.utils.MessageUtils.broadcast("");
                id.nationcore.utils.MessageUtils.broadcast("government.broadcast_header");
                id.nationcore.utils.MessageUtils.broadcast("<gold>═══════════════════════════════════════");
                id.nationcore.utils.MessageUtils.broadcast("<yellow>" + messageStr);
                id.nationcore.utils.MessageUtils.broadcast("<gold>═══════════════════════════════════════");
                id.nationcore.utils.MessageUtils.broadcast("");

                for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
                }
            });
            return;
        }

        // Resolve government for player context — per-nation if joined,
        // legacy global if not. COMMUNIST has its own prefix (see
        // getCommunistPrefix); Government object only used for REPUBLIC.
        Nation nation = plugin.getNationManager().getNationOf(uuid);
        Component prefix;
        if (nation != null && nation.getType() == GovernmentType.COMMUNIST) {
            prefix = getCommunistPrefix(uuid, nation);
        } else if (nation != null && nation.getType() == GovernmentType.MONARCHY) {
            prefix = getMonarchyPrefix(uuid, nation);
        } else if (nation != null && nation.getType() == GovernmentType.CALIPHATE) {
            prefix = getCaliphatePrefix(uuid, nation);
        } else {
            Government gov = nation != null && nation.getType() == GovernmentType.REPUBLIC
                    ? nation.getRepublicGovernment()
                    : plugin.getDataManager().getGovernment();
            prefix = getGovernmentPrefix(uuid, gov, nation);
        }

        if (prefix != null) {
            event.renderer((source, sourceDisplayName, message, viewer) -> {
                return prefix
                        .append(Component.space())
                        .append(sourceDisplayName)
                        .append(Component.text(": ", NamedTextColor.GRAY))
                        .append(message);
            });
        }
    }

    private Component getGovernmentPrefix(UUID uuid, Government gov, Nation nation) {
        if (gov == null) return getArenaOrFallback(uuid);

        // Nation name suffix for president/ministers within the nation.
        String nationSuffix = nation != null ? " " + nation.getName() : "";

        if (gov.hasPresident() && gov.getPresidentUUID().equals(uuid)) {
            String label = nation != null
                    ? nation.getType().getLeaderTitle() + nationSuffix
                    : "President";
            return Component.text("👑", NamedTextColor.GOLD)
                    .append(Component.text("[", NamedTextColor.GRAY))
                    .append(Component.text(label, NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text("]", NamedTextColor.GRAY));
        }

        for (Government.CabinetPosition pos : Government.CabinetPosition.values()) {
            UUID minister = gov.getCabinetMember(pos);
            if (minister != null && minister.equals(uuid)) {
                return getCabinetPrefix(pos, nationSuffix);
            }
        }

        return getArenaOrFallback(uuid);
    }

    /**
     * Special prefix for players in COMMUNIST nation: 🚩 Secretary General Soviet,
     * 📢 Minister of Propaganda Soviet, etc.
     */
    private Component getCommunistPrefix(UUID uuid, Nation nation) {
        CommunistGovernment cg = nation.getCommunistGovernment();
        if (cg == null) return getArenaOrFallback(uuid);

        String suffix = " " + nation.getName();

        if (cg.hasSecretaryGeneral() && cg.getSecretaryGeneralUUID().equals(uuid)) {
            return Component.text("🚩", NamedTextColor.RED)
                    .append(Component.text("[", NamedTextColor.GRAY))
                    .append(Component.text("Secretary General" + suffix,
                            NamedTextColor.RED, TextDecoration.BOLD))
                    .append(Component.text("]", NamedTextColor.GRAY));
        }

        PolitburoPosition pos = cg.getPositionByUUID(uuid);
        if (pos != null) {
            NamedTextColor color = switch (pos) {
                case PROPAGANDA -> NamedTextColor.LIGHT_PURPLE;
                case DEFENSE    -> NamedTextColor.RED;
                case TREASURY   -> NamedTextColor.GOLD;
                case HEALTH     -> NamedTextColor.GREEN;
            };
            String icon = switch (pos) {
                case PROPAGANDA -> "📢";
                case DEFENSE    -> "🛡";
                case TREASURY   -> "💰";
                case HEALTH     -> "💉";
            };
            return Component.text(icon, color)
                    .append(Component.text("[", NamedTextColor.GRAY))
                    .append(Component.text(pos.getDisplayName() + suffix, color))
                    .append(Component.text("]", NamedTextColor.GRAY));
        }

        return getArenaOrFallback(uuid);
    }

    /**
     * Royal chat prefix: 👑 King Kingdom, ⚔ Marshal Kingdom, 💰 Chancellor
     * Kingdom, ✚ Saint Kingdom, 📜 Herald Kingdom.
     */
    private Component getMonarchyPrefix(UUID uuid, Nation nation) {
        MonarchyGovernment mg = nation.getMonarchyGovernment();
        if (mg == null) return getArenaOrFallback(uuid);

        String suffix = " " + nation.getName();

        if (mg.hasKing() && mg.getKingUUID().equals(uuid)) {
            return Component.text("👑", NamedTextColor.GOLD)
                    .append(Component.text("[", NamedTextColor.GRAY))
                    .append(Component.text("King" + suffix,
                            NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text("]", NamedTextColor.GRAY));
        }

        HighCouncilPosition pos = mg.getPositionByUUID(uuid);
        if (pos != null) {
            NamedTextColor color = switch (pos) {
                case HERALD     -> NamedTextColor.LIGHT_PURPLE;
                case MARSHAL    -> NamedTextColor.RED;
                case CHANCELLOR -> NamedTextColor.GOLD;
                case SAINT      -> NamedTextColor.GREEN;
            };
            String icon = switch (pos) {
                case HERALD     -> "📜";
                case MARSHAL    -> "⚔";
                case CHANCELLOR -> "💰";
                case SAINT      -> "✚";
            };
            return Component.text(icon, color)
                    .append(Component.text("[", NamedTextColor.GRAY))
                    .append(Component.text(pos.getDisplayName() + suffix, color))
                    .append(Component.text("]", NamedTextColor.GRAY));
        }

        return getArenaOrFallback(uuid);
    }

    /**
     * Caliphate chat prefix: ☪ Caliph Caliphate, 📜 Shura Caliphate,
     * ✦ Scholar Caliphate.
     */
    private Component getCaliphatePrefix(UUID uuid, Nation nation) {
        CaliphateGovernment cg = nation.getCaliphateGovernment();
        if (cg == null) return getArenaOrFallback(uuid);

        String suffix = " " + nation.getName();

        if (cg.hasCaliph() && cg.getCaliphUUID().equals(uuid)) {
            return Component.text("☪", NamedTextColor.DARK_GREEN)
                    .append(Component.text("[", NamedTextColor.GRAY))
                    .append(Component.text("Caliph" + suffix,
                            NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                    .append(Component.text("]", NamedTextColor.GRAY));
        }

        if (cg.isShuraMember(uuid)) {
            return Component.text("📜", NamedTextColor.GREEN)
                    .append(Component.text("[", NamedTextColor.GRAY))
                    .append(Component.text("Shura" + suffix, NamedTextColor.GREEN))
                    .append(Component.text("]", NamedTextColor.GRAY));
        }

        if (cg.isScholar(uuid)) {
            return Component.text("✦", NamedTextColor.AQUA)
                    .append(Component.text("[", NamedTextColor.GRAY))
                    .append(Component.text("Scholar" + suffix, NamedTextColor.AQUA))
                    .append(Component.text("]", NamedTextColor.GRAY));
        }

        return getArenaOrFallback(uuid);
    }

    private Component getArenaOrFallback(UUID uuid) {
        if (plugin.getArenaManager().isInArena(uuid)) {
            ArenaSession session = plugin.getArenaManager().getCurrentSession();
            int kills = 0;
            if (session != null) {
                ArenaSession.ArenaStats stats = session.getPlayerStats().get(uuid);
                if (stats != null) {
                    kills = stats.getKills();
                }
            }
            if (kills >= 10) {
                return Component.text("⚔", NamedTextColor.RED)
                        .append(Component.text("[", NamedTextColor.GRAY))
                        .append(Component.text("Arena Champion", NamedTextColor.RED))
                        .append(Component.text("]", NamedTextColor.GRAY));
            } else if (kills >= 5) {
                return Component.text("⚔", NamedTextColor.YELLOW)
                        .append(Component.text("[", NamedTextColor.GRAY))
                        .append(Component.text("Arena Fighter", NamedTextColor.YELLOW))
                        .append(Component.text("]", NamedTextColor.GRAY));
            }
        }

        return null;
    }

    private Component getCabinetPrefix(Government.CabinetPosition position, String nationSuffix) {
        NamedTextColor color;
        String icon;

        switch (position) {
            case DEFENSE -> {
                color = NamedTextColor.RED;
                icon = "🛡";
            }
            case TREASURY -> {
                color = NamedTextColor.GOLD;
                icon = "💰";
            }
            default -> {
                color = NamedTextColor.WHITE;
                icon = "📋";
            }
        }

        String label = position.getDisplayName() + (nationSuffix != null ? nationSuffix : "");
        return Component.text(icon, color)
                .append(Component.text("[", NamedTextColor.GRAY))
                .append(Component.text(label, color))
                .append(Component.text("]", NamedTextColor.GRAY));
    }
}
