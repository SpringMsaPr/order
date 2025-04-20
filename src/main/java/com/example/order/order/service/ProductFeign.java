package com.example.order.order.service;

import com.example.order.order.dto.ProductDto;
import com.example.order.order.dto.ProductUpdateStockDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "product-service")
public interface ProductFeign {

    @GetMapping("/product/{productId}")
    ProductDto getProductById(@PathVariable Long productId, @RequestHeader("X-user-id") String userId);

    @PutMapping("/product/updatestock")
    void updateProductStock(@RequestBody ProductUpdateStockDto productUpdateStockDto);
}
