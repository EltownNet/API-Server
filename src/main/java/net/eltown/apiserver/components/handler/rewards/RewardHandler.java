package net.eltown.apiserver.components.handler.rewards;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Handler;
import net.eltown.apiserver.components.handler.rewards.data.DailyReward;
import net.eltown.apiserver.components.handler.rewards.data.RewardPlayer;

import java.util.List;

public class RewardHandler extends Handler<RewardProvider> {

    @SneakyThrows
    public RewardHandler(final Server server) {
        super(server, "RewardHandler", new RewardProvider(server));
        this.startCallbacking();
    }

    public void startCallbacking() {
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().receive(delivery -> {
                final String[] d = delivery.getData();
                switch (RewardCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case REQUEST_ADD_REWARD -> this.getProvider().createDailyReward(d[1], Integer.parseInt(d[2]), Integer.parseInt(d[3]), d[4]);
                    case REQUEST_REMOVE_REWARD -> this.getProvider().removeDailyReward(d[1]);
                    case REQUEST_UPDATE_DAILY_REWARD -> {
                        final DailyReward dailyReward = new DailyReward(d[1], d[2], Integer.parseInt(d[3]), Integer.parseInt(d[4]), d[5]);
                        this.getProvider().updateDailyReward(dailyReward);
                    }
                    case REQUEST_ADD_STREAK -> this.getProvider().addStreak(d[1]);
                    case REQUEST_RESET_STREAK -> this.getProvider().resetStreak(d[1]);
                }
            }, "API/Rewards[Receive]", "api.rewards.receive");
        });

        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().callback((request -> {
                final String[] d = request.getData();
                switch (RewardCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_PLAYER_DATA -> {
                        if (!this.getProvider().playerAccountExists(d[1])) {
                            this.getProvider().createPlayerAccount(d[1]);
                        }
                        final RewardPlayer rewardPlayer = this.getProvider().rewardPlayers.get(d[1]);
                        request.answer(RewardCalls.CALLBACK_PLAYER_DATA.name(), rewardPlayer.getPlayer(), String.valueOf(rewardPlayer.getDay()), String.valueOf(rewardPlayer.getLastReward()));
                    }
                    case REQUEST_REWARDS -> {
                        final List<DailyReward> rewards = this.getProvider().getRewardsByDay(Integer.parseInt(d[1]));
                        if (rewards == null || rewards.isEmpty()) {
                            request.answer(RewardCalls.CALLBACK_REWARDS.name(), "null");
                            return;
                        }
                        final StringBuilder builder = new StringBuilder();
                        rewards.forEach(e -> {
                            builder.append(e.getDescription()).append(">:<").append(e.getId()).append(">:<").append(e.getDay()).append(">:<").append(e.getChance()).append(">:<").append(e.getData()).append("-:-");
                        });
                        final String rewardString = builder.substring(0, builder.length() - 3);
                        request.answer(RewardCalls.CALLBACK_REWARDS.name(), rewardString);
                    }
                }
            }), "API/Rewards[Callback]", "api.rewards.callback");
        });
    }

}
