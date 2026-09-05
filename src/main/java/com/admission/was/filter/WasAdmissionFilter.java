package com.admission.was.filter;

import com.admission.handler.WasQueueHandler;
import com.admission.handler.WasSemaphoreHandler;
import com.admission.was.metrics.WasAdmissionMetrics;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class WasAdmissionFilter extends GenericFilterBean {

    private final WasAdmissionMetrics metrics;

    /*
     * 해당 요청이 Client로부터 온 요청인지,
     * Queue에 들어있다가 다시 permit 득을 시도하는 요청인지(->AsyncContext.dispatch)
     * 이 두개를 구분하기 위한 prefix
     * */
    private static final String ADMISSION_DISPATCH = WasAdmissionFilter.class.getName() + ".ADMISSION_DISPATCH";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        /*
         * Queue Worker가 Async.dispatch한 요청인가, Client에서 비롯한 raw 요청인가를 먼저 판단.
         * - 최초 HTTP 요청이라면? > Queue 대기하다가 dispatch된 요청(worker 측에서 semaphore를 득하고 보낸것임) -> 그냥 WAS로 보낸다.
         * - dispatch된 요청이라면? > semaphore 득하고 WAS로 보내거나, semaphore 득 못하면 queue로 보내고 이를 AsyncContext로 전환.
         * */
        if(request.getAttribute(ADMISSION_DISPATCH) != null){
            /* queue에서 dispatch한 요청
             *   -> 이미 semaphore 득함
             *   -> 그대로 was에 흘러보낸다.
             *   -> 그리고 semaphore(동시요청) 슬롯 하나를 반환한다.
             * */
            try {
                filterChain.doFilter(request,response);
            } finally {
                WasSemaphoreHandler.release();
                request.removeAttribute(ADMISSION_DISPATCH);
            }

            return;
        }

        if(WasSemaphoreHandler.tryAcquire()){
            /* 최초 요청
             *  -> 현재 슬롯이 있는지 확인한다.
             *  -> 있다면 득해서 보내고 반환한다.
             *  -> 없다면 큐에 넣는다.
             * */
            metrics.acquireSuccess();

            log.info(
                    "WAS permit available = {}",
                    WasSemaphoreHandler.availablePermits()
            );

            try {
                filterChain.doFilter(request,response);

                log.info("WAS EXIT : permits={}",
                        WasSemaphoreHandler.availablePermits());

            } finally {
                WasSemaphoreHandler.release();

                log.info("WAS RELEASE : permits={}",
                        WasSemaphoreHandler.availablePermits());
            }

            return;
        }

        /*
         * WAS 처리 슬롯이 없는 경우(현재 동시요청이 450개 꽉참)
         *  -> queue에 해당 요청을 넣기 위해 AsyncContext로 전환한다.
         * */
        metrics.acquireFailed();

        log.warn("WAS ACQUIRE FAILED : permits={}",
                WasSemaphoreHandler.availablePermits());

        AsyncContext asyncContext = request.startAsync();

        boolean queued = WasQueueHandler.offer(asyncContext);

        log.warn("WAS QUEUE : queued={}, size={}, permits={}",
                queued,
                WasQueueHandler.size(),
                WasSemaphoreHandler.availablePermits());

        /*
         * 대기큐 마저 꽉찼다면 load shedding
         *   -> WAS filter level에서 요청 자체를 받지 않고 거절한다.
         * */
        if(!queued){

            metrics.queueOfferFailed();

            //503 Error 반환
//            response.setStatus(
//                    HttpServletResponse.SC_SERVICE_UNAVAILABLE
//            );
            ((HttpServletResponse) response).setStatus(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE
            );

            //그대로 해당 요청은 종료시킨다.
            asyncContext.complete();

            return;
        } else {

            metrics.queueOfferSuccess();

        }

        /*
         * 요청들은 tomcat thread의 동기 처리가 아닌 비동기화(Async Context)된다.
         * 요청들이 진행되는 동안 thread들은 처리 응답을 기다리지 않고 제어권을 바로 넘기며, thread들은 다음 요청을 처리한다.
         */

    }
}
