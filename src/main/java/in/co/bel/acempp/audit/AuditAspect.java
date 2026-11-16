package in.co.bel.acempp.audit;


import in.co.bel.acempp.entity.AuditLog;
import in.co.bel.acempp.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;
    private final ObjectProvider<HttpServletRequest> requestProvider;

    @Around("@annotation(audit)")
    public Object audit(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {

        Object response = joinPoint.proceed();

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                        .getRequest();

        AuditLog log = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .userId((String) authentication.getDetails())
                .username(authentication.getName())
                .role(authentication.getAuthorities().iterator().next().getAuthority())
                .action(audit.action().name())
                .ipAddress(getClientIp())
                .build();

        auditService.save(log);

        return response;
    }

    private String getClientIp() {
        HttpServletRequest request = requestProvider.getIfAvailable();
        if (request == null) {
            return "N/A";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = (forwarded != null && !forwarded.isBlank()) ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }
}