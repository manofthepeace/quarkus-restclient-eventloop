package org.acme;

import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import io.quarkus.scheduler.Scheduled;
import io.vertx.core.Vertx;

public class QuartzResource {

    @Inject
    Vertx vertx;

    @Inject
    ManagedExecutor executor;

    @RestClient
    ClientResource client;

    @Inject
    Logger log;

    @Inject
    org.quartz.Scheduler quartz;

    @Scheduled(every = "1m")
    void myScheduledMethod() throws InterruptedException, SchedulerException {
        log.info("RUNNING FROM SCHEDULED METHOD");
        doCalls();
        log.info("Next 3 calls will be running from task executor");
        CountDownLatch latch = new CountDownLatch(1);
        executor.runAsync(() -> {
            doCalls();
            latch.countDown();
        });
        latch.await();

        CountDownLatch latch2 = new CountDownLatch(1);
        log.info("Next 3 calls will be running from vertx executeBlocking");
        vertx.executeBlocking(promise -> {
            doCalls();
            latch2.countDown();
        });
        latch2.await();

        // Create the job and run now
        JobDetail job = JobBuilder.newJob(MyJob.class).withIdentity("myJob", "myGroup").build();
        Trigger runOnceTrigger = TriggerBuilder.newTrigger().build();
        quartz.scheduleJob(job, runOnceTrigger);
    }

    public void doCalls() {
        log.info("Running REST GET String");
        client.test();
        log.info("Running REST GET Uni<String> with await");
        client.testAsync().await().indefinitely();
        log.info("Running REST GET Uni<String> with subscription");
        CountDownLatch latch = new CountDownLatch(1);
        client.testAsync().subscribe().with(t -> latch.countDown());
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
