/*
 * Copyright (c) 2012, the Last.fm Java Project and Committers
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.umass.lastfm;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.umass.lastfm.Result.Status;
import de.umass.lastfm.cache.ExpirationPolicy;
import okhttp3.CacheControl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static de.umass.util.StringUtilities.encode;
import static de.umass.util.StringUtilities.map;

/**
 * The <code>Caller</code> class handles the low-level communication between the client and last.fm.<br/>
 * Direct usage of this class should be unnecessary since all method calls are available via the methods in
 * the <code>Artist</code>, <code>Album</code>, <code>User</code>, etc. classes.
 * If specialized calls which are not covered by the Java API are necessary this class may be used directly.<br/>
 * Supports the setting of a custom {@link Proxy} and a custom <code>User-Agent</code> HTTP header.
 *
 * @author Janni Kovacs
 */
public class Caller {

    private static final String PARAM_API_KEY = "api_key";
    public static final String PARAM_METHOD = "method";

    static final String DEFAULT_API_ROOT = "https://ws.audioscrobbler.com/2.0/";
    private static final Caller instance = new Caller();

    private final Logger log = Logger.getLogger("de.umass.lastfm.Caller");

    private String apiRootUrl = DEFAULT_API_ROOT;

    private boolean debugMode = false;

    private Result lastError;

    private OkHttpClient client = new OkHttpClient.Builder()
            .build();

    private OkHttpClient okHttpClientTlsNoVerify = null;

    private final HashMap<Integer, ErrorNotifier> errorNotifiersMap = new HashMap<>();

