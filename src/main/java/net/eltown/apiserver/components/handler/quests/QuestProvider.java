package net.eltown.apiserver.components.handler.quests;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Provider;
import net.eltown.apiserver.components.handler.quests.data.Quest;
import net.eltown.apiserver.components.handler.quests.data.QuestPlayer;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

public class QuestProvider extends Provider {

    public final HashMap<String, Quest> cachedQuests = new HashMap<>();
    public final HashMap<String, QuestPlayer> cachedQuestPlayer = new HashMap<>();

    @SneakyThrows
    public QuestProvider(final Server server) {
        super(server, "a2_quests_quests", "a2_quests_data");

        server.log("Quests werden in den Cache geladen...");
        for (final Document document : this.getCollection("a2_quests_quests").find()) {
            this.cachedQuests.put(document.getString("_id"), new Quest(
                    document.getString("_id"),
                    document.getString("displayName"),
                    document.getList("data", String.class),
                    document.getLong("expire"),
                    document.getString("rewardData"),
                    document.getString("link")
            ));
        }
        server.log(this.cachedQuests.size() + " Quests wurden in den Cache geladen...");

        server.log("QuestDaten werden in den Cache geladen...");
        for (final Document document : this.getCollection("a2_quests_data").find()) {
            final List<String> rawData = document.getList("data", String.class);
            final List<QuestPlayer.QuestData> questPlayerData = new ArrayList<>();

            for (final String s : rawData) {
                final String[] sSplit = s.split("-:-");
                questPlayerData.add(new QuestPlayer.QuestData(sSplit[0], sSplit[1], sSplit[2], Integer.parseInt(sSplit[3]), Integer.parseInt(sSplit[4]), Long.parseLong(sSplit[5])));
            }

            this.cachedQuestPlayer.put(document.getString("_id"), new QuestPlayer(
                    document.getString("_id"),
                    questPlayerData
            ));
        }
        server.log(this.cachedQuestPlayer.size() + " QuestDaten wurden in den Cache geladen...");
    }

    public void createQuest(final String nameId, final String displayName, final List<String> data, final long expire, final String rewardData, final String link) {
        this.cachedQuests.put(nameId, new Quest(nameId, displayName, data, expire, rewardData, link));

        CompletableFuture.runAsync(() -> {
            this.getCollection("a2_quests_quests").insertOne(new Document("_id", nameId)
                    .append("displayName", displayName)
                    .append("data", data)
                    .append("expire", expire)
                    .append("rewardData", rewardData)
                    .append("link", link)
            );
        });
    }

    public void createSubQuest(final String nameId, final String subId, final String description, final String data, final int required) {
        final String dataEntry = nameId + "-:-" + subId + "-:-" + description + "-:-" + data + "-:-" + required;
        final List<String> dataEntries = new ArrayList<>(this.cachedQuests.get(nameId).getData());
        dataEntries.add(dataEntry);
        this.cachedQuests.get(nameId).setData(dataEntries);

        CompletableFuture.runAsync(() -> {
            final Document document = this.getCollection("a2_quests_quests").find(new Document("_id", nameId)).first();
            assert document != null;

            final List<String> questData = document.getList("data", String.class);
            questData.add(dataEntry);

            this.getCollection("a2_quests_quests").updateOne(new Document("_id", nameId), new Document("$set", new Document("data", questData)));
        });
    }

    public boolean questExists(final String nameId) {
        return this.cachedQuests.containsKey(nameId);
    }

    public void removeQuest(final String nameId) {
        this.cachedQuests.remove(nameId);

        CompletableFuture.runAsync(() -> {
            this.getCollection("a2_quests_quests").findOneAndDelete(new Document("_id", nameId));
        });
    }

    public void removeSubQuest(final String nameId, final String subId) {
        final List<String> list = new ArrayList<>(this.cachedQuests.get(nameId).getData());
        list.removeIf(s -> s.startsWith(nameId + "-:-" + subId));
        this.cachedQuests.get(nameId).setData(list);

        CompletableFuture.runAsync(() -> {
            /*final Document document = this.getCollection("a2_quests_quests").find(new Document("_id", nameId)).first();
            assert document != null;

            final List<String> questData = document.getList("data", String.class);
            questData.removeIf(s -> s.startsWith(nameId + "-:-" + subId));*/

            this.getCollection("a2_quests_quests").updateOne(new Document("_id", nameId), new Document("$set", new Document("data", list)));
        });
    }

    public void updateQuest(final String nameId, final String displayName, final List<String> data, final long expire, final String rewardData, final String link) {
        this.cachedQuests.remove(nameId);
        this.cachedQuests.put(nameId, new Quest(nameId, displayName, data, expire, rewardData, link));

        CompletableFuture.runAsync(() -> {
            this.getCollection("a2_quests_quests").updateOne(new Document("_id", nameId), new Document("$set", new Document("displayName", displayName).append("data", data).append("expire", expire).append("rewardData", rewardData).append("link", link)));
        });
    }

