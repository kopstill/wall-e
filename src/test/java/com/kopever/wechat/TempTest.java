package com.kopever.wechat;

import com.google.gson.*;
import okhttp3.*;
import org.junit.Test;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Lullaby on 2018/3/5
 */
public class TempTest {

    @Test
    public void testParseJson() {
        Map<String, Object> map = new HashMap<>();

        Map<String, String> baseRequest = new HashMap<>();
        baseRequest.put("Uin", "2140276020");
        baseRequest.put("Sid", "EH88r77F0dn6f6zU");
        baseRequest.put("Skey", "@crypt_7f13f237_7358c750395d46bee3c3086c43ab47ff");
        baseRequest.put("DeviceID", "e129153276582872");
        map.put("BaseRequest", baseRequest);

        map.put("Code", "3");
        map.put("FromUserName", "@8b9f7e808ac867a9b4dc0fb6fd1467ea");
        map.put("ToUserName", "@8b9f7e808ac867a9b4dc0fb6fd1467ea");
        map.put("ClientMsgId", "1520232991149");

        Gson gson = new GsonBuilder().create();

        String json = gson.toJson(map);
        System.out.println(json);

        String json1 = "{\n" +
                "    \"BaseResponse\":{\n" +
                "        \"Ret\":020,\n" +
                "        \"ErrMsg\":\"no error\"\n" +
                "    },\n" +
                "    \"MsgID\":\"6414250289872615321\"\n" +
                "}";
        JsonParser parser = new JsonParser();

        JsonObject jsonObject = parser.parse(json1).getAsJsonObject();
        System.out.println(jsonObject.get("MsgID"));

        JsonObject baseResponse = jsonObject.getAsJsonObject("BaseResponse");
        System.out.println(baseResponse.get("Ret"));
        System.out.println(baseResponse.get("ErrMsg"));

        String result = "window.synccheck={retcode:\"1102\",selector:\"0\"}";
        String retcode = extract("retcode:\"(\\d+)\"", result);
        String selector = extract("selector:\"(\\d+)\"}", result);
        System.out.println(retcode);
        System.out.println(selector);

        String response = "<error><ret>1203</ret><message>为了你的帐号安全，此微信号已不允许登录网页微信。你可以使用Windows微信或Mac微信在电脑端登录。Windows微信下载地址：https://pc.weixin.qq.com  Mac微信下载地址：https://mac.weixin.qq.com</message></error>";
        String ret = extract("<ret>(\\S+)</ret>", response);
        String message = response.substring(response.indexOf("<message>") + 9, response.indexOf("</message>"));
        System.out.println(ret);
        System.out.println(message);
    }

    private static String extract(String regex, String str) {
        Matcher matcher = Pattern.compile(regex).matcher(str);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private static JsonParser jsonParser = new JsonParser();

    private static final String TARGET_PHONE_NUMBER = "MTM2OTQyNjc0MDk=";

    @Test
    public void testGetOfoLuckyCoupon() throws IOException {
        String url = "https://ofo-misc.ofo.com/regular_packet/index.html#/?random=https%3A%2F%2Fimg.ofo.so%2Fcms%2F7d0ed865c419f1926a729e0671ca0fe8.jpg,#1146636033/4a8f22fe84346276b82de5399f829bb8f8b595619cd32cbce0ced9a4d8124c185670e8ed3014f7fd283210120e898c5ee4ec9fb47cf021b2944326c030d04293e624250920d1c6f3d470a7e9e8deb25e";
        String substring = url.substring(url.lastIndexOf("#") + 1, url.length());
        String[] refs = substring.split("[/]");
        String orderno = refs[0];
        String key = refs[1];
        System.out.println("orderno: " + orderno);
        System.out.println("key: " + key);

        String ofoActivityUrl = "https://activity.api.ofo.com";
        String ofoCouponActivityUrl = ofoActivityUrl + "/activity/couponShare";
        String ofoCouponActivityConfigUrl = ofoCouponActivityUrl + "/config";
        String ofoCouponActivityShareUrl = ofoCouponActivityUrl + "/getShareCoupon";

        OkHttpClient client = new OkHttpClient.Builder().build();
        RequestBody configRequestBody = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"),
                "source-version=5.0&orderno=" + orderno + "&key=" + key);
        Request configRequest = new Request.Builder().url(ofoCouponActivityConfigUrl).post(configRequestBody).build();
        Response configResponse = client.newCall(configRequest).execute();
        String configResult = configResponse.body().string();
        System.out.println(configResult);
        byte luckyNum = jsonParser.parse(configResult).getAsJsonObject().getAsJsonObject("values").get("luckyNum").getAsByte();
        System.out.println("luckyNum: " + luckyNum);

        RequestBody couponRequestBody = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"),
                "tel=" + new TempTest().getRandomPhoneNumber() + "&orderno=" + orderno + "&key=" + key);
        Request couponRequest = new Request.Builder().url(ofoCouponActivityShareUrl).post(couponRequestBody).build();
        Response couponResponse = client.newCall(couponRequest).execute();
        String couponResult = couponResponse.body().string();
        System.out.println(couponResult);

