package id.nationcore.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import id.nationcore.NationCore;
import id.nationcore.models.CabinetDecision;
import id.nationcore.models.Government;
import id.nationcore.models.GovernmentType;
import id.nationcore.models.Nation;
import id.nationcore.models.Treasury;
import id.nationcore.utils.MessageUtils;

public class CabinetManager {
    
    private final NationCore plugin;
    private final Map<UUID, Map<CabinetDecision.DecisionType, Long>> decisionCooldowns = new HashMap<>();
    private final Map<CabinetDecision.DecisionType, Long> activeDecisions = new HashMap<>();
    private final Map<CabinetDecision.DecisionType, Long> decisionEndTimes = new HashMap<>();
    
    // Active effect tracking
    private final Set<UUID> militaryDraftPlayers = new HashSet<>();
    private final Set<UUID> taxHolidayPlayers = new HashSet<>();
    private final Map<UUID, Double> shopDiscounts = new HashMap<>();
    private double globalDropMultiplier = 1.0;
    private double globalXpMultiplier = 1.0;

    
    public CabinetManager(NationCore plugin) {
        this.plugin = plugin;
    }
    
    public boolean canExecuteDecision(UUID ministerId, CabinetDecision.DecisionType type) {
        Government gov = plugin.getDataManager().getGovernment();
        GovernmentManager govManager = plugin.getGovernmentManager();
        
        // Check if player is the correct minister for this decision
        Government.CabinetPosition requiredPosition = getRequiredPosition(type);
        if (requiredPosition == null) return false;
        
        UUID currentMinister = gov.getCabinetMember(requiredPosition);
        if (currentMinister == null || !currentMinister.equals(ministerId)) {
            return false;
        }
        
        // Check cooldown
        long cooldownMs = plugin.getConfig().getLong("cabinet.decision-cooldown-hours", 48) * 3600000L;
        Map<CabinetDecision.DecisionType, Long> playerCooldowns = decisionCooldowns.computeIfAbsent(ministerId, k -> new HashMap<>());
        Long lastUse = playerCooldowns.get(type);
        if (lastUse != null && System.currentTimeMillis() - lastUse < cooldownMs) {
            return false;
        }
        
        // Check treasury cost
        int cost = getDecisionCost(type);
        Treasury treasury = plugin.getDataManager().getTreasury();
        return treasury.getBalance() >= cost;
    }
    
    public long getRemainingCooldown(UUID ministerId, CabinetDecision.DecisionType type) {
        long cooldownMs = plugin.getConfig().getLong("cabinet.decision-cooldown-hours", 48) * 3600000L;
        Map<CabinetDecision.DecisionType, Long> playerCooldowns = decisionCooldowns.get(ministerId);
        if (playerCooldowns == null) return 0;
        Long lastUse = playerCooldowns.get(type);
        if (lastUse == null) return 0;
        long remaining = cooldownMs - (System.currentTimeMillis() - lastUse);
        return Math.max(0, remaining);
    }
    
    public boolean executeDecision(UUID ministerId, CabinetDecision.DecisionType type) {
        if (!canExecuteDecision(ministerId, type)) {
            return false;
        }
        
        int cost = getDecisionCost(type);
        plugin.getTreasuryManager().withdraw(Treasury.TransactionType.EXECUTIVE_ORDER, (double) cost, "Cabinet Decision: " + type.name(), ministerId);
        
        // Set cooldown
        decisionCooldowns.computeIfAbsent(ministerId, k -> new HashMap<>()).put(type, System.currentTimeMillis());
        
        // Create decision record
        Government.CabinetPosition position = getRequiredPosition(type);
        CabinetDecision.CabinetPosition cabPosition = CabinetDecision.CabinetPosition.valueOf(position.name());
        CabinetDecision decision = new CabinetDecision(type, cabPosition, ministerId);
        plugin.getDataManager().getActiveDecisions().add(decision);
        
        // Track active decision
        activeDecisions.put(type, System.currentTimeMillis());
        decisionEndTimes.put(type, System.currentTimeMillis() + getDecisionDuration(type));
        
        // Execute the decision effect
        Nation n = plugin.getNationManager().getNationOf(ministerId);
        applyDecisionEffect(n, type, decision);
        
        // Broadcast
        Player minister = Bukkit.getPlayer(ministerId);
        String ministerName = minister != null ? minister.getName() : "Unknown";
        MessageUtils.broadcast("<gold>📜 <yellow>Cabinet Decision: <white>" + getDecisionDisplayName(type));
        MessageUtils.broadcast("<gray>Issued by Minister <white>" + ministerName);
        MessageUtils.broadcast("<gray>Duration: <white>" + MessageUtils.formatTime(getDecisionDuration(type)));
        
        plugin.getDataManager().saveAll();
        return true;
    }
    
