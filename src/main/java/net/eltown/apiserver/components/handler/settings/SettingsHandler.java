package net.eltown.apiserver.components.handler.settings;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Handler;

public class SettingsHandler extends Handler<SettingsProvider> {

    @SneakyThrows
    public SettingsHandler(final Server server) {
        super(server, "SettingsHandler", new SettingsProvider(server));
        this.startCallbacking();
    }

    private void startCallbacking() {
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().receive(delivery -> {
                final String[] d = delivery.getData();
                switch (SettingsCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case REQUEST_UPDATE_SETTINGS -> this.getProvider().updateEntry(d[1], d[2], d[3]);
                    case REQUEST_REMOVE_SETTINGS -> this.getProvider().removeEntry(d[1], d[2]);
                    case REQUEST_UPDATE_ALL -> this.getProvider().updateAll(d[1], d[2]);
                }
            }, "API/Settings[Receive]", "api.settings.receive");
        });
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().callback(request -> {
                final String[] d = request.getData();
                switch (SettingsCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_SETTINGS -> {
                        if (!this.getProvider().cachedSettings.containsKey(d[1])) this.getProvider().createAccountSettings(d[1]);
                        final StringBuilder builder = new StringBuilder();
                        this.getProvider().cachedSettings.get(d[1]).getSettings().forEach((key, value) -> {
                            builder.append(key).append(":").append(value).append(">:<");
                        });
                        if (builder.toString().isEmpty()) builder.append("null>:<");
                        final String settings = builder.substring(0, builder.length() - 3);
                        request.answer(SettingsCalls.CALLBACK_SETTINGS.name(), settings);
                    }
                    case REQUEST_ENTRY -> request.answer(SettingsCalls.CALLBACK_ENTRY.name(), this.getProvider().getEntry(d[1], d[2], d[3]));
                }
            }, "API/Settings[Callback]", "api.settings.callback");
        });
    }
}
