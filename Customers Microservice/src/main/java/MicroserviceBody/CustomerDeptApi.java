package MicroserviceBody;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceReference;

public class CustomerDeptApi extends AbstractVerticle {
    @Override
    public void start(Future<Void> future) {
        ServiceDiscovery discovery = ServiceDiscovery.create(vertx);

        discovery.getRecord(new JsonObject().put("name", "customer-service"), ar -> {
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
