package com.bardiademon.google.signin;

import com.bardiademon.Jjson.JjsonObject.JjsonObject;
import com.bardiademon.Jjson.data.exception.JjsonException;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

import java.io.InputStream;
import java.lang.System.Logger;

public class Main extends AbstractVerticle {

    private static JjsonObject config;

    private static String REFRESH_TOKEN = null;

    private final static Logger logger = System.getLogger(Main.class.getSimpleName());

    public static void main(String[] args) {
        logger.log(Logger.Level.INFO, "BARDIADEMON");
        Vertx.vertx().deployVerticle(Main.class.getName());
    }

    @Override
    public void start(Promise<Void> startPromise) {
        getConfig();

        final WebClient webClient = WebClient.create(vertx);

        final Future<String> fetch = REFRESH_TOKEN == null ? new S1_FetchGoogleRefreshToken().fetch(webClient, config) : Future.succeededFuture(REFRESH_TOKEN);

        fetch.onSuccess(refreshToken -> {

            logger.log(Logger.Level.INFO, "Successfully fetch refresh token, RefreshToken: " + refreshToken);

            new S2_FetchGoogleAccessToken().fetch(webClient, refreshToken, config).onSuccess(accessToken -> {

                logger.log(Logger.Level.INFO, "Successfully fetch access token, AccessToken: " + accessToken);

                new S3_FetchGoogleUserInfoInfo().fetch(webClient, accessToken).onSuccess(userInfo -> {

                    logger.log(Logger.Level.INFO, "Successfully fetch google user info, User: " + userInfo.encode());

                    System.out.println(userInfo.encodeFormatter());

                }).onFailure(failFetchUserInfo -> {
                    logger.log(Logger.Level.ERROR, "Fail to fetch google user info, RefreshToken: " + refreshToken, " , AccessToken: " + accessToken, failFetchUserInfo);
                }).onComplete(e -> closeVertx());

            }).onFailure(failFetchAccessToken -> {
                logger.log(Logger.Level.ERROR, "Fail to fetch access token", failFetchAccessToken);
                closeVertx();
            });

        }).onFailure(failFetchRefreshToken -> {
            logger.log(Logger.Level.ERROR, "Fail to fetch refresh token", failFetchRefreshToken);
            closeVertx();
        });

    }

    private void getConfig() {
        final InputStream configResourceStream = ClassLoader.getSystemResourceAsStream("conf.json");
        try {
            if (configResourceStream == null) {
                throw new NullPointerException("Cannot open config file");
            }
            config = JjsonObject.ofStream(configResourceStream);
            logger.log(Logger.Level.INFO, "Successfully fetch config file: " + config.encode());
        } catch (JjsonException | NullPointerException e) {
            logger.log(Logger.Level.ERROR, "Fail to fetch config", e);
            System.exit(-1);
        }
    }

    private void closeVertx() {
        vertx.close()
                .onSuccess(successClose -> logger.log(Logger.Level.INFO, "Successfully close vertx"))
                .onFailure(failClose -> logger.log(Logger.Level.ERROR, "Fail to close vertx", failClose));
    }
}