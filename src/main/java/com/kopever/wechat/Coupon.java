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

    static class OfoPacket {

        private static final String OFO_ACTIVITY_URL = "https://activity.api.ofo.com";
        private static final String OFO_COUPON_ACTIVITY_URL = OFO_ACTIVITY_URL + "/activity/couponShare";
        private static final String OFO_COUPON_ACTIVITY_CONFIG_URL = OFO_COUPON_ACTIVITY_URL + "/config";
        private static final String OFO_COUPON_ACTIVITY_SHARE_URL = OFO_COUPON_ACTIVITY_URL + "/getShareCoupon";

        private static final String OFO_COUPON_PARAMS_TEMPLATE = "tel=%s&orderno=%s&key=%s";

        static boolean getOfoLuckyCoupon(String url) {
            try {
                String substring = url.substring(url.lastIndexOf("#") + 1, url.length());
                String[] refs = substring.split("[/]");
                String orderno = refs[0];
                String key = refs[1];

                String configParams = "source-version=5.0&orderno=" + orderno + "&key=" + key;
                String configResult = Util.httpPost(OFO_COUPON_ACTIVITY_CONFIG_URL, configParams);
                logger.info("Config result: " + configResult);

                if (configResult == null) return false;
                byte luckyNum = jsonParser.parse(configResult).getAsJsonObject().getAsJsonObject("values").get("luckyNum").getAsByte();

                List<String> numbers = new ArrayList<>(Arrays.asList(QUALIFIED_IDENTITY_POOL));
                String detectPhoneNumber = Util.decodeBase64(numbers.remove(new Random().nextInt(numbers.size())));

                String detectParams = String.format(OFO_COUPON_PARAMS_TEMPLATE, detectPhoneNumber, orderno, key);
                String detectResult = Util.httpPost(OFO_COUPON_ACTIVITY_SHARE_URL, detectParams);
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
                            String targetResult = Util.httpPost(OFO_COUPON_ACTIVITY_SHARE_URL,
                                    String.format(OFO_COUPON_PARAMS_TEMPLATE, Util.decodeBase64(TARGET_IDENTITY), orderno, key));
                            logger.info("Target result: " + targetResult);

                            if (targetResult == null) return false;

                            JsonObject targetJsonObject = jsonParser.parse(targetResult).getAsJsonObject().getAsJsonObject("values");
                            JsonArray targetShareList = targetJsonObject.getAsJsonArray("shareList");

                            return targetShareList.size() == luckyNum;
                        } else {
                            Util.httpPost(OFO_COUPON_ACTIVITY_SHARE_URL,
                                    String.format(OFO_COUPON_PARAMS_TEMPLATE,
                                            Util.decodeBase64(numbers.remove(new Random().nextInt(numbers.size()))), orderno, key));
                        }
                    }
                }

                return false;
            } catch (IOException e) {
                return false;
            }
        }

    }

}
