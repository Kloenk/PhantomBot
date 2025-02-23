/*
 * Copyright (C) 2016-2023 phantombot.github.io/PhantomBot
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gmt2001.httpclient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONObject;

import com.gmt2001.dns.CompositeAddressResolverGroup;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.resolver.DefaultAddressResolverGroup;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient.RequestSender;
import tv.phantombot.CaselessProperties;

/**
 * Performs HTTP requests
 *
 * @author gmt2001
 */
public final class HttpClient {

    private static final String DEFAULT_USER_AGENT = "PhantomBot/2022";
    private static final int TIMEOUT_TIME = 10;

    /**
     * Hide the Constructor
     */
    private HttpClient() {
    }

    /**
     * Performs an HTTP request
     *
     * @param method The HTTP method
     * @param url The URL to request
     * @param requestHeaders The request headers to send
     * @param requestBody The request body to send for POST/PUT/PATCH
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse request(HttpMethod method, URI url, HttpHeaders requestHeaders, String requestBody) {
        reactor.netty.http.client.HttpClient client = reactor.netty.http.client.HttpClient.create();
        if (url.getScheme().equals("https")) {
            client = client.secure();
        }

        /**
         * @botproperty usedefaultdnsresolver - If `true`, only the default Java/System DNS resolver is used. Default `false`
         * @botpropertycatsort usedefaultdnsresolver 10 700 HTTP/WS
         */
        if (CaselessProperties.instance().getPropertyAsBoolean("usedefaultdnsresolver", false)) {
            client = client.resolver(DefaultAddressResolverGroup.INSTANCE);
        } else {
            client = client.resolver(CompositeAddressResolverGroup.INSTANCE);
        }

        client = client.headers(h -> {
            h.add(requestHeaders);

            if (!h.contains(HttpHeaderNames.USER_AGENT)) {
                h.add(HttpHeaderNames.USER_AGENT, DEFAULT_USER_AGENT);

                if (!h.contains(HttpHeaderNames.CONTENT_LENGTH)) {
                    if (requestBody != null) {
                        h.add(HttpHeaderNames.CONTENT_LENGTH, requestBody.getBytes(StandardCharsets.UTF_8).length);
                    } else {
                        h.add(HttpHeaderNames.CONTENT_LENGTH, 0);
                    }
                }
            }
        });

        String _requestBody = requestBody;

        if (_requestBody == null) {
            _requestBody = "";
        }

        RequestSender sender = client.followRedirect(true).request(method).uri(url);

        try {
            /**
             * @botproperty httpclienttimeout - The timeout, in seconds, for an HTTP request to complete. Default `10`
             * @botpropertycatsort httpclienttimeout 100 700 HTTP/WS
             */
            return sender.send(ByteBufFlux.fromString(Mono.just(_requestBody)))
                    .responseSingle((res, buf) -> buf.asByteArray().map(content -> new HttpClientResponse(null, requestBody, content, url, res))
                    .defaultIfEmpty(new HttpClientResponse(null, requestBody, new byte[0], url, res)))
                    .toFuture().get(CaselessProperties.instance().getPropertyAsInt("httpclienttimeout", TIMEOUT_TIME), TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            return new HttpClientResponse(ex, false, method, requestBody, ex.getClass().getName().getBytes(StandardCharsets.UTF_8), requestHeaders, null, null, url);
        }
    }

