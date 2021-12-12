package net.eltown.apiserver.components.handler.crypto;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Handler;
import net.eltown.apiserver.components.handler.crypto.data.Transaction;
import net.eltown.apiserver.components.handler.crypto.data.TransferPrices;
import net.eltown.apiserver.components.handler.crypto.data.Wallet;
import net.eltown.apiserver.components.handler.crypto.data.Worth;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

import java.util.Random;

public class CryptoHandler extends Handler<CryptoProvider> {

    @SneakyThrows
    public CryptoHandler(final Server server) {
        super(server, "CryptoHandler", new CryptoProvider(server));
        this.startCallbacking();
    }

    public void startCallbacking() {
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().callback((request) -> {
                try {

                    final String[] data = request.getData();

                    switch (CryptoCalls.valueOf(request.getKey())) {
                        case REQUEST_WALLET -> {
                            final Wallet wallet = this.getProvider().getWallet(data[1]);
                            request.answer(CryptoCalls.REQUEST_WALLET.name(), wallet.getOwner(), String.valueOf(wallet.getCtc()), String.valueOf(wallet.getElt()), String.valueOf(wallet.getNot()));
                        }
                        case REQUEST_UPDATE_WALLET -> {
                            this.getProvider().updateWallet(new Wallet(
                                    data[1],
                                    Double.parseDouble(data[2]),
                                    Double.parseDouble(data[3]),
                                    Double.parseDouble(data[4])
                            ));
                            request.answer(CryptoCalls.REQUEST_UPDATE_WALLET.name(), "null");
                        }
                        case REQUEST_WORTH -> {
                            final Worth w = this.getProvider().getWorth();
                            request.answer(CryptoCalls.REQUEST_WORTH.name(), "" + w.getCtc(), "" + w.getElt(), "" + w.getNot());
                        }
                        case REQUEST_TRANSFER_PRICES -> {
                            final TransferPrices t = this.getProvider().getTransferPrices();
                            request.answer(CryptoCalls.REQUEST_TRANSFER_PRICES.name(), "" + t.getSlow(), "" + t.getNormal(), "" + t.getFast());
                        }
                        case REQUEST_TRANSACTIONS -> {
                            final StringBuilder sb = new StringBuilder();
                            this.getProvider().getTransactions(data[1]).forEach((e) -> {
                                sb.append(e.getId())
                                        .append(">>")
                                        .append(e.getAmount())
                                        .append(">>")
                                        .append(e.getWorth())
                                        .append(">>")
                                        .append(e.getType())
                                        .append(">>")
                                        .append(e.getFrom())
                                        .append(">>")
                                        .append(e.getTo())
                                        .append(">>")
                                        .append(e.getMinutesLeft())
                                        .append(">>")
                                        .append(e.isCompleted())
                                        .append(">>")
                                        .append(e.getMinutes())
                                        .append("&");
                            });
                            request.answer(CryptoCalls.REQUEST_TRANSACTIONS.name(), sb.length() > 3 ? sb.substring(0, sb.length() - 1) : "null");
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }, "API/Crypto[Callback]", "api.crypto.callback");
        });

        this.getServer().getExecutor().execute(() -> {
                this.getTinyRabbitListener().receive((delivery) -> {
                        final String[] data = delivery.getData();

                    switch (CryptoCalls.valueOf(delivery.getKey())) {
                        case UPDATE_WALLET -> this.getProvider().updateWallet(new Wallet(
                                data[1],
                                Double.parseDouble(data[2]),
                                Double.parseDouble(data[3]),
                                Double.parseDouble(data[4])
                        ));
                        case UPDATE_TRANSFER_CRYPTO -> {
                            final String id = this.createID(32);
                            final String asset = data[1];
                            final double amount = Double.parseDouble(data[2]);
                            final double worth = Double.parseDouble(data[3]);
                            final String from = data[4];
                            final String to = data[5];
                            final int time = Integer.parseInt(data[6]);
                            this.getProvider().addTransaction(new Transaction(id, amount, worth, asset, from, to, time, time, false));
                        }
                    }
                }, "API/Crypto[Receive]", "api.crypto.receive");
        });

    }

    private String createID(final int i) {
        final String chars = "a1b72c3d48e5f6123491234567890567890";
        final StringBuilder stringBuilder = new StringBuilder();
        final Random rnd = new Random();
        while (stringBuilder.length() < i) {
            int index = (int) (rnd.nextFloat() * chars.length());
            stringBuilder.append(chars.charAt(index));
        }
        return stringBuilder.toString();
    }

}
