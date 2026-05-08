package com.knowflow.service;

import com.knowflow.entity.OperationLog;
import com.knowflow.mapper.OperationLogRepository;
import com.knowflow.security.SecurityUtils;
import org.springframework.stereotype.Service;


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
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        repository.insert(log);
    }
}
