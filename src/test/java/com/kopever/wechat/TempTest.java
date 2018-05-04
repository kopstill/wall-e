package com.kopever.wechat;

import com.google.gson.*;
import okhttp3.*;
import org.junit.Test;

import java.io.IOException;
import java.util.*;
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
        String url = "https://ofo-misc.ofo.com/regular_packet/index.html#/?random=https://img.ofo.so/cms/7d0ed865c419f1926a729e0671ca0fe8.jpg,#1447979595/fc62197feecf6d36e652b0a07af05f412a142dfdfc08ef7b57358ee8e9d28100f6f9485f5e03b966a7435419f8392502d8dfc37276e90c3c42ae46dcd7c0cf584c2c0c5e8b74a80a989c05f846da9b41";
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
        String configResult = configResponse.body() == null ? "" : configResponse.body().string();
        System.out.println(configResult);
        byte luckyNum = jsonParser.parse(configResult).getAsJsonObject().getAsJsonObject("values").get("luckyNum").getAsByte();
        System.out.println("luckyNum: " + luckyNum);

        RequestBody couponRequestBody = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"),
                "tel=" + new TempTest().generateRandomPhoneNumber() + "&orderno=" + orderno + "&key=" + key);
        Request couponRequest = new Request.Builder().url(ofoCouponActivityShareUrl).post(couponRequestBody).build();
        Response couponResponse = client.newCall(couponRequest).execute();
        String couponResult = couponResponse.body() == null ? "" : couponResponse.body().string();
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
                    String targetResult = targetResponse.body() == null ? "" : targetResponse.body().string();
                    System.out.println(targetResult);
                } else {
                    RequestBody randomRequestBody = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"),
                            "tel=" + new TempTest().generateRandomPhoneNumber() + "&orderno=" + orderno + "&key=" + key);
                    Request randomRequest = new Request.Builder().url(ofoCouponActivityShareUrl).post(randomRequestBody).build();
                    Response randomResponse = client.newCall(randomRequest).execute();
                    String randomResult = randomResponse.body() == null ? "" : randomResponse.body().string();
                    System.out.println(randomResult);
                }
            }
        }
    }

    private int[] mobilePrefix = new int[]{
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
            System.out.println(i);
        }
    }

    private String generateRandomPhoneNumber() {
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
            for (int j = 0; j < arr.length - 1 - i; j++) {
                if (arr[j] > arr[j + 1]) {
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                }
            }
        }
    }

    @Test
    public void testOfoLuckyCoupon() {
        String url = "https://ofo-misc.ofo.com/regular_packet/index.html#/?" +
                "random=https://img.ofo.so/cms/7d0ed865c419f1926a729e0671ca0fe8.jpg," +
                "#1159069238/fd689e23f0a350fcd0d4a120fd96385b495698b6120c96bbcf5473412db0653db516ad1fe86d0019aa902dab67b93e25931212fb3a3a8051c84d7dc572c7df382879e9f0774c1874d17ff6ad64289b38";
        boolean flag = Coupon.getOfoLuckyCoupon(url);
        System.out.println(flag);
    }

    @Test
    public void testArrayToList() {
        String[] arr = {
                "MTU4ODI5MDc1OTY=", "MTUxODE4NjE1MDM=", "MTc1ODE4NjM3MDk=",
                "MTM2NzgyNjU3ODk=", "MTc3MzgxMjU1NTk=", "MTUzNzg0MzE3NzY=",
                "MTM0NTcyMTc1MzA=", "MTMxMjgyNzAzNDY=", "MTU4MjAyNjE3ODk=",
        };

        List<String> numbers = new ArrayList<>(Arrays.asList(arr));
        String detectPhoneNumber = numbers.remove(new Random().nextInt(numbers.size() - 1));
        System.out.println(detectPhoneNumber);
    }

}
