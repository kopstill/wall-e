package com.kopever.wechat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Created by Lullaby on 2018/3/30
 */
class Coupon {

    private static Logger logger = Logger.getLogger(Application.class.getName());

    private static JsonParser jsonParser = new JsonParser();

    private static final String TARGET_IDENTITY = "MTM2OTQyNjc0MDk=";

    private static final String[] QUALIFIED_IDENTITY_POOL = {
            "MTU4ODI5MDc1OTY=", "MTUxODE4NjE1MDM=", "MTc1ODE4NjM3MDk=",
            "MTM2NzgyNjU3ODk=", "MTc3MzgxMjU1NTk=", "MTUzNzg0MzE3NzY=",
            "MTM0NTcyMTc1MzA=", "MTMxMjgyNzAzNDY=", "MTU4MjAyNjE3ODk=",
    };

    static boolean getOfoLuckyCoupon(String url) {
        try {
            String substring = url.substring(url.lastIndexOf("#") + 1, url.length());
            String[] refs = substring.split("[/]");
            String orderno = refs[0];
            String key = refs[1];

            String ofoActivityUrl = "https://activity.api.ofo.com";
            String ofoCouponActivityUrl = ofoActivityUrl + "/activity/couponShare";
            String ofoCouponActivityConfigUrl = ofoCouponActivityUrl + "/config";
            String ofoCouponActivityShareUrl = ofoCouponActivityUrl + "/getShareCoupon";

            String configParams = "source-version=5.0&orderno=" + orderno + "&key=" + key;
            String configResult = Util.httpPost(ofoCouponActivityConfigUrl, configParams);
            logger.info("Config result: " + configResult);

            if (configResult == null) return false;
            byte luckyNum = jsonParser.parse(configResult).getAsJsonObject().getAsJsonObject("values").get("luckyNum").getAsByte();

            List<String> numbers = new ArrayList<>(Arrays.asList(QUALIFIED_IDENTITY_POOL));
            String detectPhoneNumber = numbers.remove(new Random().nextInt(numbers.size() - 1));

            String detectParams = "tel=" + detectPhoneNumber + "&orderno=" + orderno + "&key=" + key;
            String detectResult = Util.httpPost(ofoCouponActivityShareUrl, detectParams);
            logger.info("Detect result: " + detectResult);

            if (detectResult == null) return false;

            JsonObject detectJsonObject = jsonParser.parse(detectResult).getAsJsonObject();
            if (detectJsonObject.get("errorCode").getAsInt() != 200) return false;

            JsonArray shareList = detectJsonObject.getAsJsonObject("values").getAsJsonArray("shareList");
            int shareListSize = shareList.size();
            if (shareListSize < luckyNum) {
                int count = luckyNum - shareListSize;
                for (int i = 0; i < count; i++) {
                    if (i == count - 1) {
                        String targetParams = "tel=" + Util.decodeBase64(TARGET_IDENTITY) + "&orderno=" + orderno + "&key=" + key;
                        String targetResult = Util.httpPost(ofoCouponActivityShareUrl, targetParams);
                        logger.info("Target result: " + targetResult);

                        if (targetResult == null) return false;

                        JsonObject targetJsonObject = jsonParser.parse(targetResult).getAsJsonObject().getAsJsonObject("values");
                        JsonArray targetShareList = targetJsonObject.getAsJsonArray("shareList");

                        return targetShareList.size() == luckyNum;
                    } else {
                        String randomPhoneNumber = numbers.remove(new Random().nextInt(numbers.size() - 1));
                        Util.httpPost(ofoCouponActivityShareUrl, "tel=" + Util.decodeBase64(randomPhoneNumber) + "&orderno=" + orderno + "&key=" + key);
                    }
                }
            }

            return false;
        } catch (IOException e) {
            return false;
        }
    }

}
