package MicroserviceBody;

import Models.Customer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
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
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;

import static com.sun.xml.internal.ws.spi.db.BindingContextFactory.LOGGER;

public class DatabaseVerticle extends AbstractVerticle {
    private static MySQLConnectOptions connectOptions;
    private static MySQLPool client;

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new DatabaseVerticle());
    }

    public DatabaseVerticle(){

        //Connect to the database
        connectOptions = new MySQLConnectOptions()
                .setPort(3309)
                .setHost("localhost")
                .setDatabase("customers")
                .setUser("root")
                .setPassword("123");


        //Pool options
        PoolOptions poolOptions = new PoolOptions();

        //Create the client pool
        client = MySQLPool.pool(connectOptions,poolOptions);
    }

    @Override
    public void start(Future<Void> startFuture) {

        //Create server and router
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);

        Route needThis = router.route("/api/customerApi/*").handler(BodyHandler.create());

        //GET
        Route info = router
                .get("/api/customerApi")
                .handler(this::Info);
        info.failureHandler(frc -> {
            int statusCode = frc.statusCode();
            HttpServerResponse response = frc.response();
            response.setStatusCode(statusCode).end("Sorry! Info unavailable");
        });

        //GET
        Route getId = router
                .get("/api/customerApi/customer/:id")
                .handler(this::SelectOne);
        getId.failureHandler(frc -> {
            int statusCode = frc.statusCode();
            HttpServerResponse response = frc.response();
            response.setStatusCode(statusCode).end("Sorry! GetId unavailable");
        });

        //POST
        Route post = router
                .post("/api/customerApi/customerCreate")
                .handler(this::CreateCustomer);
        post.failureHandler(frc -> {
            int statusCode = frc.statusCode();
            HttpServerResponse response = frc.response();
            response.setStatusCode(statusCode).end("Sorry! Create unavailable");
        });

        //GET
        Route getAll = router
                .get("/api/customerApi/customers")
                .handler(this::SelectAllCustomers);
        getAll.failureHandler(frc -> {
            int statusCode = frc.statusCode();
            HttpServerResponse response = frc.response();
            response.setStatusCode(statusCode).end("Sorry! GetAll unavailable code:" + statusCode);
        });

        //GET
        Route getLimit = router
                .get("/api/customerApi/customers/limit1/:id1/limit2/:id2")
                .handler(this::SelectNumberOfCustomers);
        getLimit.failureHandler(frc -> {
            int statusCode = frc.statusCode();
            HttpServerResponse response = frc.response();
            response.setStatusCode(statusCode).end("Sorry! GetLimit unavailable");
        });

        //DELETE
        Route delete = router
                .delete("/api/customerApi/customerDelete/:id")
                .handler(this::DeleteCustomer);
        delete.failureHandler(frc -> {
            int statusCode = frc.statusCode();
            HttpServerResponse response = frc.response();
            response.setStatusCode(statusCode).end("Sorry! Delete unavailable");
        });

        //Server config
        server.requestHandler(router).listen(8081, asyncResult -> {
            if (asyncResult.succeeded()) {
                LOGGER.info("Api running successfully on port 8081");
            } else {
                LOGGER.info("Error : " + asyncResult.cause());
            }
        });

    }
        private void Info(RoutingContext routingContext) {
            routingContext.response().end("<h1>CustomerApi information<h1><br>" +
                    "<h2>Delete user: /api/customerApi/customerDelete/:id<h2><br>" +
                    "<h2>Get limited number of users: /api/customerApi/customers/limit1/:id1/limit2/:id2<h2><br>" +
                    "<h2>Get all users: /api/customerApi/customers<h2><br>" +
                    "<h2>Create user: /api/customerApi/customerCreate<h2><br>" +
                    "<h2>Get one specific user: /api/customerApi/customer/:id<h2>");
        }

        //------------------------------------------DELETE----------------------------------------------//
        public void DeleteCustomer(RoutingContext routingContext){
            int id = Integer.parseInt(routingContext.request().getParam("id"));
            client.query("DELETE FROM customers WHERE customer_id="+id, res -> {
                if (res.succeeded()) {
                    routingContext.response().setStatusCode(200).end("User of id: "+id+" deleted");
                } else {
                    System.out.println("Failure: " + res.cause().getMessage());
                    client.close();
                }
            });
        }

        //------------------------------------------SELECT ONE----------------------------------------------//
        public void SelectOne(RoutingContext routingContext){
            client.query("SELECT * FROM customers", res->{
                if(res.succeeded()){
                    int id = Integer.parseInt(routingContext.request().getParam("id"));
                    Customer customer = new Customer();
                    RowSet<Row> result = res.result();
                    for (Row row : result) {
                        if(row.getInteger(0)==id) {
                            customer.setName(row.getString(1));
                            customer.setLastName(row.getString(2));
                            customer.setAge(row.getInteger(3));
                            customer.setPhoneNumber(row.getString(4));
                            customer.setId(row.getInteger(0));
                        }
                    }
                    JsonObject jsonObject =JsonObject.mapFrom(customer);
                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .setStatusCode(200)
                            .end(jsonObject.encodePrettily());
                }else{
                    System.out.println("fail");
                    client.close();
                }
            });
        }
        //------------------------------------------SELECT ALL Customers----------------------------------------------//
        public void SelectAllCustomers(RoutingContext routingContext) {

            client.query("SELECT * FROM customers",res->{
                if(res.succeeded()){
                    List<Customer> customers = new ArrayList<>();
                    JsonArray jsonUsers = new JsonArray(customers);
                    RowSet<Row> result = res.result();
                    for (Row row : result) {
                        Customer customer = new Customer();
                        customer.setName(row.getString(1));
                        customer.setLastName(row.getString(2));
                        customer.setAge(row.getInteger(3));
                        customer.setPhoneNumber(row.getString(4));
                        customer.setId(row.getInteger(0));
                        customers.add(customer);
                    }
                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .setStatusCode(200)
                            .end(jsonUsers.encodePrettily());
                }else{
                    System.out.println("Failure: " + res.cause().getMessage());
                    client.close();
                }
            });
        }
        //------------------------------------------SELECT LIMIT----------------------------------------------//
        public void SelectNumberOfCustomers(RoutingContext routingContext){
            int id1 = Integer.parseInt(routingContext.request().getParam("id1"));
            int id2 = Integer.parseInt(routingContext.request().getParam("id2"));
            client.query("SELECT * FROM customers LIMIT "+id1+","+id2, res->{
                if(res.succeeded()){
                    List<Customer> users = new ArrayList<>();
                    JsonArray jsonUsers = new JsonArray(users);
                    RowSet<Row> result = res.result();
                    for (Row row : result) {
                        Customer customer = new Customer();
                        customer.setName(row.getString(1));
                        customer.setLastName(row.getString(2));
                        customer.setAge(row.getInteger(3));
                        customer.setPhoneNumber(row.getString(4));
                        customer.setId(row.getInteger(0));
                        users.add(customer);
                    }
                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .setStatusCode(200)
                            .end(jsonUsers.encodePrettily());
                }else{
                    System.out.println("fail");
                    client.close();
                }
            });
        }
        //------------------------------------------Create----------------------------------------------//
        public void CreateCustomer(RoutingContext routingContext){
            final Customer user = Json.decodeValue(routingContext.getBody(),Customer.class);
            String name = user.getName();
            String phone_number = user.getPhoneNumber();
            client.preparedQuery("INSERT INTO customers (name, phone_number) VALUES (?, ?)", Tuple.of(name, phone_number), res -> {
                if (res.succeeded()) {
                    routingContext.response().setStatusCode(200).end("User : "+name+" created");
                } else {
                    System.out.println("Failure: " + res.cause().getMessage());
                    client.close();
                }
            });
        }


}
