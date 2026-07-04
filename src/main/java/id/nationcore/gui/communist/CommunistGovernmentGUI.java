package id.nationcore.gui.communist;

import id.nationcore.gui.GovernmentGUIUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import id.nationcore.NationCore;
import id.nationcore.models.CommunistGovernment;
import id.nationcore.models.Nation;
import id.nationcore.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class CommunistGovernmentGUI {

        private final NationCore plugin;
        public static final String TITLE = "§c§lCOMMUNIST GOVERNMENT";

        private static final int[] FILLER_SLOTS = {
                        0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53
        };

        public CommunistGovernmentGUI(NationCore plugin) {
                this.plugin = plugin;
        }

        public void open(Player player, Nation nation) {
                if (nation == null)
                        return;

                CommunistGovernment cg = nation.getCommunistGovernment();
                if (cg == null)
                        return;

                boolean isSekjen = cg.hasSecretaryGeneral()
                                && cg.getSecretaryGeneralUUID().equals(player.getUniqueId());
                boolean isPolitburo = cg.getPolitburoMemberByUUID(player.getUniqueId()) != null;

                if (!isSekjen && !isPolitburo) {
                        MessageUtils.send(player,
                                        "§cOnly the Secretary General and Politburo can open the Government GUI.");
                        return;
                }

                Inventory inv = Bukkit.createInventory(null, 54, TITLE);

                ItemStack filler = GovernmentGUIUtils.createItem(Material.RED_STAINED_GLASS_PANE, " ");
                for (int slot : FILLER_SLOTS) {
                        inv.setItem(slot, filler);
                }

                inv.setItem(4, buildNationProfile(nation, cg));

                // Minister Offices
                String propagandaMinisterName = "Vacant";
                String defenseMinisterName = "Vacant";
                String treasuryMinisterName = "Vacant";
                String healthMinisterName = "Vacant";

                CommunistGovernment.PolitburoMember propMember = cg.getPolitburoMember(CommunistGovernment.PolitburoPosition.PROPAGANDA);
                if (propMember != null) {
                        OfflinePlayer op = Bukkit.getOfflinePlayer(propMember.getUuid());
                        if (op.getName() != null) propagandaMinisterName = op.getName();
                }
                CommunistGovernment.PolitburoMember defMember = cg.getPolitburoMember(CommunistGovernment.PolitburoPosition.DEFENSE);
                if (defMember != null) {
                        OfflinePlayer op = Bukkit.getOfflinePlayer(defMember.getUuid());
                        if (op.getName() != null) defenseMinisterName = op.getName();
                }
                CommunistGovernment.PolitburoMember treMember = cg.getPolitburoMember(CommunistGovernment.PolitburoPosition.TREASURY);
                if (treMember != null) {
                        OfflinePlayer op = Bukkit.getOfflinePlayer(treMember.getUuid());
                        if (op.getName() != null) treasuryMinisterName = op.getName();
                }
                CommunistGovernment.PolitburoMember heaMember = cg.getPolitburoMember(CommunistGovernment.PolitburoPosition.HEALTH);
                if (heaMember != null) {
                        OfflinePlayer op = Bukkit.getOfflinePlayer(heaMember.getUuid());
                        if (op.getName() != null) healthMinisterName = op.getName();
                }

                inv.setItem(10, createOfficeItem(Material.DIAMOND_HELMET, "§e§lMinister of Health Office", healthMinisterName));
                inv.setItem(12, createOfficeItem(Material.CHAINMAIL_HELMET, "§e§lMinister of Propaganda Office", propagandaMinisterName));
                inv.setItem(14, createOfficeItem(Material.NETHERITE_HELMET, "§e§lMinister of Defense Office", defenseMinisterName));
                inv.setItem(16, createOfficeItem(Material.GOLDEN_HELMET, "§e§lMinister of Treasury Office", treasuryMinisterName));


                // Announcement Button (Slot 28)
                java.util.List<String> announcementLore = new java.util.ArrayList<>();
                announcementLore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                announcementLore.add("§7Update the announcement message");
                announcementLore.add("§7shown on the main menu.");
                announcementLore.add("");
                announcementLore.add("§7Cooldown: §f12 hours");

                long lastAnn = nation.getLastAnnouncementTime();
                long timeSinceLastAnn = System.currentTimeMillis() - lastAnn;
                long cooldownDurationAnn = 12L * 60 * 60 * 1000;
                boolean onCooldownAnn = timeSinceLastAnn < cooldownDurationAnn;

                if (!isSekjen) {
                        announcementLore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                        announcementLore.add("§cOnly the Secretary General can use this.");
                } else if (onCooldownAnn) {
                        long remainingAnn = cooldownDurationAnn - timeSinceLastAnn;
                        announcementLore.add("§7Status: §cOn Cooldown");
                        announcementLore.add("§7Remaining: §f" + MessageUtils.formatTime(remainingAnn));
                        announcementLore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                        announcementLore.add("§cThis action is on cooldown.");
                } else {
                        announcementLore.add("§7Status: §aReady");
                        announcementLore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                        announcementLore.add("§eClick to update message");
                }

                inv.setItem(28, GovernmentGUIUtils.createItem(Material.WRITTEN_BOOK, "§e§lSet Announcement Message",
                                announcementLore.toArray(new String[0])));

                inv.setItem(30, GovernmentGUIUtils.createItem(Material.WRITABLE_BOOK, "§e§lBorder Management",
                                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                                "§7Manage your nation's territory.",
                                "§7• Claim & release chunks",
                                "§7• Reallocate your capital",
                                "§7• Toggle border visualization",
                                "§7• Set a territory welcome message",
                                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                                "§aClick to open."));

                inv.setItem(31, GovernmentGUIUtils.createItem(Material.WRITABLE_BOOK,
                                "§e§lNation Member Management",
                                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                                "§7View and manage all nation members.",
                                "§7• Kick members",
                                "§7• Promote to Party",
                                "§7• Appoint to Politburo",
                                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                                "§eClick to open."));

                inv.setItem(32, GovernmentGUIUtils.createItem(Material.WRITABLE_BOOK,
                                "§e§lDiplomacy Management",
                                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                                "§7Manage your foreign relations",
                                "§7with every other nation.",
                                "§7• Review each nation's current status",
                                "§7• Propose Peace, Alliance, Truce or War",
                                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                                "§aClick to open."));

                inv.setItem(34, GovernmentGUIUtils.createItem(Material.KNOWLEDGE_BOOK, "§a§lSalary Claim",
                                "§7Secretary General & Politburo",
                                "§7Daily Salary Claim",
                                "",
                                "§eClick to open"));

                // Nation Settings Button (Slot 37)
                inv.setItem(37, GovernmentGUIUtils.createItem(
                                Material.COMMAND_BLOCK_MINECART,
                                "§e§lNation Settings",
                                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                                "§7Configure your nation settings.",
                                "§7• Rename nation & TAG",
                                "§7• Toggle Tax system",
                                "§7• Disband nation",
                                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                                "§eClick to open settings",
                                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
                ));

                // Executive Order (Slot 39)
                inv.setItem(39, GovernmentGUIUtils.createItem(Material.WRITABLE_BOOK, "§e§lExecutive Order",
                                "§7Manage executive orders for the nation",
                                "",
                                "§eClick to open"));

                // Event Management (Slot 40)
                inv.setItem(40, GovernmentGUIUtils.createItem(Material.WRITABLE_BOOK, "§e§lEvent Management",
                                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                                "§7This feature is currently",
                                "§7under development.",
                                "",
                                "§c⚠ Coming Soon",
                                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

                // Broadcast Message (Slot 41)
                long lastBroadcast = cg.getLastBroadcastTime();
                long timeSinceLast = System.currentTimeMillis() - lastBroadcast;
                long cooldownDuration = 6L * 60 * 60 * 1000; // 6 hours
                boolean onCooldown = timeSinceLast < cooldownDuration;

                List<String> broadcastLore = new ArrayList<>();
                broadcastLore.add("§7Broadcast a custom message to all members.");
                broadcastLore.add("§7Cooldown: §f6 hours");
                if (!isSekjen) {
                        broadcastLore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                        broadcastLore.add("§cOnly the Secretary General can use this.");
                } else if (onCooldown) {
                        long remaining = cooldownDuration - timeSinceLast;
                        broadcastLore.add("");
                        broadcastLore.add("§7Status: §cOn Cooldown");
                        broadcastLore.add("§7Remaining: §f" + MessageUtils.formatTime(remaining));
                        broadcastLore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                        broadcastLore.add("§cThis action is on cooldown.");
                } else {
                        broadcastLore.add("");
                        broadcastLore.add("§7Status: §aReady");
                        broadcastLore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                        broadcastLore.add("§eClick to broadcast");
                }

                inv.setItem(41, GovernmentGUIUtils.createItem(Material.WRITABLE_BOOK, "§e§lBroadcast Message",
                                broadcastLore.toArray(new String[0])));

                inv.setItem(43, GovernmentGUIUtils.createItem(Material.PALE_OAK_DOOR, "§c§lBack",
                                "§7Return to previous menu"));

                player.openInventory(inv);
        }

        private ItemStack buildNationProfile(Nation nation, CommunistGovernment cg) {
                int memberCount = nation.getMemberCount();
                int partyCount = cg != null ? cg.getPartyMemberCount() : 0;

                return GovernmentGUIUtils.createItem(Material.GLOW_ITEM_FRAME,
                                "§c§l" + nation.getName(),
                                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                                "§7Government : §fCommunist",
                                "§7Tag        : §f[" + nation.getTag() + "]",
                                "§7Members    : §f" + memberCount,
                                "§7Party      : §f" + partyCount,
                                "§8Displays information about the nation");
        }

        private ItemStack createOfficeItem(Material material, String name, String ministerName) {
                List<String> lore = new ArrayList<>();
                lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                lore.add("§7Manage and execute specific");
                lore.add("§7cabinet decisions & policies.");
                lore.add("");
                lore.add("§7Minister: §f" + ministerName);
                lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                lore.add("§eClick to open office console");
                lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

                ItemStack item = GovernmentGUIUtils.createItem(material, name, lore.toArray(new String[0]));
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                        try {
                                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(this.plugin, "dummy_hide_attrs");
                                org.bukkit.attribute.AttributeModifier modifier = new org.bukkit.attribute.AttributeModifier(
                                    key,
                                    0.0,
                                    org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER,
                                    org.bukkit.inventory.EquipmentSlotGroup.ANY
                                );
                                meta.addAttributeModifier(org.bukkit.attribute.Attribute.ARMOR, modifier);
                        } catch (Throwable t) {
                                // fallback or ignore if not supported
                        }
                        for (org.bukkit.inventory.ItemFlag flag : org.bukkit.inventory.ItemFlag.values()) {
                                meta.addItemFlags(flag);
                        }
                        item.setItemMeta(meta);
                }
                return item;
        }
}
