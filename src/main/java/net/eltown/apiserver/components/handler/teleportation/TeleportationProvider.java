package net.eltown.apiserver.components.handler.teleportation;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Provider;
import net.eltown.apiserver.components.handler.teleportation.data.CachedTeleport;
import net.eltown.apiserver.components.handler.teleportation.data.Home;
import net.eltown.apiserver.components.handler.teleportation.data.Warp;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class TeleportationProvider extends Provider {

    public final HashMap<String, Home> homes = new HashMap<>();
    public final HashMap<String, Warp> warps = new HashMap<>();
    public final HashMap<String, List<String>> tpas = new HashMap<>();

    public final HashMap<String, CachedTeleport> cachedTeleports = new HashMap<>();

    @SneakyThrows
    public TeleportationProvider(final Server server) {
        super(server, "teleportation_homes", "teleportation_warps");

        this.createClient("API/Teleportation[Main]");

        server.log("Homes werden in den Cache geladen...");
        for (final Document document : this.getCollection("teleportation_homes").find()) {
            this.homes.put(document.getString("player") + "/" + document.getString("home"),
                    new Home(document.getString("home"),
                            document.getString("player"),
                            document.getString("server"),
                            document.getString("world"),
                            document.getDouble("x"),
                            document.getDouble("y"),
                            document.getDouble("z"),
                            document.getDouble("yaw"),
                            document.getDouble("pitch")
                    ));
        }
        server.log(this.homes.size() + " Homes wurden in den Cache geladen...");

        server.log("Warps werden in den Cache geladen...");
        for (final Document document : this.getCollection("teleportation_warps").find()) {
            this.warps.put(document.getString("warp"),
                    new Warp(document.getString("warp"),
                            document.getString("displayName"),
                            document.getString("imageUrl"),
                            document.getString("server"),
                            document.getString("world"),
                            document.getDouble("x"),
                            document.getDouble("y"),
                            document.getDouble("z"),
                            document.getDouble("yaw"),
                            document.getDouble("pitch")
                    ));
        }
        server.log(this.warps.size() + " Warps wurden in den Cache geladen...");
    }

    public void createHome(final String name, final String player, final String server, final String world, final double x, final double y, final double z, final double yaw, final double pitch) {
        this.homes.put(player + "/" + name, new Home(name, player, server, world, x, y, z, yaw, pitch));
        CompletableFuture.runAsync(() -> {
            this.getCollection("teleportation_homes").insertOne(new Document("home", name)
                    .append("player", player)
                    .append("server", server)
                    .append("world", world)
                    .append("x", x)
                    .append("y", y)
                    .append("z", z)
                    .append("yaw", yaw)
                    .append("pitch", pitch)
            );
        });
    }

    public boolean homeExists(final String name, final String player) {
        return this.homes.containsKey(player + "/" + name);
    }

    public void deleteHome(final String name, final String player) {
        CompletableFuture.runAsync(() -> this.getCollection("teleportation_homes").findOneAndDelete(new Document("home", name).append("player", player)));
        this.homes.remove(player + "/" + name);
    }

    public void deleteServerHomes(final String server) {
        CompletableFuture.runAsync(() -> {
            for (final Document document : this.getCollection("teleportation_homes").find()) {
                if (document.getString("server").equals(server)) {
                    this.getCollection("teleportation_homes").deleteOne(document);
                    this.homes.remove(document.getString("player") + "/" + document.getString("home"));
                }
            }
        });
    }

    public void updateHomeName(final String name, final String player, final String newName) {
        CompletableFuture.runAsync(() -> {
            this.getCollection("teleportation_homes").updateOne(new Document("home", name).append("player", player), new Document("$set", new Document("home", newName)));
        });
        this.homes.get(player + "/" + name).setName(newName);
    }

    public void updateHomePosition(final String name, final String player, final String server, final String world, final double x, final double y, final double z, final double yaw, final double pitch) {
        CompletableFuture.runAsync(() -> {
            this.getCollection("teleportation_homes").updateMany(new Document("home", name).append("player", player), new Document("$set",
                    new Document("server", server)
                            .append("world", world)
                            .append("x", x)
                            .append("y", y)
                            .append("z", z)
                            .append("yaw", yaw)
                            .append("pitch", pitch)
            ));
        });
        final Home home = this.homes.get(player + "/" + name);
        home.setServer(server);
        home.setWorld(world);
        home.setX(x);
        home.setY(y);
        home.setZ(z);
        home.setYaw(yaw);
        home.setPitch(pitch);
    }

    public Set<Home> getHomes(final String player) {
        final Set<Home> set = new HashSet<>();
        this.homes.keySet().forEach(e -> {
            if (e.startsWith(player)) {
                set.add(this.homes.get(e));
            }
        });
        return set;
    }

    public void createWarp(final String name, final String displayName, final String imageUrl, final String server, final String world, final double x, final double y, final double z, final double yaw, final double pitch) {
        this.warps.put(name, new Warp(name, displayName, imageUrl, server, world, x, y, z, yaw, pitch));
        CompletableFuture.runAsync(() -> {
            this.getCollection("teleportation_warps").insertOne(new Document("warp", name)
                    .append("displayName", displayName)
                    .append("imageUrl", imageUrl)
                    .append("server", server)
                    .append("world", world)
                    .append("x", x)
                    .append("y", y)
                    .append("z", z)
                    .append("yaw", yaw)
                    .append("pitch", pitch)
            );
        });
    }

    public boolean warpExists(final String name) {
        return this.warps.containsKey(name);
    }

    public void deleteWarp(final String name) {
        CompletableFuture.runAsync(() -> this.getCollection("teleportation_warps").findOneAndDelete(new Document("warp", name)));
        this.warps.remove(name);
    }

    public void updateWarpName(final String name, final String displayName) {
        CompletableFuture.runAsync(() -> {
            this.getCollection("teleportation_warps").updateOne(new Document("warp", name), new Document("$set", new Document("displayName", displayName)));
        });
        this.warps.get(name).setDisplayName(displayName);
    }

    public void updateWarpImage(final String name, final String imageUrl) {
        CompletableFuture.runAsync(() -> {
            this.getCollection("teleportation_warps").updateOne(new Document("warp", name), new Document("$set", new Document("imageUrl", imageUrl)));
        });
        this.warps.get(name).setImageUrl(imageUrl);
    }

    public void updateWarpPosition(final String name, final String server, final String world, final double x, final double y, final double z, final double yaw, final double pitch) {
        CompletableFuture.runAsync(() -> {
            this.getCollection("teleportation_warps").updateMany(new Document("warp", name), new Document("$set",
                    new Document("server", server)
                            .append("world", world)
                            .append("x", x)
                            .append("y", y)
                            .append("z", z)
                            .append("yaw", yaw)
                            .append("pitch", pitch)
            ));
        });
        final Warp warp = this.warps.get(name);
        warp.setServer(server);
        warp.setWorld(world);
        warp.setX(x);
        warp.setY(y);
        warp.setZ(z);
        warp.setYaw(yaw);
        warp.setPitch(pitch);
    }

    public void createTpa(final String player, final String target) {
        this.tpas.computeIfAbsent(target, k -> new ArrayList<>());
        this.tpas.get(target).add(player + ":" + System.currentTimeMillis() + 120000);
    }

    public Set<String> getTpas(final String target) {
        final Set<String> set = new HashSet<>();
        if (this.tpas.get(target) == null) return set;

        new ArrayList<>(this.tpas.get(target)).forEach(e -> {
            if (Long.parseLong(e.split(":")[1]) < System.currentTimeMillis()) {
                this.removeTpa(target, e.split(":")[0]);
            } else set.add(e.split(":")[0]);
        });
        return set;
    }

    public boolean hasTpaSent(final String target, final String player) {
        final AtomicBoolean aBoolean = new AtomicBoolean(false);
        this.tpas.computeIfAbsent(target, k -> new ArrayList<>());

        new ArrayList<>(this.tpas.get(target)).forEach(e -> {
            if (e.startsWith(player)) {
                final long time = Long.parseLong(e.split(":")[1]);
                if (System.currentTimeMillis() > time) {
                    this.removeTpa(target, player);
                    aBoolean.set(false);
                } else aBoolean.set(true);
            }
        });
        return aBoolean.get();
    }

    public void removeTpa(final String target, final String player) {
        this.tpas.get(target).removeIf(e -> e.startsWith(player));
    }

}
