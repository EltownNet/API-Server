package net.eltown.apiserver.components.handler.level;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Handler;
import net.eltown.apiserver.components.handler.level.data.Level;
import net.eltown.apiserver.components.handler.level.data.LevelReward;

public class LevelHandler extends Handler<LevelProvider> {


    @SneakyThrows
    public LevelHandler(final Server server) {
        super(server, "LevelHandler", new LevelProvider(server));
        this.startCallbacking();
    }

    private void startCallbacking() {
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().receive(delivery -> {
                switch (LevelCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case REQUEST_UPDATE_TO_DATABASE -> {
                        final Level level = this.getProvider().getLevelData(delivery.getData()[1]);
                        final Level aLevel = new Level(delivery.getData()[1], Integer.parseInt(delivery.getData()[2]), Double.parseDouble(delivery.getData()[3]));
                        if (aLevel.getExperience() > level.getExperience()) {
                            this.getProvider().updateToDatabase(aLevel);
                        }
                    }
                    case REQUEST_UPDATE_REWARD -> {
                        final int l = Integer.parseInt(delivery.getData()[1]);
                        if (this.getProvider().cachedRewardData.containsKey(l)) {
                            this.getProvider().updateReward(l, delivery.getData()[2], delivery.getData()[3]);
                        } else this.getProvider().insertReward(l, delivery.getData()[2], delivery.getData()[3]);
                    }
                    case REQUEST_REMOVE_REWARD -> this.getProvider().removeReward(Integer.parseInt(delivery.getData()[1]));
                }
            }, "API/Level[Receive]", "api.level.receive");
        });
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().callback(request -> {
                switch (LevelCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_GET_LEVEL -> {
                        final Level targetLevel = this.getProvider().getLevelData(request.getData()[1]);
                        request.answer(LevelCalls.CALLBACK_LEVEL.name(), targetLevel.getPlayer(), String.valueOf(targetLevel.getLevel()), String.valueOf(targetLevel.getExperience()));
                    }
                    case REQUEST_LEVEL_REWARD -> {
                        final int level = Integer.parseInt(request.getData()[1]);
                        if (this.getProvider().cachedRewardData.containsKey(level)) {
                            final LevelReward levelReward = this.getProvider().cachedRewardData.get(level);
                            request.answer(LevelCalls.CALLBACK_LEVEL_REWARD.name(), String.valueOf(levelReward.getId()), levelReward.getDescription(), levelReward.getData());
                        } else request.answer(LevelCalls.CALLBACK_NULL.name(), "null");
                    }
                }
            }, "API/Level[Callback]", "api.level.callback");
        });
    }
}
