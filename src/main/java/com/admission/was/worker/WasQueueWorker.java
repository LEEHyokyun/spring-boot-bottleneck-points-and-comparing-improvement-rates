package com.admission.was.worker;

import com.admission.handler.WasQueueHandler;
import com.admission.handler.WasSemaphoreHandler;
import com.admission.was.filter.WasAdmissionFilter;
import com.admission.was.metrics.WasAdmissionMetrics;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.AsyncContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
* 처리 슬롯이 반환될때 대기큐에 있는 요청을 하나씩 뺀다.
* */
@Slf4j
@Component
@RequiredArgsConstructor
public class WasQueueWorker {

    private final WasAdmissionMetrics metrics;

    /*
    * WAS 대기큐를 항상 검사하며, tomcat thread가 아닌 별도 제어권을 넘겨받아 처리하는 스레드들이다.
    */
    //private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ExecutorService executor = Executors.newFixedThreadPool(12);

    /*
    * Async Context 이름에 붙일 prefix
    */
    private static final String ADMISSION_DISPATCH = WasAdmissionFilter.class.getName() + ".ADMISSION_DISPATCH";

    @PostConstruct
    public void init() {
        /*
        * 본 워커 스레드는 application 실행 시 1개의 스레드로 queue 관리에 집중하는 전용 워커 스레드이다.
        * */
        for (int i = 0; i < 12; i++) {
            executor.submit(this::manageQueue);
        }
    }

    private void manageQueue() {
        /*
        * 이 단일 워커 스레드는 애플리케이션이 실행하는 동안 계속 queue만 관리한다.
        * */
        while(!Thread.currentThread().isInterrupted()) {

            AsyncContext asyncContext = null;
            boolean acquired = false;

            try{

                /*
                * queue에 대기 요청이 들어올때까지 blocking.
                * 전용 워커 스레드만 blocking, 요청이 들어온다면 그때 관리 시작.
                * */
                asyncContext = WasQueueHandler.takeAsyncContext();
                metrics.workerTake();

                /*
                * queue에서 빼고 WAS 처리 슬롯을 확보할때까지 기다린다.
                * */
                WasSemaphoreHandler.acquire();
                acquired = true;
                metrics.workerAcquire();

                /*
                * 처리슬롯을 확보했다면 비동기(제어권이 넘겨진) 요청을 본 워커 스레드가 dispatch,
                * 이에 대한 attribute 별도 처리해서 prefix 남긴다.
                * */
                asyncContext
                        .getRequest()
                        .setAttribute(
                            ADMISSION_DISPATCH,
                            Boolean.TRUE
                        );
                asyncContext.dispatch();
                metrics.workerDispatch();

                acquired = false;

            } catch (InterruptedException e) {
                /*
                * JVM 종료 등으로 worker interrupted / 예외 상황 발생 시
                * */
                Thread.currentThread().interrupt();

                /*
                * 만약 dispatch 할때 실패하면,
                * 해당 async context가 dispatch 실패, 이에 따라 release 처리 누락 및 처리 슬롯 1개가 영구적으로 닫힌다.
                * (semaphore를 득했는데 dispatch 하는 과정에서 실패하면 context가 잡히지 않는다. 따라서 별도 semaphore를 release 필요)
                * */
                if(acquired){
                    //얻은 상태에서 실패 > 처리 슬롯 확보해야 함
                    WasSemaphoreHandler.release();
                }

                return;
            } catch (Exception Unchecked){

                /*
                * dispatch 실패 및 모든 Unchecked Exception
                */
                if(acquired){
                    WasSemaphoreHandler.release();
                }

                if(asyncContext != null){
                    //semaphore 득을 얻지 못한 상태라면 해당 요청은 실패 처리
                    try {
                        asyncContext.complete();
                    } catch (Exception extraException) {
                        log.error("[WasQueueWorker.manageQueue] Exception Occured  : " + extraException.getMessage(), extraException);
                    }
                }

            }

        }
    }
}
