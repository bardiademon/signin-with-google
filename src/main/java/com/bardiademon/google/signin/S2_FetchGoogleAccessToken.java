package com.bardiademon.google.signin;

import com.bardiademon.Jjson.JjsonObject.JjsonObject;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

public class S2_FetchGoogleAccessToken {
    private final static System.Logger logger = System.getLogger(S2_FetchGoogleAccessToken.class.getSimpleName());

    public Future<String> fetch(final WebClient webClient, final String refreshToken, final JjsonObject config) {

        final Promise<String> promise = Promise.promise();

        final JjsonObject redirect = config.getJjsonObject("redirect");
        final String redirectUrl = String.format("http://%s:%d", redirect.getString("host"), redirect.getInteger("port"));
        final String clientId = config.getString("client_id");
        final String clientSecret = config.getString("client_secret");

        final JsonObject input = new JsonObject()
                .put("refresh_token", refreshToken)
                .put("client_id", clientId)
                .put("client_secret", clientSecret)
                .put("redirect_uri", "http://" + redirectUrl)
                .put("grant_type", "refresh_token");

        System.out.println("input = " + input);

        webClient.postAbs("https://oauth2.googleapis.com/token").sendJson(input).onSuccess(bufferHttpResponse -> {

            try {
                final JsonObject response = bufferHttpResponse.bodyAsJsonObject();
                final String accessToken = response.getString("access_token", null);
                if (refreshToken == null || refreshToken.isEmpty()) {
                    throw new NullPointerException("Access token is null, Response: " + bufferHttpResponse.bodyAsString());
                }

                promise.complete(accessToken);

            } catch (Exception e) {
                logger.log(System.Logger.Level.ERROR, "Fail to fetch refresh token", e);
                promise.fail(e);
            }

        }).onFailure(fail -> {
            logger.log(System.Logger.Level.ERROR, "Fail to fetch refresh token", fail);
            promise.fail(fail);
        });

        return promise.future();
    }
}
