package com.admission.was.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WasAdmissionMetrics {

    private final MeterRegistry registry;

    // WAS Semaphore 직접 획득 성공
    public void acquireSuccess() {
        registry.counter("was_admission_acquire_total",
                "result", "success").increment();
    }

    // WAS Semaphore 직접 획득 실패
    public void acquireFailed() {
        registry.counter("was_admission_acquire_total",
                "result", "failed").increment();
    }

    // 대기 큐 삽입 성공
    public void queueOfferSuccess() {
        registry.counter("was_admission_queue_total",
                "result", "success").increment();
    }

    // 대기 큐 삽입 실패
    public void queueOfferFailed() {
        registry.counter("was_admission_queue_total",
                "result", "failed").increment();
    }

    // Queue Worker가 요청을 dispatch한 횟수
    public void workerDispatch() {
        registry.counter("was_admission_worker_dispatch_total")
                .increment();
    }

    // Queue Worker가 큐에서 요청을 꺼낸 횟수
    public void workerTake() {
        registry.counter("was_admission_worker_take_total")
                .increment();
    }

    // Queue Worker가 Semaphore 획득에 성공한 횟수
    public void workerAcquire() {
        registry.counter("was_admission_worker_acquire_total")
                .increment();
    }

    // Queue Worker에서 예외가 발생한 횟수
    public void workerError() {
        registry.counter("was_admission_worker_error_total")
                .increment();
    }

    // Queue Worker가 interrupt된 횟수
    public void workerInterrupted() {
        registry.counter("was_admission_worker_interrupted_total")
                .increment();
    }
}
