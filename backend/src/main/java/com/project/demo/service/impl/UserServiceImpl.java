package com.project.demo.service.impl;

import com.project.demo.dto.*;
import com.project.demo.dto.TokenPairResponse;
import com.project.demo.entity.UserDetailsImpl;
import com.project.demo.entity.User;
import com.project.demo.entity.UserDetail;
import com.project.demo.exception.BusinessException;
import com.project.demo.mapper.UserDetailMapper;
import com.project.demo.mapper.UserMapper;
import com.project.demo.service.EmailService;
import com.project.demo.service.UserService;
import com.project.demo.utils.JwtUtils;
import com.project.demo.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserDetailMapper userDetailMapper;

    @Value("${verify.code-length:6}")
    private int codeLength;

    @Value("${verify.expiration:300}")
    private long codeExpiration;

    @Value("${verify.send-interval:60}")
    private long sendInterval;

    // ========== 发送验证码 ==========

    /**
     * 发送验证码流程:
     * 1. 检查发送间隔 (防刷)
     * 2. 生成验证码
     * 3. 存储到Redis
     * 4. 发送邮件
     */
    @Override
    public void sendVerifyCode(SendCodeRequest request) {
        String email = request.getEmail();
        String type = request.getType();

        if (!redisUtils.canSendCode(email, type, sendInterval)) {
            throw BusinessException.badRequest("发送太频繁，请稍后再试");
        }

        validateEmailForType(email, type);

        String code = generateRandomCode(codeLength);

        System.out.println("验证码: " + code);
        redisUtils.storeVerifyCode(email, code, type, codeExpiration);

        redisUtils.recordSendTime(email, type, sendInterval);

        String subject = getSubjectByType(type);
        emailService.sendVerifyCodeEmail(email, code, subject, null);
    }

    private void validateEmailForType(String email, String type) {
        switch (type) {
            case "register" -> {
                User existUser = userMapper.findByEmail(email);
                if (existUser != null) {
                    throw BusinessException.badRequest("该邮箱已被注册");
                }
            }
            case "login" -> {
                User existUser = userMapper.findByEmail(email);
                if (existUser == null) {
                    throw BusinessException.notFound("该邮箱未注册");
                }
            }
            case "reset-password" -> {
                User existUser = userMapper.findByEmail(email);
                if (existUser == null) {
                    throw BusinessException.notFound("该邮箱未注册");
                }
            }
            default -> throw BusinessException.badRequest("无效的验证码类型");
        }
    }

    private String getSubjectByType(String type) {
        return switch (type) {
            case "register" -> "【Demo】注册验证码";
            case "login" -> "【Demo】登录验证码";
            case "reset-password" -> "【Demo】密码重置验证码";
            default -> "【Demo】验证码";
        };
    }

    private String generateRandomCode(int length) {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    // ========== 注册 ==========

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        String email = request.getEmail();
        String code = request.getCode();

        if (!redisUtils.verifyCode(email, code, "register")) {
            throw BusinessException.badRequest("验证码错误或已过期");
        }

        User existUser = userMapper.findByUsername(request.getUsername());
        if (existUser != null) {
            throw BusinessException.badRequest("用户名已存在");
        }

        User existEmail = userMapper.findByEmail(request.getEmail());
        if (existEmail != null) {
            throw BusinessException.badRequest("邮箱已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(email);
        user.setStatus(1);

        userMapper.insertUser(user);

        userMapper.insertUserRole(user.getId(), 1L);

        userDetailMapper.insert(user.getId(), user.getUsername());

        redisUtils.deleteVerifyCode(email, "register");
    }

    // ========== 登录 ==========

    @Override
    public TokenPairResponse login(LoginRequest request) {
        String username = request.getUsername();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        username,
                        request.getPassword()
                )
        );

        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        if (user.getStatus() != 1) {
            throw BusinessException.forbidden("用户已被禁用");
        }

        UserDetailsImpl userDetails = new UserDetailsImpl(
                user.getId(),
                username,
                user.getRoles()
        );
        // Redis TTL = 24h 闲置窗口（30天绝对过期由 JWT exp 保证）
        redisUtils.storeUserDetails(user.getId(), userDetails, JwtUtils.REFRESH_IDLE_SECONDS);

        String token = jwtUtils.generateToken(
                username,
                user.getId()
        );

        String refreshToken = jwtUtils.generateRefreshToken(username, user.getId());
        redisUtils.storeRefreshToken(user.getId(), refreshToken, JwtUtils.REFRESH_IDLE_SECONDS);

        return new TokenPairResponse(token, refreshToken);
    }

    @Override
    public TokenPairResponse loginByEmail(LoginByEmailRequest request) {
        String email = request.getEmail();
        String code = request.getCode();

        if (!redisUtils.verifyCode(email, code, "login")) {
            throw BusinessException.badRequest("验证码错误或已过期");
        }

        User user = userMapper.findByEmail(email);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        if (user.getStatus() != 1) {
            throw BusinessException.forbidden("用户已被禁用");
        }

        redisUtils.deleteVerifyCode(email, "login");

        UserDetailsImpl userDetails = new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getRoles()
        );
        // Redis TTL = 24h 闲置窗口（30天绝对过期由 JWT exp 保证）
        redisUtils.storeUserDetails(user.getId(), userDetails, JwtUtils.REFRESH_IDLE_SECONDS);

        String token = jwtUtils.generateToken(
                user.getUsername(),
                user.getId()
        );

        String refreshToken = jwtUtils.generateRefreshToken(user.getUsername(), user.getId());
        redisUtils.storeRefreshToken(user.getId(), refreshToken, JwtUtils.REFRESH_IDLE_SECONDS);

        return new TokenPairResponse(token, refreshToken);
    }

    @Override
    public void logout(String token) {
        Long userId = jwtUtils.getUserIdFromToken(token);

        if (userId != null) {
            redisUtils.deleteUserDetails(userId);
            redisUtils.deleteRefreshToken(userId);
        }
    }

    // ========== 忘记密码 ==========

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail();
        String code = request.getCode();

        if (!redisUtils.verifyCode(email, code, "reset-password")) {
            throw BusinessException.badRequest("验证码错误或已过期");
        }

        User user = userMapper.findByEmail(email);
        if (user == null) {
            throw BusinessException.notFound("该邮箱未注册");
        }

        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        userMapper.updatePassword(user.getId(), encodedPassword);

        redisUtils.deleteVerifyCode(email, "reset-password");
    }

    @Override
    public User getCurrentUser(String username) {
        return userMapper.findByUsername(username);
    }

    // ========== 搜索用户 ==========

    @Override
    public PageResponse<UserSearchResponse> searchUsers(String keyword, PageRequest pageRequest, Long currentUserId) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return PageResponse.of(List.of(), 0L, pageRequest.getPage(), pageRequest.getSize());
        }

        int offset = pageRequest.getOffset();
        List<Map<String, Object>> list = userDetailMapper.searchUsersWithFriendStatus(keyword.trim(), currentUserId, offset, pageRequest.getSize());
        long total = userDetailMapper.countSearchUsersWithFriendStatus(keyword.trim(), currentUserId);

        List<UserSearchResponse> result = list.stream().map(map -> {
            UserSearchResponse r = new UserSearchResponse();
            r.setUserId(((Number) map.get("userId")).longValue());
            r.setUsername((String) map.get("username"));
            r.setAvatar((String) map.get("avatar"));
            r.setAge(map.get("age") != null ? ((Number) map.get("age")).intValue() : null);
            r.setBio((String) map.get("bio"));
            r.setFriendStatus((String) map.get("friendStatus"));
            return r;
        }).toList();

        return PageResponse.of(result, total, pageRequest.getPage(), pageRequest.getSize());
    }

    // ========== 更新个人资料 ==========

    @Override
    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        // 如果要改用户名，检查唯一性
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            UserDetail existing = userDetailMapper.selectByUserId(userId);
            if (!request.getUsername().equals(existing.getUsername())) {
                UserDetail other = userDetailMapper.selectByUsername(request.getUsername());
                if (other != null && !other.getUserId().equals(userId)) {
                    throw BusinessException.badRequest("用户名已被占用");
                }
            }
        }

        UserDetail detail = new UserDetail();
        detail.setUserId(userId);
        detail.setUsername(request.getUsername());
        detail.setAvatar(request.getAvatar());
        detail.setAge(request.getAge());
        detail.setBio(request.getBio());
        userDetailMapper.updateByUserId(detail);
    }

    // ========== 获取自己的资料 ==========

    @Override
    public UserSearchResponse getMyProfile(Long userId) {
        UserDetail detail = userDetailMapper.selectByUserId(userId);
        if (detail == null) {
            throw BusinessException.notFound("用户不存在");
        }

        UserSearchResponse r = new UserSearchResponse();
        r.setUserId(detail.getUserId());
        r.setUsername(detail.getUsername());
        r.setAvatar(detail.getAvatar());
        r.setAge(detail.getAge());
        r.setBio(detail.getBio());
        return r;
    }
}