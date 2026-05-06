package com.youhuifuwu.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youhuifuwu.auth.dto.PasswordLoginRequest;
import com.youhuifuwu.auth.dto.UserRegisterRequest;
import com.youhuifuwu.auth.dto.WechatLoginRequest;
import com.youhuifuwu.auth.entity.SysAccount;
import com.youhuifuwu.auth.mapper.SysAccountMapper;
import com.youhuifuwu.auth.service.AuthService;
import com.youhuifuwu.auth.vo.LoginResponse;
import com.youhuifuwu.common.constant.LoginTypeConstants;
import com.youhuifuwu.common.constant.RoleConstants;
import com.youhuifuwu.common.exception.BusinessException;
import com.youhuifuwu.security.JwtTokenProvider;
import com.youhuifuwu.user.entity.UserProfile;
import com.youhuifuwu.user.mapper.UserProfileMapper;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthServiceImpl implements AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final SysAccountMapper sysAccountMapper;
    private final UserProfileMapper userProfileMapper;

    public AuthServiceImpl(JwtTokenProvider jwtTokenProvider,
                           SysAccountMapper sysAccountMapper,
                           UserProfileMapper userProfileMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.sysAccountMapper = sysAccountMapper;
        this.userProfileMapper = userProfileMapper;
    }

    @Override
    public LoginResponse passwordLogin(PasswordLoginRequest request) {
        String identity = trimToNull(request.getUsername());
        SysAccount account = sysAccountMapper.selectOne(
                new LambdaQueryWrapper<SysAccount>().eq(SysAccount::getUsername, identity)
        );
        UserProfile profile = null;
        if (account == null) {
            profile = userProfileMapper.selectOne(
                    new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserNo, identity)
            );
            if (profile == null) {
                throw new BusinessException(404, "Account not found");
            }
            account = sysAccountMapper.selectById(profile.getAccountId());
        } else if (RoleConstants.USER.equals(account.getRole())) {
            profile = findProfileByAccountId(account.getId());
        }
        if (account == null) {
            throw new BusinessException(404, "Account not found");
        }
        if (!StringUtils.hasText(account.getPasswordHash())) {
            throw new BusinessException(400, "Please complete password setup first");
        }
        if (!Objects.equals(request.getPassword(), account.getPasswordHash())) {
            throw new BusinessException(400, "Username or password is incorrect");
        }
        if (account.getStatus() == null || account.getStatus() != 1) {
            throw new BusinessException(403, "Account has been disabled");
        }
        account.setLastLoginTime(LocalDateTime.now());
        sysAccountMapper.updateById(account);
        return buildLoginResponse(account, profile, identity);
    }

    @Override
    @Transactional
    public LoginResponse wechatLogin(WechatLoginRequest request) {
        SysAccount account = sysAccountMapper.selectOne(
                new LambdaQueryWrapper<SysAccount>().eq(SysAccount::getWxOpenid, request.getMockOpenid())
        );
        UserProfile profile;
        if (account == null) {
            account = new SysAccount();
            account.setRole(RoleConstants.USER);
            account.setLoginType(LoginTypeConstants.WECHAT);
            account.setWxOpenid(request.getMockOpenid());
            account.setStatus(1);
            sysAccountMapper.insert(account);

            profile = new UserProfile();
            profile.setAccountId(account.getId());
            profile.setRealName(trimToNull(request.getNickname()));
            profile.setUserType("STUDENT");
            userProfileMapper.insert(profile);
        } else {
            profile = findOrCreateProfile(account.getId());
            if (!StringUtils.hasText(profile.getRealName()) && StringUtils.hasText(request.getNickname())) {
                profile.setRealName(request.getNickname().trim());
                userProfileMapper.updateById(profile);
            }
        }
        if (account.getStatus() == null || account.getStatus() != 1) {
            throw new BusinessException(403, "Account has been disabled");
        }
        account.setLastLoginTime(LocalDateTime.now());
        sysAccountMapper.updateById(account);
        return buildLoginResponse(account, profile, request.getNickname());
    }

    @Override
    @Transactional
    public LoginResponse register(UserRegisterRequest request) {
        ensureUserNoUnique(request.getUserNo(), null);

        SysAccount account = new SysAccount();
        account.setRole(RoleConstants.USER);
        account.setLoginType(LoginTypeConstants.PASSWORD);
        account.setPasswordHash(request.getPassword().trim());
        account.setStatus(1);
        account.setLastLoginTime(LocalDateTime.now());
        sysAccountMapper.insert(account);

        UserProfile profile = new UserProfile();
        profile.setAccountId(account.getId());
        profile.setRealName(trimToNull(request.getRealName()));
        profile.setUserNo(trimToNull(request.getUserNo()));
        profile.setUserType(trimToNull(request.getUserType()));
        profile.setPhone(trimToNull(request.getPhone()));
        profile.setAvatarUrl(trimToNull(request.getAvatarUrl()));
        userProfileMapper.insert(profile);

        return buildLoginResponse(account, profile, profile.getRealName());
    }

    private LoginResponse buildLoginResponse(SysAccount account, UserProfile profile, String fallbackName) {
        String displayName = resolveDisplayName(account, profile, fallbackName);
        String token = jwtTokenProvider.createToken(account.getId(), account.getRole(), displayName);
        boolean passwordConfigured = !RoleConstants.USER.equals(account.getRole())
                || StringUtils.hasText(account.getPasswordHash());
        boolean profileCompleted = !RoleConstants.USER.equals(account.getRole()) || isProfileCompleted(profile);
        return LoginResponse.builder()
                .accountId(account.getId())
                .role(account.getRole())
                .displayName(displayName)
                .token(token)
                .profileCompleted(profileCompleted)
                .passwordConfigured(passwordConfigured)
                .needProfileCompletion(RoleConstants.USER.equals(account.getRole()) && (!profileCompleted || !passwordConfigured))
                .build();
    }

    private String resolveDisplayName(SysAccount account, UserProfile profile, String fallbackName) {
        if (profile != null && StringUtils.hasText(profile.getRealName())) {
            return profile.getRealName();
        }
        if (StringUtils.hasText(account.getUsername())) {
            return account.getUsername();
        }
        if (StringUtils.hasText(fallbackName)) {
            return fallbackName.trim();
        }
        return "用户";
    }

    private boolean isProfileCompleted(UserProfile profile) {
        return profile != null
                && StringUtils.hasText(profile.getRealName())
                && StringUtils.hasText(profile.getUserNo())
                && StringUtils.hasText(profile.getUserType())
                && StringUtils.hasText(profile.getPhone());
    }

    private void ensureUserNoUnique(String userNo, Long currentProfileId) {
        String normalizedUserNo = trimToNull(userNo);
        if (!StringUtils.hasText(normalizedUserNo)) {
            throw new BusinessException(400, "User number is required");
        }
        UserProfile existing = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserNo, normalizedUserNo)
        );
        if (existing != null && !Objects.equals(existing.getId(), currentProfileId)) {
            throw new BusinessException(400, "User number already exists");
        }
    }

    private UserProfile findProfileByAccountId(Long accountId) {
        return userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getAccountId, accountId)
        );
    }

    private UserProfile findOrCreateProfile(Long accountId) {
        UserProfile profile = findProfileByAccountId(accountId);
        if (profile != null) {
            return profile;
        }
        profile = new UserProfile();
        profile.setAccountId(accountId);
        profile.setUserType("STUDENT");
        userProfileMapper.insert(profile);
        return profile;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
