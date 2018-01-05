package com.kopever.robot.controller;

import com.kopever.robot.domain.vo.WechatVerificationVO;
import com.kopever.robot.util.Jackson;
import com.kopever.robot.util.SignUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @GetMapping("/")
    public String signature(WechatVerificationVO requestVO) {
        Map map = Jackson.fromObject(requestVO, Map.class);
        logger.info("MessageController.signature.map -> {}", map);

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
