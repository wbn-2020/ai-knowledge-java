package com.knowflow.modules.user;

import com.knowflow.common.BusinessException;
import com.knowflow.modules.user.dto.ChangePasswordRequest;
import com.knowflow.modules.user.dto.UpdateProfileRequest;
import com.knowflow.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserVO current() {
        User user = getCurrentUserEntity();
        return UserVO.from(user);
    }

    @Transactional
    public UserVO updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUserEntity();
        if (StringUtils.hasText(request.email()) && !request.email().equals(user.getEmail())
                && userRepository.existsByEmailAndDeletedFalse(request.email())) {
            throw BusinessException.badRequest("邮箱已存在");
        }
        user.setNickname(request.nickname());
        user.setEmail(request.email());
        user.setAvatar(request.avatar());
        user.setBio(request.bio());
        return UserVO.from(userRepository.save(user));
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw BusinessException.badRequest("两次密码不一致");
        }
        User user = getCurrentUserEntity();
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw BusinessException.badRequest("旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private User getCurrentUserEntity() {
        return userRepository.findByIdAndDeletedFalse(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> BusinessException.unauthorized("用户不存在"));
    }
}
