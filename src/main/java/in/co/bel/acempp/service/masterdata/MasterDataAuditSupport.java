package in.co.bel.acempp.service.masterdata;

import in.co.bel.acempp.dto.general.PersonInfo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

public final class MasterDataAuditSupport {

    private MasterDataAuditSupport() {
    }

    public static PersonInfo currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        String userId = authentication.getDetails() == null
                ? null
                : String.valueOf(authentication.getDetails());
        String username = authentication.getPrincipal() == null
                ? null
                : String.valueOf(authentication.getPrincipal());
        if ((userId == null || userId.isBlank()) && (username == null || username.isBlank())) {
            return null;
        }
        return PersonInfo.builder()
                .id(userId)
                .fullName(username)
                .build();
    }

    public static void stampOnCreate(MasterDataAuditable entity) {
        entity.setCreatedBy(currentActor());
        entity.setCreatedOn(LocalDateTime.now());
    }

    public static void stampOnUpdate(MasterDataAuditable entity) {
        entity.setUpdatedBy(currentActor());
        entity.setUpdatedOn(LocalDateTime.now());
    }
}
