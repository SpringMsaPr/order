package com.example.order.order.controller;

import com.example.order.order.domain.Order;
import com.example.order.order.dto.OrderCreateDto;
import com.example.order.order.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderingService;

    public OrderController(OrderService orderingService) {
        this.orderingService = orderingService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> orderCreate(@RequestBody OrderCreateDto dtos,
                                         @RequestHeader("X-User-Id") String userId){
        Order order = orderingService.orderFeignKafkaCreate(dtos, userId);
        return new ResponseEntity<>(order.getId(), HttpStatus.CREATED);
    }

}