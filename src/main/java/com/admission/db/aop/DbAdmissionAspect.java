package com.admission.db.aop;

import com.admission.db.exception.DbOverLoadedException;
import com.admission.handler.DbSemaphoreHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class DbAdmissionAspect {

    @Around("@annotation(com.admission.db.aop.DbAdmission)")
    public Object admission(ProceedingJoinPoint joinPoint) {

        /*
        * tomcat service는 AOP를 통해 세마포어를 득하지 못하면 대기큐에 넣어진다.
        * 비동기 큐에 넣어진 요청들은 나중에 워커 스레드를 통해 요청을 비동기적으로 실행하게 된다.
        * */
        boolean acquired = DbSemaphoreHandler.tryAcquire();

        if(!acquired){
            /*
            * 동시처리작업의 최대치 도달 시 해당 요청은 예외 발생
            * */
            throw new DbOverLoadedException(
                    "DB LOAD IS FULL : LOAD SHEDDING OCCURED."
            );
        }

        /*
        * 세마포어를 획득한 요청은 실행 대상
        * */
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            /*
            * 처리 중 예외 발생 시에도 반드시 세마포어 슬롯은 반환한다.
            * */
            DbSemaphoreHandler.release();
        }
    }

}
