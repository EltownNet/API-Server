package net.eltown.apiserver.components.handler.crates;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Provider;
import net.eltown.apiserver.components.handler.crates.data.CratePlayer;
import net.eltown.apiserver.components.handler.crates.data.CrateReward;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CratesProvider extends Provider {

    public final HashMap<String, CrateReward> cachedCrateRewards = new HashMap<>();
    public final HashMap<String, CratePlayer> cachedCratePlayers = new HashMap<>();

    @SneakyThrows
    public CratesProvider(final Server server) {
        super(server, "a2_crate_data", "crate_player_data");

        server.log("Crate-Reward-Daten werden in den Cache geladen...");
        for (final Document document : this.getCollection("a2_crate_data").find()) {
            this.cachedCrateRewards.put(document.getString("_id"),
                    new CrateReward(
                            document.getString("_id"),
                            document.getString("crate"),
                            document.getString("displayName"),
                            document.getInteger("chance"),
                            document.getString("data")
                    )
            );
        }
        server.log(this.cachedCrateRewards.size() + " Crate-Reward-Daten wurden in den Cache geladen...");

        server.log("Crate-Player-Daten werden in den Cache geladen...");
        for (final Document document : this.getCollection("crate_player_data").find()) {
            final Map<String, Integer> data = new HashMap<>();
            document.getList("data", String.class).forEach(e -> {
                data.put(e.split(":")[0], Integer.parseInt(e.split(":")[1]));
            });

            this.cachedCratePlayers.put(document.getString("_id"),
                    new CratePlayer(
                            document.getString("_id"),
                            data
                    )
            );
        }
        server.log(this.cachedCratePlayers.size() + " Crate-Player-Daten wurden in den Cache geladen...");
    }

    public void createPlayerData(final String player) {
        if (!this.cachedCratePlayers.containsKey(player)) {
            final Map<String, Integer> map = new HashMap<>();
            map.put("null", 0);
            this.cachedCratePlayers.put(player, new CratePlayer(player, map));

            CompletableFuture.runAsync(() -> {
                this.getCollection("crate_player_data").insertOne(new Document("_id", player).append("data", new ArrayList<>(Collections.singletonList("null:0"))));
            });
        }
    }

    public void addCrate(final String player, final String crate, final int i) {
        this.cachedCratePlayers.get(player).getData().put(crate, this.cachedCratePlayers.get(player).getData().getOrDefault(crate, 0) + i);

        this.updatePlayerDataToDatabase(player);
    }

    public void removeCrate(final String player, final String crate, final int i) {
        this.cachedCratePlayers.get(player).getData().put(crate, this.cachedCratePlayers.get(player).getData().getOrDefault(crate, 0) - i);

        this.updatePlayerDataToDatabase(player);
    }

    private void updatePlayerDataToDatabase(final String player) {
        CompletableFuture.runAsync(() -> {
            final List<String> set = new ArrayList<>();
            this.cachedCratePlayers.get(player).getData().forEach((k, v) -> {
                set.add(k + ":" + v);
            });

            this.getCollection("crate_player_data").updateOne(new Document("_id", player), new Document("$set", new Document("data", set)));
        });
    }

    public void insertCrateReward(final String id, final String crate, final String displayName, final int chance, final String data) {
        this.cachedCrateRewards.put(id, new CrateReward(id, crate, displayName, chance, data));

        CompletableFuture.runAsync(() -> {
            this.getCollection("a2_crate_data").insertOne(new Document("_id", id)
                    .append("crate", crate)
                    .append("displayName", displayName)
                    .append("chance", chance)
                    .append("data", data)
            );
        });
    }

    public void deleteCrateReward(final String id) {
        this.cachedCrateRewards.remove(id);

        CompletableFuture.runAsync(() -> this.getCollection("a2_crate_data").findOneAndDelete(new Document("_id", id)));
    }

    public void updateCrateReward(final String id, final String crate, final String displayName, final int chance, final String data) {
        final CrateReward crateReward = this.cachedCrateRewards.get(id);
        crateReward.setCrate(crate);
        crateReward.setDisplayName(displayName);
        crateReward.setChance(chance);
        crateReward.setData(data);

        CompletableFuture.runAsync(() -> {
            this.getCollection("a2_crate_data").updateOne(new Document("_id", id), new Document("$set", new Document("crate", crate).append("displayName", displayName).append("chance", chance).append("data", data)));
        });
    }

    public Set<CrateReward> getCrateRewards(final String crate) {
        final Set<CrateReward> crateRewards = new HashSet<>();
        this.cachedCrateRewards.values().forEach(e -> {
            if (e.getCrate().equals(crate)) crateRewards.add(e);
        });
        return crateRewards;
    }

}
