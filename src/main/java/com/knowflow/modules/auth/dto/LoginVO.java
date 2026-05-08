package com.knowflow.modules.auth.dto;

import com.knowflow.modules.user.UserVO;

public record LoginVO(String token, UserVO user) {
}
