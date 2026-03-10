package com.github.cinnaio.foodEngine.action;

import com.github.cinnaio.foodEngine.util.SchedulerUtil;
import com.github.cinnaio.foodEngine.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;

public class CommandAction implements ActionExecutor {

    @Override
    public void execute(Player player, String foodId, com.github.cinnaio.foodEngine.model.ParsedAction action) {
        String command = action.getParameters();
        if (command == null || command.isEmpty()) {
            return;
        }

        final String cmd = TextUtil.applyPlaceholdersOnly(command, player);

        Map<String, String> flags = action.getFlags();
        String mode = flags.getOrDefault("as", "console").toLowerCase(Locale.ROOT);

        switch (mode) {
            case "player" -> {
                SchedulerUtil.runForPlayer(player, p ->
                        Bukkit.dispatchCommand(p, cmd)
                );
            }
            case "op" -> {
                SchedulerUtil.runForPlayer(player, p -> {
                    boolean wasOp = p.isOp();
                    try {
                        if (!wasOp) {
                            p.setOp(true);
                        }
                        Bukkit.dispatchCommand(p, cmd);
                    } finally {
                        if (!wasOp) {
                            p.setOp(false);
                        }
                    }
                });
            }
            default -> SchedulerUtil.runGlobal(() -> {
                CommandSender console = Bukkit.getConsoleSender();
                Bukkit.dispatchCommand(console, cmd);
            });
        }
    }
}

