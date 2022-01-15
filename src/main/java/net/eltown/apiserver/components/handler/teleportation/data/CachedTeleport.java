package net.eltown.apiserver.components.handler.teleportation.data;

import lombok.Getter;

@Getter
public class CachedTeleport {

    private final String type;
    private final String object;
    private final String player;
    private final String server;
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final double yaw;
    private final double pitch;

    public CachedTeleport(final String type, final String name, final String player, final String server, final String world, final double x, final double y, final double z, final double yaw, final double pitch) {
        this.type = type;
        this.object = name;
        this.player = player;
        this.server = server;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public CachedTeleport(final String type, final String target, final String player) {
        this.type = type;
        this.object = target;
        this.player = player;
        this.server = null;
        this.world = null;
        this.x = -1;
        this.y = -1;
        this.z = -1;
        this.yaw = -1;
        this.pitch = -1;
    }

}
