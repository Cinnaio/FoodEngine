package com.github.cinnaio.foodEngine.command;

import com.github.cinnaio.foodEngine.FoodEngine;
import com.github.cinnaio.foodEngine.manager.ComboManager;
import com.github.cinnaio.foodEngine.manager.FoodHistoryManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class FoodEngineCommand implements BasicCommand {

    private final FoodEngine plugin;

    public FoodEngineCommand(FoodEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull CommandSourceStack source, @NotNull String[] args) {
        CommandSender sender = source.getSender();
        Player player = sender instanceof Player p ? p : null;

        if (args.length == 0) {
            Component unknown = plugin.getLanguageManager().component("messages.error.unknown_subcommand", player);
            sender.sendMessage(unknown);
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> handleReload(sender, player);
            case "debug" -> handleDebug(sender, player, args);
            default -> {
                Component unknown = plugin.getLanguageManager().component("messages.error.unknown_subcommand", player);
                sender.sendMessage(unknown);
            }
        }
    }

    private void handleReload(CommandSender sender, Player player) {
        if (!sender.hasPermission("foodengine.reload")) {
            Component noPerm = plugin.getLanguageManager().component("messages.error.no_permission", player);
            sender.sendMessage(noPerm);
            return;
        }

        Component started = plugin.getLanguageManager().component("messages.reload.started", player);
        sender.sendMessage(started);

        boolean ok = plugin.reloadAll();
        Component result = plugin.getLanguageManager().component(ok ? "messages.reload.success" : "messages.reload.failure", player);
        sender.sendMessage(result);
    }

    private void handleDebug(CommandSender sender, Player player, String[] args) {
        if (!sender.hasPermission("foodengine.debug")) {
            Component noPerm = plugin.getLanguageManager().component("messages.error.no_permission", player);
            sender.sendMessage(noPerm);
            return;
        }

        if (args.length < 2 || !"list".equalsIgnoreCase(args[1])) {
            Component unknown = plugin.getLanguageManager().component("messages.error.unknown_subcommand", player);
            sender.sendMessage(unknown);
            return;
        }

        Player target;
        if (args.length >= 3) {
            target = plugin.getServer().getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[2]).color(NamedTextColor.RED));
                return;
            }
        } else {
            if (player == null) {
                sender.sendMessage(Component.text("You must specify a player when using this command from console.")
                        .color(NamedTextColor.RED));
                return;
            }
            target = player;
        }

        UUID uuid = target.getUniqueId();
        FoodHistoryManager history = plugin.getHistoryManager();
        ComboManager combos = plugin.getComboManager();

        sender.sendMessage(Component.text("FoodEngine debug for " + target.getName()).color(NamedTextColor.GOLD));

        var entries = history.snapshot(uuid);
        sender.sendMessage(Component.text("  Recent food history (newest last):").color(NamedTextColor.YELLOW));
        if (entries.isEmpty()) {
            sender.sendMessage(Component.text("    <none>").color(NamedTextColor.GRAY));
        } else {
            for (FoodHistoryManager.FoodHistoryEntry e : entries) {
                String ts = DateTimeFormatter.ofPattern("HH:mm:ss")
                        .withZone(ZoneId.systemDefault())
                        .format(Instant.ofEpochMilli(e.timestampMillis()));
                sender.sendMessage(Component.text("    [" + ts + "] food_id=" + e.foodEngineId()
                        + " material=" + e.material()).color(NamedTextColor.GRAY));
            }
        }

        Map<String, Integer> triggerCounts = combos.getTriggerCountsFor(uuid);
        sender.sendMessage(Component.text("  Combo triggers:").color(NamedTextColor.YELLOW));
        if (triggerCounts.isEmpty()) {
            sender.sendMessage(Component.text("    <none>").color(NamedTextColor.GRAY));
        } else {
            triggerCounts.forEach((key, count) -> sender.sendMessage(
                    Component.text("    " + key + " -> " + count).color(NamedTextColor.GRAY)
            ));
        }

    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            if (sender.hasPermission("foodengine.reload") && "reload".startsWith(prefix)) {
                suggestions.add("reload");
            }
            if (sender.hasPermission("foodengine.debug") && "debug".startsWith(prefix)) {
                suggestions.add("debug");
            }
            return suggestions;
        }

        if (args.length == 2 && "debug".equalsIgnoreCase(args[0])) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            if (sender.hasPermission("foodengine.debug") && "list".startsWith(prefix)) {
                suggestions.add("list");
            }
            return suggestions;
        }

        if (args.length == 3 && "debug".equalsIgnoreCase(args[0]) && "list".equalsIgnoreCase(args[1])) {
            if (!sender.hasPermission("foodengine.debug")) {
                return suggestions;
            }
            String prefix = args[2].toLowerCase(Locale.ROOT);
            plugin.getServer().getOnlinePlayers().forEach(p -> {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    suggestions.add(p.getName());
                }
            });
            return suggestions;
        }

        return suggestions;
    }
}