        JsonObject jsonObject = jsonParser.parse(couponResult).getAsJsonObject().getAsJsonObject("values");
        JsonArray shareList = jsonObject.getAsJsonArray("shareList");
        int shareListSize = shareList.size();
        System.out.println(shareListSize);

        if (shareListSize < luckyNum) {
            int count = luckyNum - shareListSize;
            for (int i = 0; i < count; i++) {
                if (i == count - 1) {
                    RequestBody targetRequestBody = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"),
                            "tel=" + new String(Base64.getDecoder().decode(TARGET_PHONE_NUMBER), "UTF-8") +
                                    "&orderno=" + orderno +
                                    "&key=" + key);
                    Request targetRequest = new Request.Builder().url(ofoCouponActivityShareUrl).post(targetRequestBody).build();
                    Response targetResponse = client.newCall(targetRequest).execute();
                    String targetResult = targetResponse.body().string();
                    System.out.println(targetResult);
                } else {
                    RequestBody randomRequestBody = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"),
                            "tel=" + new TempTest().getRandomPhoneNumber() + "&orderno=" + orderno + "&key=" + key);
                    Request randomRequest = new Request.Builder().url(ofoCouponActivityShareUrl).post(randomRequestBody).build();
                    Response randomResponse = client.newCall(randomRequest).execute();
                    String randomResult = randomResponse.body().string();
                    System.out.println(randomResult);
                }
            }
        }
    }

    int[] mobilePrefix = new int[]{
            133, 153, 177, 180, 181, 189, 134,
            135, 136, 137, 138, 139, 150, 151,
            152, 157, 158, 159, 178, 182, 183,
            184, 187, 188, 130, 131, 132, 155,
            156, 176, 185, 186, 145, 147, 170
    };

    @Test
    public void testBubbleSort() {
        bubbleSort(mobilePrefix);

        for (int i : mobilePrefix) {
            System.out.print(i + " ");
        }
    }

    private String getRandomPhoneNumber() {
        Random random = new Random();

        int prefix = mobilePrefix[random.nextInt(mobilePrefix.length)];

        StringBuilder phone = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            phone.append(random.nextInt(10));
        }

        return prefix + phone.toString();
    }

    private static void bubbleSort(int[] arr) {
        for (int i = 0; i < arr.length - 1; i++) {
            for (int j = 0; j < arr.length - 1; j++) {
                if (arr[j] > arr[j + 1]) {
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                }
            }
        }
    }

    private static String httpGet(String url) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder().build();

        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            ResponseBody body = response.body();
            if (body != null) {
                return body.string();
            }
        }

        return null;
    }

    @Test
    public void testOfoLuckyCoupon() {
        String url = "https://ofo-misc.ofo.com/regular_packet/index.html?from=singlemessage&isappinstalled=0#/?random=https://img.ofo.so/cms/7d0ed865c419f1926a729e0671ca0fe8.jpg,#1176785907/2d854cbf595156a97b7c8a62a46289ddb8a2ebd6052797d5cc3a384b98907669878e543cd3844be12d9967693ccd7d95715c0b6957e1baeaf0def9afec30ea182879e9f0774c1874d17ff6ad64289b38";
        boolean flag = Coupon.getOfoLuckyCoupon(url);
        System.out.println(flag);
    }

}
