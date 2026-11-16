package in.co.bel.acempp.controller;

import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.masterdata.*;
import in.co.bel.acempp.dto.masterdata.curingcycles.*;
import in.co.bel.acempp.dto.masterdata.insulation.*;
import in.co.bel.acempp.dto.masterdata.materials.*;
import in.co.bel.acempp.dto.masterdata.mixingcycles.*;
import in.co.bel.acempp.dto.masterdata.qcdivisions.*;
import in.co.bel.acempp.dto.masterdata.qualitychecks.*;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.masterdata.CuringCycleMasterDataService;
import in.co.bel.acempp.service.masterdata.InsulationSpecificationMasterDataService;
import in.co.bel.acempp.service.masterdata.MasterDataService;
import in.co.bel.acempp.service.masterdata.MaterialGradeSpecPromotionMigrationService;
import in.co.bel.acempp.service.masterdata.MaterialMasterDataService;
import in.co.bel.acempp.service.masterdata.MixingCycleMasterDataService;
import in.co.bel.acempp.service.masterdata.MixingCycleQualityCheckMigrationService;
import in.co.bel.acempp.service.masterdata.QcDivisionMasterDataService;
import in.co.bel.acempp.service.masterdata.QualityCheckMasterDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/master-data")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('Admin', 'Centre Head')")
public class MasterDataController {

    private final MasterDataService masterDataService;
    private final MaterialMasterDataService materialMasterDataService;
    private final InsulationSpecificationMasterDataService insulationSpecificationMasterDataService;
    private final MixingCycleMasterDataService mixingCycleMasterDataService;
    private final MixingCycleQualityCheckMigrationService mixingCycleQualityCheckMigrationService;
    private final MaterialGradeSpecPromotionMigrationService materialGradeSpecPromotionMigrationService;
    private final CuringCycleMasterDataService curingCycleMasterDataService;
    private final QualityCheckMasterDataService qualityCheckMasterDataService;
    private final QcDivisionMasterDataService qcDivisionMasterDataService;

    @GetMapping("/types")
    public ResponseEntity<AcemResponse<List<MasterDataTypeDescriptor>>> getTypes() {
        return AcemTemplate.success(masterDataService.getTypes(), "Master data types fetched successfully");
    }

    @PostMapping("/{type}/list")
    public ResponseEntity<AcemResponse<MasterDataListResponse>> list(
            @PathVariable String type,
            @RequestBody(required = false) MasterDataListRequest request) {
        return AcemTemplate.success(masterDataService.list(type, request), "Master data list fetched successfully");
    }

    @PostMapping("/{type}/create")
    public ResponseEntity<AcemResponse<MasterDataRecordDto>> create(
            @PathVariable String type,
            @Valid @RequestBody MasterDataCreateRequest request) {
        return AcemTemplate.created(masterDataService.create(type, request), "Master data created successfully");
    }

    @PostMapping("/{type}/update")
    public ResponseEntity<AcemResponse<MasterDataRecordDto>> update(
            @PathVariable String type,
            @Valid @RequestBody MasterDataUpdateRequest request) {
        return AcemTemplate.success(masterDataService.update(type, request), "Master data updated successfully");
    }

    @PostMapping("/{type}/delete")
    public ResponseEntity<AcemResponse<MasterDataRecordDto>> delete(
            @PathVariable String type,
            @Valid @RequestBody MasterDataDeleteRequest request) {
        return AcemTemplate.success(masterDataService.delete(type, request), "Master data disabled successfully");
    }

    @PostMapping("/{type}/enable")
    public ResponseEntity<AcemResponse<MasterDataRecordDto>> enable(
            @PathVariable String type,
            @Valid @RequestBody MasterDataDeleteRequest request) {
        return AcemTemplate.success(masterDataService.enable(type, request), "Master data enabled successfully");
    }

    // ── Materials ──
    @PostMapping("/materials/list")
    public ResponseEntity<AcemResponse<MaterialMasterListResponse>> listMaterials(
            @RequestBody(required = false) MaterialMasterListRequest request) {
        return AcemTemplate.success(materialMasterDataService.list(request), "Materials fetched successfully");
    }

    @PostMapping("/materials/create")
    public ResponseEntity<AcemResponse<MaterialMasterRecordDto>> createMaterial(
            @Valid @RequestBody MaterialMasterCreateRequest request) {
        return AcemTemplate.created(materialMasterDataService.create(request), "Material created successfully");
    }

