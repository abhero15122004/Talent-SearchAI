package com.talentmatch.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Value("${app.async.resume-core-pool-size:4}")
    private int corePoolSize;

    @Value("${app.async.resume-max-pool-size:8}")
    private int maxPoolSize;

    @Value("${app.async.resume-queue-capacity:200}")
    private int queueCapacity;

    @Bean(name = "resumeTaskExecutor")
    public Executor resumeTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("resume-exec-");
        // CallerRunsPolicy provides natural backpressure if queue becomes completely saturated
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("Initialized bounded resume task executor (core: {}, max: {}, queueCapacity: {})",
                corePoolSize, maxPoolSize, queueCapacity);
        return executor;
    }
}
