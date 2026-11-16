package in.co.bel.acempp.service;
import in.co.bel.acempp.dto.auth.*;
import in.co.bel.acempp.dto.general.DeptsDto;
import in.co.bel.acempp.dto.general.RolesDto;
import in.co.bel.acempp.dto.general.SubDeptsDto;
import in.co.bel.acempp.dto.user.UserInfoDto;
import in.co.bel.acempp.entity.*;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.*;
import in.co.bel.acempp.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final DepartmentRepository departmentRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CaptchaService captchaService;
    private final JwtService jwtService;                // unchanged – uses RefreshTokenRepository

    @Value("${jwt.access_token_expiration}")
    private long accessTokenExpiration;                 // ms
    @Value("${jwt.refresh_token_expiration}")
    private long refreshTokenExpiration;

    // ─────────────────────────── LOGIN ───────────────────────────
    public LoginResponse login(LoginRequest request) {
        // 1. Validate captcha
        validateCaptcha(request.getCaptcha());

        // 2. Find user by business userId
        User user = userRepo.findByUserId(request.getUserId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.INVALID_CREDENTIALS, "Invalid userId or password"));

        // 3. Check active
        if (user.getStatus() != Status.ACTIVE) {
            throw new AcemException(ErrorCode.INVALID_CREDENTIALS,
                    "Invalid userId or password");
        }

        // 4. Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AcemException(ErrorCode.INVALID_CREDENTIALS,
                    "Invalid userId or password");
        }

        // 5. Validate role (user has exactly one role)
        if (!user.getRoleId().equals(request.getRole().getRoleId())) {
            throw new AcemException(ErrorCode.ROLE_ACCESS_DENIED,
                    "User does not have access to the selected role");
        }

        // 6. Fetch role name for token
        String roleName = roleRepo.findByRoleId(user.getRoleId())
                .map(RoleMaster::getRoleName)
                .orElseThrow(() -> new AcemException(ErrorCode.ROLE_ACCESS_DENIED,
                        "Role not found"));

        // 7. Build department/sub-department list filtered by user's allowed subDeptIds
        List<DeptsDto> groupedDepts = buildDepartmentsForUser(user.getSubDepartmentIds());

        // 8. Generate tokens
        String accessToken = jwtUtil.generateAccessToken(
                user.getUsername(), user.getUserId(), roleName);
        String refreshToken = jwtUtil.generateRefreshToken(
                user.getUsername(), user.getUserId());

        jwtService.storeRefreshToken(
                user.getUsername(),
                refreshToken,
                new Date(System.currentTimeMillis() + refreshTokenExpiration));

        // 9. Build response
        UserInfoDto userInfo = UserInfoDto.builder()
                .userId(user.getUserId())
                .user_uuid(user.getUserUUID())
                .username(user.getUsername())
                .role(new RolesDto(user.getRoleId(), roleName))
                .departments(groupedDepts)
                .mustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()))
                .build();

        TokenDto tokenDto = TokenDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

        return LoginResponse.builder()
                .user(userInfo)
                .token(tokenDto)
                .build();
    }

    // ─────────────────────────── LOGOUT ───────────────────────────
    public LogoutResponse logout(LogoutRequest request, String username) {
        boolean valid = jwtService.validateRefreshToken(request.getRefreshToken());
        if (!valid) {
            throw new AcemException(ErrorCode.UNAUTHORIZED,
                    "The provided refresh token is expired or invalid");
        }

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new AcemException(ErrorCode.USER_NOT_FOUND,
                        "User not found"));

        jwtService.invalidateRefreshToken(request.getRefreshToken());

        log.info("User {} logged out successfully", username);
        return LogoutResponse.builder()
                .userId(user.getUserId())
                .logoutTime(LocalDateTime.now())
                .build();
    }

    // ─────────────────────── REFRESH TOKEN ───────────────────────
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String incomingRefreshToken = request.getRefreshToken();

        // 1. Validate token (DB + expiry + signature)
        boolean valid = jwtService.validateRefreshToken(incomingRefreshToken);
        if (!valid) {
            throw new AcemException(ErrorCode.UNAUTHORIZED,
                    "Refresh token is invalid or tampered");
        }

        // 2. Extract username
        String username;
        try {
            username = jwtService.extractUserName(incomingRefreshToken);
        } catch (Exception e) {
            throw new AcemException(ErrorCode.UNAUTHORIZED,
                    "Refresh token is invalid or tampered");
        }

        // 3. Fetch user
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new AcemException(ErrorCode.UNAUTHORIZED,
                        "User not found"));
        if (user.getStatus() != Status.ACTIVE) {
            throw new AcemException(ErrorCode.INVALID_CREDENTIALS,
                    "Invalid userId or password");
        }

        // 4. Get role name
        String roleName = roleRepo.findByRoleId(user.getRoleId())
                .map(RoleMaster::getRoleName)
                .orElseThrow(() -> new AcemException(ErrorCode.UNAUTHORIZED,
                        "Role not found"));

        // 5. Rotate tokens
        jwtService.invalidateRefreshToken(incomingRefreshToken);
        String newAccessToken = jwtUtil.generateAccessToken(
                user.getUsername(), user.getUserId(), roleName);
        String newRefreshToken = jwtUtil.generateRefreshToken(
                user.getUsername(), user.getUserId());
        jwtService.storeRefreshToken(
                user.getUsername(),
                newRefreshToken,
                new Date(System.currentTimeMillis() + refreshTokenExpiration));

        log.info("Tokens refreshed for user: {}", username);
        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    // ─────────────────── PRIVATE HELPERS ───────────────────
    private void validateCaptcha(CaptchaDto captcha) {
        if (captcha == null || captcha.getCaptchaId() == null || captcha.getCaptchaValue() == null) {
            throw new AcemException(ErrorCode.INVALID_CAPTCHA, "Invalid captcha");
        }
        boolean isValid = captchaService.validate(captcha.getCaptchaId(), captcha.getCaptchaValue());
        if (!isValid) {
            throw new AcemException(
                    ErrorCode.INVALID_CAPTCHA,
                    "Invalid, expired, or already used captcha");
        }
    }

    /**
     * Builds the list of DeptsDto containing only those
     * sub-departments whose IDs appear in the user's subDepartmentIds.
     */
    private List<DeptsDto> buildDepartmentsForUser(List<Long> allowedSubDeptIds) {
        if (allowedSubDeptIds == null || allowedSubDeptIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<DepartmentMaster> departments = departmentRepo.findAll(); // all departments with embedded subDepts
        Map<Long, DeptsDto> map = new LinkedHashMap<>();

        for (DepartmentMaster dept : departments) {
            if (dept.getSubDepartments() == null) continue;

            List<SubDeptsDto> filtered = dept.getSubDepartments().stream()
                    .filter(sd -> allowedSubDeptIds.contains(sd.getSubDepartmentId()))
                    .map(sd -> SubDeptsDto.builder()
                            .subDepartmentId(sd.getSubDepartmentId())
                            .subDepartmentName(sd.getSubDepartmentName())
                            .build())
                    .collect(Collectors.toList());

            if (!filtered.isEmpty()) {
                map.put(dept.getDepartmentId(), DeptsDto.builder()
                        .departmentId(dept.getDepartmentId())
                        .departmentName(dept.getDepartmentName())
                        .subDepartments(filtered)
                        .build());
            }
        }
        return new ArrayList<>(map.values());
    }
}