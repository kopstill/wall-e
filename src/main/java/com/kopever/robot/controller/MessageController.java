package com.kopever.robot.controller;

import com.kopever.robot.config.WechatConfig;
import com.kopever.robot.domain.vo.WechatVerificationVO;
import com.kopever.robot.util.Jackson;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * Created by Lullaby on 2018/1/5
 */
@RestController("messages")
public class MessageController {

    private static Logger logger = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    private WechatConfig wechatConfig;

    @GetMapping
    public String signature(WechatVerificationVO requestVO) {
        logger.info("MessageController.signature.requestVO -> {}", Jackson.toJson(requestVO));

        String[] array = {requestVO.getTimestamp(), requestVO.getNonce(), wechatConfig.getToken()};
        Arrays.sort(array);
        String text = array[0] + array[1] + array[2];
        logger.info("MessageController.signature.text -> {}", text);

        String sign = DigestUtils.sha1Hex(text);
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
