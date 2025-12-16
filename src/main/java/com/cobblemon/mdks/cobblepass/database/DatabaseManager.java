package com.cobblemon.mdks.cobblepass.database;

import com.cobblemon.mdks.cobblepass.CobblePass;
import com.cobblemon.mdks.cobblepass.battlepass.PlayerBattlePass;
import com.cobblemon.mdks.cobblepass.util.Constants;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private static final String DB_FILE = Constants.CONFIG_DIR + "/cobblepass.db";
    private static final Gson GSON = new Gson();
    private Connection connection;

    public void initialize() {
        try {
            // Ensure config directory exists
            File configDir = new File(Constants.CONFIG_DIR);
            if (!configDir.exists()) {
                configDir.mkdirs();
            }

            // Connect to SQLite database (creates file if not exists)
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
            CobblePass.LOGGER.info("Connected to SQLite database: " + DB_FILE);

            // Create tables if they don't exist
            createTables();

        } catch (SQLException e) {
            CobblePass.LOGGER.error("Failed to initialize database", e);
        }
    }

    private void createTables() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS player_data (
                uuid TEXT PRIMARY KEY,
                version TEXT DEFAULT '1.0',
                level INTEGER DEFAULT 1,
                xp INTEGER DEFAULT 0,
                is_premium INTEGER DEFAULT 0,
                claimed_free_rewards TEXT DEFAULT '[]',
                claimed_premium_rewards TEXT DEFAULT '[]',
                caught_species TEXT DEFAULT '[]'
            )
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            CobblePass.LOGGER.debug("Database tables initialized");
        }
    }

    public PlayerBattlePass loadPlayer(UUID playerId) {
        String sql = "SELECT * FROM player_data WHERE uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerId.toString());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return resultSetToPlayerBattlePass(playerId, rs);
            }
        } catch (SQLException e) {
            CobblePass.LOGGER.error("Failed to load player data for " + playerId, e);
        }

        return null; // Player not found
    }

    public List<PlayerBattlePass> loadAllPlayers() {
        List<PlayerBattlePass> players = new ArrayList<>();
        String sql = "SELECT * FROM player_data";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                UUID playerId = UUID.fromString(rs.getString("uuid"));
                PlayerBattlePass pass = resultSetToPlayerBattlePass(playerId, rs);
                players.add(pass);
            }

        } catch (SQLException e) {
            CobblePass.LOGGER.error("Failed to load all player data", e);
        }

        return players;
    }

    private PlayerBattlePass resultSetToPlayerBattlePass(UUID playerId, ResultSet rs) throws SQLException {
        PlayerBattlePass pass = new PlayerBattlePass(playerId);

        // Parse JSON arrays
        Type intSetType = new TypeToken<Set<Integer>>(){}.getType();
        Type stringSetType = new TypeToken<Set<String>>(){}.getType();

        Set<Integer> freeRewards = GSON.fromJson(rs.getString("claimed_free_rewards"), intSetType);
        Set<Integer> premiumRewards = GSON.fromJson(rs.getString("claimed_premium_rewards"), intSetType);
        Set<String> species = GSON.fromJson(rs.getString("caught_species"), stringSetType);

        // Use reflection or create a fromDatabase method to set private fields
        pass.loadFromDatabase(
            rs.getString("version"),
            rs.getInt("level"),
            rs.getInt("xp"),
            rs.getInt("is_premium") == 1,
            freeRewards != null ? freeRewards : new HashSet<>(),
            premiumRewards != null ? premiumRewards : new HashSet<>(),
            species != null ? species : new HashSet<>()
        );

        return pass;
    }

    public void savePlayer(PlayerBattlePass pass) {
        String sql = """
            INSERT OR REPLACE INTO player_data
            (uuid, version, level, xp, is_premium, claimed_free_rewards, claimed_premium_rewards, caught_species)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, pass.getPlayerId().toString());
            pstmt.setString(2, pass.getVersion());
            pstmt.setInt(3, pass.getLevel());
            pstmt.setInt(4, pass.getXP());
            pstmt.setInt(5, pass.isPremium() ? 1 : 0);
            pstmt.setString(6, GSON.toJson(pass.getClaimedFreeRewards()));
            pstmt.setString(7, GSON.toJson(pass.getClaimedPremiumRewards()));
            pstmt.setString(8, GSON.toJson(pass.getCaughtSpecies()));

            pstmt.executeUpdate();
            CobblePass.LOGGER.debug("Saved player data for " + pass.getPlayerId());

        } catch (SQLException e) {
            CobblePass.LOGGER.error("Failed to save player data for " + pass.getPlayerId(), e);
        }
    }

    public void deleteAllPlayers() {
        String sql = "DELETE FROM player_data";

        try (Statement stmt = connection.createStatement()) {
            int deleted = stmt.executeUpdate(sql);
            CobblePass.LOGGER.info("Deleted " + deleted + " player records from database");
        } catch (SQLException e) {
            CobblePass.LOGGER.error("Failed to delete all player data", e);
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
                CobblePass.LOGGER.info("Database connection closed");
            } catch (SQLException e) {
                CobblePass.LOGGER.error("Failed to close database connection", e);
            }
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