    /**
     * Shortcut to perform a HEAD with the default headers
     *
     * @param url The URL to request
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse head(URI url) {
        return head(url, createHeaders());
    }

    /**
     * Shortcut to perform a HEAD with the specified headers
     *
     * @param url The URL to request
     * @param requestHeaders The headers to send
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse head(URI url, HttpHeaders requestHeaders) {
        return request(HttpMethod.HEAD, url, requestHeaders, null);
    }

    /**
     * Shortcut to perform a GET with the default headers
     *
     * @param url The URL to request
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse get(URI url) {
        return get(url, createHeaders());
    }

    /**
     * Shortcut to perform a GET with the specified headers
     *
     * @param url The URL to request
     * @param requestHeaders The headers to send
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse get(URI url, HttpHeaders requestHeaders) {
        return request(HttpMethod.GET, url, requestHeaders, null);
    }

    /**
     * Shortcut to perform a form-urlencoded POST with the default headers
     *
     * @param url The URL to request
     * @param requestBody The request body
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse post(URI url, String requestBody) {
        return post(url, createHeaders(true, false), requestBody);
    }

    /**
     * Shortcut to perform a POST with the specified headers
     *
     * @param url The URL to request
     * @param requestHeaders The headers to send
     * @param requestBody The request body
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse post(URI url, HttpHeaders requestHeaders, String requestBody) {
        return request(HttpMethod.POST, url, requestHeaders, requestBody);
    }

    /**
     * Shortcut to URL-encode a map of post data then submit it as a form-urlencoded POST with the default headers
     *
     * @param url The URL to request
     * @param postData A map of post data
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse post(URI url, Map<String, String> postData) {
        return post(url, urlencodePost(postData));
    }

    /**
     * Shortcut to URL-encode a map of post data then submit it as a POST with the specified headers
     *
     * @param url The URL to request
     * @param requestHeaders The headers to send
     * @param postData A map of post data
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse post(URI url, HttpHeaders requestHeaders, Map<String, String> postData) {
        return post(url, requestHeaders, urlencodePost(postData));
    }

    /**
     * Shortcut to stringify a {@link JSONObject} and submit it as a POST with the default headers
     *
     * @param url The URL to send
     * @param json The JSON object to send
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse post(URI url, JSONObject json) {
        return post(url, createHeaders(true, true), json);
    }

    /**
     * Shortcut to stringify a {@link JSONObject} and submit it as a POST with the specified headers
     *
     * @param url The URL to send
     * @param requestHeaders The headers to send
     * @param json The JSON object to send
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse post(URI url, HttpHeaders requestHeaders, JSONObject json) {
        return post(url, requestHeaders, json.toString());
    }

    /**
     * Shortcut to perform a form-urlencoded PUT with the default headers
     *
     * @param url The URL to send
     * @param requestBody The request body
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse put(URI url, String requestBody) {
        return put(url, createHeaders(true, false), requestBody);
    }

    /**
     * Shortcut to perform a PUT with the specified headers
     *
     * @param url The URL to request
     * @param requestHeaders The headers to send
     * @param requestBody The request body
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse put(URI url, HttpHeaders requestHeaders, String requestBody) {
        return request(HttpMethod.PUT, url, requestHeaders, requestBody);
    }

    /**
     * Shortcut to URL-encode a map of put data then submit it as a form-urlencoded PUT with the default headers
     *
     * @param url The URL to request
     * @param putData A map of put data
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse put(URI url, Map<String, String> putData) {
        return put(url, urlencodePost(putData));
    }

    /**
     * Shortcut to URL-encode a map of put data then submit it as a PUT with the specified headers
     *
     * @param url The URL to request
     * @param requestHeaders The headers to send
     * @param putData A map of put data
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse put(URI url, HttpHeaders requestHeaders, Map<String, String> putData) {
        return put(url, requestHeaders, urlencodePost(putData));
    }

    /**
     * Shortcut to stringify a {@link JSONObject} and submit it as a PUT with the default headers
     *
     * @param url The URL to send
     * @param json The JSON object to send
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse put(URI url, JSONObject json) {
        return put(url, createHeaders(true, true), json);
    }

    /**
     * Shortcut to stringify a {@link JSONObject} and submit it as a PUT with the specified headers
     *
     * @param url The URL to send
     * @param requestHeaders The headers to send
     * @param json The JSON object to send
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse put(URI url, HttpHeaders requestHeaders, JSONObject json) {
        return put(url, requestHeaders, json.toString());
    }

    /**
     * Shortcut to perform a form-urlencoded PATCH with the default headers
     *
     * @param url The URL to send
     * @param requestBody The request body
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse patch(URI url, String requestBody) {
        return patch(url, createHeaders(true, false), requestBody);
    }

    /**
     * Shortcut to perform a PATCH with the specified headers
     *
     * @param url The URL to request
     * @param requestHeaders The headers to send
     * @param requestBody The request body
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse patch(URI url, HttpHeaders requestHeaders, String requestBody) {
        return request(HttpMethod.PATCH, url, requestHeaders, requestBody);
    }

    /**
     * Shortcut to URL-encode a map of put data then submit it as a form-urlencoded PATCH with the default headers
     *
     * @param url The URL to request
     * @param patchData A map of patch data
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse patch(URI url, Map<String, String> patchData) {
        return patch(url, urlencodePost(patchData));
    }

    /**
     * Shortcut to URL-encode a map of patch data then submit it as a PATCH with the specified headers
     *
     * @param url The URL to request
     * @param requestHeaders The headers to send
     * @param patchData A map of patch data
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse patch(URI url, HttpHeaders requestHeaders, Map<String, String> patchData) {
        return patch(url, requestHeaders, urlencodePost(patchData));
    }

    /**
     * Shortcut to stringify a {@link JSONObject} and submit it as a PATCH with the default headers
     *
     * @param url The URL to send
     * @param json The JSON object to send
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse patch(URI url, JSONObject json) {
        return patch(url, createHeaders(true, true), json);
    }

    /**
     * Shortcut to stringify a {@link JSONObject} and submit it as a PATCH with the specified headers
     *
     * @param url The URL to send
     * @param requestHeaders The headers to send
     * @param json The JSON object to send
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse patch(URI url, HttpHeaders requestHeaders, JSONObject json) {
        return patch(url, requestHeaders, json.toString());
    }

    /**
     * Shortcut to perform a DELETE with the default headers
     *
     * @param url The URL to request
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse delete(URI url) {
        return delete(url, createHeaders());
    }

    /**
     * Shortcut to perform a DELETE with the specified headers
     *
     * @param url The URL to request
     * @param requestHeaders The headers to send
     * @return A {@link HttpClientResponse} with the results
     */
    public static HttpClientResponse delete(URI url, HttpHeaders requestHeaders) {
        return request(HttpMethod.DELETE, url, requestHeaders, null);
    }

