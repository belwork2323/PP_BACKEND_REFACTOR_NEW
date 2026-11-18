package in.co.bel.acempp.service.sourcing;

import in.co.bel.acempp.dto.file.FileBindingScope;
import in.co.bel.acempp.dto.rawmaterialsourcing.CertificateRequest;
import in.co.bel.acempp.dto.rawmaterialsourcing.CertificateResponse;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.entity.FileAttachments;
import in.co.bel.acempp.entity.sourcing.Lot;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.service.FileBindingService;
import in.co.bel.acempp.service.FileMetadataEnrichmentService;
import in.co.bel.acempp.service.FileReadOnlyService;
import in.co.bel.acempp.util.FileIdExtractionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RawMaterialSourcingCertificateSupport {

    private final FileBindingService filePromotionService;
    private final FileReadOnlyService fileReadOnlyService;
    private final FileMetadataEnrichmentService fileMetadataEnrichmentService;

    void rollbackPromotedFiles(List<String> promotedFileIds) {
        if (promotedFileIds == null || promotedFileIds.isEmpty()) {
            return;
        }
        log.info("Starting rollback for {} promoted files...", promotedFileIds.size());
        for (String fileId : promotedFileIds) {
            if (fileId != null && !fileId.isBlank()) {
                try {
                    filePromotionService.rollbackOrUnbindFile(fileId);
                } catch (Exception e) {
                    log.error("Failed to execute file rollback for fileId={}", fileId, e);
                }
            }
        }
    }

    List<CertificateRequest> promoteCertificatesForCreate(
            List<CertificateRequest> certificateRequests,
            Long subDeptId,
            String sourcingId,
            String lotId,
            List<String> promotedFileIds) {
        List<CertificateRequest> lotCerts = new ArrayList<>();

        if (certificateRequests == null) {
            return lotCerts;
        }

        for (CertificateRequest certReq : certificateRequests) {
            try {
                FileAttachments savedFile = filePromotionService.promoteAndBindFile(
                        certReq.getFileId(),
                        subDeptId,
                        sourcingId,
                        FileBindingScope.forLot(lotId)
                );

                // Track successfully promoted file ID
                promotedFileIds.add(savedFile.getFileId());

                lotCerts.add(CertificateRequest.builder()
                        .fileId(savedFile.getFileId())
                        .fileName(savedFile.getOriginalFileName())
                        .fileUrl(savedFile.getFilePath())
                        .mimeType(savedFile.getMimeType())
                        .certificateType(certReq.getCertificateType())
                        .status(savedFile.getStatus())
                        .readOnly(savedFile.isReadOnly())
                        .build());

            } catch (Exception e) {
                throw new AcemException(
                        ErrorCode.FILE_TRANSFER_ERROR,
                        "File upload failed for cert fileId: " + certReq.getFileId()
                );
            }
        }
        return lotCerts;
    }

    List<CertificateRequest> processCertificateUpdates(
            Lot lot,
            List<CertificateRequest> incoming,
            List<String> promotedFileIds) {
        // Collect existing file IDs from Database
        Set<String> existingDbFileIds = (lot.getCertificates() != null)
                ? lot.getCertificates().stream()
                .map(CertificateRequest::getFileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                : Collections.emptySet();

        // Collect incoming file IDs from Request Payload
        Set<String> incomingFileIds = (incoming != null)
                ? incoming.stream()
                .map(CertificateRequest::getFileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                : Collections.emptySet();

        // Identify files removed by user (present in DB, absent in Payload)
        List<String> removedFileIds = existingDbFileIds.stream()
                .filter(fileId -> !incomingFileIds.contains(fileId))
                .collect(Collectors.toList());

        // Process certificates list
        List<CertificateRequest> updatedCerts = new ArrayList<>();

        if (incoming != null) {
            for (CertificateRequest certReq : incoming) {
                String fileId = certReq.getFileId();

                // Scenario A: Existing File -> Keep as is (already promoted)
                if (existingDbFileIds.contains(fileId)) {
                    CertificateRequest existingCert = lot.getCertificates().stream()
                            .filter(c -> fileId.equals(c.getFileId()))
                            .findFirst()
                            .orElse(null);

                    if (existingCert != null) {
                        // Build a fresh object retaining ALL previous file metadata from DB
                        DocumentDto enrichedCert = existingCert.toDocumentDto();
                        fileMetadataEnrichmentService.enrichDocument(enrichedCert, lot.getSubDepartmentId());
                        updatedCerts.add(CertificateRequest.fromDocumentDto(
                                enrichedCert, certReq.getCertificateType()));
                    }
                }
                // Scenario B: New File -> Promote & Bind
                else {
                    try {
                        FileAttachments savedFile = filePromotionService.promoteAndBindFile(
                                fileId,
                                lot.getSubDepartmentId(),
                                lot.getSourcingId(),
                                FileBindingScope.forLot(lot.getLotId())
                        );

                        promotedFileIds.add(savedFile.getFileId());

                        updatedCerts.add(CertificateRequest.builder()
                                .fileId(savedFile.getFileId())
                                .fileName(savedFile.getOriginalFileName())
                                .fileUrl(savedFile.getFilePath())
                                .certificateType(certReq.getCertificateType())
                                .mimeType(savedFile.getMimeType())
                                .status(savedFile.getStatus())
                                .readOnly(savedFile.isReadOnly())
                                .build());

                    } catch (Exception e) {
                        log.error("Failed to promote new fileId={}", fileId, e);
                        throw new AcemException(
                                ErrorCode.FILE_TRANSFER_ERROR,
                                "File upload failed for new attachment, please try again."
                        );
                    }
                }
            }
        }

        // Purge removed files permanently from Disk & Mongo DB
        if (!removedFileIds.isEmpty()) {
            filePromotionService.purgeRemovedFilesFromDiskAndDb(lot.getSourcingId(), removedFileIds);
        }

        return updatedCerts;
    }

    List<CertificateResponse> buildCertificateResponses(Lot lot, Long subDepartmentId) {
        List<CertificateResponse> certResponses = new ArrayList<>();
        if (lot.getCertificates() != null) {
            for (CertificateRequest cert : lot.getCertificates()) {
                DocumentDto enriched = cert.toDocumentDto();
                fileMetadataEnrichmentService.enrichDocument(enriched, subDepartmentId);
                certResponses.add(CertificateResponse.builder()
                        .fileId(enriched.getFileId())
                        .fileName(enriched.getFileName())
                        .fileUrl(enriched.getFileUrl())
                        .status(enriched.getStatus())
                        .mimeType(enriched.getMimeType())
                        .certificateType(cert.getCertificateType())
                        .readOnly(enriched.getReadOnly())
                        .build());
            }
        }
        return certResponses;
    }

    void markCertificatesReadOnly(Lot entity, Long subDepartmentId) {
        List<DocumentDto> certDocs = entity.getCertificates() == null
                ? List.of()
                : entity.getCertificates().stream()
                .filter(Objects::nonNull)
                .map(CertificateRequest::toDocumentDto)
                .toList();
        fileReadOnlyService.markReadOnly(
                subDepartmentId,
                FileIdExtractionUtil.extractFromDocuments(certDocs));
    }
}