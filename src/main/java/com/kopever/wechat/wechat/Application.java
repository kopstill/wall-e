package com.kopever.wechat.wechat;

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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Application {

    static {
        System.setProperty("jsse.enableSNIExtension", "false");
    }

    private static Logger logger = Logger.getLogger(Application.class.getName());

    private JsonParser jsonParser = new JsonParser();

    private static final MediaType APPLICATION_JSON = MediaType.parse("application/json; charset=utf-8");

    private static final String WECHAT_JSLOGIN_URL = "https://login.wx2.qq.com/jslogin?appid=wx782c26e4c19acffb&fun=new&lang=zh_CN&_=%s";

    private static final String WECHAT_LOGIN_URL = "https://login.weixin.qq.com/l/%s";

    private static final String WECHAT_LOGIN_CHECK_URL = "https://login.wx2.qq.com/cgi-bin/mmwebwx-bin/login?loginicon=true&uuid=%s&tip=%s&r=%s&_=%s";

    private String baseUrl;

    private String qrcode;

    private String redirectUri;

    private String skey;

    private String wxsid;

    private String wxuin;

    private String passTicket;

    private JsonObject synckey;

    private String username;

    private final String DEVICE_ID = "e" + randomInt(15);

    private boolean isLogin = false;

    private HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

    public static void main(String[] args) {
        new Application().start();
    }

    private void start() {
        logger.info("Welcome to wall-e wechat robot");

        try {
            this.qrcode = getQrcode();

            String consoleQrcode = getConsoleQrcode(String.format(WECHAT_LOGIN_URL, qrcode));
            logger.info("Please scan the QR code below to login\n" + consoleQrcode);

            checkLoginStatus(1);

            if (isLogin) {
                getLoginData();

                initWechat();

                establishNotify();

                listenMessage();
            }
        } catch (Exception e) {
            logger.severe(e.getMessage());
            logger.severe("WALL-E exception and shutdown");
            System.exit(1);
        }

        logger.info("WALL-E shutdown");
        System.exit(0);
    }

    private void listenMessage() throws IOException, InterruptedException {
        String host = baseUrl.substring(8, baseUrl.length());
        long timestamp = System.currentTimeMillis();
        while (true) {
            String checkUri = String.format("/synccheck?r=%s&skey=%s&sid=%s&uin=%s&deviceid=%s&synckey=%s&_=%s",
                    System.currentTimeMillis(),
                    URLEncoder.encode(skey, "UTF-8"),
                    URLEncoder.encode(wxsid, "UTF-8"),
                    URLEncoder.encode(wxuin, "UTF-8"),
                    URLEncoder.encode(DEVICE_ID, "UTF-8"),
                    URLEncoder.encode(getSynckey(), "UTF-8"),
                    timestamp);

            String checkUrl = "https://webpush." + host + checkUri;

            try {
                checkMessage(checkUrl);
            } catch (Exception e) {
                logger.warning(e.getMessage());
                Thread.sleep(3000);
            }

            if (checkUri.equals(checkUrl)) {
                break;
            }

            timestamp++;
        }
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
            JsonObject jsonObject = jsonParser.parse(result).getAsJsonObject();
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

        String retcode = extract("retcode:\"(\\d+)\"", result);
        String selector = extract("selector:\"(\\d+)\"}", result);
        if ("0".equals(retcode)) {
            if ("2".equals(selector)) {
                syncMessage();
            } else {
                throw new RuntimeException("Print selector: " + selector);
            }
        } else if ("1100".equals(retcode)) {
            logger.warning("Your account has logged out");
            System.exit(0);
        } else if ("1101".equals(retcode)) {
            logger.warning("Your account is logged in on another device");
            System.exit(0);
        } else {
            throw new RuntimeException("Unknown retcode: " + selector);
        }
    }

    private void syncMessage() throws IOException {
        String syncUrl = baseUrl + "/webwxsync?sid=" + wxsid + "&skey=" + skey + "&lang=zh_CN" + "&pass_ticket=" + passTicket;
        String json = "{" +
                "    \"BaseRequest\":{" +
                "        \"Uin\":\"" + wxuin + "\"," +
                "        \"Sid\":\"" + wxsid + "\"," +
                "        \"Skey\":\"" + skey + "\"," +
                "        \"DeviceID\":\"" + DEVICE_ID + "\"" +
                "    }," +
                "    \"SyncKey\":" + synckey.toString() + "," +
                "    \"rr\":" + randomNegativeInt() +
                "}";
        String result = httpPost(syncUrl, json);

        if (result != null) {
            JsonObject jsonObject = jsonParser.parse(result).getAsJsonObject();
            synckey = jsonObject.getAsJsonObject("SyncKey");

            JsonArray messages = jsonObject.getAsJsonArray("AddMsgList");
            for (int i = 0; i < messages.size(); i++) {
                JsonObject msgobj = messages.get(i).getAsJsonObject();
                logger.info("Receive message: " + msgobj.toString());

                int msgType = msgobj.get("MsgType").getAsInt();
                if (msgType == 49) {
                    handleLinkMessage(msgobj);
                }
            }
        }
    }

    private void handleLinkMessage(JsonObject msgobj) throws IOException {
        String url = msgobj.get("Url").getAsString();
        if (url.startsWith("https://common.ofo.so/packet/")) {
            if (Packet.getOfoPacket(url)) {
                sendMessage(username, "成功领取一个ofo红包");
            } else {
                sendMessage(username, "收到一个ofo红包但领取失败");
            }
        }
    }

    private void sendMessage(String toUsername, String message) throws IOException {
        String sendUrl = baseUrl + "/webwxsendmsg?lang=zh_CN&pass_ticket=" + passTicket;
        String messageId = System.currentTimeMillis() + randomInt(4);
        String json = "{" +
                "    \"BaseRequest\":{" +
                "        \"Uin\":" + wxuin + "," +
                "        \"Sid\":\"" + wxsid + "\"," +
                "        \"Skey\":\"" + skey + "\"," +
                "        \"DeviceID\":\"" + DEVICE_ID + "\"" +
                "    }," +
                "    \"Msg\":{" +
                "        \"Type\":1," +
                "        \"Content\":\"" + message + "\"," +
                "        \"FromUserName\":\"" + username + "\"," +
                "        \"ToUserName\":\"" + toUsername + "\"," +
                "        \"LocalID\":\"" + messageId + "\"," +
                "        \"ClientMsgId\":\"" + messageId + "\"" +
                "    }," +
                "    \"Scene\":0" +
                "}";

        String result = httpPost(sendUrl, json);
        if (result != null) {
            JsonObject jsonobj = jsonParser.parse(result).getAsJsonObject();
            JsonObject baseResponse = jsonobj.getAsJsonObject("BaseResponse");
            String ret = baseResponse.get("Ret").getAsString();
            String msg = baseResponse.get("ErrMsg").getAsString();

            if ("0".equals(ret)) {
                logger.info("Send message success");
            } else {
                logger.warning("Send message failed, msg: " + msg);
            }
        }
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
            logger.info("Wechat initialized");

            return;
        }

        throw new RuntimeException("Wechat initialize exception");
    }

    private void getLoginData() throws IOException {
        String result = httpGet(redirectUri + "&fun=new&version=v2");

        String ret = extract("<ret>(\\S+)</ret>", result);
        String message = extract("<message>(\\S+)</message>", result);
        if (message == null) {
            message = result.substring(result.indexOf("<message>") + "<message>".length(), result.indexOf("</message>"));
        }
        if ("0".equals(ret)) {
            logger.info("Login successfully");

            this.skey = extract("<skey>(\\S+)</skey>", result);
            this.wxsid = extract("<wxsid>(\\S+)</wxsid>", result);
            this.wxuin = extract("<wxuin>(\\S+)</wxuin>", result);
            this.passTicket = extract("<pass_ticket>(\\S+)</pass_ticket>", result);

            logger.info("Session parameters initialized");
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
            this.isLogin = true;
            String redirectUri = extract("window.redirect_uri=\"(\\S+?)\";", result);
            if (redirectUri != null) {
                this.redirectUri = redirectUri;
                this.baseUrl = redirectUri.substring(0, redirectUri.lastIndexOf("/"));
            }
        } else if ("201".equals(resultCode)) {
            logger.info("Scanned successfully");
            logger.info("Please confirm on your device");
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
        OkHttpClient client = new OkHttpClient.Builder().cookieJar(
                new CookieJar() {
                    @Override
                    public void saveFromResponse(HttpUrl httpUrl, List<Cookie> cookies) {
                        cookieStore.put(httpUrl.host(), cookies);
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl httpUrl) {
                        return loadCookie(httpUrl);
                    }
                }
        ).readTimeout(30, TimeUnit.SECONDS).build();
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

    private List<Cookie> loadCookie(HttpUrl httpUrl) {
        String cookieHost;
        String host = httpUrl.host();

        String[] items = httpUrl.host().split("[.]");
        if (items.length > 3) {
            cookieHost = host.substring(host.indexOf(".") + 1);
        } else {
            cookieHost = host;
        }

        List<Cookie> cookies = cookieStore.get(cookieHost);

        return cookies != null ? cookies : new ArrayList<>();
    }

    private String httpPost(String url, String json) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder().cookieJar(
                new CookieJar() {
                    @Override
                    public void saveFromResponse(HttpUrl httpUrl, List<Cookie> cookies) {
                        cookieStore.put(httpUrl.host(), cookies);
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl httpUrl) {
                        return loadCookie(httpUrl);
                    }
                }
        ).build();

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

    private String randomInt(int length) {
        StringBuilder builder = new StringBuilder();

        Random random = new Random();
        for (int i = 0; i < length; i++) {
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
