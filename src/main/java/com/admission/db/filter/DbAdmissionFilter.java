//package com.admission.db.filter;
//
//import com.admission.db.context.DbTask;
//import com.admission.db.exception.DbOverLoadedException;
//import com.admission.handler.DbSemaphoreHandler;
//import org.springframework.stereotype.Component;
//import org.springframework.web.context.request.async.DeferredResult;
//
//@Component
//public class DbAdmissionFilter {
//
//    /*
//    * DB 접근을 시도,
//    *   -> DB Semaphore 슬롯이 없다면 DB로 요청을 보낸다.
//    *   -> 슬롯이 없다면 대기큐가 없으므로 진입시키지 않는다.
//    * */
//
//    public static <T> T execute(DbTask<T> dbTask){
//        if(!DbSemaphoreHandler.tryAcquire()){
//            /*
//            * DB 동시 처리 임계점 돌파, AdmissionFilter 작동
//            * */
//            throw new DbOverLoadedException("DB Semaphore is over");
//        }
//
//        try {
//
//            //요청 그대로 작업 진행
//            return dbTask.execute();
//
//        } finally {
//
//            //Semaphore 슬롯 반환
//            DbSemaphoreHandler.release();
//
//        }
//    }
//
//}
