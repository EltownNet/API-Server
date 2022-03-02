package net.eltown.apiserver.components.handler.player;

import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Handler;

import java.util.UUID;

public class PlayerHandler extends Handler<PlayerProvider> {

    public PlayerHandler(final Server server) {
        super(server, "PlayerHandler", new PlayerProvider(server));
        this.startCallbacking();
    }

    private void startCallbacking() {
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().receive(delivery -> {
                switch (PlayerCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case REUEST_CREATE_PLAYER -> this.getProvider().createPlayerData(UUID.fromString(delivery.getData()[1]), delivery.getData()[2]);
                    case REQUEST_UPDATE_LAST_LOGIN -> this.getProvider().setLastLogin(UUID.fromString(delivery.getData()[1]));
                    case REQUEST_UPDATE_NAME -> this.getProvider().updateName(UUID.fromString(delivery.getData()[1]), delivery.getData()[2]);
                }
            }, "API/Player[Receive]", "api.player.receive");
        });
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().callback(request -> {
                final String[] d = request.getData();
                switch (PlayerCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_GET_PLAYER -> {
                        final String data = this.getProvider().getPlayer(UUID.fromString(d[1]));
                        if (data.equals("null")) request.answer(PlayerCalls.CALLBACK_NULL.name(), "null");
                        else request.answer(PlayerCalls.CALLBACK_GET_PLAYER.name(), data);
                    }
                }
            }, "API/Player[Callback]", "api.player.callback");
        });
    }

}
