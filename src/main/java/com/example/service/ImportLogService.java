package com.example.service;

import com.example.model.ImportLog;
import com.example.model.ImportStatus;
import com.example.repository.ImportLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportLogService {

    @Autowired
    private ImportLogRepository importLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImportLog createNewLog() {
        ImportLog log = new ImportLog();
        log.setStatus(ImportStatus.IN_PROGRESS);
        return importLogRepository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsSuccess(Long logId, int count) {
        importLogRepository.findById(logId).ifPresent(log -> {
            log.setStatus(ImportStatus.SUCCESS);
            log.setAddedCount(count);
            importLogRepository.save(log);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(Long logId, String errorMessage) {
        importLogRepository.findById(logId).ifPresent(log -> {
            log.setStatus(ImportStatus.FAILED);
            log.setErrorDetails(errorMessage.length() > 1024 ? errorMessage.substring(0, 1024) : errorMessage);
            importLogRepository.save(log);
        });
    }
}