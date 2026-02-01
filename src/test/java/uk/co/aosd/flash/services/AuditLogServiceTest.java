package uk.co.aosd.flash.services;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.co.aosd.flash.domain.AdminAuditLogEntry;
import uk.co.aosd.flash.repository.AdminAuditLogRepository;

/**
 * Unit tests for AuditLogService.
 */
class AuditLogServiceTest {

    private AdminAuditLogRepository adminAuditLogRepository;
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        adminAuditLogRepository = Mockito.mock(AdminAuditLogRepository.class);
        auditLogService = new AuditLogService(adminAuditLogRepository);
    }

    @Test
    void recordAdminAction_savesEntryWithActionEntityTypeAndEntityId() {
        final UUID entityId = UUID.randomUUID();
        auditLogService.recordAdminAction(AuditLogService.ACTION_CREATE_PRODUCT, AuditLogService.ENTITY_PRODUCT, entityId);

        verify(adminAuditLogRepository).save(argThat((AdminAuditLogEntry e) ->
            AuditLogService.ACTION_CREATE_PRODUCT.equals(e.getAction())
                && AuditLogService.ENTITY_PRODUCT.equals(e.getEntityType())
                && entityId.equals(e.getEntityId())
                && e.getOccurredAt() != null));
    }

    @Test
    void recordAdminAction_withPayload_savesEntryWithPayload() {
        final UUID entityId = UUID.randomUUID();
        final String payload = "{\"from\":\"PENDING\",\"to\":\"PAID\"}";
        auditLogService.recordAdminAction(AuditLogService.ACTION_UPDATE_ORDER_STATUS, AuditLogService.ENTITY_ORDER, entityId, payload);

        verify(adminAuditLogRepository).save(argThat((AdminAuditLogEntry e) ->
            payload.equals(e.getPayload())));
    }
}
