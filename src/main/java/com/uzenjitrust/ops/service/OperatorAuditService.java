package com.uzenjitrust.ops.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uzenjitrust.common.security.ActorPrincipal;
import com.uzenjitrust.common.security.ActorProvider;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.common.error.ForbiddenException;
import com.uzenjitrust.ops.domain.OperatorAuditEventEntity;
import com.uzenjitrust.ops.repo.OperatorAuditEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class OperatorAuditService {

    private static final Logger log = LoggerFactory.getLogger(OperatorAuditService.class);

    public static final String ACTION_OPERATOR_ACTION_FAILED = "OPERATOR_ACTION_FAILED";
    public static final String ACTION_ADMIN_FORBIDDEN_ACCESS_ATTEMPT = "ADMIN_FORBIDDEN_ACCESS_ATTEMPT";

    private final OperatorAuditEventRepository operatorAuditEventRepository;
    private final ActorProvider actorProvider;
    private final ObjectMapper objectMapper;

    public OperatorAuditService(OperatorAuditEventRepository operatorAuditEventRepository,
                                ActorProvider actorProvider,
                                ObjectMapper objectMapper) {
        this.operatorAuditEventRepository = operatorAuditEventRepository;
        this.actorProvider = actorProvider;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(String actionType,
                              String resourceType,
                              String resourceId,
                              String reason,
                              Object metadata) {
        persist(actionType, resourceType, resourceId, "SUCCESS", reason, metadata, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordNoop(String actionType,
                           String resourceType,
                           String resourceId,
                           String reason,
                           Object metadata) {
        persist(actionType, resourceType, resourceId, "NOOP", reason, metadata, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String attemptedActionType,
                              String resourceType,
                              String resourceId,
                              String reason,
                              Object metadata,
                              String errorDetail) {
        Map<String, Object> enrichedMetadata = new LinkedHashMap<>();
        enrichedMetadata.put("attemptedActionType", attemptedActionType);
        mergeMetadata(enrichedMetadata, metadata);
        persist(ACTION_OPERATOR_ACTION_FAILED, resourceType, resourceId, "FAILED", reason, enrichedMetadata, errorDetail);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordForbidden(String attemptedActionType,
                                String resourceType,
                                String resourceId,
                                String reason,
                                Object metadata,
                                String errorDetail) {
        Map<String, Object> enrichedMetadata = new LinkedHashMap<>();
        enrichedMetadata.put("attemptedActionType", attemptedActionType);
        mergeMetadata(enrichedMetadata, metadata);
        persist(ACTION_ADMIN_FORBIDDEN_ACCESS_ATTEMPT, resourceType, resourceId, "FORBIDDEN", reason, enrichedMetadata, errorDetail);
    }

    @Transactional(readOnly = true)
    public Page<OperatorAuditView> list(String actionType,
                                        String outcome,
                                        UUID actorUserId,
                                        String resourceType,
                                        int page,
                                        int size,
                                        String sortBy,
                                        Sort.Direction direction) {
        requireAdmin();
        Specification<OperatorAuditEventEntity> spec = Specification.<OperatorAuditEventEntity>where(matchesString("actionType", actionType))
                .and(matchesString("outcome", outcome == null ? null : outcome.toUpperCase()))
                .and(matchesUuid("actorUserId", actorUserId))
                .and(matchesString("resourceType", resourceType));
        return operatorAuditEventRepository.findAll(spec, PageRequest.of(page, size, Sort.by(direction, sortBy)))
                .map(this::toView);
    }

    @Transactional(readOnly = true)
    public OperatorAuditDetailView get(UUID id) {
        requireAdmin();
        return operatorAuditEventRepository.findById(id)
                .map(this::toDetail)
                .orElseThrow(() -> new com.uzenjitrust.common.error.NotFoundException("Operator audit event not found"));
    }

    private void persist(String actionType,
                         String resourceType,
                         String resourceId,
                         String outcome,
                         String reason,
                         Object metadata,
                         String errorDetail) {
        try {
            OperatorAuditEventEntity entity = new OperatorAuditEventEntity();
            ActorPrincipal actor = currentActor();
            entity.setActorUserId(actor == null ? null : actor.userId());
            entity.setActorRoles(toJson(actor == null ? null : actor.roles().stream().map(AppRole::name).toList()));
            entity.setActionType(actionType);
            entity.setResourceType(resourceType);
            entity.setResourceId(resourceId);
            entity.setOutcome(outcome);
            entity.setReason(sanitizeText(reason));
            entity.setCorrelationId(sanitizeText(MDC.get("correlationId")));
            entity.setRequestPath(sanitizeText(currentRequestPath()));
            entity.setRequestMethod(sanitizeText(currentRequestMethod()));
            entity.setMetadata(sanitizeMetadata(metadata));
            entity.setErrorDetail(sanitizeText(errorDetail));
            operatorAuditEventRepository.save(entity);
        } catch (Exception ex) {
            log.warn("Failed to persist operator audit event actionType={} resourceType={} resourceId={} error={}",
                    actionType, resourceType, resourceId, ex.getMessage());
        }
    }

    private JsonNode sanitizeMetadata(Object metadata) {
        if (metadata == null) {
            return null;
        }
        JsonNode node = toJson(metadata);
        return sanitizeNode(node);
    }

    private JsonNode sanitizeNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            var sanitized = objectMapper.createObjectNode();
            node.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                if (isSensitiveKey(key)) {
                    return;
                }
                sanitized.set(key, sanitizeNode(entry.getValue()));
            });
            return sanitized;
        }
        if (node.isArray()) {
            var array = objectMapper.createArrayNode();
            node.forEach(child -> array.add(sanitizeNode(child)));
            return array;
        }
        if (node.isTextual()) {
            return objectMapper.getNodeFactory().textNode(sanitizeText(node.asText()));
        }
        return node;
    }

    private boolean isSensitiveKey(String key) {
        String normalized = key.toLowerCase();
        return normalized.contains("password")
                || normalized.contains("secret")
                || normalized.contains("token")
                || normalized.contains("authorization")
                || normalized.contains("signature");
    }

    private JsonNode toJson(Object value) {
        return value == null ? null : objectMapper.valueToTree(value);
    }

    private ActorPrincipal currentActor() {
        try {
            return actorProvider.requireActor();
        } catch (Exception ex) {
            return null;
        }
    }

    private void requireAdmin() {
        ActorPrincipal actor = actorProvider.requireActor();
        if (!actor.roles().contains(AppRole.ADMIN)) {
            throw new ForbiddenException("Insufficient role");
        }
    }

    private String currentRequestPath() {
        HttpServletRequest request = currentRequest();
        return request == null ? null : request.getRequestURI();
    }

    private String currentRequestMethod() {
        HttpServletRequest request = currentRequest();
        return request == null ? null : request.getMethod();
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private String sanitizeText(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = value.length() > 2000 ? value.substring(0, 2000) : value;
        if (sanitized.contains("Authorization")) {
            return "[sanitized]";
        }
        return sanitized;
    }

    private void mergeMetadata(Map<String, Object> target, Object metadata) {
        if (metadata == null) {
            return;
        }
        if (metadata instanceof Map<?, ?> map) {
            map.forEach((key, value) -> target.put(String.valueOf(key), value));
        } else {
            target.put("details", metadata);
        }
    }

    private static Specification<OperatorAuditEventEntity> matchesString(String field, String value) {
        return (root, query, cb) -> value == null || value.isBlank() ? null : cb.equal(root.get(field), value);
    }

    private static Specification<OperatorAuditEventEntity> matchesUuid(String field, UUID value) {
        return (root, query, cb) -> value == null ? null : cb.equal(root.get(field), value);
    }

    private OperatorAuditView toView(OperatorAuditEventEntity entity) {
        return new OperatorAuditView(
                entity.getId(),
                entity.getActorUserId(),
                entity.getActorRoles(),
                entity.getActionType(),
                entity.getResourceType(),
                entity.getResourceId(),
                entity.getOutcome(),
                entity.getCreatedAt()
        );
    }

    private OperatorAuditDetailView toDetail(OperatorAuditEventEntity entity) {
        return new OperatorAuditDetailView(
                entity.getId(),
                entity.getActorUserId(),
                entity.getActorRoles(),
                entity.getActionType(),
                entity.getResourceType(),
                entity.getResourceId(),
                entity.getOutcome(),
                entity.getReason(),
                entity.getCorrelationId(),
                entity.getRequestPath(),
                entity.getRequestMethod(),
                entity.getMetadata(),
                entity.getErrorDetail(),
                entity.getCreatedAt()
        );
    }

    public record OperatorAuditView(
            UUID id,
            UUID actorUserId,
            JsonNode actorRoles,
            String actionType,
            String resourceType,
            String resourceId,
            String outcome,
            Instant createdAt
    ) { }

    public record OperatorAuditDetailView(
            UUID id,
            UUID actorUserId,
            JsonNode actorRoles,
            String actionType,
            String resourceType,
            String resourceId,
            String outcome,
            String reason,
            String correlationId,
            String requestPath,
            String requestMethod,
            JsonNode metadata,
            String errorDetail,
            Instant createdAt
    ) { }
}
