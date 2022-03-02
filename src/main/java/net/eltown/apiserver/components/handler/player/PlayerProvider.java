package net.eltown.apiserver.components.handler.player;

import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Provider;
import net.eltown.apiserver.components.handler.player.data.Player;
import org.bson.Document;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerProvider extends Provider {

    public final HashMap<UUID, Player> cachedPlayers = new HashMap<>();

    public PlayerProvider(final Server server) {
        super(server, "player_data");

        server.log("Spielerdaten werden in den Cache geladen...");
        for (final Document document : this.getCollection("player_data").find()) {
            this.cachedPlayers.put(UUID.fromString(document.getString("_id")),
                    new Player(
                            UUID.fromString(document.getString("_id")),
                            document.getString("name"),
                            document.getLong("firstLogin"),
                            document.getLong("lastLogin")
                    )
            );
        }
        server.log(this.cachedPlayers.size() + " Spielerdaten wurden in den Cache geladen.");
    }

    public void createPlayerData(final UUID uuid, final String player) {
        this.cachedPlayers.put(uuid, new Player(uuid, player, System.currentTimeMillis(), 0));

        CompletableFuture.runAsync(() -> {
            this.getCollection("player_data").insertOne(new Document("_id", uuid.toString())
                    .append("name", player)
                    .append("firstLogin", System.currentTimeMillis())
                    .append("lastLogin", 0L)
            );
        });
    }

    public void setLastLogin(final UUID uuid) {
        this.cachedPlayers.get(uuid).setLastLogin(System.currentTimeMillis());

        CompletableFuture.runAsync(() -> {
            this.getCollection("player_data").updateOne(new Document("_id", uuid.toString()), new Document("$set", new Document("lastLogin", System.currentTimeMillis())));
        });
    }

    public void updateName(final UUID uuid, final String player) {
        this.cachedPlayers.get(uuid).setName(player);

        CompletableFuture.runAsync(() -> {
            this.getCollection("player_data").updateOne(new Document("_id", uuid.toString()), new Document("$set", new Document("name", player)));
        });
    }

    public String getPlayer(final UUID uuid) {
        final StringBuilder builder = new StringBuilder("");
        final Player player = this.cachedPlayers.get(uuid);
        if (player == null) builder.append("null");
        else builder.append(player.getUUID()).append(";").append(player.getName()).append(";").append(player.getFirstLogin()).append(";").append(player.getLastLogin());
        return builder.toString();
    }

}
