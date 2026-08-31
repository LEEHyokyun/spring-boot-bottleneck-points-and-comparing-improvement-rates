//package com.leehyokyun.health.scheduler.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.scheduling.annotation.EnableAsync;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
//
//@EnableAsync
//@EnableScheduling
//@Configuration
//public class BackendPingSchedulingConfig {
//
//    //비동기 스레드 풀 환경 구성 : 단일 스레드 풀 10분 주기 ping
//    //Single Thread
//    @Bean
//    public ThreadPoolTaskScheduler backendPingScheduler() {
//
//        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
//        threadPoolTaskScheduler.setPoolSize(1); //single thread
//        threadPoolTaskScheduler.setThreadNamePrefix("backendPingScheduler-");
//        threadPoolTaskScheduler.initialize();
//
//        return threadPoolTaskScheduler;
//    }
//
//}
