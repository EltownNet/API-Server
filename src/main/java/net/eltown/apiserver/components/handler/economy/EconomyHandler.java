package net.eltown.apiserver.components.handler.economy;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Handler;

public class EconomyHandler extends Handler<EconomyProvider> {

    @SneakyThrows
    public EconomyHandler(final Server server) {
        super(server, "EconomyHandler", new EconomyProvider(server));
        this.startCallbacking();
    }

    public void startCallbacking() {
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().callback((request -> {
                switch (EconomyCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_GETMONEY -> request.answer(EconomyCalls.CALLBACK_MONEY.name(), String.valueOf(this.getProvider().get(request.getData()[1])));
                    case REQUEST_SETMONEY -> {
                        this.getProvider().set(request.getData()[1], Double.parseDouble(request.getData()[2]));
                        request.answer(EconomyCalls.CALLBACK_NULL.name());
                    }
                    case REQUEST_ACCOUNTEXISTS -> request.answer(EconomyCalls.CALLBACK_ACCOUNTEXISTS.name(), String.valueOf(this.getProvider().has(request.getData()[1])));
                    case REQUEST_CREATEACCOUNT -> {
                        this.getProvider().create(request.getData()[1], Double.parseDouble(request.getData()[2]));
                        request.answer(EconomyCalls.CALLBACK_NULL.name());
                    }
                    case REQUEST_GETALL -> request.answer(EconomyCalls.CALLBACK_GETALL.name(), this.getProvider().getAll().toArray(new String[0]));
                }
            }), "API/Economy[Callback]", "api.economy.callback");
        });
    }

}
