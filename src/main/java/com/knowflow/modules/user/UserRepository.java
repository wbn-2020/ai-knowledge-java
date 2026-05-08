package com.knowflow.modules.user;

import com.knowflow.common.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameAndDeletedFalse(String username);
    Optional<User> findByEmailAndDeletedFalse(String email);
    Optional<User> findByIdAndDeletedFalse(Long id);
    boolean existsByUsernameAndDeletedFalse(String username);
    boolean existsByEmailAndDeletedFalse(String email);
    Page<User> findByDeletedFalseAndUsernameContainingAndStatus(String username, UserStatus status, Pageable pageable);
    Page<User> findByDeletedFalseAndUsernameContaining(String username, Pageable pageable);
}
