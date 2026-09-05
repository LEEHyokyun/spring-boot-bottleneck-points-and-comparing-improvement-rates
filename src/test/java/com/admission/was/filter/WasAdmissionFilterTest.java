package com.admission.was.filter;

import com.admission.handler.WasQueueHandler;
import com.admission.handler.WasSemaphoreHandler;
import com.admission.was.metrics.WasAdmissionMetrics;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class WasAdmissionFilterTest {

    private final WasAdmissionMetrics metrics =
            mock(WasAdmissionMetrics.class);

    private final WasAdmissionFilter filter =
            new WasAdmissionFilter(metrics);

    /**
     * Semaphore 획득 성공
     *
     * 요청
     *   ↓
     * tryAcquire() == true
     *   ↓
     * filterChain.doFilter()
     *   ↓
     * finally release()
     */
    @Test
    void semaphore를_획득하면_정상적으로_WAS로_전달한다()
            throws Exception {

        HttpServletRequest request =
                mock(HttpServletRequest.class);

        HttpServletResponse response =
                mock(HttpServletResponse.class);

        FilterChain filterChain =
                mock(FilterChain.class);

        try (
                MockedStatic<WasSemaphoreHandler> semaphore =
                        mockStatic(WasSemaphoreHandler.class);

                MockedStatic<WasQueueHandler> queue =
                        mockStatic(WasQueueHandler.class)
        ) {

            // given

            semaphore
                    .when(WasSemaphoreHandler::tryAcquire)
                    .thenReturn(true);

            semaphore
                    .when(WasSemaphoreHandler::availablePermits)
                    .thenReturn(449);

            // when

            filter.doFilter(
                    request,
                    response,
                    filterChain
            );

            // then

            /*
             * Semaphore 획득 성공
             */
            semaphore.verify(
                    WasSemaphoreHandler::tryAcquire
            );

            /*
             * 실제 WAS FilterChain으로 전달
             */
            verify(filterChain)
                    .doFilter(request, response);

            /*
             * 처리가 끝나면 반드시 permit 반환
             */
            semaphore.verify(
                    WasSemaphoreHandler::release
            );

            /*
             * Queue에는 들어가면 안 됨
             */
            queue.verify(
                    () -> WasQueueHandler.offer(any()),
                    never()
            );

            /*
             * Metrics
             */
            verify(metrics)
                    .acquireSuccess();

            verify(metrics, never())
                    .acquireFailed();
        }
    }


    /**
     * Worker가 AsyncContext.dispatch()한 요청
     *
     * 이미 Worker가 Semaphore를 확보했으므로
     * 다시 tryAcquire() 하면 안 된다.
     */
    @Test
    void Worker가_dispatch한_요청은_그대로_WAS로_전달한다()
            throws Exception {

        HttpServletRequest request =
                mock(HttpServletRequest.class);

        HttpServletResponse response =
                mock(HttpServletResponse.class);

        FilterChain filterChain =
                mock(FilterChain.class);

        // Worker가 설정한 attribute
        when(request.getAttribute(
                WasAdmissionFilter.class.getName()
                        + ".ADMISSION_DISPATCH"
        )).thenReturn(Boolean.TRUE);

        try (
                MockedStatic<WasSemaphoreHandler> semaphore =
                        mockStatic(WasSemaphoreHandler.class);

                MockedStatic<WasQueueHandler> queue =
                        mockStatic(WasQueueHandler.class)
        ) {

            // when

            filter.doFilter(
                    request,
                    response,
                    filterChain
            );

            // then

            /*
             * 이미 Semaphore를 획득했기 때문에
             * 다시 tryAcquire()하면 안 된다.
             */
            semaphore.verify(
                    WasSemaphoreHandler::tryAcquire,
                    never()
            );

            /*
             * 정상적으로 WAS에 전달
             */
            verify(filterChain)
                    .doFilter(request, response);

            /*
             * 처리가 끝났으므로 permit 반환
             */
            semaphore.verify(
                    WasSemaphoreHandler::release
            );

            /*
             * dispatch attribute 제거
             */
            verify(request)
                    .removeAttribute(
                            WasAdmissionFilter.class.getName()
                                    + ".ADMISSION_DISPATCH"
                    );

            /*
             * Queue 관련 동작 없음
             */
            queue.verify(
                    () -> WasQueueHandler.offer(any()),
                    never()
            );
        }
    }


    /**
     * Semaphore 획득 실패
     *
     * Queue에 넣을 수 있다면
     * AsyncContext로 전환하고 요청을 대기시킨다.
     */
    @Test
    void semaphore를_획득하지_못하면_요청을_Queue에_넣는다()
            throws Exception {

        HttpServletRequest request =
                mock(HttpServletRequest.class);

        HttpServletResponse response =
                mock(HttpServletResponse.class);

        FilterChain filterChain =
                mock(FilterChain.class);

        AsyncContext asyncContext =
                mock(AsyncContext.class);

        when(request.startAsync())
                .thenReturn(asyncContext);

        try (
                MockedStatic<WasSemaphoreHandler> semaphore =
                        mockStatic(WasSemaphoreHandler.class);

                MockedStatic<WasQueueHandler> queue =
                        mockStatic(WasQueueHandler.class)
        ) {

            // given

            semaphore
                    .when(WasSemaphoreHandler::tryAcquire)
                    .thenReturn(false);

            semaphore
                    .when(WasSemaphoreHandler::availablePermits)
                    .thenReturn(0);

            queue
                    .when(() ->
                            WasQueueHandler.offer(asyncContext)
                    )
                    .thenReturn(true);

            queue
                    .when(WasQueueHandler::size)
                    .thenReturn(1);

            // when

            filter.doFilter(
                    request,
                    response,
                    filterChain
            );

            // then

            /*
             * Semaphore 획득 실패
             */
            semaphore.verify(
                    WasSemaphoreHandler::tryAcquire
            );

            /*
             * AsyncContext 생성
             */
            verify(request)
                    .startAsync();

            /*
             * Queue에 요청 삽입
             */
            queue.verify(() ->
                    WasQueueHandler.offer(asyncContext)
            );

            /*
             * Queue에 들어갔으므로
             * FilterChain은 지금 실행되면 안 된다.
             */
            verify(
                    filterChain,
                    never()
            ).doFilter(
                    any(ServletRequest.class),
                    any(ServletResponse.class)
            );

            /*
             * Queue 삽입 성공 Metric
             */
            verify(metrics)
                    .acquireFailed();

            verify(metrics)
                    .queueOfferSuccess();

            /*
             * 이 단계에서는 permit을 획득하지 못했으므로
             * release하면 안 된다.
             */
            semaphore.verify(
                    WasSemaphoreHandler::release,
                    never()
            );
        }
    }


    /**
     * Semaphore도 없고 Queue도 가득 찬 경우
     *
     * → Load Shedding
     * → HTTP 503
     * → AsyncContext.complete()
     */
    @Test
    void semaphore도_없고_queue도_가득차면_503을_반환한다()
            throws Exception {

        HttpServletRequest request =
                mock(HttpServletRequest.class);

        HttpServletResponse response =
                mock(HttpServletResponse.class);

        FilterChain filterChain =
                mock(FilterChain.class);

        AsyncContext asyncContext =
                mock(AsyncContext.class);

        when(request.startAsync())
                .thenReturn(asyncContext);

        try (
                MockedStatic<WasSemaphoreHandler> semaphore =
                        mockStatic(WasSemaphoreHandler.class);

                MockedStatic<WasQueueHandler> queue =
                        mockStatic(WasQueueHandler.class)
        ) {

            // given

            semaphore
                    .when(WasSemaphoreHandler::tryAcquire)
                    .thenReturn(false);

            semaphore
                    .when(WasSemaphoreHandler::availablePermits)
                    .thenReturn(0);

            queue
                    .when(() ->
                            WasQueueHandler.offer(asyncContext)
                    )
                    .thenReturn(false);

            queue
                    .when(WasQueueHandler::size)
                    .thenReturn(1000);

            // when

            filter.doFilter(
                    request,
                    response,
                    filterChain
            );

            // then

            /*
             * Load Shedding
             */
            verify(response)
                    .setStatus(
                            HttpServletResponse.SC_SERVICE_UNAVAILABLE
                    );

            /*
             * Async 요청 종료
             */
            verify(asyncContext)
                    .complete();

            /*
             * 실제 Controller까지 전달되면 안 됨
             */
            verify(
                    filterChain,
                    never()
            ).doFilter(
                    any(ServletRequest.class),
                    any(ServletResponse.class)
            );

            /*
             * Queue 삽입 실패 Metric
             */
            verify(metrics)
                    .queueOfferFailed();

            verify(metrics, never())
                    .queueOfferSuccess();

            /*
             * Semaphore를 얻지 못했으므로
             * release하면 안 됨
             */
            semaphore.verify(
                    WasSemaphoreHandler::release,
                    never()
            );
        }
    }
}
