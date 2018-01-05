package com.kopever.robot.domain.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class WechatVerificationVO implements Serializable {

    private String signature;

    private String timestamp;

    private String nonce;

    private String echostr;

}
