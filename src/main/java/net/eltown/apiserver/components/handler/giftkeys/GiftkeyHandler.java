package net.eltown.apiserver.components.handler.giftkeys;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Handler;
import net.eltown.apiserver.components.handler.giftkeys.data.Giftkey;

import java.util.Arrays;
import java.util.List;

public class GiftkeyHandler extends Handler<GiftkeyProvider> {

    @SneakyThrows
    public GiftkeyHandler(final Server server) {
        super(server, "GiftkeyHandler", new GiftkeyProvider(server));
        this.startCallbacking();
    }

    public void startCallbacking() {
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().receive(delivery -> {
                final String[] d = delivery.getData();
                switch (GiftkeyCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case REQUEST_DELETE_KEY:
                        this.getProvider().deleteKey(d[1]);
                        break;
                }
            }, "API/Giftkeys[Receive]", "api.giftkeys.receive");
        });

        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().callback((request -> {
                final String[] d = request.getData();
                switch (GiftkeyCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_CREATE_KEY:
                        final int maxUses = Integer.parseInt(d[1]);
                        final List<String> rewards = Arrays.asList(d[2].split(">:<"));
                        final List<String> marks = Arrays.asList(d[3].split(">:<"));
                        this.getProvider().createKey(maxUses, rewards, marks, key -> {
                            request.answer(GiftkeyCalls.CALLBACK_NULL.name(), key);
                        });
                        break;
                    case REQUEST_GET_KEY:
                        final String key = d[1];
                        if (!this.getProvider().keyExists(key)) {
                            request.answer(GiftkeyCalls.CALLBACK_NULL.name(), "null");
                            return;
                        }
                        final Giftkey giftkey = this.getProvider().getGiftKey(key);
                        final StringBuilder builder = new StringBuilder(key).append(">>").append(giftkey.getMaxUses()).append(">>");

                        String a = "null";
                        if (giftkey.getUses().size() != 0) {
                            final StringBuilder usesBuilder = new StringBuilder();
                            giftkey.getUses().forEach(e -> {
                                usesBuilder.append(e).append(">:<");
                            });
                            a = usesBuilder.substring(0, usesBuilder.length() - 3);
                        }
                        builder.append(a).append(">>");

                        final StringBuilder rewardsBuilder = new StringBuilder();
                        giftkey.getRewards().forEach(e -> {
                            rewardsBuilder.append(e).append(">:<");
                        });
                        builder.append(rewardsBuilder.substring(0, rewardsBuilder.length() - 3)).append(">>");

                        final StringBuilder marksBuilder = new StringBuilder();
                        giftkey.getMarks().forEach(e -> {
                            marksBuilder.append(e).append(">:<");
                        });
                        builder.append(marksBuilder.substring(0, marksBuilder.length() - 3));

                        request.answer(GiftkeyCalls.CALLBACK_KEY.name(), builder.toString());
                        break;
                    case REQUEST_REDEEM_KEY:
                        final String redeemKey = d[1];
                        final String player = d[2];
                        if (this.getProvider().keyExists(redeemKey)) {
                            if (!this.getProvider().alreadyRedeemed(redeemKey, player)) {
                                this.getProvider().redeemKey(redeemKey, player);

                                final Giftkey giftkey1 = this.getProvider().getGiftKey(redeemKey);
                                final StringBuilder redeemBuilder = new StringBuilder();
                                giftkey1.getRewards().forEach(e -> {
                                    redeemBuilder.append(e).append(">:<");
                                });
                                final String redeemRewards = redeemBuilder.substring(0, redeemBuilder.length() - 3);
                                request.answer(GiftkeyCalls.CALLBACK_REDEEMED.name(), redeemRewards);
                            } else request.answer(GiftkeyCalls.CALLBACK_ALREADY_REDEEMED.name(), "null");
                        } else request.answer(GiftkeyCalls.CALLBACK_NULL.name(), "null");
                        break;
                    case REQUEST_USER_CODES:
                        final List<String> list = this.getProvider().getKeysByMark(d[1]);
                        if (!list.isEmpty()) {
                            final StringBuilder codeBuilder = new StringBuilder();
                            list.forEach(e -> codeBuilder.append(e).append(">:<"));
                            final String codes = codeBuilder.substring(0, codeBuilder.length() - 3);
                            request.answer(GiftkeyCalls.CALLBACK_USER_CODES.name(), codes);
                        } else request.answer(GiftkeyCalls.CALLBACK_NULL.name(), "null");
                        break;
                    case REQUEST_ADD_MARK:
                        if (this.getProvider().keyExists(d[1])) {
                            this.getProvider().addMarkToKey(d[1], d[2], d[3]);
                            request.answer(GiftkeyCalls.CALLBACK_MARK_ADDED.name(), "null");
                        } else request.answer(GiftkeyCalls.CALLBACK_NULL.name(), "null");
                        break;
                }
            }), "API/Giftkeys[Callback]", "api.giftkeys.callback");
        });
    }

}
