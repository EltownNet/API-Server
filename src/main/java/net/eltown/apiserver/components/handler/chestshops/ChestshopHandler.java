package net.eltown.apiserver.components.handler.chestshops;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Handler;

public class ChestshopHandler extends Handler<ChestshopProvider> {

    @SneakyThrows
    public ChestshopHandler(final Server server) {
        super(server, "ChestshopHandler", new ChestshopProvider(server));
        this.startCallbacking();
    }

    private void startCallbacking() {
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().receive(delivery -> {
                final String[] d = delivery.getData();
                switch (ChestshopCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case REQUEST_CREATE_CHESTSHOP -> this.getProvider().createChestShop(Long.parseLong(d[1]), Double.parseDouble(d[2]), Double.parseDouble(d[3]),
                            Double.parseDouble(d[4]), Double.parseDouble(d[5]), Double.parseDouble(d[6]), Double.parseDouble(d[7]),
                            d[8], d[9], d[10], Integer.parseInt(d[11]), d[12], Double.parseDouble(d[13]), d[14]);
                    case REQUEST_UPDATE_AMOUNT -> this.getProvider().updateCount(Long.parseLong(d[1]), Integer.parseInt(d[2]));
                    case REQUEST_UPDATE_PRICE -> this.getProvider().updatePrice(Long.parseLong(d[1]), Double.parseDouble(d[2]));
                    case REQUEST_UPDATE_ITEM -> this.getProvider().updateItem(Long.parseLong(d[1]), d[2]);
                    case REQUEST_REMOVE_SHOP -> this.getProvider().removeChestShop(Long.parseLong(d[1]));
                    case REQUEST_SET_LICENSE -> this.getProvider().setLicense(d[1], d[2]);
                    case REQUEST_ADD_ADDITIONAL_SHOPS -> this.getProvider().addAdditionalShops(d[1], Integer.parseInt(d[2]));
                    case REQUEST_SET_ADDITIONAL_SHOPS -> this.getProvider().setAdditionalShops(d[1], Integer.parseInt(d[2]));
                }
            }, "API/ChestShop[Receive]", "api.chestshops.receive");
        });
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().callback(request -> {
                final String[] d = request.getData();
                switch (ChestshopCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_LOAD_DATA:
                        final StringBuilder chestShopDataBuilder = new StringBuilder();
                        this.getProvider().cachedChestShops.forEach((id, chestShop) -> {
                            chestShopDataBuilder
                                    .append(id).append("#")
                                    .append(chestShop.getSignX()).append("#")
                                    .append(chestShop.getSignY()).append("#")
                                    .append(chestShop.getSignZ()).append("#")
                                    .append(chestShop.getChestX()).append("#")
                                    .append(chestShop.getChestY()).append("#")
                                    .append(chestShop.getChestZ()).append("#")
                                    .append(chestShop.getLevel()).append("#")
                                    .append(chestShop.getOwner()).append("#")
                                    .append(chestShop.getShopType()).append("#")
                                    .append(chestShop.getShopCount()).append("#")
                                    .append(chestShop.getItem()).append("#")
                                    .append(chestShop.getPrice()).append("#")
                                    .append(chestShop.getBankAccount()).append("-;-");
                        });
                        if (chestShopDataBuilder.toString().isEmpty()) chestShopDataBuilder.append("null-;-");
                        final String chestShopData = chestShopDataBuilder.substring(0, chestShopDataBuilder.length() - 3);

                        final StringBuilder chestShopLicenseBuilder = new StringBuilder();
                        this.getProvider().cachedChestShopLicenses.forEach((owner, license) -> {
                            chestShopLicenseBuilder
                                    .append(owner).append("#")
                                    .append(license.getLicense()).append("#")
                                    .append(license.getAdditionalShops()).append("-;-");
                        });
                        final String chestShopLicense = chestShopLicenseBuilder.substring(0, chestShopLicenseBuilder.length() - 3);

                        request.answer(ChestshopCalls.CALLBACK_LOAD_DATA.name(), chestShopData, chestShopLicense);
                        break;
                }
            }, "API/ChestShop[Callback]", "api.chestshops.callback");
        });
    }

}
