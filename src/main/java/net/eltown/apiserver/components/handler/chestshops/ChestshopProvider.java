package net.eltown.apiserver.components.handler.chestshops;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Provider;
import net.eltown.apiserver.components.handler.chestshops.data.ChestShop;
import net.eltown.apiserver.components.handler.chestshops.data.ChestShopLicense;
import org.bson.Document;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class ChestshopProvider extends Provider {

    public final HashMap<Long, ChestShop> cachedChestShops = new HashMap<>();
    public final HashMap<String, ChestShopLicense> cachedChestShopLicenses = new HashMap<>();

    @SneakyThrows
    public ChestshopProvider(final Server server) {
        super(server, "chestshop_data_a2", "chestshop_licenses");

        server.log("ChestShops werden in den Cache geladen...");
        for (final Document document : this.getCollection("chestshop_data_a2").find()) {
            this.cachedChestShops.put(document.getLong("_id"), new ChestShop(
                    document.getLong("_id"),
                    document.getDouble("signX"),
                    document.getDouble("signY"),
                    document.getDouble("signZ"),
                    document.getDouble("chestX"),
                    document.getDouble("chestY"),
                    document.getDouble("chestZ"),
                    document.getString("level"),
                    document.getString("owner"),
                    document.getString("type"),
                    document.getInteger("count"),
                    document.getString("item"),
                    document.getDouble("price"),
                    document.getString("bankAccount")
            ));
        }
        server.log(this.cachedChestShops.size() + " ChestShops wurden in den Cache geladen...");

        server.log("ChestShop-Lizenzen werden in den Cache geladen...");
        for (final Document document : this.getCollection("chestshop_licenses").find()) {
            this.cachedChestShopLicenses.put(document.getString("_id"), new ChestShopLicense(
                    document.getString("_id"),
                    document.getString("license"),
                    document.getInteger("shops")
            ));
        }
        server.log(this.cachedChestShopLicenses.size() + " ChestShop-Lizenzen wurden in den Cache geladen...");
    }

    public void createChestShop(final long id, final double signX, final double signY, final double signZ, final double chestX, final double chestY, final double chestZ, final String level, final String owner, final String type, final int count, final String item, final double price, final String bankAccount) {
        this.cachedChestShops.put(id, new ChestShop(id, signX, signY, signZ, chestX, chestY, chestZ, level, owner, type, count, item, price, bankAccount));

        CompletableFuture.runAsync(() -> {
            try {
                this.getCollection("chestshop_data_a2").insertOne(new Document("_id", id)
                        .append("signX", signX)
                        .append("signY", signY)
                        .append("signZ", signZ)
                        .append("chestX", chestX)
                        .append("chestY", chestY)
                        .append("chestZ", chestZ)
                        .append("level", level)
                        .append("owner", owner)
                        .append("type", type)
                        .append("count", count)
                        .append("item", item)
                        .append("price", price)
                        .append("bankAccount", bankAccount)
                );
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void updateCount(final long id, final int u) {
        this.cachedChestShops.get(id).setShopCount(u);

        CompletableFuture.runAsync(() -> {
            this.getCollection("chestshop_data_a2").updateOne(new Document("_id", id), new Document("$set", new Document("count", u)));
        });
    }

    public void updatePrice(final long id, final double u) {
        this.cachedChestShops.get(id).setPrice(u);

        CompletableFuture.runAsync(() -> {
            this.getCollection("chestshop_data_a2").updateOne(new Document("_id", id), new Document("$set", new Document("price", u)));
        });
    }

    public void updateItem(final long id, final String u) {
        this.cachedChestShops.get(id).setItem(u);

        CompletableFuture.runAsync(() -> {
            this.getCollection("chestshop_data_a2").updateOne(new Document("_id", id), new Document("$set", new Document("item", u)));
        });
    }

    public void removeChestShop(final long id) {
        this.cachedChestShops.remove(id);

        CompletableFuture.runAsync(() -> {
            this.getCollection("chestshop_data_a2").findOneAndDelete(new Document("_id", id));
        });
    }

    public void setLicense(final String owner, final String license) {
        if (!this.cachedChestShopLicenses.containsKey(owner)) {
            this.cachedChestShopLicenses.put(owner, new ChestShopLicense(owner, license, 0));

            CompletableFuture.runAsync(() -> {
                this.getCollection("chestshop_licenses").insertOne(new Document("_id", owner).append("license", license).append("shops", 0));
            });
            return;
        }

        this.cachedChestShopLicenses.get(owner).setLicense(license);

        CompletableFuture.runAsync(() -> {
            this.getCollection("chestshop_licenses").updateOne(new Document("_id", owner), new Document("$set", new Document("license", license)));
        });
    }

    public void setAdditionalShops(final String owner, final int i) {
        if (!this.cachedChestShopLicenses.containsKey(owner)) {
            this.cachedChestShopLicenses.put(owner, new ChestShopLicense(owner, "STANDARD", i));

            CompletableFuture.runAsync(() -> {
                this.getCollection("chestshop_licenses").insertOne(new Document("_id", owner).append("license", "STANDARD").append("shops", i));
            });
            return;
        }

        this.cachedChestShopLicenses.get(owner).setAdditionalShops(i);

        CompletableFuture.runAsync(() -> {
            this.getCollection("chestshop_licenses").updateOne(new Document("_id", owner), new Document("$set", new Document("shops", i)));
        });
    }

    public void addAdditionalShops(final String owner, final int i) {
        if (!this.cachedChestShopLicenses.containsKey(owner)) {
            this.cachedChestShopLicenses.put(owner, new ChestShopLicense(owner, "STANDARD", i));

            CompletableFuture.runAsync(() -> {
                this.getCollection("chestshop_licenses").insertOne(new Document("_id", owner).append("license", "STANDARD").append("shops", i));
            });
            return;
        }

        this.cachedChestShopLicenses.get(owner).addShops(i);

        CompletableFuture.runAsync(() -> {
            this.getCollection("chestshop_licenses").updateOne(new Document("_id", owner), new Document("$set", new Document("shops", this.cachedChestShopLicenses.get(owner).getAdditionalShops())));
        });
    }

}
