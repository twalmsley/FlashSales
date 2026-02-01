package uk.co.aosd.flash.services;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import uk.co.aosd.flash.domain.AdminAuditLogEntry;
import uk.co.aosd.flash.repository.AdminAuditLogRepository;
import uk.co.aosd.flash.security.SecurityUtils;

/**
 * Service for recording admin actions to the audit log.
 * Resolves the current user (ID or username) when present; uses null for background/system actions.
 */
@Service
public class AuditLogService {

    public static final String ACTION_CREATE_PRODUCT = "CREATE_PRODUCT";
    public static final String ACTION_UPDATE_PRODUCT = "UPDATE_PRODUCT";
    public static final String ACTION_DELETE_PRODUCT = "DELETE_PRODUCT";
    public static final String ACTION_CREATE_FLASH_SALE = "CREATE_FLASH_SALE";
    public static final String ACTION_UPDATE_FLASH_SALE = "UPDATE_FLASH_SALE";
    public static final String ACTION_DELETE_FLASH_SALE = "DELETE_FLASH_SALE";
    public static final String ACTION_ADD_FLASH_SALE_ITEM = "ADD_FLASH_SALE_ITEM";
    public static final String ACTION_UPDATE_FLASH_SALE_ITEM = "UPDATE_FLASH_SALE_ITEM";
    public static final String ACTION_REMOVE_FLASH_SALE_ITEM = "REMOVE_FLASH_SALE_ITEM";
    public static final String ACTION_UPDATE_ORDER_STATUS = "UPDATE_ORDER_STATUS";

    public static final String ENTITY_PRODUCT = "PRODUCT";
    public static final String ENTITY_FLASH_SALE = "FLASH_SALE";
    public static final String ENTITY_FLASH_SALE_ITEM = "FLASH_SALE_ITEM";
    public static final String ENTITY_ORDER = "ORDER";

    private final AdminAuditLogRepository adminAuditLogRepository;

    public AuditLogService(final AdminAuditLogRepository adminAuditLogRepository) {
        this.adminAuditLogRepository = adminAuditLogRepository;
    }

    /**
     * Record an admin action. Resolves current user from SecurityContext when present.
     *
     * @param action     e.g. CREATE_PRODUCT, UPDATE_ORDER_STATUS
     * @param entityType e.g. PRODUCT, ORDER
     * @param entityId   the target entity ID
     * @param payload    optional JSON or text (e.g. old/new status)
     */
    public void recordAdminAction(final String action, final String entityType, final UUID entityId, final String payload) {
        final UUID actorUserId = SecurityUtils.getCurrentUserIdOrNull();
        final String actorUsername = SecurityUtils.getCurrentUsernameOrNull();

        final AdminAuditLogEntry entry = AdminAuditLogEntry.builder()
            .actorUserId(actorUserId)
            .actorUsername(actorUsername)
            .action(action)
            .entityType(entityType)
            .entityId(entityId)
            .occurredAt(OffsetDateTime.now())
            .payload(payload)
            .build();

        adminAuditLogRepository.save(entry);
    }

    /**
     * Record an admin action without payload.
     */
    public void recordAdminAction(final String action, final String entityType, final UUID entityId) {
        recordAdminAction(action, entityType, entityId, null);
    }
}
