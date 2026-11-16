package in.co.bel.acempp.audit;

import in.co.bel.acempp.enums.AuditAction;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audit {
    AuditAction action();
//    String module();
}
