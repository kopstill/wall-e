package com.kopever.robot.controller;

import com.google.common.collect.Maps;
import com.kopever.robot.domain.vo.WechatVerificationVO;
import com.kopever.robot.util.Jackson;
import com.kopever.robot.util.SignUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Created by Lullaby on 2018/1/5
 */
@RestController
public class MessageController {

    private static Logger logger = LoggerFactory.getLogger(MessageController.class);

    @Value("${business.wechat.token}")
    private String token;

    @GetMapping("/")
    public String signature(WechatVerificationVO requestVO) {
        logger.info("MessageController.signature.requestVO -> {}", Jackson.toJson(requestVO));
        Map<Object, Object> map = Maps.newHashMap();
        map.put("timestamp", requestVO.getTimestamp());
        map.put("nonce", requestVO.getNonce());
        map.put("token", token);
        logger.info("MessageController.signature.map -> {}", Jackson.toJson(map));

        String sign = SignUtil.wechatSignWithSHA1(map);
        logger.info("MessageController.signature.sign -> {}", sign);
        if (sign.equals(requestVO.getSignature())) {
            return requestVO.getEchostr();
        }

        return "sign error";
    }

    @PostMapping
    public String message() {
        return "got it";
    }

}
