package com.knowflow.modules.auth;

import com.knowflow.common.BusinessException;
import com.knowflow.common.enums.UserRole;
import com.knowflow.common.enums.UserStatus;
import com.knowflow.modules.auth.dto.LoginRequest;
import com.knowflow.modules.auth.dto.LoginVO;
import com.knowflow.modules.auth.dto.RegisterRequest;
import com.knowflow.modules.user.User;
import com.knowflow.modules.user.UserRepository;
import com.knowflow.modules.user.UserVO;
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

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginVO register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw BusinessException.badRequest("两次密码不一致");
        }
        if (userRepository.existsByUsernameAndDeletedFalse(request.username())) {
            throw BusinessException.badRequest("用户名已存在");
        }
        if (StringUtils.hasText(request.email()) && userRepository.existsByEmailAndDeletedFalse(request.email())) {
            throw BusinessException.badRequest("邮箱已存在");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setNickname(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ENABLED);
        userRepository.save(user);
        return toLoginVO(user);
    }

    public LoginVO login(LoginRequest request) {
        User user = findByAccount(request.account());
        if (user.getStatus() != UserStatus.ENABLED) {
            throw BusinessException.forbidden("账号已禁用");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw BusinessException.badRequest("账号或密码错误");
        }
        return toLoginVO(user);
    }

    public LoginVO adminLogin(LoginRequest request) {
        User user = findByAccount(request.account());
        if (user.getRole() != UserRole.ADMIN) {
            throw BusinessException.forbidden("需要管理员账号");
        }
        if (user.getStatus() != UserStatus.ENABLED) {
            throw BusinessException.forbidden("账号已禁用");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw BusinessException.badRequest("账号或密码错误");
        }
        return toLoginVO(user);
    }

    private User findByAccount(String account) {
        return userRepository.findByUsernameAndDeletedFalse(account)
                .or(() -> userRepository.findByEmailAndDeletedFalse(account))
                .orElseThrow(() -> BusinessException.badRequest("账号或密码错误"));
    }

    private LoginVO toLoginVO(User user) {
        String token = jwtService.createToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginVO(token, UserVO.from(user));
    }
}
