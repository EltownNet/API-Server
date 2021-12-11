package net.eltown.apiserver.components.handler.player;

import com.rabbitmq.client.Connection;
import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.handler.player.data.SyncPlayer;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

import java.util.ArrayList;
import java.util.List;

public class PlayerHandler {

    private final TinyRabbitListener listener;
    private final Server server;
    private final PlayerProvider provider;
    private final List<String> isSynced = new ArrayList<>();

    @SneakyThrows
    public PlayerHandler(final Server server, final Connection connection) {
        this.server = server;
        this.provider = new PlayerProvider(server);
        this.listener = new TinyRabbitListener("localhost");
        this.listener.throwExceptions(true);

        this.startCallbacking();
        this.startReceiving();
    }

    public void startReceiving() {
        this.server.getExecutor().execute(() -> {
            this.listener.receive((delivery -> {
                final String[] request = delivery.getData();

                switch (PlayerCalls.valueOf(delivery.getKey())) {
                    case REQUEST_SETSYNC:
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
                        this.provider.set(request[1], set);
                        this.isSynced.remove(request[1]);
                        break;
                    case REQUEST_SETNOSYNC:
                        SyncPlayer player = this.provider.get(request[1]);
                        player.setCanSync(false);
                        this.provider.set(request[1], player);
                        break;
                }

            }), "API/Sync[Receive]", "api.sync.receive");
        });
    }

    public void startCallbacking() {
        this.server.getExecutor().execute(() -> {
            this.listener.callback((request -> {
                switch (PlayerCalls.valueOf(request.getKey())) {
                    case REQUEST_SYNC:
                        final SyncPlayer syncPlayer = this.provider.get(request.getData()[1]);
                        if (syncPlayer.isCanSync() && !isSynced.contains(request.getData()[1])) {
                            this.isSynced.add(request.getData()[1]);
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