    /**
     * Converts a map of post data into a URL-encoded string suitable for the content type application/x-www-form-urlencoded
     * <p>
     * To put only a name without the {@code =value}, set the value to {@code null}
     *
     * @param postData A map of post data
     * @return A URL encoded string
     */
    public static String urlencodePost(Map<String, String> postData) {
        StringBuilder sb = new StringBuilder();
        postData.forEach((k, v) -> {
            if (k != null) {
                if (sb.length() > 0) {
                    sb.append('&');
                }

                sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8));

                if (v != null) {
                    sb.append('=').append(URLEncoder.encode(v, StandardCharsets.UTF_8));
                }
            }
        });

        return sb.toString();
    }

    /**
     * Creates a new, empty {@link HttpHeaders}
     *
     * @return An empty {@link HttpHeaders}
     */
    public static HttpHeaders createHeaders() {
        return createHeaders(false, false);
    }

    /**
     * Creates a new {@link HttpHeaders} with some default headers filled in
     *
     * @param method The method that is going to be requested. If it is a mutator method that can provide a body, the content type may be set
     * @param isJson If true, sets the accept to application/json. If isMutator is true, sets the content type as well
     * @return A {@link HttpHeaders} with some default headers, if the parameters require them
     */
    public static HttpHeaders createHeaders(HttpMethod method, boolean isJson) {
        return createHeaders(isMutatorWithBody(method), isJson);
    }

    /**
     * Creates a new {@link HttpHeaders} with some default headers filled in
     *
     * @param isMutatorWithBody If true, sets the content type. If isJson is false, sets to application/x-www-form-urlencoded
     * @param isJson If true, sets the accept to application/json. If isMutator is true, sets the content type as well
     * @return A {@link HttpHeaders} with some default headers, if the parameters require them
     */
    public static HttpHeaders createHeaders(boolean isMutatorWithBody, boolean isJson) {
        HttpHeaders h = new DefaultHttpHeaders();

        if (isJson) {
            h.set(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON);
        }

        if (isMutatorWithBody) {
            if (isJson) {
                h.set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
            } else {
                h.set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED);
            }
        }

        return h;
    }

    /**
     * Indicates if the provided method is a mutator that can provide a request body
     *
     * @param method The method to test
     * @return {@code true} if the {@link HttpMethod} specified is a mutator type
     */
    public static boolean isMutatorWithBody(HttpMethod method) {
        return method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH;
    }

    /**
     * Takes a Map of query params and converts it to a query string
     * <p>
     * To put only a name without the {@code =value}, set the value to {@code null}
     *
     * @param query The query params
     * @return A URL encoded string, including the {@code ?} prefix
     */
    public static String createQuery(Map<String, String> query) {
        return "?" + urlencodePost(query);
    }
}
