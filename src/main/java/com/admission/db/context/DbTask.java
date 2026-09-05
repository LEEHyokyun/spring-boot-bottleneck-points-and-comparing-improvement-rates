//package com.admission.db.context;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.web.context.request.async.DeferredResult;
//
//import java.util.function.Supplier;
//
//@Slf4j
//@RequiredArgsConstructor
//public class DbTask<T> {
//
//    /*
//    * DbTask = 요청을 담는다(Supplier)
//    * */
//    private final Supplier<T> supplier;
//    private final DeferredResult<T> deferredResult;
//
//    public void execute() {
//
//        try {
//            //요청 실행
//            T result = supplier.get();
//
//            //HTTP 결과 반환을 위한 작업
//            deferredResult.setResult(result);
//        } catch (Exception e) {
//
//            //service 실행 중 예외 발생
//            log.error("[DbTask.execute] ERROR OCCURED : " + e.getMessage(), e);
//            deferredResult.setErrorResult(e);
//
//        }
//
//    }
//
//}
