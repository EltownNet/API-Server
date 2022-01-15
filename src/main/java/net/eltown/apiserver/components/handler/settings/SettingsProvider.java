package net.eltown.apiserver.components.handler.settings;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Provider;
import net.eltown.apiserver.components.handler.settings.data.AccountSettings;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SettingsProvider extends Provider {

    public final HashMap<String, AccountSettings> cachedSettings = new HashMap<>();

    @SneakyThrows
    public SettingsProvider(final Server server) {
        super(server, "player_settings");

        server.log("Nutzer-Einstellungen werden in den Cache geladen...");
        for (final Document document : this.getCollection("player_settings").find()) {
            final List<String> d = document.getList("settings", String.class);
            final Map<String, String> map = new HashMap<>();
            d.forEach(e -> {
                map.put(e.split(":")[0], e.split(":")[1]);
            });

            this.cachedSettings.put(document.getString("_id"), new AccountSettings(
                    document.getString("_id"),
                    map
            ));
        }
        server.log(this.cachedSettings.size() + " Nutzer-Einstellungen wurden in den Cache geladen...");
    }

    public void createAccountSettings(final String player) {
        this.cachedSettings.put(player, new AccountSettings(player, new HashMap<>()));

        CompletableFuture.runAsync(() -> {
            this.getCollection("player_settings").insertOne(new Document("_id", player).append("settings", new ArrayList<String>()));
        });
    }

    public String getEntry(final String player, final String key, final String def) {
        if (!this.cachedSettings.containsKey(player)) this.createAccountSettings(player);
        return this.cachedSettings.get(player).getSettings().getOrDefault(key, def);
    }

    public void updateEntry(final String player, final String key, final String value) {
        if (!this.cachedSettings.containsKey(player)) this.createAccountSettings(player);

        final Map<String, String> map = this.cachedSettings.get(player).getSettings();
        map.remove(key);
        map.put(key, value);
        this.cachedSettings.get(player).setSettings(map);

        CompletableFuture.runAsync(() -> {
            final Document document = this.getCollection("player_settings").find(new Document("_id", player)).first();
            assert document != null;
            final List<String> d = document.getList("settings", String.class);
            final List<String> set = new ArrayList<>();

            d.forEach(e -> {
                if (!e.startsWith(key)) {
                    set.add(e);
                }
            });
            set.add(key + ":" + value);

            this.getCollection("player_settings").updateOne(new Document("_id", player), new Document("$set", new Document("settings", set)));
        });
    }

    public void updateAll(final String player, final String settings) {
        if (!this.cachedSettings.containsKey(player)) this.createAccountSettings(player);

        final List<String> rawData = Arrays.asList(settings.split(">:<"));
        final Map<String, String> map = new HashMap<>();
        rawData.forEach(e -> {
            map.put(e.split(":")[0], e.split(":")[1]);
        });
        this.cachedSettings.get(player).setSettings(map);

        CompletableFuture.runAsync(() -> {
            final Document document = this.getCollection("player_settings").find(new Document("_id", player)).first();
            assert document != null;
            final List<String> set = new ArrayList<>(rawData);

            this.getCollection("player_settings").updateOne(new Document("_id", player), new Document("$set", new Document("settings", set)));
        });
    }

    public void removeEntry(final String player, final String key) {
        final Map<String, String> map = this.cachedSettings.get(player).getSettings();
        map.remove(key);
        this.cachedSettings.get(player).setSettings(map);

        CompletableFuture.runAsync(() -> {
            final Document document = this.getCollection("player_settings").find(new Document("_id", player)).first();
            assert document != null;
            final List<String> d = document.getList("settings", String.class);
            final List<String> set = new ArrayList<>();

            d.forEach(e -> {
                if (!e.startsWith(key)) {
                    set.add(e);
                }
            });

            this.getCollection("player_settings").updateOne(new Document("_id", player), new Document("$set", new Document("settings", set)));
        });
    }
}