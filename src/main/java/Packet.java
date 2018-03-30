import okhttp3.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.Random;
import java.util.RandomAccess;

/**
 * Created by Lullaby on 2018/3/30
 */
public class Packet {

    private static final String IDENTITY = "MTM2OTQyNjc0MDk=";

    public static boolean getOfoPacket(String link) throws IOException {
        URL url = new URL(link);
        String ref = url.getRef();
        String[] refs = ref.split("[/]");

        String ofoActivityUrl = "https://activity.api.ofo.com";
        String ofoCouponActivityUrl = ofoActivityUrl + "/activity/couponShare";
        String ofoCouponActivityConfigUrl = ofoCouponActivityUrl + "/config";
        String ofoCouponActivityShareUrl = ofoCouponActivityUrl + "/getCouponShare";

        OkHttpClient client = new OkHttpClient.Builder().build();
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"),
                "source-version=5.0&orderno=" + refs[0] + "&key=" + refs[1]);
        Request request = new Request.Builder().url(ofoCouponActivityConfigUrl).post(requestBody).build();
        Response response = client.newCall(request).execute();
        String result = response.body().string();
        System.out.println(result);

        String identity = new String(Base64.getDecoder().decode("MTM2OTQyNjc0MDk="), "UTF-8");
        System.out.println(identity);

        return false;
    }

}

class PhoneNumberGenerator {

    private static final int[] MOBILE_PREFIX = new int[]{133, 153, 177, 180,
            181, 189, 134, 135, 136, 137, 138, 139, 150, 151, 152, 157, 158, 159,
            178, 182, 183, 184, 187, 188, 130, 131, 132, 155, 156, 176, 185, 186,
            145, 147, 170};

    public String generate() {
        StringBuilder post = new StringBuilder();

        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            post.append(random.nextInt(10));
        }

        return prefix() + post.toString();
    }

    private static int prefix() {
        return MOBILE_PREFIX[new Random().nextInt(MOBILE_PREFIX.length)];
    }

}