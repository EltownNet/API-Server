package net.eltown.apiserver.components.handler.player;

import com.rabbitmq.client.Connection;
import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Handler;
import net.eltown.apiserver.components.handler.player.data.SyncPlayer;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

import java.util.ArrayList;
import java.util.List;

public class PlayerHandler extends Handler<PlayerProvider> {


    @SneakyThrows
    public PlayerHandler(final Server server) {
        super(server, "PlayerHandler", new PlayerProvider(server));

        this.startCallbacking();
        this.startReceiving();
    }

    public void startReceiving() {
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().receive((delivery -> {
                final String[] request = delivery.getData();

                switch (PlayerCalls.valueOf(delivery.getKey())) {
                    case REQUEST_SETSYNC -> {
                        SyncPlayer set = new SyncPlayer(
                                request[2],
                                request[3],
                                request[4],
                                request[5],
                                request[6],
                                request[7],
                                request[8],
                                request[9],
                                request[10],
                                request[11],
                                request[12],
                                request[13],
                                request[14],
                                true
                        );
                        this.getProvider().set(request[1], set);
                    }
                    case REQUEST_SETNOSYNC -> {
                        SyncPlayer player = this.getProvider().get(request[1]);
                        player.setCanSync(false);
                        this.getProvider().set(request[1], player);
                    }
                }

            }), "API/Sync[Receive]", "api.sync.receive");
        });
    }

    public void startCallbacking() {
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().callback((request -> {
                switch (PlayerCalls.valueOf(request.getKey())) {
                    case REQUEST_SYNC:
                        final SyncPlayer syncPlayer = this.getProvider().get(request.getData()[1]);
                        if (syncPlayer.isCanSync()) {
                            request.answer(PlayerCalls.GOT_SYNC.name(), syncPlayer.getInventory(), syncPlayer.getArmorInventory(), syncPlayer.getEnderchest(), syncPlayer.getFoodLevel(), syncPlayer.getSaturation(), syncPlayer.getExhaustion(), syncPlayer.getSelectedSlot(), syncPlayer.getPotionEffects(), syncPlayer.getTotalExperience(), syncPlayer.getLevel(), syncPlayer.getExperience(), syncPlayer.getGamemode(), syncPlayer.getFlying());
                        } else {
                            request.answer(PlayerCalls.GOT_NOSYNC.name(), request.getData()[1]);
                        }
                        break;
                }
            }), "API/Sync[Callback]", "api.sync.callback");
        });
    }

}
