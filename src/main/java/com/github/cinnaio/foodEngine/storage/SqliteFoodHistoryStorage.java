package com.github.cinnaio.foodEngine.storage;

import com.github.cinnaio.foodEngine.FoodEngine;
import com.github.cinnaio.foodEngine.manager.FoodHistoryManager;
import org.bukkit.Material;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public class SqliteFoodHistoryStorage implements FoodHistoryStorage {

    private final FoodEngine plugin;
    private final Connection connection;

    public SqliteFoodHistoryStorage(FoodEngine plugin, File file) {
        this.plugin = plugin;
        try {
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:" + file.getAbsolutePath();
            this.connection = DriverManager.getConnection(url);

            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL;");
                st.execute("PRAGMA synchronous=NORMAL;");
                st.execute("PRAGMA busy_timeout=3000;");
            }

            try (Statement st = connection.createStatement()) {
                st.execute("""
                        CREATE TABLE IF NOT EXISTS food_history (
                          id INTEGER PRIMARY KEY AUTOINCREMENT,
                          player_uuid TEXT NOT NULL,
                          ts BIGINT NOT NULL,
                          food_engine_id TEXT,
                          material TEXT NOT NULL
                        );
                        """);
                st.execute("CREATE INDEX IF NOT EXISTS idx_food_history_player_ts ON food_history(player_uuid, ts);");
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to initialize SQLite food history storage", ex);
        }
    }

    @Override
    public synchronized void record(UUID playerId, FoodHistoryManager.FoodHistoryEntry entry, int maxEntries) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO food_history(player_uuid, ts, food_engine_id, material) VALUES(?, ?, ?, ?)")) {
            ps.setString(1, playerId.toString());
            ps.setLong(2, entry.timestampMillis());
            ps.setString(3, entry.foodEngineId());
            ps.setString(4, entry.material().name());
            ps.executeUpdate();
        } catch (Exception ex) {
            if (plugin.isDebug()) {
                plugin.getLogger().warning("SQLite food_history insert failed: " + ex.getMessage());
            }
            return;
        }

        // prune to last N
        try (PreparedStatement ps = connection.prepareStatement(
                """
                        DELETE FROM food_history
                        WHERE player_uuid = ?
                          AND id NOT IN (
                            SELECT id FROM food_history
                            WHERE player_uuid = ?
                            ORDER BY ts DESC, id DESC
                            LIMIT ?
                          );
                        """)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, playerId.toString());
            ps.setInt(3, Math.max(1, maxEntries));
            ps.executeUpdate();
        } catch (Exception ex) {
            if (plugin.isDebug()) {
                plugin.getLogger().warning("SQLite food_history prune failed: " + ex.getMessage());
            }
        }
    }

    @Override
    public synchronized Deque<FoodHistoryManager.FoodHistoryEntry> loadRecent(UUID playerId, int maxEntries) {
        Deque<FoodHistoryManager.FoodHistoryEntry> result = new ArrayDeque<>();
        try (PreparedStatement ps = connection.prepareStatement(
                """
                        SELECT ts, food_engine_id, material
                        FROM food_history
                        WHERE player_uuid = ?
                        ORDER BY ts ASC, id ASC
                        LIMIT ?;
                        """)) {
            ps.setString(1, playerId.toString());
            ps.setInt(2, Math.max(1, maxEntries));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long ts = rs.getLong(1);
                    String foodId = rs.getString(2);
                    String mat = rs.getString(3);
                    Material material = Material.matchMaterial(mat);
                    if (material == null) {
                        continue;
                    }
                    result.addLast(new FoodHistoryManager.FoodHistoryEntry(foodId, material, ts));
                }
            }
        } catch (Exception ex) {
            if (plugin.isDebug()) {
                plugin.getLogger().warning("SQLite food_history loadRecent failed: " + ex.getMessage());
            }
        }
        return result;
    }

    @Override
    public synchronized void close() {
        try {
            connection.close();
        } catch (Exception ignored) {
        }
    }
}

