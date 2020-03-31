import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.HttpEndpoint;

import javax.xml.ws.spi.http.HttpExchange;

public class ServerDiscoveryExample extends AbstractVerticle {



    @Override
    public void start(){
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.route("/").handler(this::Hello);
        server.requestHandler(router).listen(8888);


        ServiceDiscovery discovery = ServiceDiscovery.create(vertx);

        Record record = HttpEndpoint.createRecord("example-rest-api","localhost",8888,"/api");
        discovery.publish(record,ar->{
            if(ar.succeeded()){
                Record publishedRecord =ar.result();
            }else {
                System.out.println("publishing fail");
            }
        });
    }

    private void Hello(RoutingContext routingContext) {
        routingContext.response().end("hello boy");
    }

}
