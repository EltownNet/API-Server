package net.eltown.apiserver.components.handler.ticketsystem;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Handler;
import net.eltown.apiserver.components.Provider;
import net.eltown.apiserver.components.handler.ticketsystem.data.Ticket;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

import java.util.LinkedHashSet;
import java.util.LinkedList;

public class TicketHandler extends Handler<TicketProvider> {

    @SneakyThrows
    public TicketHandler(final Server server) {
        super(server, "TicketHandler", new TicketProvider(server));
        this.startCallbacking();
    }

    public void startCallbacking() {
        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().receive(delivery -> {
                final String[] d = delivery.getData();
                switch (TicketCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case REQUEST_TAKE_TICKET -> {
                        final Ticket ticket1 = this.getProvider().getTicket(d[2]);
                        if (ticket1.getSupporter().equals("null")) {
                            this.getProvider().setTicketSupporter(d[2], d[1]);
                        }
                    }
                    case REQUEST_SEND_MESSAGE -> this.getProvider().addNewTicketMessage(d[3], d[1], d[2]);
                    case REQUEST_SET_PRIORITY -> this.getProvider().setTicketPriority(d[3], d[2]);
                    case REQUEST_CLOSE_TICKET -> this.getProvider().closeTicket(d[2]);
                }
            }, "API/Ticketsystem[Receive]", "api.ticketsystem.receive");
        });

        this.getServer().getExecutor().execute(() -> {
            this.getTinyRabbitListener().callback((request -> {
                final String[] d = request.getData();
                switch (TicketCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_OPEN_TICKET -> this.getProvider().getTickets(d[1], tickets -> {
                        if (tickets.size() >= 5) {
                            request.answer(TicketCalls.CALLBACK_TOO_MANY_TICKETS.name(), "null");
                        } else {
                            this.getProvider().createTicket(d[1], d[2], d[3], d[4], d[5]);
                            request.answer(TicketCalls.CALLBACK_NULL.name(), "null");
                        }
                    });
                    case REQUEST_MY_TICKETS -> this.getProvider().getTickets(d[1], tickets -> {
                        if (tickets.size() == 0) {
                            request.answer(TicketCalls.CALLBACK_NO_TICKETS.name(), "null");
                        } else {
                            final LinkedList<String> list = new LinkedList<>();
                            for (Ticket e : tickets) {
                                final StringBuilder builder = new StringBuilder();
                                e.getMessages().forEach(f -> {
                                    builder.append(f).append("~~~");
                                });
                                final String messages = builder.substring(0, builder.length() - 3);
                                list.add(e.getCreator() + ">>" + e.getSupporter() + ">>" + e.getId() + ">>" + e.getSubject() + ">>" + e.getSection() + ">>" + e.getPriority() + ">>" + messages + ">>" + e.getDateOpened() + ">>" + e.getDateClosed());
                            }
                            request.answer(TicketCalls.CALLBACK_MY_TICKETS.name(), list.toArray(new String[0]));
                        }
                    });
                    case REQUEST_OPEN_TICKETS -> {
                        final LinkedHashSet<Ticket> tickets = this.getProvider().getOpenTickets();
                        if (tickets.size() == 0) {
                            request.answer(TicketCalls.CALLBACK_OPEN_TICKETS.name(), "null");
                        } else {
                            final LinkedList<String> list = new LinkedList<>();
                            for (Ticket e : tickets) {
                                final StringBuilder builder = new StringBuilder();
                                e.getMessages().forEach(f -> {
                                    builder.append(f).append("~~~");
                                });
                                final String messages = builder.substring(0, builder.length() - 3);
                                list.add(e.getCreator() + ">>" + e.getSupporter() + ">>" + e.getId() + ">>" + e.getSubject() + ">>" + e.getSection() + ">>" + e.getPriority() + ">>" + messages + ">>" + e.getDateOpened() + ">>" + e.getDateClosed());
                            }
                            request.answer(TicketCalls.CALLBACK_OPEN_TICKETS.name(), list.toArray(new String[0]));
                        }
                    }
                    case REQUEST_MY_SUPPORT_TICKETS -> {
                        final LinkedHashSet<Ticket> mySupportTickets = this.getProvider().getMySupportTickets(d[1]);
                        if (mySupportTickets.size() == 0) {
                            request.answer(TicketCalls.CALLBACK_MY_SUPPORT_TICKETS.name(), "null");
                        } else {
                            final LinkedList<String> list = new LinkedList<>();
                            for (Ticket e : mySupportTickets) {
                                final StringBuilder builder = new StringBuilder();
                                e.getMessages().forEach(f -> {
                                    builder.append(f).append("~~~");
                                });
                                final String messages = builder.substring(0, builder.length() - 3);
                                list.add(e.getCreator() + ">>" + e.getSupporter() + ">>" + e.getId() + ">>" + e.getSubject() + ">>" + e.getSection() + ">>" + e.getPriority() + ">>" + messages + ">>" + e.getDateOpened() + ">>" + e.getDateClosed());
                            }
                            request.answer(TicketCalls.CALLBACK_MY_SUPPORT_TICKETS.name(), list.toArray(new String[0]));
                        }
                    }
                }
            }), "API/Ticketsystem[Callback]", "api.ticketsystem.callback");
        });
    }

}

