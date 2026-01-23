package uk.co.aosd.flash.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Tests for ActivateDraftSalesJob.
 */
public class ActivateDraftSalesJobTest {

    private ActivateDraftSalesJob job;
    private FlashSalesService flashSalesService;
    private JobExecutionContext jobExecutionContext;

    @BeforeEach
    public void setUp() throws Exception {
        flashSalesService = Mockito.mock(FlashSalesService.class);
        jobExecutionContext = Mockito.mock(JobExecutionContext.class);
        job = new ActivateDraftSalesJob();
        
        // Use reflection to inject the mock service
        final Field field = ActivateDraftSalesJob.class.getDeclaredField("flashSalesService");
        field.setAccessible(true);
        field.set(job, flashSalesService);
    }

    @Test
    public void shouldExecuteJobSuccessfully() throws JobExecutionException {
        when(flashSalesService.activateDraftSales()).thenReturn(3);

        job.execute(jobExecutionContext);

        verify(flashSalesService, times(1)).activateDraftSales();
    }

    @Test
    public void shouldHandleZeroActivatedSales() throws JobExecutionException {
        when(flashSalesService.activateDraftSales()).thenReturn(0);

        job.execute(jobExecutionContext);

        verify(flashSalesService, times(1)).activateDraftSales();
    }

    @Test
    public void shouldThrowJobExecutionExceptionOnServiceError() {
        final RuntimeException serviceException = new RuntimeException("Service error");
        doThrow(serviceException).when(flashSalesService).activateDraftSales();

        assertThrows(JobExecutionException.class, () -> {
            job.execute(jobExecutionContext);
        });

        verify(flashSalesService, times(1)).activateDraftSales();
    }

}
