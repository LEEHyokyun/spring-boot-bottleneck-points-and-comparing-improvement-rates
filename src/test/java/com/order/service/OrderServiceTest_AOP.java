package com.order.service;

import com.admission.db.aop.DbAdmissionAspect;
import com.admission.db.exception.DbOverLoadedException;
import com.admission.handler.DbSemaphoreHandler;
import com.order.model.entity.Order;
import com.order.model.request.OrderUpdateRequest;
import com.order.model.response.OrderUpdateResponse;
import com.order.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceTest_AOP {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void AOPTest() {

        // given

        Order order = mock(Order.class);

        OrderUpdateRequest request =
                mock(OrderUpdateRequest.class);

        when(request.getOrderId())
                .thenReturn(1L);

        when(request.getOrderStatus())
                .thenReturn(null);

        when(orderRepository.getReferenceById(1L))
                .thenReturn(order);

        // when

        OrderUpdateResponse response =
                orderService.update(request);

        // then

        /*
         * 실제 Spring Proxy를 통과했다.
         *
         * 따라서
         *
         * DbAdmissionAspect
         *       ↓
         * tryAcquire()
         *       ↓
         * joinPoint.proceed()
         *       ↓
         * OrderService.update()
         *       ↓
         * finally release()
         *
         * 흐름을 거친다.
         */

        assertNotNull(response);

        verify(orderRepository)
                .getReferenceById(1L);

        verify(order)
                .update(null);
    }

    @Test
    void DbSemaphoreWouldThrowDbOverLoadedException_LoadSheddingTest() {

        // given

        /*
         * Semaphore의 모든 permit을 선점한다.
         *
         * 그러면 실제 OrderService가 실행될 시점에는
         *
         * tryAcquire() == false
         *
         * 가 된다.
         */
        int acquiredCount = 0;

        while (DbSemaphoreHandler.tryAcquire()) {
            acquiredCount++;
        }

        assertTrue(acquiredCount > 0);

        OrderUpdateRequest request =
                mock(OrderUpdateRequest.class);

        when(request.getOrderId())
                .thenReturn(1L);

        // when & then

        assertThrows(
                DbOverLoadedException.class,
                () -> orderService.update(request)
        );

        /*
         * Semaphore를 획득하지 못했으므로
         * joinPoint.proceed() 자체가 호출되지 않는다.
         *
         * 따라서 Repository도 호출되면 안 된다.
         */
        verify(
                orderRepository,
                never()
        ).getReferenceById(anyLong());

        /*
         * 테스트에서 직접 확보했던 permit 반환
         */
        for (int i = 0; i < acquiredCount; i++) {
            DbSemaphoreHandler.release();
        }
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class TestConfig {

        @Bean
        OrderRepository orderRepository() {
            return mock(OrderRepository.class);
        }

        @Bean
        DbAdmissionAspect dbAdmissionAspect() {
            return new DbAdmissionAspect();
        }

        @Bean
        OrderService orderService(
                OrderRepository orderRepository
        ) {
            return new OrderService(orderRepository);
        }
    }
}