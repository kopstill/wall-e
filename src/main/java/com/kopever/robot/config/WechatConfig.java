package com.kopever.robot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Created by Lullaby on 2018/1/9
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "wechat")
@PropertySource("classpath:business.yml")
public class WechatConfig {

    @Value("${token}")
    private String token;

}
