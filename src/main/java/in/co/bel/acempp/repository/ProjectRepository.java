package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface ProjectRepository extends MongoRepository<Project, String> {

    Optional<Project> findByProjectIdAndIsDeletedFalse(String projectId);

    Optional<Project> findByProjectId(String projectId);

    boolean existsByProjectNameIgnoreCaseAndIsDeletedFalse(String projectName);

    boolean existsByProjectNameIgnoreCaseAndProjectIdNotAndIsDeletedFalse(
            String projectName,
            String projectId
    );


    long countByProjectIdInAndIsDeletedFalseAndCreatedAtBetween(
            Collection<String> projectIds,
            LocalDateTime start,
            LocalDateTime end
    );
    Page<Project> findByIsDeletedFalse(
            Pageable pageable
    );

    Page<Project> findByIsDeletedFalseAndProjectDateBetween(
            LocalDate from,
            LocalDate to,
            Pageable pageable
    );

    Page<Project> findByIsDeletedFalseAndProjectNameContainingIgnoreCase(
            String search,
            Pageable pageable
    );

    Page<Project> findByIsDeletedFalseAndProjectDateBetweenAndProjectNameContainingIgnoreCase(
            LocalDate from,
            LocalDate to,
            String search,
            Pageable pageable
    );

    long countByIsDeletedFalse();

    long countByIsDeletedFalseAndCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByProjectIdInAndIsDeletedFalse(Collection<String> projectIds);
}
