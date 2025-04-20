package com.example.order.order.service;

import com.example.order.order.domain.Order;
import com.example.order.order.dto.OrderCreateDto;
import com.example.order.order.dto.ProductDto;
import com.example.order.order.dto.ProductUpdateStockDto;
import com.example.order.order.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;
    private final ProductFeign productFeign;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderService(OrderRepository orderRepository, RestTemplate restTemplate,
                        ProductFeign productFeign, KafkaTemplate<String, Object> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.restTemplate = restTemplate;
        this.productFeign = productFeign;
        this.kafkaTemplate = kafkaTemplate;
    }

    public Order orderCreate(OrderCreateDto orderCreateDto, String userId){
        String productGetUrl = "http://product-service/product/" + orderCreateDto.getProductId();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("X-user-Id", userId);
        HttpEntity<String> httpEntity = new HttpEntity<>(httpHeaders);

        ResponseEntity<ProductDto> response = restTemplate.exchange(productGetUrl, HttpMethod.GET,
                httpEntity, ProductDto.class);

        ProductDto productDto = response.getBody();
        int quantity = orderCreateDto.getProductCount();

        if(productDto.getStockQuantity() < quantity){
            throw new IllegalArgumentException("재고 부족");
        }else{
            String productPutUrl = "http://prodcut-service/product/updatestock";
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ProductUpdateStockDto> updateEntity = new HttpEntity(
                    ProductUpdateStockDto.builder()
                            .productId(orderCreateDto.getProductId())
                            .productQuantity(orderCreateDto.getProductCount())
                            .build()
                    , httpHeaders
            );
            restTemplate.exchange(productPutUrl, HttpMethod.PUT, updateEntity, Void.class);

            Order newOrder = Order.builder()
                    .memberId(Long.parseLong(userId))
                    .productId(orderCreateDto.getProductId())
                    .quantity(orderCreateDto.getProductCount())
                    .build();

            orderRepository.save(newOrder);
            return newOrder;
        }

    }


    @CircuitBreaker(name = "productService", fallbackMethod = "fallbackProductService")
    public Order orderFeignKafkaCreate(OrderCreateDto orderCreateDto, String userId){

        ProductDto productDto = productFeign.getProductById(orderCreateDto.getProductId(), userId);

        int quantity = orderCreateDto.getProductCount();
        if(productDto.getStockQuantity() < quantity){
            throw new IllegalArgumentException("재고 부족");
        }else{

            ProductUpdateStockDto dto = ProductUpdateStockDto.builder().productId(orderCreateDto.getProductId())
                    .productQuantity(orderCreateDto.getProductCount()).build();
            kafkaTemplate.send("update-stock-topic", dto);
        }

        Order order = Order.builder()
                .memberId(Long.parseLong(userId))
                .productId(orderCreateDto.getProductId())
                .quantity(orderCreateDto.getProductCount())
                .build();
        orderRepository.save(order);
        return order;
    }


    public Order fallbackProductService(OrderCreateDto orderDto, String userId, Throwable t){
        throw new RuntimeException("상품서비스가 응답이 없어, 에러가 발생했습니다. 나중에 다시 시도해주세요.");
    }



}