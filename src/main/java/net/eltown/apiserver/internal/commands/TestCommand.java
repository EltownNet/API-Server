package net.eltown.apiserver.internal.commands;

import net.eltown.apiserver.internal.Command;

public class TestCommand extends Command {

    public TestCommand() {
        super("test");
    }

    @Override
    public void execute(String[] args) {
        this.getServer().log("Hello");
    }
}
