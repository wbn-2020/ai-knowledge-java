package com.knowflow.service;

import com.knowflow.entity.OperationLog;
import com.knowflow.mapper.OperationLogRepository;
import com.knowflow.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


@Service
public class OperationLogService {
    private final OperationLogRepository repository;

    public OperationLogService(OperationLogRepository repository) {
        this.repository = repository;
    }

    public void record(String action, String targetType, Long targetId, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId(SecurityUtils.getCurrentUserId());
        log.setAction(action);
        log.setModule(targetType);
        log.setPath(currentPath());
        log.setResult("SUCCESS");
        log.setFailureReason(null);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        repository.insert(log);
    }

    private String currentPath() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "";
        }
        HttpServletRequest request = attrs.getRequest();
        return request == null ? "" : request.getRequestURI();
    }
}
