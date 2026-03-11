package com.github.cinnaio.foodEngine.storage;

import com.github.cinnaio.foodEngine.FoodEngine;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SqliteComboStorage implements ComboStorage {

    private final FoodEngine plugin;
    private final Connection connection;

    public SqliteComboStorage(FoodEngine plugin, File file) {
        this.plugin = plugin;
        try {
            // Ensure driver is loaded
            Class.forName("org.sqlite.JDBC");

            String url = "jdbc:sqlite:" + file.getAbsolutePath();
            this.connection = DriverManager.getConnection(url);

            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL;");
                st.execute("PRAGMA synchronous=NORMAL;");
                st.execute("PRAGMA foreign_keys=ON;");
                st.execute("PRAGMA busy_timeout=3000;");
            }

            try (Statement st = connection.createStatement()) {
                st.execute("""
                        CREATE TABLE IF NOT EXISTS combo_state (
                          player_uuid TEXT NOT NULL,
                          food_id TEXT NOT NULL,
                          combo_index INTEGER NOT NULL,
                          trigger_count INTEGER NOT NULL DEFAULT 0,
                          PRIMARY KEY (player_uuid, food_id, combo_index)
                        );
                        """);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to initialize SQLite combo storage", ex);
        }
    }

    

    @Override
    public synchronized int incrementTriggerCount(UUID playerId, String foodId, int comboIndex) {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                        INSERT INTO combo_state(player_uuid, food_id, combo_index, trigger_count)
                        VALUES(?, ?, ?, 1)
                        ON CONFLICT(player_uuid, food_id, combo_index)
                        DO UPDATE SET trigger_count=combo_state.trigger_count + 1;
                        """)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, foodId);
            ps.setInt(3, comboIndex);
            ps.executeUpdate();
        } catch (Exception ex) {
            if (plugin.isDebug()) {
                plugin.getLogger().warning("SQLite incrementTrigger failed: " + ex.getMessage());
            }
        }

        // Read back current count
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT trigger_count FROM combo_state WHERE player_uuid=? AND food_id=? AND combo_index=?")) {
            ps.setString(1, playerId.toString());
            ps.setString(2, foodId);
            ps.setInt(3, comboIndex);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception ex) {
            return 0;
        }
    }

    

    @Override
    public synchronized Map<String, Integer> getTriggerCountsFor(UUID playerId) {
        Map<String, Integer> result = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT food_id, combo_index, trigger_count FROM combo_state WHERE player_uuid=?")) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String foodId = rs.getString(1);
                    int idx = rs.getInt(2);
                    int count = rs.getInt(3);
                    result.put(foodId + "|" + idx, count);
                }
            }
        } catch (Exception ex) {
            if (plugin.isDebug()) {
                plugin.getLogger().warning("SQLite getTriggerCountsFor failed: " + ex.getMessage());
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
