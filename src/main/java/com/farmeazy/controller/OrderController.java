package com.farmeazy.controller;

import com.farmeazy.dto.OrderCreateDto;
import com.farmeazy.dto.OrderDto;
import com.farmeazy.dto.RetryPaymentDto;
import com.farmeazy.entity.User;
import com.farmeazy.service.OrderService;
import com.farmeazy.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = {
    "https://farm-eazy.com",
    "https://www.farm-eazy.com",
    "https://farm-eazy.vercel.app",
    "http://localhost:3000",
    "http://localhost:4200"
})
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@Valid @RequestBody OrderCreateDto orderCreateDto, Principal principal) {
        logger.info("ORDER_CONTROLLER_CREATE user={}", principal != null ? principal.getName() : null);
        User user = userService.findByEmail(principal.getName());
        OrderDto createdOrder = orderService.createOrder(user, orderCreateDto);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> getOrders(Principal principal) {
        logger.info("ORDER_CONTROLLER_GET_ALL user={}", principal != null ? principal.getName() : null);
        User user = userService.findByEmail(principal.getName());
        List<OrderDto> orders = orderService.getUserOrders(user);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable Long orderId, Principal principal) {
        logger.info("ORDER_CONTROLLER_GET_BY_ID user={} orderId={}", principal != null ? principal.getName() : null, orderId);
        User user = userService.findByEmail(principal.getName());
        OrderDto order = orderService.getOrder(user, orderId);
        return new ResponseEntity<>(order, HttpStatus.OK);
    }

    /**
     * Retry payment for failed Razorpay orders
     */
    @PostMapping("/{orderId}/retry-payment")
    public ResponseEntity<OrderDto> retryPayment(@PathVariable Long orderId, @RequestBody RetryPaymentDto retryDto, Principal principal) {
        logger.info("ORDER_CONTROLLER_RETRY_PAYMENT user={} orderId={}", principal != null ? principal.getName() : null, orderId);
        User user = userService.findByEmail(principal.getName());
        OrderDto updatedOrder = orderService.retryPayment(user, orderId, retryDto);
        return new ResponseEntity<>(updatedOrder, HttpStatus.OK);
    }

    /**
     * Cancel failed or pending order
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId, Principal principal) {
        logger.info("ORDER_CONTROLLER_CANCEL user={} orderId={}", principal != null ? principal.getName() : null, orderId);
        User user = userService.findByEmail(principal.getName());
        orderService.cancelOrder(orderId, user);
        return ResponseEntity.ok().build();
    }
}
