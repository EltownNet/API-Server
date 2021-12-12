package net.eltown.apiserver.components.handler.drugs;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import lombok.Getter;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Provider;
import net.eltown.apiserver.components.config.Config;
import net.eltown.apiserver.components.handler.drugs.data.Delivery;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DrugProvider extends Provider {

    @Getter
    private final Map<String, Delivery> deliveries = new HashMap<>();

    public DrugProvider(final Server server) {
        super(server, "drug_deliveries");
        server.log("Lieferungen werden in den Cache geladen...");
        
        for (final Document doc : this.getCollection("drug_deliveries").find()) {
            this.deliveries.put(doc.getString("_id"),
                    new Delivery(
                            doc.getString("_id"),
                            doc.getString("receiver"),
                            doc.getString("type"),
                            doc.getString("quality"),
                            doc.getInteger("amount"),
                            doc.getInteger("time"),
                            doc.getInteger("timeLeft"),
                            doc.getBoolean("completed")
                    )
            );
        }

        final DrugTask task = new DrugTask(server, this);
        task.run();

        server.log(this.deliveries.size() + " Lieferungen wurden in den Cache geladen.");
    }

    public void updateDelivery(final Delivery delivery) {
        this.deliveries.put(delivery.getId(), delivery);
        CompletableFuture.runAsync(() -> {
            this.getCollection("drug_deliveries").updateOne(new Document("_id", delivery.getId()),
                    new Document("$set", new Document("receiver", delivery.getReceiver())
                            .append("type", delivery.getType())
                            .append("quality", delivery.getQuality())
                            .append("amount", delivery.getAmount())
                            .append("time", delivery.getTime())
                            .append("timeLeft", delivery.getTimeLeft())
                            .append("completed", delivery.isCompleted())
                    )
            );
        });
    }

    public void addDelivery(final Delivery delivery) {
        this.deliveries.put(delivery.getId(), delivery);
        CompletableFuture.runAsync(() -> {
            this.getCollection("drug_deliveries").insertOne(
                    new Document("_id", delivery.getId())
                            .append("receiver", delivery.getReceiver())
                            .append("type", delivery.getType())
                            .append("quality", delivery.getQuality())
                            .append("amount", delivery.getAmount())
                            .append("time", delivery.getTime())
                            .append("timeLeft", delivery.getTimeLeft())
                            .append("completed", delivery.isCompleted())
            );
        });
    }

    public void removeDelivery(final Delivery delivery) {
        this.deliveries.remove(delivery.getId());
        CompletableFuture.runAsync(() -> {
            this.getCollection("drug_deliveries").deleteOne(new Document("_id", delivery.getId()));
        });
    }

}
