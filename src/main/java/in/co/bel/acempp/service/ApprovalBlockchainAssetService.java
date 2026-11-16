package in.co.bel.acempp.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import in.co.bel.acempp.dto.batch.BatchDetailsRequest;
import in.co.bel.acempp.entity.DepartmentMaster;
import in.co.bel.acempp.entity.manufacturing.RawMaterialPreparation;
import in.co.bel.acempp.entity.qualitycontrol.QcDivisionMaster;
import in.co.bel.acempp.exception.ApiGatewayException;
import in.co.bel.acempp.repository.DepartmentRepository;
import in.co.bel.acempp.repository.qualitycontrol.QcDivisionMasterRepository;
import in.co.bel.acempp.repository.qualitycontrol.QcDivisionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import in.co.bel.acempp.entity.batch.BlockchainEvent;
import in.co.bel.acempp.repository.BlockchainEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalBlockchainAssetService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final BlockchainEventRepository blockchainEventRepository;
    private final DepartmentRepository departmentRepository;
    private final QcDivisionMasterRepository qcDivisionRepository;

    @Value("${blockchain.asset.url:http://172.16.69.88:8080}")
    private String assetBaseUrl;

    @Value("${blockchain.asset.peer-srt:}")
    private String peerSrt;

    @Value("${blockchain.asset.userid:admin}")
    private String userId;

    @Value("${blockchain.asset.schema-id:uid:acem:schema/process-schema}")
    private String schemaId;

    @Value("${blockchain.asset.create.enabled:true}")
    private boolean assetCreateEnabled;

    @Value("${blockchain.asset.crn.id:uid:acem:asset/}")
    private String defaultAssetId;

    public void createAssetOnBlockchain(Long subDepartmentId, String batchId, String formId, Long division, Object payload, Object identifier, String performedBy) {
        if (!assetCreateEnabled) {
            log.info("Blockchain asset CREATE bypassed (blockchain.asset.create.enabled=false) for subDepartmentId={}, batchId={}, formId={}, division={}, identifier={}",
                    subDepartmentId, batchId, formId, division, identifier);
            return;
        }
        if (!isBlockchainActiveDepartment(subDepartmentId) || payload == null) {
            return;
        }

        log.info("Calling blockchain asset API for subDepartmentId={}, batchId={}, formId={}, division={}, identifier={}",
                subDepartmentId, batchId, formId, division, identifier);
        JsonNode request = buildRequest(subDepartmentId, batchId, formId, division, identifier, payload);
        JsonNode response = null;

        try {
            response = invokeAssetGateway(request);
        } catch (Exception e) {
            boolean isConflict409 = false;
            String errorResponseBody = null;
            if (e instanceof org.springframework.web.client.HttpStatusCodeException) {
                org.springframework.web.client.HttpStatusCodeException hsce = (org.springframework.web.client.HttpStatusCodeException) e;
                if (hsce.getStatusCode().value() == 409) {
                    isConflict409 = true;
                    errorResponseBody = hsce.getResponseBodyAsString();
                }
            }

            String errorMsg = e.getMessage() != null ? e.getMessage().toUpperCase() : "";
            if (isConflict409 || errorMsg.contains("ASSET_ALREADY_EXISTS") || errorMsg.contains("ALREADY EXISTS")) {
                log.warn("Asset already exists (409 Conflict). Parsing error response to extract asset payload from 'data'.");

                if (errorResponseBody != null && !errorResponseBody.isBlank()) {
                    try {
                        ObjectMapper objectMapper = new ObjectMapper(); // Or inject your ObjectMapper bean
                        JsonNode errorJson = objectMapper.readTree(errorResponseBody);
                        if (errorJson.has("data") && errorJson.get("data").isObject() && !errorJson.get("data").isEmpty()) {
                            response = errorJson.get("data");
                            log.info("Successfully unwrapped asset payload from 409 error response 'data' field.");
                        } else {
                            response = errorJson;
                        }
                    } catch (Exception parseEx) {
                        log.error("Failed to parse 409 error response body as JSON: {}", parseEx.getMessage());
                        throw e;
                    }
                } else {
                    throw e;
                }
            } else {
                throw e;
            }
        }
        saveBlockchainEvent(subDepartmentId, batchId, identifier, formId, division, performedBy, response);
    }

    public void createAssetOnBlockchain(Long subDepartmentId, String batchId, String formId, Object payload, Object identifier, String performedBy) {
        createAssetOnBlockchain(subDepartmentId, batchId, formId, null, payload, identifier, performedBy);
    }

    public void createBatchAssetOnBlockchain(String batchId, Map<String, Object> batchPayload, String performedBy) {
        if (!assetCreateEnabled) {
            log.info("Batch blockchain asset CREATE bypassed (blockchain.asset.create.enabled=false) for batchId={}", batchId);
            return;
        }
        if (batchPayload == null || batchPayload.isEmpty()) {
            log.warn("Batch blockchain payload is empty for batchId={}", batchId);
            return;
        }

        log.info("Calling blockchain asset API for Batch creation, batchId={}", batchId);
        JsonNode request = buildBatchRequest(batchId, batchPayload);
        JsonNode response = null;

        try {
            response = invokeAssetGateway(request);
        } catch (Exception e) {
            boolean isConflict409 = false;
            String errorResponseBody = null;
            if (e instanceof org.springframework.web.client.HttpStatusCodeException) {
                org.springframework.web.client.HttpStatusCodeException hsce = (org.springframework.web.client.HttpStatusCodeException) e;
                if (hsce.getStatusCode().value() == 409) {
                    isConflict409 = true;
                    errorResponseBody = hsce.getResponseBodyAsString();
                }
            }

            String errorMsg = e.getMessage() != null ? e.getMessage().toUpperCase() : "";
            if (isConflict409 || errorMsg.contains("ASSET_ALREADY_EXISTS") || errorMsg.contains("ALREADY EXISTS")) {
                log.warn("Batch asset already exists (409 Conflict) for batchId={}. Extracting payload from error 'data'.", batchId);

                if (errorResponseBody != null && !errorResponseBody.isBlank()) {
                    try {
                        ObjectMapper objectMapper = new ObjectMapper(); // Or inject your ObjectMapper bean
                        JsonNode errorJson = objectMapper.readTree(errorResponseBody);
                        if (errorJson.has("data") && errorJson.get("data").isObject() && !errorJson.get("data").isEmpty()) {
                            response = errorJson.get("data");
                            log.info("Successfully unwrapped batch asset payload from 409 error response 'data' field.");
                        } else {
                            response = errorJson;
                        }
                    } catch (Exception parseEx) {
                        log.error("Failed to parse 409 error response body as JSON for batchId={}: {}", batchId, parseEx.getMessage());
                        throw e;
                    }
                } else {
                    throw e;
                }
            } else {
                throw e;
            }
        }
        saveBlockchainEvent(null, batchId, null, null, null, performedBy, response);
    }

    private JsonNode buildBatchRequest(String batchId, Map<String, Object> batchPayload) {
        ObjectNode root = objectMapper.createObjectNode();
        String assetId = defaultAssetId + batchId;

        root.put("id", assetId);
        root.put("schemaId", schemaId);
        root.put("subjectId", batchId);
        ObjectNode payloadNode = root.putObject("payload");
        payloadNode.set("data", cleanNulls(objectMapper.valueToTree(batchPayload)));

        ArrayNode tags = root.putArray("tags");
        tags.add("batch");
        tags.add("batch-creation");

        return root;
    }


    private JsonNode buildRequest(Long subDepartmentId, String batchId, String formId, Long division, Object identifier, Object payload) {
        ObjectNode root = objectMapper.createObjectNode();
        String shortName = shortSubDepartmentName(subDepartmentId);
        String assetId = buildAssetId(batchId, shortName, division, identifier);

        root.put("id", assetId);
        root.put("schemaId", schemaId);
        root.put("subjectId", batchId);
        root.put("subjectSecondaryId", formId);

        ObjectNode payloadNode = root.putObject("payload");
        if (payload instanceof Map<?, ?> map && map.containsKey("data")) {
            payloadNode.set("data", cleanNulls(objectMapper.valueToTree(map.get("data"))));
        } else if (payload instanceof JsonNode jsonNode) {
            payloadNode.set("data", cleanNulls(jsonNode));
        } else if (payload != null) {
            payloadNode.set("data", cleanNulls(objectMapper.valueToTree(payload)));
        }

        ArrayNode tags = root.putArray("tags");
        for (String tag : tagsFor(subDepartmentId)) {
            tags.add(tag);
        }
        return root;
    }

    private JsonNode invokeAssetGateway(JsonNode request) {
        if (peerSrt == null || peerSrt.isBlank()) {
            throw new ApiGatewayException("Blockchain configuration error: PEER_SRT is missing.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("peer_srt", peerSrt);
        headers.set("userid", userId);

        String url = assetBaseUrl.replaceAll("/$", "") + "/gateway/api/v1/assets/";
        log.info("POST blockchain asset request to {}", url);

        try {
            long startTime = System.currentTimeMillis();
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    JsonNode.class
            );

            long duration = System.currentTimeMillis() - startTime;
            log.info("Received response from blockchain asset gateway in {} ms with status={}", duration, response.getStatusCode());

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("Blockchain asset gateway rejected request after {} ms with status={} body={}", duration, response.getStatusCode(), response.getBody());
                throw new ApiGatewayException("Blockchain gateway rejected the request with status: " + response.getStatusCode());
            }

            return response.getBody();
        } catch (org.springframework.web.client.HttpStatusCodeException hsce) {
            log.error("Blockchain HTTP error: {} - {}", hsce.getStatusCode(), hsce.getResponseBodyAsString(), hsce);
            // Extract meaningful message from gateway error body if possible, or give a clean UI message
            throw new ApiGatewayException("Blockchain sync failed: Server responded with status " + hsce.getStatusCode().value());
        } catch (RestClientException e) {
            log.error("Blockchain connection failed for request {}", request, e);
            throw new ApiGatewayException("Unable to connect to the blockchain network. Please try again later.");
        } catch (Exception e) {
            log.error("Unexpected error during blockchain invocation", e);
            throw new ApiGatewayException("An unexpected error occurred while communicating with the blockchain.");
        }
    }

    private void saveBlockchainEvent(Long subDepartmentId, String batchId, Object identifier, String formId, Long division, String performedBy, JsonNode body) {
        if (body == null) {
            log.warn("Blockchain response body is null for formId: {}", formId);
            return;
        }
        JsonNode txIdNode = body.has("txId") ? body.get("txId") : null;
        if (txIdNode == null || txIdNode.isNull() || txIdNode.asText().isBlank()) {
            log.warn("Blockchain asset gateway returned no txId for formId: {}", formId);
        }
        String txId = (txIdNode != null && !txIdNode.isNull()) ? txIdNode.asText() : null;
        JsonNode payloadHashNode = body.has("payloadHash") ? body.get("payloadHash") : null;
        String payloadHash = (payloadHashNode != null && !payloadHashNode.isNull()) ? payloadHashNode.asText() : null;
        JsonNode assetIdNode = body.get("id");
        String assetId = (assetIdNode != null && !assetIdNode.isNull() && !assetIdNode.asText().isBlank())
                ? assetIdNode.asText()
                : buildAssetId(batchId, subDepartmentId, division, identifier);

        log.info("Saving blockchain event to local DB for assetId: {}, txId: {}", assetId, txId);
        String departmentName = null;
        String subDepartmentName = null;

        if (subDepartmentId != null) {
            DepartmentMaster department = departmentRepository.findBySubDepartmentsSubDepartmentId(subDepartmentId).orElse(null);
            if (department != null) {
                departmentName = department.getDepartmentName();
                if (department.getSubDepartments() != null) {
                    subDepartmentName = department.getSubDepartments().stream()
                            .filter(sub -> sub.getSubDepartmentId().equals(subDepartmentId))
                            .map(sub -> sub.getSubDepartmentName())
                            .findFirst()
                            .orElse(subDepartmentName);
                }
            }
        }
        String divisionName = null;
        if (division != null) {
            QcDivisionMaster divisionMaster = qcDivisionRepository.findByDivisionId(division)
                    .orElseGet(() -> qcDivisionRepository.findByTypesDivisionId(division).orElse(null));

            if (divisionMaster != null) {
                if (divisionMaster.getDivisionId().equals(division)) {
                    divisionName = divisionMaster.getDivisionName();
                } else if (divisionMaster.getTypes() != null) {
                    divisionName = divisionMaster.getTypes().stream()
                            .filter(type -> type.getDivisionId().equals(division))
                            .map(type -> type.getDivisionName())
                            .findFirst()
                            .orElse("");
                }
            }
        }
        blockchainEventRepository.save(BlockchainEvent.builder()
                .transactionId(txId)
                .batchId(batchId)
                .eventType(eventTypeFor(subDepartmentId))
                .assetId(assetId)
                .department(departmentName)
                .subDepartment(subDepartmentName)
                .division(divisionName)
                .performedBy(performedBy)
                .timestamp(LocalDateTime.now())
                .payloadHash(payloadHash)
                .build());
    }

    private String buildAssetId(String batchId, Long subDepartmentId, Long division, Object identifier) {
        return buildAssetId(batchId, shortSubDepartmentName(subDepartmentId), division, identifier);
    }

    private String buildAssetId(String batchId, String shortDepartmentName, Long divisionId, Object identifier) {
        String name = shortDepartmentName == null || shortDepartmentName.isBlank() ? "BATCH" : shortDepartmentName;

        StringBuilder assetIdBuilder = new StringBuilder(defaultAssetId)
                .append(batchId)
                .append(":")
                .append(name);
        String shortDiv = shortDivisionName(divisionId);
        boolean shouldAppendDivision = divisionId != null;

        if (shouldAppendDivision) {
            assetIdBuilder.append(":").append(shortDiv);
            if (identifier != null && !String.valueOf(identifier).isBlank()) {
                String ref = String.valueOf(identifier).replaceAll("[^A-Za-z0-9_.:-]", "-");
                assetIdBuilder.append(":").append(ref);
            }
        } else if (identifier != null && !String.valueOf(identifier).isBlank()) {
            String ref = String.valueOf(identifier).replaceAll("[^A-Za-z0-9_.:-]", "-");
            assetIdBuilder.append(":").append(ref);
        }

        return assetIdBuilder.toString();
    }

    private String shortSubDepartmentName(Long subDepartmentId) {
        return switch (subDepartmentId == null ? 0 : subDepartmentId.intValue()) {
            case 3 -> "RMP"; // Raw Material Preparation
            case 4 -> "CP"; // case preparation
            case 5 -> "MIX"; // mixing
            case 6 -> "CC"; // casting and curing
            case 7 -> "PC"; // post cure operations
            case 8 -> "SSP"; // subscale
            case 9 -> "TRIM"; // trimming
            case 10 -> "NDT"; // ndt
            case 11 -> "QA"; // quality assurance
            case 12 -> "STF"; // static test facility
            case 13 -> "DIS"; // dispatch
            default -> "BATCH";
        };
    }

    private String shortDivisionName(Long divisionId) {
        return switch (divisionId == null ? 0 : divisionId.intValue()) {
            case 101 -> "RMR";   // Raw Material Revalidation
            case 102 -> "RMP";   // Raw Material Processing
            case 2 -> "MIX";   // Mixing
            case 3 -> "HDW";   // Hardware
            case 4 -> "CST";   // Casting
            case 5 -> "CUR";   // Curing
            case 6 -> "DEC";   // De-coring
            case 7 -> "TRM";   // Trimming
            case 8 -> "PSC";   // Post Cure
            case 9 -> "NDT";   // NDT
            case 10 -> "QC";    // QC
            case 11 -> "WGT";   // Weighment
            case 12 -> "STF";   // Static Test Facility
            default -> "QC";    // QC
        };
    }

    private boolean isBlockchainActiveDepartment(Long subDepartmentId) {
        return subDepartmentId != null && subDepartmentId != 1L && subDepartmentId != 2L;
    }

    private String[] tagsFor(Long subDepartmentId) {
        return switch (subDepartmentId == null ? 0 : subDepartmentId.intValue()) {
            case 3 -> new String[]{"raw-material", "preparation", "batch", "RMP"};
            case 4 -> new String[]{"case", "preparation", "batch", "CASE_PREP"};
            case 5 -> new String[]{"mixing", "batch", "MIXING"};
            case 6 -> new String[]{"casting", "curing", "batch", "CASTING_CURING"};
            case 7 -> new String[]{"post-cure", "batch", "POST_CURE"};
            case 8 -> new String[]{"subscale", "processing", "batch", "SUBSCALE"};
            case 9 -> new String[]{"trimming", "batch", "TRIMMING"};
            case 10 -> new String[]{"ndt", "batch", "NDT"};
            case 11 -> new String[]{"qc", "batch", "division"};
            case 12 -> new String[]{"static-testing", "batch", "STF"};
            case 13 -> new String[]{"dispatch", "batch", "DISPATCH"};
            default -> new String[]{"batch", "approval"};
        };
    }

    private String eventTypeFor(Long subDepartmentId) {
        return switch (subDepartmentId == null ? 0 : subDepartmentId.intValue()) {
            case 3 -> "RMP_PREMIX_ASSET_CREATED";
            case 4 -> "CASE_PREP_ASSET_CREATED";
            case 5 -> "MIXING_ASSET_CREATED";
            case 6 -> "CASTING_CURING_ASSET_CREATED";
            case 7 -> "POST_CURE_ASSET_CREATED";
            case 8 -> "SUBSCALE_ASSET_CREATED";
            case 9 -> "TRIMMING_ASSET_CREATED";
            case 10 -> "NDT_ASSET_CREATED";
            case 11 -> "QC_ASSET_CREATED";
            case 12 -> "STF_ASSET_CREATED";
            case 13 -> "DISPATCH_ASSET_CREATED";
            default -> "BATCH_ASSET_CREATED";
        };
    }

    private JsonNode cleanNulls(JsonNode node) {
        if (node == null || node.isNull()) {
            return node;
        }
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            List<String> fields = new ArrayList<>();
            object.fieldNames().forEachRemaining(fields::add);
            for (String field : fields) {
                JsonNode child = object.get(field);
                if (child == null || child.isNull()) {
                    object.remove(field);
                } else {
                    cleanNulls(child);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                cleanNulls(child);
            }
        }
        return node;
    }

    public Object fetchBlockchainAsset(BatchDetailsRequest request) {
        if (peerSrt == null || peerSrt.isBlank()) {
            throw new IllegalArgumentException("BLOCKCHAIN_ASSET_PEER_SRT is not configured");
        }

        try {
            String targetId;
            if (request.getAssetId() != null && !request.getAssetId().isBlank()) {
                targetId = request.getAssetId().trim();
            } else if (request.getBatchId() != null && !request.getBatchId().isBlank()) {
                targetId =defaultAssetId + request.getBatchId().trim();
            } else {
                throw new IllegalArgumentException("Either assetId or batchId must be provided.");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("peer_srt", peerSrt);
            headers.set("userid", userId);

            String baseUrl = assetBaseUrl.replaceAll("/$", "") + "/gateway/api/v1/assets/";

            String finalUrl = org.springframework.web.util.UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("id", targetId)
                    .toUriString();

            log.info("Fetching blockchain asset from URL: {}", finalUrl);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    finalUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    JsonNode.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Blockchain gateway returned non-success status: " + response.getStatusCode());
            }

            JsonNode body = response.getBody();

            if (body.isArray()) {
                return objectMapper.convertValue(
                        body, new TypeReference<List<Map<String, Object>>>() {
                        });
            } else {
                return objectMapper.convertValue(
                        body, new TypeReference<java.util.Map<String, Object>>() {
                        });
            }

        } catch (RestClientException | IllegalArgumentException ex) {
            log.error("Failed to fetch blockchain asset for batchId {} / assetId {}: {}", request.getBatchId(), request.getAssetId(), ex.getMessage());
            throw new RuntimeException("Failed to call blockchain asset API: " + ex.getMessage(), ex);
        }
    }
}
