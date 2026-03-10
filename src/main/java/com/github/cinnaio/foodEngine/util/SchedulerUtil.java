package com.github.cinnaio.foodEngine.util;

import com.github.cinnaio.foodEngine.FoodEngine;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

public final class SchedulerUtil {

    private static Plugin plugin;

    private SchedulerUtil() {
    }

    public static void initialize(FoodEngine foodEngine) {
        plugin = foodEngine;
    }

    public static void runGlobal(Runnable runnable) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, runnable);
    }

    public static void runForPlayer(Player player, Consumer<Player> consumer) {
        player.getScheduler().execute(plugin, () -> consumer.accept(player), null, 0L);
    }

    public static void runForPlayerLater(Player player, long delayTicks, Consumer<Player> consumer) {
        player.getScheduler().runDelayed(plugin, (ScheduledTask task) -> consumer.accept(player), null, delayTicks);
    }
}
