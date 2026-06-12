package id.nationcore.managers;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import id.nationcore.NationCore;
import id.nationcore.models.ArenaSession;
import id.nationcore.models.CabinetDecision;
import id.nationcore.models.Election;
import id.nationcore.models.ExecutiveOrder;
import id.nationcore.models.Government;
import id.nationcore.models.Nation;
import id.nationcore.models.PlayerData;
import id.nationcore.models.PresidentHistory;
import id.nationcore.models.RecallPetition;
import id.nationcore.models.TaxRecord;
import id.nationcore.models.Treasury;

public class DataManager {

    private final NationCore plugin;
    private final Gson gson;
    private final File dataFolder;

    private Government government;
    private Election election;
    private Treasury treasury;
    private PresidentHistory presidentHistory;
    private Map<UUID, PlayerData> playerDataMap;
    private List<ExecutiveOrder> activeOrders;
    private List<CabinetDecision> activeDecisions;
    private ArenaSession arenaSession;
    private RecallPetition recallPetition;
    private TaxRecord taxRecord;
    private long lastExecutiveOrderTime;
    private int gamesThisTerm;

    public DataManager(NationCore plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        this.government = new Government();
        this.election = new Election();
        this.treasury = new Treasury();
        this.presidentHistory = new PresidentHistory();
        this.playerDataMap = new HashMap<>();
        this.activeOrders = new ArrayList<>();
        this.activeDecisions = new ArrayList<>();
        this.taxRecord = new TaxRecord();
    }

    public void loadAll() {
        loadGovernment();
        loadElection();
        loadTreasury();
        loadPresidentHistory();
        loadPlayerData();
        loadActiveOrders();
        loadActiveDecisions();
        loadArenaSession();
        loadRecallPetition();
        loadTaxRecord();
        loadMiscData();
        loadNations();
    }

    public void saveAll() {
        saveGovernment();
        saveElection();
        saveTreasury();
        savePresidentHistory();
        savePlayerData();
        saveActiveOrders();
        saveActiveDecisions();
        saveArenaSession();
        saveRecallPetition();
        saveTaxRecord();
        saveMiscData();
        saveNations();
    }

