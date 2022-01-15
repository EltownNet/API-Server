package net.eltown.apiserver.components.handler.shops;

import lombok.Getter;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Provider;
import net.eltown.apiserver.components.handler.shops.data.ItemPrice;
import org.bson.Document;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class ShopProvider extends Provider {

    @Getter
    private final Map<String, ItemPrice> prices = new HashMap<>();
    @Getter
    private final Set<String> toUpdate = new HashSet<>();

    private final double increaseFactor = 0.05;
    private final int amountFactor = 64;

    public ShopProvider(final Server server) {
        super(server, "shop_prices");
        server.log("Shop Preise werden in den Cache geladen...");

        for (final Document e : this.getCollection("shop_prices").find()) {
            this.prices.put(e.getString("_id"),
                    new ItemPrice(e.getString("_id"), e.getDouble("price"), e.getDouble("minBuy"), e.getDouble("minSell"), e.getInteger("bought"), e.getInteger("sold"))
            );
        }

        server.log(this.prices.size() + " Shop Preise wurden in den Cache geladen.");
        final ShopTask task = new ShopTask(server, this);
        task.run();
    }

    public double[] getPrice(final String namespaceId) {
        if (this.prices.containsKey(namespaceId)) {
            final ItemPrice price = this.prices.get(namespaceId);
            return new double[]{
                    Math.max(price.getPrice(), price.getMinBuy()),
                    Math.max(price.getPrice() * 0.23, price.getMinSell())
            };
        } else {
            final ItemPrice price = this.createPrice(namespaceId);
            return new double[]{
                    price.getPrice(),
                    price.getPrice() * 0.23
            };
        }
    }

    @Deprecated
    public double getSellPrice(final double d) {
        return .23 * d;
    }

    public void setMinBuy(final String namespaceId, final double minBuy) {
        final ItemPrice ip = this.prices.get(namespaceId);
        ip.setMinBuy(minBuy);
        this.updatePrice(ip);
    }

    public void setMinSell(final String namespaceId, final double minSell) {
        final ItemPrice ip = this.prices.get(namespaceId);
        ip.setMinSell(minSell);
        this.updatePrice(ip);
    }

    public void setPrice(final String namespaceId, final double price) {
        final ItemPrice ip = this.prices.get(namespaceId);
        ip.setPrice(price);
        this.updatePrice(ip);
    }

    public void addBought(final String namespaceId, final int amount) {
        final ItemPrice price = this.prices.get(namespaceId);
        price.addBought(amount);
        this.getPrices().put(price.getNamespaceId(), price);
        this.toUpdate.add(namespaceId);
        if (price.getBought() >= amountFactor * 2) this.updatePrices();
    }

    public void addSold(final String namespaceId, final int amount) {
        final ItemPrice price = this.prices.get(namespaceId);
        price.addSold(amount);
        this.getPrices().put(price.getNamespaceId(), price);
        this.toUpdate.add(namespaceId);
        if (price.getSold() >= amountFactor * 2) this.updatePrices();
    }

    public void updatePrices() {
        final AtomicInteger count = new AtomicInteger();
        this.getServer().log(4, "Aktualisiere Shop Preise...");

        this.getPrices().values().forEach((e) -> {
            final int toDevide = e.getBought() - e.getSold();
            if (toDevide != 0) {

                final double toMultiply = (double) toDevide / (double) amountFactor;
                double add = 1 + (toMultiply * increaseFactor);

                if (add > 2.0) add = 2.0;
                double newPrice = e.getPrice() * add;
                if (newPrice < (e).getMinSell()) newPrice = e.getMinSell();

                if ((e.getMinSell() * 3) > newPrice) {
                    if (ThreadLocalRandom.current().nextInt(3) == 2) {
                        newPrice += newPrice * (1 + ThreadLocalRandom.current().nextDouble(0.40) + 0.10);
                    }
                }

                e.setPrice(newPrice);
                e.setBought(0);
                e.setSold(0);

                this.toUpdate.add(e.getNamespaceId());
                count.incrementAndGet();
            }
        });

        this.getServer().log(4, count.get() + " Shop Preise wurden aktualisiert.");
    }

    public void updatePrice(final ItemPrice price) {
        this.prices.put(price.getNamespaceId(), price);
        CompletableFuture.runAsync(() -> {
            try {
                this.getCollection("shop_prices").updateOne(new Document("_id", price.getNamespaceId()),
                        new Document("$set", new Document("price", price.getPrice())
                                .append("bought", price.getBought())
                                .append("sold", price.getSold())
                                .append("minBuy", price.getMinBuy())
                                .append("minSell", price.getMinSell())
                        )
                );
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private ItemPrice createPrice(final String namespaceId) {
        final ItemPrice itemPrice = new ItemPrice(namespaceId, 5, 0.25, 0.01, 0, 0);
        this.getPrices().put(namespaceId, itemPrice);
        CompletableFuture.runAsync(() -> {
            this.getCollection("shop_prices").insertOne(
                    new Document("_id", namespaceId)
                            .append("price", 5d)
                            .append("bought", 0)
                            .append("sold", 0)
                            .append("minBuy", 0.25d)
                            .append("minSell", 0.01d)
            );
        });
        return itemPrice;
    }

}
