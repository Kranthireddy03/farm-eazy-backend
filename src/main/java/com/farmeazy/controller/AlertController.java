package com.farmeazy.controller;

import com.farmeazy.service.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    @Autowired
    private SmsService smsService;

    @PostMapping("/sms")
    public String sendSmsAlert(@RequestParam String phone, @RequestParam String message) {
        smsService.sendSms(phone, message);
        return "SMS alert sent";
    }
}
