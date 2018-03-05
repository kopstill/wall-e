import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import okhttp3.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WallE {

    static {
        System.setProperty("jsse.enableSNIExtension", "false");
    }

    private static Logger logger = Logger.getLogger(WallE.class.getName());

    private JsonParser jsonParser = new JsonParser();

    private static final MediaType APPLICATION_JSON = MediaType.parse("application/json; charset=utf-8");

    private static final String WECHAT_JSLOGIN_URL = "https://login.wx.qq.com/jslogin?appid=wx782c26e4c19acffb&fun=new&lang=zh_CN&_=%s";

    private static final String WECHAT_LOGIN_URL = "https://login.weixin.qq.com/l/%s";

    private static final String WECHAT_LOGIN_CHECK_URL = "https://login.wx.qq.com/cgi-bin/mmwebwx-bin/login?loginicon=true&uuid=%s&tip=%s&r=%s&_=%s";

    private static final String WECHAT_CHECK_MESSAGE_URI = "/synccheck?r=%s&skey=%s&sid=%s&uin=%s&deviceid=%s&synckey=%s&_=%s";

    private String baseUrl;

    private String qrcode;

    private String redirectUri;

    private String skey;

    private String wxsid;

    private String wxuin;

    private String passTicket;

    private JsonObject synckey;

    private String username;

    private final String DEVICE_ID = "e" + randomInt();

    private boolean isLogin = false;

    public static void main(String[] args) {
        new WallE().start();
    }

    private void start() {
        logger.info("Welcome to wall-e wechat robot");

        try {
            this.qrcode = getQrcode();

            String consoleQrcode = getConsoleQrcode(String.format(WECHAT_LOGIN_URL, qrcode));
            logger.info("Please scan the QR code below to login\n" + consoleQrcode);

            checkLoginStatus(1);

            if (isLogin) {
                getLoginMessage();

                initWechat();

                listenMessage();
            }
        } catch (Exception e) {
            logger.severe(e.getMessage());
            logger.severe("WALL-E shutdown");
            System.exit(1);
        }

        logger.info("WALL-E shutdown");
        System.exit(0);
    }

    private void listenMessage() throws IOException {
        establishNotify();

        String checkUri = String.format(WECHAT_CHECK_MESSAGE_URI,
                System.currentTimeMillis(),
                URLEncoder.encode(skey, "UTF-8"),
                URLEncoder.encode(wxsid, "UTF-8"),
                URLEncoder.encode(wxuin, "UTF-8"),
                URLEncoder.encode(DEVICE_ID, "UTF-8"),
                URLEncoder.encode(getSynckey(), "UTF-8"),
                System.currentTimeMillis());
        String checkUrl = "https://webpush." + baseUrl.substring(8, baseUrl.length()) + checkUri;
        System.out.println(checkUrl);
        checkMessage(checkUrl);
    }

    private void establishNotify() throws IOException {
        String establishUrl = baseUrl + "/webwxstatusnotify?pass_ticket=" + passTicket;

        Map<String, Object> params = new HashMap<>();

        Map<String, String> baseRequest = new HashMap<>();
        baseRequest.put("Uin", wxuin);
        baseRequest.put("Sid", wxsid);
        baseRequest.put("Skey", skey);
        baseRequest.put("DeviceID", DEVICE_ID);
        params.put("BaseRequest", baseRequest);

        params.put("Code", "3");
        params.put("FromUserName", username);
        params.put("ToUserName", username);
        params.put("ClientMsgId", System.currentTimeMillis());

        String json = new GsonBuilder().create().toJson(params);
        String result = httpPost(establishUrl, json);

        if (result != null) {
            JsonParser parser = new JsonParser();
            JsonObject jsonObject = parser.parse(result).getAsJsonObject();
            JsonObject baseResponse = jsonObject.getAsJsonObject("BaseResponse");

            String ret = baseResponse.get("Ret").getAsString();
            String msg = baseResponse.get("ErrMsg").getAsString();
            if ("0".equals(ret)) {
                logger.info("Notify connection established");
                return;
            }

            logger.severe(msg);
        }

        throw new RuntimeException("Establish notify connection failed");
    }

    private void checkMessage(String checkUrl) throws IOException {
        String result = httpGet(checkUrl);
        System.out.println(result);

        String retcode = extract("retcode:\"(\\d+)\"", result);
        String selector = extract("selector:\"(\\d+)\"}", result);
        if ("0".equals(retcode)) {
            if ("2".equals(selector)) {
                syncMessage();
            }
        } else if ("1102".equals(retcode)) {
            throw new RuntimeException("Synchronizing message failed");
        } else {
            checkMessage(checkUrl);
        }
    }

    private void syncMessage() {
        logger.info("New message");
//        https://webpush.wx.qq.com/cgi-bin/mmwebwx-bin/synccheck?r=1520173966405&skey=%40crypt_7f13f237_00a506c60cef29b70355a3c21e168a07&sid=iowqMDOjQwbkDqak&uin=2140276020&deviceid=e085999928113228&synckey=1_668232436%7C2_668232467%7C3_668232405%7C11_668232412%7C201_1520173815%7C203_1520170461%7C1000_1520168762%7C1001_1520168834&_=1520173216177
    }

    private void initWechat() throws IOException {
        String initUrl = baseUrl + "/webwxinit" + "?r=" + randomNegativeInt() + "&lang=zh_CN" + "&pass_ticket=" + passTicket;

        Map<String, String> params = new HashMap<>(4);
        params.put("DeviceID", DEVICE_ID);
        params.put("Sid", wxsid);
        params.put("Skey", skey);
        params.put("Uin", wxuin);

        String result = httpPost(initUrl, "{\"BaseRequest\":" + toJson(params) + "}");
        if (result != null) {
            JsonObject jsonObject = jsonParser.parse(result).getAsJsonObject();
            synckey = jsonObject.getAsJsonObject("SyncKey");
            username = jsonObject.getAsJsonObject("User").get("UserName").getAsString();
            logger.info("Init wechat successfully");

            return;
        }

        throw new RuntimeException("Init wechat exception");
    }

    private void getLoginMessage() throws IOException {
        String result = httpGet(redirectUri + "&fun=new&version=v2");

        String ret = extract("<ret>(\\S+)</ret>", result);
        String message = extract("<message>(\\S+)</message>", result);
        if ("0".equals(ret)) {
            this.skey = extract("<skey>(\\S+)</skey>", result);
            this.wxsid = extract("<wxsid>(\\S+)</wxsid>", result);
            this.wxuin = extract("<wxuin>(\\S+)</wxuin>", result);
            this.passTicket = extract("<pass_ticket>(\\S+)</pass_ticket>", result);

            logger.info("Parameters initialized");
            return;
        }

        throw new RuntimeException(message);
    }

    private void checkLoginStatus(int tip) throws IOException {
        String checkLoginUrl = String.format(WECHAT_LOGIN_CHECK_URL, qrcode, tip, randomNegativeInt(), System.currentTimeMillis());
        String result = httpGet(checkLoginUrl);

        tip = 0;

        String resultCode = extract("window.code=(\\d+);", result);
        if ("408".equals(resultCode)) {
            logger.info("Waiting for scanning the qrcode");
            checkLoginStatus(tip);
        } else if ("200".equals(resultCode)) {
            isLogin = true;
            logger.info("Login successfully");
            String redirectUri = extract("window.redirect_uri=\"(\\S+?)\";", result);
            if (redirectUri != null) {
                this.redirectUri = redirectUri;
                this.baseUrl = redirectUri.substring(0, redirectUri.lastIndexOf("/"));
            }
        } else if ("201".equals(resultCode)) {
            logger.info("Scanned successfully");
            logger.info("Please confirm on your phone");
            checkLoginStatus(tip);
        } else if ("400".equals(resultCode)) {
            logger.warning("Qrcode expired");
            logger.warning("Please restart the application to get new qrcode");
        } else {
            logger.warning(result);
            throw new RuntimeException("Unexpected login status");
        }
    }

    private String getQrcode() throws IOException {
        String jsloginUrl = String.format(WECHAT_JSLOGIN_URL, System.currentTimeMillis());
        String content = httpGet(jsloginUrl);

        String code = extract("window.QRLogin.code = (\\d+);", content);
        String uuid = extract("window.QRLogin.uuid = \"(.*)\";", content);
        if ("200".equals(code)) {
            return uuid;
        }

        throw new RuntimeException(content);
    }

    private String getSynckey() {
        StringBuilder syncKeyTmp = new StringBuilder();

        JsonArray jsonArray = synckey.getAsJsonArray("List");
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject obj = (JsonObject) jsonArray.get(i);
            syncKeyTmp.append(obj.get("Key")).append("_").append(obj.get("Val")).append("|");
        }

        if (syncKeyTmp.length() > 0) {
            syncKeyTmp = new StringBuilder(syncKeyTmp.substring(0, syncKeyTmp.length() - 1));
        }

        return syncKeyTmp.toString();
    }

    private String httpGet(String url) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build();
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

    private String httpPost(String url, String json) throws IOException {
        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = RequestBody.create(APPLICATION_JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();
        ResponseBody responseBody = response.body();

        if (responseBody != null) {
            return responseBody.string();
        }

        return null;
    }

    private String extract(String regex, String str) {
        Matcher matcher = Pattern.compile(regex).matcher(str);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private String randomNegativeInt() {
        return Integer.toString(~((int) System.currentTimeMillis()));
    }

    private String randomInt() {
        StringBuilder builder = new StringBuilder();

        Random random = new Random();
        for (int i = 0; i < 15; i++) {
            builder.append(random.nextInt(10));
        }

        return builder.toString();
    }

    private String getConsoleQrcode(String content) throws WriterException {
        int width = 30, height = 30;

        Map<EncodeHintType, Object> qrParams = new HashMap<>(2);
        qrParams.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        qrParams.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height, qrParams);

        return matrixToAscii(matrix);
    }

    private String matrixToAscii(BitMatrix matrix) {
        StringBuilder builder = new StringBuilder();

        for (int rows = 0; rows < matrix.getHeight(); rows++) {
            for (int cols = 0; cols < matrix.getWidth(); cols++) {
                boolean flag = matrix.get(rows, cols);
                if (flag) {
                    builder.append("\033[30m  \033[0;39m");
                } else {
                    builder.append("\033[47m  \033[0m");
                }
            }

            if (rows != matrix.getHeight() - 1) {
                builder.append("\n");
            }
        }

        return builder.toString();
    }

    private String toJson(Object obj) {
        return new GsonBuilder().create().toJson(obj);
    }

}
