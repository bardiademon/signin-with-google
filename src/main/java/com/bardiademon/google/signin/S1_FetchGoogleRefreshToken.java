package com.bardiademon.google.signin;

import com.bardiademon.Jjson.JjsonObject.JjsonObject;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

public class S1_FetchGoogleRefreshToken implements Function<String, Void> {
    private final static System.Logger logger = System.getLogger(S1_FetchGoogleRefreshToken.class.getSimpleName());
    private WebClient webClient;
    private Promise<String> result;

    private String redirectUrl;
    private String clientId;
    private String clientSecret;

    public Future<String> fetch(final WebClient webClient, final JjsonObject config) {
        this.webClient = webClient;

        this.result = Promise.promise();

        final JjsonObject redirect = config.getJjsonObject("redirect");
        new GoogleResponseHandler(redirect.getString("host"), redirect.getInteger("port"), this);

        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {

            redirectUrl = String.format("%s:%d", redirect.getString("host"), redirect.getInteger("port"));
            final String accessType = config.getString("access_type");
            clientId = config.getString("client_id");
            clientSecret = config.getString("client_secret");
            final String responseType = config.getString("response_type");
            final String prompt = config.getString("prompt");
            final String scope = config.getJjsonArray("scope").stream().map(item -> URLEncoder.encode(((String) item), StandardCharsets.UTF_8)).collect(Collectors.joining("+"));

            final String uri = String.format("%s?redirect_uri=%s&" +
                            "access_type=%s&" +
                            "client_id=%s&" +
                            "response_type=%s&" +
                            "prompt=%s&" +
                            "scope=%s",
                    config.getString("google_auth_uri"), URLEncoder.encode("http://" + redirectUrl, StandardCharsets.UTF_8), accessType, clientId, responseType, prompt, scope);

            try {
                Desktop.getDesktop().browse(new URI(uri));
                logger.log(System.Logger.Level.INFO, "Successfully open browser, URI: " + uri);
            } catch (IOException | URISyntaxException e) {
                logger.log(System.Logger.Level.ERROR, "Fail to open browser, URI: " + uri, e);
            }
            executorService.shutdown();
        });

        return result.future();
    }

    @Override
    public Void apply(final String code) {
        System.out.println("code = " + code);

        final JsonObject input = new JsonObject()
                .put("code", code)
                .put("client_id", clientId)
                .put("client_secret", clientSecret)
                .put("redirect_uri", "http://" + redirectUrl)
                .put("grant_type", "authorization_code");

        webClient.postAbs("https://oauth2.googleapis.com/token").sendJson(input).onSuccess(bufferHttpResponse -> {

            try {
                final JjsonObject response = JjsonObject.ofString(bufferHttpResponse.bodyAsString());
                final String refreshToken = response.getString("refresh_token", null);
                if (refreshToken == null || refreshToken.isEmpty()) {
                    throw new NullPointerException("Refresh token is null, Response: " + bufferHttpResponse.bodyAsString());
                }

                result.handle(Future.succeededFuture(refreshToken));

            } catch (Exception e) {
                logger.log(System.Logger.Level.ERROR, "Fail to fetch refresh token", e);
                result.handle(Future.failedFuture(e));
            }

        }).onFailure(fail -> {
            logger.log(System.Logger.Level.ERROR, "Fail to fetch refresh token", fail);
            result.handle(Future.failedFuture(fail));
        });

        return null;
    }
}
