package in.co.bel.acempp.service.sourcing;

import in.co.bel.acempp.dto.rocketmotorcasing.RmcSectionsRequest;
import in.co.bel.acempp.dto.rocketmotorcasing.RmcSectionsResponse;
import in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments.Sections;
import org.springframework.stereotype.Component;

@Component
public class RmcSectionsMapper {

    public Sections toPersistence(RmcSectionsRequest request) {
        if (request == null) return null;
        return Sections.builder()
                .motorReceipt(request.getMotorReceipt())
                .visualInspection(request.getVisualInspection())
                .weightment(request.getWeightment())
                .dimensionalInspection(request.getDimensionalInspection())
                .mockTrial(request.getMockTrial())
                .reportUpload(request.getReportUpload())
                .radiographyDetails(request.getRadiographyDetails())
                .build();
    }

    public RmcSectionsResponse toResponse(Sections sections) {
        if (sections == null) return null;
        return RmcSectionsResponse.builder()
                .motorReceipt(sections.getMotorReceipt())
                .visualInspection(sections.getVisualInspection())
                .weightment(sections.getWeightment())
                .dimensionalInspection(sections.getDimensionalInspection())
                .mockTrial(sections.getMockTrial())
                .reportUpload(sections.getReportUpload())
                .radiographyDetails(sections.getRadiographyDetails())
                .build();
    }
}
