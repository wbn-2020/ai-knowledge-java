package com.knowflow.modules.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentProcessTaskRepository extends JpaRepository<DocumentProcessTask, Long> {
    Page<DocumentProcessTask> findByDeletedFalse(Pageable pageable);
}
