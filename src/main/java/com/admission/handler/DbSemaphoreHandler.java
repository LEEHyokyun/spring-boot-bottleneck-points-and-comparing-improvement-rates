package com.admission.handler;

import java.util.concurrent.Semaphore;

public class DbSemaphoreHandler {

    /*
     * 부하를 대기시키기 위한 대기큐 350개
     * 동시요청을 허용하는 트래픽의 개수는 시스템의 임계치인 100개
     * */
    private static final int MAX_CONCURRENCY = 20;

    //Semaphore : 동시처리 가능한 요청 수 제한
    private static final Semaphore semaphore = new Semaphore(MAX_CONCURRENCY);

    public static boolean tryAcquire() {
        return semaphore.tryAcquire();
    }

    public static void release() {
        semaphore.release();
    }

    public static int availablePermits() {
        return semaphore.availablePermits();
    }

    /*
    * queue worker에서 사용하는 전용 메소드
    *   -> semaphore 슬롯이 생길때까지 worker thread는 기다린다.
    * */
    public static void acquire() throws InterruptedException {
        semaphore.acquire();
    }

}
