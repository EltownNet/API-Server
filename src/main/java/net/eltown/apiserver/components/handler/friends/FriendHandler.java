package net.eltown.apiserver.components.handler.friends;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Handler;
import net.eltown.apiserver.components.handler.friends.data.FriendData;

public class FriendHandler extends Handler<FriendProvider> {

    @SneakyThrows
    public FriendHandler(final Server server) {
        super(server, "FriendHandler", new FriendProvider(server));
        this.startCallbacking();
    }

    private void startCallbacking() {
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().receive(delivery -> {
                final String[] d = delivery.getData();
                switch (FriendCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case REQUEST_CREATE_FRIEND_DATA:
                        if (!this.getProvider().friendDataExists(d[1])) this.getProvider().createFriendData(d[1]);
                        break;
                    case REQUEST_CREATE_FRIEND_REQUEST:
                        this.getProvider().createFriendRequest(d[1], d[2]);
                        break;
                    case REQUEST_REMOVE_FRIEND_REQUEST:
                        this.getProvider().removeFriendRequest(d[1], d[2]);
                        break;
                    case REQUEST_CREATE_FRIENDSHIP:
                        this.getProvider().createFriendship(d[1], d[2]);
                        break;
                    case REQUEST_REMOVE_FRIENDSHIP:
                        this.getProvider().removeFriendship(d[1], d[2]);
                        break;
                }
            }, "API/Friends[Receive]", "api.friends.receive");
        });
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().callback(request -> {
                final String[] d = request.getData();
                switch (FriendCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_CHECK_ARE_FRIENDS -> request.answer(FriendCalls.CALLBACK_ARE_FRIENDS.name(), String.valueOf(this.getProvider().areFriends(d[1], d[2])));
                    case REQUEST_CHECK_REQUEST_EXISTS -> request.answer(FriendCalls.CALLBACK_REQUEST_EXISTS.name(), String.valueOf(this.getProvider().requestExists(d[1], d[2])));
                    case REQUEST_FRIEND_DATA -> {
                        if (!this.getProvider().friendDataExists(d[1])) {
                            request.answer(FriendCalls.CALLBACK_NULL.name(), "null");
                            return;
                        }
                        final FriendData data = this.getProvider().cachedFriendData.get(d[1]);
                        final StringBuilder friendBuilder = new StringBuilder();
                        data.getFriends().forEach(e -> {
                            friendBuilder.append(e).append(":");
                        });
                        if (friendBuilder.toString().isEmpty()) friendBuilder.append("null:");
                        final String friends = friendBuilder.substring(0, friendBuilder.length() - 1);
                        final StringBuilder requestBuilder = new StringBuilder();
                        data.getRequests().forEach(e -> {
                            requestBuilder.append(e).append(":");
                        });
                        if (requestBuilder.toString().isEmpty()) requestBuilder.append("null:");
                        final String requests = requestBuilder.substring(0, requestBuilder.length() - 1);
                        request.answer(FriendCalls.CALLBACK_FRIEND_DATA.name(), friends, requests);
                    }
                }
            }, "API/Friends[Callback]", "api.friends.callback");
        });
    }
}
