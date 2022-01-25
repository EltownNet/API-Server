package net.eltown.apiserver.components.handler.giftkeys;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Provider;
import net.eltown.apiserver.components.handler.giftkeys.data.Giftkey;
import org.bson.Document;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class GiftkeyProvider extends Provider {

    public final LinkedHashMap<String, Giftkey> giftkeys = new LinkedHashMap<>();

    @SneakyThrows
    public GiftkeyProvider(final Server server) {
        super(server, "a2_giftkeys");

        server.log("Giftkeys werden in den Cache geladen...");
        for (final Document document : this.getCollection("a2_giftkeys").find()) {
            this.giftkeys.put(document.getString("_id"), new Giftkey(
                    document.getString("_id"),
                    document.getInteger("maxUses"),
                    document.getList("uses", String.class),
                    document.getList("rewards", String.class),
                    document.getList("marks", String.class),
                    document.getLong("duration")
            ));
        }
        server.log(this.giftkeys.size() + " Giftkeys wurden in den Cache geladen...");
    }

    public void createKey(final String key, final int maxUses, final List<String> rewards, List<String> marks, final long duration, final Consumer<String> keyCallback) {
        this.giftkeys.put(key, new Giftkey(key, maxUses, new ArrayList<>(), rewards, marks, duration));
        CompletableFuture.runAsync(() -> {
            this.getCollection("a2_giftkeys").insertOne(new Document("_id", key).append("maxUses", maxUses).append("uses", new ArrayList<>()).append("rewards", rewards).append("marks", marks).append("duration", duration));
        });
        keyCallback.accept(key);
    }

    public boolean keyExists(final String key) {
        return this.giftkeys.containsKey(key);
    }

    public Giftkey getGiftKey(final String key) {
        return this.giftkeys.get(key);
    }

    public void redeemKey(final String key, final String player) {
        final Giftkey giftkey = this.giftkeys.get(key);
        final List<String> list = giftkey.getUses();
        list.add(player);
        giftkey.setUses(list);
        CompletableFuture.runAsync(() -> {
            this.getCollection("a2_giftkeys").updateOne(new Document("_id", key), new Document("$set", new Document("uses", list)));

            if (list.size() >= giftkey.getMaxUses()) this.deleteKey(key);
        });
    }

    public boolean alreadyRedeemed(final String key, final String player) {
        final AtomicBoolean aBoolean = new AtomicBoolean(false);
        this.giftkeys.values().forEach(e -> {
            if (e.getKey().equals(key)) {
                if (e.getUses().contains(player)) aBoolean.set(true);
            }
        });
        return aBoolean.get();
    }

    public List<String> getKeysByMark(final String mark) {
        final List<String> keyList = new ArrayList<>();
        this.giftkeys.forEach((key, giftkey) -> {
            if (giftkey.getMarks().contains(mark)) {
                if (!giftkey.getUses().contains(mark)) {
                    keyList.add(giftkey.getKey());
                }
            }
        });
        return keyList;
    }

    public void addMarkToKey(final String key, final String target, final String from) {
        try {
            final List<String> list = new ArrayList<>();
            this.giftkeys.get(key).getMarks().forEach(e -> {
                if (!e.equals(from)) {
                    list.add(e);
                }
            });
            list.add(target);
            this.giftkeys.get(key).setMarks(list);

            CompletableFuture.runAsync(() -> {
                this.getCollection("a2_giftkeys").updateOne(new Document("_id", key), new Document("$set", new Document("marks", list)));
            });
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteKey(final String key) {
        this.giftkeys.remove(key);
        CompletableFuture.runAsync(() -> {
            this.getCollection("a2_giftkeys").findOneAndDelete(new Document("_id", key));
        });
    }

}
