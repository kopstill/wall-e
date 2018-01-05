package com.kopever.robot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by Lullaby on 2018/1/5
 */
@RestController
public class RobotController {

    @GetMapping("token")
    public String home() {
        return "Hello World!";
    }

}
