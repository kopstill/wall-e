package com.kopever.robot.util;

import com.google.common.collect.Lists;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SignUtil {

    public static String wechatSignWithSHA1(Map map) {
        List list = Lists.newArrayList(map.keySet());

        int size = list.size();
        String[] keys = new String[size];
        for (int i = 0; i < size; i++) {
            keys[i] = list.get(i).toString();
        }

        Arrays.sort(keys);

        StringBuilder result = new StringBuilder();

        for (String key : keys) {
            Object value = map.get(key);
            if (!StringUtils.isEmpty(value)) {
                result.append(value.toString());
            }
        }

        return DigestUtils.sha1Hex(result.toString());
    }

}
