package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);   // used by JwtService.validate()
    List<RefreshToken> findByUsername(String username);
}