package com.github.cinnaio.foodEngine;

import com.github.cinnaio.foodEngine.config.FoodConfigLoader;
import com.github.cinnaio.foodEngine.command.FoodEngineCommand;
import com.github.cinnaio.foodEngine.lang.LanguageManager;
import com.github.cinnaio.foodEngine.listener.FoodConsumeListener;
import com.github.cinnaio.foodEngine.manager.ActionManager;
import com.github.cinnaio.foodEngine.manager.ComboManager;
import com.github.cinnaio.foodEngine.manager.FoodHistoryManager;
import com.github.cinnaio.foodEngine.manager.FoodRegistry;
import com.github.cinnaio.foodEngine.parser.ActionParser;
import com.github.cinnaio.foodEngine.parser.ConditionParser;
import com.github.cinnaio.foodEngine.storage.ComboStorage;
import com.github.cinnaio.foodEngine.storage.InMemoryComboStorage;
import com.github.cinnaio.foodEngine.storage.FoodHistoryStorage;
import com.github.cinnaio.foodEngine.storage.SqliteComboStorage;
import com.github.cinnaio.foodEngine.storage.SqliteFoodHistoryStorage;
import com.github.cinnaio.foodEngine.util.SchedulerUtil;
import com.github.cinnaio.foodEngine.util.TextUtil;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class FoodEngine extends JavaPlugin {

    private static FoodEngine instance;

    private boolean debug;
    private LanguageManager languageManager;
    private FoodRegistry foodRegistry;
    private ActionManager actionManager;
    private FoodConfigLoader foodConfigLoader;
    private NamespacedKey foodIdKey;
    private FoodHistoryManager historyManager;
    private ComboManager comboManager;
    private ComboStorage comboStorage;
    private FoodHistoryStorage historyStorage;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        reloadDebugFlag();

        this.languageManager = new LanguageManager(this);
        this.languageManager.reload();

        TextUtil.initialize(this);
        SchedulerUtil.initialize(this);

        ActionParser actionParser = new ActionParser(new ConditionParser(this));
        this.foodRegistry = new FoodRegistry();
        this.actionManager = new ActionManager(this, actionParser, foodRegistry);
        this.foodConfigLoader = new FoodConfigLoader(this, actionParser, foodRegistry);
        this.historyStorage = createHistoryStorage();
        this.historyManager = new FoodHistoryManager(20, historyStorage);
        this.comboStorage = createComboStorage();
        this.comboManager = new ComboManager(comboStorage);

        this.foodIdKey = new NamespacedKey(this, "food_engine_id");

        foodConfigLoader.loadAll();

        Bukkit.getPluginManager().registerEvents(
                new FoodConsumeListener(this, foodRegistry, actionManager, historyManager, comboManager, foodIdKey),
                this
        );

        registerCommands();

        Component enabledMessage = languageManager.component("messages.plugin.enabled", null);
        Audience audience = Bukkit.getConsoleSender();
        audience.sendMessage(enabledMessage);
    }

    @Override
    public void onDisable() {
        Component disabledMessage = languageManager != null
                ? languageManager.component("messages.plugin.disabled", null)
                : Component.text("FoodEngine disabled");
        Bukkit.getConsoleSender().sendMessage(disabledMessage);
        if (comboStorage != null) {
            comboStorage.close();
            comboStorage = null;
        }
        if (historyStorage != null) {
            historyStorage.close();
            historyStorage = null;
        }
        instance = null;
    }

    private void registerCommands() {
        FoodEngineCommand command = new FoodEngineCommand(this);
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register("foodengine", "FoodEngine administration command", java.util.List.of("fe", "feg"), command);
        });
    }

    public boolean reloadAll() {
        try {
            reloadConfig();
            reloadDebugFlag();
            languageManager.reload();
            foodConfigLoader.loadAll();
            return true;
        } catch (Exception ex) {
            getLogger().severe("Failed to reload FoodEngine configuration");
            ex.printStackTrace();
            return false;
        }
    }

    private void reloadDebugFlag() {
        this.debug = getConfig().getBoolean("debug", false);
    }

    public static FoodEngine getInstance() {
        return instance;
    }

    public boolean isDebug() {
        return debug;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public FoodRegistry getFoodRegistry() {
        return foodRegistry;
    }

    public ActionManager getActionManager() {
        return actionManager;
    }

    public NamespacedKey getFoodIdKey() {
        return foodIdKey;
    }

    public Plugin asPlugin() {
        return this;
    }

    public FoodHistoryManager getHistoryManager() {
        return historyManager;
    }

    public ComboManager getComboManager() {
        return comboManager;
    }

    private ComboStorage createComboStorage() {
        boolean enabled = getConfig().getBoolean("storage.combo.sqlite.enabled", true);
        if (!enabled) {
            return new InMemoryComboStorage();
        }

        String fileName = getConfig().getString("storage.combo.sqlite.file", "combo.db");
        if (fileName == null || fileName.isBlank()) {
            fileName = "combo.db";
        }
        File file = new File(getDataFolder(), fileName);
        try {
            return new SqliteComboStorage(this, file);
        } catch (Exception ex) {
            getLogger().warning("SQLite combo storage init failed; falling back to memory: " + ex.getMessage());
            return new InMemoryComboStorage();
        }
    }

    private FoodHistoryStorage createHistoryStorage() {
        boolean enabled = getConfig().getBoolean("storage.history.sqlite.enabled", true);
        if (!enabled) {
            return null;
        }
        String fileName = getConfig().getString("storage.history.sqlite.file", "combo.db");
        if (fileName == null || fileName.isBlank()) {
            fileName = "combo.db";
        }
        File file = new File(getDataFolder(), fileName);
        try {
            return new SqliteFoodHistoryStorage(this, file);
        } catch (Exception ex) {
            getLogger().warning("SQLite history storage init failed; history will be in-memory only: " + ex.getMessage());
            return null;
        }
    }
}
