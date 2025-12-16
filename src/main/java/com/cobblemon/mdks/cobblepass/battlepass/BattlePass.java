package com.cobblemon.mdks.cobblepass.battlepass;

import com.cobblemon.mdks.cobblepass.CobblePass;
import com.cobblemon.mdks.cobblepass.database.DatabaseManager;
import com.cobblemon.mdks.cobblepass.util.Constants;
import com.cobblemon.mdks.cobblepass.util.Utils;
import com.cobblemon.mdks.cobblepass.config.TierConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BattlePass {
    private final Map<UUID, PlayerBattlePass> playerPasses = new HashMap<>();
    private TierConfig tierConfig;
    private final DatabaseManager databaseManager;

    public BattlePass() {
        this.tierConfig = new TierConfig();
        this.databaseManager = new DatabaseManager();
    }

    public void init() {
        // Initialize database connection
        databaseManager.initialize();

        // Migrate legacy JSON files if they exist
        migrateLegacyData();

        // Load all player data from database
        List<PlayerBattlePass> players = databaseManager.loadAllPlayers();
        for (PlayerBattlePass pass : players) {
            playerPasses.put(pass.getPlayerId(), pass);
        }
        CobblePass.LOGGER.info("Loaded " + players.size() + " player battle passes from database");
    }

    public void loadPlayerPass(String uuid) {
        UUID playerId = UUID.fromString(uuid);

        // Check if already loaded in memory
        if (playerPasses.containsKey(playerId)) {
            return;
        }

        // Try to load from database
        PlayerBattlePass pass = databaseManager.loadPlayer(playerId);

        if (pass == null) {
            // Create new player pass if not in database
            pass = new PlayerBattlePass(playerId);
            playerPasses.put(playerId, pass);
            // Save the new pass immediately
            savePlayerPass(uuid);
            return;
        }

        playerPasses.put(playerId, pass);
        CobblePass.LOGGER.debug("Loaded battle pass for " + uuid + " with level " + pass.getLevel() + " and XP " + pass.getXP());
    }

    public void savePlayerPass(String uuid) {
        UUID playerId = UUID.fromString(uuid);
        PlayerBattlePass pass = playerPasses.get(playerId);
        if (pass != null) {
            databaseManager.savePlayer(pass);
            CobblePass.LOGGER.debug("Saved battle pass for " + uuid + " with level " + pass.getLevel() + " and XP " + pass.getXP());
        }
    }

    public void reload() {
        this.tierConfig = new TierConfig();
        init();
    }

    public void reloadTiers() {
        // Only reload tier configuration without touching player data
        this.tierConfig = new TierConfig();
    }

    public void save() {
        for (PlayerBattlePass pass : playerPasses.values()) {
            databaseManager.savePlayer(pass);
        }
        tierConfig.save();
        CobblePass.LOGGER.info("Saved " + playerPasses.size() + " player battle passes to database");
    }

    public PlayerBattlePass getPlayerPass(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (!playerPasses.containsKey(uuid)) {
            // Try to load from file first
            loadPlayerPass(uuid.toString());
        }
        // If loading failed or no file exists, create new pass
        return playerPasses.computeIfAbsent(uuid,
                id -> {
                    PlayerBattlePass newPass = new PlayerBattlePass(id);
                    // Save the new pass immediately
                    savePlayerPass(id.toString());
                    return newPass;
                });
    }

    public void addXP(ServerPlayer player, int amount) {
        // Check if season is active
        if (!CobblePass.config.isSeasonActive()) {
            return; // Don't award XP if no season is active
        }

        PlayerBattlePass pass = getPlayerPass(player);

        // Apply premium bonus if player has premium
        if (pass.isPremium()) {
            amount = (int)(amount * 1.1); // 10% bonus
        }

        int oldLevel = pass.getLevel();
        pass.addXP(amount);
        int newLevel = pass.getLevel();

        // Show action bar with XP gained
        player.sendSystemMessage(
            Component.literal(String.format("§a+%d XP", amount))
                .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GREEN)),
            true  // true = overlay (action bar)
        );

        // Check if player leveled up
        if (newLevel > oldLevel) {
            player.sendSystemMessage(
                Component.literal(String.format(Constants.MSG_LEVEL_UP, newLevel))
            );
        }

        // Save after XP change - use sync to ensure level progression is saved immediately
        savePlayerPass(player.getUUID().toString());
    }

    public boolean claimReward(ServerPlayer player, int level, boolean premium) {
        PlayerBattlePass pass = getPlayerPass(player);
        BattlePassTier tier = getTier(level);

        if (tier == null) {
            return false;
        }

        // Check if player has reached required level
        if (level > pass.getLevel()) {
            player.sendSystemMessage(Component.literal(String.format(
                Constants.MSG_LEVEL_NOT_REACHED,
                level
            )));
            return false;
        }

        // Check if already claimed
        if (premium && pass.hasClaimedPremiumReward(level)) {
            player.sendSystemMessage(Component.literal(String.format(
                Constants.MSG_ALREADY_CLAIMED_LEVEL,
                level
            )));
            return false;
        } else if (!premium && pass.hasClaimedFreeReward(level)) {
            player.sendSystemMessage(Component.literal(String.format(
                Constants.MSG_ALREADY_CLAIMED_LEVEL,
                level
            )));
            return false;
        }

        // Mark as claimed first
        if (premium) {
            pass.claimPremiumReward(level);
        } else {
            pass.claimFreeReward(level);
        }

        // Save claim state immediately
        savePlayerPass(player.getUUID().toString());

        // Grant reward after saving claim state
        if (premium) {
            tier.grantPremiumReward(player);
        } else {
            tier.grantFreeReward(player);
        }
        return true;
    }

    public BattlePassTier getTier(int level) {
        return tierConfig.getTier(level);
    }

    public Map<Integer, BattlePassTier> getTiers() {
        return tierConfig.getAllTiers();
    }

    public void resetAllPlayerData() {
        CobblePass.LOGGER.info("Resetting all player battle pass data for new season...");

        // Clear in-memory player data
        playerPasses.clear();

        // Delete all player data from database
        databaseManager.deleteAllPlayers();

        CobblePass.LOGGER.info("All player battle pass data has been reset");
    }

    /**
     * Migrate legacy JSON files to database
     */
    private void migrateLegacyData() {
        File playerDir = new File(Constants.PLAYER_DATA_DIR);
        if (!playerDir.exists() || !playerDir.isDirectory()) {
            return;
        }

        File[] files = playerDir.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        int migrated = 0;
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".json")) {
                String uuid = file.getName().replace(".json", "");
                String content = Utils.readFileSync(Constants.PLAYER_DATA_DIR, file.getName());

                if (content != null && !content.isEmpty()) {
                    try {
                        UUID playerId = UUID.fromString(uuid);
                        JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                        PlayerBattlePass pass = new PlayerBattlePass(playerId);
                        pass.fromJson(json);

                        // Save to database
                        databaseManager.savePlayer(pass);
                        migrated++;

                        // Delete the JSON file after successful migration
                        if (file.delete()) {
                            CobblePass.LOGGER.debug("Migrated and deleted legacy file: " + file.getName());
                        }
                    } catch (Exception e) {
                        CobblePass.LOGGER.error("Failed to migrate legacy data for " + uuid, e);
                    }
                }
            }
        }

        if (migrated > 0) {
            CobblePass.LOGGER.info("Migrated " + migrated + " player records from JSON files to database");
        }
    }

    /**
     * Close database connection
     */
    public void close() {
        databaseManager.close();
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
