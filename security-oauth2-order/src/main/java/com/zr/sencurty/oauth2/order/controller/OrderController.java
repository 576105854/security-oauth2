package com.zr.sencurty.oauth2.order.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    @GetMapping("/test")
    @PreAuthorize("hasAnyAuthority('user.list')") //拥有p1权限才能访问
    public String test(){
        return "访问资源";
    }
}
