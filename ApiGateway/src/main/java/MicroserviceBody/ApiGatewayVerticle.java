package MicroserviceBody;
import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpClient;
import io.vertx.reactivex.core.http.HttpClientRequest;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import io.vertx.reactivex.ext.web.handler.BodyHandler;

import static com.sun.xml.internal.ws.spi.db.BindingContextFactory.LOGGER;

public class ApiGatewayVerticle extends AbstractVerticle {
    private String com[]={"/api/customerApi/customers","/api/musicApi/selectArtists"};

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new ApiGatewayVerticle());
    }

    @Override
    public void start(Future<Void> future) throws Exception{
        super.start();

        Router router = Router.router(vertx);

        //Body Handler
        router.route("/*").handler(BodyHandler.create());

        // api gateway dispatch
        router.route("/api/*").handler(this::dispatchOneRequests);

        router.route("/both/SelectAll").handler(this::dispatchBothSelectAll);

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


    //---------Just single Http Request---------- Test2 -------
    private void dispatchOneRequests(RoutingContext routingContext) {
        String path = routingContext.normalisedPath();
        if(path.contains("customerApi")){
            doDispatchOne(routingContext,8081,path);
        }else if(path.contains("musicApi")){
            doDispatchOne(routingContext,8080,path);
        }
    }

    private void doDispatchOne(RoutingContext routingContext, int port, String command) {
        WebClient client = WebClient.create(vertx);
        if(command.contains("delete")){
            Single<HttpResponse<Buffer>> delete = client
                    .delete(port, "localhost", command)
                    .rxSend();
            delete.subscribe(resp-> routingContext.response().end("Deleted successfully"));
        }else if(command.contains("create")){
            Single<HttpResponse<Buffer>> post = client
                    .post(port, "localhost", command)
                    .rxSend();
            post.subscribe(resp-> routingContext.response().end("Creation successful"));
        }else {
            Single <HttpResponse<JsonArray>> get = client
                    .get(port, "localhost", command)
                    .as(BodyCodec.jsonArray())
                    .rxSend();
            get.subscribe(resp-> routingContext.response().end(resp.body().encodePrettily()));

        }
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
        HttpClient client = vertx.createHttpClient();

        HttpClientRequest req1 = client.request(HttpMethod.GET, 8080, "localhost", com[0]);
        HttpClientRequest req2 = client.request(HttpMethod.GET, 8081, "localhost", com[1]);

        Flowable<JsonArray> obs1 = req1.toFlowable().flatMap(element->element.toFlowable()).
                map(buffer -> new JsonArray(buffer.toString("UTF-8")));
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


