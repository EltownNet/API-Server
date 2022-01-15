package net.eltown.apiserver.components.handler.player;

import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Provider;
import net.eltown.apiserver.components.handler.player.data.SyncPlayer;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PlayerProvider extends Provider {

    private final Map<String, SyncPlayer> players = new HashMap<>();

    public PlayerProvider(final Server server) {
        super(server, "a2_players");
        server.log("Spieler werden in den Cache geladen...");

        for (Document document : this.getCollection("a2_players").find()) {
            this.players.put(document.getString("_id"),
                    new SyncPlayer(
                            document.getString("inventory"),
                            document.getString("armorInventory"),
                            document.getString("enderchest"),
                            document.getString("foodLevel"),
                            document.getString("saturation"),
                            document.getString("exhaustion"),
                            document.getString("selectedSlot"),
                            document.getString("potionEffects"),
                            document.getString("totalExperience"),
                            document.getString("level"),
                            document.getString("experience"),
                            document.getString("gamemode"),
                            document.getString("flying"),
                            true
                    )
            );
        }
        server.log(this.players.size() + " Spieler wurden in den Cache geladen.");
    }

    public SyncPlayer get(String id) {
        return players.getOrDefault(id, new SyncPlayer("", "", "", "20", "20", "20", "0", "", "0", "0", "0", "SURVIVAL", "false", true));
    }

    public void set(String id, SyncPlayer player) {
        this.players.put(id, player);
        CompletableFuture.runAsync(() -> {
            Document document = this.getCollection("a2_players").find(new Document("_id", id)).first();
            if (document != null) {
                this.getCollection("a2_players").updateOne(new Document("_id", id), new Document("$set",
                        new Document("inventory", player.getInventory())
                        .append("armorInventory", player.getArmorInventory())
                        .append("enderchest", player.getEnderchest())
                        .append("foodLevel", player.getFoodLevel())
                        .append("saturation", player.getSaturation())
                        .append("exhaustion", player.getExhaustion())
                        .append("selectedSlot", player.getSelectedSlot())
                        .append("potionEffects", player.getPotionEffects())
                        .append("totalExperience", player.getTotalExperience())
                        .append("level", player.getLevel())
                        .append("experience", player.getExperience())
                        .append("gamemode", player.getGamemode())
                        .append("flying", player.getFlying())
                ));
            } else {
                this.getCollection("a2_players").insertOne(new Document("_id", id)
                        .append("inventory", player.getInventory())
                        .append("armorInventory", player.getArmorInventory())
                        .append("enderchest", player.getEnderchest())
                        .append("foodLevel", player.getFoodLevel())
                        .append("saturation", player.getSaturation())
                        .append("exhaustion", player.getExhaustion())
                        .append("selectedSlot", player.getSelectedSlot())
                        .append("potionEffects", player.getPotionEffects())
                        .append("totalExperience", player.getTotalExperience())
                        .append("level", player.getLevel())
                        .append("experience", player.getExperience())
                        .append("gamemode", player.getGamemode())
                        .append("flying", player.getFlying())
                );
            }
        });
    }

}
