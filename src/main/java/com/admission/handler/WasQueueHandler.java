package com.admission.handler;

import jakarta.servlet.AsyncContext;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class WasQueueHandler {

    /*
     * 450 concurrent 이후 동일하게 DBAdmission을 거친다.
     * 이 중 HikariCP의 임계치인 최대 100개의 동시 요청만 접근하도록 설정
     * */
    private static final int QUEUE_CAPACITY = 800; //20000;

    //JVM Heap 무한 증식을 방지하기 위해 대기 큐를 넣지만, 500개로 제한(총 트래픽의 수는 950)
    private static final BlockingQueue<AsyncContext> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

    /*
    * WAS 대기큐에 요청을 넣고, 자리가 없으면 false 반환.
    * */
    public static boolean offer(AsyncContext asyncContext) {
        return queue.offer(asyncContext);
    }

    /*
    * 대기큐에서 요청을 하나 꺼낸다.
    * 큐가 비어있다면 요청이 들어올때까지 blocking 상태이다.
    * */
    public static AsyncContext takeAsyncContext() throws InterruptedException {
        return queue.take();
    }

    /*
    * Queue에 대기 중인 요청 수
    * */
    public static int size() {
        return queue.size();
    }


}
