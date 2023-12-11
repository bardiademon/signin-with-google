package com.bardiademon.google.signin;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;

import java.util.function.Function;

public class GoogleResponseHandler extends AbstractVerticle {
    private final static System.Logger logger = System.getLogger(GoogleResponseHandler.class.getSimpleName());

    private final String host;
    private final int port;
    private final Function<String, Void> function;

    private HttpServer httpServer;

    public GoogleResponseHandler(final String host, final int port, final Function<String, Void> function) {
        this.host = host;
        this.port = port;
        this.function = function;
        Vertx.vertx().deployVerticle(this);
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        final HttpServerOptions serverOptions = new HttpServerOptions()
                .setHost(host)
                .setPort(port);

        httpServer = vertx.createHttpServer(serverOptions).requestHandler(request -> {

            System.out.println("request.params() = " + request.params());
            System.out.println("request.response().getStatusCode() = " + request.response().getStatusCode());

            if (request.response().getStatusCode() == 200 && request.params().contains("code")) {
                final String code = request.params().get("code");
                request.response()
                        .putHeader("content-type", "text/html")
                        .end("<h1>Successfully</h1>");
                function.apply(code);
            } else {
                request.response()
                        .putHeader("content-type", "text/html")
                        .end("<h1>Fail</h1>");
                function.apply(null);
            }
            die();
        });

        httpServer.listen(serverOptions.getPort()).onSuccess(httpServer -> {
            logger.log(System.Logger.Level.INFO, "Successfully run http server on " + serverOptions.getHost() + ":" + serverOptions.getPort());
            startPromise.complete();
        }).onFailure(fail -> {
            logger.log(System.Logger.Level.ERROR, "Fail to run http server on " + serverOptions.getHost() + ":" + serverOptions.getPort(), fail);
            startPromise.fail(fail);
        });
    }

    public void die() {
        Future.join(httpServer.close(), vertx.close())
                .onSuccess(successClose -> logger.log(System.Logger.Level.INFO, "Successfully close server"))
                .onFailure(failClose -> logger.log(System.Logger.Level.ERROR, "Fail to close server", failClose));
    }
}
