package net.eltown.apiserver.components;

import lombok.Getter;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.data.Colors;
import net.eltown.apiserver.components.data.LogLevel;
import net.eltown.apiserver.components.handler.economy.EconomyProvider;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbit;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

@Getter
public class Handler<T extends Provider> {

    private final Server server;
    private final String name;
    private final T provider;
    private final TinyRabbitListener tinyRabbitListener;
    private TinyRabbit tinyRabbitClient;

    public Handler(final Server server, final String name, final T provider) {
        this.server = server;
        this.name = name;
        this.server.log(LogLevel.DEBUG, "Starte " + name + "...");
        this.provider = provider;
        this.tinyRabbitListener = new TinyRabbitListener("localhost");
        this.tinyRabbitListener.throwExceptions(true);
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

    public void onDisable() {
        this.getServer().log(LogLevel.HIGH, Colors.ANSI_RED + this.getClass().getSimpleName() + "::onDisable" + Colors.ANSI_RESET);
    }

}
