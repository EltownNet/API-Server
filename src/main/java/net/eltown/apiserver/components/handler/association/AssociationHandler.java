package net.eltown.apiserver.components.handler.association;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Handler;

public class AssociationHandler extends Handler<AssociationProvider> {


    @SneakyThrows
    public AssociationHandler(final Server server) {
        super(server, "AssociationHandler", new AssociationProvider(server));
        this.startCallbacking();
    }

    private void startCallbacking() {
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().receive(delivery -> {
                final String[] d = delivery.getData();
                switch (AssociationCalls.valueOf(delivery.getKey().toUpperCase())) {

                }
            }, "API/Associations[Receive]", "api.associations.receive");
        });
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().callback(request -> {
                final String[] d = request.getData();
                switch (AssociationCalls.valueOf(request.getKey().toUpperCase())) {

                }
            }, "API/Associations[Callback]", "api.associations.callback");
        });
    }

}
