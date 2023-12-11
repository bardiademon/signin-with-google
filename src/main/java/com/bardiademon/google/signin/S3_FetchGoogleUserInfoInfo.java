package com.bardiademon.google.signin;

import com.bardiademon.Jjson.JjsonObject.JjsonObject;
import com.bardiademon.Jjson.data.exception.JjsonException;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.web.client.WebClient;

public class S3_FetchGoogleUserInfoInfo {
    private final static System.Logger logger = System.getLogger(S3_FetchGoogleUserInfoInfo.class.getSimpleName());

    public Future<JjsonObject> fetch(final WebClient webClient, final String accessToken) {

        final Promise<JjsonObject> promise = Promise.promise();

        webClient.getAbs("https://www.googleapis.com/oauth2/v2/userinfo").putHeader("Authorization", "Bearer " + accessToken).send().onSuccess(bufferHttpResponse -> {
            try {
                final JjsonObject userInfo = JjsonObject.ofString(bufferHttpResponse.bodyAsString());
                promise.complete(userInfo);
            } catch (JjsonException e) {
                logger.log(System.Logger.Level.ERROR, "Fail to fetch user info, Response: " + bufferHttpResponse.bodyAsString() + " , AccessToken: " + accessToken, e);
            }
        }).onFailure(fail -> {
            logger.log(System.Logger.Level.ERROR, "Fail to fetch user info, AccessToken: " + accessToken, fail);
            promise.fail(fail);
        });

        return promise.future();
    }
}
