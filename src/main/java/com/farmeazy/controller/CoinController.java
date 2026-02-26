package com.farmeazy.controller;

import com.farmeazy.dto.UserCoinsDto;
import com.farmeazy.service.CoinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coins")
@CrossOrigin(origins = {
    "https://farm-eazy.com",
    "https://www.farm-eazy.com",
    "https://farm-eazy.vercel.app",
    "http://localhost:3000",
    "http://localhost:4200"
})
public class CoinController {
    
    private final CoinService coinService;
    
    @Autowired
    public CoinController(CoinService coinService) {
        this.coinService = coinService;
    }
    
    @GetMapping
    public ResponseEntity<UserCoinsDto> getUserCoins(Authentication authentication) {
        String email = authentication.getName();
        UserCoinsDto coins = coinService.getUserCoins(email);
        return ResponseEntity.ok(coins);
    }
    
    @PostMapping("/login-bonus")
    public ResponseEntity<UserCoinsDto> processLoginBonus(Authentication authentication) {
        String email = authentication.getName();
        UserCoinsDto coins = coinService.processLoginBonus(email);
        return ResponseEntity.ok(coins);
    }
}
