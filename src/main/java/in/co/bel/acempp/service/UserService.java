package in.co.bel.acempp.service;

import in.co.bel.acempp.dto.general.DeptsDto;
import in.co.bel.acempp.dto.general.PaginationDto;
import in.co.bel.acempp.dto.general.RolesDto;
import in.co.bel.acempp.dto.general.SubDeptsDto;
import in.co.bel.acempp.dto.user.*;
import in.co.bel.acempp.entity.DepartmentMaster;
import in.co.bel.acempp.entity.RoleMaster;
import in.co.bel.acempp.entity.SubDepartmentMaster;
import in.co.bel.acempp.entity.User;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.ResetRequestStatus;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.DepartmentRepository;
import in.co.bel.acempp.repository.PasswordResetRequestRepository;
import in.co.bel.acempp.repository.RoleRepository;
import in.co.bel.acempp.repository.UserRepository;
import in.co.bel.acempp.util.DateTimeConversionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.FacetOperation;
import org.bson.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordResetRequestRepository passwordResetRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;
    // ─────────────────────────────────────────
    // CREATE USER
    // ─────────────────────────────────────────
    public CreateUserResponse createUser(
            CreateUserRequest request,
            String createdBy) {


        // 1. CHECK DUPLICATE USERNAME
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AcemException(ErrorCode.DUPLICATE_USER, "Username already exists: " + request.getUsername());
        }


        // 2. CHECK DUPLICATE USER ID
        if (userRepository.existsByUserId(request.getUserId())) {
            throw new AcemException(
                    ErrorCode.DUPLICATE_USER,
                    "UserId already exists: "
                            + request.getUserId());
        }

        // 3. VALIDATE ROLE
        RoleMaster roleMaster = roleRepository.findByRoleId(request.getRoleId())
                        .orElseThrow(() ->
                                new AcemException(
                                        ErrorCode.ROLES_NOT_FOUND,
                                        "Role not found with id: "
                                                + request.getRoleId()));

        // 4. FETCH DEPARTMENT MASTER DATA
        List<DepartmentMaster> departmentMasters = departmentRepository.findAll();


        // 5. VALIDATE SUB DEPARTMENTS
        // ONLY IF PROVIDED
        if (request.getSubDepartmentIds() != null && !request.getSubDepartmentIds().isEmpty()) {
            validateSubDepartments(
                    request.getSubDepartmentIds(),
                    departmentMasters);
        }
        // 6. GENERATE UUID + DEFAULT PASSWORD
        String generatedUUID = UUID.randomUUID().toString();
        String defaultPassword = in.co.bel.acempp.util.PasswordPolicy.generateDefaultPassword(request.getUserId());

        // 7. CREATE USER
        User user = new User();
        user.setUserUUID(generatedUUID);
        user.setUserId(request.getUserId());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(defaultPassword));
        user.setMustChangePassword(true);
        user.setTemporaryPassword(defaultPassword);
        user.setPasswordHistory(new ArrayList<>());
        user.setRoleId(request.getRoleId());

        // EMPTY LIST = GLOBAL ACCESS
        user.setSubDepartmentIds(request.getSubDepartmentIds() != null ? request.getSubDepartmentIds()
                        : new ArrayList<>());
        user.setStatus(Status.ACTIVE);
        user.setCreatedBy(createdBy);
        user.setUpdatedBy(createdBy);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // 8. SAVE USER
        User savedUser = userRepository.save(user);
        return mapToCreateUserResponse(savedUser, roleMaster, departmentMasters);
    }

    // ─────────────────────────────────────────
    // VALIDATE SUB DEPARTMENTS
    // ─────────────────────────────────────────
    private void validateSubDepartments(
            List<Long> subDepartmentIds,
            List<DepartmentMaster> departmentMasters) {

        Set<Long> availableSubDeptIds =
                departmentMasters.stream()
                        .flatMap(dept ->
                                dept.getSubDepartments()
                                        .stream())
                        .map(SubDepartmentMaster::getSubDepartmentId)
                        .collect(Collectors.toSet());

        List<Long> invalidSubDeptIds =
                subDepartmentIds.stream()
                        .filter(id ->
                                !availableSubDeptIds.contains(id))
                        .toList();

        if (!invalidSubDeptIds.isEmpty()) {

            throw new AcemException(
                    ErrorCode.SUB_DEPARTMENTS_NOT_FOUND,
                    "Invalid Sub Department IDs",
                    invalidSubDeptIds);
        }
    }

    // ─────────────────────────────────────────
    // MAP RESPONSE
    // ─────────────────────────────────────────
    private CreateUserResponse mapToCreateUserResponse(
            User user,
            RoleMaster roleMaster,
            List<DepartmentMaster> departmentMasters) {
        Map<Long, DeptsDto>
                departmentMap = new HashMap<>();
        for (Long subDeptId :
                user.getSubDepartmentIds()) {
            for (DepartmentMaster deptMaster :
                    departmentMasters) {
                Optional<SubDepartmentMaster>
                        subDeptOptional =
                        deptMaster.getSubDepartments()
                                .stream()
                                .filter(sd ->
                                        sd.getSubDepartmentId()
                                                .equals(subDeptId))
                                .findFirst();

                if (subDeptOptional.isPresent()) {

                    DeptsDto department =
                            departmentMap.computeIfAbsent(
                                    deptMaster.getDepartmentId(),
                                    key ->
                                            DeptsDto
                                                    .builder()
                                                    .departmentId(
                                                            deptMaster.getDepartmentId())
                                                    .departmentName(
                                                            deptMaster.getDepartmentName())
                                                    .subDepartments(
                                                            new ArrayList<>())
                                                    .build());

                    department.getSubDepartments()
                            .add(
                                    SubDeptsDto
                                            .builder()
                                            .subDepartmentId(
                                                    subDeptOptional.get()
                                                            .getSubDepartmentId())
                                            .subDepartmentName(
                                                    subDeptOptional.get()
                                                            .getSubDepartmentName())
                                            .build()
                            );
                }
            }
        }

        return CreateUserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .user_uuid(user.getUserUUID())
//                .defaultPassword(defaultPassword)
                .role(
                        RolesDto
                                .builder()
                                .roleId(
                                        roleMaster.getRoleId())
                                .roleName(
                                        roleMaster.getRoleName())
                                .build()
                )
                .departments(
                        departmentMap.values()
                                .stream()
                                .toList()
                )
                .status(
                        user.getStatus().name())
                .createdAt(
                        user.getCreatedAt())
                .build();
    }

    // ─────────────────────────────────────────
    // UPDATE USER
    // ─────────────────────────────────────────
    public UpdateUserResponse updateUser(
            UpdateUserRequest request,
            String updatedBy) {

        // 1. Find user
        User user = userRepository
                .findByUserUUID(
                        request.getUser_uuid())
                .orElseThrow(() ->
                        new AcemException(
                                ErrorCode.USER_NOT_FOUND,
                                "The requested user does not exist"));

        // 2. Update username
        if (request.getUsername() != null
                && !request.getUsername().isBlank()) {

            userRepository.findByUsername(
                            request.getUsername())
                    .ifPresent(existing -> {

                        if (!existing.getUserUUID()
                                .equals(request.getUser_uuid())) {

                            throw new AcemException(
                                    ErrorCode.DUPLICATE_USER,
                                    "Username already taken");
                        }
                    });

            user.setUsername(
                    request.getUsername());
        }

        // 3. Update sub departments
        if (request.getSubDepartmentIds() != null
                && !request.getSubDepartmentIds().isEmpty()) {

            // Validate sub departments
            List<DepartmentMaster> departmentMasters =
                    departmentRepository.findAll();

            validateSubDepartments(
                    request.getSubDepartmentIds(),
                    departmentMasters);

            user.setSubDepartmentIds(
                    request.getSubDepartmentIds());
        }

        // 4. Audit fields
        user.setUpdatedBy(updatedBy);

        user.setUpdatedAt(LocalDateTime.now());

        // 5. Save
        User savedUser =
                userRepository.save(user);

        // 6. Fetch role
        RoleMaster roleMaster =
                roleRepository.findByRoleId(
                                savedUser.getRoleId())
                        .orElseThrow(() ->
                                new AcemException(
                                        ErrorCode.ROLES_NOT_FOUND,
                                        "Role not found"));

        // 7. Build response
        return buildUpdateUserResponse(
                savedUser,
                roleMaster);
    }

    private UpdateUserResponse buildUpdateUserResponse(
            User user,
            RoleMaster roleMaster) {

        List<DepartmentMaster> departmentMasters =
                departmentRepository.findAll();

        Map<Long,
                DeptsDto>
                departmentMap = new HashMap<>();

        for (Long subDeptId :
                user.getSubDepartmentIds()) {

            for (DepartmentMaster deptMaster : departmentMasters) {

                Optional<SubDepartmentMaster> subDeptOptional = deptMaster.getSubDepartments()
                                .stream()
                                .filter(sd ->
                                        sd.getSubDepartmentId()
                                                .equals(subDeptId))
                                .findFirst();

                if (subDeptOptional.isPresent()) {

                    DeptsDto department = departmentMap.computeIfAbsent(
                                    deptMaster.getDepartmentId(),
                                    key ->
                                            DeptsDto
                                                    .builder()
                                                    .departmentId(deptMaster.getDepartmentId())
                                                    .departmentName(
                                                            deptMaster.getDepartmentName())
                                                    .subDepartments(
                                                            new ArrayList<>())
                                                    .build());

                    department.getSubDepartments()
                            .add(SubDeptsDto
                                        .builder()
                                        .subDepartmentId(
                                                subDeptOptional.get()
                                                        .getSubDepartmentId())
                                        .subDepartmentName(
                                                subDeptOptional.get()
                                                        .getSubDepartmentName())
                                        .build()
                            );
                }
            }
        }

        return UpdateUserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .role(
                        RolesDto
                                .builder()
                                .roleId(roleMaster.getRoleId())
                                .roleName(roleMaster.getRoleName())
                                .build()
                )
                .departments(
                        departmentMap.values()
                                .stream()
                                .toList()
                )
                .status(user.getStatus().name())
                .build();
    }




    public DeleteUserResponse deleteUser(UserUuidRequest request) {

        // 1. Find user
        User user = userRepository
                .findByUserUUID(request.getUser_uuid())
                .orElseThrow(() ->
                        new AcemException(
                                ErrorCode.USER_NOT_FOUND,
                                "The requested user does not exist"));

        // 2. Soft‑delete: set status to INACTIVE
        if (user.getStatus() == Status.ACTIVE) {
            user.setStatus(Status.INACTIVE);
            userRepository.save(user);
        }

        return DeleteUserResponse.builder()
                .userId(user.getUserId())
                .status(user.getStatus().name())   // returns "INACTIVE" (or existing if already inactive)
                .build();
    }

    /** Soft-reactivate a previously deactivated user. */
    public ActivateUserResponse activateUser(UserUuidRequest request) {
        User user = userRepository
                .findByUserUUID(request.getUser_uuid())
                .orElseThrow(() ->
                        new AcemException(
                                ErrorCode.USER_NOT_FOUND,
                                "The requested user does not exist"));

        if (user.getStatus() != Status.ACTIVE) {
            user.setStatus(Status.ACTIVE);
            userRepository.save(user);
        }

        return ActivateUserResponse.builder()
                .userId(user.getUserId())
                .status(user.getStatus().name())
                .build();
    }

    // ─────────────────────────────────────────
    // USER DETAILS
    // ─────────────────────────────────────────
    public UserDetailsResponse getUserDetails(
            UserUuidRequest request) {

        // 1. Find User
        User user = userRepository
                .findByUserUUID(
                        request.getUser_uuid())
                .orElseThrow(() ->
                        new AcemException(
                                ErrorCode.USER_NOT_FOUND,
                                "The requested user does not exist"));

        // 2. Fetch role
        RoleMaster roleMaster =
                roleRepository.findByRoleId(
                                user.getRoleId())
                        .orElseThrow(() ->
                                new AcemException(
                                        ErrorCode.ROLES_NOT_FOUND,
                                        "Role not found"));

        // 3. Fetch departments
        List<DepartmentMaster> departmentMasters =
                departmentRepository.findAll();

        // 4. Build grouped departments
        Map<Long,
                DeptsDto>
                departmentMap = new HashMap<>();

        for (Long subDeptId :
                user.getSubDepartmentIds()) {

            for (DepartmentMaster deptMaster :
                    departmentMasters) {

                Optional<SubDepartmentMaster>
                        subDeptOptional =
                        deptMaster.getSubDepartments()
                                .stream()
                                .filter(sd ->
                                        sd.getSubDepartmentId()
                                                .equals(subDeptId))
                                .findFirst();

                if (subDeptOptional.isPresent()) {

                    DeptsDto
                            department =
                            departmentMap.computeIfAbsent(
                                    deptMaster.getDepartmentId(),
                                    key ->
                                            DeptsDto
                                                    .builder()
                                                    .departmentId(
                                                            deptMaster.getDepartmentId())
                                                    .departmentName(
                                                            deptMaster.getDepartmentName())
                                                    .subDepartments(
                                                            new ArrayList<>())
                                                    .build());

                    department.getSubDepartments()
                            .add(
                                    SubDeptsDto
                                            .builder()
                                            .subDepartmentId(
                                                    subDeptOptional.get()
                                                            .getSubDepartmentId())
                                            .subDepartmentName(
                                                    subDeptOptional.get()
                                                            .getSubDepartmentName())
                                            .build()
                            );
                }
            }
        }

        // 5. Created By
        UserDetailsResponse.CreatedBy createdBy =
                null;

        if (user.getCreatedBy() != null) {
            createdBy =
                    UserDetailsResponse.CreatedBy
                            .builder()
                            .username(
                                    user.getCreatedBy())
                            .role(
                                    RolesDto
                                            .builder()
                                            .roleId(1L)
                                            .roleName("Admin")
                                            .build()
                            )
                            .build();
        }

        // 6. Build response
        return UserDetailsResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .role(
                        RolesDto
                                .builder()
                                .roleId(roleMaster.getRoleId())
                                .roleName(roleMaster.getRoleName())
                                .build()
                )
                .departments(
                        departmentMap.values()
                                .stream()
                                .toList()
                )
                .status(user.getStatus().name())
                .createdOn(user.getCreatedAt())
                .createdBy(createdBy)
                .build();
    }

    // ─────────────────────────────────────────
    // USER LIST
    // ─────────────────────────────────────────
    public UserListResponse getUserList(UserListRequest request) {

        int page =
                Math.max(request.getPage(), 1);

        int pageSize =
                Math.max(request.getPageSize(), 1);

        long skip =
                (long) (page - 1) * pageSize;

        List<AggregationOperation> operations =
                new ArrayList<>();

        // =====================================================
        // FILTERS
        // =====================================================

        List<Criteria> criteriaList =
                new ArrayList<>();

        // DATE FILTER (optional)
        if ((request.getFilterType() != null && !request.getFilterType().isBlank())
                || (request.getStartDate() != null && !request.getStartDate().isBlank())
                || (request.getEndDate() != null && !request.getEndDate().isBlank())) {

            DateRange range = resolveDateRange(
                    request.getFilterType(),
                    request.getStartDate(),
                    request.getEndDate());

            criteriaList.add(
                    Criteria.where("createdAt")
                            .gte(range.start())
                            .lte(range.end())
            );
        }

        // SEARCH
        if (request.getSearch() != null
                && !request.getSearch().isBlank()) {

            criteriaList.add(
                    new Criteria().orOperator(

                            Criteria.where("username")
                                    .regex(request.getSearch(), "i"),

                            Criteria.where("userId")
                                    .regex(request.getSearch(), "i")
                    )
            );
        }

        // STATUS
        if (request.getStatus() != null
                && !request.getStatus().equalsIgnoreCase("All")) {

            criteriaList.add(
                    Criteria.where("status")
                            .is(request.getStatus().toUpperCase())
            );
        }
        // SUB DEPARTMENT FILTER
        if (request.getSubDepartmentId() != null) {

            criteriaList.add(
                    Criteria.where("subDepartmentIds")
                            .is(request.getSubDepartmentId())
            );
        }


        // APPLY FILTERS
        if (!criteriaList.isEmpty()) {

            operations.add(
                    Aggregation.match(
                            new Criteria().andOperator(
                                    criteriaList.toArray(new Criteria[0])
                            )
                    )
            );
        }

        // =====================================================
        // ROLE LOOKUP
        // =====================================================

        operations.add(
                Aggregation.lookup(
                        "roles",
                        "roleId",
                        "roleId",
                        "role"
                )
        );

        operations.add(
                Aggregation.unwind(
                        "role",
                        true
                )
        );

        // ROLE FILTER
        if (request.getRole() != null
                && !request.getRole().equalsIgnoreCase("All")) {

            operations.add(
                    Aggregation.match(
                            Criteria.where("role.roleName")
                                    .is(request.getRole())
                    )
            );
        }

        // =====================================================
        // DEPARTMENT LOOKUP
        // =====================================================

        operations.add(
                Aggregation.lookup(
                        "departments",
                        "subDepartmentIds",
                        "subDepartments.subDepartmentId",
                        "departments"
                )
        );

        // DEPARTMENT FILTER
        if (request.getDepartment() != null
                && !request.getDepartment().equalsIgnoreCase("All")) {

            operations.add(
                    Aggregation.match(
                            Criteria.where("departments.departmentName")
                                    .is(request.getDepartment())
                    )
            );
        }

        // SORT (most recent first)
        // =====================================================
        operations.add(
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        // =====================================================
        // FACET
        // =====================================================

        FacetOperation facetOperation =
                Aggregation.facet(
                                Aggregation.skip(skip),
                                Aggregation.limit(pageSize)

                        ).as("users")

                        .and(
                                Aggregation.count()
                                        .as("totalRecords")
                        ).as("totalCount");

        operations.add(facetOperation);

        Aggregation aggregation =
                Aggregation.newAggregation(
                        operations
                );

        AggregationResults<Document> results =
                mongoTemplate.aggregate(
                        aggregation,
                        "users",
                        Document.class
                );

        Document result =
                results.getUniqueMappedResult();

        // =====================================================
        // EMPTY RESPONSE
        // =====================================================

        if (result == null) {

            return UserListResponse.builder()
                    .users(new ArrayList<>())
                    .pagination(
                            PaginationDto.builder()
                                    .page(page)
                                    .pageSize(pageSize)
                                    .totalRecords(0L)
                                    .totalPages(0)
                                    .build()
                    )
                    .build();
        }

        // =====================================================
        // USERS
        // =====================================================

        List<Document> usersDocs =
                (List<Document>) result.get("users");

        List<UserListItemDto> users =
                usersDocs.stream()
                        .map(this::mapToUserListItem)
                        .toList();

        // =====================================================
        // TOTAL COUNT
        // =====================================================

        List<Document> totalCount =
                (List<Document>) result.get("totalCount");

        long totalRecords = 0;

        if (totalCount != null
                && !totalCount.isEmpty()) {

            totalRecords =
                    ((Number) totalCount.get(0)
                            .get("totalRecords"))
                            .longValue();
        }

        int totalPages =
                (int) Math.ceil(
                        (double) totalRecords / pageSize
                );

        // =====================================================
        // RESPONSE
        // =====================================================

        return UserListResponse.builder()
                .users(users)
                .pagination(
                        PaginationDto.builder()
                                .page(page)
                                .pageSize(pageSize)
                                .totalRecords(totalRecords)
                                .totalPages(totalPages)
                                .build()
                )
                .build();
    }

    // ─────────────────────────────────────────
    // USER MAPPER
    // ─────────────────────────────────────────

    private UserListItemDto mapToUserListItem(Document doc) {

        Document role = (Document) doc.get("role");
        List<Document> departments = (List<Document>) doc.get("departments");
        RolesDto roleDto = null;
        if (role != null) {
            roleDto = RolesDto.builder().roleId(((Number) role.get("roleId")).longValue())
                    .roleName(role.getString("roleName"))
                    .build();
        }

        List<Long> userSubDepartmentIds = Optional.ofNullable((List<?>) doc.get("subDepartmentIds"))
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(id -> ((Number) id).longValue())
                        .toList();

        List<DeptsDto> deptDtos = new ArrayList<>();
        if (departments != null) {
            deptDtos = departments.stream()
                            .map(dept ->
                                    mapDepartment(
                                            dept,
                                            userSubDepartmentIds))
                            .filter(dept ->
                                    !dept.getSubDepartments().isEmpty())
                            .toList();
        }

        Boolean mustChange = doc.getBoolean("mustChangePassword");
        boolean needsChange = Boolean.TRUE.equals(mustChange);

        return UserListItemDto.builder()
                .user_uuid(doc.getString("userUUID"))
                .userId(doc.getString("userId"))
                .username(doc.getString("username"))
                .role(roleDto)
                .department(deptDtos)
                .status(doc.getString("status"))
                .mustChangePassword(needsChange)
                .temporaryPassword(needsChange ? doc.getString("temporaryPassword") : null)
                .build();
    }

    // ─────────────────────────────────────────
    // DEPARTMENT MAPPER
    // ─────────────────────────────────────────

    private DeptsDto mapDepartment(Document doc, List<Long> userSubDepartmentIds) {

        List<Document> subDocs = (List<Document>) doc.get("subDepartments");
        List<SubDeptsDto> subDtos = new ArrayList<>();
        if (subDocs != null) {
            subDtos = subDocs.stream().filter(sd ->
                                    userSubDepartmentIds.contains(
                                            ((Number) sd.get("subDepartmentId"))
                                                    .longValue()))
                            .map(sd ->
                                    SubDeptsDto.builder()
                                            .subDepartmentId(
                                                    ((Number) sd.get("subDepartmentId"))
                                                            .longValue())
                                            .subDepartmentName(
                                                    sd.getString("subDepartmentName"))
                                            .build())
                            .toList();
        }
        return DeptsDto.builder().departmentId(((Number) doc.get("departmentId"))
                                .longValue()).departmentName(doc.getString("departmentName"))
                .subDepartments(subDtos).build();
    }

    // ─────────────────────────────────────────
    // USER STATS
    // ─────────────────────────────────────────

    private record DateRange(LocalDateTime start, LocalDateTime end) {
    }
    private DateRange resolveDateRange(String filterType,
                                                    String startDate, String endDate) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = switch (filterType) {
            case "day" -> now.toLocalDate().atStartOfDay();
            case "week" -> now.toLocalDate()
                    .with(DayOfWeek.MONDAY).atStartOfDay();
            case "month" -> now.toLocalDate()
                    .withDayOfMonth(1).atStartOfDay();
            case "custom" -> {
                if (startDate == null || startDate.isBlank())
                    throw new AcemException(ErrorCode.INVALID_REQUEST,
                            "startDate is required for custom filter");
                yield DateTimeConversionUtil.parseLocalDate(startDate).atStartOfDay();
            }
            default -> throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "filterType must be day, week, month, or custom");
        };

        LocalDateTime end = now;
        if (endDate != null && !endDate.isBlank()) {
            end = DateTimeConversionUtil.parseLocalDate(endDate).atTime(23, 59, 59);
        }

        if (start.isAfter(end)) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "startDate cannot be greater than endDate");
        }

        return new DateRange(start, end);
    }
    public UserStatsResponse getUserStats(UserStatsRequest request) {

        DateRange range = resolveDateRange(
                request.getFilterType(),
                request.getStartDate(),
                request.getEndDate());

        LocalDateTime start = range.start();
        LocalDateTime end = range.end();

        long totalUsers = userRepository.countByCreatedAtBetween(start, end);

        long activeUsers = userRepository
                .countByStatusAndCreatedAtBetween(
                        Status.ACTIVE, start, end);

        long inactiveUsers = userRepository
                .countByStatusAndCreatedAtBetween(
                        Status.INACTIVE, start, end);

        // Pending reset requests are actionable items — count all PENDING (not date-scoped)
        long pendingResetRequests =
                passwordResetRequestRepository.countByStatus(ResetRequestStatus.PENDING);

        return UserStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .pendingResetRequests(pendingResetRequests)
                .build();
    }

}
