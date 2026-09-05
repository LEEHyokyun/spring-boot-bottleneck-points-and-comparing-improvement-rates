package com.admission.was.worker;

import com.admission.handler.WasQueueHandler;
import com.admission.handler.WasSemaphoreHandler;
import com.admission.was.filter.WasAdmissionFilter;
import com.admission.was.metrics.WasAdmissionMetrics;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class WasQueueWorkerTest {

    private final WasAdmissionMetrics metrics =
            mock(WasAdmissionMetrics.class);

    /**
     * worker는 spring boot load시점에 12개 생성..이를 재현함
     */
    private void invokeManageQueue(
            WasQueueWorker worker
    ) throws Exception {

        Method method =
                WasQueueWorker.class
                        .getDeclaredMethod("manageQueue");

        method.setAccessible(true);

        method.invoke(worker);
    }


    @Test
    void Queue에서_요청을_꺼내고_Semaphore를_획득한_후_dispatch한다()
            throws Exception {

        // given

        WasQueueWorker worker =
                new WasQueueWorker(metrics);

        AsyncContext asyncContext =
                mock(AsyncContext.class);

        ServletRequest request =
                mock(ServletRequest.class);

        when(asyncContext.getRequest())
                .thenReturn(request);

        /*
         * 첫 번째 take()
         * → 정상적인 AsyncContext 반환
         *
         * 두 번째 take()
         * → Worker 종료를 위해 InterruptedException 발생
         */
        CountDownLatch dispatched =
                new CountDownLatch(1);

        try (
                MockedStatic<WasQueueHandler> queue =
                        mockStatic(WasQueueHandler.class);

                MockedStatic<WasSemaphoreHandler> semaphore =
                        mockStatic(WasSemaphoreHandler.class)
        ) {

            queue
                    .when(WasQueueHandler::takeAsyncContext)
                    .thenReturn(
                            asyncContext
                    )
                    .thenAnswer(invocation -> {
                        throw new InterruptedException();
                    });

            semaphore
                    .when(WasSemaphoreHandler::acquire)
                    .thenAnswer(invocation -> null);

            /*
             * dispatch가 호출되었는지 확인
             */
            doAnswer(invocation -> {
                dispatched.countDown();
                return null;
            }).when(asyncContext).dispatch();

            // when

            Thread workerThread =
                    new Thread(() -> {

                        try {
                            invokeManageQueue(worker);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                    });

            workerThread.start();

            // then

            assertTrue(
                    dispatched.await(
                            2,
                            TimeUnit.SECONDS
                    ),
                    "Worker가 dispatch하지 않았다."
            );

            /*
             * Queue에서 요청을 가져옴
             */
            queue.verify(
                    WasQueueHandler::takeAsyncContext
            );

            /*
             * WAS Semaphore 획득
             */
            semaphore.verify(
                    WasSemaphoreHandler::acquire
            );

            /*
             * Worker가 dispatch할 요청이라는 표시
             */
            verify(request)
                    .setAttribute(
                            eq(
                                    WasAdmissionFilter.class.getName()
                                            + ".ADMISSION_DISPATCH"
                            ),
                            eq(Boolean.TRUE)
                    );

            /*
             * AsyncContext dispatch
             */
            verify(asyncContext)
                    .dispatch();

            /*
             * Worker는 정상 dispatch 후
             * semaphore를 release하면 안 된다.
             *
             * 실제 release는 dispatch 이후
             * WasAdmissionFilter가 수행한다.
             */
            semaphore.verify(
                    WasSemaphoreHandler::release,
                    never()
            );

            /*
             * 종료
             */
            workerThread.interrupt();
            workerThread.join(2000);
        }
    }


    @Test
    void dispatch_중_예외가_발생하면_Semaphore를_반환하고_요청을_종료한다()
            throws Exception {

        // given

        WasQueueWorker worker =
                new WasQueueWorker(metrics);

        AsyncContext asyncContext =
                mock(AsyncContext.class);

        ServletRequest request =
                mock(ServletRequest.class);

        when(asyncContext.getRequest())
                .thenReturn(request);

        RuntimeException dispatchException =
                new RuntimeException("dispatch failed");

        try (
                MockedStatic<WasQueueHandler> queue =
                        mockStatic(WasQueueHandler.class);

                MockedStatic<WasSemaphoreHandler> semaphore =
                        mockStatic(WasSemaphoreHandler.class)
        ) {

            queue
                    .when(WasQueueHandler::takeAsyncContext)
                    .thenReturn(asyncContext)
                    .thenAnswer(invocation -> {
                        throw new InterruptedException();
                    });

            semaphore
                    .when(WasSemaphoreHandler::acquire)
                    .thenAnswer(invocation -> null);

            /*
             * dispatch에서 예외 발생
             */
            doThrow(dispatchException)
                    .when(asyncContext)
                    .dispatch();

            // when

            Thread workerThread =
                    new Thread(() -> {

                        try {
                            invokeManageQueue(worker);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                    });

            workerThread.start();

            /*
             * Worker가 dispatch 실패를 처리할 시간을 준다.
             */
            Thread.sleep(100);

            // then

            /*
             * Semaphore를 이미 획득한 상태에서
             * dispatch가 실패했으므로
             *
             * 반드시 release해야 한다.
             */
            semaphore.verify(
                    WasSemaphoreHandler::release
            );

            /*
             * 요청도 종료해야 한다.
             */
            verify(asyncContext)
                    .complete();

            /*
             * dispatch 시도
             */
            verify(asyncContext)
                    .dispatch();

            /*
             * 종료
             */
            workerThread.interrupt();
            workerThread.join(2000);
        }
    }


    @Test
    void Worker가_Queue에서_요청을_가져왔음을_기록한다()
            throws Exception {

        // given

        WasQueueWorker worker =
                new WasQueueWorker(metrics);

        AsyncContext asyncContext =
                mock(AsyncContext.class);

        ServletRequest request =
                mock(ServletRequest.class);

        when(asyncContext.getRequest())
                .thenReturn(request);

        CountDownLatch dispatched =
                new CountDownLatch(1);

        try (
                MockedStatic<WasQueueHandler> queue =
                        mockStatic(WasQueueHandler.class);

                MockedStatic<WasSemaphoreHandler> semaphore =
                        mockStatic(WasSemaphoreHandler.class)
        ) {

            queue
                    .when(WasQueueHandler::takeAsyncContext)
                    .thenReturn(asyncContext)
                    .thenAnswer(invocation -> {
                        throw new InterruptedException();
                    });

            semaphore
                    .when(WasSemaphoreHandler::acquire)
                    .thenAnswer(invocation -> null);

            doAnswer(invocation -> {
                dispatched.countDown();
                return null;
            }).when(asyncContext).dispatch();

            // when

            Thread workerThread =
                    new Thread(() -> {

                        try {
                            invokeManageQueue(worker);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                    });

            workerThread.start();

            assertTrue(
                    dispatched.await(
                            2,
                            TimeUnit.SECONDS
                    )
            );

            // then

            verify(metrics)
                    .workerTake();

            verify(metrics)
                    .workerAcquire();

            verify(metrics)
                    .workerDispatch();

            workerThread.interrupt();
            workerThread.join(2000);
        }
    }
}
