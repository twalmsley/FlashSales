package uk.co.aosd.flash.services;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quartz job that completes ACTIVE sales whose end time has passed.
 */
@Component
public class CompleteActiveSalesJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(CompleteActiveSalesJob.class);

    @Autowired
    private FlashSalesService flashSalesService;

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        log.info("Starting CompleteActiveSalesJob execution");
        try {
            final int completedCount = flashSalesService.completeActiveSales();
            log.info("CompleteActiveSalesJob completed successfully. Completed {} sale(s)", completedCount);
        } catch (final Exception e) {
            log.error("Error executing CompleteActiveSalesJob", e);
            throw new JobExecutionException("Failed to complete active sales", e);
        }
    }
}