    // Government
    private void loadGovernment() {
        File file = new File(dataFolder, "government.json");
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                government = gson.fromJson(reader, Government.class);
                if (government == null)
                    government = new Government();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load government data: " + e.getMessage());
                government = new Government();
            }
        }
    }

    public void saveGovernment() {
        File file = new File(dataFolder, "government.json");
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(government, writer);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save government data: " + e.getMessage());
        }
    }

    // Election
    private void loadElection() {
        File file = new File(dataFolder, "election.json");
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                election = gson.fromJson(reader, Election.class);
                if (election == null)
                    election = new Election();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load election data: " + e.getMessage());
                election = new Election();
            }
        }
    }

    public void saveElection() {
        File file = new File(dataFolder, "election.json");
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(election, writer);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save election data: " + e.getMessage());
        }
    }

    // Treasury
    private void loadTreasury() {
        File file = new File(dataFolder, "treasury.json");
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                treasury = gson.fromJson(reader, Treasury.class);
                if (treasury == null)
                    treasury = new Treasury();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load treasury data: " + e.getMessage());
                treasury = new Treasury();
            }
        }
    }

    private void saveTreasury() {
        File file = new File(dataFolder, "treasury.json");
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(treasury, writer);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save treasury data: " + e.getMessage());
        }
    }

    // President History
    private void loadPresidentHistory() {
        File file = new File(dataFolder, "history.json");
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                presidentHistory = gson.fromJson(reader, PresidentHistory.class);
                if (presidentHistory == null)
                    presidentHistory = new PresidentHistory();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load president history: " + e.getMessage());
                presidentHistory = new PresidentHistory();
            }
        }
    }

    private void savePresidentHistory() {
        File file = new File(dataFolder, "history.json");
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(presidentHistory, writer);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save president history: " + e.getMessage());
        }
    }

    // Player Data
    private void loadPlayerData() {
        File file = new File(dataFolder, "players.json");
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                PlayerData[] players = gson.fromJson(reader, PlayerData[].class);
                if (players != null) {
                    for (PlayerData data : players) {
                        playerDataMap.put(data.getUuid(), data);
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load player data: " + e.getMessage());
            }
        }
    }

    // Active Orders
    private void loadActiveOrders() {
        File file = new File(dataFolder, "orders.json");
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                ExecutiveOrder[] orders = gson.fromJson(reader, ExecutiveOrder[].class);
                if (orders != null) {
                    activeOrders = new ArrayList<>(Arrays.asList(orders));
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load executive orders: " + e.getMessage());
            }
        }
    }

    private void saveActiveOrders() {
        File file = new File(dataFolder, "orders.json");
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(activeOrders.toArray(new ExecutiveOrder[0]), writer);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save executive orders: " + e.getMessage());
        }
    }

    // Active Decisions
    private void loadActiveDecisions() {
        File file = new File(dataFolder, "decisions.json");
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                CabinetDecision[] decisions = gson.fromJson(reader, CabinetDecision[].class);
                if (decisions != null) {
                    activeDecisions = new ArrayList<>(Arrays.asList(decisions));
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load cabinet decisions: " + e.getMessage());
            }
        }
    }

    private void saveActiveDecisions() {
        File file = new File(dataFolder, "decisions.json");
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(activeDecisions.toArray(new CabinetDecision[0]), writer);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save cabinet decisions: " + e.getMessage());
        }
    }

    // Arena Session
    private void loadArenaSession() {
        File file = new File(dataFolder, "arena.json");
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                arenaSession = gson.fromJson(reader, ArenaSession.class);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load arena session: " + e.getMessage());
            }
        }
    }

    private void saveArenaSession() {
        if (arenaSession == null)
            return;
        File file = new File(dataFolder, "arena.json");
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(arenaSession, writer);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save arena session: " + e.getMessage());
        }
    }

    // Recall Petition
    private void loadRecallPetition() {
        File file = new File(dataFolder, "recall.json");
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                recallPetition = gson.fromJson(reader, RecallPetition.class);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load recall petition: " + e.getMessage());
            }
        }
    }

    private void saveRecallPetition() {
        File file = new File(dataFolder, "recall.json");
        if (recallPetition == null) {
            if (file.exists())
                file.delete();
            return;
        }
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(recallPetition, writer);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save recall petition: " + e.getMessage());
        }
    }

    // Tax Record
    private void loadTaxRecord() {
        File file = new File(dataFolder, "tax_record.json");
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                taxRecord = gson.fromJson(reader, TaxRecord.class);
                if (taxRecord == null)
                    taxRecord = new TaxRecord();
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                plugin.getLogger().warning("Failed to load tax record: " + e.getMessage());
                taxRecord = new TaxRecord();
            }
        }
        // Files written by the pre-invoice tax system lack the new collections.
        taxRecord.ensureInitialized();
    }

    private void saveTaxRecord() {
        File file = new File(dataFolder, "tax_record.json");
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(taxRecord, writer);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save tax record: " + e.getMessage());
        }
    }

    // Misc Data
    private void loadMiscData() {
        File file = new File(dataFolder, "misc.json");
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                MiscData data = gson.fromJson(reader, MiscData.class);
                if (data != null) {
                    lastExecutiveOrderTime = data.lastExecutiveOrderTime;
                    gamesThisTerm = data.gamesThisTerm;
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load misc data: " + e.getMessage());
            }
        }
    }

    private void saveMiscData() {
        File file = new File(dataFolder, "misc.json");
        MiscData data = new MiscData();
        data.lastExecutiveOrderTime = lastExecutiveOrderTime;
        data.gamesThisTerm = gamesThisTerm;
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save misc data: " + e.getMessage());
        }
    }

    private static class MiscData {
        long lastExecutiveOrderTime;
        int gamesThisTerm;
    }

    // === Nations (multi-nation system, sejak v1.5) ===

    /**
     * Memuat seluruh nation dari disk dan mendelegasikan ke NationManager.
     * Aman dipanggil meskipun NationManager belum siap — daftar akan tetap
     * tersimpan di file dan di-load ulang saat NationManager dibuat.
     */
    public void loadNations() {
        File file = new File(dataFolder, "nations.json");
        if (!file.exists()) {
            // Belum ada nation; tidak perlu load apa pun.
            if (plugin.getNationManager() != null) {
                plugin.getNationManager().loadNations(new ArrayList<>());
            }
            return;
        }
        try (Reader reader = new FileReader(file)) {
            Nation[] arr = gson.fromJson(reader, Nation[].class);
            List<Nation> list = arr != null ? new ArrayList<>(Arrays.asList(arr)) : new ArrayList<>();
            if (plugin.getNationManager() != null) {
                plugin.getNationManager().loadNations(list);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load nations data: " + e.getMessage());
        }
    }

    public void saveNations() {
        if (plugin.getNationManager() == null) return;
        File file = new File(dataFolder, "nations.json");
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(plugin.getNationManager().getAllNations().toArray(new Nation[0]), writer);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save nations data: " + e.getMessage());
        }
    }

    /**
     * Versi public dari savePlayerData — dipanggil oleh NationManager
     * setelah operasi membership selesai.
     */
    public void savePlayerData() {
        File file = new File(dataFolder, "players.json");
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(playerDataMap.values().toArray(new PlayerData[0]), writer);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save player data: " + e.getMessage());
        }
    }

    // Getters
    public Government getGovernment() {
        return government;
    }

    public Election getElection() {
        return election;
    }

    public Treasury getTreasury() {
        return treasury;
    }

    public PresidentHistory getPresidentHistory() {
        return presidentHistory;
    }

    public List<ExecutiveOrder> getActiveOrders() {
        return activeOrders;
    }

    public List<CabinetDecision> getActiveDecisions() {
        return activeDecisions;
    }

    public ArenaSession getArenaSession() {
        return arenaSession;
    }

    public void setArenaSession(ArenaSession arenaSession) {
        this.arenaSession = arenaSession;
    }

    public RecallPetition getRecallPetition() {
        return recallPetition;
    }

    public TaxRecord getTaxRecord() {
        return taxRecord;
    }

    public void setRecallPetition(RecallPetition recallPetition) {
        this.recallPetition = recallPetition;
    }

    public long getLastExecutiveOrderTime() {
        return lastExecutiveOrderTime;
    }

    public void setLastExecutiveOrderTime(long time) {
        this.lastExecutiveOrderTime = time;
    }

    public int getGamesThisTerm() {
        return gamesThisTerm;
    }

    public void setGamesThisTerm(int games) {
        this.gamesThisTerm = games;
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
    }

    public PlayerData getOrCreatePlayerData(UUID uuid, String name) {
        return playerDataMap.computeIfAbsent(uuid, k -> new PlayerData(uuid, name));
    }

    public Collection<PlayerData> getAllPlayerData() {
        return playerDataMap.values();
    }

    public int getActivePlayerCount() {
        long threshold = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000); // 7 days
        return (int) playerDataMap.values().stream()
                .filter(p -> p.getLastSeen() > threshold)
                .count();
    }

    public List<PresidentHistory.PresidentRecord> getAllPresidentHistory() {
        return presidentHistory.getRecords();
    }

    // Reset methods for NationCommand
    public void resetGovernment() {
        government = new Government();
        saveGovernment();
    }

    public void resetElection() {
        election = new Election();
        saveElection();
    }

    public void resetTreasury() {
        treasury = new Treasury();
        saveTreasury();
    }
}
