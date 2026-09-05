//package com.admission.handler;
//
//import com.admission.db.context.DbTask;
//import org.springframework.web.context.request.async.DeferredResult;
//
//import java.util.concurrent.ArrayBlockingQueue;
//import java.util.concurrent.BlockingQueue;
//
//public class DbQueueHandler {
//
//    /*
//     * Service 차원에서 대기큐를 운영할 경우 AsyncContext가 불가능
//     * 대기큐에 들어가서 dispatch될때까지 모든 과정을 Tomcat Thread가 기다리게 된다.
//     * 대기큐는 적용하지 않는다.
//     * 대신 Sempaphore만 최대치 수준으로 유지(200).
//     * */
//
//    /*
//     * 450 concurrent 이후 동일하게 DBAdmission을 거친다.
//     * 이 중 HikariCP의 임계치인 최대 100개의 동시 요청만 접근하도록 설정
//     * */
//    private static final int QUEUE_CAPACITY = 50;//350;
//
//    private static final BlockingQueue<DbTask<?>> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
//
//    public static boolean offer(DbTask<?> dbtask) {
//        return queue.offer(dbtask);
//    }
//
//    public static DbTask<?> take() throws InterruptedException {
//        return queue.take();
//    }
//
//    public static int size() {
//        return queue.size();
//    }
//}
