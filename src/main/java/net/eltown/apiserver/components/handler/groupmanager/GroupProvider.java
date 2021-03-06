package net.eltown.apiserver.components.handler.groupmanager;

import lombok.Getter;
import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Provider;
import net.eltown.apiserver.components.handler.groupmanager.data.Group;
import net.eltown.apiserver.components.handler.groupmanager.data.GroupedPlayer;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Getter
public class GroupProvider extends Provider {

    public final HashMap<String, Group> groups = new HashMap<>();
    public final HashMap<String, GroupedPlayer> groupedPlayers = new HashMap<>();

    @SneakyThrows
    public GroupProvider(final Server server) {
        super(server, "group_groups", "group_players");
        this.createClient("API/Groupmanager[Main]");

        server.log("Gruppen werden in den Cache geladen...");
        for (final Document document : this.getCollection("group_groups").find()) {
            this.groups.put(document.getString("group"), new Group(
                    document.getString("group"),
                    document.getString("prefix"),
                    document.getList("permissions", String.class),
                    document.getList("inheritances", String.class)
            ));
        }
        server.log(this.groups.size() + " Gruppen wurden in den Cache geladen...");

        server.log("Spieler werden in den Cache geladen...");
        for (final Document document : this.getCollection("group_players").find()) {
            this.groupedPlayers.put(document.getString("_id"), new GroupedPlayer(
                    document.getString("_id"),
                    document.getString("group"),
                    document.getLong("duration"),
                    document.getList("permissions", String.class)
            ));
        }
        server.log(this.groupedPlayers.size() + " Spieler wurden in den Cache geladen...");

        if (this.groups.get("SPIELER") == null) {
            this.createGroup("SPIELER", "??7Spieler ??8| ??7%p");
        }
    }

    public boolean playerExists(final String player) {
        return this.groupedPlayers.containsKey(player);
    }

    public boolean isInGroup(final String player, final String group) {
        return this.groupedPlayers.get(player).getGroup().equals(group);
    }

    public void createPlayer(final String player) {
        this.groupedPlayers.put(player, new GroupedPlayer(player, "SPIELER", -1, Collections.singletonList("none")));
        CompletableFuture.runAsync(() -> {
            this.getCollection("group_players").insertOne(new Document("_id", player).append("group", "SPIELER").append("duration", (long) -1).append("permissions", Collections.singletonList("none")));
        });
    }

    public void setGroup(final String player, final String group, final long duration) {
        this.groupedPlayers.get(player).setGroup(group);
        this.groupedPlayers.get(player).setDuration(duration);
        System.out.println(group);
        System.out.println(this.groupedPlayers.get(player).getGroup());
        CompletableFuture.runAsync(() -> {
            this.getCollection("group_players").updateMany(new Document("_id", player), new Document("$set", new Document("group", group).append("duration", duration)));
        });
    }

    public void createGroup(final String group, final String prefix) {
        this.groups.put(group, new Group(group, prefix, new ArrayList<String>(), new ArrayList<String>()));
        this.getTinyRabbitClient().send("core.proxy.groupmanager.receive", GroupCalls.REQUEST_CHANGE_PREFIX.name(), group, prefix);
        CompletableFuture.runAsync(() -> {
            this.getCollection("group_groups").insertOne(new Document("group", group.toUpperCase()).append("prefix", prefix).append("permissions", new ArrayList<String>()).append("inheritances", new ArrayList<String>()));
        });
    }

    public void removeGroup(final String group) {
        this.groups.remove(group);
        this.groupedPlayers.values().forEach(e -> {
            if (e.getGroup().equals(group)) {
                this.setGroup(e.getPlayer(), "SPIELER", -1);
            }
        });
        CompletableFuture.runAsync(() -> {
            this.getCollection("group_groups").findOneAndDelete(new Document("group", group));
        });
    }

    public void addPermission(final String group, final String permission) {
        final List<String> permissions = new ArrayList<>(this.groups.get(group).getPermissions());
        permissions.add(permission);
        this.groups.get(group).setPermissions(permissions);
        CompletableFuture.runAsync(() -> {
            this.getCollection("group_groups").updateOne(new Document("group", group), new Document("$set", new Document("permissions", permissions)));
        });
    }

    public void removePermission(final String group, final String permission) {
        final List<String> permissions = new ArrayList<>(this.groups.get(group).getPermissions());
        permissions.remove(permission);
        this.groups.get(group).setPermissions(permissions);
        CompletableFuture.runAsync(() -> {
            this.getCollection("group_groups").updateOne(new Document("group", group), new Document("$set", new Document("permissions", permissions)));
        });
    }

    public void addPlayerPermission(final String player, final String permission) {
        final List<String> permissions = new ArrayList<>(this.groupedPlayers.get(player).getPermissions());
        permissions.add(permission);
        this.groupedPlayers.get(player).setPermissions(permissions);
        CompletableFuture.runAsync(() -> {
            this.getCollection("group_players").updateOne(new Document("_id", player), new Document("$set", new Document("permissions", permissions)));
        });
    }

    public void removePlayerPermission(final String player, final String permission) {
        final List<String> permissions = new ArrayList<>(this.groupedPlayers.get(player).getPermissions());
        permissions.remove(permission);
        this.groupedPlayers.get(player).setPermissions(permissions);
        CompletableFuture.runAsync(() -> {
            this.getCollection("group_players").updateOne(new Document("_id", player), new Document("$set", new Document("permissions", permissions)));
        });
    }

    public void addInheritance(final String group, final String inheritance) {
        final List<String> inheritances = new ArrayList<>(this.groups.get(group).getInheritances());
        inheritances.add(inheritance);
        this.groups.get(group).setInheritances(inheritances);
        CompletableFuture.runAsync(() -> {
            this.getCollection("group_groups").updateOne(new Document("group", group), new Document("$set", new Document("inheritances", inheritances)));
        });
    }

    public void removeInheritance(final String group, final String inheritance) {
        final List<String> inheritances = new ArrayList<>(this.groups.get(group).getInheritances());
        inheritances.remove(inheritance);
        this.groups.get(group).setInheritances(inheritances);
        CompletableFuture.runAsync(() -> {
            this.getCollection("group_groups").updateOne(new Document("group", group), new Document("$set", new Document("inheritances", inheritances)));
        });
    }

    public void changePrefix(final String group, final String prefix) {
        final Group sGroup = this.groups.get(group);
        sGroup.setPrefix(prefix);
        CompletableFuture.runAsync(() -> {
            final Document document = this.getCollection("group_groups").find(new Document("group", group)).first();
            assert document != null;
            this.getCollection("group_groups").updateOne(new Document("group", group), new Document("$set", new Document("prefix", prefix)));
        });

        this.getTinyRabbitClient().send("core.proxy.groupmanager.receive", GroupCalls.REQUEST_CHANGE_PREFIX.name(), group, prefix);
    }

}
