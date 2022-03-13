package net.eltown.apiserver.components.handler.passes;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Handler;
import net.eltown.apiserver.components.handler.passes.data.Season;

public class PassHandler extends Handler<PassProvider> {

    @SneakyThrows
    public PassHandler(final Server server) {
        super(server, "PassHandler", new PassProvider(server));
        this.startCallbacking();
    }

    private void startCallbacking() {
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().receive(delivery -> {
                final String[] d = delivery.getData();
                switch (PassCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case REQUEST_CREATE_SEASON -> this.getProvider().createSeason(d[1], d[2], Long.parseLong(d[3]));
                    case REQUEST_ADD_QUEST -> this.getProvider().addQuestToSeason(d[1]);
                    case REQUEST_REMOVE_QUEST -> this.getProvider().removeQuestFromSeason(d[1]);
                    case REQUEST_ADD_REWARD -> this.getProvider().addRewardToSeason(d[1], Integer.parseInt(d[2]), d[3], d[4], d[5], d[6]);
                    case REQUEST_REMOVE_REWARD -> this.getProvider().removeRewardFromSeason(d[1]);
                    case REQUEST_UPDATE_SEASON_NAME -> this.getProvider().updateName(d[1]);
                    case REQUEST_UPDATE_SEASON_DESCRIPTION -> this.getProvider().updateDescription(d[1]);
                    case REQUEST_UPDATE_SEASON_EXPIRE -> this.getProvider().updateExpire(Long.parseLong(d[1]));
                    case REQUEST_TOGGLE_SEASON_ACTIVE -> this.getProvider().toggleActive(Boolean.parseBoolean(d[1]));
                    case REQUEST_UPDATE_REWARD -> this.getProvider().updateReward(d[1], Integer.parseInt(d[2]), d[3], d[4], d[5], d[6]);
                    case REQUEST_DELETE_SEASON -> this.getProvider().deleteSeason();
                }
            }, "API/Passes[Receive]", "api.passes.receive");
        });
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().callback(request -> {
                final String[] d = request.getData();
                switch (PassCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_GET_CURRENT_SEASON -> {
                        final Season season = this.getProvider().season;
                        if (season == null) {
                            request.answer(PassCalls.CALLBACK_NULL.name(), "null");
                            return;
                        }
                        String keyBack = PassCalls.CALLBACK_GET_CURRENT_SEASON.name();
                        if (!season.isActive()) {
                            keyBack = PassCalls.CALLBACK_SEASON_NOT_ACTIVE.name();
                        }

                        final String name = season.getName();
                        final String description = season.getDescription();
                        final String quests = season.getQuests().isEmpty() ? "null" : String.join("#", season.getQuests());
                        final StringBuilder rewards = new StringBuilder();
                        season.getRewards().values().forEach(seasonReward -> {
                            rewards.append(seasonReward.getId()).append(";").append(seasonReward.getPoints()).append(";")
                                    .append(seasonReward.getType()).append(";").append(seasonReward.getImage()).append(";")
                                    .append(seasonReward.getDescription()).append(";").append(seasonReward.getData()).append(";-;");
                        });
                        if (rewards.isEmpty()) rewards.append("null;-;");
                        final long expire = season.getExpire();
                        final boolean isActive = season.isActive();

                        request.answer(keyBack, name, description, quests, rewards.substring(0, rewards.length() - 1), String.valueOf(expire), String.valueOf(isActive));
                    }
                }
            }, "API/Passes[Callback]", "api.passes.callback");
        });
    }

}
