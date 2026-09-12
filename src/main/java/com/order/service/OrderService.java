package com.order.service;

import com.admission.db.aop.DbAdmission;
import com.order.model.entity.Order;
import com.order.model.request.OrderUpdateRequest;
import com.order.model.response.OrderUpdateResponse;
import com.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.async.DeferredResult;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @DbAdmission
    @Transactional
    public OrderUpdateResponse update(OrderUpdateRequest orderUpdateRequest) {

        Order order = orderRepository.getReferenceById(orderUpdateRequest.getOrderId());
        order.update(orderUpdateRequest.getOrderStatus());

        return OrderUpdateResponse.from(order);
    }
}
