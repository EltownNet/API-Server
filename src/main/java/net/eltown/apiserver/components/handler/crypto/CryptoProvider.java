package net.eltown.apiserver.components.handler.crypto;

import lombok.Getter;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Provider;
import net.eltown.apiserver.components.handler.crypto.data.Transaction;
import net.eltown.apiserver.components.handler.crypto.data.TransferPrices;
import net.eltown.apiserver.components.handler.crypto.data.Wallet;
import net.eltown.apiserver.components.handler.crypto.data.Worth;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CryptoProvider extends Provider {

    private final Map<String, Wallet> wallets = new HashMap<>();
    @Getter
    private final Map<String, Transaction> transactions = new HashMap<>();
    @Getter
    private final Worth worth;
    @Getter
    private final TransferPrices transferPrices;

    public CryptoProvider(final Server server) {
        super(server, "wallets", "crypto_worth", "crypto_transfers");
        server.log("Wallets werden in den Cache geladen...");
        //final Config config = server.getConfig();
        //this.client = new MongoClient(new MongoClientURI(config.getString("MongoDB.Uri")));
        //this.collection = this.client.getDatabase(config.getString("MongoDB.CryptoDB")).getCollection("wallets");
        //this.worthCollection = this.client.getDatabase(config.getString("MongoDB.CryptoDB")).getCollection("crypto_worth");
        //this.transferCollection = this.client.getDatabase(config.getString("MongoDB.CryptoDB")).getCollection("crypto_transfers");

        final Document cryptoWorth = this.getCollection("crypto_worth").find(new Document("_id", "worth")).first();
        final Document transferPrices = this.getCollection("crypto_worth").find(new Document("_id", "transferPrices")).first();

        if (cryptoWorth != null) {
            this.worth = new Worth(cryptoWorth.getDouble("CTC"), cryptoWorth.getDouble("ELT"), cryptoWorth.getDouble("NOT"));
        } else {
            this.getCollection("crypto_worth").insertOne(
                    new Document("_id", "worth")
                            .append("CTC", 10000.0d)
                            .append("ELT", 2750.0d)
                            .append("NOT", 50.0d)
            );

            this.worth = new Worth(10000, 2750, 50);
        }

        if (transferPrices != null) {
            this.transferPrices = new TransferPrices(transferPrices.getDouble("slow"), transferPrices.getDouble("normal"), transferPrices.getDouble("fast"));
        } else {
            this.getCollection("crypto_worth").insertOne(
                    new Document("_id", "transferPrices")
                            .append("slow", 8.99d)
                            .append("normal", 24.99d)
                            .append("fast", 59.99d)
            );

            this.transferPrices = new TransferPrices(8.99d, 24.99d, 59.99d);
        }

        for (final Document document : this.getCollection("wallets").find()) {
            this.wallets.put(document.getString("_id"),
                    new Wallet(
                            document.getString("_id"),
                            document.getDouble("CTC"),
                            document.getDouble("ELT"),
                            document.getDouble("NOT")
                    )
            );
        }

        for (final Document document : this.getCollection("crypto_transfers").find()) {
            this.transactions.put(document.getString("_id"),
                    new Transaction(document.getString("_id"),
                            document.getDouble("amount"),
                            document.getDouble("worth"),
                            document.getString("type"),
                            document.getString("from"),
                            document.getString("to"),
                            document.getInteger("minutesLeft"),
                            document.getInteger("minutes"),
                            document.getBoolean("completed")
                    )
            );
        }

        final CryptoTask task = new CryptoTask(server, this);
        task.run();
        server.log(this.wallets.size() + " Wallets geladen.");
    }

    public Set<Transaction> getTransactions(final String player) {
        return this.transactions.values().stream()
                .filter(p -> p.getFrom().equalsIgnoreCase(player) || p.getTo().equalsIgnoreCase(player))
                .collect(Collectors.toSet());
    }

    public void updateTransaction(final Transaction transaction) {
        CompletableFuture.runAsync(() -> {
            this.getCollection("crypto_transfers").updateOne(new Document("_id", transaction.getId()),
                    new Document("$set", new Document("amount", transaction.getAmount())
                            .append("worth", transaction.getWorth())
                            .append("type", transaction.getType())
                            .append("from", transaction.getFrom())
                            .append("to", transaction.getTo())
                            .append("minutesLeft", transaction.getMinutesLeft())
                            .append("minutes", transaction.getMinutes())
                            .append("completed", transaction.isCompleted())
                    )

            );
        });
    }

    public void addTransaction(final Transaction transaction) {
        this.transactions.put(transaction.getId(), transaction);
        CompletableFuture.runAsync(() -> {
            this.getCollection("crypto_transfers").insertOne(new Document("_id", transaction.getId())
                    .append("amount", transaction.getAmount())
                    .append("worth", transaction.getWorth())
                    .append("type", transaction.getType())
                    .append("from", transaction.getFrom())
                    .append("to", transaction.getTo())
                    .append("minutesLeft", transaction.getMinutesLeft())
                    .append("minutes", transaction.getMinutes())
                    .append("completed", transaction.isCompleted())
            );
        });
    }


    public Wallet getWallet(final String owner) {
        if (this.wallets.containsKey(owner)) {
            return this.wallets.get(owner);
        } else {
            final Wallet wallet = new Wallet(owner, 0f, 0f, 0f);
            this.createWallet(wallet);
            return this.getWallet(owner);
        }
    }

    public void updateWallet(final Wallet wallet) {
        this.wallets.put(wallet.getOwner(), wallet);
        CompletableFuture.runAsync(() -> {
            this.getCollection("wallets").updateOne(new Document("_id", wallet.getOwner()),
                    new Document("$set", new Document("CTC", wallet.getCtc())
                            .append("ELT", wallet.getElt())
                            .append("NOT", wallet.getNot()))
            );
        });
    }

    private void createWallet(final Wallet wallet) {
        this.wallets.put(wallet.getOwner(), wallet);
        CompletableFuture.runAsync(() -> {
            this.getCollection("wallets").insertOne(new Document("_id", wallet.getOwner())
                    .append("CTC", wallet.getCtc())
                    .append("ELT", wallet.getElt())
                    .append("NOT", wallet.getNot())
            );
        });
    }

}
