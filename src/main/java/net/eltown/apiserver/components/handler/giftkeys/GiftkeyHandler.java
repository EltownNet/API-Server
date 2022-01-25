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
                    case REQUEST_DELETE_KEY -> this.getProvider().deleteKey(d[1]);
                }
            }, "API/Giftkeys[Receive]", "api.giftkeys.receive");
        });

        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().callback((request -> {
                final String[] d = request.getData();
                switch (GiftkeyCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_CREATE_KEY:
                        final String key = d[1];
                        if (!this.getProvider().keyExists(key)) {
                            final int maxUses = Integer.parseInt(d[2]);
                            final List<String> rewards = Arrays.asList(d[3].split(">:<"));
                            final List<String> marks = Arrays.asList(d[4].split(">:<"));
                            final long duration = Long.parseLong(d[5]);
                            this.getProvider().createKey(key, maxUses, rewards, marks, duration, s -> {
                                request.answer(GiftkeyCalls.CALLBACK_NULL.name(), s);
                            });
                        } else request.answer(GiftkeyCalls.CALLBACK_GIFTKEY_ALREADY_EXISTS.name(), "null");
                        break;
                    case REQUEST_GET_KEY:
                        final String key1 = d[1];
                        if (!this.getProvider().keyExists(key1)) {
                            request.answer(GiftkeyCalls.CALLBACK_NULL.name(), "null");
                            return;
                        }
                        final Giftkey giftkey = this.getProvider().getGiftKey(key1);
                        final StringBuilder builder = new StringBuilder(key1).append(">>").append(giftkey.getMaxUses()).append(">>").append(giftkey.getDuration()).append(">>");

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
                            final Giftkey giftkey1 = this.getProvider().getGiftKey(redeemKey);
                            if (!this.getProvider().alreadyRedeemed(redeemKey, player)) {
                                if (giftkey1.getDuration() == -1 || giftkey1.getDuration() > System.currentTimeMillis()) {
                                    this.getProvider().redeemKey(redeemKey, player);

                                    final StringBuilder redeemBuilder = new StringBuilder();
                                    giftkey1.getRewards().forEach(e -> {
                                        redeemBuilder.append(e).append(">:<");
                                    });
                                    final String redeemRewards = redeemBuilder.substring(0, redeemBuilder.length() - 3);
                                    request.answer(GiftkeyCalls.CALLBACK_REDEEMED.name(), redeemRewards);
                                } else {
                                    request.answer(GiftkeyCalls.CALLBACK_CODE_EXPIRED.name(), "null");
                                    this.getProvider().deleteKey(giftkey1.getKey());
                                }
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
