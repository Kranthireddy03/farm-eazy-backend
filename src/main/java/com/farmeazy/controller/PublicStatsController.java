package com.farmeazy.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import com.farmeazy.repository.UserRepository;
import com.farmeazy.repository.FarmRepository;
import com.farmeazy.repository.OrderItemRepository;
import com.farmeazy.repository.AddressRepository;
import com.farmeazy.repository.OrderItemRepository;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class PublicStatsController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FarmRepository farmRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired

    @GetMapping("/api/public/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeFarmers", userRepository.countByActiveTrue());
        stats.put("farms", farmRepository.count());
        stats.put("states", addressRepository.countDistinctStates());
        stats.put("products", orderItemRepository.count());
        return stats;
    }
}
