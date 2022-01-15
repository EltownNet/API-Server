package net.eltown.apiserver.components.handler.crates;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Handler;
import net.eltown.apiserver.components.handler.crates.data.CrateReward;

public class CratesHandler extends Handler<CratesProvider> {


    @SneakyThrows
    public CratesHandler(final Server server) {
        super(server, "CratesHandler", new CratesProvider(server));
        this.startCallbacking();
    }

    private void startCallbacking() {
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().receive(delivery -> {
                final String[] d = delivery.getData();
                switch (CratesCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case REQUEST_ADD_CRATE -> this.getProvider().addCrate(d[1], d[2], Integer.parseInt(d[3]));
                    case REQUEST_REMOVE_CRATE -> this.getProvider().removeCrate(d[1], d[2], Integer.parseInt(d[3]));
                    case REQUEST_DELETE_REWARD -> this.getProvider().deleteCrateReward(d[1]);
                    case REQUEST_UPDATE_REWARD -> this.getProvider().updateCrateReward(d[1], d[2], d[3], Integer.parseInt(d[4]), d[5]);
                    case REQUEST_INSERT_REWARD_DATA -> this.getProvider().insertCrateReward(d[1], d[2], d[3], Integer.parseInt(d[4]), d[5]);
                    case REQUEST_CREATE_PLAYER_DATA -> this.getProvider().createPlayerData(d[1]);
                }
            }, "API/Crates[Receive]", "api.crates.receive");
        });
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().callback(request -> {
                final String[] d = request.getData();
                switch (CratesCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_GET_REWARD_DATA -> {
                        final CrateReward crateReward = this.getProvider().cachedCrateRewards.get(d[1]);
                        if (crateReward != null) {
                            request.answer(CratesCalls.CALLBACK_GET_REWARD_DATA.name(), crateReward.getId(), crateReward.getCrate(), crateReward.getDisplayName(), String.valueOf(crateReward.getChance()), crateReward.getData());
                        } else request.answer(CratesCalls.CALLBACK_NULL.name(), "null");
                    }
                    case REQUEST_GET_CRATE_REWARDS -> {
                        final StringBuilder builder = new StringBuilder();
                        this.getProvider().getCrateRewards(d[1]).forEach(e -> {
                            builder.append(e.getId()).append(">:<").append(e.getCrate()).append(">:<").append(e.getDisplayName()).append(">:<").append(e.getChance()).append(">:<").append(e.getData()).append(">#<");
                        });
                        if (builder.length() == 0) builder.append("null>#<");
                        request.answer(CratesCalls.CALLBACK_GET_CRATE_REWARDS.name(), builder.substring(0, builder.length() - 3));
                    }
                    case REQUEST_PLAYER_DATA -> {
                        final StringBuilder builder1 = new StringBuilder();
                        this.getProvider().cachedCratePlayers.get(d[1]).getData().forEach((k, v) -> {
                            builder1.append(k).append(":").append(v).append("#");
                        });
                        if (builder1.length() == 0) builder1.append("null#");
                        request.answer(CratesCalls.CALLBACK_PLAYER_DATA.name(), builder1.substring(0, builder1.length() - 1));
                    }
                }
            }, "API/Crates[Callback]", "api.crates.callback");
        });
    }

}
