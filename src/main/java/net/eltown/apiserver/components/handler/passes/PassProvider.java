package net.eltown.apiserver.components.handler.passes;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Provider;
import net.eltown.apiserver.components.handler.passes.data.Season;
import net.eltown.apiserver.components.handler.passes.data.SeasonPlayer;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PassProvider extends Provider {

    private final HashMap<String, SeasonPlayer> cachedPlayers = new HashMap<>();
    public Season season = null;

    @SneakyThrows
    public PassProvider(final Server server) {
        super(server, "pass_season", "pass_players");

        server.log("Season wird in den Cache geladen...");
        final Document seasonDocument = this.getCollection("pass_season").find().first();
        if (seasonDocument != null) {
            final List<String> rawRewards = seasonDocument.getList("rewards", String.class);
            final HashMap<String, Season.SeasonReward> seasonRewards = new HashMap<>();
            rawRewards.forEach(rawReward -> {
                final String[] d = rawReward.split(";");
                seasonRewards.put(d[0], new Season.SeasonReward(d[0], Integer.parseInt(d[1]), d[2], d[3], d[4], d[5]));
            });

            this.season = new Season(
                    seasonDocument.getString("name"),
                    seasonDocument.getString("description"),
                    seasonDocument.getList("quests", String.class),
                    seasonRewards,
                    seasonDocument.getLong("expire"),
                    seasonDocument.getBoolean("isActive")
            );
            server.log("Season wurde in den Cache geladen.");
        } else server.log("Es konnte keine Season gefunden werden.");

        server.log("Pass-Spieler werden in den Cache geladen...");
        for (final Document document : this.getCollection("pass_players").find()) {
            this.cachedPlayers.put(document.getString("_id"), new SeasonPlayer(
                    document.getString("_id"),
                    document.getInteger("points"),
                    document.getList("claimedRewards", String.class),
                    document.getBoolean("isPremium")
            ));
        }
        server.log(this.cachedPlayers.size() + " Pass-Spieler wurden in den Cache geladen...");
    }

    public void createSeason(final String name, final String description, final long expire) {
        this.season = new Season(name, description, new ArrayList<>(), new HashMap<>(), expire, false);

        CompletableFuture.runAsync(() -> {
            this.getCollection("pass_season").insertOne(new Document("name", name)
                    .append("description", description)
                    .append("quests", new ArrayList<String>())
                    .append("rewards", new ArrayList<String>())
                    .append("expire", expire)
                    .append("isActive", false)
            );
        });
    }

    public boolean seasonExists() {
        return this.season != null;
    }

    public void deleteSeason() {
        if (this.season != null) {
            CompletableFuture.runAsync(() -> {
                this.getCollection("pass_season").findOneAndDelete(new Document("name", this.season.getName()));
            });

            if (this.season.isActive()) {
                // TODO: 11.03.2022 Remove all active player quests of this season
            }
            this.season = null;
        }
    }

    public void addQuestToSeason(final String questId) {
        if (this.season != null) {
            this.season.getQuests().add(questId);

            CompletableFuture.runAsync(() -> {
                final Document document = this.getCollection("pass_season").find(new Document("name", this.season.getName())).first();
                assert document != null;
                final List<String> list = document.getList("quests", String.class);
                list.add(questId);
                this.getCollection("pass_season").updateOne(new Document("name", this.season.getName()), new Document("$set", new Document("quests", list)));
            });
        }
    }

    public void removeQuestFromSeason(final String questId) {
        if (this.season != null) {
            this.season.getQuests().remove(questId);

            CompletableFuture.runAsync(() -> {
                final Document document = this.getCollection("pass_season").find(new Document("name", this.season.getName())).first();
                assert document != null;
                final List<String> list = document.getList("quests", String.class);
                list.remove(questId);
                this.getCollection("pass_season").updateOne(new Document("name", this.season.getName()), new Document("$set", new Document("quests", list)));
            });
        }
    }

    public void addRewardToSeason(final String id, final int points, final String type, final String image, final String description, final String data) {
        if (this.season != null) {
            this.season.getRewards().put(id, new Season.SeasonReward(id, points, type, image, description, data));

            CompletableFuture.runAsync(() -> {
                final Document document = this.getCollection("pass_season").find(new Document("name", this.season.getName())).first();
                assert document != null;
                final List<String> list = document.getList("rewards", String.class);
                list.add(id + ";" + points + ";" + type + ";" + image + ";" + description + ";" + data);
                this.getCollection("pass_season").updateOne(new Document("name", this.season.getName()), new Document("$set", new Document("rewards", list)));
            });
        }
    }

    public void removeRewardFromSeason(final String id) {
        if (this.season != null) {
            this.season.getRewards().remove(id);

            CompletableFuture.runAsync(() -> {
                final Document document = this.getCollection("pass_season").find(new Document("name", this.season.getName())).first();
                assert document != null;
                final List<String> list = document.getList("rewards", String.class);
                list.removeIf(s -> s.startsWith(id));
                this.getCollection("pass_season").updateOne(new Document("name", this.season.getName()), new Document("$set", new Document("rewards", list)));
            });
        }
    }

    public void updateName(final String newName) {
        if (this.season != null) {
            this.getCollection("pass_season").updateOne(new Document("name", this.season.getName()), new Document("$set", new Document("name", newName)));
            this.season.setName(newName);
        }
    }

    public void updateDescription(final String newDescription) {
        if (this.season != null) {
            this.season.setDescription(newDescription);

            CompletableFuture.runAsync(() -> {
                this.getCollection("pass_season").updateOne(new Document("name", this.season.getName()), new Document("$set", new Document("description", newDescription)));
            });
        }
    }

    public void updateExpire(final long newExpire) {
        if (this.season != null) {
            this.season.setExpire(newExpire);

            CompletableFuture.runAsync(() -> {
                this.getCollection("pass_season").updateOne(new Document("name", this.season.getName()), new Document("$set", new Document("expire", newExpire)));
            });
        }
    }

    public void toggleActive(final boolean active) {
        if (this.season != null) {
            this.season.setActive(active);

            CompletableFuture.runAsync(() -> {
                this.getCollection("pass_season").updateOne(new Document("name", this.season.getName()), new Document("$set", new Document("isActive", active)));
            });
        }
    }

    public void updateReward(final String id, final int points, final String type, final String image, final String description, final String data) {
        if (this.season != null) {
            final Season.SeasonReward reward = this.season.getRewards().get(id);
            reward.setPoints(points);
            reward.setType(type);
            reward.setImage(image);
            reward.setDescription(description);
            reward.setData(data);
            this.season.getRewards().remove(id);
            this.season.getRewards().put(id, reward);

            CompletableFuture.runAsync(() -> {
                final Document document = this.getCollection("pass_season").find(new Document("name", this.season.getName())).first();
                assert document != null;
                final List<String> list = document.getList("rewards", String.class);
                list.removeIf(s -> s.startsWith(id));
                list.add(id + ";" + points + ";" + type + ";" + image + ";" + description + ";" + data);
                this.getCollection("pass_season").updateOne(new Document("name", this.season.getName()), new Document("$set", new Document("rewards", list)));
            });
        }
    }

}
