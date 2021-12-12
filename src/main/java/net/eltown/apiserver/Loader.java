package net.eltown.apiserver;

import net.eltown.apiserver.components.data.Colors;

public class Loader {

    private static Server server;
    private static Internal internal;
    private static String version = "API Alpha2 1.0.0.0";

    public static void main(String[] args) {
        server = new Server();
        server.start();

        server.log("\n"+Colors.ANSI_WHITE + "///////// " + Colors.ANSI_CYAN + version + Colors.ANSI_WHITE + " /////////"
                + "\n" + "Server: " + Colors.ANSI_GREEN + "OK" +
                "\n" + Colors.ANSI_WHITE + "///////// " + Colors.ANSI_CYAN + version + Colors.ANSI_WHITE + " /////////");

        internal = new Internal();
        internal.init();

        System.exit(0);

    }

}
