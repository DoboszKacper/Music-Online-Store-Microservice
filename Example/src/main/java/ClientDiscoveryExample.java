import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceReference;
import io.vertx.servicediscovery.types.HttpEndpoint;
import sun.net.www.http.HttpClient;

import java.io.IOException;
import java.sql.SQLOutput;

public class ClientDiscoveryExample extends AbstractVerticle {

    @Override
    public void start() {
        ServiceDiscovery discovery = ServiceDiscovery.create(vertx);

        discovery.getRecord(new JsonObject().put("name", "example-rest-api"), ar -> {
            if (ar.succeeded() && ar.result() != null) {
                // Retrieve the service reference
                ServiceReference reference = discovery.getReference(ar.result());
                // Retrieve the service object
                WebClient client = reference.getAs(WebClient.class);

                // You need to path the complete path
                client.get("/").send(
                        response -> {

                            System.out.println(response.result().body());

                            // Dont' forget to release the service
                            reference.release();

                        });
            }
        });
    }
}
