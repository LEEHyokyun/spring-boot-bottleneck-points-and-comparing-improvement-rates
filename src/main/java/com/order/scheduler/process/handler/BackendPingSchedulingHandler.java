//package com.leehyokyun.health.scheduler.process.handler;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.batch.core.Job;
//import org.springframework.batch.core.JobParametersBuilder;
//import org.springframework.batch.core.JobParametersInvalidException;
//import org.springframework.batch.core.launch.JobLauncher;
//import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
//import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
//import org.springframework.batch.core.repository.JobRestartException;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//import java.util.concurrent.TimeUnit;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class BackendPingSchedulingHandler {
//
////    @Scheduled(
////        fixedDelay = 10, //10min
////        initialDelay = 10,
////        timeUnit = TimeUnit.MINUTES,
////        scheduler = "backendPingScheduler"
////    )
//    /*
//    * 10분마다 ping .. alive 상태 유지
//    * */
//    //@Scheduled(cron = "0 0 13 ? * SUN")
//    public void backendPingScheduling() {
//        log.info("[BackendPingSchedulingHandler.backendPingScheduling][INFO] Ping is now working.");
//    }
//}
