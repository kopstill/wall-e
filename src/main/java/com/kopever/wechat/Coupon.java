package com.kopever.wechat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Base64;
import java.util.logging.Logger;

/**
 * Created by Lullaby on 2018/3/30
 */
class Coupon {

    private static Logger logger = Logger.getLogger(Application.class.getName());

    private static JsonParser jsonParser = new JsonParser();

    private static final String IDENTITY = "MTM2OTQyNjc0MDk=";

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

            String detectParams = "tel=" + Util.generateRandomPhoneNumber() + "&orderno=" + orderno + "&key=" + key;
            String detectResult = Util.httpPost(ofoCouponActivityShareUrl, detectParams);
            logger.info("Detect result: " + detectResult);

            if (detectResult == null) return false;
            JsonObject jsonObject = jsonParser.parse(detectResult).getAsJsonObject().getAsJsonObject("values");
            JsonArray shareList = jsonObject.getAsJsonArray("shareList");

            int shareListSize = shareList.size();
            if (shareListSize < luckyNum) {
                int count = luckyNum - shareListSize;
                for (int i = 0; i < count; i++) {
                    if (i == count - 1) {
                        String targetParams = "tel=" + new String(Base64.getDecoder().decode(IDENTITY), "UTF-8") + "&orderno=" + orderno + "&key=" + key;
                        String targetResult = Util.httpPost(ofoCouponActivityShareUrl, targetParams);
                        logger.info("Target result: " + detectResult);

                        if (targetResult == null) return false;
                        JsonObject targetJsonObject = jsonParser.parse(targetResult).getAsJsonObject().getAsJsonObject("values");
                        JsonArray targetShareList = targetJsonObject.getAsJsonArray("shareList");

                        return targetShareList.size() == luckyNum;
                    } else {
                        Util.httpPost(ofoCouponActivityShareUrl, "tel=" + Util.generateRandomPhoneNumber() + "&orderno=" + orderno + "&key=" + key);
                    }
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

}
