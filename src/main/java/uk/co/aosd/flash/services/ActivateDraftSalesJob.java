package uk.co.aosd.flash.services;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quartz job that activates DRAFT sales whose start time has passed.
 */
@Component
public class ActivateDraftSalesJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(ActivateDraftSalesJob.class);

    @Autowired
    private FlashSalesService flashSalesService;

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        log.info("Starting ActivateDraftSalesJob execution");
        try {
            final int activatedCount = flashSalesService.activateDraftSales();
            log.info("ActivateDraftSalesJob completed successfully. Activated {} sale(s)", activatedCount);
        } catch (final Exception e) {
            log.error("Error executing ActivateDraftSalesJob", e);
            throw new JobExecutionException("Failed to activate draft sales", e);
        }
    }
}
