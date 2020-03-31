package MicroserviceBody;

import Models.Artist;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.servicediscovery.types.HttpEndpoint;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;



public class MusicVerticle extends AbstractVerticle {

    private static MySQLConnectOptions connectOptions;
    private static MySQLPool client;
    public ServiceDiscovery discovery;
    public Record musicRecord;
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MusicVerticle());
    }

    public MusicVerticle() {

        //Connect to the database
        connectOptions = new MySQLConnectOptions()
                .setPort(3308)
                .setHost("localhost")
                .setDatabase("artists")
                .setUser("root")
                .setPassword("123");

        //Pool options
        PoolOptions poolOptions = new PoolOptions();

        //Create the client pool
        client = MySQLPool.pool(connectOptions,poolOptions);
    }

    @Override
    public void start(Future<Void> future) {

        //Creating server and router
        Router router = Router.router(vertx);

        Route needThis = router.route("/api/musicApi/*").handler(BodyHandler.create());

        //GET
        Route info = router
                .get("/api/musicApi")
                .handler(this::Info);
        info.failureHandler(frc -> {
            int statusCode = frc.statusCode();
            HttpServerResponse response = frc.response();
            response.setStatusCode(statusCode).end("Sorry! Info unavailable");
        });

        //GET
        Route getId = router
                .get("/api/musicApi/selectArtist/:id")
                .handler(this::SelectOneArtist);
        getId.failureHandler(frc -> {
            int statusCode = frc.statusCode();
            HttpServerResponse response = frc.response();
            response.setStatusCode(statusCode).end("Sorry! GetId unavailable");
        });

        //POST
        Route post = router
                .post("/api/musicApi/createArtist")
                .handler(BodyHandler.create()).handler(this::CreateArtist);
        post.failureHandler(frc -> {
            int statusCode = frc.statusCode();
            HttpServerResponse response = frc.response();
            response.setStatusCode(statusCode).end("Sorry! Create unavailable");
        });

        //GET
        Route getAll = router
                .get("/api/musicApi/selectArtists")
                .handler(this::SelectAllArtists);
        getAll.failureHandler(frc -> {
            int statusCode = frc.statusCode();
            HttpServerResponse response = frc.response();
            response.setStatusCode(statusCode).end("Sorry! GetAll unavailable code:"+statusCode);
        });

        //DELETE
        Route delete = router
                .delete("/api/musicApi/deleteArtist/:id")
                .handler(this::DeleteArtist);
        delete.failureHandler(frc -> {
            int statusCode = frc.statusCode();
            HttpServerResponse response = frc.response();
            response.setStatusCode(statusCode).end("Sorry! Delete unavailable");
        });

        discovery = ServiceDiscovery.create(vertx,
                new ServiceDiscoveryOptions()
                        .setAnnounceAddress("service-announce")
                        .setName("MusicApi"));

        musicRecord = HttpEndpoint.createRecord(
                "musicApi", // The service name
                "localhost", // The host
                8082, // the port
                "/api/musicApi");// the root of the service

        HttpServer server = vertx.createHttpServer();
        server
                .requestHandler(router)
                .listen(8082, "localhost", ar -> {
                    if (ar.succeeded()) {
                        discovery.publish(musicRecord, arr -> {
                            if (ar.succeeded()) {
                                Record publishedRecord = arr.result();
                                System.out.println(publishedRecord.getName() + " is published");
                            } else {
                                System.out.println("Result: " +ar.result());
                            }
                        });
                        future.isComplete();
                        System.out.println("API Gateway is running on port " + 8082);
                    } else {
                        future.fail(ar.cause());
                    }
                });
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        discovery.unpublish(musicRecord.getRegistration(), ar -> {
            if (ar.succeeded()) {
                System.out.println("Service is successfully unpublished");
            } else {
                System.out.println("Result: " +ar.result());
            }
        });
    }


    private void Info(RoutingContext routingContext) {
        routingContext.response().end("<h1>MusicApi information<h1><br>" +
                "<h2>Delete artist: /api/musicApi/deleteArtist/:id<h2><br>" +
                "<h2>Get all artists: /api/musicApi/selectArtists<h2><br>" +
                "<h2>Create artist: /api/musicApi/createArtist<h2><br>" +
                "<h2>Get one specific artist: /api/musicApi/selectArtist/:id<h2>");
    }

    //------------------------------------------SELECT ONE----------------------------------------------//
    public void SelectOneArtist(RoutingContext routingContext) {
        client.query("SELECT * FROM artists",res->{
            if(res.succeeded()){
                Artist artist = new Artist();
                int id = Integer.parseInt(routingContext.request().getParam("id"));
                RowSet<Row> result = res.result();
                for (Row row : result) {
                    if(row.getInteger(0)==id) {
                        artist.setId(row.getInteger(0));
                        artist.setName(row.getString(1));
                    }
                }
                JsonObject jsonObject =JsonObject.mapFrom(artist);

                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .setStatusCode(200)
                        .end(jsonObject.encodePrettily());
            }else{
                System.out.println("Failure: " + res.cause().getMessage());
                client.close();
            }
        });
    }

    //------------------------------------------SELECT ALL Artists----------------------------------------------//
    public void SelectAllArtists(RoutingContext routingContext) {
        client.query("SELECT * FROM artists",res->{
            if(res.succeeded()){
                RowSet<Row> result = res.result();
                List<Artist> list = new ArrayList<>();
                JsonArray jsonHolidays = new JsonArray(list);
                for (Row row : result) {
                    Artist artist = new Artist();
                    artist.setId(row.getInteger(0));
                    artist.setName(row.getString(1));

                    list.add(artist);
                }
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .setStatusCode(200)
                        .end(jsonHolidays.encodePrettily());
            }else{
                System.out.println("Failure: " + res.cause().getMessage());
                client.close();
            }
        });
    }

    //------------------------------------------DELETE----------------------------------------------//
    public void DeleteArtist(RoutingContext routingContext){
        int id = Integer.parseInt(routingContext.request().getParam("id"));
        client.query("DELETE FROM Artist WHERE artist_id='"+id+"';", res -> {
            if (res.succeeded()) {
                routingContext.response().setStatusCode(200).end("Artist of id: "+id+" deleted");
            } else {
                System.out.println("Failure: " + res.cause().getMessage());
                client.close();
            }
        });
    }

    //------------------------------------------Create----------------------------------------------//
    public void CreateArtist(RoutingContext routingContext){
        final Artist artist = Json.decodeValue(routingContext.getBody(),Artist.class);
        int idU = artist.getId();
        String place = artist.getName();

        client.preparedQuery("insert into holidays (artist_id,name values (?, ?)", Tuple.of(idU,place), res -> {
            if (res.succeeded()) {
                routingContext.response().setStatusCode(200).end("Holiday on: "+idU+" created");
            } else {
                System.out.println("Failure: " + res.cause().getMessage());
                client.close();
            }
        });
    }
}
