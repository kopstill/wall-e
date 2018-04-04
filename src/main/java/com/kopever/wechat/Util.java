package com.kopever.wechat;

import okhttp3.*;

import java.io.IOException;
import java.util.Random;

/**
 * Created by Lullaby on 2018/4/4
 */
public class Util {

    private static final int[] MOBILE_PREFIX = new int[]{
            130, 131, 132, 133, 134, 135, 136,
            137, 138, 139, 145, 147, 150, 151,
            152, 153, 155, 156, 157, 158, 159,
            170, 176, 177, 178, 180, 181, 182,
            183, 184, 185, 186, 187, 188, 189
    };

    private static OkHttpClient client = new OkHttpClient.Builder().build();

    public static String generateRandomPhoneNumber() {
        StringBuilder mobileSuffix = new StringBuilder();

        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            mobileSuffix.append(random.nextInt(10));
        }

        return MOBILE_PREFIX[new Random().nextInt(MOBILE_PREFIX.length)] + mobileSuffix.toString();
    }

    public static String httpPost(String url, String params) throws IOException {
        RequestBody configRequestBody = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), params);
        Request configRequest = new Request.Builder().url(url).post(configRequestBody).build();
        Response configResponse = client.newCall(configRequest).execute();

        return configResponse.body().string();
    }

}