    private void applyDecisionEffect(Nation nation, CabinetDecision.DecisionType type, CabinetDecision decision) {
        if (nation == null) {
            nation = plugin.getNationManager().getNationOf(decision.getIssuedBy());
        }
        switch (type) {
            // Defense Ministry Decisions
            case DECLARE_WAR -> startWarGamesEvent();
            case MILITARY_DRAFT -> activateMilitaryDraft();
            case DEFENSE_PROTOCOL -> activateNationalProtection();
            case ARMORY_DISCOUNT -> activateArmoryDiscount();
            case BORDER_PATROL -> activateBorderPatrol();
            
            // Treasury Ministry Decisions
            case TAX_HOLIDAY -> activateTaxHoliday();
            case ECONOMIC_STIMULUS -> distributeEconomicStimulus();
            case AUCTION_BOOST -> activateAuctionBoost();
            case TREASURY_BONUS -> distributeTreasuryBonus();
            case MARKET_CRASH -> triggerMarketCrash();

            // Health Ministry Decisions
            case QUARANTINE_PROTOCOL -> activateQuarantineProtocol(nation);
            case FIELD_MEDICINE -> activateFieldMedicine(nation);
            case VACCINATION_DRIVE -> activateVaccinationDrive(nation);
            case EMERGENCY_RATIONS -> activateEmergencyRations(nation);
            case PLAGUE -> activatePlague(nation);
        }
    }

