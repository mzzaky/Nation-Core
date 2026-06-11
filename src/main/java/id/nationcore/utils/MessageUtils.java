package id.nationcore.utils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import id.nationcore.NationCore;
import id.nationcore.models.Nation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;

public class MessageUtils {

    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();

    private static final Map<String, String> messages = new HashMap<>();

    public static Component parse(String message) {
        if (message.contains("&")) {
            return legacy.deserialize(message);
        } else {
            return mm.deserialize(message);
        }
    }

    /**
     * Renders a string using ONLY legacy ampersand colour codes. Use this for
     * player-provided content (e.g. nation welcome messages) so MiniMessage tags
     * embedded in user input are treated as literal text instead of being
     * interpreted — preventing tag injection.
     */
    public static Component parseLegacy(String message) {
        return legacy.deserialize(message == null ? "" : message);
    }

    public static Component prefix() {
        return parse(NationCore.getInstance().getPrefix() + " ");
    }

    public static void send(Player player, String message) {
        String msgContent = messages.containsKey(message) ? messages.get(message) : message;
        player.sendMessage(prefix().append(parse(msgContent)));
    }

    public static void send(CommandSender sender, String message) {
        if (sender instanceof Player player) {
            send(player, message);
        } else {
            String msgContent = messages.containsKey(message) ? messages.get(message) : message;
            sender.sendMessage(prefix().append(parse(msgContent)));
        }
    }

    public static void sendRaw(Player player, String message) {
        String msgContent = messages.containsKey(message) ? messages.get(message) : message;
        player.sendMessage(parse(msgContent));
    }

    public static void broadcast(String message) {
        String msgContent = messages.containsKey(message) ? messages.get(message) : message;
        Component msg = prefix().append(parse(msgContent));
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg));
        Bukkit.getConsoleSender().sendMessage(msg);
    }

    public static void broadcastRaw(String message) {
        String msgContent = messages.containsKey(message) ? messages.get(message) : message;
        Component msg = parse(msgContent);
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg));
        Bukkit.getConsoleSender().sendMessage(msg);
    }

    public static void broadcastAnnouncement(String title, String message) {
        String titleContent = messages.containsKey(title) ? messages.get(title) : title;
        String msgContent = messages.containsKey(message) ? messages.get(message) : message;

        Component border = Component.text("═".repeat(50)).color(NamedTextColor.GOLD);
        Component titleComp = Component.text("📜 " + titleContent).color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD);
        Component msgComp = parse(msgContent);

        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendMessage(Component.empty());
            p.sendMessage(border);
            p.sendMessage(titleComp);
            p.sendMessage(msgComp);
            p.sendMessage(border);
            p.sendMessage(Component.empty());
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        });
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        String titleContent = messages.containsKey(title) ? messages.get(title) : title;
        String subtitleContent = messages.containsKey(subtitle) ? messages.get(subtitle) : subtitle;

        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L));
        Title titleObj = Title.title(parse(titleContent), parse(subtitleContent), times);
        player.showTitle(titleObj);
    }

    public static void broadcastTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Bukkit.getOnlinePlayers().forEach(p -> sendTitle(p, title, subtitle, fadeIn, stay, fadeOut));
    }

    public static void sendActionBar(Player player, String message) {
        String msgContent = messages.containsKey(message) ? messages.get(message) : message;
        player.sendActionBar(parse(msgContent));
    }

    public static void broadcastActionBar(String message) {
        String msgContent = messages.containsKey(message) ? messages.get(message) : message;
        Component msg = parse(msgContent);
        Bukkit.getOnlinePlayers().forEach(p -> p.sendActionBar(msg));
    }

    public static String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }

    public static String formatTimeShort(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " days";
        } else if (hours > 0) {
            return hours + " hours";
        } else if (minutes > 0) {
            return minutes + " minutes";
        } else {
            return seconds + " seconds";
        }
    }

    public static String formatNumber(double number) {
        if (number >= 1_000_000) {
            return String.format("%.2fM", number / 1_000_000);
        } else if (number >= 1_000) {
            return String.format("%.2fK", number / 1_000);
        }
        return String.format("%.0f", number);
    }

    public static void playSound(Player player, Sound sound) {
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }

    public static void broadcastSound(Sound sound) {
        Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), sound, 1.0f, 1.0f));
    }

    // Language system methods
    public static void loadLanguage() {
        messages.clear();
        loadMessagesFromConfig(NationCore.getInstance().getLanguageConfig());

        // Debug logging
        NationCore.getInstance().getLogger().info("Loaded " + messages.size() + " messages from language.yml");
        if (messages.size() > 0) {
            // Show first 5 keys as example
            int count = 0;
            for (String key : messages.keySet()) {
                if (count++ < 5) {
                    NationCore.getInstance().getLogger().info("  Example key: " + key + " -> "
                            + messages.get(key).substring(0, Math.min(30, messages.get(key).length())) + "...");
                }
            }
        } else {
            NationCore.getInstance().getLogger().warning("No messages loaded! Check language.yml structure.");
        }
    }

    public static void reloadLanguage() {
        loadLanguage();
    }

    private static void loadMessagesFromConfig(org.bukkit.configuration.file.FileConfiguration config) {
        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                String message = config.getString(key);
                if (message != null) {
                    messages.put(key, message);
                }
            }
        }
    }

    public static String getMessage(String key) {
        return messages.getOrDefault(key, "&cMessage not found: " + key);
    }

    public static String getMessage(String key, Object... args) {
        String message = getMessage(key);
        for (int i = 0; i < args.length; i += 2) {
            if (i + 1 < args.length) {
                String placeholder = "{" + args[i] + "}";
                String value = String.valueOf(args[i + 1]);
                message = message.replace(placeholder, value);
            }
        }
        return message;
    }

    // Overloaded send methods for message keys
    public static void send(Player player, String key, Object... args) {
        send(player, getMessage(key, args));
    }

    public static void send(CommandSender sender, String key, Object... args) {
        send(sender, getMessage(key, args));
    }

    public static void broadcast(String key, Object... args) {
        broadcast(getMessage(key, args));
    }

    public static void broadcastRaw(String key, Object... args) {
        broadcastRaw(getMessage(key, args));
    }

    /**
     * Send a message only to online members of the given nation.
     */
    public static void sendToNation(Nation nation, String message) {
        if (nation == null) return;
        String msgContent = messages.containsKey(message) ? messages.get(message) : message;
        Component msg = prefix().append(parse(msgContent));
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> nation.isMember(p.getUniqueId()))
                .forEach(p -> p.sendMessage(msg));
    }

    /**
     * Send a title only to online members of the given nation.
     */
    public static void sendToNationTitle(Nation nation, String title, String subtitle,
                                         int fadeIn, int stay, int fadeOut) {
        if (nation == null) return;
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> nation.isMember(p.getUniqueId()))
                .forEach(p -> sendTitle(p, title, subtitle, fadeIn, stay, fadeOut));
    }
}
