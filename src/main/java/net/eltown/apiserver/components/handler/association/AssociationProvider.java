package net.eltown.apiserver.components.handler.association;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.Provider;
import net.eltown.apiserver.components.handler.association.data.Association;
import org.bson.Document;

import java.util.HashMap;

public class AssociationProvider extends Provider {

    public final HashMap<String, Association> cachedAssociations = new HashMap<>();

    @SneakyThrows
    public AssociationProvider(final Server server) {
        super(server, "associations");

        server.log("Vereine werden in den Cache geladen...");
        for (final Document document : this.getCollection("associations").find()) {

        }
        server.log(this.cachedAssociations.size() + " Vereine wurden in den Cache geladen...");
    }

}