    @PostMapping("/materials/update")
    public ResponseEntity<AcemResponse<MaterialMasterRecordDto>> updateMaterial(
            @Valid @RequestBody MaterialMasterUpdateRequest request) {
        return AcemTemplate.success(materialMasterDataService.update(request), "Material updated successfully");
    }

    @PostMapping("/materials/delete")
    public ResponseEntity<AcemResponse<MaterialMasterRecordDto>> deleteMaterial(
            @Valid @RequestBody MaterialMasterDeleteRequest request) {
        return AcemTemplate.success(materialMasterDataService.softDisable(request), "Material disabled successfully");
    }

    @PostMapping("/materials/enable")
    public ResponseEntity<AcemResponse<MaterialMasterRecordDto>> enableMaterial(
            @Valid @RequestBody MaterialMasterDeleteRequest request) {
        return AcemTemplate.success(materialMasterDataService.enable(request), "Material enabled successfully");
    }

    /** One-time: copy unique grade specs onto materials that have no top-level specifications. */
    @PostMapping("/materials/migrate-grade-specs")
    public ResponseEntity<AcemResponse<MaterialGradeSpecPromotionMigrationService.MigrationResult>> migrateMaterialGradeSpecs() {
        return AcemTemplate.success(
                materialGradeSpecPromotionMigrationService.migrate(),
                "Grade specifications promoted to material level");
    }

    // ── Insulation ──
    @PostMapping("/insulation-specifications/list")
    public ResponseEntity<AcemResponse<InsulationSpecListResponse>> listInsulationSpecs(
            @RequestBody(required = false) InsulationSpecListRequest request) {
        return AcemTemplate.success(insulationSpecificationMasterDataService.list(request),
                "Insulation specifications fetched successfully");
    }

    @PostMapping("/insulation-specifications/create")
    public ResponseEntity<AcemResponse<InsulationSpecRecordDto>> createInsulationSpec(
            @Valid @RequestBody InsulationSpecCreateRequest request) {
        return AcemTemplate.created(insulationSpecificationMasterDataService.create(request),
                "Insulation specification created successfully");
    }

    @PostMapping("/insulation-specifications/update")
    public ResponseEntity<AcemResponse<InsulationSpecRecordDto>> updateInsulationSpec(
            @Valid @RequestBody InsulationSpecUpdateRequest request) {
        return AcemTemplate.success(insulationSpecificationMasterDataService.update(request),
                "Insulation specification updated successfully");
    }

    @PostMapping("/insulation-specifications/delete")
    public ResponseEntity<AcemResponse<InsulationSpecRecordDto>> deleteInsulationSpec(
            @Valid @RequestBody InsulationSpecDeleteRequest request) {
        return AcemTemplate.success(insulationSpecificationMasterDataService.softDisable(request),
                "Insulation specification disabled successfully");
    }

    // ── Mixing cycles ──
    @PostMapping("/mixing-cycles/list")
    public ResponseEntity<AcemResponse<MixingCycleMasterListResponse>> listMixingCycles(
            @RequestBody(required = false) MixingCycleListRequest request) {
        return AcemTemplate.success(mixingCycleMasterDataService.list(request), "Mixing cycles fetched successfully");
    }

    @PostMapping("/mixing-cycles/create")
    public ResponseEntity<AcemResponse<MixingCycleMasterRecordDto>> createMixingCycle(
            @Valid @RequestBody MixingCycleMasterCreateRequest request) {
        return AcemTemplate.created(mixingCycleMasterDataService.create(request), "Mixing cycle created successfully");
    }

    @PostMapping("/mixing-cycles/update")
    public ResponseEntity<AcemResponse<MixingCycleMasterRecordDto>> updateMixingCycle(
            @Valid @RequestBody MixingCycleMasterUpdateRequest request) {
        return AcemTemplate.success(mixingCycleMasterDataService.update(request), "Mixing cycle updated successfully");
    }

    @PostMapping("/mixing-cycles/delete")
    public ResponseEntity<AcemResponse<MixingCycleMasterRecordDto>> deleteMixingCycle(
            @Valid @RequestBody MixingCycleMasterDeleteRequest request) {
        return AcemTemplate.success(mixingCycleMasterDataService.softDisable(request), "Mixing cycle disabled successfully");
    }