    public void updateSubQuest(final String questNameId, final String questSubId, final String description, final String data, final int required) {
        final String dataEntry = questNameId + "-:-" + questSubId + "-:-" + description + "-:-" + data + "-:-" + required;
        final List<String> setData = new ArrayList<>(this.cachedQuests.get(questNameId).getData());
        setData.removeIf(s -> s.startsWith(questNameId + "-:-" + questSubId));
        setData.add(dataEntry);
        this.cachedQuests.get(questNameId).setData(setData);

        CompletableFuture.runAsync(() -> {
            /*final Document document = this.getCollection("a2_quests_quests").find(new Document("_id", questNameId)).first();
            assert document != null;

            final List<String> questData = document.getList("data", String.class);
            questData.removeIf(s -> s.startsWith(questNameId + "-:-" + questSubId));
            questData.add(dataEntry);*/

            this.getCollection("a2_quests_quests").updateOne(new Document("_id", questNameId), new Document("$set", new Document("data", setData)));
        });
    }

    public void createPlayer(final String player) {
        this.cachedQuestPlayer.put(player, new QuestPlayer(player, new ArrayList<>()));

        CompletableFuture.runAsync(() -> {
            this.getCollection("a2_quests_data").insertOne(new Document("_id", player).append("data", new ArrayList<String>()));
        });
    }

    public boolean playerExists(final String player) {
        return this.cachedQuestPlayer.containsKey(player);
    }

    public void setQuestOnPlayer(final String player, final String questNameId) {
        final Quest quest = this.cachedQuests.get(questNameId);

        final List<QuestPlayer.QuestData> playerData = new ArrayList<>(this.cachedQuestPlayer.get(player).getQuestPlayerData());
        quest.getData().forEach(data -> {
            final String[] splitData = data.split("-:-");
            playerData.add(new QuestPlayer.QuestData(splitData[0], splitData[1], splitData[3], 0, Integer.parseInt(splitData[4]), (System.currentTimeMillis() + quest.getExpire())));
        });
        this.cachedQuestPlayer.get(player).setQuestPlayerData(playerData);

        CompletableFuture.runAsync(() -> {
            final Document document = this.getCollection("a2_quests_data").find(new Document("_id", player)).first();
            assert document != null;

            final List<String> list = document.getList("data", String.class);
            quest.getData().forEach(data -> {
                final String[] splitData = data.split("-:-");
                list.add(splitData[0] + "-:-" + splitData[1] + "-:-" + splitData[3] + "-:-0-:-" + Integer.parseInt(splitData[4]) + "-:-" + (System.currentTimeMillis() + quest.getExpire()));
            });

            this.getCollection("a2_quests_data").updateOne(new Document("_id", player), new Document("$set", new Document("data", list)));
        });
    }

    public void updatePlayerData(final String player, final List<String> data) {
        final List<QuestPlayer.QuestData> dataSet = new ArrayList<>();
        data.forEach(e -> {
            final String[] eSplit = e.split("-:-");
            dataSet.add(new QuestPlayer.QuestData(eSplit[0], eSplit[1], eSplit[2], Integer.parseInt(eSplit[3]), Integer.parseInt(eSplit[4]), Long.parseLong(eSplit[5])));
        });
        this.cachedQuestPlayer.get(player).setQuestPlayerData(dataSet);

        CompletableFuture.runAsync(() -> {
            this.getCollection("a2_quests_data").updateOne(new Document("_id", player), new Document("$set", new Document("data", data)));
        });
    }

    public void removeQuestFromPlayer(final String player, final String questNameId) {
        final List<QuestPlayer.QuestData> playerData = new ArrayList<>(this.cachedQuestPlayer.get(player).getQuestPlayerData());
        playerData.removeIf(s -> s.getQuestNameId().equals(questNameId));
        this.cachedQuestPlayer.get(player).setQuestPlayerData(playerData);

        CompletableFuture.runAsync(() -> {
            final Document document = this.getCollection("a2_quests_data").find(new Document("_id", player)).first();
            assert document != null;
            final List<String> list = document.getList("data", String.class);
            list.removeIf(s -> s.startsWith(questNameId));

            this.getCollection("a2_quests_data").updateOne(new Document("_id", player), new Document("$set", new Document("data", list)));
        });
    }

    private QuestPlayer.QuestData getQuestPlayerDataFromQuestId(final String player, final String questNameId, final String questSubId) {
        final AtomicReference<QuestPlayer.QuestData> questPlayerData = new AtomicReference<>();

        if (this.cachedQuestPlayer.get(player) == null || this.cachedQuestPlayer.get(player).getQuestPlayerData().isEmpty()) return null;

        this.cachedQuestPlayer.get(player).getQuestPlayerData().forEach(e -> {
            if (e.getQuestNameId().equals(questNameId) && e.getQuestSubId().equals(questSubId)) questPlayerData.set(e);
        });

        return questPlayerData.get();
    }

    public Quest getRandomQuestByLink(final String link) {
        final List<Quest> list = new ArrayList<>();
        this.cachedQuests.values().forEach(e -> {
            if (e.getLink().equals(link)) list.add(e);
        });

        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    public void checkIfQuestIsExpired(final String player) {
        final List<String> list = new ArrayList<>();
        if (!this.cachedQuestPlayer.get(player).getQuestPlayerData().isEmpty()) {
            this.cachedQuestPlayer.get(player).getQuestPlayerData().forEach(e -> {
                if (e.getExpire() < System.currentTimeMillis()) list.add(e.getQuestNameId());
            });
        }

        if (!list.isEmpty()) {
            list.forEach(e -> this.removeQuestFromPlayer(player, e));
        }
    }

}
