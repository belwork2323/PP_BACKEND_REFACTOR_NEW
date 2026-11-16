package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.User;
import in.co.bel.acempp.enums.Status;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    boolean existsByUserId(String userId);
    long countByStatus(Status status);
    boolean existsByUsername(String username);
    boolean existsByUserUUID(String userUUID);
    Optional<User> findByUserUUID(String userUUID);
    Optional<User> findByUserId(String userId);
    Optional<User> findByUsername(String username);
    long count();
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    long countByStatusAndCreatedAtBetween(Status status, LocalDateTime start, LocalDateTime end);
}