package in.co.bel.acempp.repository;

import in.co.bel.acempp.entity.Captcha;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CaptchaRepository extends MongoRepository<Captcha, String> {

    Optional<Captcha> findByCaptchaIdAndIsUsedFalse(String captchaId);

}