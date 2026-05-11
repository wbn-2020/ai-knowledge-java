package com.knowflow.service;

import com.knowflow.common.BusinessException;
import com.knowflow.enums.UserRole;
import com.knowflow.enums.UserStatus;
import com.knowflow.dto.LoginRequest;
import com.knowflow.vo.LoginVO;
import com.knowflow.dto.RegisterRequest;
import com.knowflow.entity.KnowledgeBase;
import com.knowflow.mapper.KnowledgeBaseRepository;
import com.knowflow.service.LogService;
import com.knowflow.entity.User;
import com.knowflow.mapper.UserRepository;
import com.knowflow.vo.UserVO;
import com.knowflow.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LogService logService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       LogService logService,
                       KnowledgeBaseRepository knowledgeBaseRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.logService = logService;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
    }

    @Transactional
    public LoginVO register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw BusinessException.badRequest("passwords do not match");
        }
        if (userRepository.existsByUsernameAndDeletedFalse(request.username())) {
            throw BusinessException.badRequest("username already exists");
        }
        if (StringUtils.hasText(request.email()) && userRepository.existsByEmailAndDeletedFalse(request.email())) {
            throw BusinessException.badRequest("email already exists");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setNickname(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ENABLED);
        userRepository.insert(user);
        createDefaultKnowledgeBase(user);
        return toLoginVO(user);
    }

    public LoginVO login(LoginRequest request) {
        User user = findByAccountOptional(request.account()).orElse(null);
        if (user == null) {
            logService.recordLogin(null, request.account(), false, "invalid account or password");
            throw BusinessException.badRequest("invalid account or password");
        }
        if (user.getStatus() != UserStatus.ENABLED) {
            logService.recordLogin(user.getId(), request.account(), false, "account is disabled");
            throw BusinessException.forbidden("account is disabled");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            logService.recordLogin(user.getId(), request.account(), false, "invalid password");
            throw BusinessException.badRequest("invalid account or password");
        }
        logService.recordLogin(user.getId(), request.account(), true, "login success");
        return toLoginVO(user);
    }

    public LoginVO adminLogin(LoginRequest request) {
        User user = findByAccountOptional(request.account()).orElse(null);
        if (user == null) {
            logService.recordLogin(null, request.account(), false, "invalid account or password");
            throw BusinessException.badRequest("invalid account or password");
        }
        if (user.getRole() != UserRole.ADMIN) {
            logService.recordLogin(user.getId(), request.account(), false, "admin account required");
            throw BusinessException.forbidden("admin account required");
        }
        if (user.getStatus() != UserStatus.ENABLED) {
            logService.recordLogin(user.getId(), request.account(), false, "account is disabled");
            throw BusinessException.forbidden("account is disabled");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            logService.recordLogin(user.getId(), request.account(), false, "invalid password");
            throw BusinessException.badRequest("invalid account or password");
        }
        logService.recordLogin(user.getId(), request.account(), true, "admin login success");
        return toLoginVO(user);
    }

    private java.util.Optional<User> findByAccountOptional(String account) {
        return userRepository.findByUsernameAndDeletedFalse(account)
                .or(() -> userRepository.findByEmailAndDeletedFalse(account));
    }

    private LoginVO toLoginVO(User user) {
        String token = jwtService.createToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginVO(token, UserVO.from(user));
    }

    private void createDefaultKnowledgeBase(User user) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setUserId(user.getId());
        kb.setName("Default Knowledge Base");
        kb.setDescription("Default space created during registration");
        kb.setIcon("book");
        kb.setCategory("default");
        knowledgeBaseRepository.insert(kb);
    }
}
