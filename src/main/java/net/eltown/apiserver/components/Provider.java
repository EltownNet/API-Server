package net.eltown.apiserver.components;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import lombok.Getter;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.config.Config;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbit;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;

public class Provider {

    @Getter
    private final Server server;
    private final MongoClient client;
    private final Map<String, MongoCollection<Document>> collections = new HashMap<>();
    @Getter
    private TinyRabbit tinyRabbitClient;

    public Provider(final Server server, final String... collections) {
        this.server = server;
        final Config config = this.server.getConfig();
        this.client = new MongoClient(new MongoClientURI(config.getString("MongoDB.Connection")));
        for (final String collection : collections) {
            this.collections.put(collection, this.client.getDatabase(config.getString("MongoDB.Database")).getCollection(collection));
        }
    }

    public TinyRabbit createClient(final String connectionName) {
        try {
            this.tinyRabbitClient = new TinyRabbit("localhost", connectionName);
            this.tinyRabbitClient.throwExceptions(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return this.tinyRabbitClient;
    }

    public MongoCollection<Document> getCollection(final String collection) {
        return this.collections.get(collection);
    }

}
