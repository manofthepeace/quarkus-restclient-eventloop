package org.acme;

import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import io.vertx.core.Vertx;

public class MyJob implements Job {
    @RestClient
    ClientResource client;

    @Inject
    ManagedExecutor executor;

    @Inject
    Vertx vertx;

    @Inject
    Logger log;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("RUNNING FROM QUARTZ JOB");

        doCalls();

        log.info("Next 3 calls will be running from task executor");
        CountDownLatch latch = new CountDownLatch(1);
        executor.runAsync(() -> {
            doCalls();
            latch.countDown();
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        CountDownLatch latch2 = new CountDownLatch(1);
        log.info("Next 3 calls will be running from vertx executeBlocking");
        vertx.executeBlocking(promise -> {
            doCalls();
            latch2.countDown();
        });
        try {
            latch2.await();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void doCalls() {
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
