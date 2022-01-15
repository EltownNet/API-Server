package net.eltown.apiserver.components.handler.teleportation;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Handler;
import net.eltown.apiserver.components.handler.teleportation.data.CachedTeleport;
import net.eltown.apiserver.components.handler.teleportation.data.Home;
import net.eltown.apiserver.components.handler.teleportation.data.Warp;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

public class TeleportationHandler extends Handler<TeleportationProvider> {

    @SneakyThrows
    public TeleportationHandler(final Server server) {
        super(server, "TeleportationHandler", new TeleportationProvider(server));
        this.startCallbacking();
    }

    public void startCallbacking() {
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().receive(delivery -> {
                final String[] d = delivery.getData();
                switch (TeleportationCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case REQUEST_DELETE_HOME -> this.getProvider().deleteHome(delivery.getData()[1], delivery.getData()[2]);
                    case REQUEST_RENAME_HOME -> this.getProvider().updateHomeName(delivery.getData()[1], delivery.getData()[2], delivery.getData()[3]);
                    case REQUEST_UPDATE_POSITION -> this.getProvider().updateHomePosition(delivery.getData()[1], delivery.getData()[2], delivery.getData()[3], delivery.getData()[4], Double.parseDouble(delivery.getData()[5]),
                            Double.parseDouble(delivery.getData()[6]), Double.parseDouble(delivery.getData()[7]), Double.parseDouble(delivery.getData()[8]), Double.parseDouble(delivery.getData()[9]));
                    case REQUEST_DELETE_ALL_SERVER_HOMES -> this.getProvider().deleteServerHomes(delivery.getData()[1]);
                    case REQUEST_DELETE_WARP -> this.getProvider().deleteWarp(delivery.getData()[1]);
                    case REQUEST_RENAME_WARP -> this.getProvider().updateWarpName(delivery.getData()[1], delivery.getData()[2]);
                    case REQUEST_UPDATE_WARP_IMAGE -> this.getProvider().updateWarpImage(delivery.getData()[1], delivery.getData()[2]);
                    case REQUEST_UPDATE_WARP_POSITION -> this.getProvider().updateWarpPosition(delivery.getData()[1], delivery.getData()[2], delivery.getData()[3], Double.parseDouble(delivery.getData()[4]),
                            Double.parseDouble(delivery.getData()[5]), Double.parseDouble(delivery.getData()[6]), Double.parseDouble(delivery.getData()[7]), Double.parseDouble(delivery.getData()[8]));
                    case REQUEST_TELEPORT_HOME, REQUEST_TELEPORT_WARP -> {
                        this.getProvider().cachedTeleports.put(d[3], new CachedTeleport(d[1], d[2], d[3], d[4], d[5], Double.parseDouble(d[6]), Double.parseDouble(d[7]), Double.parseDouble(d[8]), Double.parseDouble(d[9]), Double.parseDouble(d[10])));
                        this.getProvider().getTinyRabbitClient().send("core.proxy.teleportation.receive", TeleportationCalls.REQUEST_TELEPORT_SERVER.name(), d[3], d[4]);
                    }
                    case REQUEST_TELEPORT -> {
                        this.getProvider().cachedTeleports.put(d[2], new CachedTeleport(d[1], d[3], d[2]));
                        this.getProvider().getTinyRabbitClient().send("core.proxy.teleportation.receive", TeleportationCalls.REQUEST_TELEPORT_PLAYER.name(), d[3], d[4]);
                    }
                    case REQUEST_ACCEPT_TPA -> {
                        this.getProvider().removeTpa(delivery.getData()[1], delivery.getData()[2]);
                        this.getProvider().getTinyRabbitClient().send("core.proxy.teleportation.receive", TeleportationCalls.REQUEST_TELEPORT_PLAYER.name(), delivery.getData()[1], delivery.getData()[2]);
                    }
                    case REQUEST_DENY_TPA -> this.getProvider().removeTpa(delivery.getData()[1], delivery.getData()[2]);
                }
            }, "API/Teleportation[Receive]", "api.teleportation.receive");
        });
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().callback((request -> {
                switch (TeleportationCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_ALL_HOMES:
                        final Set<Home> homes = this.getProvider().getHomes(request.getData()[1]);
                        if (homes.size() == 0) {
                            request.answer(TeleportationCalls.CALLBACK_NULL.name(), "null");
                        } else {
                            final LinkedList<String> list = new LinkedList<>();
                            for (final Home home : homes) {
                                list.add(home.getName() + ">>" + home.getPlayer() + ">>" + home.getServer() + ">>" + home.getWorld() + ">>" + home.getX() + ">>" + home.getY() + ">>" + home.getZ() + ">>" + home.getYaw() + ">>" + home.getPitch());
                            }
                            request.answer(TeleportationCalls.CALLBACK_ALL_HOMES.name(), list.toArray(new String[0]));
                        }
                        break;
                    case REQUEST_ADD_HOME:
                        if (!this.getProvider().homeExists(request.getData()[1], request.getData()[2])) {
                            this.getProvider().createHome(request.getData()[1], request.getData()[2], request.getData()[3], request.getData()[4], Double.parseDouble(request.getData()[5]),
                                    Double.parseDouble(request.getData()[6]), Double.parseDouble(request.getData()[7]), Double.parseDouble(request.getData()[8]), Double.parseDouble(request.getData()[9]));
                            request.answer(TeleportationCalls.CALLBACK_NULL.name(), "null");
                        } else request.answer(TeleportationCalls.CALLBACK_HOME_ALREADY_SET.name(), "null");
                        break;
                    case REQUEST_ALL_WARPS:
                        final Collection<Warp> warps = this.getProvider().warps.values();
                        if (warps.size() == 0) {
                            request.answer(TeleportationCalls.CALLBACK_NULL.name(), "null");
                        } else {
                            final LinkedList<String> list = new LinkedList<>();
                            for (final Warp warp : warps) {
                                list.add(warp.getName() + ">>" + warp.getDisplayName() + ">>" + warp.getImageUrl() + ">>" + warp.getServer() + ">>" + warp.getWorld() + ">>" + warp.getX() + ">>" + warp.getY() + ">>" + warp.getZ() + ">>" + warp.getYaw() + ">>" + warp.getPitch());
                            }
                            request.answer(TeleportationCalls.CALLBACK_ALL_WARPS.name(), list.toArray(new String[0]));
                        }
                        break;
                    case REQUEST_ADD_WARP:
                        if (!this.getProvider().warpExists(request.getData()[1])) {
                            this.getProvider().createWarp(request.getData()[1], request.getData()[2], request.getData()[3], request.getData()[4], request.getData()[5], Double.parseDouble(request.getData()[6]),
                                    Double.parseDouble(request.getData()[7]), Double.parseDouble(request.getData()[8]), Double.parseDouble(request.getData()[9]), Double.parseDouble(request.getData()[10]));
                            request.answer(TeleportationCalls.CALLBACK_NULL.name(), "null");
                        } else request.answer(TeleportationCalls.CALLBACK_WARP_ALREADY_SET.name(), "null");
                        break;
                    case REQUEST_CACHED_DATA:
                        if (this.getProvider().cachedTeleports.containsKey(request.getData()[1])) {
                            final CachedTeleport cachedTeleport = this.getProvider().cachedTeleports.get(request.getData()[1]);
                            if (cachedTeleport.getType().equals("home") || cachedTeleport.getType().equals("warp")) {
                                request.answer(TeleportationCalls.CALLBACK_CACHED_DATA.name(), cachedTeleport.getType(), cachedTeleport.getObject(), cachedTeleport.getWorld(), String.valueOf(cachedTeleport.getX()), String.valueOf(cachedTeleport.getY()), String.valueOf(cachedTeleport.getZ()),
                                        String.valueOf(cachedTeleport.getYaw()), String.valueOf(cachedTeleport.getPitch()));
                            } else if (cachedTeleport.getType().equals("tpa") || cachedTeleport.getType().equals("teleport")) {
                                request.answer(TeleportationCalls.CALLBACK_CACHED_DATA.name(), cachedTeleport.getType(), cachedTeleport.getObject());
                            }
                            this.getProvider().cachedTeleports.remove(request.getData()[1]);
                        } else request.answer(TeleportationCalls.CALLBACK_NULL.name(), "null");
                        break;
                    case REQUEST_SEND_TPA:
                        try {
                            if (!this.getProvider().hasTpaSent(request.getData()[2], request.getData()[1])) {
                                this.getProvider().createTpa(request.getData()[1], request.getData()[2]);
                                request.answer(TeleportationCalls.CALLBACK_NULL.name(), "null");

                                this.getProvider().getTinyRabbitClient().send("core.proxy.teleportation.receive", TeleportationCalls.REQUEST_TPA_NOTIFICATION.name(), request.getData()[1], request.getData()[2]);
                            } else request.answer(TeleportationCalls.CALLBACK_TPA_ALREADY_SENT.name(), "null");
                        } catch (final Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case REQUEST_TPAS:
                        try {
                            final Collection<String> tpas = this.getProvider().getTpas(request.getData()[1]);
                            if (tpas.size() == 0) {
                                request.answer(TeleportationCalls.CALLBACK_NULL.name(), "null");
                            } else {
                                request.answer(TeleportationCalls.CALLBACK_TPAS.name(), tpas.toArray(new String[0]));
                            }
                        } catch (final Exception e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }), "API/Teleportation[Callback]", "api.teleportation.callback");
        });
    }

}

