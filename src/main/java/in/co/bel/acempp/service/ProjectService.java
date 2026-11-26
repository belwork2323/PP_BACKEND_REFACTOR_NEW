package in.co.bel.acempp.service;

import in.co.bel.acempp.dto.general.PaginationDto;
import in.co.bel.acempp.dto.project.*;
import in.co.bel.acempp.entity.Project;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.repository.BatchRepository;
import in.co.bel.acempp.repository.ProjectRepository;
import in.co.bel.acempp.repository.sourcing.RocketMotorCasingRepository;
import in.co.bel.acempp.util.DateTimeConversionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


import java.time.DayOfWeek;
import java.time.LocalDate;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final BatchRepository batchRepository;
    private final RocketMotorCasingRepository rocketMotorCasingRepository;

    // ─────────────────────────────────────────
    // CREATE PROJECT
    // ─────────────────────────────────────────
    public CreateProjectResponse createProject(CreateProjectRequest request, String createdBy) {

        // 1. Duplicate Check
        if (projectRepository.existsByProjectNameIgnoreCaseAndIsDeletedFalse(request.getProjectName().trim())) {
            throw new AcemException(
                    ErrorCode.DUPLICATE_PROJECT,
                    "Project with same name already exists"
            );
        }
        LocalDate today = LocalDateTime.now().toLocalDate();

        // 2. Generate Project ID
        String generatedProjectId = generateProjectId(today);

        // 3. Save Entity
        Project project = Project.builder().projectId(generatedProjectId).projectName(request.getProjectName().trim())
                .projectDescription(request.getProjectDescription().trim())
                .projectDate(LocalDate.now())
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        projectRepository.save(project);
        // 4. Response
        return CreateProjectResponse.builder()
                .projectId(project.getProjectId())
                .projectName(project.getProjectName())
                .projectDescription(project.getProjectDescription())
                .projectDate(LocalDate.now())
                .createdOn(project.getCreatedAt())
                .build();
    }

    // ─────────────────────────────────────────
    // GENERATE PROJECT ID
    // Example: PRJ-2026-0001
    private String generateProjectId(LocalDate today) {

        long count = projectRepository.count() + 1;

        return String.format(
                "PRJ-%d-%04d",
                today.getYear(),
                count
        );
    }

    // ─────────────────────────────────────────
    // UPDATE PROJECT
    // ─────────────────────────────────────────
    public UpdateProjectResponse updateProject(UpdateProjectRequest request) {

        // 1. Fetch Existing Project
        Project project = projectRepository.findByProjectIdAndIsDeletedFalse(request.getProjectId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.PROJECT_NOT_FOUND,
                        "No project found for given projectId"
                ));

        // 2. Validate At Least One Field
        boolean isNameEmpty = request.getProjectName() == null || request.getProjectName().trim().isEmpty();
        boolean isDescriptionEmpty = request.getProjectDescription() == null ||
                request.getProjectDescription().trim().isEmpty();

        if (isNameEmpty && isDescriptionEmpty) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "At least one field must be provided"
            );
        }

        // 3. Update Project Name
        if (!isNameEmpty) {
            boolean alreadyExists = projectRepository.existsByProjectNameIgnoreCaseAndProjectIdNotAndIsDeletedFalse(
                    request.getProjectName().trim(),
                    request.getProjectId());
            if (alreadyExists) {
                throw new AcemException(ErrorCode.DUPLICATE_PROJECT, "Another project with same name already exists"
                );
            }
            project.setProjectName(request.getProjectName().trim()
            );
        }

        // 4. Update Description
        if (request.getProjectDescription() != null) {
            String trimmed = request.getProjectDescription().trim();
            project.setProjectDescription(trimmed.isEmpty() ? "" : trimmed);
        }
        // 5. Update Audit Field
        project.setUpdatedAt(LocalDateTime.now());
        Project updatedProject = projectRepository.save(project);
        return UpdateProjectResponse.builder()
                .projectId(updatedProject.getProjectId())
                .projectName(updatedProject.getProjectName())
                .projectDescription(updatedProject.getProjectDescription())
                .projectDate(updatedProject.getProjectDate().toString())
                .createdOn(updatedProject.getCreatedAt())
                .updatedOn(updatedProject.getUpdatedAt())
                .build();
    }

    public ProjectListResponse getProjectList(ProjectListRequest request) {
        Sort sort = buildSort(request);
        Pageable pageable = PageRequest.of(
                request.getPage() - 1,
                request.getLimit(),
                sort
        );

        LocalDate fromDate = null;
        LocalDate toDate = null;

        if (request.getStartDate() != null && !request.getStartDate().isBlank()) {
            fromDate = DateTimeConversionUtil.parseLocalDate(request.getStartDate());
        }

        if (request.getEndDate() != null && !request.getEndDate().isBlank()) {
            toDate = DateTimeConversionUtil.parseLocalDate(request.getEndDate());
        }

        String search = request.getSearch();
        Page<Project> page = fetchProjects(search, fromDate, toDate, pageable);

        List<ProjectDataDTO> projects = page.getContent()
                .stream()
                .map(project -> ProjectDataDTO.builder()
                        .projectId(project.getProjectId())
                        .projectName(project.getProjectName())
                        .projectDescription(project.getProjectDescription())
                        .projectDate(project.getProjectDate() != null ? project.getProjectDate().toString() : null)
                        .createdOn(project.getCreatedAt())
                        .isActive(!Boolean.TRUE.equals(project.getIsDeleted()))
                        .build())
                .collect(Collectors.toList());

        return ProjectListResponse.builder()
                .projects(projects)
                .pagination(
                        PaginationDto.builder()
                                .page(request.getPage())
                                .pageSize(request.getLimit())
                                .totalRecords(page.getTotalElements())
                                .totalPages(page.getTotalPages())
                                .build()
                )
                .build();
    }

    private Sort buildSort(ProjectListRequest request) {
        String field = switch (
                request.getSortBy() != null
                        ? request.getSortBy()
                        : "") {
            case "projectName" -> "projectName";
            case "projectDate" -> "projectDate";
            case "createdOn" -> "createdAt";
            default -> "createdAt";
        };
        return "asc".equalsIgnoreCase(request.getSortOrder())
                ? Sort.by(field).ascending()
                : Sort.by(field).descending();
    }

    private Page<Project> fetchProjects(String search, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasDate = fromDate != null && toDate != null;

        if (hasSearch && hasDate) {
            return projectRepository
                    .findByProjectDateBetweenAndProjectNameContainingIgnoreCase(
                            fromDate,
                            toDate,
                            search.trim(),
                            pageable
                    );
        }
        if (hasSearch) {
            return projectRepository.findByProjectNameContainingIgnoreCase(search.trim(), pageable);
        }
        if (hasDate) {
            return projectRepository.findByProjectDateBetween(fromDate, toDate, pageable);
        }
        return projectRepository.findAll(pageable);
    }

    public DeleteProjectResponse deleteProject(DeleteProjectRequest request) {

        Project project = projectRepository.findByProjectIdAndIsDeletedFalse(request.getProjectId())
                .orElseThrow(() -> new AcemException(ErrorCode.PROJECT_NOT_FOUND,
                        "No project found for given projectId"));

        if (batchRepository.existsByProjectIdAndIsDeletedFalse(request.getProjectId())
                || rocketMotorCasingRepository.existsByProjectIdAndActiveTrue(request.getProjectId())) {
            throw new AcemException(ErrorCode.PROJECT_IN_USE,
                    "Project is linked with active batches");
        }

        project.setIsDeleted(true);
        project.setDeletedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);
        return DeleteProjectResponse.builder().projectId(project.getProjectId()).deletedOn(LocalDateTime.now()).build();
    }

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
            case "six_months" -> now.minusMonths(6);
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

    public ProjectStatsResponse getProjectStats(ProjectStatsRequest request) {

        DateRange range = resolveDateRange(
                request.getFilterType(),
                request.getStartDate(),
                request.getEndDate());

        LocalDateTime start = range.start();
        LocalDateTime end = range.end();

        long activeProjectsCount = projectRepository
                .countByIsDeletedFalseAndCreatedAtBetween(start, end);

        long inactiveProjects = projectRepository
                .countByIsDeletedTrueAndCreatedAtBetween(start, end);

        long totalProjects = activeProjectsCount + inactiveProjects;

        Set<String> activeProjectIds = new HashSet<>();
        batchRepository.findDistinctProjectIds().forEach(batch -> {
            if (batch.getProjectId() != null) {
                activeProjectIds.add(batch.getProjectId());
            }
        });

        long projectsWithBatches = activeProjectIds.isEmpty()
                ? 0
                : projectRepository.countByProjectIdInAndIsDeletedFalseAndCreatedAtBetween(
                activeProjectIds,
                start,
                end);

        long idleProjects = activeProjectsCount - projectsWithBatches;

        return ProjectStatsResponse.builder()
                .totalProjects(totalProjects)
                .activeProjects(activeProjectsCount)
                .inactiveProjects(inactiveProjects)
                .idleProjects(inactiveProjects)
                .build();
    }

}