    @PostMapping("/mixing-cycles/enable")
    public ResponseEntity<AcemResponse<MixingCycleMasterRecordDto>> enableMixingCycle(
            @Valid @RequestBody MixingCycleMasterDeleteRequest request) {
        return AcemTemplate.success(mixingCycleMasterDataService.enable(request), "Mixing cycle enabled successfully");
    }

    /** One-time: copy legacy quality_checks onto mixing cycles by motorStage. */
    @PostMapping("/mixing-cycles/migrate-quality-checks")
    public ResponseEntity<AcemResponse<MixingCycleQualityCheckMigrationService.MigrationResult>> migrateMixingCycleQualityChecks() {
        return AcemTemplate.success(
                mixingCycleQualityCheckMigrationService.migrate(),
                "Quality checks migrated onto mixing cycles");
    }

    // ── Curing cycles ──
    @PostMapping("/curing-cycles/list")
    public ResponseEntity<AcemResponse<CuringCycleMasterListResponse>> listCuringCycles(
            @RequestBody(required = false) CuringCycleListRequest request) {
        return AcemTemplate.success(curingCycleMasterDataService.list(request), "Curing cycles fetched successfully");
    }

    @PostMapping("/curing-cycles/create")
    public ResponseEntity<AcemResponse<CuringCycleMasterRecordDto>> createCuringCycle(
            @Valid @RequestBody CuringCycleMasterCreateRequest request) {
        return AcemTemplate.created(curingCycleMasterDataService.create(request), "Curing cycle created successfully");
    }

    @PostMapping("/curing-cycles/update")
    public ResponseEntity<AcemResponse<CuringCycleMasterRecordDto>> updateCuringCycle(
            @Valid @RequestBody CuringCycleMasterUpdateRequest request) {
        return AcemTemplate.success(curingCycleMasterDataService.update(request), "Curing cycle updated successfully");
    }

    @PostMapping("/curing-cycles/delete")
    public ResponseEntity<AcemResponse<CuringCycleMasterRecordDto>> deleteCuringCycle(
            @Valid @RequestBody CuringCycleMasterDeleteRequest request) {
        return AcemTemplate.success(curingCycleMasterDataService.softDisable(request), "Curing cycle disabled successfully");
    }

    @PostMapping("/curing-cycles/enable")
    public ResponseEntity<AcemResponse<CuringCycleMasterRecordDto>> enableCuringCycle(
            @Valid @RequestBody CuringCycleMasterDeleteRequest request) {
        return AcemTemplate.success(curingCycleMasterDataService.enable(request), "Curing cycle enabled successfully");
    }

    // ── Quality checks (legacy read-only; create/update retired — use Mixing Cycle) ──
    @PostMapping("/quality-checks/list")
    public ResponseEntity<AcemResponse<QualityCheckMasterListResponse>> listQualityChecks(
            @RequestBody(required = false) QualityCheckListRequest request) {
        return AcemTemplate.success(qualityCheckMasterDataService.list(request), "Quality checks fetched successfully");
    }

    // ── QC divisions ──
    @PostMapping("/qc-divisions/list")
    public ResponseEntity<AcemResponse<QcDivisionMasterListResponse>> listQcDivisions(
            @RequestBody(required = false) QcDivisionMasterListRequest request) {
        return AcemTemplate.success(qcDivisionMasterDataService.list(request), "QC divisions fetched successfully");
    }

    @PostMapping("/qc-divisions/create")
    public ResponseEntity<AcemResponse<QcDivisionMasterRecordDto>> createQcDivision(
            @Valid @RequestBody QcDivisionMasterCreateRequest request) {
        return AcemTemplate.created(qcDivisionMasterDataService.create(request), "QC division created successfully");
    }

    @PostMapping("/qc-divisions/update")
    public ResponseEntity<AcemResponse<QcDivisionMasterRecordDto>> updateQcDivision(
            @Valid @RequestBody QcDivisionMasterUpdateRequest request) {
        return AcemTemplate.success(qcDivisionMasterDataService.update(request), "QC division updated successfully");
    }

    @PostMapping("/qc-divisions/delete")
    public ResponseEntity<AcemResponse<QcDivisionMasterRecordDto>> deleteQcDivision(
            @Valid @RequestBody QcDivisionMasterDeleteRequest request) {
        return AcemTemplate.success(qcDivisionMasterDataService.softDisable(request), "QC division disabled successfully");
    }
}
