package com.youhuifuwu.auth.service;

import com.youhuifuwu.auth.dto.PasswordLoginRequest;
import com.youhuifuwu.auth.dto.UserRegisterRequest;
import com.youhuifuwu.auth.dto.WechatLoginRequest;
import com.youhuifuwu.auth.vo.LoginResponse;

public interface AuthService {

    LoginResponse passwordLogin(PasswordLoginRequest request);

    LoginResponse wechatLogin(WechatLoginRequest request);

    LoginResponse register(UserRegisterRequest request);
}
