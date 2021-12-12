package net.eltown.apiserver.components.handler.friends;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Provider;
import net.eltown.apiserver.components.config.Config;
import net.eltown.apiserver.components.handler.friends.data.FriendData;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbit;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FriendProvider extends Provider {

    public final HashMap<String, FriendData> cachedFriendData = new HashMap<>();

    @SneakyThrows
    public FriendProvider(final Server server) {
        super(server, "friends");

        server.log("Freunde-Daten werden in den Cache geladen...");
        for (final Document document : this.getCollection("friends").find()) {
            this.cachedFriendData.put(document.getString("_id"), new FriendData(
                    document.getString("_id"),
                    document.getList("friends", String.class),
                    document.getList("requests", String.class)
            ));
        }
        server.log(this.cachedFriendData.size() + " Freunde-Daten wurden in den Cache geladen...");
    }

    public void createFriendData(final String player) {
        this.cachedFriendData.put(player, new FriendData(player, new ArrayList<>(), new ArrayList<>()));

        CompletableFuture.runAsync(() -> {
            this.getCollection("friends").insertOne(new Document("_id", player).append("friends", new ArrayList<String>()).append("requests", new ArrayList<String>()));
        });
    }

    public boolean friendDataExists(final String player) {
        return this.cachedFriendData.containsKey(player);
    }

    public void createFriendRequest(final String player, final String target) {
        this.cachedFriendData.get(target).getRequests().add(player);

        CompletableFuture.runAsync(() -> {
            final Document tD = this.getCollection("friends").find(new Document("_id", target)).first();
            assert tD != null;

            final List<String> tDSet = tD.getList("requests", String.class);
            tDSet.add(player);

            this.getCollection("friends").updateOne(new Document("_id", target), new Document("$set", new Document("requests", tDSet)));
        });
    }

    public void removeFriendRequest(final String player, final String target) {
        this.cachedFriendData.get(player).getRequests().remove(target);

        CompletableFuture.runAsync(() -> {
            final Document tD = this.getCollection("friends").find(new Document("_id", player)).first();
            assert tD != null;

            final List<String> tDSet = tD.getList("requests", String.class);
            tDSet.remove(target);

            this.getCollection("friends").updateOne(new Document("_id", player), new Document("$set", new Document("requests", tDSet)));
        });
    }

    public void createFriendship(final String player, final String target) {
        this.cachedFriendData.get(player).getFriends().add(target);
        this.cachedFriendData.get(target).getFriends().add(player);

        CompletableFuture.runAsync(() -> {
            final Document pD = this.getCollection("friends").find(new Document("_id", player)).first();
            final Document tD = this.getCollection("friends").find(new Document("_id", target)).first();
            assert pD != null && tD != null;

            final List<String> pDSet = pD.getList("friends", String.class);
            pDSet.add(target);
            final List<String> tDSet = tD.getList("friends", String.class);
            tDSet.add(player);

            this.getCollection("friends").updateOne(new Document("_id", player), new Document("$set", new Document("friends", pDSet)));
            this.getCollection("friends").updateOne(new Document("_id", target), new Document("$set", new Document("friends", tDSet)));
        });
    }

    public void removeFriendship(final String player, final String target) {
        this.cachedFriendData.get(player).getFriends().remove(target);
        this.cachedFriendData.get(target).getFriends().remove(player);

        CompletableFuture.runAsync(() -> {
            final Document pD = this.getCollection("friends").find(new Document("_id", player)).first();
            final Document tD = this.getCollection("friends").find(new Document("_id", target)).first();
            assert pD != null && tD != null;

            final List<String> pDSet = pD.getList("friends", String.class);
            pDSet.remove(target);
            final List<String> tDSet = tD.getList("friends", String.class);
            tDSet.remove(player);

            this.getCollection("friends").updateOne(new Document("_id", player), new Document("$set", new Document("friends", pDSet)));
            this.getCollection("friends").updateOne(new Document("_id", target), new Document("$set", new Document("friends", tDSet)));
        });
    }

    public boolean areFriends(final String player, final String target) {
        final FriendData pD = this.cachedFriendData.get(player);
        final FriendData tD = this.cachedFriendData.get(target);
        return (pD.getFriends().contains(target) && tD.getFriends().contains(player));
    }

    public boolean requestExists(final String player, final String target) {
        final FriendData pD = this.cachedFriendData.get(player);
        return (pD.getRequests().contains(target));
    }

}
