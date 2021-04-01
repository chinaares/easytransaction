package com.xhy.tron.easytransaction.util;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HttpclientUtil {

    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public static HttpResponse<String> send(HttpMethod httpMethod, String url, Map<String, String> headers, String body) throws IOException, InterruptedException {

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .method(httpMethod.getMethod(), HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(8))
                .uri(URI.create(url));
        if (headers != null){
            headers.forEach((key, value) -> requestBuilder.headers(key, value));
        }
        HttpRequest request = requestBuilder.build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @AllArgsConstructor
    @Getter
    public static enum HttpMethod {
        GET("GET"),
        POST("POST"),
        ;
        private String method;
    }

}
