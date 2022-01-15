package net.eltown.apiserver.components.handler.shops;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Handler;
import net.eltown.apiserver.components.handler.shops.data.ItemPrice;

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
                    final double[] prices = this.getProvider().getPrice(data[1]);
                    request.answer(ShopCalls.REQUEST_ITEM_PRICE.name(),
                            "" + prices[0],
                            "" + prices[1]
                    );
                }
                case REQUEST_MIN_BUY_SELL -> {
                    final ItemPrice price = this.getProvider().getPrices().get(data[1]);
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
                        data[1],
                        Integer.parseInt(data[2])
                );
                case UPDATE_ITEM_BOUGHT -> this.getProvider().addBought(
                        data[1],
                        Integer.parseInt(data[2])
                );
                case UPDATE_ITEM_PRICE -> this.getProvider().setPrice(
                        data[1],
                        Double.parseDouble(data[2])
                );
                case UPDATE_MIN_BUY -> this.getProvider().setMinBuy(
                        data[1],
                        Double.parseDouble(data[2])
                );
                case UPDATE_MIN_SELL -> this.getProvider().setMinSell(
                        data[1],
                        Double.parseDouble(data[2])
                );
            }

        }, "API/Shops[Receive]", "api.shops.receive");
    }

}
