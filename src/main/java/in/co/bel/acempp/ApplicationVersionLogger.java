package in.co.bel.acempp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationVersionLogger {

    private static final Logger log =
            LoggerFactory.getLogger(ApplicationVersionLogger.class);

    @Value("${application.name}")
    private String applicationName;

    @Value("${application.version}")
    private String applicationVersion;

    @Value("${application.artifact}")
    private String applicationArtifact;

    @EventListener(ApplicationReadyEvent.class)
    public void logApplicationVersion() {

        log.info("==============================================");
        log.info("APPLICATION STARTED SUCCESSFULLY");
        log.info("Application Name    : {}", applicationName);
        log.info("Application Artifact: {}", applicationArtifact);
        log.info("Application Version : {}", applicationVersion);
        log.info("==============================================");
    }
}