    private void activateQuarantineProtocol(Nation nation) {
        if (nation == null) return;
        for (UUID uuid : nation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) MessageUtils.send(p, "<aqua>💉 Quarantine Protocol active!</aqua> <gray>Non-members cannot enter territory for 10 minutes.</gray>");
        }
    }

    private void activateFieldMedicine(Nation nation) {
        if (nation == null) return;
        int ticks = 5 * 60 * 20;
        for (UUID memberUUID : nation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(memberUUID);
            if (p != null && p.isOnline()) {
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, ticks, 1, false, true, true));
                MessageUtils.send(p, "<green>💉 Field Medicine: <gray>Regeneration II for 5 minutes.</gray>");
            }
        }
    }

    private void activateVaccinationDrive(Nation nation) {
        if (nation == null) return;
        for (UUID uuid : nation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) MessageUtils.send(p, "<green>💉 Vaccination Drive aktif!</green> <gray>Anggota kebal poison & wither selama 1 jam.</gray>");
        }
    }

    private void activateEmergencyRations(Nation nation) {
        if (nation == null) return;
        int distributed = 0;
        for (UUID memberUUID : nation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(memberUUID);
            if (p == null || !p.isOnline()) continue;
            if (p.getFoodLevel() >= 10) continue;
            ItemStack bread = new ItemStack(Material.BREAD, 8);
            var leftover = p.getInventory().addItem(bread);
            for (var dropItem : leftover.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), dropItem);
            }
            MessageUtils.send(p, "<gold>🍞 Emergency Rations: <gray>8 bread for you.</gray>");
            distributed++;
        }
        for (UUID uuid : nation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                MessageUtils.send(p, "<gold>🍞 Emergency Rations distributed to " + distributed + " members with low hunger.");
            }
        }
    }

    private void activatePlague(Nation nation) {
        if (nation == null) return;
        for (UUID uuid : nation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) MessageUtils.send(p, "<dark_red>☠ Plague active!</dark_red> <gray>Enemies entering territory will receive Weakness II + Hunger for 30 seconds. Active for 10 minutes.</gray>");
        }
    }
    
    // ==================== DEFENSE MINISTRY DECISIONS ====================
    
    private void startWarGamesEvent() {
        MessageUtils.broadcast("<red>⚔ <dark_red>WAR GAMES EVENT <red>⚔");
        MessageUtils.broadcast("<gray>PvP rewards doubled! Special combat challenges active!");
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 72000, 0)); // 1 hour
        }
    }
    
    private void activateMilitaryDraft() {
        MessageUtils.broadcast("<red>🎖 <yellow>MILITARY DRAFT ACTIVE");
        MessageUtils.broadcast("<gray>All players receive combat gear and buffs!");
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            militaryDraftPlayers.add(player.getUniqueId());
            // Give basic combat gear
            if (player.getInventory().getHelmet() == null) {
                player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
            }
            if (player.getInventory().getChestplate() == null) {
                player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
            }
            if (player.getInventory().getLeggings() == null) {
                player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
            }
            if (player.getInventory().getBoots() == null) {
                player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
            }
            // Combat buffs
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 144000, 0)); // 2 hours
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 144000, 0));
        }
    }
    
    private void activateNationalProtection() {
        MessageUtils.broadcast("<green>🛡 <aqua>NATIONAL PROTECTION ACTIVE");
        MessageUtils.broadcast("<gray>All players receive defensive buffs!");
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 144000, 1)); // 2 hours
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 144000, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 144000, 0));
        }
    }
    
    private void activateArmoryDiscount() {
        shopDiscounts.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            shopDiscounts.put(player.getUniqueId(), 0.5); // 50% discount
        }
        MessageUtils.broadcast("<gold>🗡 <yellow>ARMORY DISCOUNT ACTIVE");
        MessageUtils.broadcast("<gray>50% off all weapon and armor purchases!");
    }
    
    private void activateBorderPatrol() {
        MessageUtils.broadcast("<blue>🚨 <aqua>BORDER PATROL ACTIVE");
        MessageUtils.broadcast("<gray>Increased mob spawns at world borders, bonus rewards for kills!");
        // This would integrate with mob spawn events
    }
    
    // ==================== TREASURY MINISTRY DECISIONS ====================
    
    private void activateTaxHoliday() {
        taxHolidayPlayers.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            taxHolidayPlayers.add(player.getUniqueId());
        }
        MessageUtils.broadcast("<green>💰 <yellow>TAX HOLIDAY DECLARED");
        MessageUtils.broadcast("<gray>No transaction taxes for the duration!");
    }
    
    private void distributeEconomicStimulus() {
        double amount = plugin.getConfig().getDouble("cabinet.decisions.treasury.stimulus-amount", 25000);
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getVaultHook().deposit(player.getUniqueId(), amount);
            MessageUtils.send(player, "<green>You received $" + MessageUtils.formatNumber(amount) + " economic stimulus!");
            count++;
        }
        MessageUtils.broadcast("<green>💵 <yellow>ECONOMIC STIMULUS DISTRIBUTED");
        MessageUtils.broadcast("<gray>" + count + " players received $" + MessageUtils.formatNumber(amount) + " each!");
    }
    
    private void activateAuctionBoost() {
        MessageUtils.broadcast("<gold>🔨 <yellow>AUCTION BOOST ACTIVE");
        MessageUtils.broadcast("<gray>All auction house fees reduced by 75%!");
    }
    
    private void distributeTreasuryBonus() {
        Treasury treasury = plugin.getDataManager().getTreasury();
        double bonus = treasury.getBalance() * 0.05; // 5% of treasury
        double perPlayer = bonus / Math.max(1, Bukkit.getOnlinePlayers().size());
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getVaultHook().deposit(player.getUniqueId(), perPlayer);
            MessageUtils.send(player, "<green>You received $" + MessageUtils.formatNumber(perPlayer) + " treasury bonus!");
        }
        
        plugin.getTreasuryManager().withdraw(Treasury.TransactionType.MISC_EXPENSE, bonus, "Treasury Bonus Distribution", plugin.getDataManager().getGovernment().getPresidentUUID());
        MessageUtils.broadcast("<gold>🏦 <yellow>TREASURY BONUS DISTRIBUTED");
        MessageUtils.broadcast("<gray>5% of treasury distributed to all online players!");
    }
    
    private void triggerMarketCrash() {
        MessageUtils.broadcast("<red>📉 <dark_red>MARKET CRASH SIMULATION");
        MessageUtils.broadcast("<gray>All shop prices reduced by 80% for limited time!");
        MessageUtils.broadcast("<yellow>Quick! Buy everything you can!");
    }
    
    // ==================== HEALTH MINISTRY DECISIONS ====================
    
    private void doQuarantineProtocol(Nation nation, CabinetDecision.DecisionType type) {
        if (nation == null || nation.getRepublicGovernment() == null) return;
        nation.getRepublicGovernment().setQuarantineUntil(System.currentTimeMillis() + getDecisionDuration(type));
        broadcastToNation(nation, "<aqua>💉 Quarantine Protocol active!</aqua> <gray>Non-members cannot enter territory for 10 minutes.</gray>");
    }
    
    private void doFieldMedicine(Nation nation, CabinetDecision.DecisionType type) {
        if (nation == null || nation.getRepublicGovernment() == null) return;
        Government gov = nation.getRepublicGovernment();
        if (gov.isFieldMedicineOnCooldown()) return;
        
        long cooldownMs = 2L * 60 * 60 * 1000; // 2 hours
        gov.setFieldMedicineCooldownUntil(System.currentTimeMillis() + cooldownMs);
        
        int durationTicks = (int) (getDecisionDuration(type) / 50);
        int affected = 0;
        for (UUID memberUUID : nation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(memberUUID);
            if (p == null || !p.isOnline()) continue;
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, durationTicks, 1, false, true, true));
            MessageUtils.send(p, "<green>💉 Field Medicine: <gray>Regeneration II for 5 minutes.</gray>");
            affected++;
        }
        broadcastToNation(nation, "<green>💉 Field Medicine!</green> <gray>" + affected + " online members receive Regeneration II for 5 minutes.</gray>");
    }
    
    private void doVaccinationDrive(Nation nation, CabinetDecision.DecisionType type) {
        if (nation == null || nation.getRepublicGovernment() == null) return;
        nation.getRepublicGovernment().setVaccinationUntil(System.currentTimeMillis() + getDecisionDuration(type));
        broadcastToNation(nation, "<green>💉 Vaccination Drive active!</green> <gray>Members immune to poison & wither for 1 hour.</gray>");
    }
    
    private void doEmergencyRations(Nation nation) {
        if (nation == null) return;
        int distributed = 0;
        for (UUID memberUUID : nation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(memberUUID);
            if (p == null || !p.isOnline()) continue;
            if (p.getFoodLevel() >= 10) continue;
            ItemStack bread = new ItemStack(Material.BREAD, 8);
            var leftover = p.getInventory().addItem(bread);
            for (var dropItem : leftover.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), dropItem);
            }
            MessageUtils.send(p, "<gold>🍞 Emergency Rations: <gray>8 bread for you.</gray>");
            distributed++;
        }
        broadcastToNation(nation, "<gold>🍞 Emergency Rations distributed to " + distributed + " members with low hunger.");
    }
    
    private void doPlague(Nation nation, CabinetDecision.DecisionType type) {
        if (nation == null || nation.getRepublicGovernment() == null) return;
        nation.getRepublicGovernment().setPlagueUntil(System.currentTimeMillis() + getDecisionDuration(type));
        broadcastToNation(nation, "<dark_red>☠ Plague active!</dark_red> <gray>Enemies entering territory will receive Weakness II + Hunger for 30 seconds. Active for 10 minutes.</gray>");
    }
    
    private void broadcastToNation(Nation nation, String message) {
        if (nation == null) return;
        for (UUID uuid : nation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) MessageUtils.send(p, message);
        }
    }
    

    
    // ==================== UTILITY METHODS ====================
    
    public boolean isDecisionActive(CabinetDecision.DecisionType type) {
        Long endTime = decisionEndTimes.get(type);
        return endTime != null && System.currentTimeMillis() < endTime;
    }
    
    public boolean isDecisionActive(Nation nation, CabinetDecision.DecisionType type) {
        if (nation == null) return isDecisionActive(type);
        return nation.getActiveDecisions().stream()
                .anyMatch(d -> d.getType() == type && d.isActive() && !d.isExpired());
    }
    
    public long getDecisionRemainingTime(CabinetDecision.DecisionType type) {
        Long endTime = decisionEndTimes.get(type);
        if (endTime == null) return 0;
        return Math.max(0, endTime - System.currentTimeMillis());
    }
    
    public void checkExpiredDecisions() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<CabinetDecision.DecisionType, Long>> it = decisionEndTimes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<CabinetDecision.DecisionType, Long> entry = it.next();
            if (now >= entry.getValue()) {
                expireDecision(entry.getKey());
                it.remove();
                activeDecisions.remove(entry.getKey());
            }
        }
    }
    
    private void expireDecision(CabinetDecision.DecisionType type) {
        MessageUtils.broadcast("<gray>Cabinet decision <white>" + getDecisionDisplayName(type) + " <gray>has expired.");
        
        switch (type) {
            case TAX_HOLIDAY -> taxHolidayPlayers.clear();
            case ARMORY_DISCOUNT -> shopDiscounts.clear();
        }
    }
    
    public Government.CabinetPosition getRequiredPosition(CabinetDecision.DecisionType type) {
        return switch (type) {
            case DECLARE_WAR, MILITARY_DRAFT, DEFENSE_PROTOCOL, ARMORY_DISCOUNT, BORDER_PATROL -> 
                Government.CabinetPosition.DEFENSE;
            case TAX_HOLIDAY, ECONOMIC_STIMULUS, AUCTION_BOOST, TREASURY_BONUS, MARKET_CRASH -> 
                Government.CabinetPosition.TREASURY;
            case QUARANTINE_PROTOCOL, FIELD_MEDICINE, VACCINATION_DRIVE, EMERGENCY_RATIONS, PLAGUE ->
                Government.CabinetPosition.HEALTH;
        };
    }
    
    public int getDecisionCost(CabinetDecision.DecisionType type) {
        String path = "cabinet.decisions." + getRequiredPosition(type).name().toLowerCase() + ".cost";
        return plugin.getConfig().getInt(path, 100000);
    }
    
    public long getDecisionDuration(CabinetDecision.DecisionType type) {
        if (type.getDurationMillis() > 0) return type.getDurationMillis();
        String path = "cabinet.decisions." + getRequiredPosition(type).name().toLowerCase() + ".duration-hours";
        return plugin.getConfig().getLong(path, 24) * 3600000L;
    }
    
    public String getDecisionDisplayName(CabinetDecision.DecisionType type) {
        return switch (type) {
            case DECLARE_WAR -> "War Games Event";
            case MILITARY_DRAFT -> "Military Draft";
            case DEFENSE_PROTOCOL -> "National Protection";
            case ARMORY_DISCOUNT -> "Armory Discount";
            case BORDER_PATROL -> "Border Patrol";
            case TAX_HOLIDAY -> "Tax Holiday";
            case ECONOMIC_STIMULUS -> "Economic Stimulus";
            case AUCTION_BOOST -> "Auction Boost";
            case TREASURY_BONUS -> "Treasury Bonus";
            case MARKET_CRASH -> "Market Crash";
            case QUARANTINE_PROTOCOL -> "Quarantine Protocol";
            case FIELD_MEDICINE -> "Field Medicine";
            case VACCINATION_DRIVE -> "Vaccination Drive";
            case EMERGENCY_RATIONS -> "Emergency Rations";
            case PLAGUE -> "Plague";
        };
    }
    
    public List<CabinetDecision.DecisionType> getDecisionsForPosition(Government.CabinetPosition position) {
        List<CabinetDecision.DecisionType> decisions = new ArrayList<>();
        for (CabinetDecision.DecisionType type : CabinetDecision.DecisionType.values()) {
            if (getRequiredPosition(type) == position) {
                decisions.add(type);
            }
        }
        return decisions;
    }
    
    // Getters for active effects
    public boolean isTaxHolidayActive() { return !taxHolidayPlayers.isEmpty(); }
    public double getGlobalDropMultiplier() { return globalDropMultiplier; }
    public double getGlobalXpMultiplier() { return globalXpMultiplier; }
    public double getShopDiscount(UUID playerId) { return shopDiscounts.getOrDefault(playerId, 1.0); }
    
    public void applyEffectsToPlayer(Player player) {
        // Apply any active decision effects to newly joined players
        if (isDecisionActive(CabinetDecision.DecisionType.MILITARY_DRAFT)) {
            militaryDraftPlayers.add(player.getUniqueId());
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 144000, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 144000, 0));
        }
        if (isDecisionActive(CabinetDecision.DecisionType.DEFENSE_PROTOCOL)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 144000, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 144000, 0));
        }

        if (isDecisionActive(CabinetDecision.DecisionType.TAX_HOLIDAY)) {
            taxHolidayPlayers.add(player.getUniqueId());
        }
        if (isDecisionActive(CabinetDecision.DecisionType.ARMORY_DISCOUNT)) {
            shopDiscounts.put(player.getUniqueId(), 0.5);
        }
    }

    // Additional methods for NationCommand
    public List<CabinetDecision> getAllActiveDecisions() {
        return plugin.getDataManager().getActiveDecisions();
    }

    public boolean isDecisionOnCooldown(CabinetDecision.DecisionType type) {
        return getRemainingCooldown(null, type) > 0; // Check if any cooldown exists
    }

    public boolean issueDecision(Player player, CabinetDecision.DecisionType type) {
        return executeDecision(player.getUniqueId(), type);
    }

    public List<CabinetDecision> getActiveDecisionsByPosition(CabinetDecision.CabinetPosition position) {
        return plugin.getDataManager().getActiveDecisions().stream()
            .filter(decision -> decision.getMinisterPosition() == position)
            .toList();
    }

    // ==========================================================
    // Context-aware (per-nation) overloads — Phase 2B
    // ==========================================================

    public List<CabinetDecision> getAllActiveDecisions(Nation nation) {
        return nation != null ? nation.getActiveDecisions() : getAllActiveDecisions();
    }

    public List<CabinetDecision> getActiveDecisionsByPosition(Nation nation,
            CabinetDecision.CabinetPosition position) {
        return getAllActiveDecisions(nation).stream()
                .filter(decision -> decision.getMinisterPosition() == position)
                .toList();
    }

    /**
     * Cek apakah pemain (sebagai menteri di nation) bisa eksekusi decision.
     * Pengecekan: posisi menteri sesuai, cooldown belum berakhir, dan kas
     * nation cukup. Cooldown disimpan di map global per-pemain (cross-nation
     * cooldown — pemain yang resign dari satu nation lalu diangkat di nation
     * lain tetap kena cooldown lama; ini sengaja untuk mencegah abuse).
     */
    public boolean canExecuteDecision(Nation nation, UUID ministerId, CabinetDecision.DecisionType type) {
        if (nation == null || nation.getType() != GovernmentType.REPUBLIC) return false;
        Government gov = nation.getRepublicGovernment();
        if (gov == null) return false;

        Government.CabinetPosition required = getRequiredPosition(type);
        if (required == null) return false;
        UUID currentMinister = gov.getCabinetMember(required);
        if (currentMinister == null || !currentMinister.equals(ministerId)) return false;

        long cooldownMs = plugin.getConfig().getLong("cabinet.decision-cooldown-hours", 48) * 3600000L;
        Map<CabinetDecision.DecisionType, Long> playerCooldowns =
                decisionCooldowns.computeIfAbsent(ministerId, k -> new HashMap<>());
        Long lastUse = playerCooldowns.get(type);
        if (lastUse != null && System.currentTimeMillis() - lastUse < cooldownMs) return false;

        int cost = getDecisionCost(type);
        return plugin.getTreasuryManager().canAfford(nation, cost);
    }

    public boolean executeDecision(Nation nation, UUID ministerId, CabinetDecision.DecisionType type) {
        if (!canExecuteDecision(nation, ministerId, type)) return false;

        int cost = getDecisionCost(type);
        plugin.getTreasuryManager().withdraw(nation, Treasury.TransactionType.EXECUTIVE_ORDER,
                (double) cost, "Cabinet Decision: " + type.name(), ministerId);

        decisionCooldowns.computeIfAbsent(ministerId, k -> new HashMap<>())
                .put(type, System.currentTimeMillis());

        Government.CabinetPosition position = getRequiredPosition(type);
        CabinetDecision.CabinetPosition cabPos = CabinetDecision.CabinetPosition.valueOf(position.name());
        CabinetDecision decision = new CabinetDecision(type, cabPos, ministerId);
        nation.getActiveDecisions().add(decision);

        // Catatan: activeDecisions/decisionEndTimes in-memory tetap dipakai untuk
        // tracking efek server-wide (akan jadi per-nation di Phase 2C bila
        // efek-spesifik nation diperlukan).
        activeDecisions.put(type, System.currentTimeMillis());
        decisionEndTimes.put(type, System.currentTimeMillis() + getDecisionDuration(type));

        applyDecisionEffect(nation, type, decision);

        Player minister = Bukkit.getPlayer(ministerId);
        String ministerName = minister != null ? minister.getName() : "Unknown";
        MessageUtils.broadcast("<gold>📜 <yellow>Cabinet Decision (" + nation.getName() +
                "): <white>" + getDecisionDisplayName(type));
        MessageUtils.broadcast("<gray>Diumumkan oleh Menteri <white>" + ministerName);
        MessageUtils.broadcast("<gray>Durasi: <white>" + MessageUtils.formatTime(getDecisionDuration(type)));

        plugin.getDataManager().saveAll();
        return true;
    }

    public boolean issueDecision(Player player, Nation nation, CabinetDecision.DecisionType type) {
        if (nation == null) return issueDecision(player, type);
        return executeDecision(nation, player.getUniqueId(), type);
    }
}
