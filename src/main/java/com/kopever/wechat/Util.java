package com.kopever.wechat;

import okhttp3.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.util.Random;

/**
 * Created by Lullaby on 2018/4/4
 */
class Util {

    private static final int[] MOBILE_PREFIX = new int[]{
            130, 131, 132, 133, 134, 135, 136,
            137, 138, 139, 145, 147, 150, 151,
            152, 153, 155, 156, 157, 158, 159,
            170, 176, 177, 178, 180, 181, 182,
            183, 184, 185, 186, 187, 188, 189
    };

    private static OkHttpClient client = getUnsafeOkHttpClient();

    static String generateRandomPhoneNumber() {
        StringBuilder mobileSuffix = new StringBuilder();

        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            mobileSuffix.append(random.nextInt(10));
        }

        return MOBILE_PREFIX[new Random().nextInt(MOBILE_PREFIX.length)] + mobileSuffix.toString();
    }

    static String httpPost(String url, String params) throws IOException {
        RequestBody configRequestBody = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), params);
        Request configRequest = new Request.Builder().url(url).post(configRequestBody).build();
        Response configResponse = client.newCall(configRequest).execute();

        if (configResponse.body() != null) {
            return configResponse.body().string();
        }

        return null;
    }

    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final X509TrustManager[] trustAllCerts = new X509TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0]);

            builder.hostnameVerifier((hostname, session) -> true);

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
