package com.ss.heartlinkapi.admin.controller;

import com.ss.heartlinkapi.admin.service.AdminCoupleService;
import com.ss.heartlinkapi.linkmatch.entity.LinkMatchEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminCoupleController {

    @Autowired
    private AdminCoupleService adminCoupleService;

    @GetMapping("/questions")
    public Page<LinkMatchEntity> getAllquestions
            (@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {

        System.out.println(adminCoupleService.findAllByOrderByIdDesc(page, size).toString());
        return adminCoupleService.findAllByOrderByIdDesc(page, size);
    }

}
