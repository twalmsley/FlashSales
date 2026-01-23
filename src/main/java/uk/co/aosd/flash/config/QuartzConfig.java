package uk.co.aosd.flash.config;

import java.util.HashMap;

import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import uk.co.aosd.flash.services.ActivateDraftSalesJob;
import uk.co.aosd.flash.services.CompleteActiveSalesJob;

/**
 * Configuration for Quartz scheduled jobs.
 */
@Configuration
public class QuartzConfig {

    @Value("${app.scheduler.interval-seconds:30}")
    private int intervalSeconds;

    /**
     * Job detail for activating draft sales.
     */
    @Bean
    public JobDetailFactoryBean activateDraftSalesJobDetail() {
        final JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(ActivateDraftSalesJob.class);
        factoryBean.setName("activateDraftSalesJob");
        factoryBean.setDescription("Activates DRAFT sales whose start time has passed");
        factoryBean.setDurability(true);
        factoryBean.setJobDataAsMap(new HashMap<>());
        factoryBean.afterPropertiesSet();
        return factoryBean;
    }

    /**
     * Trigger for activating draft sales job.
     */
    @Bean
    public SimpleTriggerFactoryBean activateDraftSalesTrigger(final JobDetail activateDraftSalesJobDetail) {
        final SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(activateDraftSalesJobDetail);
        factoryBean.setName("activateDraftSalesTrigger");
        factoryBean.setDescription("Trigger for activating draft sales");
        factoryBean.setRepeatInterval(intervalSeconds * 1000L); // Convert seconds to milliseconds
        factoryBean.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
        factoryBean.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT);
        factoryBean.afterPropertiesSet();
        return factoryBean;
    }

    /**
     * Job detail for completing active sales.
     */
    @Bean
    public JobDetailFactoryBean completeActiveSalesJobDetail() {
        final JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(CompleteActiveSalesJob.class);
        factoryBean.setName("completeActiveSalesJob");
        factoryBean.setDescription("Completes ACTIVE sales whose end time has passed");
        factoryBean.setDurability(true);
        factoryBean.setJobDataAsMap(new HashMap<>());
        factoryBean.afterPropertiesSet();
        return factoryBean;
    }

    /**
     * Trigger for completing active sales job.
     */
    @Bean
    public SimpleTriggerFactoryBean completeActiveSalesTrigger(final JobDetail completeActiveSalesJobDetail) {
        final SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(completeActiveSalesJobDetail);
        factoryBean.setName("completeActiveSalesTrigger");
        factoryBean.setDescription("Trigger for completing active sales");
        factoryBean.setRepeatInterval(intervalSeconds * 1000L); // Convert seconds to milliseconds
        factoryBean.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
        factoryBean.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT);
        factoryBean.afterPropertiesSet();
        return factoryBean;
    }
}