    public static TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{};
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
    };


    /**
     * Returns the single instance of the <code>Caller</code> class.
     *
     * @return a <code>Caller</code>
     */
    public static Caller getInstance() {
        return instance;
    }

    public void setClient(OkHttpClient client) {
        this.client = client;
    }

    public OkHttpClient getClient() {
        return client;
    }

    public void setCache(@NonNull File directory, int maxSize) {
        setCache(directory, maxSize, null);
    }

    public void setCache(@NonNull File directory, int maxSize, @Nullable ExpirationPolicy policy) {
        CacheInterceptor cacheInterceptor;

        if (policy == null) {
            cacheInterceptor = new CacheInterceptor();
        } else {
            cacheInterceptor = new CacheInterceptor(policy);
        }

        client = client.newBuilder()
                .addNetworkInterceptor(cacheInterceptor)
                .cache(new okhttp3.Cache(directory, maxSize))
                .build();
    }

    public OkHttpClient getOkHttpClientTlsNoVerify() {

        if (okHttpClientTlsNoVerify == null) {
            okHttpClientTlsNoVerify = createOkHttpClientIgnoreSslErrors(client);
        }

        return okHttpClientTlsNoVerify;
    }

    public OkHttpClient createOkHttpClientIgnoreSslErrors(OkHttpClient okHttpClient) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            return okHttpClient.newBuilder()
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .build();

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Set api root url.
     *
     * @param apiRootUrl new api root url
     */
    public void setApiRootUrl(String apiRootUrl) {
        this.apiRootUrl = apiRootUrl;
    }

    /**
     * Sets a {@link Proxy} instance this Caller will use for all upcoming HTTP requests. May be <code>null</code>.
     *
     * @param proxy A <code>Proxy</code> or <code>null</code>.
     */
    public void setProxy(Proxy proxy) {
        client = client.newBuilder()
                .proxy(proxy)
                .build();
    }

    /**
     * Sets the <code>debugMode</code> property. If <code>debugMode</code> is <code>true</code> all call() methods
     * will print debug information and error messages on failure to stdout and stderr respectively.<br/>
     * Default is <code>false</code>. Set this to <code>true</code> while in development and for troubleshooting.
     *
     * @param debugMode <code>true</code> to enable debug mode
     * @see de.umass.lastfm.Caller#getLogger()
     * @deprecated Use the Logger instead
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        log.setLevel(debugMode ? Level.ALL : Level.OFF);
    }

    /**
     * @return the debugMode property
     * @see de.umass.lastfm.Caller#getLogger()
     * @deprecated Use the Logger instead
     */
    public boolean isDebugMode() {
        return debugMode;
    }

    public Logger getLogger() {
        return log;
    }

    public Result getLastError() {
        return lastError;
    }

    public void setErrorNotifier(int errorCode, ErrorNotifier handler) {
        errorNotifiersMap.put(errorCode, handler);
    }

    public Result call(String method, String apiKey, String... params) throws CallException {
        return call(method, apiKey, map(params));
    }

    public Result call(String method, String apiKey, Map<String, String> params) throws CallException {
        return call(null, method, apiKey, params, null);
    }

    public Result call(String apiRootUrl, String method, String apiKey, Map<String, String> params) throws CallException {
        return call(apiRootUrl, method, apiKey, params, null);
    }

    public Result call(String method, Session session, String... params) {
        return call(null, method, session.getApiKey(), map(params), session);
    }

    public Result call(String method, Session session, Map<String, String> params) {
        return call(null, method, session.getApiKey(), params, session);
    }

    /**
     * Performs the web-service call. If the <code>session</code> parameter is <code>non-null</code> then an
     * authenticated call is made. If it's <code>null</code> then an unauthenticated call is made.<br/>
     * The <code>apiKey</code> parameter is always required, even when a valid session is passed to this method.
     *
     * @param method  The method to call
     * @param apiKey  A Last.fm API key
     * @param params  Parameters
     * @param session A Session instance or <code>null</code>
     * @return the result of the operation
     */

    public Result call(String apiRootUrl, String method, String apiKey, Map<String, String> params,
                       Session session) {
        params = new HashMap<>(params); // create new Map in case params is an immutable Map
        InputStream inputStream = null;
        boolean loadedFromNetwork;

        if (apiRootUrl == null) {
            if (session != null)
                apiRootUrl = session.getApiRootUrl();
            else
                apiRootUrl = this.apiRootUrl;
        }

        // fill parameter map with apiKey and session info

        if (session != null) {
            params.put(PARAM_API_KEY, session.getApiKey());
            params.put("sk", session.getKey());
            params.put("api_sig", Authenticator.createSignature(method, params, session.getSecret()));
        }

        if (!params.containsKey(PARAM_API_KEY))
            params.put(PARAM_API_KEY, apiKey);

        try {
            CacheStrategy cacheStrategy = null;
            boolean isTlsNoVerify = false;

            if (session != null) {
                cacheStrategy = session.getCacheStrategy();
                isTlsNoVerify = session.isTlsNoVerify();
            }

            Response response = getOkHttpResponse(apiRootUrl, method, params, cacheStrategy, isTlsNoVerify);
            ResponseBody responseBody = response.body();

            inputStream = responseBody.byteStream();

            loadedFromNetwork = response.networkResponse() != null;
//            boolean loadedFromCache = response.cacheResponse() != null;

//            getLogger().log(Level.WARNING, "loadedFromNetwork: " + loadedFromNetwork + ", loadedFromCache: " + loadedFromCache);

        } catch (Exception e) {
            throw new CallException(e);
        }

        try {
            Result result = createResultFromInputStream(inputStream);
            if (!result.isSuccessful()) {
                String errMsg = String.format(method + " failed with result: %s%n", result);
                log.warning(errMsg);

                ErrorNotifier errorNotifier = errorNotifiersMap.get(result.errorCode);
                if (errorNotifier != null)
                    errorNotifier.notify(new CallException(errMsg));

                lastError = result;
            } else {
                lastError = null;
                result.setFromCache(!loadedFromNetwork);
            }
            if (session != null)
                session.setResult(result);
            return result;
        } catch (Exception e) {
            throw new CallException(e);
        }
    }

    // This is only here for this thing to compile.
    @Deprecated
    public HttpsURLConnection openConnection(String url) throws IOException {
        log.info("Open connection: " + url);
        URL u = new URL(url);
        HttpsURLConnection urlConnection = (HttpsURLConnection) u.openConnection();
        urlConnection.setRequestProperty("User-Agent", "tst");
        return urlConnection;
    }

    private Response getOkHttpResponse(
            String apiRootUrl,
            String method,
            Map<String, String> params,
            @Nullable CacheStrategy cacheStrategy,
            boolean isTlsNoVerify
    ) throws IOException {
        String query = buildPostBody(method, params);
        Request.Builder requestBuilder = new Request.Builder();

        if (method.contains(".get")) {
            requestBuilder.url(apiRootUrl + "?" + query);
        } else {
            requestBuilder.url(apiRootUrl)
                    .post(RequestBody.create(query, MediaType.parse("application/x-www-form-urlencoded")));
        }

        if (cacheStrategy == null)
            cacheStrategy = CacheStrategy.CACHE_FIRST;

        switch (cacheStrategy) {
            case CACHE_FIRST:
                // nothing
                break;
            case CACHE_ONLY_INCLUDE_EXPIRED:
                requestBuilder.cacheControl(CacheControl.FORCE_CACHE);
                break;
            case NETWORK_ONLY:
                requestBuilder.cacheControl(CacheControl.FORCE_NETWORK);
                break;
            case CACHE_FIRST_ONE_DAY:
                requestBuilder.cacheControl(
                        new CacheControl.Builder()
                                .maxAge(1, TimeUnit.DAYS)
                                .build()
                );
                break;
            case CACHE_FIRST_ONE_WEEK:
                requestBuilder.cacheControl(
                        new CacheControl.Builder()
                                .maxAge(7, TimeUnit.DAYS)
                                .build()
                );
                break;
        }

        OkHttpClient callingClient = isTlsNoVerify ? getOkHttpClientTlsNoVerify() : getClient();

        return callingClient.newCall(requestBuilder.build()).execute();
    }

    private Result createResultFromInputStream(InputStream inputStream) throws SAXException, IOException {
        InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        Document document = newDocumentBuilder().parse(new InputSource(isr));
        isr.close();
        Element root = document.getDocumentElement(); // lfm element
        String statusString = root.getAttribute("status");
        Status status = "ok".equals(statusString) ? Status.OK : Status.FAILED;
        if (status == Status.FAILED) {
            Element errorElement = (Element) root.getElementsByTagName("error").item(0);
            int errorCode = Integer.parseInt(errorElement.getAttribute("code"));
            String message = errorElement.getTextContent();
            return Result.createRestErrorResult(errorCode, message);
        } else {
            return Result.createOkResult(document);
        }
    }

    private DocumentBuilder newDocumentBuilder() {
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            return builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            // better never happens
            throw new RuntimeException(e);
        }
    }

    private String buildPostBody(String method, Map<String, String> params, String... strings) {
        StringBuilder builder = new StringBuilder(100);
        builder.append(PARAM_METHOD)
                .append("=")
                .append(method)
                .append('&');
        for (Iterator<Entry<String, String>> it = params.entrySet().iterator(); it.hasNext(); ) {
            Entry<String, String> entry = it.next();
            builder.append(entry.getKey())
                    .append('=')
                    .append(encode(entry.getValue()));
            if (it.hasNext() || strings.length > 0)
                builder.append('&');
        }
        int count = 0;
        for (String string : strings) {
            builder.append(count % 2 == 0 ? string : encode(string));
            count++;
            if (count != strings.length) {
                if (count % 2 == 0) {
                    builder.append('&');
                } else {
                    builder.append('=');
                }
            }
        }
        return builder.toString();
    }

    public enum CacheStrategy {
        CACHE_FIRST,
        CACHE_FIRST_ONE_DAY,
        CACHE_FIRST_ONE_WEEK,
        CACHE_ONLY_INCLUDE_EXPIRED,
        NETWORK_ONLY
    }
}
