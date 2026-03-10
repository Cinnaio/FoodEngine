package com.github.cinnaio.foodEngine.storage;

import com.github.cinnaio.foodEngine.manager.FoodHistoryManager;

import java.util.Deque;
import java.util.UUID;

public interface FoodHistoryStorage extends AutoCloseable {

    void record(UUID playerId, FoodHistoryManager.FoodHistoryEntry entry, int maxEntries);

    Deque<FoodHistoryManager.FoodHistoryEntry> loadRecent(UUID playerId, int maxEntries);

    @Override
    void close();
}

