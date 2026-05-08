package com.knowflow.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.entity.User;
import com.knowflow.enums.UserStatus;
import java.util.Optional;



public interface UserRepository extends BaseMapper<User> {
    default long count() {
        return selectCount(new LambdaQueryWrapper<>());
    }

    default Optional<User> findByUsernameAndDeletedFalse(String username) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username).last("limit 1")));
    }

    default Optional<User> findByEmailAndDeletedFalse(String email) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email).last("limit 1")));
    }

    default Optional<User> findByIdAndDeletedFalse(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    default boolean existsByUsernameAndDeletedFalse(String username) {
        return selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0;
    }

    default boolean existsByEmailAndDeletedFalse(String email) {
        return selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0;
    }

    default Page<User> findByDeletedFalseAndUsernameContainingAndStatus(String username, UserStatus status, Page<User> page) {
        return selectPage(page, new LambdaQueryWrapper<User>()
                .like(username != null && !username.isBlank(), User::getUsername, username)
                .eq(User::getStatus, status));
    }

    default Page<User> findByDeletedFalseAndUsernameContaining(String username, Page<User> page) {
        return selectPage(page, new LambdaQueryWrapper<User>()
                .like(username != null && !username.isBlank(), User::getUsername, username));
    }
}
