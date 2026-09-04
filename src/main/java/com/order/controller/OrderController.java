package com.order.controller;

import com.order.model.request.OrderUpdateRequest;
import com.order.model.response.OrderUpdateResponse;
import com.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/update")
    public OrderUpdateResponse update(@RequestBody OrderUpdateRequest orderUpdateRequest) {
        return orderService.update(orderUpdateRequest);
    }

}
