package net.eltown.apiserver.components.handler.economy;

import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Provider;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class EconomyProvider extends Provider {

    private final Map<String, Double> players = new HashMap<>();

    public EconomyProvider(final Server server) {
        super(server, "economy");

        for (Document document : this.getCollection("economy").find()) {
            this.players.put(document.getString("_id"), document.getDouble("money"));
        }

        server.log(this.players.size() + " Spieler wurden in den Cache geladen.");
    }

    public double get(String id) {
        return players.getOrDefault(id, 0.0);
    }

    public boolean has(String id) {
        return this.players.containsKey(id);
    }

    public void create(String id, double money) {
        this.players.put(id, money);
        CompletableFuture.runAsync(() -> {
            this.getCollection("economy").insertOne(new Document("_id", id).append("money", money));
        });
    }

    public void set(String id, double money) {
        this.players.put(id, money);
        CompletableFuture.runAsync(() -> {
            this.getCollection("economy").updateOne(new Document("_id", id), new Document("$set", new Document("money", money)));
        });
    }

    public List<String> getAll() {
        final List<String> list = new ArrayList<>();
        players.forEach((user, money) -> list.add(user + ":" + money));
        return list;
    }

}
