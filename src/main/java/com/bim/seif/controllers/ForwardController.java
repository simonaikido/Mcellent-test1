package com.bim.seif.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class ForwardController {
    @RequestMapping(value = "/{path:^(?!.*\\.).*}")
    public String redirect() {
        return "forward:/seif/int/index.html";
    }
}
