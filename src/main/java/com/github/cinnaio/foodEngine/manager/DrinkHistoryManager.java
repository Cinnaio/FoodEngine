package com.github.cinnaio.foodEngine.manager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DrinkHistoryManager {

    public record Result(int count, long intervalSeconds) {}

    private final Map<java.util.UUID, Map<String, List<Long>>> drinkHistory = new ConcurrentHashMap<>();

    public void record(java.util.UUID playerId, String foodEngineId, long timestampMillis) {
        if (playerId == null || foodEngineId == null || foodEngineId.isEmpty()) {
            return;
        }
        Map<String, List<Long>> perFood = drinkHistory.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        List<Long> list = perFood.computeIfAbsent(foodEngineId, k -> Collections.synchronizedList(new ArrayList<>()));
        list.add(timestampMillis);
    }

    public Result compute(java.util.UUID playerId, String foodEngineId, long windowSeconds, long nowMillis) {
        Map<String, List<Long>> perFood = drinkHistory.get(playerId);
        List<Long> list = perFood == null ? null : perFood.get(foodEngineId);
        if (list == null || list.isEmpty()) {
            return new Result(0, 0L);
        }
        long windowMillis = Math.max(0L, windowSeconds) * 1000L;
        long cutoff = windowMillis > 0 ? nowMillis - windowMillis : Long.MIN_VALUE;

        int count = 0;
        long last = 0L;
        synchronized (list) {
            Iterator<Long> it = list.iterator();
            while (it.hasNext()) {
                long ts = it.next();
                if (ts < cutoff) {
                    it.remove();
                    continue;
                }
                count++;
                if (ts > last) {
                    last = ts;
                }
            }
        }
        long interval = last > 0L ? Math.max(0L, (nowMillis - last) / 1000L) : 0L;
        return new Result(count, interval);
    }
}
