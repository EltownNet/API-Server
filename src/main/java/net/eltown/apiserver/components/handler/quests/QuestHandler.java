package net.eltown.apiserver.components.handler.quests;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Handler;
import net.eltown.apiserver.components.handler.quests.data.Quest;
import net.eltown.apiserver.components.handler.quests.data.QuestPlayer;

public class QuestHandler extends Handler<QuestProvider> {

    @SneakyThrows
    public QuestHandler(final Server server) {
        super(server, "QuestHandler", new QuestProvider(server));
        this.startCallbacking();
    }

    public void startCallbacking() {
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().receive(delivery -> {
                final String[] d = delivery.getData();
                switch (QuestCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case REQUEST_CREATE_QUEST -> this.getProvider().createQuest(d[1], d[2], d[3], d[4], Integer.parseInt(d[5]), Long.parseLong(d[6]), d[7], d[8]);
                    case REQUEST_REMOVE_QUEST -> this.getProvider().removeQuest(d[1]);
                    case REQUEST_SET_PLAYER_QUEST -> this.getProvider().setQuestOnPlayer(d[1], d[2]);
                    case REQUEST_REMOVE_PLAYER_QUEST -> this.getProvider().removeQuestFromPlayer(d[1], d[2]);
                    case REQUEST_UPDATE_QUEST -> this.getProvider().updateQuest(d[1], d[2], d[3], d[4], Integer.parseInt(d[5]), Long.parseLong(d[6]), d[7], d[8]);
                    case REQUEST_UPDATE_PLAYER_DATA -> this.getProvider().updateQuestPlayerProgress(d[1], d[2], Integer.parseInt(d[3]));
                }
            }, "API/Quests[Receive]", "api.quests.receive");
        });

        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().callback((request -> {
                final String[] d = request.getData();
                switch (QuestCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_PLAYER_DATA:
                        try {
                            final QuestPlayer questPlayer = this.getProvider().cachedQuestPlayer.get(d[1]);
                            if (questPlayer != null) {
                                final StringBuilder builder = new StringBuilder();

                                if (!questPlayer.getQuestPlayerData().isEmpty() || questPlayer.getQuestPlayerData() != null) {
                                    questPlayer.getQuestPlayerData().forEach(e -> {
                                        builder.append(e.getQuestNameId()).append("-:-").append(e.getExpire()).append("-:-").append(e.getRequired()).append("-:-").append(e.getCurrent()).append("-#-");
                                    });
                                    String questPlayerData = "null";
                                    if (builder.length() != 0) {
                                        questPlayerData = builder.substring(0, builder.length() - 3);
                                    }
                                    request.answer(QuestCalls.CALLBACK_PLAYER_DATA.name(), questPlayerData);
                                } else request.answer(QuestCalls.CALLBACK_PLAYER_DATA.name(), "null");
                            } else {
                                this.getProvider().createPlayer(d[1]);
                                request.answer(QuestCalls.CALLBACK_NULL.name(), "null");
                            }
                        } catch (final Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case REQUEST_PLAYER_EXISTS:
                        if (this.getProvider().cachedQuestPlayer.containsKey(d[1])) {
                            request.answer(QuestCalls.CALLBACK_PLAYER_EXISTS.name(), "null");
                        } else request.answer(QuestCalls.CALLBACK_NULL.name(), "null");
                        break;
                    case REQUEST_QUEST_DATA:
                        final Quest quest = this.getProvider().cachedQuests.get(d[1]);
                        if (quest != null) {
                            request.answer(QuestCalls.CALLBACK_QUEST_DATA.name(), quest.getNameId(), quest.getDisplayName(), quest.getDescription(), quest.getData(), String.valueOf(quest.getRequired()), String.valueOf(quest.getExpire()), quest.getRewardData(), quest.getLink());
                        } else request.answer(QuestCalls.CALLBACK_NULL.name(), "null");
                        break;
                    case REQUEST_RANDOM_QUEST_DATA_BY_LINK:
                        final Quest randomLinkQuest = this.getProvider().getRandomQuestByLink(d[1]);
                        if (randomLinkQuest != null) {
                            request.answer(QuestCalls.CALLBACK_RANDOM_QUEST_DATA_BY_LINK.name(), randomLinkQuest.getNameId(), randomLinkQuest.getDisplayName(), randomLinkQuest.getDescription(), randomLinkQuest.getData(), String.valueOf(randomLinkQuest.getRequired()), String.valueOf(randomLinkQuest.getExpire()), randomLinkQuest.getRewardData(), randomLinkQuest.getLink());
                        } else request.answer(QuestCalls.CALLBACK_NULL.name(), "null");
                        break;
                }
            }), "API/Quests[Callback]", "api.quests.callback");
        });
    }

}