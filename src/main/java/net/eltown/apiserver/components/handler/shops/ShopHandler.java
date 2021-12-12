package net.eltown.apiserver.components.handler.shops;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Handler;
import net.eltown.apiserver.components.handler.shops.data.ItemPrice;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

public class ShopHandler extends Handler<ShopProvider> {

    @SneakyThrows
    public ShopHandler(final Server server) {
        super(server, "ShopHandler", new ShopProvider(server));
        this.startCallbacking();
        this.startListening();
    }

    public void startCallbacking() {
        this.getTinyRabbitListener().callback((request) -> {
            final String[] data = request.getData();
            switch (ShopCalls.valueOf(request.getKey())) {
                case REQUEST_ITEM_PRICE -> {
                    final double[] prices = this.getProvider().getPrice(new int[]{Integer.parseInt(data[1]), Integer.parseInt(data[2])});
                    request.answer(ShopCalls.REQUEST_ITEM_PRICE.name(),
                            "" + prices[0],
                            "" + prices[1]
                    );
                }
                case REQUEST_MIN_BUY_SELL -> {
                    final ItemPrice price = this.getProvider().getPrices().get(data[1] + ":" + data[2]);
                    request.answer(ShopCalls.REQUEST_MIN_BUY_SELL.name(),
                            "" + price.getMinBuy(),
                            "" + price.getMinSell()
                    );
                }
            }
        }, "API/Shops[Callback]", "api.shops.callback");
    }

    public void startListening() {
        this.getTinyRabbitListener().receive((delivery) -> {
            final String[] data = delivery.getData();
            switch (ShopCalls.valueOf(delivery.getKey())) {
                case UPDATE_ITEM_SOLD -> this.getProvider().addSold(
                        new int[]{Integer.parseInt(data[1]), Integer.parseInt(data[2])},
                        Integer.parseInt(data[3])
                );
                case UPDATE_ITEM_BOUGHT -> this.getProvider().addBought(
                        new int[]{Integer.parseInt(data[1]), Integer.parseInt(data[2])},
                        Integer.parseInt(data[3])
                );
                case UPDATE_ITEM_PRICE -> this.getProvider().setPrice(
                        new int[]{Integer.parseInt(data[1]), Integer.parseInt(data[2])},
                        Double.parseDouble(data[3])
                );
                case UPDATE_MIN_BUY -> this.getProvider().setMinBuy(
                        new int[]{Integer.parseInt(data[1]), Integer.parseInt(data[2])},
                        Double.parseDouble(data[3])
                );
                case UPDATE_MIN_SELL -> this.getProvider().setMinSell(
                        new int[]{Integer.parseInt(data[1]), Integer.parseInt(data[2])},
                        Double.parseDouble(data[3])
                );
            }

        }, "API/Shops[Receive]", "api.shops.receive");
    }

}
