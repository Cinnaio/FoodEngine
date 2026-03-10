package com.github.cinnaio.foodEngine.manager;

import org.bukkit.Material;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FoodHistoryManager {

    public record FoodHistoryEntry(String foodEngineId, Material material, long timestampMillis) {
    }

    private final Map<UUID, Deque<FoodHistoryEntry>> history = new ConcurrentHashMap<>();
    private final int maxEntries;
    private final com.github.cinnaio.foodEngine.storage.FoodHistoryStorage storage;

    public FoodHistoryManager(int maxEntries) {
        this(maxEntries, null);
    }

    public FoodHistoryManager(int maxEntries, com.github.cinnaio.foodEngine.storage.FoodHistoryStorage storage) {
        this.maxEntries = Math.max(1, maxEntries);
        this.storage = storage;
    }

    public void record(UUID playerId, String foodEngineId, Material material, long timestampMillis) {
        Deque<FoodHistoryEntry> deque = history.computeIfAbsent(playerId, id -> new ArrayDeque<>());
        FoodHistoryEntry entry = new FoodHistoryEntry(foodEngineId, material, timestampMillis);
        synchronized (deque) {
            deque.addLast(entry);
            while (deque.size() > maxEntries) {
                deque.removeFirst();
            }
        }
        if (storage != null) {
            storage.record(playerId, entry, maxEntries);
        }
    }

    public Deque<FoodHistoryEntry> snapshot(UUID playerId) {
        Deque<FoodHistoryEntry> deque = history.get(playerId);
        if (deque == null) {
            if (storage != null) {
                Deque<FoodHistoryEntry> loaded = storage.loadRecent(playerId, maxEntries);
                if (!loaded.isEmpty()) {
                    history.put(playerId, loaded);
                    return new ArrayDeque<>(loaded);
                }
            }
            return new ArrayDeque<>();
        }
        synchronized (deque) {
            return new ArrayDeque<>(deque);
        }
    }
}

