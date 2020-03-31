import io.vertx.core.Vertx;

public class Starter {

    public static void main(String[] args) {
        Vertx v = Vertx.vertx();
        v.deployVerticle(new ServerDiscoveryExample(), result->{
            v.deployVerticle(new ClientDiscoveryExample());
        });
    }
}
