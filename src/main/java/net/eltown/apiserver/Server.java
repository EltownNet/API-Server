package net.eltown.apiserver;

import lombok.Getter;
import lombok.SneakyThrows;
import net.eltown.apiserver.components.Handler;
import net.eltown.apiserver.components.config.Config;
import net.eltown.apiserver.components.data.Colors;
import net.eltown.apiserver.components.data.LogLevel;
import net.eltown.apiserver.components.handler.association.AssociationHandler;
import net.eltown.apiserver.components.handler.bank.BankHandler;
import net.eltown.apiserver.components.handler.chestshops.ChestshopHandler;
import net.eltown.apiserver.components.handler.crates.CratesHandler;
import net.eltown.apiserver.components.handler.crypto.CryptoHandler;
import net.eltown.apiserver.components.handler.drugs.DrugHandler;
import net.eltown.apiserver.components.handler.economy.EconomyHandler;
import net.eltown.apiserver.components.handler.friends.FriendHandler;
import net.eltown.apiserver.components.handler.giftkeys.GiftkeyHandler;
import net.eltown.apiserver.components.handler.groupmanager.GroupHandler;
import net.eltown.apiserver.components.handler.level.LevelHandler;
import net.eltown.apiserver.components.handler.player.PlayerHandler;
import net.eltown.apiserver.components.handler.quests.QuestHandler;
import net.eltown.apiserver.components.handler.rewards.RewardHandler;
import net.eltown.apiserver.components.handler.settings.SettingsHandler;
import net.eltown.apiserver.components.handler.shops.ShopHandler;
import net.eltown.apiserver.components.handler.teleportation.TeleportationHandler;
import net.eltown.apiserver.components.handler.ticketsystem.TicketHandler;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
public class Server {

    private static Server instance;

    private ExecutorService executor;

    private Config config;

    private final Map<String, Handler> handlers = new HashMap<>();

    public Server() {
        instance = this;
    }

    @SneakyThrows
    public void start() {
        this.log("Server wird gestartet...");
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.log(4, Runtime.getRuntime().availableProcessors() + " Threads erstellt.");
        this.config = new Config(this.getDataFolder() + "/config.yml", Config.YAML);
        config.reload();
        config.save();
        if (!config.exists("LogLevel")) config.set("LogLevel", 1);
        if (!config.exists("MongoDB")) {
            config.set("MongoDB.Connection", "mongodb://root:Qco7TDqoYq3RXq4pA3y7ETQTK6AgqzmTtRGLsgbN@45.138.50.23:27017/admin?authSource=admin");
            config.set("MongoDB.Database", "eltown");
        }
        final int logLevel = config.getInt("LogLevel");
        Internal.LOG_LEVEL = logLevel == 1 ? LogLevel.HIGH : logLevel == 2 ? LogLevel.MEDIUM : logLevel == 3 ? LogLevel.LOW : LogLevel.DEBUG;
        config.save();

        this.registerHandler(
                new EconomyHandler(this),
                new CryptoHandler(this),
                new PlayerHandler(this),
                new GroupHandler(this),
                new TeleportationHandler(this),
                new TicketHandler(this),
                new ShopHandler(this),
                new GiftkeyHandler(this),
                new LevelHandler(this),
                new DrugHandler(this),
                new BankHandler(this),
                new RewardHandler(this),
                new QuestHandler(this),
                new FriendHandler(this),
                new SettingsHandler(this),
                new CratesHandler(this),
                new ChestshopHandler(this),
                new AssociationHandler(this)
        );

        this.log("Server wurde erfolgreich gestartet.");
    }

    @SneakyThrows
    public void stop() {
        this.log("Server wird gestoppt...");
        for (final Handler handler : this.handlers.values()) handler.onDisable();
        this.log("Auf wiedersehen!");
    }

    public void registerHandler(final Handler... handlers) {
        for (final Handler handler : handlers) this.handlers.put(handler.getName(), handler);
    }

    @SneakyThrows
    public String getDataFolder() {
        return Loader.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath().replace("api-server.jar", "");
    }

    public static Server getInstance() {
        return instance;
    }

    public void log(final String message) {
        this.log(LogLevel.HIGH, message);
    }

    public void log(final int level, final String message) {
        this.log(level == 1 ? LogLevel.HIGH : level == 2 ? LogLevel.MEDIUM : level == 3 ? LogLevel.LOW : LogLevel.DEBUG, message);
    }

    public void log(final LogLevel logLevel, final String message) {
        CompletableFuture.runAsync(() -> {
            if (logLevel.level <= Internal.LOG_LEVEL.level) {
                try {
                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                    LocalDateTime time = timestamp.toLocalDateTime();
                    System.out.println(Colors.ANSI_CYAN + time.getHour() + ":" + time.getMinute() + ":" + time.getSecond() + Colors.ANSI_WHITE + " [" + Colors.ANSI_BLUE + "LOG" + Colors.ANSI_WHITE + "] " + message);
                    String file = time.getHour() + ":" + time.getMinute() + ":" + time.getSecond() + " [LOG] " + message + "\n";
                    Files.writeString(Paths.get("logs/" + time.getDayOfMonth() + "-" + time.getMonth() + "-" + time.getYear() + ".log"),
                            file,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.APPEND
                    );
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.out.println(Colors.ANSI_CYAN + "--------");
                    System.out.println(Colors.ANSI_CYAN + "HINWEIS: " + Colors.ANSI_RESET + " Falls es sich um den logs/XX-MONAT-XXXX Fehler handelt, erstelle den Ordner 'logs'.");
                    System.out.println(Colors.ANSI_CYAN + "--------");
                }
            }
        });
    }

}
