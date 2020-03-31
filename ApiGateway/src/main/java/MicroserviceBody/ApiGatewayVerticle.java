package MicroserviceBody;
import io.reactivex.*;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.circuitbreaker.CircuitBreaker;
import io.vertx.reactivex.circuitbreaker.HystrixMetricHandler;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Promise;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpClient;
import io.vertx.reactivex.core.http.HttpClientRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceReference;
import io.vertx.servicediscovery.types.HttpEndpoint;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.sun.xml.internal.ws.spi.db.BindingContextFactory.LOGGER;

public class ApiGatewayVerticle extends AbstractVerticle {

    private String com[]={"/api/customerApi/customers","/api/musicApi/selectArtists"};
    private CircuitBreaker breaker;
    private ServiceDiscovery discovery;

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new ApiGatewayVerticle());
    }

    @Override
    public void start(Future<Void> future){
        breaker = CircuitBreaker.create("my-circuit-breaker", vertx,
                new CircuitBreakerOptions()
                        .setMaxFailures(5) // number of failure before opening the circuit
                        .setTimeout(2000) // consider a failure if the operation does not succeed in time
                        .setFallbackOnFailure(true) // do we call the fallback on failure
                        .setResetTimeout(10000))
                .closeHandler(v-> System.out.println("Circuit opened"))
                .closeHandler(v-> System.out.println("Circuit closed"));

        Router router = Router.router(vertx);


                    //Body Handler
                    router.route("/*").handler(BodyHandler.create());

                    // api gateway dispatch
                     router.route("/api/selectDiscover").handler(this::OneDiscover);

                    // api gateway dispatch
                    //router.route("/api/*").handler(this::dispatchOneRequests);

                    // api dispatch both
                    router.route("/both/selectAll").handler(this::dispatchBothSelectAll);

                    // api info
                    router.route("/api").handler(this::Info);
                
        // create http server
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8888,"localhost", ar -> {
                    if (ar.succeeded()) {
                        future.isComplete();
                        LOGGER.info("API Gateway is running on port " + 8888);

                    } else {
                        future.failed();
                    }
                });
    }

    private void OneDiscover(RoutingContext context) {
        discovery.getRecord(
                new JsonObject().put("name", "customer-service"), found -> {
                    if(found.succeeded()) {
                        Record match = found.result();
                        ServiceReference reference = discovery.getReference(match);
                        HttpClient client = reference.get();

                        client.getNow("/hello", response ->
                                response.bodyHandler(
                                        body ->
                                                System.out.println(body.toString())));
                    }
                });
    }

    //---------Just single Http Request
    private void dispatchOneRequests(RoutingContext routingContext) {
        String path = routingContext.normalisedPath();
        if(path.contains("customerApi")){
            doDispatchOne(routingContext,8081,path);
        }else if(path.contains("musicApi")){
            doDispatchOne(routingContext,8082,path);
        }
    }

    private void doDispatchOne(RoutingContext routingContext, int port, String command) {
        breaker.executeWithFallback(future-> {
            WebClient client = WebClient.create(vertx);
            if (command.contains("delete")) {
                Single<HttpResponse<Buffer>> delete = client
                        .delete(port, "localhost", command)
                        .rxSend();
                delete.subscribe(resp -> {
                    if(resp.statusCode()!=200){
                        future.fail("HTTP error");
                    }else {
                        routingContext.response().end("Deleted successfully");
                        future.complete();
                    }
                });
            } else if (command.contains("create")) {
                Single<HttpResponse<Buffer>> post = client
                        .post(port, "localhost", command)
                        .rxSend();
                post.subscribe(resp -> {
                    if(resp.statusCode()!=200){
                        future.fail("HTTP error");
                    }else {
                        routingContext.response().end("Creation successful");
                        future.complete();
                    }
                });
            } else {
                Single<HttpResponse<JsonArray>> get = client
                        .get(port, "localhost", command)
                        .as(BodyCodec.jsonArray())
                        .rxSend();
                get.subscribe(resp -> {
                    if(resp.statusCode()!=200){
                        future.fail("HTTP error");
                    }else {
                        routingContext.response().end(resp.body().encodePrettily());
                        future.complete();
                    }
                });
            }
        },v->{
                return "Test1";
            },ar->{
                System.out.println("Result: " + ar.result());
            });
    }



    private void Info(RoutingContext routingContext) {
        routingContext.response().end(
                "<h1><font color=\"red\">BOTH Tests</font></h1><br>" +
                "Test 4: both/Test4 <font color=\"green\"> Dose Work! </font><br>");
    }

    @Override
    public void stop() {
        LOGGER.info("API Gateway stopped working");
    }


    public void dispatchBothSelectAll(RoutingContext context){
        AtomicBoolean port_8081= new AtomicBoolean(false);
        boolean port_8082=false;
        HttpClient client = vertx.createHttpClient();

        HttpClientRequest req1 = client.request(HttpMethod.GET, 8081, "localhost", com[0]);
        Flowable<JsonArray> obs1 = req1.toFlowable().flatMap(element->element.toFlowable()).
                map(buffer -> new JsonArray(buffer.toString("UTF-8")));

        HttpClientRequest req2 = client.request(HttpMethod.GET, 8082, "localhost", com[1]);
        Flowable<JsonArray> obs2 = req2.toFlowable().flatMap(element->element.toFlowable()).
                map(buffer -> new JsonArray(buffer.toString("UTF-8")));

        obs1.zipWith(obs2, (b1, b2) -> new JsonObject()
                .put("Request 1: ", b1)
                .put("Request 2: ", b2))
                .subscribe(json -> context.response().end(json.encodePrettily()),
                        Throwable::printStackTrace);
        req1.end();
        req2.end();
    }
}


