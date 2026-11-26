package in.co.bel.acempp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import in.co.bel.acempp.dto.dispatch.DispatchDetailsResponse;
import in.co.bel.acempp.dto.dispatch.DispatchMotor;
import in.co.bel.acempp.dto.dispatch.DispatchPayloads.FinalMixResult;
import in.co.bel.acempp.dto.dispatch.DispatchPayloads.InspectionRow;
import in.co.bel.acempp.dto.dispatch.DispatchPayloads.PackingRow;
import in.co.bel.acempp.dto.dispatch.DispatchPayloads.PropellantProperty;
import in.co.bel.acempp.dto.dispatch.DispatchPayloads.VehicleRow;
import in.co.bel.acempp.dto.form.*;
import in.co.bel.acempp.dto.casepreparation.*;
import in.co.bel.acempp.dto.castingcuring.*;
import in.co.bel.acempp.dto.mixing.mixingcycle.MixingDetailsDto;
import in.co.bel.acempp.dto.mixing.mixingform.MixingFormRequest;
import in.co.bel.acempp.dto.mixing.mixingform.MixingFormResponse;
import in.co.bel.acempp.dto.ndt.NdtMotorDto;
import in.co.bel.acempp.dto.ndt.NdtResponse;
import in.co.bel.acempp.dto.ndt.motor.RadiographyPlanDetailsDto;
import in.co.bel.acempp.dto.postcureoperations.*;
import in.co.bel.acempp.dto.postcureoperations.PostCureSectionPayloads.*;
import in.co.bel.acempp.dto.rawmaterialpreparation.*;
import in.co.bel.acempp.dto.statictestingfacility.StfMotorRequest;
import in.co.bel.acempp.dto.statictestingfacility.StfResponse;
import in.co.bel.acempp.dto.statictestingfacility.bem.BemMotorDetailsResponseDto;
import in.co.bel.acempp.dto.subscale.subscalecreation.*;
import in.co.bel.acempp.dto.subscale.SubscaleProcessingDetailsResponse;
import in.co.bel.acempp.dto.qcdivision.DivisionDetailDto;
import in.co.bel.acempp.dto.qcdivision.DivisionStatusDto;
import in.co.bel.acempp.dto.qcdivision.QcDivisionFormResponse;
import in.co.bel.acempp.dto.qcdivision.QcMotorStatusDto;
import in.co.bel.acempp.dto.statictestingfacility.StaticMotorData;
import in.co.bel.acempp.dto.statictestingfacility.StfPayloads.*;
import in.co.bel.acempp.dto.rawmaterialsourcing.*;
import in.co.bel.acempp.dto.rawmaterialsourcing.preparation.AdductPreparationDetails;
import in.co.bel.acempp.dto.rawmaterialsourcing.preparation.HtpbBlendingPreparationDetails;
import in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments.*;
import in.co.bel.acempp.dto.rocketmotorcasing.*;
import in.co.bel.acempp.dto.trimming.trimmingcreation.*;
import in.co.bel.acempp.dto.trimming.TrimmingDetailsResponse;
import in.co.bel.acempp.dto.user.operations.SpecificationRangeDto;
import in.co.bel.acempp.entity.CastingStation;
import in.co.bel.acempp.entity.batch.BuildingMaster;
import in.co.bel.acempp.entity.qualitycontrol.BemMotor;
import in.co.bel.acempp.entity.sourcing.Lot;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.PreparationType;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.qualitycontrol.BemMotorRepository;
import in.co.bel.acempp.repository.CastingStationRepository;
import in.co.bel.acempp.repository.BuildingMasterRepository;
import in.co.bel.acempp.repository.sourcing.LotRepository;
import in.co.bel.acempp.service.dispatch.DispatchService;
import in.co.bel.acempp.service.manufacturing.*;
import in.co.bel.acempp.service.qualitycontrol.BemMotorService;
import in.co.bel.acempp.service.qualitycontrol.NdtService;
import in.co.bel.acempp.service.qualitycontrol.QcDivisionService;
import in.co.bel.acempp.service.qualitycontrol.StaticTestingService;
import in.co.bel.acempp.service.sourcing.RawMaterialSourcingService;
import in.co.bel.acempp.service.sourcing.RocketMotorCasingService;
import in.co.bel.acempp.util.DateTimeConversionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class FormPdfService {

    // ── Services ─────────────────────────────────────────────────
    private final RawMaterialSourcingService rmpService;
    private final RocketMotorCasingService rmcService;
    private final RawMaterialPrepService rawMaterialPrepService;
    private final CasePreparationService casePreparationService;
    private final MixingService mixingService;
    private final CastingCuringService castingCuringService;
    private final PostCureOptService postCureOptService;
    private final SubscaleProcessingService subscaleProcessingService;
    private final TrimmingService trimmingService;
    private final NdtService ndtService;
    private final QcDivisionService qcDivisionService;
    private final StaticTestingService stfService;
    private final DispatchService dispatchService;
    private final ObjectMapper mapper;
    private final BemMotorService bemMotorService;
    private final BemMotorRepository bemMotorRepository;
    private final LotRepository lotRepository;
    private final CastingStationRepository castingStationRepository;
    private final BuildingMasterRepository buildingMasterRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    // ── Styling Constants ─────────────────────────────────────────
    private static final Color BRAND_BLUE = new Color(13, 71, 116);
    private static final Color GOLD = new Color(184, 134, 11);
    private static final Color ROW_ALT = new Color(235, 242, 250);
    private static final Color BORDER_LIGHT = new Color(210, 220, 230);
    private static final Color PASS_GREEN = new Color(39, 174, 96);
    private static final Color FAIL_RED = new Color(192, 57, 43);
    private static final Color PENDING_GRAY = new Color(127, 140, 141);
    private static final Color TEXT_GRAY = new Color(120, 120, 120);
    private static final Color WHITE = new Color(255, 255, 255);

    private static final Color STATUS_APPROVED = new Color(39, 174, 96);   // green
    private static final Color STATUS_PENDING = new Color(241, 196, 15);   // yellow / amber
    private static final Color STATUS_REJECTED = new Color(192, 57, 43);    // red
    private static final Color STATUS_DEFAULT = new Color(127, 140, 141);  // gray

    // ── Fonts ─────────────────────────────────────────────────────
    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 15, Font.BOLD, BRAND_BLUE);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT_GRAY);
    private static final Font SECTION_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, BRAND_BLUE);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 8, Font.BOLD, WHITE);
    private static final Font BODY_FONT = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.BLACK);
    private static final Font BODY_BOLD = new Font(Font.HELVETICA, 8, Font.BOLD, Color.BLACK);
    private static final Font GOLD_FONT = new Font(Font.HELVETICA, 8, Font.BOLD, GOLD);
    private static final Font FOOTER_FONT = new Font(Font.HELVETICA, 8, Font.ITALIC, TEXT_GRAY);
    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 8, Font.BOLD, BRAND_BLUE);
    private static final DateTimeFormatter PDF_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter PDF_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");


    // ═════════════════════════════════════════════════════════════
    // MAIN ENTRY POINT
    // ═════════════════════════════════════════════════════════════
    public FormPdfResponse generatePdf(FormPdfRequest request, String approverUsername) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 48, 40);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            // Keep images in document order so later sections (e.g. Remarks) cannot
            // fill the gap when a large drawing is deferred to the next page.
            writer.setStrictImageSequence(true);

            // Page event for header/footer on every page
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter w, Document doc) {
                    PdfContentByte cb = w.getDirectContent();
                    // Page number bottom center
                    ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                            new Phrase("Page " + w.getPageNumber(), FOOTER_FONT),
                            (doc.left() + doc.right()) / 2, doc.bottom() - 10, 0);
                }
            });

            document.open();

            String label = resolveFormLabel(request.getSubDepartmentId());
            renderBySubDepartment(document, request, label, approverUsername);

            document.close();
            return buildResponse(request, baos);

        } catch (AcemException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("PDF generation failed for form {}", request.getFormId(), e);
            throw new AcemException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "PDF generation failed: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════
    // ROUTER
    // ═════════════════════════════════════════════════════════════
    private void renderBySubDepartment(Document doc, FormPdfRequest req,
                                       String label, String approver) throws DocumentException {
        int id = req.getSubDepartmentId().intValue();
        switch (id) {
            case 1 -> { // Raw Material Procurement
                Lot lot = lotRepository.findByLotId(req.getFormId())
                        .orElseThrow(() -> new AcemException(ErrorCode.LOT_NOT_FOUND,
                                "Lot not found for PDF generation: " + req.getFormId()));
                // Reuse the existing service method (or build manually)
                LotDetailsRequest detailsReq = new LotDetailsRequest();
                detailsReq.setLotId(req.getFormId());
                LotDetailsResponse data = rmpService.getLotDetails(detailsReq);
                addDocHeader(doc, label,
                        "Lot: " + data.getLotId() + "  ·  Material: " + data.getMaterialCode()
                                + "  ·  Date: " + today(), approver);
                renderRmp(doc, data);
            }
            case 2 -> { // Rocket Motor Casing
                RmcDetailsRequest detailsReq = new RmcDetailsRequest();
                detailsReq.setMotorCasingId(req.getFormId());
                RmcDetailsResponse data = rmcService.getFormDetails(detailsReq);
                addDocHeader(doc, label,
                        "Motor Casing: " + data.getMotorCasingId()
                                + "  ·  Motor Stage: " + data.getMotorStage()
                                + "  ·  Date: " + today(), approver);
                renderRmc(doc, data);
            }
            case 3 -> {
                PreparationFormDetailsResponse data = rawMaterialPrepService.getFormDetails(req.getFormId());
                addDocHeader(doc, label,
                        "Batch: " + data.getBatchId()
                                + "  ·  Date: " + today(),
                        approver);

                renderRawMaterialPreparation(doc, data);
            }

            case 4 -> {
                CasePreparationResponse data =
                        casePreparationService.getFormDetails(req.getFormId());

                addDocHeader(doc, label,
                        "Batch: " + data.getBatchId()
                                + "  ·  Date: " + today(),
                        approver);

                renderCasePreparation(doc, data);
            }

            case 5 -> { // Mixing
                // Instantiate the correct request object required by your mixingService method
                MixingFormRequest detailsReq = new MixingFormRequest();
                detailsReq.setFormId(req.getFormId());

                MixingFormResponse data = mixingService.getFormDetails(detailsReq);

                addDocHeader(doc, label,
                        "Batch: " + data.getBatchId()
                                + "  ·  Date: " + today(),
                        approver);

                renderMixing(doc, data);
            }
            case 6 -> { // Casting and Curing
                CastingCuringFormResponse data = castingCuringService.getFormDetails(req.getFormId());

                addDocHeader(doc, label,
                        "Batch: " + data.getBatchId()
                                + "  ·  Date: " + today(),
                        approver);

                renderCastingCuring(doc, data);
            }
            case 7 -> { // Post Cure
                PostCureResponse data = postCureOptService.getFormDetails(req.getFormId());

                addDocHeader(doc, label,
                        "Batch: " + data.getBatchId()
                                + "  ·  Date: " + today(),
                        approver);

                renderPostCure(doc, data);
            }
            case 8 -> { // Subscale
                doc.setPageSize(PageSize.A4.rotate());
                SubscaleProcessingDetailsResponse data = subscaleProcessingService.getFormDetails(req.getFormId());

                addDocHeader(doc, label,
                        "Batch: " + data.getBatchId()
                                + "  ·  Date: " + today(),
                        approver);

                renderSubscaleProcessing(doc, data);
            }
            case 9 -> { // Trimming
                TrimmingDetailsResponse data = trimmingService.getFormDetails(req.getFormId());

                addDocHeader(doc, label,
                        "Batch: " + data.getBatchId()
                                + "  ·  Date: " + today(),
                        approver);

                renderTrimmingDetails(doc, data);
            }
            case 10 -> { // NDT
                NdtResponse data = ndtService.getFormDetails(req.getFormId());

                addDocHeader(doc, label,
                        "Batch: " + data.getBatchId()
                                + "  ·  Date: " + today(),
                        approver);

                renderNdtDetails(doc, data);
            }
            case 11 -> { // Quality Control
                QcDivisionFormResponse data = qcDivisionService.getFormDetails(
                        req.getFormId(), req.getSubDepartmentId());

                addDocHeader(doc, label,
                        "Batch: " + data.getBatchId()
                                + "  ·  Date: " + today(),
                        approver);

                renderQcDivisionDetails(doc, data);
            }
            case 12 -> { // STF
                String formId = req.getFormId();

                if (formId != null && formId.startsWith("STF_B")) {
                    // 1. Fetch entity directly from the repository using formId
                    BemMotor motor = bemMotorRepository.findByFormIdAndIsDeletedFalse(formId)
                            .orElseThrow(() -> new AcemException(ErrorCode.RESOURCE_NOT_FOUND, "BEM Motor form not found: " + formId));

                    // 2. Extract motorId from the repository result
                    String motorId = motor.getMotorId();

                    // 3. Pass motorId into getFormDetails
                    BemMotorDetailsResponseDto data = bemMotorService.getBemMotorDetails(motorId);

                    addDocHeader(doc, label,
                            "Motor: " + motorId
                                    + "  ·  Date: " + today(),
                            approver);

                    renderBemMotorDetails(doc, data);
                } else {
                    // Default standard STF service for "STF-" forms
                    StfResponse data = stfService.getFormDetails(formId);

                    addDocHeader(doc, label,
                            "Batch: " + data.getBatchId()
                                    + "  ·  Date: " + today(),
                            approver);

                    renderStfDetails(doc, data);
                }
            }
            case 13 -> { // Dispatch
                DispatchDetailsResponse data = dispatchService.getFormDetails(req.getFormId());

                addDocHeader(doc, label,
                        "Batch: " + data.getBatchId()
                                + "  ·  Date: " + today(),
                        approver);

                renderDispatchDetails(doc, data);
            }


            default -> doc.add(new Paragraph(
                    "Data rendering for Sub-Dept " + id + " is not yet configured.", BODY_FONT));
        }
    }

    // ═════════════════════════════════════════════════════════════
    // 111 — RAW MATERIAL SOURCING
    // ═════════════════════════════════════════════════════════════
    private void renderRmp(Document doc, LotDetailsResponse data) throws DocumentException {

        // ------------------------------------------------------------------
        // 1. TOP INFO STRIP
        // ------------------------------------------------------------------
        PdfPTable headerInfo = createTable(new float[]{25, 25, 25, 25});
        headerInfo.setSpacingAfter(10);

        addInfoStripCell(headerInfo, "Man. Lot/Batch No.", str(data.getLotId()));
        addInfoStripCell(headerInfo, "Supply Order No.", str(data.getSupplyOrderNo()));
        addInfoStripCell(headerInfo, "Manufacturer", str(data.getManufacturerName()));
        addInfoStripCell(headerInfo, "Receipt Date", str(data.getReceiptDate()));

        doc.add(headerInfo);

        PdfPTable lotStatusTable = createTable(new float[]{100});
        PdfPCell lotStatusCell = new PdfPCell();
        lotStatusCell.setBorderColor(BORDER_LIGHT);
        lotStatusCell.setPadding(6);
        lotStatusCell.setBackgroundColor(WHITE);
        Paragraph lotStatus = new Paragraph();
        lotStatus.add(new Chunk("Lot Status: ", LABEL_FONT));
        lotStatus.add(new Chunk(
                workflowStatusLabel(data.getStatus()),
                new Font(Font.HELVETICA, 8, Font.BOLD,
                        getStatusColor(str(data.getStatus())))));
        lotStatusCell.addElement(lotStatus);
        lotStatusTable.addCell(lotStatusCell);
        lotStatusTable.setSpacingAfter(10);
        doc.add(lotStatusTable);
        spacer(doc, 8);

        // ------------------------------------------------------------------
        // 2. ADDUCT / HTPB PREPARATION DETAILS
        // ------------------------------------------------------------------
        renderRmpPreparationDetails(doc, data);

        // ------------------------------------------------------------------
        // 3. MATERIAL SPECIFICATIONS TABLE
        // ------------------------------------------------------------------
        List<SpecificationResponse> specs = data.getSpecifications();
        if (specs != null && !specs.isEmpty()) {

            addSection(doc, "Material Specifications", "");

            PdfPTable table = createTable(new float[]{12, 16, 28, 16, 14, 14});
            addHeaders(table,
                    "MATERIAL",
                    "MAN. LOT/BATCH NO.",
                    "QUALIFICATION PARAMETER",
                    "SPECIFICATION",
                    "ANALYZE RESULT @ SOURCE",
                    "ACEM QC RESULT");

            boolean alt = false;
            String materialCode = str(data.getMaterialCode());
            String lotId = str(data.getLotId());

            for (int i = 0; i < specs.size(); i++) {
                SpecificationResponse spec = specs.get(i);
                Color bg = alt ? ROW_ALT : WHITE;

                // MATERIAL – show only on first row, blank afterwards
                if (i == 0) {
                    addCell(table, materialCode, BODY_BOLD, bg);
                } else {
                    addEmptyCell(table, bg);          // ← important
                }

                // MAN. LOT/BATCH NO. – show only on first row
                if (i == 0) {
                    addCell(table, lotId, BODY_FONT, bg);
                } else {
                    addEmptyCell(table, bg);          // ← important
                }

                // QUALIFICATION PARAMETER
                addCell(table, str(spec.getSpecificationName()), BODY_FONT, bg);

                // SPECIFICATION (range)
                addCell(table, formatRange(spec.getReferenceRange()), GOLD_FONT, bg);

                // ANALYZE RESULT @ SOURCE
                addCell(table, str(spec.getAnalysedResult()), BODY_FONT, bg);

                // ACEM QC RESULT
                addCell(table, str(spec.getAcemQcResult()), BODY_FONT, bg);

                alt = !alt;
            }
            doc.add(table);
            spacer(doc, 12);
        }

        // ------------------------------------------------------------------
        // 4. CERTIFICATES & ATTACHMENTS
        // ------------------------------------------------------------------
        List<CertificateResponse> certs = data.getCertificates();
        if (certs != null && !certs.isEmpty()) {
            addSection(doc, "Certificates & Attachments", "");

            PdfPTable certTable = createTable(new float[]{70, 30});
            addHeaders(certTable, "FILE", "CERTIFICATE TYPE");

            boolean alt = false;
            for (CertificateResponse cert : certs) {
                Color bg = alt ? ROW_ALT : WHITE;
                addCell(certTable, str(cert.getFileName()), BODY_FONT, bg);
                addCell(certTable, str(cert.getCertificateType()), BODY_FONT, bg);
                alt = !alt;
            }
            doc.add(certTable);
        }
    }

    /**
     * Renders ACEM adduct / HTPB blending preparation sections when present on the lot.
     */
    private void renderRmpPreparationDetails(Document doc, LotDetailsResponse data)
            throws DocumentException {
        PreparationType preparationType = data.getPreparationType();
        AdductPreparationDetails adduct = data.getAdductPreparation();
        HtpbBlendingPreparationDetails htpb = data.getHtpbBlendingPreparation();

        boolean showAdduct = adduct != null
                && (preparationType == null || preparationType == PreparationType.ADDUCT);
        boolean showHtpb = htpb != null
                && (preparationType == null || preparationType == PreparationType.HTPB_BLENDING);

        if (!showAdduct && !showHtpb) {
            return;
        }

        if (showAdduct) {
            addSection(doc, "Adduct preparation details", "");
            PdfPTable table = createKeyValueTable();
            addKV(table, "Date of preparation of ADDUCT batch", str(adduct.getAdductBatchPrepDate()));
            addKV(table, "Mfg / Lot no of TMP used", str(adduct.getTmpMfgLotNo()));
            addKV(table, "Total Qty (gm) of TMP used", str(adduct.getTmpTotalQtyGm()));
            addKV(table, "Mfg / Lot no of nBD used", str(adduct.getNbdMfgLotNo()));
            addKV(table, "Total Qty (gm) of nBD used", str(adduct.getNbdTotalQtyGm()));
            addKV(table, "RPM", str(adduct.getRpm()));
            addKV(table, "Process temp", str(adduct.getProcessTemp()));
            addKV(table, "Jacket temp", str(adduct.getJacketTemp()));
            addKV(table, "Process start time", str(adduct.getProcessStartTime()));
            addKV(table, "Process end time", str(adduct.getProcessEndTime()));
            addKV(table, "Final quantity of adduct preparation", str(adduct.getFinalAdductQty()));
            addKV(table, "Date & Time Of Dispatch", str(adduct.getDispatchDateTime()));
            doc.add(table);
            spacer(doc, 10);
        }

        if (showHtpb) {
            addSection(doc, "HTPB blending preparation details", "");
            PdfPTable table = createKeyValueTable();
            addKV(table, "Mfg / Batch lot no", str(htpb.getMfgBatchLotNo()));
            addKV(table, "Total qty", str(htpb.getTotalQty()));
            addKV(table, "Equipment id", str(htpb.getEquipmentId()));
            addKV(table, "Agitator RPM", str(htpb.getAgitatorRpm()));
            addKV(table, "Process temp", str(htpb.getProcessTemp()));
            addKV(table, "Jacket temp", str(htpb.getJacketTemp()));
            addKV(table, "Start date & time", str(htpb.getProcessStartTime()));
            addKV(table, "End date & time", str(htpb.getProcessEndTime()));
            addKV(table, "Any other observation", str(htpb.getOtherObservations()));
            doc.add(table);
            spacer(doc, 10);
        }
    }

    // ═════════════════════════════════════════════════════════════
    // 112 — ROCKET MOTOR CASING
    // ═════════════════════════════════════════════════════════════
    private void renderRmc(Document doc, RmcDetailsResponse data) throws DocumentException {
        RmcSectionsResponse sec = data.getSections();
        if (sec == null) return;

        // ------------------------------------------------------------------
        // 1. IDENTIFICATION (top card)
        // ------------------------------------------------------------------
        addSection(doc, "Identification", "");
        PdfPTable idTable = createTable(new float[]{33, 33, 34});
        idTable.setSpacingAfter(6);

        addIdCardCell(idTable, "MOTOR CASING ID", str(data.getMotorCasingId()));
        addIdCardCell(idTable, "PROJECT ID", data.getProject() != null ? str(data.getProject().getProjectId()) : "-");
        addIdCardCell(idTable, "MOTOR ID", str(data.getMotorId()));

        addIdCardCell(idTable, "MOTOR STAGE", "Stage " + str(data.getMotorStage()));
        MotorReceipt receipt = sec.getMotorReceipt();
        addIdCardCell(idTable, "CASING TYPE", receipt != null ? str(receipt.getCasingType()) : "-");
        String insType = (receipt != null && receipt.getInsulation() != null)
                ? str(receipt.getInsulation().getType()) : "-";
        addIdCardCell(idTable, "INSULATION TYPE", insType);

        addIdCardCell(idTable, "RECEIVING DATE", receipt != null ? str(receipt.getReceivingDate()) : "-");
        addIdCardCell(idTable, "CREATED BY", data.getCreatedByInfo().getFullName()); // replace with actual field if available
        addIdCardCell(idTable, "CREATED ON", str(data.getCreatedAt()));

        // Motor status – full width with colour
        PdfPCell statusCell = new PdfPCell();
        statusCell.setColspan(3);
        statusCell.setBackgroundColor(ROW_ALT);
        statusCell.setPadding(6);
        statusCell.setBorderColor(BORDER_LIGHT);

        Paragraph statusPara = new Paragraph();
        statusPara.add(new Chunk("Motor Status: ", BODY_BOLD));
        statusPara.add(new Chunk(
                workflowStatusLabel(data.getStatus()),
                new Font(Font.HELVETICA, 8, Font.BOLD,
                        getStatusColor(str(data.getStatus())))));
        statusCell.addElement(statusPara);

        idTable.addCell(statusCell);

        doc.add(idTable);
        spacer(doc, 12);

        // ------------------------------------------------------------------
        // 2. MOTOR RECEIPT
        // ------------------------------------------------------------------
        if (receipt != null) {
            addSection(doc, "Motor receipt", "Ref: " + str(data.getMotorCasingId()));

            PdfPTable mr = createKeyValueTable();
            addKV(mr, "Project ID", data.getProject() != null ? str(data.getProject().getProjectId()) : "-");
            addKV(mr, "Motor stage", "Stage " + str(data.getMotorStage()));
            addKV(mr, "Motor ID", str(data.getMotorId()));
            addKV(mr, "Motor casing ID", str(data.getMotorCasingId()));
            addKV(mr, "Casing type", str(receipt.getCasingType()));
            addKV(mr, "Receiving date", str(receipt.getReceivingDate()));

            ItemsReceived items = receipt.getItemsReceived();
            if (items != null) {
                addKV(mr, "Item type / description",
                        prettyText(str(items.getItemType())) + " / " + prettyText(str(items.getDescription())));
                addKV(mr, "Dimension", str(items.getDimension()) + " " + str(items.getUnit()));
                addKV(mr, "Receipt status", str(items.getReceiptStatus()));
                addKV(mr, "Observations", str(items.getObservations()));
            }
            doc.add(mr);
            spacer(doc, 10);
        }

        // ------------------------------------------------------------------
        // 3. RADIOGRAPHY DETAILS
        // ------------------------------------------------------------------
        RadiographyDetails radio = sec.getRadiographyDetails();   // ← adjust getter name if different
        if (radio != null) {
            addSection(doc, "Radiography details", "");

            // Header key-value
            PdfPTable radioHeader = createKeyValueTable();
            addKV(radioHeader, "Radiography plan ID", str(radio.getRadiographyPlanId()));
            addKV(radioHeader, "Plan name", str(radio.getRadiographyPlanName()));
            doc.add(radioHeader);
            spacer(doc, 6);

            // Plan table
            List<RadiographyPlanDetailsDto> planList = radio.getRadiographyPlanDetails();
            if (planList != null && !planList.isEmpty()) {
                Paragraph planTitle = new Paragraph("Plan", SECTION_FONT);
                planTitle.setSpacingAfter(4);
                doc.add(planTitle);

                PdfPTable planTable = createTable(new float[]{8, 14, 16, 10, 18, 18, 16});
                addHeaders(planTable,
                        "SR.",
                        "SECTIONS",
                        "ORIENTATIONS",
                        "SFD",
                        "NO. OF NORMAL EXPOSURE",
                        "NO. OF TANGENTIAL EXPOSURE",
                        "TYPE OF DETECTOR");

                boolean alt = false;
                int sr = 1;
                for (RadiographyPlanDetailsDto p : planList) {
                    Color bg = alt ? ROW_ALT : WHITE;
                    addCell(planTable, String.valueOf(sr++), BODY_FONT, bg);
                    addCell(planTable, str(p.getNumberOfSections()), BODY_FONT, bg);
                    addCell(planTable, str(p.getNumberOfOrientations()), BODY_FONT, bg);
                    addCell(planTable, str(p.getSfd()), BODY_FONT, bg);
                    addCell(planTable, str(p.getNumberOfNormalExposures()), BODY_FONT, bg);
                    addCell(planTable, str(p.getNumberOfTangentialExposures()), BODY_FONT, bg);
                    addCell(planTable, prettyText(str(p.getDetectorType())), BODY_FONT, bg);
                    alt = !alt;
                }
                doc.add(planTable);
            }
            spacer(doc, 10);
        }

        // ------------------------------------------------------------------
        // CLEARANCES
        // ------------------------------------------------------------------
        Clearances clear = receipt != null ? receipt.getClearances() : null;
        if (clear != null) {
            addSection(doc, "Clearances", "");
            PdfPTable cl = createKeyValueTable();
            addKV(cl, "Green card status", str(clear.getGreenCardStatus()));
            addKV(cl, "Green card no.", str(clear.getGreenCardNo()));
            addKV(cl, "Clearance date", str(clear.getClearanceDate()));
            addKV(cl, "Authority", str(clear.getAuthority()));
            addKV(cl, "Details & observations", str(clear.getDetailsAndObservations()));
            doc.add(cl);
            spacer(doc, 10);
        }

        // ------------------------------------------------------------------
        // INSULATION + MECHANICAL / THERMAL PROPERTIES
        // ------------------------------------------------------------------
        Insulation insulation = receipt != null ? receipt.getInsulation() : null;
        if (insulation != null) {
            addSection(doc, "Insulation", "Ref: 1");

            PdfPTable ins = createKeyValueTable();
            addKV(ins, "Insulation curing date", str(insulation.getInsulationCuringDate()));
            addKV(ins, "Insulation type", str(insulation.getType()));
            addKV(ins, "Report no.", str(insulation.getReportNo()));
            addKV(ins, "Receipt status", str(insulation.getReceiptStatus()));
            addKV(ins, "Report upload", documentReference(insulation.getReportUpload()));
            doc.add(ins);
            spacer(doc, 8);

            InsulationSpecificationDto spec = insulation.getInsulationSpecification();
            if (spec != null && spec.getSpecifications() != null) {
                for (SpecificationCategoryDto category : spec.getSpecifications()) {
                    Paragraph catTitle = new Paragraph(category.getCategory(), SECTION_FONT);
                    catTitle.setSpacingBefore(6);
                    catTitle.setSpacingAfter(4);
                    doc.add(catTitle);

                    PdfPTable specTable = createTable(new float[]{40, 20, 20, 20});
                    addHeaders(specTable, "PARAMETER", "SPECIFICATION", "REPORTED", "TEST RESULT @ ACEM");

                    boolean alt = false;
                    for (SpecificationParameterDto p : category.getParameters()) {
                        Color bg = alt ? ROW_ALT : WHITE;
                        String parameterName = p.getSpecificationName() != null
                                && !p.getSpecificationName().isBlank()
                                ? p.getSpecificationName()
                                : str(p.getSpecificationCode());
                        addCell(specTable, parameterName, BODY_FONT, bg);
                        addCell(specTable, formatInsulationRange(p.getReferenceRange()), BODY_FONT, bg);
                        addCell(specTable, str(p.getReported()), BODY_FONT, bg);
                        addCell(specTable, str(p.getAcemSpec()), BODY_FONT, bg);
                        alt = !alt;
                    }
                    doc.add(specTable);
                }
            }
            spacer(doc, 10);
        }

        // ------------------------------------------------------------------
        // NDT / UT REPORT
        // ------------------------------------------------------------------
        NdtUtReport ndt = receipt != null ? receipt.getNdtUtReport() : null;
        if (ndt != null || receipt != null) {
            addSection(doc, "NDT / UT report", "");
            PdfPTable ndtTable = createKeyValueTable();
            if (ndt != null) {
                addKV(ndtTable, "Post-PPT UT date", str(ndt.getPostPptUtDate()));
                addKV(ndtTable, "NDT date", str(ndt.getNdtDate()));
                addKV(ndtTable, "NDT observations", str(ndt.getObservations()));
            }
            if (receipt != null) {
                addKV(ndtTable, "ACEM NDT observations", str(receipt.getAcemNdtObservations()));
                addKV(ndtTable, "Project rubber surface observations",
                        str(receipt.getProjectRubberSurfaceObservations()));
                addKV(ndtTable, "Other details", str(receipt.getOtherDetails()));
            }
            doc.add(ndtTable);
            spacer(doc, 10);
        }

        // ------------------------------------------------------------------
        // VISUAL INSPECTION
        // ------------------------------------------------------------------
        List<VisualInspection> visItems = sec.getVisualInspection();
        if (visItems != null && !visItems.isEmpty()) {
            addSection(doc, "Visual inspection", "");

            PdfPTable viTable = createTable(new float[]{55, 25, 20});
            addHeaders(viTable, "SECTION / PARAMETER", "DETAILS", "REMARKS");

            boolean alt = false;
            for (VisualInspection vi : visItems) {
                Color bg = alt ? ROW_ALT : WHITE;
                String param = !isBlank(vi.getDescription()) ? vi.getDescription() : str(vi.getItemKey());
                addCell(viTable, param, BODY_FONT, bg);
                addCell(viTable, str(vi.getObservations()), BODY_FONT, bg);
                addCell(viTable, str(vi.getRemark()), BODY_FONT, bg);
                alt = !alt;

                if (vi.getSubItems() != null) {
                    for (VisualInspectionSubItem sub : vi.getSubItems()) {
                        bg = alt ? ROW_ALT : WHITE;
                        addCell(viTable, "    " + str(sub.getDescription()), BODY_FONT, bg);
                        addCell(viTable, str(sub.getObservations()), BODY_FONT, bg);
                        addCell(viTable, str(sub.getRemark()), BODY_FONT, bg);
                        alt = !alt;
                    }
                }
            }
            doc.add(viTable);
            spacer(doc, 10);
        }

        // ------------------------------------------------------------------
        // WEIGHMENT
        // ------------------------------------------------------------------
        Weightment weight = sec.getWeightment();
        if (weight != null) {
            addSection(doc, "Weighment", "");
            PdfPTable w = createKeyValueTable();
            addKV(w, "Weight without harness", formatMeasurement(weight.getWeightWithoutHarness()));
            addKV(w, "Weight with harness", formatMeasurement(weight.getWeightWithHarness()));
            if (weight.getWeighscaleCalibration() != null) {
                addKV(w, "Weighscale equipment",
                        str(weight.getWeighscaleCalibration().getEquipmentDetails()));
                addKV(w, "Calibration due date",
                        str(weight.getWeighscaleCalibration().getCalibrationDueDate()));
            }
            doc.add(w);
            spacer(doc, 10);
        }

        // ------------------------------------------------------------------
        // DIMENSIONAL INSPECTION
        // ------------------------------------------------------------------
        List<DimensionalInspection> dims = sec.getDimensionalInspection();
        if (dims != null && !dims.isEmpty()) {
            addSection(doc, "Dimensional inspection", "");

            PdfPTable dimTable = createTable(new float[]{8, 18, 22, 13, 13, 13, 13});
            addHeaders(dimTable, "#", "DIMENSION", "SPEC RANGE (ACEM)", "R2T-R2B", "R1R-R1L", "TL-BR", "TR-BL");

            boolean alt = false;
            int idx = 1;
            for (DimensionalInspection dim : dims) {
                Color bg = alt ? ROW_ALT : WHITE;
                Readings r = dim.getReadings();
                addCell(dimTable, String.valueOf(idx++), BODY_FONT, bg);
                addCell(dimTable, dashIfBlank(dim.getParamName()), BODY_BOLD, bg);
                addCell(dimTable, formatSpecRange(dim.getSpecifiedDimension()), BODY_FONT, bg);
                addCell(dimTable, dashIfBlank(r != null ? r.getR2tR2b() : null), BODY_FONT, bg);
                addCell(dimTable, dashIfBlank(r != null ? r.getR1rR1l() : null), BODY_FONT, bg);
                addCell(dimTable, dashIfBlank(r != null ? r.getTlBr() : null), BODY_FONT, bg);
                addCell(dimTable, dashIfBlank(r != null ? r.getTrBl() : null), BODY_FONT, bg);
                alt = !alt;
            }
            doc.add(dimTable);
            spacer(doc, 10);
        }

        // ------------------------------------------------------------------
        // MOCK TRIAL
        // ------------------------------------------------------------------
        MockTrial mock = sec.getMockTrial();
        if (mock != null) {
            addSection(doc, "Mock trial", "");

            MockTrialBasicDetails basic = mock.getBasicDetails();
            if (basic != null) {
                PdfPTable basicT = createKeyValueTable();
                addKV(basicT, "Casting Station", str(basic.getCastingStation()));
                addKV(basicT, "Mandrel Id No.", str(basic.getMandrelId()));
                addKV(basicT, "Bottom Cup Id", str(basic.getBottomCupId()));
                doc.add(basicT);
                spacer(doc, 8);
            }

            // Motor Dimensions
            List<MotorLengthMeasurement> dimsList = mock.getMotorDimensions();
            if (dimsList != null && !dimsList.isEmpty()) {
                Paragraph h = new Paragraph("Motor Dimensions", SECTION_FONT);
                h.setSpacingAfter(4);
                doc.add(h);

                PdfPTable md = createTable(new float[]{8, 18, 18, 14, 14, 14, 14});
                addHeaders(md,
                        "SR. NO.",
                        "LOOSE FLAP THICKNESS OF RUBBER AT HE (MM)",
                        "HE BOSS WIDTH WITHOUT LF RUBBER (MM)",
                        "HE DIA ID (MM)",
                        "HE OUTER TO NE OUTER",
                        "HE INNER TO NE INNER",
                        "NE OUTER TO HE INNER");

                boolean alt = false;
                for (MotorLengthMeasurement row : dimsList) {
                    Color bg = alt ? ROW_ALT : WHITE;
                    MotorLengthDetails len = row.getMotorLength();
                    addCell(md, dashIfBlank(row.getSrNo()), BODY_FONT, bg);
                    addCell(md, formatMeasurement(row.getLfRubberThicknessHe()), BODY_FONT, bg);
                    addCell(md, formatMeasurement(row.getHeBossWidthWithoutLfRubber()), BODY_FONT, bg);
                    addCell(md, formatMeasurement(row.getHeDiaId()), BODY_FONT, bg);
                    addCell(md, formatMeasurement(len != null ? len.getHeOuterToNeOuter() : null), BODY_FONT, bg);
                    addCell(md, formatMeasurement(len != null ? len.getHeInnerToNeInner() : null), BODY_FONT, bg);
                    addCell(md, formatMeasurement(len != null ? len.getNeOuterToHeInner() : null), BODY_FONT, bg);
                    alt = !alt;
                }
                doc.add(md);
                spacer(doc, 8);
            }

            // Mandrel Assembly Measurements
            List<MandrelAssemblyMeasurement> assemblies = mock.getMandrelAssemblyMeasurements();
            if (assemblies != null && !assemblies.isEmpty()) {
                Paragraph h = new Paragraph("Mandrel Assembly Measurements", SECTION_FONT);
                h.setSpacingAfter(4);
                doc.add(h);

                PdfPTable ma = createTable(new float[]{10, 22, 22, 16, 15, 15});
                addHeaders(ma,
                        "SR. NO.",
                        "MANDREL REST ON DOME (A)",
                        "MANDREL REST ON BOTTOM CUP (B)",
                        "DIFFERENCE C=(A-B)",
                        "BELLOW THICKNESS (D)",
                        "MANDREL LIFT E=(C-D)");

                boolean alt = false;
                for (MandrelAssemblyMeasurement row : assemblies) {
                    Color bg = alt ? ROW_ALT : WHITE;
                    addCell(ma, dashIfBlank(row.getSrNo()), BODY_FONT, bg);
                    addCell(ma, dashIfBlank(row.getMandrelRestOnDomeA()), BODY_FONT, bg);
                    addCell(ma, dashIfBlank(row.getMandrelRestOnBottomCupB()), BODY_FONT, bg);
                    addCell(ma, dashIfBlank(row.getDifferenceC()), BODY_FONT, bg);
                    addCell(ma, dashIfBlank(row.getBellowThicknessD()), BODY_FONT, bg);
                    addCell(ma, dashIfBlank(row.getMandrelLiftE()), BODY_FONT, bg);
                    alt = !alt;
                }
                doc.add(ma);
            }
        }
        // ------------------------------------------------------------------
        // UPLOADED REPORTS
        // ------------------------------------------------------------------
        ReportUploadDto reports = sec.getReportUpload();   // ← adjust getter name if different
        if (reports != null) {
            addSection(doc, "Uploaded Reports", "");

            PdfPTable reportTable = createTable(new float[]{45, 55});
            addHeaders(reportTable, "SECTION", "FILE");

            boolean alt = false;

            // Helper lambda-style calls (or just repeat the pattern)
            alt = addReportRow(reportTable, "NDT / UT Report", reports.getNdtUtReport(), alt);
            alt = addReportRow(reportTable, "Visual Inspection Report", reports.getVisualInspectionReport(), alt);
            alt = addReportRow(reportTable, "Weighment Report", reports.getWeighmentReport(), alt);
            alt = addReportRow(reportTable, "Dimensional Inspection Report", reports.getDimensionalInspectionReport(), alt);
            alt = addReportRow(reportTable, "Mock Trial Report", reports.getMockTrialReport(), alt);
            alt = addReportRow(reportTable, "Insulation Lining Report", reports.getInsulationLiningReport(), alt);

            doc.add(reportTable);
        }
    }

    // ------------------------------------------------------------------
    // Helpers (add if missing)
    // ------------------------------------------------------------------
    private PdfPTable createKeyValueTable() {
        return createTable(new float[]{40, 60});
    }

    private boolean addReportRow(PdfPTable table, String sectionName,
                                 List<DocumentDto> files, boolean alt) {
        if (files == null || files.isEmpty()) {
            Color bg = alt ? ROW_ALT : WHITE;
            addLeftAlignedCell(table, sectionName, bg);
            addLeftAlignedCell(table, "-", bg);
            return !alt;
        }

        for (int i = 0; i < files.size(); i++) {
            Color bg = alt ? ROW_ALT : WHITE;
            DocumentDto docDto = files.get(i);

            // Only show the section name on the first file of the group
            String sectionLabel = (i == 0) ? sectionName : "";
            addLeftAlignedCell(table, sectionLabel, bg);

            String fileName = docDto != null && docDto.getFileName() != null
                    ? docDto.getFileName()
                    : "-";
            addLeftAlignedCell(table, fileName, bg);

            alt = !alt;
        }
        return alt;
    }

    private String formatSpecRange(SpecifiedDimension sd) {
        if (sd == null) {
            return "-";
        }
        String min = sd.getMinValue() != null ? String.valueOf(sd.getMinValue()) : "";
        String max = sd.getMaxValue() != null ? String.valueOf(sd.getMaxValue()) : "";
        String unit = sd.getUnit() != null ? " " + sd.getUnit() : "";
        String source = sd.getSource() != null ? " (" + sd.getSource() + ")" : "";

        if (min.isEmpty() && max.isEmpty()) {
            return "-";
        }
        return min + " - " + max + unit + source;
    }

    private String dashIfBlank(Object value) {
        if (value == null || value.toString().isBlank()
                || "N/A".equalsIgnoreCase(value.toString().trim())) {
            return "-";
        }
        return str(value);
    }

    private void addIdCardCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(BORDER_LIGHT);
        cell.setPadding(6);
        cell.setBackgroundColor(WHITE);

        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n", LABEL_FONT));
        p.add(new Chunk(value != null ? value : "-", BODY_BOLD));
        cell.addElement(p);
        table.addCell(cell);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // ═════════════════════════════════════════════════════════════
    // RAW MATERIAL PREPARATION
    // ═════════════════════════════════════════════════════════════
    private void renderRawMaterialPreparation(Document doc, PreparationFormDetailsResponse data)
            throws DocumentException {

        // ------------------------------------------------------------------
        // 1. FORM SUMMARY
        // ------------------------------------------------------------------
        addSection(doc, "Raw Material Preparation", "");

        PdfPTable summary = createTable(new float[]{25, 25, 25, 25});
        addInfoStripCell(summary, "Batch ID", str(data.getBatchId()));
        addInfoStripCell(summary, "Submitted by", workflowStatusLabel(data.getSubmittedBy().getFullName()));
        addInfoStripCell(summary, "Approved Premixes",
                data.getApprovedPremixCount() + " / " + data.getTotalPremixCount());
        addInfoStripCell(summary, "Pending / Rejected",
                str(data.getPendingPremixCount()) + " / " + str(data.getRejectedPremixCount()));

        // Status (coloured)
        PdfPCell statusCell = new PdfPCell();
        statusCell.setColspan(4);
        statusCell.setBorderColor(BORDER_LIGHT);
        statusCell.setPadding(6);
        statusCell.setBackgroundColor(ROW_ALT);
        Paragraph sp = new Paragraph();
        sp.add(new Chunk("Form Status: ", BODY_BOLD));
        sp.add(new Chunk(
                workflowStatusLabel(data.getStatus()),
                new Font(Font.HELVETICA, 8, Font.BOLD,
                        getStatusColor(str(data.getStatus())))));
        statusCell.addElement(sp);
        summary.addCell(statusCell);

        doc.add(summary);
        spacer(doc, 12);

        PreparationDetailsRequest details = data.getPreparationDetails();
        if (details == null) {
            doc.add(new Paragraph("No preparation details available.", BODY_FONT));
            return;
        }

        // ------------------------------------------------------------------
        // 2. PREMIXES
        // ------------------------------------------------------------------
        if (details.getPremixes() != null) {
            for (PremixRequest premix : details.getPremixes()) {

                PremixStatusResponse premixStatus = findPremixStatus(
                        data.getPremixStatuses(), premix.getPremixNo());
                Object premixApprovalStatus = premixStatus != null
                        ? premixStatus.getPremixSubmissionStatus()
                        : null;

                // Simple Premix header + coloured status
                Paragraph premixTitle = new Paragraph();
                premixTitle.add(new Chunk("Premix " + premix.getPremixNo() + "   ", SECTION_FONT));
                String premixStatusValue = premixApprovalStatus != null
                        ? premixApprovalStatus.toString()
                        : "-";
                premixTitle.add(new Chunk(
                        workflowStatusLabel(premixStatusValue),
                        new Font(Font.HELVETICA, 8, Font.BOLD,
                                getStatusColor(premixStatusValue))));
                premixTitle.setSpacingBefore(10);
                premixTitle.setSpacingAfter(6);
                doc.add(premixTitle);

                // Solid process
                renderProcessMaterials(doc, premix.getPremixNo(), "Solid", premix.getSolidProcess());

                // Liquid process
                renderProcessMaterials(doc, premix.getPremixNo(), "Liquid", premix.getLiquidProcess());
            }
        }

        // ------------------------------------------------------------------
        // 3. WEIGHMENT SHEET
        // ------------------------------------------------------------------
        WeightmentSheetRequest ws = details.getWeightmentSheet();
        if (ws != null) {
            addSection(doc, "Weighment Sheet for Dispatch of Material to Mixing Station", "");

            PdfPTable info = createKeyValueTable();
            addKV(info, "Mixer Building Number", str(ws.getMixerBuildingNumber()));
            doc.add(info);
            spacer(doc, 6);

            if (ws.getWeightmentDetails() != null && !ws.getWeightmentDetails().isEmpty()) {
                PdfPTable table = createTable(new float[]{14, 22, 12, 12, 12, 14, 14});
                addHeaders(table,
                        "Material Code",
                        "Material Name",
                        "Container Type",
                        "Container Number",
                        "Weigh Scale Number",
                        "Weight Transferred (kg)",
                        "Weighing Date & Time");

                boolean alt = false;
                for (WeightmentDetailRequest w : ws.getWeightmentDetails()) {
                    Color bg = alt ? ROW_ALT : WHITE;

                    // Material Code + Grade Code
                    String codeDisplay = str(w.getMaterialCode());
                    if (w.getGradeCode() != null && !w.getGradeCode().isBlank()) {
                        codeDisplay = codeDisplay + " - " + w.getGradeCode();
                    }
                    addCell(table, codeDisplay, BODY_FONT, bg);

                    // Material Name + Grade Name
                    String nameDisplay = str(w.getMaterialName());
                    if (w.getGradeName() != null && !w.getGradeName().isBlank()) {
                        nameDisplay = w.getGradeName() + " (" + nameDisplay + ")";
                    } else if (w.getGradeCode() != null && !w.getGradeCode().isBlank()) {
                        nameDisplay = w.getGradeCode() + " (" + nameDisplay + ")";
                    }
                    addCell(table, nameDisplay, BODY_FONT, bg);

                    addCell(table, str(w.getContainerType()), BODY_FONT, bg);
                    addCell(table, str(w.getContainerNumber()), BODY_FONT, bg);
                    addCell(table, str(w.getWeighScaleNumber()), BODY_FONT, bg);
                    addCell(table, qcRangeValue(w.getWeightTransferred()), BODY_FONT, bg);
                    addCell(table, formatPdfDateTimeValue(w.getWeighingDateTime()), BODY_FONT, bg);

                    alt = !alt;
                }
                doc.add(table);
            }

            if (ws.getValidation() != null) {
                spacer(doc, 6);
                PdfPTable valTable = createKeyValueTable();
                addKV(valTable, "Compare with identification sheet",
                        yesNo(ws.getValidation().isCompareWithIdentificationSheet()));
                addKV(valTable, "Deviation found",
                        yesNo(ws.getValidation().isDeviationFound()));
                if (ws.getValidation().getDeviationMessage() != null
                        && !ws.getValidation().getDeviationMessage().isBlank()) {
                    addKV(valTable, "Deviation Message",
                            ws.getValidation().getDeviationMessage());
                }
                doc.add(valTable);
            }
        }
    }

    // ------------------------------------------------------------------
    // Process materials (Solid / Liquid) – generic, works for any material
    // ------------------------------------------------------------------
    private void renderProcessMaterials(
            Document doc,
            int premixNo,
            String processType,
            List<MaterialProcessRequest> processes) throws DocumentException {

        if (processes == null || processes.isEmpty()) return;

        for (MaterialProcessRequest material : processes) {

            // Material header
            // Material header with grade
            StringBuilder matLabel = new StringBuilder();
            matLabel.append(processType)
                    .append("  ·  ")
                    .append(str(material.getMaterialCode()));

            if (material.getGradeCode() != null && !material.getGradeCode().isBlank()) {
                matLabel.append(" - ").append(material.getGradeCode());
            }

            if (material.getMaterialName() != null && !material.getMaterialName().isBlank()) {
                matLabel.append("  (").append(material.getMaterialName()).append(")");
            }

            Paragraph matHeader = new Paragraph(matLabel.toString(), SECTION_FONT);
            matHeader.setSpacingBefore(8);
            matHeader.setSpacingAfter(4);
            doc.add(matHeader);

            // ---- Lot Details ----
            if (material.getLotDetails() != null && !material.getLotDetails().isEmpty()) {
                addSection(doc, "Lot Details", "");
                PdfPTable lotTable = createTable(new float[]{50, 50});
                addHeaders(lotTable, "Quantity", "Lot ID");
                boolean alt = false;
                for (LotDetailDto lot : material.getLotDetails()) {
                    Color bg = alt ? ROW_ALT : WHITE;
                    addCell(lotTable,
                            lot.getQuantity() != null ? String.valueOf(lot.getQuantity()) : "",
                            BODY_FONT, bg);
                    addCell(lotTable, str(lot.getLotId()), BODY_FONT, bg);
                    alt = !alt;
                }
                doc.add(lotTable);
                spacer(doc, 6);
            }

            // ---- Equipment (generic) ----
            if (material.getApCoarse() != null && material.getApCoarse().getEquipmentType() != null) {
                addSection(doc, "Equipment", "");
                PdfPTable eq = createKeyValueTable();
                addKV(eq, "Equipment Type", str(material.getApCoarse().getEquipmentType()));
                doc.add(eq);
                spacer(doc, 4);
            }

            // ---- Blending / Drying Parameters ----
            List<OperationParameterRowDto> blendingParams = null;
            if (material.getApCoarse() != null) {
                blendingParams = material.getApCoarse().getBlendingDryingParameters();
            } else if (material.getApFine() != null) {
                blendingParams = material.getApFine().getBlendingDryingParameters();
            }
            if (blendingParams != null && !blendingParams.isEmpty()) {
                addSection(doc, "Blending / Drying Parameters", "");
                PdfPTable table = createTable(new float[]{40, 35, 25});
                addHeaders(table, "Operation", "Set Parameter", "Actual Parameter");
                boolean alt = false;
                for (OperationParameterRowDto row : blendingParams) {
                    Color bg = alt ? ROW_ALT : WHITE;
                    addCell(table, str(row.getOperation()), BODY_FONT, bg);
                    addCell(table, str(row.getSetParameter()), BODY_FONT, bg);
                    addCell(table, str(row.getActualParameter()), BODY_FONT, bg);
                    alt = !alt;
                }
                doc.add(table);
                spacer(doc, 6);
            }

            // ---- Drying Operation (RVD) ----
            List<TimedOperationParameterRowDto> dryingOps = null;
            if (material.getApCoarse() != null) {
                dryingOps = material.getApCoarse().getDryingOperationRvd();
            } else if (material.getApFine() != null) {
                dryingOps = material.getApFine().getDryingOperationRvd();
            }
            if (dryingOps != null && !dryingOps.isEmpty()) {
                addSection(doc, "Drying Operation (RVD)", "");
                PdfPTable table = createTable(new float[]{22, 28, 18, 16, 16});
                addHeaders(table, "Operation", "Set Parameter", "Actual Parameter",
                        "Start Date & Time", "End Date & Time");
                boolean alt = false;
                for (TimedOperationParameterRowDto row : dryingOps) {
                    Color bg = alt ? ROW_ALT : WHITE;
                    addCell(table, str(row.getOperation()), BODY_FONT, bg);
                    addCell(table, str(row.getSetParameter()), BODY_FONT, bg);
                    addCell(table, str(row.getActualParameter()), BODY_FONT, bg);
                    addCell(table, str(row.getStartDateTime()), BODY_FONT, bg);
                    addCell(table, str(row.getEndDateTime()), BODY_FONT, bg);
                    alt = !alt;
                }
                doc.add(table);
                spacer(doc, 6);
            }

            // ---- Particle Size Distribution ----
            List<ParticleSizeRowDto> psd = null;
            if (material.getApCoarse() != null) {
                psd = material.getApCoarse().getParticleSizeDistribution();
            } else if (material.getApFine() != null) {
                psd = material.getApFine().getParticleSizeDistribution();
            }
            if (psd != null && !psd.isEmpty()) {
                addSection(doc, "Particle Size Distribution", "");
                PdfPTable table = createTable(new float[]{45, 30, 25});
                addHeaders(table, "PSD Requirement", "Specification", "Result");
                boolean alt = false;
                for (ParticleSizeRowDto row : psd) {
                    Color bg = alt ? ROW_ALT : WHITE;
                    addCell(table, str(row.getPsdRequirement()), BODY_FONT, bg);
                    addCell(table, str(row.getSpecification()), BODY_FONT, bg);
                    addCell(table, str(row.getResult()), BODY_FONT, bg);
                    alt = !alt;
                }
                doc.add(table);
                spacer(doc, 6);
            }

            // ---- Generic Drying / Tray Oven (used by multiple materials) ----
            DryingTrayOvenDto drying = material.getDrying();
            if (drying == null && material.getApFine() != null) {
                drying = material.getApFine().getTrayOvenStorage();
            }
            if (drying == null && material.getApUltraFine() != null) {
                drying = material.getApUltraFine().getDrying();
            }
            if (drying != null) {
                addSection(doc, "Drying", "");
                PdfPTable t = createTable(new float[]{12, 12, 14, 17, 17, 10, 18});
                addHeaders(t, "Oven Type", "Oven Number", "Set Temperature",
                        "Start Date & Time", "End Date & Time", "Moisture", "Observation");
                addCell(t, str(drying.getOvenType()), BODY_FONT, WHITE);
                addCell(t, str(drying.getOvenNumber()), BODY_FONT, WHITE);
                addCell(t, str(drying.getOvenSetTemperature()), BODY_FONT, WHITE);
                addCell(t, str(drying.getStartDatetime()), BODY_FONT, WHITE);
                addCell(t, str(drying.getEndDatetime()), BODY_FONT, WHITE);
                addCell(t, str(drying.getMoisture()), BODY_FONT, WHITE);
                addCell(t, str(drying.getObservation()), BODY_FONT, WHITE);
                doc.add(t);
                spacer(doc, 6);
            }

            // ---- Sieving ----
            SievingDto sieving = material.getSieving();
            if (sieving == null && material.getApFine() != null) {
                sieving = material.getApFine().getSieving();
            }
            if (sieving == null && material.getApUltraFine() != null) {
                sieving = material.getApUltraFine().getSieving();
            }
            if (sieving != null) {
                addSection(doc, "Sieving", "");
                PdfPTable t = createTable(new float[]{26, 22, 20, 32});
                addHeaders(t, "Dispatch Date & Time", "Sieved Quantity",
                        "Sieve Mesh Size", "Observation");
                addCell(t, str(sieving.getSievingDispatchDatetime()), BODY_FONT, WHITE);
                addCell(t, str(sieving.getSievedQuantity()), BODY_FONT, WHITE);
                addCell(t, str(sieving.getSieveMeshSize()), BODY_FONT, WHITE);
                addCell(t, str(sieving.getObservation()), BODY_FONT, WHITE);
                doc.add(t);
                spacer(doc, 6);
            }

            // ---- Grinding (Fine / Ultra Fine) ----
            if (material.getApFine() != null) {
                ApFineProcessDto ap = material.getApFine();
                addSection(doc, "Grinding Parameters", "");
                PdfPTable t = createKeyValueTable();
                addKV(t, "ACM Equipment Id", str(ap.getAcmEquipmentId()));
                addKV(t, "Mill RPM", str(ap.getMillRpm()));
                addKV(t, "Classifier RPM", str(ap.getClassifierRpm()));
                addKV(t, "Screw Feeder RPM", str(ap.getScrewFeederRpm()));
                addKV(t, "ID Fan RPM", str(ap.getIdFanRpm()));
                addKV(t, "Set Pressure", str(ap.getSetPressure()));
                addKV(t, "Start Date & Time", str(ap.getGrindingStartDatetime()));
                addKV(t, "End Date & Time", str(ap.getGrindingEndDatetime()));
                addKV(t, "Observation", str(ap.getGrindingObservation()));
                addKV(t, "Qty. (kg) qualified", str(ap.getQtyKgQualified()));
                doc.add(t);
                spacer(doc, 6);
            }

            if (material.getApUltraFine() != null) {
                ApUltraFineProcessDto ap = material.getApUltraFine();
                addSection(doc, "Grinding Parameters", "");
                PdfPTable t = createKeyValueTable();
                addKV(t, "Equipment Id", str(ap.getEquipmentId()));
                addKV(t, "Screw Feeder RPM", str(ap.getScrewFeederRpm()));
                addKV(t, "Feed Pressure", str(ap.getFeedPressure()));
                addKV(t, "Grinding Pressure", str(ap.getGrindingPressure()));
                addKV(t, "Start Date & Time", str(ap.getGrindingStartDatetime()));
                addKV(t, "End Date & Time", str(ap.getGrindingEndDatetime()));
                addKV(t, "Observation", str(ap.getGrindingObservation()));
                addKV(t, "Particle Size (6±1μm)", str(ap.getParticleSizeResult()));
                addKV(t, "Qty. (kg) qualified", str(ap.getQtyKgQualified()));
                doc.add(t);
                spacer(doc, 6);
            }

            // ---- Aluminum ----
            if (material.getAluminum() != null) {
                AluminumProcessDto al = material.getAluminum();
                addSection(doc, "Aluminum Powder Processing", "");
                PdfPTable t = createTable(new float[]{13, 10, 15, 15, 20, 12, 15});
                addCenteredHeaders(t,
                        "EQUIPMENT ID",
                        "SET RPM",
                        "START DATE & TIME",
                        "END DATE & TIME",
                        "OBSERVATION",
                        "QTY. (KG) QUALIFIED",
                        "DATE & TIME OF DISPATCH");
                addCenteredCell(t, str(al.getEquipmentId()), WHITE);
                addCenteredCell(t, str(al.getSetRpm()), WHITE);
                addCenteredCell(t, str(al.getStartDatetime()), WHITE);
                addCenteredCell(t, str(al.getEndDatetime()), WHITE);
                addCenteredCell(t, str(al.getObservation()), WHITE);
                addCenteredCell(t, str(al.getQtyKgQualified()), WHITE);
                addCenteredCell(t, str(al.getDispatchDatetime()), WHITE);
                doc.add(t);
                spacer(doc, 6);
            }

            // ---- DOA / HTPB / TDI / CC / IO / NONOX-D ----
            if (material.getDoa() != null) {
                DoaProcessDto d = material.getDoa();
                String processTypeStr = String.valueOf(material.getProcessType()).trim().toUpperCase(Locale.ROOT);
                String sectionTitle = switch (processTypeStr) {
                    case "HTPB" -> "HTPB";
                    case "TDI" -> "TDI";
                    case "CC" -> "Copper Chromite (CC)";
                    case "IO" -> "Iron Oxide (IO)";
                    case "NONOX_D", "NONOXD" -> "NONOX-D";
                    default -> "DOA";
                };
                addSection(doc, sectionTitle, "");
                PdfPTable t = createKeyValueTable();
                if ("CC".equals(processTypeStr) || "IO".equals(processTypeStr)) {
                    addKV(t, "Sieving date and time", str(d.getSievingDatetime()));
                }
                if ("NONOX_D".equals(processTypeStr) || "NONOXD".equals(processTypeStr)) {
                    addKV(t, "Quantity sieved (kg)", str(d.getQuantitySieved()));
                    addKV(t, "Sieve Mesh Size", str(d.getSieveMeshSize()));
                }
                addKV(t, "Date & Time of dispatch", str(d.getDispatchDatetime()));
                addKV(t, "Observation", str(d.getObservation()));
                addKV(t, "Total Quantity sent for premix", str(d.getTotalQtySentForPremix()));
                doc.add(t);
                spacer(doc, 6);
            }

            spacer(doc, 10);
        }
    }

    @SuppressWarnings("unchecked")
    private void renderCasePreparation(Document doc, CasePreparationResponse data)
            throws DocumentException {

        // ------------------------------------------------------------------
        // 1. FORM SUMMARY
        // ------------------------------------------------------------------
        addSection(doc, "Case Preparation", "");

        PdfPTable summary = createTable(new float[]{25, 25, 25, 25});
        addInfoStripCell(summary, "Batch ID", str(data.getBatchId()));
        addInfoStripCell(summary, "Submitted by",
                data.getSubmittedBy() != null ? str(data.getSubmittedBy().getFullName()) : "-");
        addInfoStripCell(summary, "Approved Motors",
                data.getApprovedMotorCount() + " / " + data.getTotalMotorCount());
        addInfoStripCell(summary, "Pending / Rejected",
                str(data.getPendingMotorCount()) + " / " + str(data.getRejectedMotorCount()));

        // Status (coloured)
        PdfPCell statusCell = new PdfPCell();
        statusCell.setColspan(4);
        statusCell.setBorderColor(BORDER_LIGHT);
        statusCell.setPadding(6);
        statusCell.setBackgroundColor(ROW_ALT);
        Paragraph sp = new Paragraph();
        sp.add(new Chunk("Form Status: ", BODY_BOLD));
        sp.add(new Chunk(
                workflowStatusLabel(data.getStatus()),
                new Font(Font.HELVETICA, 8, Font.BOLD,
                        getStatusColor(str(data.getStatus())))));
        statusCell.addElement(sp);
        summary.addCell(statusCell);

        doc.add(summary);
        spacer(doc, 12);

        CasePreparationDetailsRequest details = data.getCasePreparationDetails();
        if (details == null || details.getMotors() == null || details.getMotors().isEmpty()) {
            doc.add(new Paragraph("No case preparation details available.", BODY_FONT));
            return;
        }

        // ------------------------------------------------------------------
        // 2. MOTORS
        // ------------------------------------------------------------------
        for (CasePreparationMotorRequest motor : details.getMotors()) {

            MotorStatusResponse savedMotorStatus = findMotorStatus(
                    data.getMotorStatuses(), motor.getMotorId());
            Object motorApprovalStatus = savedMotorStatus != null
                    ? savedMotorStatus.getMotorSubmissionStatus()
                    : motor.getMotorSubmissionStatus();

            // Motor header + coloured status
            Paragraph motorTitle = new Paragraph();
            motorTitle.add(new Chunk("Motor · " + str(motor.getMotorId()) + "   ", SECTION_FONT));
            motorTitle.add(createStatusPhrase(
                    workflowStatusLabel(motorApprovalStatus)));
            motorTitle.setSpacingBefore(10);
            motorTitle.setSpacingAfter(6);
            doc.add(motorTitle);

            // Quick meta
            PdfPTable meta = createKeyValueTable();
            addKV(meta, "PRRC Clearance Date",
                    motor.getPrrcClearanceDate() != null
                            ? motor.getPrrcClearanceDate().format(PDF_DATE_FORMAT)
                            : "-");
            doc.add(meta);
            spacer(doc, 8);

            // Sections
            renderCasePreparationSection(doc, "Abrading Operation", motor.getAbradingOperation());
            addCasePreparationTotalDust(doc, motor.getAbradingOperation());
            renderCasePreparationSection(doc, "Bellow Bonding", motor.getBellowBonding());
            renderCasePreparationSection(doc, "TCE Cleaning", motor.getTceCleaning());
            renderCasePreparationSection(doc, "Pre-Heating", motor.getPreHeating());
            renderCasePreparationSection(doc, "Liner Coating Operation", motor.getLinerCoatingOperation());
            renderCasePreparationSection(doc, "Dispatch To Casting", motor.getDispatchToCasting());

            spacer(doc, 12);
        }
    }

    @SuppressWarnings("unchecked")
    private void renderCasePreparationSection(
            Document doc,
            String title,
            Object sectionData) throws DocumentException {

        if (sectionData == null) return;

        addSection(doc, title, "");

        Map<String, Object> row = mapper.convertValue(
                sectionData, new TypeReference<Map<String, Object>>() {
                });

        Map<String, Object> flatFields = new LinkedHashMap<>();
        Map<String, List<Map<String, Object>>> nestedTables = new LinkedHashMap<>();

        // Flatten the DTO's table members into a shared shape for the PDF tables below.
        normalizeCasePreparationRows(row);

        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String key = entry.getKey();
            if ("srNo".equals(key) || "type".equals(key) || "fieldType".equals(key)) continue;

            Object value = entry.getValue();

            if ("rh".equals(key)) continue;

            if ("testReport".equals(key)) {
                flatFields.put(key, casePreparationDocumentName(value));
                continue;
            }

            if (value instanceof Map<?, ?> nestedMap) {
                Object rows = nestedMap.get("rows");
                if (rows instanceof List<?>) {
                    nestedTables.put(key, (List<Map<String, Object>>) rows);
                } else {
                    // treat nested map as flat fields
                    for (Map.Entry<?, ?> ne : nestedMap.entrySet()) {
                        flatFields.put(key + "." + ne.getKey(), ne.getValue());
                    }
                }
            } else if (value instanceof List<?>) {
                nestedTables.put(key, (List<Map<String, Object>>) value);
            } else {
                flatFields.put(key, value);
            }
        }

        // ---- Flat key-value fields ----
        if (!flatFields.isEmpty()) {
            PdfPTable flatTable = createKeyValueTable();
            for (Map.Entry<String, Object> field : flatFields.entrySet()) {
                addKV(flatTable, casePreparationFieldLabel(field.getKey()), str(field.getValue()));
            }
            doc.add(flatTable);
            spacer(doc, 6);
        }

        // ---- Nested tables ----
        for (Map.Entry<String, List<Map<String, Object>>> tableEntry : nestedTables.entrySet()) {
            List<Map<String, Object>> rows = tableEntry.getValue();
            if (rows == null || rows.isEmpty()) continue;

            Paragraph heading = new Paragraph(prettyText(tableEntry.getKey()), SECTION_FONT);
            heading.setSpacingBefore(6);
            heading.setSpacingAfter(4);
            doc.add(heading);

            Map<String, Object> first = rows.get(0);

            if (first.containsKey("startDateTime")
                    && first.containsKey("endDateTime")
                    && first.containsKey("dustQty")) {
                renderCasePreparationCutRows(doc, rows, tableEntry.getKey());
            } else if (first.containsKey("specification")) {
                // Qualification-style table
                PdfPTable pdf = createTable(new float[]{8, 35, 27, 30});
                addHeaders(pdf, "Sr. No.", "Parameter", "Specification", "Result");
                boolean alt = false;
                for (Map<String, Object> r : rows) {
                    Color bg = alt ? ROW_ALT : WHITE;
                    addCell(pdf, str(r.get("srNo")), BODY_FONT, bg);
                    addCell(pdf, str(r.get("parameter")), BODY_FONT, bg);
                    addCell(pdf, str(r.get("specification")), BODY_FONT, bg);
                    addCell(pdf, str(r.get("result")), BODY_BOLD, bg);
                    alt = !alt;
                }
                doc.add(pdf);

            } else if (first.containsKey("ingredient") || first.containsKey("mfgLot")) {
                // Ingredients table (Premix / Final Mix)
                PdfPTable pdf = createTable(new float[]{8, 22, 20, 15, 15, 20});
                addHeaders(pdf, "Sr. No.", "Ingredient", "Mfg Lot", "Parts by Wt.", "Qty Taken", "Total Qty");
                boolean alt = false;
                for (Map<String, Object> r : rows) {
                    Color bg = alt ? ROW_ALT : WHITE;
                    addCell(pdf, str(r.get("srNo")), BODY_FONT, bg);
                    addCell(pdf, str(r.get("ingredient")), BODY_FONT, bg);
                    addCell(pdf, str(r.get("mfgLot")), BODY_FONT, bg);
                    addCell(pdf, str(r.get("partsByWt")), BODY_FONT, bg);
                    addCell(pdf, str(r.get("qtyTaken")), BODY_FONT, bg);
                    addCell(pdf, str(r.get("totalQty")), BODY_FONT, bg);
                    alt = !alt;
                }
                doc.add(pdf);

            } else if (first.containsKey("operation") || first.containsKey("parameter")) {
                // Generic operation / parameter table
                boolean hasAttachments = first.containsKey("attachments") || first.containsKey("files");
                boolean hasRemarks = first.containsKey("remarks") || first.containsKey("remarksObservations");

                float[] widths = hasAttachments
                        ? new float[]{30, 25, 25, 20}
                        : new float[]{40, 30, 30};

                PdfPTable pdf = createTable(widths);

                if (hasAttachments) {
                    addHeaders(pdf, "Operation / Parameter", "Value", "Remarks", "Attachments");
                } else {
                    addHeaders(pdf, "Operation / Parameter",
                            first.containsKey("observations") ? "Observation" : "Value", "Remarks");
                }

                boolean alt = false;
                for (Map<String, Object> r : rows) {
                    Color bg = alt ? ROW_ALT : WHITE;

                    String label = r.containsKey("operation")
                            ? str(r.get("operation"))
                            : str(r.get("parameter"));
                    if ("Dispatch Time".equalsIgnoreCase(label.trim())) {
                        label = "Dispatch Date & Time";
                    }

                    String value = r.containsKey("value") ? str(r.get("value"))
                            : (r.containsKey("observations") ? str(r.get("observations")) : defObs(r));
                    if (r.get("unit") != null) {
                        value += " " + r.get("unit");
                    }

                    String remarks = r.containsKey("remarksObservations")
                            ? str(r.get("remarksObservations"))
                            : str(r.get("remarks"));

                    addCell(pdf, label, BODY_FONT, bg);
                    addCell(pdf, value, BODY_FONT, bg);
                    addCell(pdf, remarks, BODY_FONT, bg);

                    if (hasAttachments) {
                        String attachments = r.containsKey("attachments")
                                ? str(r.get("attachments"))
                                : defFiles(r);
                        addCell(pdf, attachments, BODY_FONT, bg);
                    }
                    alt = !alt;
                }
                doc.add(pdf);

            } else {
                // Fallback – dynamic columns
                List<String> headers = new ArrayList<>(first.keySet());
                float[] widths = new float[headers.size()];
                java.util.Arrays.fill(widths, 1f);
                PdfPTable pdf = createTable(widths);
                addHeaders(pdf, headers.stream().map(this::prettyText).toArray(String[]::new));

                boolean alt = false;
                for (Map<String, Object> r : rows) {
                    Color bg = alt ? ROW_ALT : WHITE;
                    for (String header : headers) {
                        addCell(pdf, str(r.get(header)), BODY_FONT, bg);
                    }
                    alt = !alt;
                }
                doc.add(pdf);
            }
            spacer(doc, 6);
        }
    }

    private void addCasePreparationTotalDust(Document doc, Object abradingOperation)
            throws DocumentException {
        if (abradingOperation == null) return;
        Map<String, Object> abrading = mapper.convertValue(abradingOperation,
                new TypeReference<Map<String, Object>>() {
                });
        double total = 0;
        boolean found = false;
        for (String key : List.of("firstCut", "secondCut")) {
            Object rawRows = abrading.get(key);
            if (!(rawRows instanceof List<?> rows)) continue;
            for (Object rawRow : rows) {
                Map<String, Object> cut = mapper.convertValue(rawRow,
                        new TypeReference<Map<String, Object>>() {
                        });
                Object dust = cut.get("dustQty");
                if (dust != null) {
                    try {
                        total += Double.parseDouble(dust.toString());
                        found = true;
                    } catch (NumberFormatException ignored) {
                        // Ignore non-numeric dust entries; retain the valid recorded values.
                    }
                }
            }
        }
        if (found) {
            PdfPTable totalTable = createKeyValueTable();
            addKV(totalTable, "Total Dust Weight (in gm) (A+B)", formatPdfNumber(total));
            doc.add(totalTable);
            spacer(doc, 6);
        }
    }

    private String formatPdfNumber(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }

    private void normalizeCasePreparationRows(Map<String, Object> section) {
        for (String tableName : List.of("premixIngredients", "finalMixIngredients")) {
            Object raw = section.get(tableName);
            if (!(raw instanceof List<?> rows)) continue;
            List<Map<String, Object>> normalized = new ArrayList<>();
            for (Object item : rows) {
                Map<String, Object> source = mapper.convertValue(item,
                        new TypeReference<Map<String, Object>>() {
                        });
                Map<String, Object> target = new LinkedHashMap<>(source);
                target.put("ingredient", firstNonNull(source.get("ingredient"), source.get("materialName")));
                target.put("partsByWt", firstNonNull(source.get("partsByWt"), source.get("partsByWeight")));
                target.put("qtyTaken", firstNonNull(source.get("qtyTaken"), source.get("quantityTaken")));
                target.put("totalQty", firstNonNull(source.get("totalQty"), source.get("totalQuantity")));
                normalized.add(target);
            }
            section.put(tableName, normalized);
        }
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private String casePreparationFieldLabel(String field) {
        return switch (field) {
            case "testReport" -> "Test Report";
            case "dispatchTime", "dispatchDateTime" -> "Dispatch Date & Time";
            default -> prettyText(field);
        };
    }

    private String casePreparationDocumentName(Object value) {
        if (value instanceof DocumentDto document) return documentReference(document);
        if (value instanceof Map<?, ?> map) {
            Object name = map.get("fileName");
            if (name != null && !name.toString().isBlank()) return name.toString();
            Object id = map.get("fileId");
            return id == null ? "—" : id.toString();
        }
        return str(value);
    }

    private void renderCasePreparationCutRows(
            Document doc,
            List<Map<String, Object>> rows,
            String cutName) throws DocumentException {
        String dustWeightLabel = "Dust Weight (in gm) "
                + (cutName.toLowerCase(Locale.ROOT).contains("second") ? "(B)" : "(A)");
        PdfPTable pdf = createTable(new float[]{8, 21, 21, 20, 12, 18});
        addHeaders(pdf, "Sr. No.", "Start Date & Time", "End Date & Time",
                dustWeightLabel, "Observations", "Photos / Videos");

        boolean alternate = false;
        for (Map<String, Object> row : rows) {
            Color background = alternate ? ROW_ALT : WHITE;
            addCell(pdf, str(row.get("srNo")), BODY_FONT, background);
            addCell(pdf, formatPdfDateTimeValue(row.get("startDateTime")),
                    BODY_FONT, background);
            addCell(pdf, formatPdfDateTimeValue(row.get("endDateTime")),
                    BODY_FONT, background);
            addCell(pdf, str(row.get("dustQty")), BODY_FONT, background);
            addCell(pdf, str(row.get("observations")), BODY_FONT, background);
            addCell(pdf, casePreparationAttachments(row.get("attachments")),
                    BODY_FONT, background);
            alternate = !alternate;
        }
        doc.add(pdf);
    }

    private String casePreparationAttachments(Object value) {
        if (!(value instanceof List<?> attachments) || attachments.isEmpty()) {
            return "No photos/videos uploaded";
        }
        List<String> references = new ArrayList<>();
        for (Object attachment : attachments) {
            if (attachment instanceof DocumentDto document) {
                references.add(documentReference(document));
            } else if (attachment instanceof Map<?, ?> map) {
                Object name = map.get("fileName");
                Object id = map.get("fileId");
                Object reference = name != null && !name.toString().isBlank() ? name : id;
                if (reference != null && !reference.toString().isBlank()) {
                    references.add(reference.toString());
                }
            } else if (attachment != null && !attachment.toString().isBlank()) {
                references.add(attachment.toString());
            }
        }
        return references.isEmpty()
                ? "No photos/videos uploaded"
                : String.join(", ", references);
    }

    private void renderMixing(Document doc, MixingFormResponse data) throws DocumentException {

        if (data == null || data.getMixingDetails() == null) {
            doc.add(new Paragraph("No mixing process details available.", BODY_FONT));
            return;
        }

        // ------------------------------------------------------------------
        // 1. FORM SUMMARY
        // ------------------------------------------------------------------
        addSection(doc, "Mixing", "");

        PdfPTable summary = createTable(new float[]{25, 25, 25, 25});
        addInfoStripCell(summary, "Batch ID", str(data.getBatchId()));
        addInfoStripCell(summary, "Batch Type", str(data.getBatchType()));
        addInfoStripCell(summary, "Submitted By",
                data.getSubmittedBy() != null ? str(data.getSubmittedBy().getFullName()) : "-");
        addInfoStripCell(summary, "Submitted On", str(data.getSubmittedAt()));

        // Status
        PdfPCell statusCell = new PdfPCell();
        statusCell.setColspan(4);
        statusCell.setBorderColor(BORDER_LIGHT);
        statusCell.setPadding(6);
        statusCell.setBackgroundColor(ROW_ALT);
        Paragraph sp = new Paragraph();
        sp.add(new Chunk("Form Status: ", BODY_BOLD));
        sp.add(createStatusPhrase(data.getStatus() != null ? data.getStatus().toString() : "-"));
        statusCell.addElement(sp);
        summary.addCell(statusCell);

        doc.add(summary);
        spacer(doc, 12);

        MixingDetailsDto details = data.getMixingDetails();
        var stages = details.getStages();
        if (stages == null || stages.isEmpty()) {
            doc.add(new Paragraph("No mixing stage sequences logged.", BODY_FONT));
            return;
        }

        // ------------------------------------------------------------------
        // 2. STAGES (PREMIX / FINAL_MIX)
        // ------------------------------------------------------------------
        for (var stage : stages) {
            String stageType = str(stage.getStageType());
            boolean isPremix = "PREMIX".equalsIgnoreCase(stageType);
            String stageLabel = isPremix ? "Premix Stage" : "Final Mix Stage";

            addSection(doc, stageLabel, "");

            var premixes = stage.getPremixes();
            if (premixes == null || premixes.isEmpty()) continue;

            for (var premix : premixes) {

                String subLabel = isPremix ? "Premix" : "Final Mix";
                String premixNo = premix.getPremixNo() != null ? premix.getPremixNo().toString() : "-";

                List<PremixStatusResponse> stageStatuses = isPremix
                        ? data.getPremixStatuses()
                        : data.getFinalMixStatuses();

                PremixStatusResponse savedStatus = premix.getPremixNo() != null
                        ? findPremixStatus(stageStatuses, premix.getPremixNo())
                        : null;

                Object approvalStatus = savedStatus != null
                        ? savedStatus.getPremixSubmissionStatus()
                        : premix.getPremixSubmissionStatus();

                // Header with coloured status
                Paragraph title = new Paragraph();
                title.add(new Chunk(subLabel + " " + premixNo + "   ", SECTION_FONT));
                title.add(createStatusPhrase(
                        approvalStatus != null ? approvalStatus.toString() : "-"));
                title.setSpacingBefore(8);
                title.setSpacingAfter(6);
                doc.add(title);

                // ---- Configuration strip ----
                var config = premix.getMixerConfiguration();
                var mix = premix.getMixDetails();

                String mixCycle = "";
                String finalMixCycle = "";
                if (premix.getMixingCycle() != null) {
                    String cycleName = premix.getMixingCycle().getMixingCycleName();
                    String cycleCode = premix.getMixingCycle().getMixingCycleCode();
                    mixCycle = cycleName != null && !cycleName.isBlank()
                            ? cycleName
                            : str(cycleCode);
                    finalMixCycle = cycleName != null && !cycleName.isBlank()
                            && cycleCode != null && !cycleCode.isBlank()
                            ? cycleName + " (" + cycleCode + ")"
                            : mixCycle;
                }
                PdfPTable configTable = createTable(new float[]{25, 25, 25, 25});
                if (isPremix) {
                    addInfoStripCell(configTable, "Mixer", config != null ? str(config.getMixerId()) : "-");
                    addInfoStripCell(configTable, "Bldg No.", config != null ? str(config.getBldgNo()) : "-");
                    addInfoStripCell(configTable, "Bowl ID", config != null ? str(config.getBowlId()) : "-");
                    addInfoStripCell(configTable, "Batch Size",
                            mix != null && mix.getMixQuantity() != null
                                    ? mix.getMixQuantity() + " kg" : "-");
                    addInfoStripCell(configTable, "Mixing Cycle", mixCycle.isBlank() ? "-" : mixCycle);
                    addInfoStripCell(configTable, "Mix Date",
                            mix != null ? str(mix.getMixDate()) : "-");
                    addInfoStripCell(configTable, "Observations",
                            premix.getTrialDetails() != null
                                    ? str(premix.getTrialDetails().getObservations()) : "-");
                    addInfoStripCell(configTable, "Premix / Final Mix No.", premixNo);
                    addInfoStripCell(configTable, "Bowl Trial Date & Time",
                            premix.getTrialDetails() != null
                                    ? str(premix.getTrialDetails().getTrialDateTime()) : "-", 4);
                } else {
                    addInfoStripCell(configTable, "Final Mix No.", premixNo);
                    addInfoStripCell(configTable, "Mixer", config != null ? str(config.getMixerId()) : "-");
                    addInfoStripCell(configTable, "Bldg No.", config != null ? str(config.getBldgNo()) : "-");
                    addInfoStripCell(configTable, "Bowl Id", config != null ? str(config.getBowlId()) : "-");
                    addInfoStripCell(configTable, "Final Mix Cycle",
                            finalMixCycle.isBlank() ? "-" : finalMixCycle, 4);
                }

                doc.add(configTable);
                spacer(doc, 8);

                // ---- Process Particulars ----
                var particulars = premix.getProcessParticulars();
                if (particulars != null && !particulars.isEmpty()) {
                    addSection(doc, "Process Particulars", "");

                    PdfPTable partTable = createTable(new float[]{28, 18, 18, 18, 18});
                    addHeaders(partTable,
                            "OPERATION",
                            "ROTATION (RPM)",
                            "TIME (MIN)",
                            "TEMP (°C)",
                            "VACUUM (TORR)");

                    boolean alt = false;
                    int idx = 1;
                    for (var r : particulars) {
                        Color bg = alt ? ROW_ALT : WHITE;
                        String opLabel = r.getOperationName() != null && !r.getOperationName().isBlank()
                                ? r.getOperationName()
                                : "Operation " + idx;
                        addCell(partTable, opLabel, BODY_FONT, bg);
                        addCell(partTable, str(r.getRpm()), BODY_FONT, bg);
                        addCell(partTable, str(r.getTime()), BODY_FONT, bg);
                        addCell(partTable, str(r.getTemp()), BODY_FONT, bg);
                        addCell(partTable, str(r.getVacuum()), BODY_FONT, bg);
                        alt = !alt;
                        idx++;
                    }
                    doc.add(partTable);
                    spacer(doc, 8);
                }

                // ---- Quality Checks ----
                var qcList = premix.getQualityChecks();
                if (qcList != null && !qcList.isEmpty()) {
                    addSection(doc, "Quality Checks", "");

                    // Discover sample numbers
                    java.util.SortedSet<Integer> sampleNumbers = new java.util.TreeSet<>();
                    for (var qc : qcList) {
                        var observations = qc.getObservations();
                        if (observations == null) continue;
                        int obsIdx = 1;
                        for (var observation : observations) {
                            int sampleNo = observation.getSampleNo() != null && observation.getSampleNo() > 0
                                    ? observation.getSampleNo()
                                    : obsIdx;
                            sampleNumbers.add(sampleNo);
                            obsIdx++;
                        }
                    }
                    if (sampleNumbers.isEmpty()) sampleNumbers.add(1);

                    float[] qcWidths = new float[2 + sampleNumbers.size()];
                    qcWidths[0] = 28;
                    qcWidths[1] = 22;
                    float observedWidth = 50f / sampleNumbers.size();
                    for (int i = 2; i < qcWidths.length; i++) {
                        qcWidths[i] = observedWidth;
                    }

                    PdfPTable qcTable = createTable(qcWidths);

                    // Header row 1
                    PdfPCell pHeader = new PdfPCell(new Phrase("PARAMETER", HEADER_FONT));
                    pHeader.setBackgroundColor(BRAND_BLUE);
                    pHeader.setPadding(5);
                    pHeader.setRowspan(2);
                    pHeader.setBorderColor(BORDER_LIGHT);
                    pHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
                    pHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    qcTable.addCell(pHeader);

                    PdfPCell sHeader = new PdfPCell(new Phrase("SPECIFICATION", HEADER_FONT));
                    sHeader.setBackgroundColor(BRAND_BLUE);
                    sHeader.setPadding(5);
                    sHeader.setRowspan(2);
                    sHeader.setBorderColor(BORDER_LIGHT);
                    sHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
                    sHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    qcTable.addCell(sHeader);

                    PdfPCell oHeader = new PdfPCell(new Phrase("OBSERVED VALUES", HEADER_FONT));
                    oHeader.setBackgroundColor(BRAND_BLUE);
                    oHeader.setPadding(5);
                    oHeader.setColspan(sampleNumbers.size());
                    oHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
                    oHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    oHeader.setBorderColor(BORDER_LIGHT);
                    qcTable.addCell(oHeader);

                    // Header row 2 – sample numbers
                    for (Integer sampleNo : sampleNumbers) {
                        PdfPCell sampleHeader = new PdfPCell(new Phrase(sampleNo.toString(), HEADER_FONT));
                        sampleHeader.setBackgroundColor(BRAND_BLUE);
                        sampleHeader.setPadding(5);
                        sampleHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
                        sampleHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        sampleHeader.setBorderColor(BORDER_LIGHT);
                        qcTable.addCell(sampleHeader);
                    }
                    qcTable.setHeaderRows(2);

                    boolean alt = false;
                    for (var qc : qcList) {
                        Color bg = alt ? ROW_ALT : WHITE;

                        String paramLabel = qc.getParameterName() != null && !qc.getParameterName().isBlank()
                                ? qc.getParameterName()
                                : str(qc.getParameterId());

                        String specification = qcSpecification(qc.getSpecification());
                        if ("—".equals(specification) || specification == null) {
                            specification = "-";
                        }

                        Map<Integer, String> observedValues = new LinkedHashMap<>();
                        var observations = qc.getObservations();
                        if (observations != null) {
                            int obsIdx = 1;
                            for (var obs : observations) {
                                int sampleNo = obs.getSampleNo() != null && obs.getSampleNo() > 0
                                        ? obs.getSampleNo()
                                        : obsIdx;
                                observedValues.put(sampleNo, str(obs.getValue()));
                                obsIdx++;
                            }
                        }

                        addCenteredCell(qcTable, paramLabel, bg);
                        addCenteredCell(qcTable, specification, bg);
                        for (Integer sampleNo : sampleNumbers) {
                            addCenteredCell(qcTable,
                                    observedValues.getOrDefault(sampleNo, "-"), bg);
                        }
                        alt = !alt;
                    }
                    doc.add(qcTable);
                }

                spacer(doc, 12);
            }
        }
    }

    private void renderCastingCuring(Document doc, CastingCuringFormResponse data) throws DocumentException {

        if (data == null || data.getMotors() == null || data.getMotors().isEmpty()) {
            doc.add(new Paragraph("No casting or curing metrics found.", BODY_FONT));
            return;
        }

        // ------------------------------------------------------------------
        // 1. FORM SUMMARY
        // ------------------------------------------------------------------
        addSection(doc, "Casting & Curing", "");

        PdfPTable summary = createTable(new float[]{25, 25, 25, 25});
        addInfoStripCell(summary, "Batch ID", str(data.getBatchId()));
        addInfoStripCell(summary, "Submission Type", workflowStatusLabel(data.getFormSubmissionType()));
        addInfoStripCell(summary, "Created By", str(data.getCreatedBy()));
        addInfoStripCell(summary, "Submitted By",
                data.getSubmittedBy() != null ? str(data.getSubmittedBy().getFullName()) : "-");

        // Status
        PdfPCell statusCell = new PdfPCell();
        statusCell.setColspan(4);
        statusCell.setBorderColor(BORDER_LIGHT);
        statusCell.setPadding(6);
        statusCell.setBackgroundColor(ROW_ALT);
        Paragraph sp = new Paragraph();
        sp.add(new Chunk("Form Status: ", BODY_BOLD));
        sp.add(createStatusPhrase(data.getStatus() != null ? data.getStatus().toString() : "-"));
        statusCell.addElement(sp);
        summary.addCell(statusCell);

        doc.add(summary);
        spacer(doc, 12);

        // ------------------------------------------------------------------
        // 2. MOTORS
        // ------------------------------------------------------------------
        for (CastingCuringMotor motor : data.getMotors()) {

            String motorId = motor.getMotorId();
            String approvalStatus = castingMotorStatus(data, motor, motorId);

            // Motor header + coloured status
            Paragraph motorTitle = new Paragraph();
            motorTitle.add(new Chunk("Motor · " + str(motorId) + "   ", SECTION_FONT));
            motorTitle.add(createStatusPhrase(approvalStatus));
            motorTitle.setSpacingBefore(10);
            motorTitle.setSpacingAfter(6);
            doc.add(motorTitle);

            Map<String, Object> motorMap = mapper.convertValue(
                    motor, new TypeReference<Map<String, Object>>() {
                    });
            Map<String, List<Map<String, Object>>> castMap = ccTypedCastingSectionMap(motor);
            Map<String, List<Map<String, Object>>> cureMap = ccTypedCuringSectionMap(motor);
            Map<String, Object> castingSetup = ccFirstMap(motor.getSetup());
            Map<String, Object> curingSetup = ccFirstMap(motor.getCuringSetup());

            // Quick meta
            PdfPTable meta = createKeyValueTable();
            addKV(meta, "Motor Received Date & Time",
                    ccText(motorMap, "motorReceivedDateTime", "motorReceivedAt"));
            doc.add(meta);
            spacer(doc, 8);

            // ==============================================================
            // CASTING
            // ==============================================================
            addSection(doc, "Casting", "");

            // Setup
            PdfPTable setupTable = createKeyValueTable();
//            addKV(setupTable, "Type of Casting", ccText(castingSetup, "castingType", "typeOfCasting"));
            addKV(setupTable, "Casting Station", ccText(castingSetup, "castingStation"));
            doc.add(setupTable);
            spacer(doc, 6);

            // ---- Section A: Final Assembly Details ----
            List<Map<String, Object>> weightRows = ccRows(
                    ccSection(castMap, "EMPTY_MOTOR_WEIGHT", "FINAL_ASSEMBLY_DETAILS"),
                    "rows", "FINAL_ASSEMBLY_DETAILS");

            List<Map<String, Object>> mandrelRows = ccRows(
                    ccSection(castMap, "MANDREL_MEASUREMENTS"),
                    "rows", "MANDREL_MEASUREMENTS");

            List<Map<String, Object>> feedRows = ccRows(
                    ccSection(castMap, "FEED_PIPE_DISTANCE"),
                    "rows", "FEED_PIPE_DISTANCE");

            if (!weightRows.isEmpty() || !mandrelRows.isEmpty() || !feedRows.isEmpty()) {
                addSection(doc, "Final Assembly Details", "");


                if (!mandrelRows.isEmpty()) {
                    Paragraph h = new Paragraph("Distance between centering top to mandrel top (mm)", BODY_BOLD);
                    h.setSpacingAfter(3);
                    doc.add(h);

                    PdfPTable table = createTable(new float[]{6, 12, 12, 12, 12, 12, 12, 11, 11});
                    addHeaders(table,
                            "SR.",
                            "A (MOCK)",
                            "B (MOCK)",
                            "B (FINAL)",
                            "C (MOCK)",
                            "C (FINAL)",
                            "D (BELLOW)",
                            "E (MOCK)",
                            "E (FINAL)");

                    boolean alt = false;
                    int sr = 1;
                    for (Map<String, Object> row : mandrelRows) {
                        Color bg = alt ? ROW_ALT : WHITE;
                        addCell(table, ccSerial(row, sr++), BODY_FONT, bg);
                        addCell(table, ccText(row, "A_MOCK", "aMock"), BODY_FONT, bg);
                        addCell(table, ccText(row, "B_MOCK", "bMock"), BODY_FONT, bg);
                        addCell(table, ccText(row, "B_FINAL", "bFinal"), BODY_FONT, bg);
                        addCell(table, ccText(row, "C_MOCK", "cMock"), BODY_FONT, bg);
                        addCell(table, ccText(row, "C_FINAL", "cFinal"), BODY_FONT, bg);
                        addCell(table, ccText(row, "BELLOWS_THICKNESS_D", "bellowsThicknessD"), BODY_FONT, bg);
                        addCell(table, ccText(row, "E_MOCK", "eMock"), BODY_FONT, bg);
                        addCell(table, ccText(row, "E_FINAL", "eFinal"), BODY_FONT, bg);
                        alt = !alt;
                    }
                    doc.add(table);
                    spacer(doc, 6);
                }
                if (!weightRows.isEmpty()) {
                    PdfPTable t = createKeyValueTable();
                    addKV(t, "Weight of assembled empty motor casing (kg)",
                            ccUnit(qcValue(weightRows.get(0), "EMPTY_MOTOR_WEIGHT", "emptyMotorWeight"), "kg"));
                    addKV(t, "Vacuum Applied (overnight)", ccText(weightRows.get(0), "vacuumApplied"));
                    doc.add(t);
                    spacer(doc, 4);
                }
                if (!feedRows.isEmpty()) {
                    Paragraph h = new Paragraph("Measurement of distance between feed pipe & cone apex", BODY_BOLD);
                    h.setSpacingAfter(3);
                    doc.add(h);

                    PdfPTable table = createTable(new float[]{15, 42, 43});
                    addHeaders(table, "SR.", "READING 1 (mm)", "READING 2 (mm)");
                    int sr = 1;
                    boolean alt = false;
                    for (Map<String, Object> row : feedRows) {
                        Color bg = alt ? ROW_ALT : WHITE;
                        addCell(table, ccSerial(row, sr++), BODY_FONT, bg);
                        addCell(table, ccText(row, "READING_1", "reading1"), BODY_FONT, bg);
                        addCell(table, ccText(row, "READING_2", "reading2"), BODY_FONT, bg);
                        alt = !alt;
                    }
                    doc.add(table);
                    spacer(doc, 6);
                }
            }

            // ---- Section B: Casting Process ----
            List<Map<String, Object>> processRows = ccRows(
                    ccSection(castMap, "CASTING_PROCESS", "CASTING_PROCESS_DETAILS"),
                    "rows", "CASTING_PROCESS");
            Map<String, Object> process = processRows.isEmpty()
                    ? ccFirstMap(qcValue(motorMap, "castingProcess"))
                    : processRows.get(0);

            List<Map<String, Object>> finalMixRows = ccRows(
                    ccSection(castMap, "FINAL_MIX_DETAILS", "FINAL_MIX_BOWL_DETAILS"),
                    "rows", "FINAL_MIX_BOWL_DETAILS", "FINAL_MIX_DETAILS");

            List<Map<String, Object>> castingRows = ccRows(
                    ccSection(castMap, "CASTING_FROM_BOWL_DETAILS", "CASTING_FROM_BOWL"),
                    "rows", "CASTING_FROM_BOWL_DETAILS", "CASTING_FROM_BOWL");

            if (!process.isEmpty() || !finalMixRows.isEmpty() || !castingRows.isEmpty()) {
                addSection(doc, "Casting Process", "");

                if (!finalMixRows.isEmpty()) {
                    addSection(doc, "Final Mix Bowl Details", "");
                    PdfPTable table = createTable(
                            new float[]{11, 15, 9, 9, 10, 12, 12, 11, 11});
                    addCenteredHeaders(table,
                            "BOWL ID",
                            "RECEIPT DATE & TIME",
                            "INITIAL WEIGHT",
                            "FINAL WEIGHT",
                            "INITIAL SLURRY DEPTH",
                            "D/C VALVE OPEN DATE & TIME",
                            "D/C VALVE CLOSE DATE & TIME",
                            "SLURRY DEPTH AFTER D/C OPEN",
                            "BALL VALVE OPEN DATE & TIME");
                    boolean alt = false;
                    for (Map<String, Object> row : finalMixRows) {
                        Color bg = alt ? ROW_ALT : WHITE;
                        addCenteredCell(table, ccText(row, "BOWL_ID", "bowlId"), bg);
                        addCenteredCell(table,
                                ccDateTime(row, "bowlReceiptDateTime", "bowlReceiptDate", "bowlReceiptTime"), bg);
                        addCenteredCell(table, ccText(row, "INITIAL_WEIGHT", "initialWeight"), bg);
                        addCenteredCell(table, ccText(row, "FINAL_WEIGHT", "finalWeight"), bg);
                        addCenteredCell(table,
                                ccText(row, "INITIAL_SLURRY_DEPTH", "initialSlurryDepth"), bg);
                        addCenteredCell(table,
                                ccDateTime(row, "dcOpenDateTime", "dcOpenDate", "dcOpenTime"), bg);
                        addCenteredCell(table,
                                ccDateTime(row, "dcCloseDateTime", "dcCloseDate", "dcCloseTime"), bg);
                        addCenteredCell(table,
                                ccText(row, "SLURRY_DEPTH_AFTER_DC", "slurryDepthAfterDc"), bg);
                        addCenteredCell(table,
                                ccDateTime(row, "ballValveOpenDateTime", "ballValveOpenDate", "ballValveOpenTime"), bg);
                        alt = !alt;
                    }
                    doc.add(table);
                    spacer(doc, 6);
                }

                if (!castingRows.isEmpty()) {
                    Paragraph h = new Paragraph("Casting From Bowl Details", BODY_BOLD);
                    h.setSpacingAfter(3);
                    doc.add(h);

                    PdfPTable table = createTable(new float[]{14, 8, 7, 9, 10, 8, 9, 8, 9, 10});
                    addHeaders(table,
                            "BOWL ID",
                            "TIME",
                            "RH %",
                            "VISCOSITY (POISE)",
                            "MOTOR ID",
                            "DEPTH (CM)",
                            "CAST (KG)",
                            "FLOW (KG/MIN)",
                            "VALVE OPENING (%)",
                            "VACUUM (TORR)");

                    boolean alt = false;
                    for (Map<String, Object> row : castingRows) {
                        Color bg = alt ? ROW_ALT : WHITE;
                        addCell(table, ccText(row, "BOWL_ID", "bowlId"), BODY_FONT, bg);
                        addCell(table, ccText(row, "TIME_INTERVAL", "timeInterval"), BODY_FONT, bg);
                        addCell(table, ccText(row, "RH", "relativeHumidity"), BODY_FONT, bg);
                        addCell(table, ccText(row, "VISCOSITY", "viscosity"), BODY_FONT, bg);
                        addCell(table, ccText(row, "MOTOR_ID", "motorId"), BODY_FONT, bg);
                        addCell(table, ccText(row, "SLURRY_DEPTH", "slurryDepth"), BODY_FONT, bg);
                        addCell(table, ccText(row, "SLURRY_CAST", "slurryCast"), BODY_FONT, bg);
                        addCell(table, ccText(row, "FLOW_RATE", "flowRate"), BODY_FONT, bg);
                        addCell(table, ccText(row, "VALVE_OPENING", "valveOpening"), BODY_FONT, bg);
                        addCell(table, ccText(row, "VACUUM_LEVEL", "vacuumLevel"), BODY_FONT, bg);
                        alt = !alt;
                    }
                    doc.add(table);
                    spacer(doc, 6);
                }

                if (!process.isEmpty()) {
                    PdfPTable t = createKeyValueTable();
                    addKV(t, "Initial Vacuum", ccText(process, "INITIAL_VACUUM", "initialVacuum"));
                    addKV(t, "Vacuum Pressure Created for Casting",
                            ccText(process, "VACUUM_PRESSURE_CASTING", "vacuumPressureCasting"));
                    addKV(t, "Vacuum Pressure Created for Soaking",
                            ccText(process, "VACUUM_PRESSURE_SOAKING", "vacuumPressureSoaking"));
                    addKV(t, "TDI Clearance after Vacuum Trial",
                            ccDateTime(process, "TDI_CLEARANCE_AFTER_VACUUM",
                                    "tdiClearanceAfterVacuum", null));
                    doc.add(t);
                    spacer(doc, 6);
                }
            }

            // ---- Section C: Slurry Cast ----
            List<Map<String, Object>> slurryRows = ccRows(
                    ccSection(castMap, "SLURRY_CAST_DETAILS", "SLURRY_CAST_FROM_BOWLS"),
                    "rows", "SLURRY_CAST_FROM_BOWLS", "SLURRY_CAST_DETAILS");

            if (!slurryRows.isEmpty()) {
                addSection(doc, "Slurry Cast into Motor from Bowls", "");
                PdfPTable table = createTable(new float[]{60, 40});
                addHeaders(table, "FM / MOTOR ID", "SLURRY CAST (KG)");
                boolean alt = false;
                for (Map<String, Object> row : slurryRows) {
                    Color bg = alt ? ROW_ALT : WHITE;
                    addCell(table, ccText(row, "FM_MOTOR_LABEL", "fmMotorLabel", "BOWL_ID"), BODY_FONT, bg);
                    addCell(table, ccText(row, "SLURRY_CAST", "slurryCast"), BODY_FONT, bg);
                    alt = !alt;
                }
                doc.add(table);
                spacer(doc, 6);
            }

            // ---- Section D: Post Cast Operations ----
            List<Map<String, Object>> postCastRows = ccRows(
                    ccSection(castMap, "POST_CAST_OPERATIONS", "POST_CAST_TABLE"),
                    "rows", "POST_CAST_TABLE", "POST_CAST_OPERATIONS");

            if (!postCastRows.isEmpty()) {
                addSection(doc, "Post Cast Operations", "");
                PdfPTable table = createTable(new float[]{45, 55});
                addHeaders(table, "ACTIVITY", "POST CAST OPERATION DETAILS");
                boolean alt = false;
                for (Map<String, Object> row : postCastRows) {
                    Color bg = alt ? ROW_ALT : WHITE;
                    addCell(table, ccCastingActivityLabel(
                            ccText(row, "ACTIVITY", "activity", "PARAMETER", "parameter")), BODY_FONT, bg);
                    addCell(table, ccText(row, "DETAILS", "details", "VALUE", "value"), BODY_FONT, bg);
                    alt = !alt;
                }
                doc.add(table);
                spacer(doc, 8);
            }

            // ==============================================================
            // CURING
            // ==============================================================
            addSection(doc, "Curing", "");

            PdfPTable cureSetup = createKeyValueTable();
            addKV(cureSetup, "Type of Curing", prettyText(ccText(curingSetup, "curingType")));
            addKV(cureSetup, "Oven", ccText(curingSetup, "oven", "selectOven"));
            addKV(cureSetup, "Oven Number", ccText(curingSetup, "ovenNo"));
            doc.add(cureSetup);
            spacer(doc, 6);

            // Curing Cycles
            List<Map<String, Object>> curingRows = ccRows(
                    ccSection(cureMap, "CURING_TABLE", "CURING_CYCLES"),
                    "rows", "CURING_TABLE", "CURING_CYCLES");

            if (!curingRows.isEmpty()) {
                addSection(doc, "Curing Cycles", "");
                // Give the date columns enough room to keep each timestamp together in portrait PDFs.
                PdfPTable table = createTable(new float[]{5, 7, 7, 22, 22, 11, 7, 9, 10});
                addHeaders(table,
                        "S.NO",
                        "TEMP (°C)",
                        "TIME (MIN)",
                        "START DATE & TIME",
                        "END DATE & TIME",
                        "PRESSURE (BAR)",
                        "HOT WATER",
                        "DURATION (MIN)",
                        "REMARKS");

                boolean alt = false;
                int sr = 1;
                for (Map<String, Object> row : curingRows) {
                    Color bg = alt ? ROW_ALT : WHITE;
                    addCell(table, ccSerial(row, sr++), BODY_FONT, bg);
                    addCell(table, ccText(row, "TEMPERATURE", "temperature"), BODY_FONT, bg);
                    addCell(table, ccText(row, "TIME", "duration"), BODY_FONT, bg);
                    addCell(table, ccDateTime(row, "START_DATE_TIME", "START_DATE", "START_TIME"),
                            BODY_FONT, bg);
                    addCell(table, ccDateTime(row, "END_DATE_TIME", "END_DATE", "END_TIME"),
                            BODY_FONT, bg);
                    addCell(table, ccPropellantPressure(row), BODY_FONT, bg);
                    addCell(table, ccText(row, "HOT_WATER_STATUS", "hotWaterStatus"), BODY_FONT, bg);
                    addCell(table, ccCycleDuration(row), BODY_FONT, bg);
                    addCell(table, ccText(row, "REMARKS", "remarks"), BODY_FONT, bg);
                    alt = !alt;
                }
                doc.add(table);
                spacer(doc, 6);
            }

            // Post Curing Details
            List<Map<String, Object>> postCuringRows = ccRows(
                    ccSection(cureMap, "POST_CURING_DETAILS"),
                    "rows", "POST_CURING_DETAILS");

            if (!postCuringRows.isEmpty()) {
                Map<String, Object> row = postCuringRows.get(0);
                addSection(doc, "Post Curing Details", "");
                PdfPTable t = createKeyValueTable();
                addKV(t, "Any Other Observations", ccText(row, "OTHER_OBSERVATIONS", "otherObservations"));
                addKV(t, "Visual Observation of Cured Motor", ccText(row, "VISUAL_OBSERVATION", "visualObservation"));
                addKV(t, "Date & Time for Removal of Pressure Plate",
                        ccDateTime(row, "PRESSURE_PLATE_REMOVAL_DATE_TIME",
                                "pressurePlateRemovalDateTime", null));
                addKV(t, "Hardness (Shore A)", ccText(row, "SHORE_A_HARDNESS", "shoreAHardness"));
                addKV(t, "Date & Time of Dispatch for De-coring",
                        ccDateTime(row, "DE_CORING_DISPATCH_DATE_TIME",
                                "deCoringDispatchDateTime", null));
                doc.add(t);
                spacer(doc, 6);
            }

            // De-coring Details
            List<Map<String, Object>> decoringRows = ccRows(
                    ccSection(cureMap, "DECORING_DETAILS"),
                    "rows", "DECORING_DETAILS");

            if (!decoringRows.isEmpty()) {
                Map<String, Object> row = decoringRows.get(0);
                addSection(doc, "De-coring Details", "");
                PdfPTable t = createKeyValueTable();
                addKV(t, "Date & Time of De-coring", ccText(row,
                        "DECORING_DATE_TIME", "decoringDateTime", "DECORING_DATE", "decoringDate"));
                addKV(t, "Building No", ccBuildingName(ccText(row, "BUILDING_NO", "buildingNo")));
                addKV(t, "De-coring Load (kg)", ccText(row, "DECORING_LOAD", "decoringLoad"));
                addKV(t, "Propellent Temperature (Celcius)", ccText(row,
                        "PROPELLANT_TEMPERATURE", "propellantTemperature"));
                addKV(t, "RH (%)", ccText(row, "RH", "rh"));
                addKV(t, "Mandrel Weight (KG)", ccText(row, "MANDREL_WEIGHT", "mandrelWeight"));
                addKV(t, "Remarks", ccText(row, "DECORING_REMARKS", "decoringRemarks"));
                addKV(t, "Visual Observations", extractFileNames(
                        qcValue(row, "DECORING_VISUAL_OBSERVATION", "decoringVisualObservation")));
                doc.add(t);
            }

            spacer(doc, 14);
        }
    }

    private String extractFileNames(Object value) {
        if (value == null) return "-";

        // Already a plain string
        if (value instanceof String s) {
            return s.isBlank() ? "-" : s;
        }

        // List of maps / DocumentDto
        if (value instanceof Collection<?> list) {
            StringBuilder sb = new StringBuilder();
            for (Object item : list) {
                String name = extractSingleFileName(item);
                if (name != null && !name.isBlank() && !"-".equals(name)) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(name);
                }
            }
            return sb.length() > 0 ? sb.toString() : "-";
        }

        // Single map
        return extractSingleFileName(value);
    }

    private String extractSingleFileName(Object item) {
        if (item == null) return "-";

        if (item instanceof String s) return s;

        if (item instanceof Map<?, ?> map) {
            Object fileName = map.get("fileName");
            if (fileName == null) fileName = map.get("name");
            if (fileName == null) fileName = map.get("originalFileName");
            return fileName != null ? fileName.toString() : "-";
        }

        // Try reflection for DocumentDto style objects
        try {
            var method = item.getClass().getMethod("getFileName");
            Object result = method.invoke(item);
            if (result != null) return result.toString();
        } catch (Exception ignored) {
        }

        return str(item); // fallback
    }

    private Map<String, List<Map<String, Object>>> ccTypedCastingSectionMap(
            CastingCuringMotor motor) {
        Map<String, List<Map<String, Object>>> sections = new LinkedHashMap<>();
        if (motor.getCastingSections() == null) return sections;

        var finalAssembly = motor.getCastingSections().getFinalAssemblyDetails();
        if (finalAssembly != null) {
            List<Map<String, Object>> motorCasings = ccTypedRows(finalAssembly.getMotorCasing());
            sections.put(normalizePdfKey("EMPTY_MOTOR_WEIGHT"), motorCasings);
            sections.put(normalizePdfKey("MANDREL_MEASUREMENTS"), motorCasings.stream()
                    .flatMap(row -> qcMapList(qcValue(row, "mandrelMeasurements")).stream())
                    .toList());
            sections.put(normalizePdfKey("FEED_PIPE_DISTANCE"), motorCasings.stream()
                    .flatMap(row -> qcMapList(qcValue(row, "feedPipeDistance")).stream())
                    .toList());
        }

        var process = motor.getCastingSections().getCastingProcess();
        if (process != null) {
            sections.put(normalizePdfKey("CASTING_PROCESS"), ccTypedRows(process));
            sections.put(normalizePdfKey("FINAL_MIX_BOWL_DETAILS"),
                    ccTypedRows(process.getFinalMixBowlDetails()));
            sections.put(normalizePdfKey("CASTING_FROM_BOWL_DETAILS"),
                    ccTypedRows(process.getCastingFromBowlDetails()));
        }

        var slurry = motor.getCastingSections().getSlurryCastDetails();
        if (slurry != null) {
            sections.put(normalizePdfKey("SLURRY_CAST_DETAILS"),
                    ccTypedRows(slurry.getSlurryCastFromBowls()));
        }

        var postCast = motor.getCastingSections().getPostCastOperations();
        if (postCast != null) {
            sections.put(normalizePdfKey("POST_CAST_OPERATIONS"),
                    ccTypedRows(postCast.getPostCastTable()));
        }
        return sections;
    }

    private Map<String, List<Map<String, Object>>> ccTypedCuringSectionMap(
            CastingCuringMotor motor) {
        Map<String, List<Map<String, Object>>> sections = new LinkedHashMap<>();
        if (motor.getCuringSections() == null) return sections;

        var cycles = motor.getCuringSections().getCuringCycles();
        if (cycles != null) {
            sections.put(normalizePdfKey("CURING_CYCLES"),
                    ccTypedRows(cycles.getCuringTable()));
        }
        if (motor.getCuringSections().getPostCuringDetails() != null) {
            sections.put(normalizePdfKey("POST_CURING_DETAILS"),
                    ccTypedRows(motor.getCuringSections().getPostCuringDetails()));
        }
        if (motor.getCuringSections().getDecoringDetails() != null) {
            sections.put(normalizePdfKey("DECORING_DETAILS"),
                    ccTypedRows(motor.getCuringSections().getDecoringDetails()));
        }
        return sections;
    }

    private List<Map<String, Object>> ccTypedRows(Object value) {
        if (value == null) return Collections.emptyList();
        if (value instanceof Collection<?>) {
            return mapper.convertValue(
                    value,
                    new TypeReference<List<Map<String, Object>>>() {
                    });
        }
        return List.of(mapper.convertValue(
                value,
                new TypeReference<Map<String, Object>>() {
                }));
    }

    private Map<String, List<Map<String, Object>>> ccSectionMap(Object value) {
        Map<String, List<Map<String, Object>>> sections = new LinkedHashMap<>();
        for (Map<String, Object> section : qcMapList(value)) {
            String sectionId = qcText(qcValue(section, "sectionId"));
            if (sectionId == null) continue;
            sections.put(normalizePdfKey(sectionId), qcMapList(qcValue(section, "sectionData")));
        }
        return sections;
    }

    private List<Map<String, Object>> ccSection(
            Map<String, List<Map<String, Object>>> sections,
            String... ids) {
        for (String id : ids) {
            List<Map<String, Object>> rows = sections.get(normalizePdfKey(id));
            if (rows != null) return rows;
        }
        return Collections.emptyList();
    }

    private List<Map<String, Object>> ccRows(
            List<Map<String, Object>> sectionData,
            String... nestedKeys) {
        if (sectionData == null || sectionData.isEmpty()) return Collections.emptyList();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> item : sectionData) {
            boolean nestedRowsFound = false;
            for (String nestedKey : nestedKeys) {
                List<Map<String, Object>> nestedRows = qcMapList(qcValue(item, nestedKey));
                if (!nestedRows.isEmpty()) {
                    rows.addAll(nestedRows);
                    nestedRowsFound = true;
                    break;
                }
            }
            if (!nestedRowsFound) rows.add(item);
        }
        return rows;
    }

    private Map<String, Object> ccFirstMap(Object value) {
        if (value instanceof Map<?, ?> map) return stringKeyMap(map);
        List<Map<String, Object>> maps = qcMapList(value);
        if (!maps.isEmpty()) return maps.get(0);
        if (value == null || value instanceof Collection<?>) return Collections.emptyMap();
        return mapper.convertValue(value, new TypeReference<Map<String, Object>>() {
        });
    }

    private Map<String, Object> ccFirstRow(
            Map<String, List<Map<String, Object>>> sections,
            String... ids) {
        List<Map<String, Object>> rows = ccRows(ccSection(sections, ids), "rows");
        return rows.isEmpty() ? Collections.emptyMap() : rows.get(0);
    }

    private String ccText(Map<String, Object> source, String... keys) {
        return str(qcValue(source, keys));
    }

    private String ccPropellantPressure(Map<String, Object> row) {
        Object value = qcValue(row, "PROPELLANT_PRESSURE", "propellantPressure");
        if (!(value instanceof Map<?, ?> pressureMap)) {
            return str(value);
        }

        Map<String, Object> readings = stringKeyMap(pressureMap);
        List<String> values = new ArrayList<>();
        for (String key : List.of("reading1", "reading2")) {
            Object reading = qcValue(readings, key);
            if (reading != null) {
                String formatted = str(reading);
                if (!"—".equals(formatted)) {
                    values.add(formatted);
                }
            }
        }
        return values.isEmpty() ? "—" : String.join(", ", values);
    }

    private String ccCycleDuration(Map<String, Object> row) {
        Object explicit = qcValue(row, "DURATION", "durationMinutes", "cycleDuration");
        if (explicit != null) return str(explicit);
        Object start = qcValue(row, "START_DATE_TIME", "startDateTime");
        Object end = qcValue(row, "END_DATE_TIME", "endDateTime");
        try {
            LocalDateTime startTime = start instanceof LocalDateTime dateTime
                    ? dateTime : DateTimeConversionUtil.parseLocalDateTime(start.toString());
            LocalDateTime endTime = end instanceof LocalDateTime dateTime
                    ? dateTime : DateTimeConversionUtil.parseLocalDateTime(end.toString());
            return Long.toString(java.time.Duration.between(startTime, endTime).toMinutes());
        } catch (Exception ignored) {
            return "—";
        }
    }

    private String ccSerial(Map<String, Object> row, int fallback) {
        Object serial = qcValue(row, "srNo", "serialNo", "rowKey");
        return serial != null ? str(serial) : Integer.toString(fallback);
    }

    private String ccUnit(Object value, String unit) {
        String text = str(value);
        if ("—".equals(text) || text.toLowerCase().endsWith(unit.toLowerCase())) return text;
        return text + " " + unit;
    }

    private String ccBuildingName(String buildingCode) {
        if (buildingCode == null || buildingCode.isBlank() || buildingMasterRepository == null) {
            return buildingCode;
        }
        return buildingMasterRepository.findByCode(buildingCode)
                .map(BuildingMaster::getName)
                .filter(name -> name != null && !name.isBlank())
                .orElse(buildingCode);
    }

    private String ccDateTime(
            Map<String, Object> row,
            String dateTimeKey,
            String legacyDateKey,
            String legacyTimeKey) {
        Object dateTimeValue = qcValue(row, dateTimeKey);
        if (dateTimeValue != null) {
            return formatPdfDateTimeValue(dateTimeValue);
        }
        String date = formatPdfDateValue(qcValue(row, legacyDateKey));
        String time = formatPdfTimeValue(qcValue(row, legacyTimeKey));
        if ("—".equals(date)) return time;
        if ("—".equals(time)) return date;
        return date + " " + time;
    }

    private String formatPdfDateValue(Object value) {
        if (value == null) return "—";
        if (value instanceof LocalDate date) return date.format(PDF_DATE_FORMAT);
        if (value instanceof List<?> parts && parts.size() >= 3) {
            try {
                return LocalDate.of(
                                Integer.parseInt(parts.get(0).toString()),
                                Integer.parseInt(parts.get(1).toString()),
                                Integer.parseInt(parts.get(2).toString()))
                        .format(PDF_DATE_FORMAT);
            } catch (NumberFormatException | DateTimeException ignored) {
                return str(value);
            }
        }
        String text = value.toString().trim();
        if (text.isEmpty()) return "—";
        try {
            LocalDate date = DateTimeConversionUtil.parseLocalDate(text);
            return date != null ? date.format(PDF_DATE_FORMAT) : str(value);
        } catch (DateTimeParseException ignored) {
            return str(value);
        }
    }

    private String formatPdfTimeValue(Object value) {
        if (value == null) return "—";
        if (value instanceof LocalTime time) return time.format(PDF_TIME_FORMAT);
        if (value instanceof List<?> parts && parts.size() >= 2) {
            try {
                int second = parts.size() > 2 ? Integer.parseInt(parts.get(2).toString()) : 0;
                return LocalTime.of(
                                Integer.parseInt(parts.get(0).toString()),
                                Integer.parseInt(parts.get(1).toString()),
                                second)
                        .format(PDF_TIME_FORMAT);
            } catch (NumberFormatException | DateTimeException ignored) {
                return str(value);
            }
        }
        String text = value.toString().trim();
        if (text.isEmpty()) return "—";
        try {
            return LocalTime.parse(text).format(PDF_TIME_FORMAT);
        } catch (DateTimeParseException ignored) {
            return str(value);
        }
    }

    private String castingMotorStatus(
            CastingCuringFormResponse data,
            CastingCuringMotor motor,
            String motorId) {
        Object savedStatus = motor.getMotorSubmissionStatus();
        if (savedStatus != null) return workflowStatusLabel(savedStatus.toString());
        if (data.getMotorStatuses() != null) {
            for (MotorStatusResponse status : data.getMotorStatuses()) {
                if (motorId.equals(str(status.getMotorId()))) {
                    return workflowStatusLabel(str(status.getMotorSubmissionStatus()));
                }
            }
        }
        return "—";
    }

    private void addCcTitle(Document doc, String title) throws DocumentException {
        Paragraph paragraph = new Paragraph(title, BODY_BOLD);
        paragraph.setSpacingBefore(6);
        paragraph.setSpacingAfter(3);
        doc.add(paragraph);
    }

    private void renderPostCure(Document doc, PostCureResponse data) throws DocumentException {
        if (data == null || data.getMotors() == null || data.getMotors().isEmpty()) {
            doc.add(new Paragraph("No post cure details available.", BODY_FONT));
            return;
        }

        // ------------------------------------------------------------------
        // 1. FORM SUMMARY
        // ------------------------------------------------------------------
        addSection(doc, "Post-Cure", "");

        PdfPTable summary = createTable(new float[]{25, 25, 25, 25});
        addInfoStripCell(summary, "Batch ID", str(data.getBatchId()));
        addInfoStripCell(summary, "Batch Type", str(data.getBatchType()));
        addInfoStripCell(summary, "Created By", str(data.getCreatedBy()));
        addInfoStripCell(summary, "Submitted By",
                data.getSubmittedBy() != null ? str(data.getSubmittedBy().getFullName()) : "-");

        // Status
        PdfPCell statusCell = new PdfPCell();
        statusCell.setColspan(4);
        statusCell.setBorderColor(BORDER_LIGHT);
        statusCell.setPadding(6);
        statusCell.setBackgroundColor(ROW_ALT);
        Paragraph sp = new Paragraph();
        sp.add(new Chunk("Form Status: ", BODY_BOLD));
        sp.add(createStatusPhrase(data.getStatus() != null ? data.getStatus().toString() : "-"));
        statusCell.addElement(sp);
        summary.addCell(statusCell);

        doc.add(summary);
        spacer(doc, 12);

        // ------------------------------------------------------------------
        // 2. MOTORS
        // ------------------------------------------------------------------
        for (PostCureMotor motor : data.getMotors()) {
            String motorId = str(motor.getMotorId());
            String approvalStatus = postCureMotorStatus(data, motor, motorId);

            // Motor header + coloured status
            Paragraph motorTitle = new Paragraph();
            motorTitle.add(new Chunk("Motor · " + motorId + "   ", SECTION_FONT));
            motorTitle.add(createStatusPhrase(approvalStatus));
            motorTitle.setSpacingBefore(10);
            motorTitle.setSpacingAfter(6);
            doc.add(motorTitle);

            // Meta
            PdfPTable meta = createKeyValueTable();
            addKV(meta, "Motor Receipt Date & Time", str(motor.getMotorReceiptDateTime()));
            if (motor.getInhibitorType() != null) {
                addKV(meta, "Inhibitor Type", postCureDisplay(motor.getInhibitorType()));
            }
            doc.add(meta);
            spacer(doc, 8);

            renderTypedPostCureDetails(doc, motor);
            spacer(doc, 12);
        }
    }

    private void renderTypedPostCureDetails(
            Document doc,
            PostCureMotor motor) throws DocumentException {
        LooseFlapFillingDetails looseFlap = motor.getLooseFlapFillingDetails();
        if (looseFlap != null) {
            renderBellowRemovalDetails(doc, looseFlap.getBellowRemovalDetails());
            renderPostCureEpoxyPreparation(
                    doc,
                    looseFlap.getEpoxyPreparationIngredients(),
                    "Loose Flap Epoxy Preparation");
            renderPostCureQualification(
                    doc,
                    looseFlap.getQualificationDetails(),
                    "Qualification Details");
            renderLooseFlapFillingDetails(doc, looseFlap.getFillingDetails());
        }

        InhibitionDetails inhibition = motor.getInhibitionDetails();
        if (inhibition == null) return;

        renderPostCureMixDetails(doc, inhibition.getPremixDetails(),
                motor.getInhibitorType() + " Premix");
        renderPostCureMixDetails(doc, inhibition.getFinalMixDetails(),
                motor.getInhibitorType() + " Final Mix");
        renderPostCureQualification(
                doc,
                inhibition.getQualificationDetails(),
                motor.getInhibitorType() + " Qualification Details");
        renderInhibitorBatchDetails(doc, inhibition.getInhibitorBatchDetails());
        renderInhibitionApplicationDetails(doc, inhibition.getApplicationDetails());
        renderPostCureDispatchDetails(doc, inhibition.getDispatchDetails());

        if (inhibition.getNotApplicableRemarks() != null
                && !inhibition.getNotApplicableRemarks().isBlank()) {
            addCcTitle(doc, "Inhibition Not Applicable");
            doc.add(new Paragraph(inhibition.getNotApplicableRemarks(), BODY_FONT));
        }
    }

    private void renderBellowRemovalDetails(Document doc, List<BellowRemovalDetail> rows)
            throws DocumentException {
        if (rows == null || rows.isEmpty()) return;

        addSection(doc, "Bellow Removal Details", "");
        PdfPTable table = createTable(new float[]{22, 22, 22, 34});
        addHeaders(table, "LOCATION", "FROM DATE & TIME", "TO DATE & TIME", "OBSERVATIONS");

        boolean alt = false;
        for (BellowRemovalDetail row : rows) {
            Color bg = alt ? ROW_ALT : WHITE;
            addCell(table, postCureDisplay(row.getLocation()), BODY_FONT, bg);
            addCell(table, str(row.getFromDateTime()), BODY_FONT, bg);
            addCell(table, str(row.getToDateTime()), BODY_FONT, bg);
            addCell(table, str(row.getObservations()), BODY_FONT, bg);
            alt = !alt;
        }
        doc.add(table);
        spacer(doc, 6);
    }

    private void renderPostCureEpoxyPreparation(
            Document doc, EpoxyPreparationDetails details, String title) throws DocumentException {
        if (details == null) return;

        addSection(doc, title, "");
        PdfPTable summary = createKeyValueTable();
        addKV(summary, "Batch No.", str(details.getBatchNo()));
        addKV(summary, "Date & Time of Preparation", str(details.getPreparationDateTime()));
        doc.add(summary);
        spacer(doc, 4);

        renderPostCureIngredients(doc, details.getParameters());
    }

    private void renderPostCureIngredients(Document doc, List<MixIngredient> rows)
            throws DocumentException {
        if (rows == null || rows.isEmpty()) return;

        PdfPTable table = createTable(new float[]{10, 30, 20, 20, 20});
        addHeaders(table, "SR NO.", "INGREDIENT", "MFG LOT", "PARTS BY WEIGHT", "QUANTITY (G)");

        boolean alt = false;
        for (MixIngredient row : rows) {
            boolean isTotal = "TOTAL".equalsIgnoreCase(str(row.getRowKey()))
                    || "Total Quantity".equalsIgnoreCase(str(row.getIngredient()));
            Font font = isTotal ? BODY_BOLD : BODY_FONT;
            Color bg = alt ? ROW_ALT : WHITE;

            addCell(table, str(row.getRowKey()), font, bg);
            addCell(table, str(row.getIngredient()), font, bg);
            addCell(table, str(row.getMfgLot()), font, bg);
            addCell(table, str(row.getPartsByWeight()), font, bg);
            addCell(table, str(row.getQuantityTaken()), font, bg);
            alt = !alt;
        }
        doc.add(table);
        spacer(doc, 6);
    }

    private void renderPostCureMixDetails(Document doc, MixDetails details, String title)
            throws DocumentException {
        if (details == null) return;

        addSection(doc, title, "");
        PdfPTable summary = createKeyValueTable();
        addKV(summary, "Batch No.", str(details.getBatchNo()));
        String mixDateLabel = title.toLowerCase(Locale.ROOT).contains("final mix")
                ? "Date of Final Mix"
                : "Date of Premix";
        addKV(summary, mixDateLabel, str(details.getPreparationDateTime()));
        doc.add(summary);
        spacer(doc, 4);

        renderPostCureIngredients(doc, details.getIngredients());
    }

    private void renderPostCureQualification(
            Document doc, QualificationDetails details, String title) throws DocumentException {
        if (details == null) return;

        addSection(doc, title, "");
        PdfPTable summary = createKeyValueTable();
        addKV(summary, "Batch No.", str(details.getBatchNo()));
        addKV(summary, "Date & Time of Preparation", str(details.getPreparationDateTime()));

        // QC Report – extract file name properly
        String qcReport = extractFileNames(details.getQcReport());
        addKV(summary, "QC Report", qcReport);
        doc.add(summary);
        spacer(doc, 4);

        if (details.getParameters() == null || details.getParameters().isEmpty()) return;

        PdfPTable table = createTable(new float[]{40, 30, 30});
        addHeaders(table, "PARAMETER", "SPECIFICATION", "RESULT");

        boolean alt = false;
        for (QualificationParameter row : details.getParameters()) {
            Color bg = alt ? ROW_ALT : WHITE;
            addCell(table, str(row.getParameter()), BODY_FONT, bg);
            addCell(table, formatPostCureSpecification(row.getSpecification()), BODY_FONT, bg);
            addCell(table, str(row.getResult()), BODY_FONT, bg);
            alt = !alt;
        }
        doc.add(table);
        spacer(doc, 6);
    }

    private void renderLooseFlapFillingDetails(Document doc, List<FillingDetail> rows)
            throws DocumentException {
        if (rows == null || rows.isEmpty()) return;

        addSection(doc, "LF Epoxy Filling Details", "");
        PdfPTable table = createTable(new float[]{18, 18, 18, 18, 28});
        addHeaders(table, "LOCATION", "FROM DATE & TIME", "TO DATE & TIME", "QTY FILLED", "OBSERVATIONS");

        boolean alt = false;
        for (FillingDetail row : rows) {
            Color bg = alt ? ROW_ALT : WHITE;
            addCell(table, postCureDisplay(row.getLocation()), BODY_FONT, bg);
            addCell(table, str(row.getFromDateTime()), BODY_FONT, bg);
            addCell(table, str(row.getToDateTime()), BODY_FONT, bg);
            addCell(table, str(row.getQuantityFilled()), BODY_FONT, bg);
            addCell(table, str(row.getObservations()), BODY_FONT, bg);
            alt = !alt;
        }
        doc.add(table);
        spacer(doc, 6);
    }

    private void renderInhibitorBatchDetails(Document doc, InhibitorBatchDetails details)
            throws DocumentException {
        if (details == null) return;

        addSection(doc, "Inhibitor Batch Information", "");
        PdfPTable table = createKeyValueTable();
        addKV(table, "Batch No.", str(details.getBatchNo()));
        addKV(table, "Batch Size (g)", str(details.getBatchSize()));
        doc.add(table);
        spacer(doc, 6);
    }

    private void renderInhibitionApplicationDetails(Document doc, List<ApplicationDetail> rows)
            throws DocumentException {
        if (rows == null || rows.isEmpty()) return;

        addSection(doc, "Inhibition Application Details", "");
        PdfPTable table = createTable(new float[]{18, 18, 18, 18, 28});
        addHeaders(table, "LOCATION", "FROM DATE & TIME", "TO DATE & TIME", "QTY APPLIED", "OBSERVATIONS");

        boolean alt = false;
        for (ApplicationDetail row : rows) {
            Color bg = alt ? ROW_ALT : WHITE;
            addCell(table, postCureDisplay(row.getLocation()), BODY_FONT, bg);
            addCell(table, str(row.getFromDateTime()), BODY_FONT, bg);
            addCell(table, str(row.getToDateTime()), BODY_FONT, bg);
            addCell(table, str(row.getQuantityApplied()), BODY_FONT, bg);
            addCell(table, str(row.getObservations()), BODY_FONT, bg);
            alt = !alt;
        }
        doc.add(table);
        spacer(doc, 6);
    }

    private void renderPostCureDispatchDetails(Document doc, DispatchDetails details)
            throws DocumentException {
        if (details == null) return;

        addSection(doc, "Dispatch Details", "");
        PdfPTable table = createKeyValueTable();
        addKV(table, "Date & Time of Dispatch", str(details.getDispatchDateTime()));
        addKV(table, "Dispatch Station", castingStationName(details.getDispatchStation()));
        doc.add(table);
    }

    private String castingStationName(String stationCode) {
        if (stationCode == null || stationCode.isBlank()) return "-";
        if (castingStationRepository == null) return stationCode;
        return castingStationRepository.findByStationCode(stationCode.trim())
                .map(CastingStation::getStationName)
                .filter(name -> name != null && !name.isBlank())
                .orElse(stationCode);
    }


    private String postCureMotorStatus(
            PostCureResponse data,
            PostCureMotor motor,
            String motorId) {
        if (motor.getMotorSubmissionStatus() != null) {
            return workflowStatusLabel(motor.getMotorSubmissionStatus().toString());
        }
        if (data.getMotorStatuses() != null) {
            for (MotorStatusResponse status : data.getMotorStatuses()) {
                if (motorId.equals(str(status.getMotorId()))) {
                    return workflowStatusLabel(str(status.getMotorSubmissionStatus()));
                }
            }
        }
        return "—";
    }

    private String postCureDisplay(Object value) {
        String text = str(value);
        if ("—".equals(text)) return text;
        if ("HE_SIDE".equalsIgnoreCase(text)) return "HE Side";
        if ("NE_SIDE".equalsIgnoreCase(text)) return "NE Side";
        String label = text.replace('_', ' ').trim().toLowerCase();
        return label.isEmpty()
                ? "—"
                : Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }

    private String formatPostCureSpecification(Specification specification) {
        if (specification == null) return "—";

        BigDecimal minValue = specification.getMin();
        BigDecimal maxValue = specification.getMax();
        String range;
        if (minValue != null && maxValue != null) {
            range = formatPdfNumber(minValue) + "–" + formatPdfNumber(maxValue);
        } else if (minValue != null) {
            range = "Min: " + formatPdfNumber(minValue);
        } else if (maxValue != null) {
            range = "Max: " + formatPdfNumber(maxValue);
        } else {
            range = "";
        }

        String unit = specification.getUnit() == null ? "" : specification.getUnit().trim();
        if (range.isEmpty()) return unit.isEmpty() ? "—" : unit;
        return unit.isEmpty() ? range : range + " " + unit;
    }

    private String formatPdfNumber(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private void renderSubscaleProcessing(Document doc, SubscaleProcessingDetailsResponse data)
            throws DocumentException {

        if (data == null) {
            doc.add(new Paragraph("No subscale processing details available.", BODY_FONT));
            return;
        }

        // ------------------------------------------------------------------
        // 1. FORM SUMMARY
        // ------------------------------------------------------------------
        addSection(doc, "Subscale Processing", "");

        PdfPTable summary = createTable(new float[]{25, 25, 25, 25});
        addInfoStripCell(summary, "Batch ID", str(data.getBatchId()));
        addInfoStripCell(summary, "Batch Type", str(data.getBatchType()));
        addInfoStripCell(summary, "Created By", str(data.getCreatedBy()));
        addInfoStripCell(summary, "Submitted By",
                data.getSubmittedBy() != null ? str(data.getSubmittedBy().getFullName()) : "-");

        // Status
        PdfPCell statusCell = new PdfPCell();
        statusCell.setColspan(4);
        statusCell.setBorderColor(BORDER_LIGHT);
        statusCell.setPadding(6);
        statusCell.setBackgroundColor(ROW_ALT);
        Paragraph sp = new Paragraph();
        sp.add(new Chunk("Form Status: ", BODY_BOLD));
        sp.add(createStatusPhrase(data.getStatus() != null ? data.getStatus().toString() : "-"));
        statusCell.addElement(sp);
        summary.addCell(statusCell);

        doc.add(summary);
        spacer(doc, 12);

        // ------------------------------------------------------------------
        // 2. HARDWARE PREPARATION DETAILS
        // ------------------------------------------------------------------
        if (data.getHardwarePreparationDetails() != null) {
            HardwarePreparationDetailsDTO hw = data.getHardwarePreparationDetails();
            addSection(doc, "Hardware Preparation Details", "");

            // Counts
            PdfPTable counts = createKeyValueTable();
            if (hw.getArticles() != null && !hw.getArticles().isEmpty()) {
                for (HardwareArticleSelectionDTO article : hw.getArticles()) {
                    if (article == null) continue;
                    addKV(counts, str(article.getSubscaleArticleName()) + " – Articles",
                            str(article.getNoOfArticles()));
                    addKV(counts, str(article.getSubscaleArticleName()) + " – Parts",
                            str(article.getNoOfParts()));
                }
            } else {
                addKV(counts, "No. of Articles (2 Kg Bem)", str(hw.getNumberOf2KgBems()));
                addKV(counts, "No. of Parts (2 Kg Bem)", str(hw.getNumberOf2KgBems())); // adjust if separate field
                addKV(counts, "No. of Articles (10 Kg Bem)", str(hw.getNumberOf10KgBems()));
                addKV(counts, "No. of Parts (10 Kg Bem)", str(hw.getNumberOf10KgBems()));
                addKV(counts, "No. of Articles (20 Kg Bem)", str(hw.getNumberOf20KgBems()));
                addKV(counts, "No. of Parts (20 Kg Bem)", str(hw.getNumberOf20KgBems()));
                addKV(counts, "No. of Articles (40 Kg Bem)", str(hw.getNumberOf40KgBems()));
                addKV(counts, "No. of Parts (40 Kg Bem)", str(hw.getNumberOf40KgBems()));
                addKV(counts, "No. of Wheel Peel", str(hw.getNumberOfWheelPeels()));
                addKV(counts, "No of Cartoons", str(hw.getNumberOfCartoons()));
            }
            addKV(counts, "Liner Type", str(hw.getLinerType()));
            addKV(counts, "Batch No.", str(hw.getLinerBatchNo()));
            addKV(counts, "Batch Date", str(hw.getLinerBatchDate()));
            doc.add(counts);
            spacer(doc, 6);
        }

        // Article Type table
        if (data.getHardwarePreparationTable() != null && !data.getHardwarePreparationTable().isEmpty()) {
            addSection(doc, "Article Type", "");
            PdfPTable table = createTable(new float[]{7, 16, 14, 11, 11, 11, 12, 18, 18});
            addHeaders(table,
                    "SR NO",
                    "ARTICLE TYPE",
                    "RUBBER MATERIAL",
                    "SLEEVE NO.",
                    "MOULD NO.",
                    "SIZE (MM)",
                    "THICKNESS (MM)",
                    "LINER APPLIED",
                    "OBSERVATIONS");

            boolean alt = false;
            int sr = 1;
            for (HardwarePreparationTableDTO r : data.getHardwarePreparationTable()) {
                Color bg = alt ? ROW_ALT : WHITE;
                addCell(table, String.valueOf(sr++), BODY_FONT, bg);
                addCell(table, str(r.getArticleType()), BODY_FONT, bg);
                addCell(table, str(r.getRubberMaterial()), BODY_FONT, bg);
                addCell(table, str(r.getSleeveNo()), BODY_FONT, bg);
                addCell(table, str(r.getMouldNo()), BODY_FONT, bg);
                addCell(table, str(r.getLengthMm()), BODY_FONT, bg);
                addCell(table, str(r.getThicknessMm()), BODY_FONT, bg);
                String linerObs = str(r.getLinerApplied());
                String obs = str(r.getObservations());
//                if (r.getObservations() != null && !r.getObservations().isBlank()) {
//                    linerObs += " / " + r.getObservations();
//                }
                addCell(table, linerObs, BODY_FONT, bg);
                addCell(table, obs, BODY_FONT, bg);
                alt = !alt;
            }
            doc.add(table);
            spacer(doc, 9);
        }

        // ------------------------------------------------------------------
        // 3. CASTING DETAILS
        // ------------------------------------------------------------------
        if (data.getCastingDetails() != null) {
            CastingDetailsDTO casting = data.getCastingDetails();
            addSection(doc, "Casting Details", "");

            PdfPTable info = createKeyValueTable();
            addKV(info, "Date & Time of Casting", str(casting.getDateOfCastingDateTime()));
            doc.add(info);
            spacer(doc, 4);

            List<CastingTableDTO> rows = casting.getCastingTable();
            if (rows != null && !rows.isEmpty()) {
                PdfPTable table = createTable(new float[]{6, 13, 13, 11, 11, 10, 10, 10, 16});
                addHeaders(table,
                        "SR NO",
                        "ARTICLE TYPE",
                        "ARTICLE NO / BEM NO",
                        "MOULD NO",
                        "CASTING PIT NO",
                        "CASTING START DATE & TIME",
                        "CASTING END DATE & TIME",
                        "VACUUM LEVEL",
                        "REMARKS");

                boolean alt = false;
                int sr = 1;
                for (CastingTableDTO r : rows) {
                    Color bg = alt ? ROW_ALT : WHITE;
                    addCell(table, String.valueOf(sr++), BODY_FONT, bg);
                    addCell(table, str(r.getArticleType()), BODY_FONT, bg);
                    addCell(table, str(r.resolveBemIdentity()), BODY_FONT, bg);
                    addCell(table, str(r.getBemMouldNo()), BODY_FONT, bg);
                    addCell(table, str(r.getCastingPitNo()), BODY_FONT, bg);
                    addCell(table, str(r.getCastingStartDateTime()), BODY_FONT, bg);
                    addCell(table, str(r.getCastingEndDateTime()), BODY_FONT, bg);
                    addCell(table, str(r.getVacuumLevel()), BODY_FONT, bg);
                    addCell(table, str(r.getRemarks()), BODY_FONT, bg);
                    alt = !alt;
                }
                doc.add(table);
                spacer(doc, 8);
            }
        }

        // ------------------------------------------------------------------
        // 4. CURING DETAILS
        // ------------------------------------------------------------------
        if (data.getCuringDetails() != null
                && data.getCuringDetails().getCuringTable() != null
                && !data.getCuringDetails().getCuringTable().isEmpty()) {

            addSection(doc, "Curing Details", "");
            PdfPTable table = createTable(new float[]{5, 11, 11, 10, 10, 8, 8, 8, 10, 9, 10});
            addHeaders(table,
                    "SR NO",
                    "ARTICLE TYPE",
                    "ARTICLE NO / BEM NO",
                    "CURING START DATE & TIME",
                    "CURING END DATE & TIME",
                    "OVEN NO",
                    "TEMP (°C)",
                    "HARDNESS",
                    "DECORING DATE & TIME",
                    "DECORING LOAD",
                    "OBSERVATIONS");

            boolean alt = false;
            int sr = 1;
            for (CuringTableDTO r : data.getCuringDetails().getCuringTable()) {
                Color bg = alt ? ROW_ALT : WHITE;
                addCell(table, String.valueOf(sr++), BODY_FONT, bg);
                addCell(table, str(r.getArticleType()), BODY_FONT, bg);
                addCell(table, str(r.getBemNo()), BODY_FONT, bg);
                addCell(table, str(r.getCuringStartDateTime()), BODY_FONT, bg);
                addCell(table, str(r.getCuringEndDateTime()), BODY_FONT, bg);
                addCell(table, str(r.getOvenNo()), BODY_FONT, bg);
                addCell(table, str(r.getTemperature()), BODY_FONT, bg);
                addCell(table, str(r.getHardness()), BODY_FONT, bg);
                addCell(table, str(r.getDecoringDateTime()), BODY_FONT, bg);
                addCell(table, str(r.getDecoringLoad()), BODY_FONT, bg);
                addCell(table, str(r.getGrainSurfaceObservations()), BODY_FONT, bg);
                alt = !alt;
            }
            doc.add(table);
            spacer(doc, 8);
        }

        // ------------------------------------------------------------------
        // 5. NDT DETAILS
        // ------------------------------------------------------------------
        if (data.getNdtDetails() != null
                && data.getNdtDetails().getNdtTable() != null
                && !data.getNdtDetails().getNdtTable().isEmpty()) {

            addSection(doc, "NDT Details", "");
            PdfPTable table = createTable(new float[]{8, 22, 22, 20, 28});
            addHeaders(table, "SR NO", "ARTICLE TYPE", "ARTICLE NO / BEM NO", "DATE & TIME OF NDT", "OBSERVATIONS");

            boolean alt = false;
            int sr = 1;
            for (NdtTableDTO r : data.getNdtDetails().getNdtTable()) {
                Color bg = alt ? ROW_ALT : WHITE;
                addCell(table, String.valueOf(sr++), BODY_FONT, bg);
                addCell(table, str(r.getArticleType()), BODY_FONT, bg);
                addCell(table, str(r.getBemNo()), BODY_FONT, bg);
                addCell(table, str(r.getDateOfNdtDateTime()), BODY_FONT, bg);
                addCell(table, str(r.getObservations()), BODY_FONT, bg);
                alt = !alt;
            }
            doc.add(table);
            spacer(doc, 8);
        }

        // ------------------------------------------------------------------
        // 6. TRIMMING DETAILS
        // ------------------------------------------------------------------
        if (data.getTrimmingDetails() != null
                && data.getTrimmingDetails().getTrimmingTable() != null
                && !data.getTrimmingDetails().getTrimmingTable().isEmpty()) {

            addSection(doc, "Trimming Details", "");
            PdfPTable table = createTable(new float[]{4, 8, 8, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 8});
            addHeaders(table,
                    "SR",
                    "ARTICLE TYPE",
                    "BEM NO",
                    "HE SIDE OD",
                    "HE PORT IN",
                    "HE PORT OUT",
                    "HE BEFORE IN",
                    "HE BEFORE OUT",
                    "NE SIDE OD",
                    "NE PORT IN",
                    "NE PORT OUT",
                    "NE WEB IN",
                    "NE WEB OUT",
                    "LENGTH BEFORE INHIB");

            boolean alt = false;
            int sr = 1;
            for (TrimmingTableDTO r : data.getTrimmingDetails().getTrimmingTable()) {
                Color bg = alt ? ROW_ALT : WHITE;
                String srNo = String.valueOf(sr++);
                String articleType = str(r.getArticleType());
                String bemNo = str(r.getBemNo());
                String lengthBefore = str(r.getLengthBeforeInhibition());

                addCell(table, srNo, BODY_FONT, bg);
                addCell(table, articleType, BODY_FONT, bg);
                addCell(table, bemNo, BODY_FONT, bg);
                addCell(table, str(r.getHeOd()), BODY_FONT, bg);
                addCell(table, str(r.getHePortInner()), BODY_FONT, bg);
                addCell(table, str(r.getHePortOuter()), BODY_FONT, bg);
                addCell(table, str(r.getHeBeforeInhibitionInner()), BODY_FONT, bg);
                addCell(table, str(r.getHeBeforeInhibitionOuter()), BODY_FONT, bg);
                addCell(table, str(r.getNeOd()), BODY_FONT, bg);
                addCell(table, str(r.getNePortInner()), BODY_FONT, bg);
                addCell(table, str(r.getNePortOuter()), BODY_FONT, bg);
                addCell(table, str(r.getNeWebInner()), BODY_FONT, bg);
                addCell(table, str(r.getNeWebOuter()), BODY_FONT, bg);
                addCell(table, lengthBefore, BODY_FONT, bg);
                alt = !alt;
            }
            doc.add(table);
            spacer(doc, 8);
        }

        // ------------------------------------------------------------------
        // 7. INHIBITION DETAILS
        // ------------------------------------------------------------------
        if (data.getInhibitionDetails() != null) {
            InhibitionDetailsDTO inh = data.getInhibitionDetails();
            addSection(doc, "Inhibition Details", "");

            PdfPTable info = createKeyValueTable();
            addKV(info, "IR Batch No", str(inh.getIrBatchNo()));
            addKV(info, "Date & Time of Manufacturing", str(inh.getDateOfManufacturingDateTime()));
            doc.add(info);
            spacer(doc, 4);

            List<InhibitionTableDTO> rows = inh.getInhibitionTable();
            if (rows != null && !rows.isEmpty()) {
                PdfPTable table = createTable(new float[]{5, 11, 11, 11, 11, 11, 10, 10, 10, 10});
                addHeaders(table,
                        "SR",
                        "ARTICLE TYPE",
                        "BEM NO",
                        "LINER COATED WT (A)",
                        "WT BEFORE INHIB (B)",
                        "WT AFTER INHIB (C)",
                        "IR APPLIED",
                        "PROPELLANT WT (C-B-A)",
                        "DATE & TIME OF APPLICATION",
                        "REMARKS");

                boolean alt = false;
                int sr = 1;
                for (InhibitionTableDTO r : rows) {
                    Color bg = alt ? ROW_ALT : WHITE;
                    LocalDateTime rowDate = r.getDateOfApplicationDateTime();
                    addCell(table, String.valueOf(sr++), BODY_FONT, bg);
                    addCell(table, str(r.getArticleType()), BODY_FONT, bg);
                    addCell(table, str(r.getBemNo()), BODY_FONT, bg);
                    addCell(table, str(r.getLinerCoatedSleeveWeight()), BODY_FONT, bg);
                    addCell(table, str(r.getWeightBeforeInhibition()), BODY_FONT, bg);
                    addCell(table, str(r.getWeightAfterInhibition()), BODY_FONT, bg);
                    addCell(table, str(r.getIrAppliedWeight()), BODY_FONT, bg);
                    addCell(table, str(computeInhibitionPropellantWeight(r)), BODY_FONT, bg);
                    addCell(table, str(rowDate), BODY_FONT, bg);
                    addCell(table, str(r.getRemarks()), BODY_FONT, bg);
                    alt = !alt;
                }
                doc.add(table);
                spacer(doc, 8);
            }
        }

        // ------------------------------------------------------------------
        // 8. STATIC TESTING (if present)
        // ------------------------------------------------------------------
        if (data.getStaticTestingDetails() != null
                && data.getStaticTestingDetails().getStaticTestingTable() != null
                && !data.getStaticTestingDetails().getStaticTestingTable().isEmpty()) {

            addSection(doc, "Static Testing Details", "");
            PdfPTable table = createTable(new float[]{12, 12, 10, 10, 10, 12, 12, 10, 12});
            addHeaders(table,
                    "BEM ID", "PROPELLANT MASS", "Dt (mm)", "WEB THICK",
                    "n-VALUE", "AVG PRES", "AVG THRUST", "BURN RATE", "GRAPH");

            boolean alt = false;
            for (StaticTestingTableDTO r : data.getStaticTestingDetails().getStaticTestingTable()) {
                if (r == null) continue;
                Color bg = alt ? ROW_ALT : WHITE;
                addCell(table, str(r.getBemNo()), BODY_FONT, bg);
                addCell(table, str(r.getPropellantMass()), BODY_FONT, bg);
                addCell(table, str(r.getDt()), BODY_FONT, bg);
                addCell(table, str(r.getWebThickness()), BODY_FONT, bg);
                addCell(table, str(r.getNValue()), BODY_FONT, bg);
                addCell(table, str(r.getPressureAvg()), BODY_FONT, bg);
                addCell(table, str(r.getThrustAvg()), BODY_FONT, bg);
                addCell(table, str(r.getBurnRate()), BODY_FONT, bg);
                addCell(table, extractFileNames(r.getGraph()), BODY_FONT, bg);
                alt = !alt;
            }
            doc.add(table);
            spacer(doc, 8);
        }

        // ------------------------------------------------------------------
        // 9. MECHANICAL PROPERTIES (if present)
        // ------------------------------------------------------------------
        if (data.getMechanicalInterfaceProperties() != null
                && data.getMechanicalInterfaceProperties().getMechanicalPropertiesTable() != null
                && !data.getMechanicalInterfaceProperties().getMechanicalPropertiesTable().isEmpty()) {

            addSection(doc, "Mechanical Properties", "");
            PdfPTable table = createTable(new float[]{12, 10, 12, 10, 8, 8, 12, 10, 18});
            addHeaders(table,
                    "BEM ID", "TS (KSC)", "ELONGATION %", "MODULUS",
                    "SBS", "TBS", "PEEL STRENGTH", "DENSITY", "SIGN OFF");

            boolean alt = false;
            for (MechanicalPropertiesTableDTO r :
                    data.getMechanicalInterfaceProperties().getMechanicalPropertiesTable()) {
                Color bg = alt ? ROW_ALT : WHITE;
                addCell(table, str(r.getBemNo()), BODY_FONT, bg);
                addCell(table, str(r.getTs()), BODY_FONT, bg);
                addCell(table, str(r.getElongation()), BODY_FONT, bg);
                addCell(table, str(r.getModulus()), BODY_FONT, bg);
                addCell(table, str(r.getSbs()), BODY_FONT, bg);
                addCell(table, str(r.getTbs()), BODY_FONT, bg);
                addCell(table, str(r.getPeelStrength()), BODY_FONT, bg);
                addCell(table, str(r.getDensity()), BODY_FONT, bg);
                addCell(table, str(r.getActor()), BODY_FONT, bg);
                alt = !alt;
            }
            doc.add(table);
        }
    }

    private String subscaleYesNo(Boolean value) {
        if (value == null) return "—";
        return value ? "Yes" : "No";
    }

    private void renderTrimmingDetails(Document doc, TrimmingDetailsResponse data)
            throws DocumentException {

        if (data == null || data.getMotors() == null || data.getMotors().isEmpty()) {
            doc.add(new Paragraph("No operational metrics found for Trimming Form.", BODY_FONT));
            return;
        }

        // ------------------------------------------------------------------
        // 1. FORM SUMMARY
        // ------------------------------------------------------------------
        addSection(doc, "Trimming", "");

        PdfPTable summary = createTable(new float[]{25, 25, 25, 25});
        addInfoStripCell(summary, "Batch ID", str(data.getBatchId()));
        addInfoStripCell(summary, "Batch Type", str(data.getBatchType()));
        addInfoStripCell(summary, "Submitted Date & Time", str(data.getSubmittedAt()));
        addInfoStripCell(summary, "Submitted By",
                data.getSubmittedByDetails() != null
                        ? str(data.getSubmittedByDetails().getFullName())
                        : str(data.getSubmittedBy()));

        // Status
        PdfPCell statusCell = new PdfPCell();
        statusCell.setColspan(4);
        statusCell.setBorderColor(BORDER_LIGHT);
        statusCell.setPadding(6);
        statusCell.setBackgroundColor(ROW_ALT);
        Paragraph sp = new Paragraph();
        sp.add(new Chunk("Form Status: ", BODY_BOLD));
        sp.add(createStatusPhrase(data.getStatus() != null ? data.getStatus().toString() : "-"));
        statusCell.addElement(sp);
        summary.addCell(statusCell);

        doc.add(summary);
        spacer(doc, 12);

        // ------------------------------------------------------------------
        // 2. MOTORS
        // ------------------------------------------------------------------
        for (MotorDTO motor : data.getMotors()) {
            if (motor == null) continue;

            String motorId = str(motor.getMotorId());
            String stage = motor.getMotorStage() != null ? String.valueOf(motor.getMotorStage()) : "-";
            String receivedAt = str(motor.getMotorReceivedDateTime());
            MotorStatusResponse savedMotorStatus = findMotorStatus(
                    data.getMotorStatuses(), motor.getMotorId());
            Object motorApprovalStatus = savedMotorStatus != null
                    ? savedMotorStatus.getMotorSubmissionStatus()
                    : motor.getMotorSubmissionStatus();

            // Motor header + coloured status
            Paragraph motorTitle = new Paragraph();
            motorTitle.add(new Chunk("Motor · " + motorId + "   ", SECTION_FONT));
            motorTitle.add(createStatusPhrase(
                    motorApprovalStatus != null ? motorApprovalStatus.toString() : "-"));
            motorTitle.setSpacingBefore(8);
            motorTitle.setSpacingAfter(4);
            doc.add(motorTitle);

            // Meta
            PdfPTable meta = createKeyValueTable();
            addKV(meta, "Motor Stage", "Stage " + stage);
            addKV(meta, "Motor Received Date & Time", receivedAt);
            doc.add(meta);
            spacer(doc, 6);

            if (motor.getSections() == null || motor.getSections().isEmpty()) {
                continue;
            }

            for (SectionDTO<?> section : motor.getSections()) {
                if (section == null) continue;

                // ----------------------------------------------------------
                // TRIMMING DETAILS
                // ----------------------------------------------------------
                if (section instanceof TrimmingDetailsSectionDTO trimSection) {
                    List<TrimmingDetailsSectionDTO.TrimmingDetailData> detailsList =
                            trimSection.getSectionData();

                    if (detailsList != null && !detailsList.isEmpty()) {
                        addSection(doc, "Trimming Details", "");

                        PdfPTable table = createTable(new float[]{22, 15, 15, 14, 14, 20, 14, 14, 14});
                        addHeaders(table,
                                "MACHINE DETAILS",
                                "START DATE & TIME",
                                "COMPLETION DATE & TIME",
                                "ARBOR SIZE",
                                "CUTTER SIZE",
                                "INGEST SIZE", "MOTOR TRUING", "ARBOR TRUING",
                                "REMARKS");

                        boolean alt = false;
                        for (TrimmingDetailsSectionDTO.TrimmingDetailData r : detailsList) {
                            Color bg = alt ? ROW_ALT : WHITE;
                            addCell(table, str(r.getMachineDetails()), BODY_FONT, bg);
                            addCell(table, str(r.getStartDatetime()), BODY_FONT, bg);
                            addCell(table, str(r.getCompletionDatetime()), BODY_FONT, bg);
                            addCell(table, str(r.getArborSize()), BODY_FONT, bg);
                            addCell(table, str(r.getCutterSize()), BODY_FONT, bg);
                            addCell(table, str(r.getIngestSize()), BODY_FONT, bg);
                            addCell(table, str(r.getMotorTruing()), BODY_FONT, bg);
                            addCell(table, str(r.getArborTuring()), BODY_FONT, bg);
                            addCell(table, str(r.getRemarks()), BODY_FONT, bg);
                            alt = !alt;
                        }
                        doc.add(table);
                        spacer(doc, 8);
                    }
                }

                // ----------------------------------------------------------
                // DIMENSIONS AFTER TRIMMING
                // ----------------------------------------------------------
                else if (section instanceof TrimmingCommonFormatSectionDTO commonFormatSection) {
                    List<TrimmingCommonFormatSectionDTO.CommonFormatData> commonDataList =
                            commonFormatSection.getSectionData();

                    if (commonDataList != null && !commonDataList.isEmpty()) {
                        addSection(doc, "Dimensions After Trimming", "");

                        // Discover reading keys (R2T, R2B, R1R, R1L ...)
                        LinkedHashSet<String> readingKeys = new LinkedHashSet<>();
                        for (var cfData : commonDataList) {
                            if (cfData.getStages() == null) continue;
                            for (var stageData : cfData.getStages()) {
                                if (stageData.getReadings() != null) {
                                    readingKeys.addAll(stageData.getReadings().keySet());
                                }
                            }
                        }

                        // Build column widths
                        int readingCount = Math.max(readingKeys.size(), 1);
                        float[] widths = new float[3 + readingCount];
                        widths[0] = 22; // Parameter
                        widths[1] = 16; // Stage
                        widths[2] = 14; // Specification
                        float readWidth = 48f / readingCount;
                        for (int i = 3; i < widths.length; i++) {
                            widths[i] = readWidth;
                        }

                        PdfPTable table = createTable(widths);

                        // Headers
                        List<String> headers = new ArrayList<>();
                        headers.add("PARAMETER");
                        headers.add("STAGE");
                        headers.add("SPECIFICATION");
                        headers.addAll(readingKeys);
                        addHeaders(table, headers.toArray(new String[0]));

                        boolean alt = false;
                        for (var cfData : commonDataList) {
                            if (cfData.getStages() == null) continue;

                            for (var stageData : cfData.getStages()) {
                                Color bg = alt ? ROW_ALT : WHITE;
                                addCell(table, str(cfData.getParameterName()), BODY_FONT, bg);
                                addCell(table, str(stageData.getStageName()), BODY_FONT, bg);
                                addCell(table, str(stageData.getSpecification()), BODY_FONT, bg);

                                Map<String, String> readings = stageData.getReadings() != null
                                        ? stageData.getReadings()
                                        : Collections.emptyMap();

                                for (String key : readingKeys) {
                                    Object val = readings.get(key);
                                    addCell(table, val != null ? str(val) : "-", BODY_FONT, bg);
                                }
                                alt = !alt;
                            }
                        }
                        doc.add(table);
                        spacer(doc, 8);
                    }
                }

                // ----------------------------------------------------------
                // DRAWING
                // ----------------------------------------------------------
                else if (section instanceof TrimmingDrawingSectionDTO drawingSection) {
                    List<TrimmingDrawingSectionDTO.TrimmingDrawingData> drawingList =
                            drawingSection.getSectionData();

                    if (drawingList != null && !drawingList.isEmpty()) {
                        addSection(doc, "Drawing", "");

                        for (var d : drawingList) {
                            if (d == null) continue;
                            PdfPTable table = createKeyValueTable();
                            addKV(table, "Description", str(d.getDescription()));
                            doc.add(table);
                            addTrimmingDrawingImages(doc, d.getDrawingFile());
                        }
                    }
                }

                // ----------------------------------------------------------
                // REMARKS & ATTACHMENTS
                // ----------------------------------------------------------
                else if (section instanceof TrimmingRemarksSectionDTO remarksSection) {
                    List<TrimmingRemarksSectionDTO.TrimmingRemarksData> remarksList =
                            remarksSection.getSectionData();

                    if (remarksList != null && !remarksList.isEmpty()) {
                        addSection(doc, "Remarks & Attachments", "");

                        PdfPTable table = createKeyValueTable();
                        for (var r : remarksList) {
                            addKV(table, "Remarks", str(r.getRemarks()));

                            String files = "-";
                            if (r.getReportFile() != null && !r.getReportFile().isEmpty()) {
                                files = r.getReportFile().stream()
                                        .filter(Objects::nonNull)
                                        .map(f -> f.getFileName() != null && !f.getFileName().isBlank()
                                                ? f.getFileName()
                                                : str(f.getFileId()))
                                        .filter(s -> !s.isBlank())
                                        .collect(Collectors.joining(", "));
                                if (files.isBlank()) files = "-";
                            }
                            addKV(table, "Attachments", files);
                        }
                        doc.add(table);
                    }
                }
            }
            spacer(doc, 12);
        }
    }

    private void addTrimmingDrawingImages(Document doc, List<DocumentDto> drawingFiles)
            throws DocumentException {
        if (drawingFiles == null || drawingFiles.isEmpty()) {
            return;
        }
        for (DocumentDto file : drawingFiles) {
            if (file == null) continue;
            boolean embedded = tryEmbedDocumentImage(doc, file);
            if (!embedded) {
                PdfPTable table = createKeyValueTable();
                String label = file.getFileName() != null && !file.getFileName().isBlank()
                        ? file.getFileName()
                        : str(file.getFileId());
                addKV(table, "Drawing File", label.isBlank() ? "-" : label);
                doc.add(table);
            }
        }
        spacer(doc, 6);
    }

    /**
     * Embeds a drawing image directly under the Drawing section.
     * Uses a table cell so the image stays in document order (not deferred past Remarks).
     */
    private boolean tryEmbedDocumentImage(Document doc, DocumentDto file) {
        if (file == null) return false;
        String path = file.getFileUrl();
        if (path == null || path.isBlank()) return false;

        String mime = file.getMimeType() != null ? file.getMimeType().toLowerCase(Locale.ROOT) : "";
        String lowerPath = path.toLowerCase(Locale.ROOT);
        boolean looksLikeImage = mime.startsWith("image/")
                || lowerPath.endsWith(".png")
                || lowerPath.endsWith(".jpg")
                || lowerPath.endsWith(".jpeg")
                || lowerPath.endsWith(".gif")
                || lowerPath.endsWith(".webp")
                || lowerPath.endsWith(".bmp");
        if (!looksLikeImage && !mime.isBlank()) {
            return false;
        }

        try {
            java.nio.file.Path imagePath = java.nio.file.Paths.get(path);
            if (!java.nio.file.Files.isRegularFile(imagePath)) {
                return false;
            }
            com.lowagie.text.Image image =
                    com.lowagie.text.Image.getInstance(imagePath.toAbsolutePath().toString());
            float maxWidth = doc.right() - doc.left();
            // Cap height so the drawing usually stays on the same page as its section heading.
            float maxHeight = Math.min(PageSize.A4.getHeight() * 0.45f, 380f);
            image.scaleToFit(maxWidth, maxHeight);

            PdfPTable imageTable = new PdfPTable(1);
            imageTable.setWidthPercentage(100f);
            imageTable.setSpacingBefore(4f);
            imageTable.setSpacingAfter(6f);
            imageTable.setKeepTogether(true);

            PdfPCell imageCell = new PdfPCell(image, false);
            imageCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            imageCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            imageCell.setPadding(2f);
            imageTable.addCell(imageCell);

            doc.add(imageTable);
            return true;
        } catch (Exception e) {
            log.warn("Could not embed trimming drawing image fileId={}: {}",
                    file.getFileId(), e.getMessage());
            return false;
        }
    }

    private List<Map<String, Object>> trimmingOperationRows(Map<String, Object> motor) {
        List<Map<String, Object>> direct = trimmingMapList(motor.get("trimmingOperationDetails"));
        if (!direct.isEmpty()) return direct;

        List<Map<String, Object>> sectionData = trimmingSectionData(motor, "TRIMMING_DETAILS");
        return unwrapTrimmingRows(sectionData, "TRIMMING_DETAILS");
    }

    private List<Map<String, Object>> trimmingMeasurementRows(Map<String, Object> motor) {
        List<Map<String, Object>> direct = trimmingMapList(motor.get("trimmingMeasurementDetails"));
        if (!direct.isEmpty()) return flattenTrimmingMeasurements(direct);

        List<Map<String, Object>> legacyDimensions = trimmingSectionData(
                motor, "DIMENSIONS_AFTER_TRIMMING");
        List<Map<String, Object>> rows = unwrapTrimmingRows(
                legacyDimensions, "DIMENSION_TABLE");
        if (!rows.isEmpty()) return flattenTrimmingMeasurements(rows);

        return flattenTrimmingMeasurements(
                trimmingSectionData(motor, "TRIMMING_COMMON_FORMAT"));
    }

    private Map<String, Object> trimmingRemarks(Map<String, Object> motor) {
        Map<String, Object> direct = trimmingMap(trimmingValue(
                motor, "trimmingRemarks", "remarksAndAttachments", "remarksAttachments"));
        if (!direct.isEmpty()) return direct;

        List<Map<String, Object>> sectionData = trimmingSectionData(motor, "TRIMMING_REMARKS");
        if (sectionData.isEmpty()) {
            sectionData = trimmingSectionData(motor, "REMARKS_AND_ATTACHMENTS");
        }
        return sectionData.isEmpty() ? Collections.emptyMap() : sectionData.getFirst();
    }

    private Map<String, Object> trimmingDrawing(Map<String, Object> motor) {
        Map<String, Object> direct = trimmingMap(trimmingValue(
                motor, "trimmingDrawing", "drawing", "drawingDetails"));
        if (!direct.isEmpty()) return direct;

        List<Map<String, Object>> sectionData = trimmingSectionData(motor, "TRIMMING_DRAWING");
        return sectionData.isEmpty() ? Collections.emptyMap() : sectionData.getFirst();
    }

    private List<Map<String, Object>> trimmingSectionData(
            Map<String, Object> motor,
            String sectionId) {
        for (Map<String, Object> section : trimmingMapList(motor.get("sections"))) {
            if (normalizePdfKey(sectionId).equals(normalizePdfKey(section.get("sectionId")))) {
                return trimmingMapList(section.get("sectionData"));
            }
        }
        return Collections.emptyList();
    }

    private List<Map<String, Object>> unwrapTrimmingRows(
            List<Map<String, Object>> sectionData,
            String nestedKey) {
        if (sectionData.isEmpty()) return sectionData;
        Object nested = trimmingValue(sectionData.getFirst(), nestedKey);
        List<Map<String, Object>> nestedRows = trimmingMapList(nested);
        return nestedRows.isEmpty() ? sectionData : nestedRows;
    }

    private List<Map<String, Object>> flattenTrimmingMeasurements(
            List<Map<String, Object>> source) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> parameter : source) {
            List<Map<String, Object>> stages = trimmingMapList(parameter.get("stages"));
            if (stages.isEmpty()) {
                result.add(parameter);
                continue;
            }
            for (Map<String, Object> stage : stages) {
                Map<String, Object> row = new LinkedHashMap<>(parameter);
                row.remove("stages");
                row.putAll(stage);
                Map<String, Object> readings = trimmingMap(stage.get("readings"));
                readings.forEach(row::putIfAbsent);
                result.add(row);
            }
        }
        return result;
    }

    private List<Map<String, Object>> trimmingMapList(Object value) {
        if (!(value instanceof Collection<?> collection)) return Collections.emptyList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : collection) {
            if (item instanceof Map<?, ?>) result.add(trimmingMap(item));
        }
        return result;
    }

    private Map<String, Object> trimmingMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) return Collections.emptyMap();
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private Object trimmingValue(Map<String, Object> source, String... keys) {
        if (source == null || source.isEmpty()) return null;
        for (String key : keys) {
            if (source.containsKey(key)) return source.get(key);
        }
        for (String key : keys) {
            String normalized = normalizePdfKey(key);
            for (Map.Entry<String, Object> entry : source.entrySet()) {
                if (normalized.equals(normalizePdfKey(entry.getKey()))) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private String trimmingMotorStatus(
            TrimmingDetailsResponse data,
            Map<String, Object> motor,
            String motorId) {
        Object savedStatus = trimmingValue(motor, "motorSubmissionStatus", "approvalStatus");
        if (savedStatus != null) return workflowStatusLabel(savedStatus);
        if (data.getMotorStatuses() != null) {
            for (MotorStatusResponse status : data.getMotorStatuses()) {
                if (motorId.equals(str(status.getMotorId()))) {
                    return workflowStatusLabel(status.getMotorSubmissionStatus());
                }
            }
        }
        return "—";
    }

    private String normalizePdfKey(Object key) {
        return key == null ? "" : String.valueOf(key)
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase(java.util.Locale.ROOT);
    }


    private void renderNdtDetails(Document doc, NdtResponse data) throws DocumentException {
        if (data == null || data.getMotors() == null || data.getMotors().isEmpty()) {
            doc.add(new Paragraph("No NDT details available.", BODY_FONT));
            return;
        }

        addSection(doc, "NDT", "");

        PdfPTable summary = createTable(new float[]{25, 25, 25, 25});
        addInfoStripCell(summary, "Batch ID", str(data.getBatchId()));
        addInfoStripCell(summary, "Batch Type", str(data.getBatchType()));
        addInfoStripCell(summary, "Submitted Date & Time", str(data.getSubmittedAt()));
        addInfoStripCell(summary, "Submitted By", qcPersonName(data.getSubmittedBy()));

        PdfPCell statusCell = new PdfPCell();
        statusCell.setColspan(4);
        statusCell.setBorderColor(BORDER_LIGHT);
        statusCell.setPadding(6);
        statusCell.setBackgroundColor(ROW_ALT);

        Paragraph statusParagraph = new Paragraph();
        statusParagraph.add(new Chunk("Form Status: ", BODY_BOLD));
        statusParagraph.add(createStatusPhrase(
                data.getFormStatus() != null ? data.getFormStatus().toString() : "-"));

        statusCell.addElement(statusParagraph);
        summary.addCell(statusCell);

        doc.add(summary);
        spacer(doc, 8);

        for (NdtMotorDto motor : data.getMotors()) {

            Map<String, Object> motorMap = motor == null
                    ? Collections.emptyMap()
                    : objectMapper.convertValue(
                    motor,
                    new TypeReference<Map<String, Object>>() {
                    }
            );

            String motorId = ccText(motorMap, "motorId", "motorIdNo");
            addPlainMotorStatusHeader(doc, motorId, ndtMotorStatus(data, motor, motorId));

            Map<String, Object> radiography = qcObjectMap(
                    qcValue(motorMap, "radiographyDetails"));

            if (!radiography.isEmpty()) {
                addCcTitle(doc, "Radiography setup");

                PdfPTable setupTable = createTable(new float[]{38, 62});
                addHeaders(setupTable, "Parameter", "Value");

                addCell(setupTable, "Equipment utilized for radiography work", BODY_BOLD, ROW_ALT);
                addCell(setupTable, qcDisplayValue(qcValue(
                        radiography, "equipmentUtilized")), BODY_FONT, WHITE);

                addCell(setupTable, "X ray beam energy used for radiography", BODY_BOLD, ROW_ALT);
                addCell(setupTable, qcDisplayValue(qcValue(
                        radiography, "xrayBeamEnergies")), BODY_FONT, WHITE);

                addCell(setupTable, "Radiography plan", BODY_BOLD, ROW_ALT);
                addCell(setupTable, ccText(radiography, "radiographyPlanId"), BODY_FONT, WHITE);

                doc.add(setupTable);

                List<Map<String, Object>> plans = ndtPlanDetails(
                        qcValue(radiography, "radiographyPlanDetails"));

                if (!plans.isEmpty()) {
                    addCcTitle(doc, "Radiography plan details");

                    PdfPTable table = createTable(new float[]{7, 15, 15, 13, 15, 15, 20});
                    addHeaders(table, "Sr.", "Sections", "Orientations", "SFD",
                            "Normal", "Tangential", "Detector");

                    for (int index = 0; index < plans.size(); index++) {
                        Map<String, Object> plan = plans.get(index);

                        addCell(table, Integer.toString(index + 1), BODY_FONT, WHITE);
                        addCell(table, ccText(plan, "numberOfSections"), BODY_FONT, WHITE);
                        addCell(table, ccText(plan, "numberOfOrientations"), BODY_FONT, WHITE);
                        addCell(table, ccText(plan, "sfd"), BODY_FONT, WHITE);
                        addCell(table, ccText(plan, "numberOfNormalExposures"), BODY_FONT, WHITE);
                        addCell(table, ccText(plan, "numberOfTangentialExposures"), BODY_FONT, WHITE);

                        Object detector = qcValue(plan, "detectorType");
                        addCell(table, detector != null
                                ? prettyText(detector.toString()) : "—", BODY_FONT, WHITE);
                    }

                    doc.add(table);
                }
            }

            List<Map<String, Object>> additionalExposures = qcMapList(
                    qcValue(motorMap, "additionalExposureDetails"));

            if (!additionalExposures.isEmpty()) {
                addCcTitle(doc, "Additional exposure details");

                PdfPTable table = createTable(new float[]{30, 35, 35});
                addHeaders(table, "Section", "Orientation", "Exposures");

                for (Map<String, Object> exposure : additionalExposures) {
                    addCell(table, ccText(exposure, "sectionNumber"), BODY_FONT, WHITE);
                    addCell(table, ccText(exposure, "orientation"), BODY_FONT, WHITE);
                    addCell(table, ccText(exposure, "numberOfExposure"), BODY_FONT, WHITE);
                }

                doc.add(table);
            }

            List<Map<String, Object>> radiographyObservations = qcMapList(
                    qcValue(motorMap, "radiographyObservations"));

            if (!radiographyObservations.isEmpty()) {
                addCcTitle(doc, "Observation in radiography");

                PdfPTable table = createTable(new float[]{15, 18, 27, 40});
                addHeaders(table, "Section", "Orientation", "Observations", "Images");

                for (Map<String, Object> observation : radiographyObservations) {
                    addCell(table, ccText(observation, "sectionNumber"), BODY_FONT, WHITE);
                    addCell(table, ccText(observation, "orientation"), BODY_FONT, WHITE);
                    addCell(table, ccText(observation, "observation"), BODY_FONT, WHITE);
                    addCell(table, qcDisplayValue(qcValue(
                            observation, "uploadedImages", "images")), BODY_FONT, WHITE);
                }

                doc.add(table);
            }

            List<Map<String, Object>> visualInspections = qcMapList(
                    qcValue(motorMap, "visualInspectionDetails"));

            if (!visualInspections.isEmpty()) {
                addCcTitle(doc, "Visual inspection");

                PdfPTable table = createTable(new float[]{40, 12, 13, 35});
                addHeaders(table, "Observation", "Section", "Orientation", "Images");

                for (Map<String, Object> inspection : visualInspections) {
                    String type = ndtObservationLabel(qcValue(inspection, "observationType"));
                    String observation = ccText(inspection, "observation");

                    String combined = "—".equals(type)
                            ? observation
                            : type + ("—".equals(observation) ? "" : ": " + observation);

                    addCell(table, combined, BODY_FONT, WHITE);
                    addCell(table, ccText(inspection, "sectionNumber"), BODY_FONT, WHITE);
                    addCell(table, ccText(inspection, "orientation"), BODY_FONT, WHITE);
                    addCell(table, qcDisplayValue(qcValue(
                            inspection, "uploadedImages", "images")), BODY_FONT, WHITE);
                }

                doc.add(table);
            }

            Object visualMedia = qcValue(motorMap, "uploadedVideos", "visualInspectionMedia");

            if (hasQcDisplayValue(visualMedia)) {
                addCcTitle(doc, "Visual inspection media");
                doc.add(new Paragraph(qcDisplayValue(visualMedia), BODY_FONT));
            }

            Object signedReport = qcValue(motorMap, "signedNdtReport");

            if (hasQcDisplayValue(signedReport)) {
                addCcTitle(doc, "Signed NDT report");
                doc.add(new Paragraph(qcDisplayValue(signedReport), BODY_FONT));
            }

            Object additionalRemarks = qcValue(motorMap, "additionalRemarks");

            if (hasQcDisplayValue(additionalRemarks)) {
                addCcTitle(doc, "Additional remarks");
                doc.add(new Paragraph(qcDisplayValue(additionalRemarks), BODY_FONT));
            }

            spacer(doc, 8);
        }
    }


    private String ndtMotorStatus(
            NdtResponse data,
            NdtMotorDto motor,
            String motorId) {

        if (motor != null) {
            Map<String, Object> motorMap = objectMapper.convertValue(
                    motor,
                    new TypeReference<Map<String, Object>>() {
                    }
            );

            Object savedStatus = qcValue(
                    motorMap, "motorSubmissionStatus", "approvalStatus");

            if (savedStatus != null) {
                return workflowStatusLabel(savedStatus);
            }
        }

        if (data.getMotorStatuses() != null) {
            for (MotorStatusResponse status : data.getMotorStatuses()) {
                if (motorId.equals(str(status.getMotorId()))) {
                    return workflowStatusLabel(status.getMotorSubmissionStatus());
                }
            }
        }

        return "—";
    }


    private String ndtObservationLabel(Object observationType) {
        if (observationType == null) return "—";
        String value = observationType.toString();
        return switch (normalizePdfKey(value)) {
            case "ROCKETMOTOREXTERNALSURFACE" -> "Rocket motor external surface";
            case "SURFACEPAINTFINISH" -> "Surface Paint / Finish";
            case "DENTSSCRATCHESMOTORCASE" -> "Dents/scratch/abnormalities on motor case";
            case "DENTSSCRATCHESPROPELLANT" -> "Dents/scratch/abnormalities on propellant";
            case "NUTBOLTGROOVESCLEANLINESS", "NUTBOLTGROVESCLEANLINESS" -> "Nut & bolt grooves cleanliness";
            case "OBSERVATIONNOZZLEENDFLANGE", "NOZZLEENDFLANGE" -> "Observation on nozzle end flange";
            case "OBSERVATIONHEADENDFLANGE", "HEADENDFLANGE" -> "Observation on Head End Flange";
            case "PORTCLEANLINESS" -> "Port cleanliness";
            case "BEADINGCONDITION" -> "Beading condition";
            default -> prettyText(value);
        };
    }

    private List<Map<String, Object>> ndtPlanDetails(Object value) {
        List<Map<String, Object>> plans = new ArrayList<>();
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                Map<String, Object> plan = qcObjectMap(item);
                if (!plan.isEmpty()) plans.add(plan);
            }
            return plans;
        }
        Map<String, Object> plan = qcObjectMap(value);
        if (!plan.isEmpty()) plans.add(plan);
        return plans;
    }

    private Map<String, Object> stringKeyMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    public void renderQcDivisionDetails(Document doc, QcDivisionFormResponse data)
            throws DocumentException {
        if (data == null) {
            doc.add(new Paragraph("No QC Division form details are available.", BODY_FONT));
            return;
        }

        addSection(doc, "Quality Assurance Division Inspection Report", "");

        PdfPTable metaTable = createTable(new float[]{25, 25, 25, 25});
        addInfoStripCell(metaTable, "Batch ID", str(data.getBatchId()));
        addInfoStripCell(metaTable, "Sub Department ID", str(data.getSubDepartmentId()));
        addInfoStripCell(metaTable, "Submission Type", workflowStatusLabel(data.getFormSubmissionType()));
        addInfoStripCell(metaTable, "Submitted By", qcPersonName(data.getSubmittedBy()));
        Phrase formStatusPhrase = new Phrase();
        formStatusPhrase.add(new Chunk("Form Status: ",
                new Font(Font.HELVETICA, 8, Font.BOLD, Color.BLACK)));
        formStatusPhrase.add(new Chunk(workflowStatusLabel(data.getStatus()),
                new Font(Font.HELVETICA, 8, Font.BOLD, workflowStatusColor(data.getStatus()))));
        PdfPCell formStatusCell = new PdfPCell(formStatusPhrase);
        formStatusCell.setColspan(4);
        formStatusCell.setPadding(6);
        formStatusCell.setBackgroundColor(ROW_ALT);
        formStatusCell.setBorderColor(BORDER_LIGHT);
        metaTable.addCell(formStatusCell);
        doc.add(metaTable);

        renderQcDivisionMotorStatusSummary(doc, data);

        List<DivisionDetailDto> details = data.getDivisionDetails();
        if (details == null || details.isEmpty()) {
            addSection(doc, "Division Details", "");
            doc.add(new Paragraph("No division data has been saved for this form.", BODY_FONT));
            return;
        }

        List<DivisionDetailDto> orderedDetails = new ArrayList<>(details);
        orderedDetails.sort(Comparator.comparingInt(detail -> qcDivisionPdfOrder(
                detail == null ? null : qcEffectiveDivision(
                        detail.getDivision(), detail.getSubType()))));
        for (DivisionDetailDto detail : orderedDetails) {
            if (detail == null) continue;

            String division = detail.getDivision();
            String effectiveDivision = qcEffectiveDivision(division, detail.getSubType());
            String displayDivision = qcDivisionDisplayName(effectiveDivision);
            addSection(doc, displayDivision, "");

            if (detail.getRemarks() != null && !detail.getRemarks().isBlank()) {
                Paragraph remarks = new Paragraph("Remarks: " + detail.getRemarks(), BODY_FONT);
                remarks.setSpacingBefore(3);
                doc.add(remarks);
            }
            if (detail.getRejectionReason() != null && !detail.getRejectionReason().isBlank()) {
                Paragraph rejection = new Paragraph(
                        "Rejection reason: " + detail.getRejectionReason(), BODY_FONT);
                rejection.setSpacingBefore(3);
                doc.add(rejection);
            }

            Map<String, Object> divisionData = detail.getData() == null
                    ? Collections.emptyMap()
                    : mapper.convertValue(
                    detail.getData(),
                    new TypeReference<Map<String, Object>>() {
                    });
            if (divisionData == null || divisionData.isEmpty()) {
                doc.add(new Paragraph("No data recorded for this division.", BODY_FONT));
            } else if (!renderConciseQcDivisionData(
                    doc, effectiveDivision, divisionData)) {
                for (Map.Entry<String, Object> entry : divisionData.entrySet()) {
                    renderQcDivisionValue(doc, entry.getKey(), entry.getValue(), 0);
                }
            }
            spacer(doc, 8);
        }
    }

    private void renderQcQaRemarks(Document doc, Object value) throws DocumentException {
        if (!hasQcDisplayValue(value)) return;
        addQcEvaluationHeading(doc, "QA Remarks", 0);
        PdfPTable table = createTable(new float[]{100});
        addCell(table, qcDisplayValue(value), BODY_FONT, WHITE);
        doc.add(table);
    }

    private boolean renderConciseQcDivisionData(
            Document doc,
            String division,
            Map<String, Object> data) throws DocumentException {
        String divisionKey = normalizePdfKey(division);
        if ("RAWMATERIALREVALIDATION".equals(divisionKey)) {
            return renderQcRawMaterialRevalidation(doc, data);
        }
        if ("RAWMATERIALPROCESSING".equals(divisionKey)) {
            return renderQcRawMaterialProcessing(doc, data);
        }
        if ("PROPELLANTPROPERTIES".equals(divisionKey)) {
            return renderQcPropellantProperties(doc, data);
        }
        if ("MIXING".equals(divisionKey)) {
            return renderQcMixingDivision(doc, data);
        }
        if ("HARDWARE".equals(divisionKey)) {
            return renderQcHardwareDivision(doc, data);
        }
        if ("WEIGHTMENT".equals(divisionKey)) {
            return renderQcWeighmentDivision(doc, data);
        }
        if ("TRIMMING".equals(divisionKey)) {
            return renderQcTrimmingDivision(doc, data);
        }
        if ("NDT".equals(divisionKey)) {
            return renderQcNdtDivision(doc, data);
        }
        if (java.util.Set.of("CASTING", "CURING", "DECORING")
                .contains(divisionKey)) {
            return renderQcMotorProcessDivision(doc, divisionKey, data);
        }
        if (java.util.Set.of("POSTCURE", "POSTCUREOPERATION").contains(divisionKey)) {
            return renderQcMotorProcessDivision(doc, "POSTCURE", data);
        }
        return false;
    }

    private String qcEffectiveDivision(String division, String subType) {
        if (division == null || division.isBlank()) return subType;
        if ("RAWMATERIAL".equals(normalizePdfKey(division))
                && subType != null && !subType.isBlank()) {
            return subType;
        }
        return division;
    }

    private String qcDivisionDisplayName(String division) {
        if ("NDT".equalsIgnoreCase(division)) return "NDT";
        return "PROPELLANTPROPERTIES".equals(normalizePdfKey(division))
                ? "QC Division"
                : prettyText(division);
    }

    private int qcDivisionPdfOrder(String division) {
        return switch (normalizePdfKey(division)) {
            case "CASTING" -> 10;
            case "CURING" -> 20;
            case "DECORING" -> 30;
            case "TRIMMING" -> 40;
            case "POSTCURE", "POSTCUREOPERATION" -> 50;
            default -> 100;
        };
    }

    private boolean renderQcRawMaterialRevalidation(
            Document doc,
            Map<String, Object> data) throws DocumentException {
        List<Map<String, Object>> materials = qcMapList(qcValue(data, "materials"));
        if (materials.isEmpty()) return false;

        for (int materialIndex = 0; materialIndex < materials.size(); materialIndex++) {
            Map<String, Object> material = materials.get(materialIndex);
            List<Map<String, Object>> checks = qcMapList(qcValue(
                    material, "qcDetails", "qualityChecks", "parameters", "specifications"));

            String serialNumber = qcText(qcValue(material, "srNo"));
            if (serialNumber == null) serialNumber = String.valueOf(materialIndex + 1);
            String ingredient = qcText(qcValue(
                    material, "ingredient", "materialName", "rawMaterial", "materialCode"));
            if (ingredient == null) ingredient = "Material";

            Paragraph materialTitle = new Paragraph(
                    serialNumber + ". " + ingredient, LABEL_FONT);
            materialTitle.setSpacingBefore(7);
            materialTitle.setSpacingAfter(2);
            doc.add(materialTitle);

            String lotNumber = qcText(qcValue(
                    material, "lotBatchNumber", "lotId", "batchNumber", "acemBatchNo"));
            if (lotNumber == null && !checks.isEmpty()) {
                lotNumber = qcText(qcValue(
                        checks.getFirst(), "lotBatchNumber", "lotId", "batchNumber"));
            }
            if (lotNumber != null) {
                Paragraph lot = new Paragraph("Lot: " + lotNumber, SUBTITLE_FONT);
                lot.setSpacingAfter(3);
                doc.add(lot);
            }

            if (checks.isEmpty()) {
                Map<String, Object> remaining = new LinkedHashMap<>(material);
                remaining.keySet().removeIf(key -> java.util.Set.of(
                                "SRNO", "INGREDIENT", "MATERIALNAME", "RAWMATERIAL", "MATERIALCODE")
                        .contains(normalizePdfKey(key)));
                if (!remaining.isEmpty()) {
                    renderQcDivisionMap(doc, "Material Details", remaining, 1);
                } else {
                    doc.add(new Paragraph("No QC checks recorded.", SUBTITLE_FONT));
                }
                continue;
            }

            PdfPTable table = createTable(new float[]{25, 20, 12, 12, 14, 17});
            table.setHeaderRows(1);
            addHeaders(table, "Parameter", "Specification", "Analysed", "ACEM QC",
                    "Validity", "Remarks");
            boolean alternate = false;
            java.util.LinkedHashSet<String> certificates = new java.util.LinkedHashSet<>();
            for (Map<String, Object> check : checks) {
                Color background = alternate ? ROW_ALT : WHITE;
                addCell(table, str(qcValue(check,
                        "parameter", "parameterName", "specificationName")), BODY_FONT, background);
                addCell(table, qcSpecification(qcValue(check,
                        "specification", "specs", "referenceRange")), BODY_FONT, background);
                addCell(table, str(qcValue(check,
                                "analysedResult", "analyzedResult", "result", "actualValue")),
                        BODY_FONT, background);
                addCell(table, str(qcValue(check,
                        "acemQcResult", "acemQCResult", "qcResult")), BODY_FONT, background);
                addCell(table, str(qcValue(check,
                        "validity", "validityDate", "validUntil")), BODY_FONT, background);
                addCell(table, str(qcValue(check,
                        "remarks", "observation", "observations")), BODY_FONT, background);
                collectQcCertificates(qcValue(
                                check, "qcCertificate", "certificates", "attachments", "filePaths"),
                        certificates);
                alternate = !alternate;
            }
            collectQcCertificates(qcValue(
                            material, "qcCertificate", "certificates", "attachments", "filePaths"),
                    certificates);
            doc.add(table);
            renderQcCertificateLine(doc, certificates);
        }
        return true;
    }

    private boolean renderQcRawMaterialProcessing(
            Document doc,
            Map<String, Object> data) throws DocumentException {
        List<Map<String, Object>> premixes = qcMapList(qcValue(data, "premixes"));
        if (premixes.isEmpty()) return false;

        for (int premixIndex = 0; premixIndex < premixes.size(); premixIndex++) {
            Map<String, Object> premix = premixes.get(premixIndex);
            Object premixNumber = qcValue(premix, "premixNo");
            Object displayPremixNumber = premixNumber != null
                    ? premixNumber : premixIndex + 1;

            List<Map<String, Object>> processingItems = new ArrayList<>();
            appendQcMaps(processingItems, qcValue(
                    premix, "solidProcessingDetails", "solidProcess"));
            appendQcMaps(processingItems, qcValue(
                    premix, "liquidProcessingDetails", "liquidProcess"));

            Object detailsValue = qcValue(premix, "details");
            if (processingItems.isEmpty() && detailsValue instanceof Map<?, ?> rawDetails) {
                Map<String, Object> details = stringKeyMap(rawDetails);
                appendQcMaps(processingItems, qcValue(
                        details, "solidProcessingDetails", "solidProcess"));
                appendQcMaps(processingItems, qcValue(
                        details, "liquidProcessingDetails", "liquidProcess"));
            }

            if (processingItems.isEmpty()) {
                Paragraph emptyPremix = new Paragraph(
                        "Premix-" + str(displayPremixNumber)
                                + " — No approved processing details",
                        SUBTITLE_FONT);
                emptyPremix.setSpacingBefore(6);
                emptyPremix.setSpacingAfter(3);
                doc.add(emptyPremix);
                continue;
            }

            for (int itemIndex = 0; itemIndex < processingItems.size(); itemIndex++) {
                renderQcProcessingItem(
                        doc, processingItems.get(itemIndex), itemIndex + 1,
                        displayPremixNumber);
            }
        }
        return true;
    }

    private boolean renderQcPropellantProperties(
            Document doc,
            Map<String, Object> data) throws DocumentException {
        List<Map<String, Object>> motors = qcMapList(qcValue(
                data, "motors", "motorDetails"));
        if (motors.isEmpty()) return false;

        for (int motorIndex = 0; motorIndex < motors.size(); motorIndex++) {
            Map<String, Object> motor = qcMergedMotorDetails(motors.get(motorIndex));
            String motorId = qcText(qcValue(motor, "motorId", "motorIdNo"));
            if (motorId == null) motorId = "Motor " + (motorIndex + 1);

            Paragraph motorTitle = new Paragraph(motorId + " — QC", LABEL_FONT);
            motorTitle.setSpacingBefore(8);
            motorTitle.setSpacingAfter(4);
            doc.add(motorTitle);

            List<Map<String, Object>> processes = qcMapList(qcValue(
                    motor, "processes", "processDetails"));
            if (processes.isEmpty()) {
                Map<String, Object> motorData = new LinkedHashMap<>(motor);
                motorData.remove("motorId");
                motorData.remove("motorIdNo");
                renderQcEvaluationValue(doc, null, motorData, 0);
                continue;
            }

            for (Map<String, Object> process : processes) {
                String processType = qcText(qcValue(
                        process, "processType", "process", "type"));
                Object processData = qcValue(process, "data", "details", "processData");
                if (processData == null) {
                    Map<String, Object> remaining = new LinkedHashMap<>(process);
                    remaining.remove("processType");
                    remaining.remove("process");
                    remaining.remove("type");
                    processData = remaining;
                }
                renderQcEvaluationValue(doc, qcProcessTitle(processType), processData, 0);
            }
        }
        return true;
    }

    private boolean renderQcMixingDivision(
            Document doc,
            Map<String, Object> data) throws DocumentException {
        List<Map<String, Object>> premixes = qcMapList(qcValue(data, "premixes"));
        if (!premixes.isEmpty()) {
            String dataStageType = qcText(qcValue(data, "stageType", "subType"));
            for (int index = 0; index < premixes.size(); index++) {
                renderQcMixingPremix(doc, premixes.get(index), dataStageType, index + 1);
            }
            return true;
        }

        List<Map<String, Object>> stages = qcMapList(qcValue(data, "stages"));
        if (stages.isEmpty()) return false;
        for (Map<String, Object> stage : stages) {
            String stageType = qcText(qcValue(stage, "stageType", "type"));
            List<Map<String, Object>> stagePremixes = qcMapList(qcValue(stage, "premixes"));
            for (int index = 0; index < stagePremixes.size(); index++) {
                renderQcMixingPremix(doc, stagePremixes.get(index), stageType, index + 1);
            }
        }
        return true;
    }

    private void renderQcMixingPremix(
            Document doc,
            Map<String, Object> premix,
            String suppliedStageType,
            int fallbackNumber) throws DocumentException {
        Object premixNumber = qcValue(premix, "premixNo");
        if (premixNumber == null) premixNumber = fallbackNumber;

        Object premixDetails = qcValue(premix, "premixDetails");
        Object finalMixDetails = qcValue(premix, "finalMixDetails");
        if (premixDetails != null) {
            renderQcMixingStage(doc, "PREMIX", premixNumber, premixDetails);
        }
        if (finalMixDetails != null) {
            renderQcMixingStage(doc, "FINAL_MIX", premixNumber, finalMixDetails);
        }
        if (premixDetails != null || finalMixDetails != null) return;

        Object details = qcValue(premix, "details");
        if (details == null) details = premix;
        String stageType = qcText(qcValue(premix, "stageType"));
        if (stageType == null) stageType = suppliedStageType;
        if (stageType == null) stageType = "PREMIX";
        renderQcMixingStage(doc, stageType, premixNumber, details);
    }

    private void renderQcMixingStage(
            Document doc,
            String stageType,
            Object premixNumber,
            Object detailsValue) throws DocumentException {
        if (!(detailsValue instanceof Map<?, ?> rawDetails)) return;
        Map<String, Object> details = stringKeyMap(rawDetails);
        String normalizedStage = normalizePdfKey(stageType);
        boolean finalMix = normalizedStage.contains("FINAL");

        Paragraph title = new Paragraph(
                "Mixing · " + (finalMix ? "Final Mix" : "Premix")
                        + " - " + str(premixNumber),
                LABEL_FONT);
        title.setSpacingBefore(7);
        title.setSpacingAfter(4);
        doc.add(title);

        Map<String, Object> configuration = qcMap(qcValue(
                details, "mixerConfiguration", "configuration"));
        Map<String, Object> mixDetails = qcMap(qcValue(details, "mixDetails"));

        Object bowlNumber = qcValue(details, "bowlNo", "bowlNumber", "bowlId");
        if (bowlNumber == null) bowlNumber = qcValue(configuration, "bowlId", "bowlNo");
        Object mixDate = qcValue(details,
                finalMix ? "dateOfFinalMix" : "dateOfPremix", "mixDate", "date");
        if (mixDate == null) mixDate = qcValue(mixDetails, "mixDate", "date");
        Object mixerBuilding = qcValue(details,
                "mixerBuildingNo", "mixerAndBuildingNo", "mixerBldgNo");
        if (mixerBuilding == null) {
            String mixer = qcText(qcValue(configuration, "mixerId", "mixerNo"));
            String building = qcText(qcValue(configuration, "bldgNo", "buildingNo"));
            if (mixer != null || building != null) {
                mixerBuilding = (mixer != null ? mixer : "—")
                        + (building != null ? " & " + building : "");
            }
        }
        Object batchSize = qcValue(details,
                "premixQty", "batchSize", "batchSizeKg", "mixQuantity", "quantity");
        if (batchSize == null) batchSize = qcValue(mixDetails, "mixQuantity", "quantity");

        List<Map<String, Object>> parameters = qcMapList(qcValue(
                details, "parameters", "qualityChecks", "qcChecks"));
        List<Map<String, Object>> viscosityRows = qcMapList(qcValue(
                details, "viscosityBuildUp", "viscosityBuildUpDetails"));
        boolean hasMixingData = hasQcDisplayValue(bowlNumber)
                || hasQcDisplayValue(mixDate)
                || hasQcDisplayValue(mixerBuilding)
                || hasQcDisplayValue(batchSize)
                || !parameters.isEmpty()
                || !viscosityRows.isEmpty();
        if (!hasMixingData) {
            doc.add(new Paragraph("No approved mixing details are available.", SUBTITLE_FONT));
            return;
        }
        PdfPTable table = createTable(new float[]{14, 13, 16, 12, 17, 13, 9, 6});
        table.setHeaderRows(1);
        addHeaders(table,
                "Bowl No",
                finalMix ? "Date of Final Mix" : "Date of Premix",
                "Mixer & Bldg No.", "Batch size (KG)",
                "Parameter", "Specification", "Value", "Remarks");

        List<Map<String, Object>> rows = parameters.isEmpty()
                ? List.of(Collections.emptyMap()) : parameters;
        boolean alternate = false;
        for (int index = 0; index < rows.size(); index++) {
            Map<String, Object> parameter = rows.get(index);
            Color background = alternate ? ROW_ALT : WHITE;
            boolean firstRow = index == 0;
            addCell(table, firstRow ? str(bowlNumber) : "", BODY_FONT, background);
            addCell(table, firstRow ? str(mixDate) : "", BODY_FONT, background);
            addCell(table, firstRow ? str(mixerBuilding) : "", BODY_FONT, background);
            addCell(table, firstRow ? str(batchSize) : "", BODY_FONT, background);
            addCell(table, str(qcValue(parameter,
                    "parameter", "parameterName", "name")), BODY_FONT, background);
            addCell(table, qcSpecification(qcValue(parameter,
                    "specification", "specs", "referenceRange")), BODY_FONT, background);
            addCell(table, qcMixingParameterValue(parameter), BODY_FONT, background);
            addCell(table, str(qcValue(parameter,
                    "remarks", "observationRemarks", "comments")), BODY_FONT, background);
            alternate = !alternate;
        }
        doc.add(table);

        if (!viscosityRows.isEmpty()) {
            addQcEvaluationHeading(doc, "Viscosity Build-up (P @ 40°C)", 0);
            PdfPTable viscosityTable = createTable(new float[]{12, 36, 52});
            viscosityTable.setHeaderRows(1);
            addHeaders(viscosityTable,
                    "S. No", "Time (min)", "Viscosity Value (P @ 40°C)");
            boolean viscosityAlternate = false;
            for (int index = 0; index < viscosityRows.size(); index++) {
                Map<String, Object> row = viscosityRows.get(index);
                Color background = viscosityAlternate ? ROW_ALT : WHITE;
                Object serial = qcValue(row, "srNo", "serialNo", "sampleNo");
                addCell(viscosityTable, str(serial != null ? serial : index + 1),
                        BODY_FONT, background);
                addCell(viscosityTable, str(qcValue(row,
                        "time", "timeMin", "timeMinutes")), BODY_FONT, background);
                addCell(viscosityTable, str(qcValue(row,
                        "viscosityValue", "viscosity", "value")), BODY_FONT, background);
                viscosityAlternate = !viscosityAlternate;
            }
            doc.add(viscosityTable);
        }
    }

    private String qcMixingParameterValue(Map<String, Object> parameter) {
        Object directValue = qcValue(parameter,
                "value", "result", "actualValue", "observedValue", "observedValues");
        if (hasQcDisplayValue(directValue)) return qcEvaluationCellValue(directValue);

        // QC Mixing domain stores sample columns as string[] `values`.
        Object valuesList = qcValue(parameter, "values");
        if (valuesList instanceof Collection<?> rawValues) {
            List<String> fromValues = new ArrayList<>();
            for (Object value : rawValues) {
                if (hasQcDisplayValue(value)) fromValues.add(str(value));
            }
            if (!fromValues.isEmpty()) return String.join(", ", fromValues);
        }

        Object observationsValue = qcValue(parameter, "observations", "samples");
        if (!(observationsValue instanceof Collection<?> observations)) return "—";
        List<String> values = new ArrayList<>();
        for (Object observation : observations) {
            if (observation instanceof Map<?, ?> rawObservation) {
                Map<String, Object> mapped = stringKeyMap(rawObservation);
                Object value = qcValue(mapped, "value", "observedValue", "result");
                if (hasQcDisplayValue(value)) values.add(str(value));
            } else if (hasQcDisplayValue(observation)) {
                values.add(str(observation));
            }
        }
        return values.isEmpty() ? "—" : String.join(", ", values);
    }

    private boolean renderQcHardwareDivision(
            Document doc,
            Map<String, Object> data) throws DocumentException {
        List<Map<String, Object>> motors = qcMapList(qcValue(
                data, "motorDetails", "motors"));
        if (motors.isEmpty()) return false;

        for (int motorIndex = 0; motorIndex < motors.size(); motorIndex++) {
            Map<String, Object> motor = qcMergedMotorDetails(motors.get(motorIndex));
            String motorId = qcText(qcValue(motor, "motorIdNo", "motorId"));
            if (motorId == null) motorId = "Motor " + (motorIndex + 1);
            Paragraph motorTitle = new Paragraph(motorId, LABEL_FONT);
            motorTitle.setSpacingBefore(8);
            motorTitle.setSpacingAfter(4);
            doc.add(motorTitle);

            Map<String, Object> processDetails = qcMap(qcValue(motor, "processDetails"));
            if (!processDetails.isEmpty()) {
                renderQcHardwareAbrading(doc, qcValue(processDetails, "abrading"));
                renderQcHardwareRows(doc, "Preheating", "PREHEATING",
                        qcValue(processDetails, "preheating", "preheatingDetails"), motor);
                renderQcHardwareRows(doc, "Liner Coating", "LINERCOATING",
                        qcValue(processDetails, "linerCoating", "linerCoatingDetails"), motor);
                renderQcHardwareDispatchSection(doc,
                        qcValue(processDetails, "dispatch", "dispatchDetails"),
                        qcValue(motor, "attachments"));
                continue;
            }

            if (qcValue(motor, "abrading") != null
                    || qcValue(motor, "preheating", "preheatingDetails") != null
                    || qcValue(motor, "linerCoating", "linerCoatingDetails") != null
                    || qcValue(motor, "dispatch", "dispatchDetails") != null) {
                renderQcHardwareAbrading(doc, qcValue(motor, "abrading"));
                renderQcHardwareRows(doc, "Preheating", "PREHEATING",
                        qcValue(motor, "preheating", "preheatingDetails"), motor);
                renderQcHardwareRows(doc, "Liner Coating", "LINERCOATING",
                        qcValue(motor, "linerCoating", "linerCoatingDetails"), motor);
                renderQcHardwareDispatchSection(doc,
                        qcValue(motor, "dispatch", "dispatchDetails"),
                        qcValue(motor, "attachments"));
                continue;
            }

            for (Map<String, Object> section : qcMapList(qcValue(motor, "sections"))) {
                String sectionId = qcText(qcValue(section, "sectionId"));
                Object sectionData = qcValue(section, "sectionData");
                String sectionKey = normalizePdfKey(sectionId);
                if (sectionKey.contains("ABRADING")) {
                    renderQcHardwareAbrading(doc, sectionData);
                } else if (sectionKey.contains("PREHEAT")) {
                    renderQcHardwareRows(
                            doc, "Preheating", "PREHEATING", sectionData, motor);
                } else if (sectionKey.contains("LINER") && sectionKey.contains("COAT")) {
                    renderQcHardwareRows(
                            doc, "Liner Coating", "LINERCOATING", sectionData, motor);
                } else if (sectionKey.contains("DISPATCH")) {
                    renderQcHardwareDispatchSection(doc, sectionData,
                            qcValue(motor, "attachments"));
                }
            }
        }
        return true;
    }

    private boolean renderQcWeighmentDivision(
            Document doc,
            Map<String, Object> data) throws DocumentException {
        Map<String, Object> weighscale = qcMap(qcValue(
                data, "weighscaleDetails", "weighScaleDetails"));
        List<Map<String, Object>> motors = qcMapList(qcValue(
                data, "motorWeightDetails", "motors"));
        if (weighscale.isEmpty() && motors.isEmpty()) return false;

        if (motors.isEmpty()) renderQcWeighscaleDetails(doc, weighscale);

        for (int motorIndex = 0; motorIndex < motors.size(); motorIndex++) {
            Map<String, Object> motor = qcMergedMotorDetails(motors.get(motorIndex));
            String motorId = qcText(qcValue(motor, "motorId", "motorIdNo"));
            if (motorId == null) motorId = "Motor " + (motorIndex + 1);

            Paragraph motorTitle = new Paragraph(motorId + " — Weighment", LABEL_FONT);
            motorTitle.setSpacingBefore(7);
            motorTitle.setSpacingAfter(4);
            doc.add(motorTitle);

            if (motorIndex == 0) renderQcWeighscaleDetails(doc, weighscale);

            List<Map<String, Object>> weights = qcMapList(qcValue(
                    motor, "weights", "weightDetails", "weighmentDetails"));
            if (!weights.isEmpty()) {
                PdfPTable weightTable = createTable(new float[]{9, 71, 20});
                weightTable.setHeaderRows(1);
                addHeaders(weightTable, "Ref.", "Weight Parameter", "Weight (kg)");
                boolean alternate = false;
                for (Map<String, Object> weight : weights) {
                    Color background = alternate ? ROW_ALT : WHITE;
                    String reference = str(qcValue(weight,
                            "srNo", "reference", "serialNo"));
                    String parameter = str(qcValue(weight,
                            "weightParameter", "parameter", "description"));
                    Map<String, Object> calculation = qcMap(qcValue(weight, "calculation"));
                    String formula = qcText(qcValue(calculation, "formula"));
                    if (formula == null) formula = qcText(qcValue(weight, "formula"));
                    if (formula != null) {
                        parameter += "\nFormula: " + formula;
                        if (Boolean.TRUE.equals(qcValue(calculation, "autoCalculate"))) {
                            parameter += " (calculated automatically)";
                        }
                    }
                    addCell(weightTable, reference, BODY_BOLD, background);
                    addCell(weightTable, parameter, BODY_FONT, background);
                    addCell(weightTable, str(qcValue(weight,
                            "weightKg", "weight", "value")), BODY_FONT, background);
                    alternate = !alternate;
                }
                doc.add(weightTable);
            }
            List<Map<String, Object>> uploadedFiles = qcMapList(qcValue(
                    motor, "uploadedFiles", "attachments"));
            if (!uploadedFiles.isEmpty()) {
                addQcEvaluationHeading(doc, "Attachments — Upload Report", 0);
                renderQcAttachmentRows(doc, null, uploadedFiles, 0);
            }
            renderQcQaRemarks(doc, qcValue(motor, "qaRemarks", "qaRemark"));
        }
        return true;
    }

    private void renderQcWeighscaleDetails(
            Document doc,
            Map<String, Object> weighscale) throws DocumentException {
        if (weighscale == null || weighscale.isEmpty()) return;
        addQcEvaluationHeading(doc, "Weighment Details", 0);
        PdfPTable scaleTable = createTable(new float[]{50, 50});
        scaleTable.setHeaderRows(1);
        addHeaders(scaleTable, "Weighscale No.", "Calibration Due Date");
        addCell(scaleTable, str(qcValue(weighscale,
                "weighscaleNo", "weighScaleNo", "scaleNo")), BODY_FONT, WHITE);
        addCell(scaleTable, str(qcValue(weighscale,
                "calibrationDueDate", "calibrationDate")), BODY_FONT, WHITE);
        doc.add(scaleTable);
    }

    private void renderQcHardwareAbrading(Document doc, Object value)
            throws DocumentException {
        if (!hasQcDisplayValue(value)) return;
        addQcEvaluationHeading(doc, "Abrading", 0);
        List<Map<String, Object>> firstCut = qcHardwareRows(value,
                "firstCut", "firstCutDetails");
        List<Map<String, Object>> secondCut = qcHardwareRows(value,
                "secondCut", "secondCutDetails");
        if (firstCut.isEmpty() && secondCut.isEmpty()) {
            firstCut = qcHardwareRows(value);
        }
        renderQcHardwareRows(doc, "First Cut", "CUT", firstCut);
        renderQcHardwareRows(doc, "Second Cut", "CUT", secondCut);
        renderQcQaRemarks(doc, qcValue(qcHardwareSingleMap(value), "qaRemarks", "qaRemark"));
    }

    private void renderQcHardwareRows(
            Document doc,
            String heading,
            String rowType,
            Object value) throws DocumentException {
        renderQcHardwareRows(
                doc, heading, rowType, qcHardwareRows(value), Collections.emptyMap());
    }

    private void renderQcHardwareRows(
            Document doc,
            String heading,
            String rowType,
            Object value,
            Map<String, Object> context) throws DocumentException {
        Map<String, Object> rowContext = new LinkedHashMap<>(context);
        Map<String, Object> section = qcHardwareSingleMap(value);
        String sourceDateTimeField = switch (rowType) {
            case "PREHEATING" -> "preHeatingDateTime";
            case "LINERCOATING" -> "linerCoatingDateTime";
            default -> null;
        };
        if (sourceDateTimeField != null && section.containsKey(sourceDateTimeField)) {
            rowContext.put(sourceDateTimeField, section.get(sourceDateTimeField));
        }
        renderQcHardwareRows(doc, heading, rowType, qcHardwareRows(value), rowContext);
        renderQcQaRemarks(doc, qcValue(section, "qaRemarks", "qaRemark"));
    }

    private void renderQcHardwareRows(
            Document doc,
            String heading,
            String rowType,
            List<Map<String, Object>> sourceRows) throws DocumentException {
        renderQcHardwareRows(doc, heading, rowType, sourceRows, Collections.emptyMap());
    }

    private void renderQcHardwareRows(
            Document doc,
            String heading,
            String rowType,
            List<Map<String, Object>> sourceRows,
            Map<String, Object> context) throws DocumentException {
        if (sourceRows.isEmpty()) return;
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int index = 0; index < sourceRows.size(); index++) {
            rows.add(qcHardwareCanonicalRow(
                    sourceRows.get(index), rowType, index + 1, context));
        }
        addQcEvaluationHeading(doc, heading, 0);
        renderQcEvaluationRows(doc, null, rows, 1);
    }

    private Map<String, Object> qcHardwareCanonicalRow(
            Map<String, Object> row,
            String rowType,
            int serialNumber,
            Map<String, Object> context) {
        Map<String, Object> result = new LinkedHashMap<>();
        Object serial = qcValue(row, "srNo", "serialNo", "rowNo");
        result.put("srNo", serial != null ? serial : serialNumber);
        if ("CUT".equals(rowType)) {
            result.put("startDateTime", qcTableValue(qcValue(
                    row, "startDateTime", "startTime", "fromTime")));
            result.put("endDateTime", qcTableValue(qcValue(
                    row, "endDateTime", "endTime", "toTime")));
            result.put("qtyOfDustGms", qcTableValue(qcValue(row,
                    "qtyOfDustGms", "quantityOfDust", "dustQuantity", "dustQty")));
        } else if ("PREHEATING".equals(rowType)) {
            result.put("date", qcTableValue(qcValue(row, "date", "operationDate")));
            result.put("startDateTime", qcTableValue(qcValue(
                    row, "startDateTime", "startTime", "fromTime")));
            result.put("endDateTime", qcTableValue(qcValue(
                    row, "endDateTime", "endTime", "toTime")));
            Object preHeatingDateTime = qcValue(row, "preHeatingDateTime");
            if (preHeatingDateTime == null) {
                preHeatingDateTime = qcValue(context, "preHeatingDateTime");
            }
            result.put("preHeatingDateTime", qcTableValue(preHeatingDateTime));
            Object ovenNumber = qcValue(row, "ovenNumber", "ovenNo");
            if (ovenNumber == null) ovenNumber = qcValue(context, "ovenNumber", "ovenNo");
            Object buildingNo = qcValue(row, "buildingNo", "bldgNo");
            if (buildingNo == null) buildingNo = qcValue(context, "buildingNo", "bldgNo");
            result.put("ovenNumber", qcTableValue(ovenNumber));
            result.put("buildingNo", qcTableValue(buildingNo));
            result.put("temperature", qcTableValue(qcValue(row, "temperature", "temp")));
            result.put("vacuumLevel", qcTableValue(qcValue(row, "vacuumLevel", "vacuum")));
        } else if ("LINERCOATING".equals(rowType)) {
            result.put("date", qcTableValue(qcValue(row, "date", "operationDate")));
            result.put("startDateTime", qcTableValue(qcValue(
                    row, "startDateTime", "startTime", "fromTime")));
            result.put("endDateTime", qcTableValue(qcValue(
                    row, "endDateTime", "endTime", "toTime")));
            Object linerCoatingDateTime = qcValue(row, "linerCoatingDateTime");
            if (linerCoatingDateTime == null) {
                linerCoatingDateTime = qcValue(context, "linerCoatingDateTime");
            }
            result.put("linerCoatingDateTime", qcTableValue(linerCoatingDateTime));
            result.put("quantityOfLinerUsed", qcTableValue(qcValue(row,
                    "quantityOfLinerUsed", "linerQuantity", "quantity", "linerQty")));
            result.put("insulationTemp", qcTableValue(qcValue(row,
                    "insulationTemp", "insulationTemperature")));
            Object rh = qcValue(row, "rh", "relativeHumidity");
            if (rh == null) rh = qcValue(context, "rh", "relativeHumidity");
            result.put("rh", qcTableValue(rh));
        }
        result.put("observations", qcTableValue(qcValue(row,
                "observations", "observation", "remarks")));
        return result;
    }

    private Object qcTableValue(Object value) {
        return hasQcDisplayValue(value) ? value : "—";
    }

    private void renderQcHardwareDispatch(Document doc, Object value)
            throws DocumentException {
        Map<String, Object> dispatch = qcHardwareSingleMap(value);
        if (dispatch.isEmpty()) return;
        addQcEvaluationHeading(doc, "Dispatch", 0);

        Map<String, Object> punctures = qcMap(qcValue(
                dispatch, "looseFlapPunctures", "punctures"));
        Object he = qcValue(dispatch,
                "noOfPuncturesInLooseFlapAtHE", "hePunctures", "puncturesAtHe");
        Object ne = qcValue(dispatch,
                "noOfPuncturesInLooseFlapAtNE", "nePunctures", "puncturesAtNe");
        Object lf = qcValue(dispatch, "lfPunctures", "puncturesAtLf");
        if (he == null) he = qcValue(punctures, "he", "headEnd");
        if (ne == null) ne = qcValue(punctures, "ne", "nozzleEnd");
        Object dispatchDateTime = qcValue(dispatch,
                "dispatchDateTime", "dispatchTime", "dateTimeOfDispatch", "motorDispatchDateTime");

        if (hasQcDisplayValue(he)
                || hasQcDisplayValue(ne)
                || hasQcDisplayValue(lf)
                || hasQcDisplayValue(dispatchDateTime)) {
            PdfPTable dispatchTable = createTable(new float[]{58, 42});
            dispatchTable.setHeaderRows(1);
            addHeaders(dispatchTable, "Parameter", "Value");
            if (hasQcDisplayValue(he)) {
                addCell(dispatchTable, "No of punctures in Loose Flap at HE", BODY_FONT, WHITE);
                addCell(dispatchTable, str(he), BODY_FONT, WHITE);
            }
            if (hasQcDisplayValue(ne)) {
                addCell(dispatchTable, "No of punctures in Loose Flap at NE", BODY_FONT, ROW_ALT);
                addCell(dispatchTable, str(ne), BODY_FONT, ROW_ALT);
            }
            if (hasQcDisplayValue(lf)) {
                addCell(dispatchTable, "No of punctures in Loose Flap at LF Extension", BODY_FONT, WHITE);
                addCell(dispatchTable, str(lf), BODY_FONT, WHITE);
            }
            if (hasQcDisplayValue(dispatchDateTime)) {
                addCell(dispatchTable,
                        "Dispatch Date & Time",
                        BODY_FONT, ROW_ALT);
                addCell(dispatchTable, str(dispatchDateTime), BODY_FONT, ROW_ALT);
            }
            doc.add(dispatchTable);
        }

        Map<String, Object> observations = qcMap(qcValue(
                dispatch, "observations", "dispatchObservations"));
        List<Map<String, Object>> visualObservations = qcMapList(qcValue(
                dispatch, "visualObservations", "dispatchVisualObservations"));
        if (!visualObservations.isEmpty()) {
            addQcEvaluationHeading(doc, "Visual Observations", 0);
            PdfPTable visualTable = createTable(new float[]{10, 48, 26, 16});
            visualTable.setHeaderRows(1);
            addHeaders(visualTable, "SR No.", "Parameter", "Observation", "Remarks");
            boolean alternate = false;
            for (Map<String, Object> visualObservation : visualObservations) {
                Color background = alternate ? ROW_ALT : WHITE;
                addCell(visualTable, str(qcValue(visualObservation, "srNo")), BODY_FONT, background);
                addCell(visualTable, str(qcValue(visualObservation, "parameter")), BODY_FONT, background);
                addCell(visualTable, str(qcValue(visualObservation, "observations")), BODY_FONT, background);
                addCell(visualTable, str(qcValue(visualObservation, "remarks")), BODY_FONT, background);
                alternate = !alternate;
            }
            doc.add(visualTable);
        }
        Object linerObservation = qcValue(dispatch,
                "linerCoatedRubberSurfaceVisualObservation", "linerSurfaceObservation");
        Object flapObservation = qcValue(dispatch,
                "observationsOverLooseFlapBellowBonding", "looseFlapObservation");
        if (linerObservation == null) linerObservation = qcValue(observations,
                "linerCoatedRubberSurfaceVisualObservation", "linerSurfaceObservation");
        if (flapObservation == null) flapObservation = qcValue(observations,
                "observationsOverLooseFlapBellowBonding", "looseFlapObservation");
        if (hasQcDisplayValue(linerObservation) || hasQcDisplayValue(flapObservation)) {
            addQcEvaluationHeading(doc, "Observations", 0);
            PdfPTable observationTable = createTable(new float[]{58, 42});
            observationTable.setHeaderRows(1);
            addHeaders(observationTable, "Observation", "Value");
            if (hasQcDisplayValue(linerObservation)) {
                addCell(observationTable,
                        "Liner Coated Rubber Surface Visual Observation", BODY_FONT, WHITE);
                addCell(observationTable, str(linerObservation), BODY_FONT, WHITE);
            }
            if (hasQcDisplayValue(flapObservation)) {
                addCell(observationTable,
                        "Observations Over Loose Flap / Bellow Bonding", BODY_FONT, ROW_ALT);
                addCell(observationTable, str(flapObservation), BODY_FONT, ROW_ALT);
            }
            doc.add(observationTable);
        }

        renderQcHardwareAttachments(doc, dispatch);
    }

    private void renderQcHardwareDispatchSection(
            Document doc,
            Object dispatchValue,
            Object attachmentValue) throws DocumentException {
        renderQcHardwareDispatch(doc, dispatchValue);
        // Hardware attachments are stored beside dispatch on the motor, not
        // inside the dispatch section in the QC response.
        renderQcHardwareAttachments(doc, attachmentValue);
        renderQcQaRemarks(doc, qcValue(qcHardwareSingleMap(dispatchValue),
                "qaRemarks", "qaRemark"));
    }

    private void renderQcHardwareAttachments(
            Document doc,
            Object dispatchValue) throws DocumentException {
        Map<String, Object> dispatch = qcObjectMap(dispatchValue);
        Object attachmentsValue = qcValue(dispatch,
                "attachments", "uploadReportGraphPhoto", "uploadedFiles");
        // qcMapList returns an immutable empty list; this list is populated
        // below when attachments use the uploadReport/uploadGraph/uploadPhoto shape.
        List<Map<String, Object>> attachmentRows = new ArrayList<>(qcMapList(attachmentsValue));
        if (attachmentRows.isEmpty() && attachmentsValue instanceof Map<?, ?> rawAttachments) {
            Map<String, Object> attachments = stringKeyMap(rawAttachments);
            for (String[] typeAndKey : List.of(
                    new String[]{"Report", "uploadReport", "Report"},
                    new String[]{"Graph", "uploadGraph", "Graph"},
                    new String[]{"Photo", "uploadPhoto", "Photo"})) {
                Object file = qcValue(attachments, typeAndKey[1], typeAndKey[2]);
                if (hasQcDisplayValue(file)) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("attachmentType", typeAndKey[0]);
                    row.put("uploadedFiles", file);
                    attachmentRows.add(row);
                }
            }
        }
        if (attachmentRows.isEmpty()) {
            for (String[] typeAndKey : List.of(
                    new String[]{"Report", "uploadReport", "Report"},
                    new String[]{"Graph", "uploadGraph", "Graph"},
                    new String[]{"Photo", "uploadPhoto", "Photo"})) {
                Object file = qcValue(dispatch, typeAndKey[1], typeAndKey[2]);
                if (hasQcDisplayValue(file)) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("attachmentType", typeAndKey[0]);
                    row.put("uploadedFiles", file);
                    attachmentRows.add(row);
                }
            }
        }
        if (attachmentRows.isEmpty()) return;

        addQcEvaluationHeading(doc, "Upload Report / Graph / Photo", 0);
        PdfPTable table = createTable(new float[]{28, 72});
        table.setHeaderRows(1);
        addHeaders(table, "Attachment Type", "Uploaded Files");
        boolean alternate = false;
        for (Map<String, Object> row : attachmentRows) {
            Color background = alternate ? ROW_ALT : WHITE;
            addCell(table, str(qcValue(row,
                    "attachmentType", "type", "fileType")), BODY_FONT, background);
            addCell(table, qcEvaluationCellValue(qcValue(row,
                    "uploadedFiles", "file", "fileName", "attachment")), BODY_FONT, background);
            alternate = !alternate;
        }
        doc.add(table);
    }

    private List<Map<String, Object>> qcHardwareRows(Object value, String... nestedKeys) {
        if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = stringKeyMap(rawMap);
            List<Map<String, Object>> rows = qcHardwareRows(qcValue(map, "rows"));
            if (!rows.isEmpty()) return rows;
            for (String nestedKey : nestedKeys) {
                Object nested = qcValue(map, nestedKey);
                List<Map<String, Object>> nestedRows = qcHardwareRows(nested);
                if (!nestedRows.isEmpty()) return nestedRows;
            }
            return List.of(map);
        }
        List<Map<String, Object>> rows = qcMapList(value);
        if (rows.size() == 1 && nestedKeys.length > 0) {
            for (String nestedKey : nestedKeys) {
                Object nested = qcValue(rows.getFirst(), nestedKey);
                List<Map<String, Object>> nestedRows = qcHardwareRows(nested);
                if (!nestedRows.isEmpty()) return nestedRows;
            }
        }
        return rows;
    }

    private Map<String, Object> qcHardwareSingleMap(Object value) {
        if (value instanceof Map<?, ?> map) return stringKeyMap(map);
        List<Map<String, Object>> rows = qcMapList(value);
        return rows.isEmpty() ? Collections.emptyMap() : rows.getFirst();
    }

    private Map<String, Object> qcMap(Object value) {
        return value instanceof Map<?, ?> map
                ? stringKeyMap(map) : Collections.emptyMap();
    }

    private boolean renderQcTrimmingDivision(
            Document doc,
            Map<String, Object> data) throws DocumentException {
        List<Map<String, Object>> motors = qcMapList(qcValue(
                data, "trimmingDetails", "motorDetails", "motors"));
        if (motors.isEmpty()) return false;

        addQcEvaluationHeading(doc, "Trimming Division Details", 0);

        for (int motorIndex = 0; motorIndex < motors.size(); motorIndex++) {
            Map<String, Object> motor = qcMergedMotorDetails(motors.get(motorIndex));
            String motorId = qcText(qcValue(motor, "motorIdNo", "motorId"));
            if (motorId == null) motorId = "Motor " + (motorIndex + 1);

            Paragraph motorTitle = new Paragraph(motorId, LABEL_FONT);
            motorTitle.setSpacingBefore(8);
            motorTitle.setSpacingAfter(4);
            doc.add(motorTitle);

            Object receivedDate = qcValue(
                    motor, "motorReceivedDateTime", "motorReceivedDate", "motorReceivedAt");
            PdfPTable receivedTable = createTable(new float[]{24, 76});
            addCell(receivedTable, "Motor Received Date & Time", BODY_BOLD, ROW_ALT);
            addCell(receivedTable, receivedDate != null ? str(receivedDate) : "—",
                    BODY_FONT, WHITE);
            doc.add(receivedTable);

            List<Map<String, Object>> operationRows = trimmingOperationRows(motor);
            if (!operationRows.isEmpty()) {
                addQcEvaluationHeading(doc, "Trimming Details", 0);
                PdfPTable table = createTable(new float[]{18, 12, 12, 9, 9, 9, 9, 9, 13});
                table.setHeaderRows(1);
                addHeaders(table, "Machine Details", "Start Date & Time", "Completion Date & Time",
                        "Arbor Size", "Cutter Size", "Ingest Size", "Motor Truing",
                        "Arbor Turing", "Remarks");
                boolean alternate = false;
                for (Map<String, Object> row : operationRows) {
                    Color background = alternate ? ROW_ALT : WHITE;
                    addCell(table, str(trimmingValue(row,
                            "machineDetails", "machineNo", "machineId")), BODY_FONT, background);
                    addCell(table, str(trimmingValue(row, "startDatetime", "startDate")),
                            BODY_FONT, background);
                    addCell(table, str(trimmingValue(row,
                                    "completionDatetime", "completionDate", "endDate")),
                            BODY_FONT, background);
                    addCell(table, str(trimmingValue(row, "arborSize")), BODY_FONT, background);
                    addCell(table, str(trimmingValue(row, "cutterSize")), BODY_FONT, background);
                    addCell(table, str(trimmingValue(row, "ingestSize")), BODY_FONT, background);
                    addCell(table, str(trimmingValue(row, "motorTruing")), BODY_FONT, background);
                    addCell(table, str(trimmingValue(row, "arborTuring")), BODY_FONT, background);
                    addCell(table, str(trimmingValue(row, "remarks")), BODY_FONT, background);
                    alternate = !alternate;
                }
                doc.add(table);
            }

            List<Map<String, Object>> measurementRows = trimmingMeasurementRows(motor);
            if (!measurementRows.isEmpty()) {
                addQcEvaluationHeading(doc, "Dimensions After Trimming", 0);
                PdfPTable table = createTable(new float[]{20, 18, 18, 11, 11, 11, 11});
                table.setHeaderRows(1);
                addHeaders(table, "Parameter", "Stage", "Specification",
                        "R2T", "R2B", "R1R", "R1L");
                boolean alternate = false;
                for (Map<String, Object> row : measurementRows) {
                    Color background = alternate ? ROW_ALT : WHITE;
                    Map<String, Object> readings = trimmingMap(row.get("readings"));
                    addCell(table, str(trimmingValue(row,
                            "dimension", "parameterName", "parameterId")), BODY_FONT, background);
                    addCell(table, str(trimmingValue(row,
                                    "measurementStage", "measurementType", "stageName", "stage")),
                            BODY_FONT, background);
                    addCell(table, str(trimmingValue(row,
                            "specified", "specification", "spec")), BODY_FONT, background);
                    for (String reading : List.of("R2T", "R2B", "R1R", "R1L")) {
                        Object readingValue = trimmingValue(row, reading);
                        addCell(table, str(readingValue != null
                                        ? readingValue : trimmingValue(readings, reading)),
                                BODY_FONT, background);
                    }
                    alternate = !alternate;
                }
                doc.add(table);
            }

            Map<String, Object> drawing = trimmingDrawing(motor);
            Object drawingDescription = trimmingValue(drawing, "description", "drawingDescription");
            Object drawingFile = trimmingValue(drawing, "drawingFile", "drawingFiles", "drawingImage");
            if (hasQcDisplayValue(drawingDescription) || hasQcDisplayValue(drawingFile)) {
                addQcEvaluationHeading(doc, "Drawing", 0);
                PdfPTable table = createTable(new float[]{24, 76});
                if (hasQcDisplayValue(drawingDescription)) {
                    addCell(table, "Description", BODY_BOLD, ROW_ALT);
                    addCell(table, qcEvaluationCellValue(drawingDescription), BODY_FONT, WHITE);
                }
                if (hasQcDisplayValue(drawingFile)) {
                    addCell(table, "Drawing File", BODY_BOLD, ROW_ALT);
                    addCell(table, qcEvaluationCellValue(drawingFile), BODY_FONT, WHITE);
                }
                doc.add(table);
                if (drawingFile instanceof List<?> fileList) {
                    List<DocumentDto> docs = new ArrayList<>();
                    for (Object item : fileList) {
                        if (item instanceof DocumentDto documentDto) {
                            docs.add(documentDto);
                        } else if (item instanceof Map<?, ?> map) {
                            DocumentDto dto = new DocumentDto();
                            Object fileId = map.get("fileId");
                            Object fileName = map.get("fileName");
                            Object fileUrl = map.get("fileUrl");
                            Object mimeType = map.get("mimeType");
                            if (fileId != null) dto.setFileId(String.valueOf(fileId));
                            if (fileName != null) dto.setFileName(String.valueOf(fileName));
                            if (fileUrl != null) dto.setFileUrl(String.valueOf(fileUrl));
                            if (mimeType != null) dto.setMimeType(String.valueOf(mimeType));
                            docs.add(dto);
                        }
                    }
                    addTrimmingDrawingImages(doc, docs);
                }
            }

            Map<String, Object> remarks = trimmingRemarks(motor);
            Object remarksText = trimmingValue(remarks, "remarks");
            Object report = trimmingValue(remarks,
                    "reportLink", "uploadReport", "report", "attachment");
            if (hasQcDisplayValue(remarksText) || hasQcDisplayValue(report)) {
                addQcEvaluationHeading(doc, "Remarks & Attachments", 0);
                PdfPTable table = createTable(new float[]{24, 76});
                if (hasQcDisplayValue(remarksText)) {
                    addCell(table, "Remarks", BODY_BOLD, ROW_ALT);
                    addCell(table, qcEvaluationCellValue(remarksText), BODY_FONT, WHITE);
                }
                if (hasQcDisplayValue(report)) {
                    addCell(table, "Upload Report", BODY_BOLD, ROW_ALT);
                    addCell(table, qcEvaluationCellValue(report), BODY_FONT, WHITE);
                }
                doc.add(table);
            }
        }
        return true;
    }

    private boolean renderQcNdtDivision(
            Document doc,
            Map<String, Object> data) throws DocumentException {
        List<Map<String, Object>> motors = qcMapList(qcValue(data, "motorDetails", "motors"));
        if (motors.isEmpty()) return false;

        for (int motorIndex = 0; motorIndex < motors.size(); motorIndex++) {
            Map<String, Object> motor = qcMergedMotorDetails(motors.get(motorIndex));
            String motorId = qcText(qcValue(motor, "motorId", "motorIdNo"));
            if (motorId == null) motorId = "Motor " + (motorIndex + 1);

            // The division title already identifies this as NDT; avoid a
            // redundant per-motor "Motor-101 — NDT" heading.

            List<Map<String, Object>> radiographyRows = new ArrayList<>();
            List<Map<String, Object>> observationRows = new ArrayList<>();
            List<Map<String, Object>> visualRows = new ArrayList<>();
            java.util.LinkedHashSet<String> media = new java.util.LinkedHashSet<>();

            appendQcNdtRadiographyRows(
                    radiographyRows, qcValue(motor, "radiographyDetails"));
            observationRows.addAll(qcMapList(qcValue(
                    motor, "radiographyObservations")));
            visualRows.addAll(qcMapList(qcValue(
                    motor, "visualInspectionDetails", "visualInspection")));
            collectQcCertificates(qcValue(
                    motor, "uploadedVideos", "uploadedPhotos", "mediaDetails", "uploads"), media);

            List<Map<String, Object>> processes = qcMapList(qcValue(
                    motor, "processDetails", "processes"));
            for (Map<String, Object> process : processes) {
                String processType = normalizePdfKey(qcValue(
                        process, "process", "processType", "type"));
                if (processType.contains("RADIOGRAPHY")) {
                    appendQcNdtRadiographyRows(radiographyRows, qcValue(
                            process, "radiographyDetails", "details", "data"));
                    observationRows.addAll(qcMapList(qcValue(
                            process, "radiographyObservations", "observations")));
                } else if (processType.contains("VISUAL")) {
                    visualRows.addAll(qcMapList(qcValue(
                            process, "visualInspectionDetails", "details", "data")));
                } else if (processType.contains("UPLOAD") || processType.contains("MEDIA")) {
                    collectQcCertificates(qcValue(
                            process, "mediaDetails", "details", "data", "uploads"), media);
                }
            }

            if (!radiographyRows.isEmpty()) {
                renderQcEvaluationRows(doc, "Radiography Details",
                        qcRowsWithSerialNumbers(radiographyRows), 0);
            }
            if (!observationRows.isEmpty()) {
                renderQcEvaluationRows(doc, "Radiography Observations",
                        qcRowsWithSerialNumbers(observationRows), 0);
            }
            if (!visualRows.isEmpty()) {
                renderQcEvaluationRows(doc, "Visual Inspection",
                        qcRowsWithSerialNumbers(visualRows), 0);
            }
            if (!media.isEmpty()) {
                addQcEvaluationHeading(doc, "Upload Videos / Photos", 0);
                doc.add(new Paragraph(String.join(", ", media), BODY_FONT));
            }
        }
        return true;
    }

    private void appendQcNdtRadiographyRows(
            List<Map<String, Object>> target,
            Object value) {
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) appendQcNdtRadiographyRows(target, item);
            return;
        }
        if (!(value instanceof Map<?, ?> rawMap)) return;

        Map<String, Object> radiography = stringKeyMap(rawMap);
        List<Map<String, Object>> plans = qcMapList(qcValue(
                radiography, "radiographyPlanDetails", "planDetails", "plans"));
        if (plans.isEmpty()) {
            target.add(radiography);
            return;
        }

        for (Map<String, Object> plan : plans) {
            Map<String, Object> row = new LinkedHashMap<>();
            Object machine = qcValue(radiography,
                    "machineNo", "equipmentUtilized", "equipment", "machineNumber");
            if (machine != null) row.put("machineNo", machine);
            row.putAll(plan);
            target.add(row);
        }
    }

    private List<Map<String, Object>> qcRowsWithSerialNumbers(
            List<Map<String, Object>> rows) {
        List<Map<String, Object>> numberedRows = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            Map<String, Object> source = rows.get(index);
            Map<String, Object> row = new LinkedHashMap<>();
            Object serialNumber = qcValue(source, "srNo", "sampleNo", "rowNo");
            row.put("srNo", serialNumber != null ? serialNumber : index + 1);
            source.forEach((key, value) -> {
                if (!java.util.Set.of("SRNO", "SAMPLENO", "ROWNO")
                        .contains(normalizePdfKey(key))) {
                    row.put(key, value);
                }
            });
            numberedRows.add(row);
        }
        return numberedRows;
    }

    private Map<String, Object> qcMergedMotorDetails(Map<String, Object> sourceMotor) {
        Map<String, Object> motor = new LinkedHashMap<>(sourceMotor);
        Object detailsValue = qcValue(motor, "details");
        if (detailsValue instanceof Map<?, ?> rawDetails) {
            motor.remove("details");
            motor.putAll(stringKeyMap(rawDetails));
        }
        return motor;
    }

    private void removeQcCastingNonReportFields(Map<String, Object> value) {
        if (value == null || value.isEmpty()) return;
        List<String> ignored = List.of(
                "MOTORRECEIVEDAT", "MOTORRECEIVEDDATE", "MOTORRECEIVEDDATETIME",
                "SETUP", "EMPTYMOTORWEIGHT", "FINALMIXBOWLDETAILS",
                "INITIALVACUUM", "VACUUMPRESSURECASTING", "VACUUMPRESSURESOAKING");
        value.entrySet().removeIf(entry -> ignored.contains(normalizePdfKey(entry.getKey())));
        for (Object nested : value.values()) {
            if (nested instanceof Map<?, ?> rawMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) rawMap;
                removeQcCastingNonReportFields(nestedMap);
            } else if (nested instanceof Collection<?> collection) {
                for (Object item : collection) {
                    if (item instanceof Map<?, ?> rawMap) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> nestedMap = (Map<String, Object>) rawMap;
                        removeQcCastingNonReportFields(nestedMap);
                    }
                }
            }
        }
    }

    private boolean renderQcMotorProcessDivision(
            Document doc,
            String divisionKey,
            Map<String, Object> data) throws DocumentException {
        String unitsKey = switch (divisionKey) {
            case "CURING" -> "curingDetails";
            case "DECORING" -> "deCoringDetails";
            case "POSTCURE" -> "postCureMotorDetails";
            default -> "motorDetails";
        };
        List<Map<String, Object>> motors = qcMapList(qcValue(data, unitsKey, "motors"));
        if (motors.isEmpty()) return false;

        for (int motorIndex = 0; motorIndex < motors.size(); motorIndex++) {
            Map<String, Object> sourceMotor = motors.get(motorIndex);
            String motorId = qcText(qcValue(sourceMotor, "motorIdNo", "motorId"));
            if (motorId == null) motorId = "Motor " + (motorIndex + 1);

            String motorHeading = "POSTCURE".equals(divisionKey)
                    ? motorId + " — Loose Flap Filling"
                    : motorId;
            Paragraph motorTitle = new Paragraph(motorHeading, LABEL_FONT);
            motorTitle.setSpacingBefore(8);
            motorTitle.setSpacingAfter(4);
            doc.add(motorTitle);

            Map<String, Object> motor = qcMergedMotorDetails(sourceMotor);
            motor.remove("motorIdNo");
            motor.remove("motorId");
            if ("DECORING".equals(divisionKey)) {
                renderQcDivisionMap(doc, "De-coring Details", motor, 0);
                continue;
            }
            if ("CASTING".equals(divisionKey)) {
                Map<String, Object> setup = qcMap(qcValue(motor, "setup"));
                Object castingType = qcValue(setup, "castingType");
                removeQcCastingNonReportFields(motor);
                if (hasQcDisplayValue(castingType)) {
                    PdfPTable castingTypeTable = createTable(new float[]{34, 66});
                    addCell(castingTypeTable, "Type of Casting", BODY_BOLD, ROW_ALT);
                    addCell(castingTypeTable, qcDisplayValue(castingType), BODY_FONT, WHITE);
                    doc.add(castingTypeTable);
                }
                renderQcCastingSections(doc, motorId, motor);
                continue;
            }
            String motorDetailsHeading = switch (divisionKey) {
                case "CURING" -> "Curing Configuration";
                case "DECORING" -> "De-coring Details";
                default -> null;
            };
            renderQcEvaluationValue(
                    doc,
                    motorDetailsHeading,
                    motor,
                    0);
        }
        return true;
    }

    private void renderQcCastingSections(Document doc, String motorId, Map<String, Object> motor)
            throws DocumentException {
        Map<String, Object> sections = qcMap(qcValue(motor, "castingSections", "sections"));
        renderQcCastingSection(doc, "Final Assembly", qcValue(sections, "finalAssemblyDetails"));
        renderQcCastingSection(doc, "Propellant Casting", qcValue(sections, "castingProcess"));
        Object weighment = qcValue(sections, "weightmentDetails", "weighmentDetails");
        renderQcCastingSection(doc, "Weighment Details", weighment);
        renderQcCastingSection(doc, "Post Cast Operations", qcValue(sections, "postCastOperations"));
    }

    private void renderQcCastingSection(Document doc, String title, Object section)
            throws DocumentException {
        if (!hasQcDisplayValue(section)) return;
        renderQcEvaluationValue(doc, title, section, 0);
    }

    private void renderQcEvaluationValue(
            Document doc,
            String heading,
            Object value,
            int depth) throws DocumentException {
        if (!hasQcDisplayValue(value) || depth > 10) return;

        if (value instanceof Collection<?> collection) {
            List<Map<String, Object>> rows = qcMapList(collection);
            boolean sectionCollection = !rows.isEmpty()
                    && rows.size() == collection.size()
                    && rows.stream().allMatch(row -> qcValue(row, "sectionId") != null
                    && qcValue(row, "sectionData") != null);
            if (sectionCollection) {
                if (rows.size() == 1) {
                    Map<String, Object> section = rows.getFirst();
                    String sectionHeading = qcEvaluationTitle(
                            String.valueOf(qcValue(section, "sectionId")));
                    if (heading == null || sameQcHeading(heading, sectionHeading)) {
                        renderQcEvaluationValue(
                                doc,
                                heading != null ? heading : sectionHeading,
                                qcValue(section, "sectionData"),
                                depth);
                        return;
                    }
                }
                addQcEvaluationHeading(doc, heading, depth);
                for (Map<String, Object> section : rows) {
                    String sectionHeading = qcEvaluationTitle(
                            String.valueOf(qcValue(section, "sectionId")));
                    renderQcEvaluationValue(
                            doc,
                            sameQcHeading(heading, sectionHeading) ? null : sectionHeading,
                            qcValue(section, "sectionData"),
                            depth + 1);
                }
                return;
            }
            boolean singleNestedContainer = rows.size() == 1
                    && collection.size() == 1
                    && rows.getFirst().entrySet().stream().anyMatch(entry ->
                    hasQcDisplayValue(entry.getValue())
                            && !isQcInlineEvaluationValue(entry.getValue()));
            if (singleNestedContainer) {
                renderQcEvaluationValue(doc, heading, rows.getFirst(), depth);
                return;
            }
            if (rows.size() == collection.size() && !rows.isEmpty()) {
                renderQcEvaluationRows(doc, heading, rows, depth);
            } else if (collection.stream().allMatch(this::isQcScalarValue)) {
                addQcEvaluationHeading(doc, heading, depth);
                doc.add(new Paragraph(qcDisplayValue(collection), BODY_FONT));
            }
            return;
        }

        if (!(value instanceof Map<?, ?> rawMap)) {
            if (!isQcScalarValue(value)) {
                Map<String, Object> converted = qcObjectMap(value);
                if (!converted.isEmpty()) {
                    renderQcEvaluationValue(doc, heading, converted, depth);
                    return;
                }
            }
            addQcEvaluationHeading(doc, heading, depth);
            doc.add(new Paragraph(qcDisplayValue(value), BODY_FONT));
            return;
        }

        Map<String, Object> map = stringKeyMap(rawMap);
        Map<String, Object> scalarFields = new LinkedHashMap<>();
        Map<String, Object> attachmentFields = new LinkedHashMap<>();
        Map<String, Object> nestedFields = new LinkedHashMap<>();
        Object qaRemarks = null;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (normalizePdfKey(entry.getKey()).endsWith("QAREMARKS")) {
                qaRemarks = entry.getValue();
                continue;
            }
            if (isQcWorkflowField(entry.getKey()) || !hasQcDisplayValue(entry.getValue())) continue;
            if (isQcInlineEvaluationValue(entry.getValue())) {
                if (isQcAttachmentField(entry.getKey())) {
                    attachmentFields.put(entry.getKey(), entry.getValue());
                } else {
                    scalarFields.put(entry.getKey(), entry.getValue());
                }
            } else {
                nestedFields.put(entry.getKey(), entry.getValue());
            }
        }

        if (scalarFields.isEmpty() && nestedFields.size() == 1) {
            Map.Entry<String, Object> only = nestedFields.entrySet().iterator().next();
            String nestedHeading = isQcTransparentContainer(only.getKey())
                    ? null
                    : qcEvaluationTitle(only.getKey());
            if (heading == null
                    || nestedHeading == null
                    || sameQcHeading(heading, nestedHeading)) {
                renderQcEvaluationValue(
                        doc,
                        heading != null ? heading : nestedHeading,
                        only.getValue(),
                        depth);
                renderQcEvaluationAttachments(doc, attachmentFields, depth);
                renderQcQaRemarks(doc, qaRemarks);
                return;
            }
        }

        addQcEvaluationHeading(doc, heading, depth);
        renderQcCompactFields(doc, scalarFields);
        for (Map.Entry<String, Object> nested : nestedFields.entrySet()) {
            String nestedHeading = isQcTransparentContainer(nested.getKey())
                    ? null : qcEvaluationTitle(nested.getKey());
            renderQcEvaluationValue(
                    doc,
                    sameQcHeading(heading, nestedHeading) ? null : nestedHeading,
                    nested.getValue(), depth + 1);
        }
        renderQcEvaluationAttachments(doc, attachmentFields, depth);
        // QA remarks belong to this section, so keep them directly after its
        // rendered fields/table instead of collecting them at division level.
        renderQcQaRemarks(doc, qaRemarks);
    }

    private void renderQcEvaluationAttachments(
            Document doc,
            Map<String, Object> attachmentFields,
            int depth) throws DocumentException {
        for (Map.Entry<String, Object> attachment : attachmentFields.entrySet()) {
            List<Map<String, Object>> files = qcMapList(attachment.getValue());
            if (!files.isEmpty() && files.stream().allMatch(file ->
                    qcValue(file, "fileId", "fileName") != null)) {
                renderQcAttachmentRows(doc, qcEvaluationTitle(attachment.getKey()), files, depth + 1);
                continue;
            }
            addQcEvaluationHeading(doc, qcEvaluationTitle(attachment.getKey()), depth + 1);
            PdfPTable table = createTable(new float[]{30, 70});
            addHeaders(table, "Property", "Value");
            Map<String, Object> file = qcObjectMap(attachment.getValue());
            for (Map.Entry<String, Object> field : file.entrySet()) {
                String key = normalizePdfKey(field.getKey());
                if (java.util.Set.of("FILEID", "FILEURL", "MIMETYPE", "READONLY").contains(key)) continue;
                addCell(table, qcColumnLabel(field.getKey()), BODY_BOLD, ROW_ALT);
                addCell(table, qcDisplayValue(field.getValue()), BODY_FONT, WHITE);
            }
            if (table.size() > 1) doc.add(table);
        }
    }

    private boolean sameQcHeading(String first, String second) {
        return first != null && second != null
                && normalizePdfKey(first).equals(normalizePdfKey(second));
    }

    private void renderQcEvaluationRows(
            Document doc,
            String heading,
            List<Map<String, Object>> rows,
            int depth) throws DocumentException {
        if (isQcAttachmentRows(rows)) {
            renderQcAttachmentRows(doc, heading, rows, depth);
            return;
        }

        String headingKey = normalizePdfKey(heading);
        boolean numberedDomainTable = java.util.Set.of(
                        "BELLOWBONDINGDETAILS", "BELLOWREMOVALDETAILS",
                        "QUALIFICATIONDETAILS", "LFEPOXYFILLINGDETAILS")
                .contains(headingKey);
        boolean hasIdentityColumn = rows.stream().flatMap(row -> row.keySet().stream())
                .map(this::normalizePdfKey)
                .anyMatch(key -> java.util.Set.of("SRNO", "SAMPLENO", "ROWNO")
                        .contains(key));
        if (numberedDomainTable && !hasIdentityColumn) {
            rows = qcRowsWithSerialNumbers(rows);
        }

        java.util.LinkedHashSet<String> columns = new java.util.LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                // Row-level QA remarks are part of the inspection record and
                // belong in the same table row as their measurements.
                if ((!isQcWorkflowField(entry.getKey())
                        || "QAREMARKS".equals(normalizePdfKey(entry.getKey())))
                        && hasQcDisplayValue(entry.getValue())
                        && isQcInlineEvaluationValue(entry.getValue())) {
                    columns.add(entry.getKey());
                }
            }
        }
        if (columns.isEmpty()) {
            for (int index = 0; index < rows.size(); index++) {
                renderQcDivisionMap(doc, "Row " + (index + 1), rows.get(index), depth + 1);
            }
            return;
        }

        addQcEvaluationHeading(doc, heading, depth);
        List<String> allColumns = new ArrayList<>(columns);
        allColumns.removeIf(column -> "QAREMARKS".equals(normalizePdfKey(column)));
        if (rows.stream().anyMatch(row -> hasQcDisplayValue(qcValue(row, "qaRemarks")))) {
            allColumns.add("qaRemarks");
        }
        if (allColumns.size() <= 9) {
            renderQcEvaluationTable(doc, rows, allColumns);
        } else {
            String identityColumn = allColumns.stream()
                    .filter(column -> java.util.Set.of("SRNO", "SAMPLENO", "ROWNO")
                            .contains(normalizePdfKey(column)))
                    .findFirst()
                    .orElse(allColumns.getFirst());
            List<String> remaining = allColumns.stream()
                    .filter(column -> !column.equals(identityColumn))
                    .toList();
            for (int offset = 0; offset < remaining.size(); offset += 8) {
                List<String> tableColumns = new ArrayList<>();
                tableColumns.add(identityColumn);
                tableColumns.addAll(remaining.subList(
                        offset, Math.min(offset + 8, remaining.size())));
                if (offset > 0) {
                    Paragraph continued = new Paragraph("Additional columns", SUBTITLE_FONT);
                    continued.setSpacingBefore(3);
                    continued.setSpacingAfter(2);
                    doc.add(continued);
                }
                renderQcEvaluationTable(doc, rows, tableColumns);
            }
        }

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Map<String, Object> nested = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : rows.get(rowIndex).entrySet()) {
                if (!columns.contains(entry.getKey())
                        && !isQcWorkflowField(entry.getKey())
                        && hasQcDisplayValue(entry.getValue())) {
                    nested.put(entry.getKey(), entry.getValue());
                }
            }
            if (!nested.isEmpty()) {
                renderQcEvaluationValue(
                        doc, "Additional details — row " + (rowIndex + 1), nested, depth + 1);
            }
        }
    }

    private boolean isQcAttachmentRows(List<Map<String, Object>> rows) {
        return rows != null && !rows.isEmpty()
                && rows.stream().allMatch(row -> qcValue(row, "fileId", "fileName") != null
                && (row.containsKey("fileId") || row.containsKey("fileName")));
    }

    /**
     * Keep internal file paths and storage metadata out of the human-readable QC PDF.
     */
    private void renderQcAttachmentRows(
            Document doc,
            String heading,
            List<Map<String, Object>> rows,
            int depth) throws DocumentException {
        addQcEvaluationHeading(doc, heading, depth);
        PdfPTable table = createTable(new float[]{78, 22});
        table.setHeaderRows(1);
        addHeaders(table, "File Name", "Status");
        boolean alternate = false;
        for (Map<String, Object> row : rows) {
            Color background = alternate ? ROW_ALT : WHITE;
            Object fileName = qcValue(row, "fileName", "originalFileName", "name");
            if (fileName == null) {
                fileName = qcValue(row, "fileUrl");
            }
            addCell(table, fileName == null ? "—" : qcAttachmentDisplayName(String.valueOf(fileName)),
                    BODY_FONT, background);
            Object status = qcValue(row, "status", "fileStatus", "uploadStatus");
            addCell(table, status == null ? "—" : str(status), BODY_FONT, background);
            alternate = !alternate;
        }
        doc.add(table);
    }

    private String qcAttachmentDisplayName(String value) {
        if (value == null || value.isBlank()) return "—";
        String normalized = value.replace('\\', '/');
        int separator = normalized.lastIndexOf('/');
        return separator >= 0 ? normalized.substring(separator + 1) : normalized;
    }

    private void renderQcEvaluationTable(
            Document doc,
            List<Map<String, Object>> rows,
            List<String> columns) throws DocumentException {
        float[] widths = new float[columns.size()];
        int columnIndex = 0;
        for (String column : columns) {
            String key = normalizePdfKey(column);
            widths[columnIndex++] = key.contains("PROPERTY") || key.contains("DETAIL")
                    ? 2.2f
                    : key.contains("REMARK") || key.contains("UPLOAD")
                    || key.contains("GRAPH") || key.contains("FILE")
                    ? 1.8f
                    : key.contains("SAMPLENO") || key.equals("SRNO")
                    ? 0.8f
                    : 1.15f;
        }

        PdfPTable table = createTable(widths);
        table.setHeaderRows(1);
        addHeaders(table, columns.stream().map(this::qcColumnLabel).toArray(String[]::new));
        boolean alternate = false;
        for (Map<String, Object> row : rows) {
            Color background = alternate ? ROW_ALT : WHITE;
            for (String column : columns) {
                String value = "MATERIALTYPE".equals(normalizePdfKey(column))
                        ? qcMaterialTypeLabel(row.get(column))
                        : qcEvaluationCellValue(row.get(column));
                addCell(table, value, BODY_FONT, background);
            }
            alternate = !alternate;
        }
        doc.add(table);
    }

    private boolean isQcInlineEvaluationValue(Object value) {
        if (isQcScalarValue(value) || isQcScalarCollection(value)) return true;
        Map<String, Object> map = value instanceof Map<?, ?> rawMap
                ? stringKeyMap(rawMap) : qcObjectMap(value);
        if (map.isEmpty()) return isQcSpecificationValue(value, map);
        return qcValue(map, "fileName", "documentId", "fileUrl") != null
                || isQcSpecificationValue(value, map);
    }

    private boolean isQcAttachmentField(String key) {
        String normalized = normalizePdfKey(key);
        return normalized.contains("UPLOAD")
                || normalized.contains("GRAPH")
                || normalized.contains("ATTACHMENT")
                || normalized.contains("CERTIFICATE")
                || normalized.contains("FILE")
                || normalized.contains("REPORT");
    }

    private boolean isQcTransparentContainer(String key) {
        return java.util.Set.of(
                        "DETAILS", "PROCESSDETAILS", "PROCESSES", "FORMDETAILS", "DATA",
                        "SECTIONS", "LOOSEFLAPFILLING", "LOOSEFLAPFILLINGDETAILS")
                .contains(normalizePdfKey(key));
    }

    private String qcEvaluationCellValue(Object value) {
        if (isQcSpecificationValue(value, qcObjectMap(value))) {
            return qcSpecification(value);
        }
        if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = stringKeyMap(rawMap);
            Object file = qcValue(map, "fileName", "documentId", "fileUrl");
            return file != null ? str(file) : qcDisplayValue(map);
        }
        return qcDisplayValue(value);
    }

    private String ccCastingActivityLabel(String value) {
        if (value == null) return null;
        return switch (normalizePdfKey(value)) {
            case "SOAKINGTIME" -> "Soaking Date & Time";
            case "TIMEOFREMOVALFROMPIT" -> "Date & Time of Removal from Pit";
            case "TIMEOFDISPATCHTOCURINGSTATION" -> "Date & Time of Dispatch to Curing Station";
            default -> value;
        };
    }

    private void addQcEvaluationHeading(Document doc, String heading, int depth)
            throws DocumentException {
        if (heading == null || heading.isBlank()
                || java.util.Set.of("ROWS", "SAMPLES", "VALUES", "DATA")
                .contains(normalizePdfKey(heading))) {
            return;
        }
        Font font = depth == 0 ? BODY_BOLD : new Font(
                Font.HELVETICA, 8, depth == 1 ? Font.BOLD : Font.ITALIC,
                depth == 1 ? Color.BLACK : TEXT_GRAY);
        Paragraph paragraph = new Paragraph(heading, font);
        paragraph.setSpacingBefore(4);
        paragraph.setSpacingAfter(3);
        doc.add(paragraph);
    }

    private String qcProcessTitle(String processType) {
        String key = normalizePdfKey(processType);
        if (key.contains("MECHANICAL")) return "Mechanical Properties Evaluation";
        if (key.contains("INTERFACE")) return "Interface Properties";
        if (key.contains("SSBR") || key.contains("UBR")) return "SSBR / UBR Burn Rate";
        if (key.contains("BALLISTIC") || key.contains("BEM")) {
            return "Ballistic Evaluation Motor Burn Rate";
        }
        return processType == null ? "Evaluation Details" : prettyText(processType);
    }

    private String qcEvaluationTitle(String key) {
        String normalized = normalizePdfKey(key);
        String domainTitle = switch (normalized) {
            case "ABRADING" -> "Abrading";
            case "FIRSTCUT" -> "First Cut";
            case "SECONDCUT" -> "Second Cut";
            case "PREHEATING", "PREHEATINGDETAILS" -> "Preheating Details";
            case "LINERCOATING", "LINERCOATINGDETAILS" -> "Liner Coating Details";
            case "DISPATCH", "DISPATCHDETAILS" -> "Dispatch";
            case "CASTINGSELECTION", "CASTINGCONFIGURATION" -> "Casting Configuration";
            case "FINALASSEMBLY" -> "Final Assembly";
            case "MANDRELASSEMBLY" -> "Mandrel Assembly";
            case "PROPELLANTCASTING" -> "Propellant Casting";
            case "CASTINGDETAILS" -> "Casting Details";
            case "WEIGHTMENTDETAILS" -> "Weighment Details";
            case "POSTCASTOPERATIONS" -> "Post Cast Operations";
            case "SOAKINGDETAILS" -> "Soaking Details";
            case "PRESSUREPLATEASSEMBLY" -> "Pressure Plate Assembly";
            case "PRESSUREPLATEDETAILS" -> "Pressure Plate Assembly Details";
            case "CURINGCONFIGURATION" -> "Curing Configuration";
            case "CURINGSECTIONS", "CURINGCYCLEDETAILS", "CURINGCYCLES" -> "Curing Cycle Details";
            case "POSTCURINGDETAILS", "POSTCUREDETAILS" -> "Post Curing Details";
            case "SUBSCALEDETAILS", "SUBSCALEARTICLESCURING" -> "Subscale Articles Curing";
            case "CURINGTABLE", "CURINGPARAMETERS" -> "Curing Parameters";
            case "DECORINGDETAILS", "DECORING" -> "De-coring Details";
            case "TRIMMINGOPERATIONDETAILS", "TRIMMINGDETAILS" -> "Trimming Details";
            case "TRIMMINGMEASUREMENTDETAILS", "DIMENSIONSAFTERTRIMMING" -> "Dimensions After Trimming";
            case "TRIMMINGREMARKS" -> "Remarks & Attachments";
            case "TRIMMINGDRAWING" -> "Drawing";
            case "BELLOWBONDINGDETAILS", "BELLOWREMOVALDETAILS" -> "Bellow Bonding Details";
            case "LOOSEFLAPEPOXYDETAILS", "LOOSEFLAPEPOXYPREPARATION" -> "Loose Flap Epoxy Details";
            case "QUALIFICATIONDETAILS", "LFEPOXYQUALIFICATION" -> "Qualification Details";
            case "LFEPOXYFILLINGDETAILS" -> "LF Epoxy Filling Details";
            case "UPLOADQCREPORT", "QCREPORT" -> "Upload QC Report";
            case "RADIOGRAPHYDETAILS" -> "Radiography Details";
            case "RADIOGRAPHYOBSERVATIONS" -> "Radiography Observations";
            case "VISUALINSPECTION", "VISUALINSPECTIONDETAILS" -> "Visual Inspection";
            case "UPLOADMEDIA", "UPLOADEDVIDEOS", "UPLOADEDVIDEOSPHOTOS" -> "Upload Videos / Photos";
            default -> null;
        };
        if (domainTitle != null) return domainTitle;
        if (normalized.contains("MECHANICAL")) return "Mechanical Properties Evaluation";
        if (normalized.contains("INTERFACE")) return "Interface Properties";
        if (normalized.contains("SSBR") || normalized.contains("UBR")) {
            return "SSBR / UBR Burn Rate";
        }
        if (normalized.contains("BALLISTIC") || normalized.contains("BEM")) {
            return "Ballistic Evaluation Motor Burn Rate";
        }
        if (normalized.contains("UPLOADGRAPH") || normalized.equals("GRAPH")) return "Upload Graph";
        return prettyText(key);
    }

    private String qcColumnLabel(String key) {
        return switch (normalizePdfKey(key)) {
            case "FM1", "FINALMIX1" -> "FM-1";
            case "FM2", "FINALMIX2" -> "FM-2";
            case "AVG", "AVERAGE" -> "Avg";
            case "STDDEV", "STANDARDDEVIATION" -> "Std Dev";
            case "SPEC", "SPECS" -> "Specification";
            case "SAMPLENO" -> "Sample No";
            case "SRNO", "ROWNO" -> "Sr. No.";
            case "R2T" -> "R2T";
            case "R2B" -> "R2B";
            case "R1R" -> "R1R";
            case "R1L" -> "R1L";
            case "PARAMETERID", "PARAMETERNAME", "DIMENSION" -> "Parameter";
            case "MEASUREMENTSTAGE", "MEASUREMENTTYPE", "STAGENAME" -> "Stage";
            case "SPECIFIED" -> "Specification";
            case "MACHINENO", "MACHINENUMBER" -> "Machine No.";
            case "NUMBEROFSECTIONS", "NOOFSECTIONS" -> "No. of Sections";
            case "NUMBEROFORIENTATIONS", "NOOFORIENTATIONS" -> "No. of Orientations";
            case "NUMBEROFNORMALEXPOSURES", "NOOFNORMALEXPOSURES",
                 "NORMALEXPOSURES" -> "No. of Normal Exposures";
            case "NUMBEROFTANGENTIALEXPOSURES", "NOOFTANGENTIALEXPOSURES",
                 "TANGENTIALEXPOSURES" -> "No. of Tangential Exposures";
            case "TYPEOFDEFECT" -> "Type of Defect";
            case "OBSERVATIONTYPE" -> "Observations";
            case "UPLOADEDIMAGES", "UPLOADIMAGE" -> "Upload Image";
            case "ACEMBATCHNO", "MFGBATCHLOTNO" -> "Mfg Batch Lot Number";
            case "AGITATORRPM" -> "Agitator Rpm";
            case "PROCESSDATETIME" -> "Process Date & Time";
            case "STARTDATETIME" -> "Start Date & Time";
            case "ENDDATETIME" -> "End Date & Time";
            case "DISPATCHDATETIME" -> "Dispatch Date & Time";
            case "TOTALQUANTITYSENTFORPREMIX" -> "Total Quantity Sent For Premix";
            case "QTYOFDUSTGMS", "QUANTITYOFDUST" -> "Qty of Dust (gms)";
            case "OVENNO", "OVENNUMBER" -> "Oven Number";
            case "BLDGNO", "BUILDINGNO" -> "Building No";
            case "TEMPERATURE", "TEMP" -> "Temperature (°C)";
            case "VACUUMLEVEL", "VACUUM" -> "Vacuum Level (Torr)";
            case "QUANTITYOFLINERUSED", "LINERQUANTITY" -> "Quantity of liner used (g)";
            case "INSULATIONTEMP", "INSULATIONTEMPERATURE" -> "Insulation Temp";
            case "RH", "RELATIVEHUMIDITY" -> "RH";
            case "UPLOAD", "UPLOADGRAPH", "GRAPH", "GRAPHFILE" -> "Upload";
            default -> prettyText(key);
        };
    }

    private void renderQcProcessingItem(
            Document doc,
            Map<String, Object> item,
            int itemIndex,
            Object premixNumber) throws DocumentException {
        String materialCode = qcText(qcValue(
                item, "materialCode", "rawMaterial", "ingredient"));
        String materialName = qcText(qcValue(item, "materialName"));
        String operation = qcText(qcValue(item, "operation", "process", "operationName"));

        StringBuilder title = new StringBuilder("Premix-").append(str(premixNumber));
        String materialIdentity = materialCode != null ? materialCode : materialName;
        if (materialIdentity == null) materialIdentity = "Material " + itemIndex;
        title.append(" ").append(materialIdentity);
        if (materialName != null && !materialName.equalsIgnoreCase(materialIdentity)) {
            title.append(" · ").append(materialName);
        }
        Paragraph itemTitle = new Paragraph(title.toString(), LABEL_FONT);
        itemTitle.setSpacingBefore(7);
        itemTitle.setSpacingAfter(4);
        doc.add(itemTitle);

        List<Map<String, Object>> checks = new ArrayList<>();
        appendQcMaps(checks, qcValue(item, "parameters", "qcChecks", "qualityChecks"));
        renderQcProcessingChecks(doc, checks);

        java.util.LinkedHashSet<String> certificates = new java.util.LinkedHashSet<>();
        collectQcCertificates(qcValue(
                        item, "qcCertificate", "certificates", "attachments", "filePaths"),
                certificates);
        renderQcCertificateLine(doc, certificates);

        Object sections = qcValue(item, "sections");
        if (sections instanceof Collection<?> collection && !collection.isEmpty()) {
            for (Map<String, Object> section : qcMapList(collection)) {
                String sectionId = qcText(qcValue(section, "sectionId", "sectionName"));
                Object sectionData = qcValue(section, "sectionData", "data", "details");
                String sectionHeading = sectionId == null
                        ? "Processing Details" : qcProcessingSectionTitle(sectionId);
                if (!checks.isEmpty() && isQcProcessingChecksKey(sectionHeading)) continue;
                renderQcProcessingSectionValue(
                        doc,
                        sectionHeading,
                        sectionData,
                        0);
            }
        }
    }

    private String qcMaterialTypeLabel(Object value) {
        String materialType = str(value);
        if (materialType == null || materialType.isBlank()) return "—";
        materialType = materialType.trim().replaceFirst("(?i)^DEFAULT[_\\s-]*", "");
        return prettyText(materialType);
    }

    private void renderQcProcessingSectionValue(
            Document doc,
            String heading,
            Object value,
            int depth) throws DocumentException {
        if (!hasQcDisplayValue(value) || depth > 8) return;
        if (isQcProcessingChecksKey(heading)) {
            List<Map<String, Object>> checks = qcMapList(value);
            if (checks.isEmpty() && value instanceof Map<?, ?> map) {
                checks = List.of(stringKeyMap(map));
            }
            renderQcProcessingChecks(doc, checks);
            return;
        }

        if (value instanceof Collection<?> collection) {
            List<Map<String, Object>> rows = qcMapList(collection);
            if (rows.size() == collection.size() && !rows.isEmpty()) {
                if (rows.size() == 1 && rows.getFirst().entrySet().stream().anyMatch(entry ->
                        hasQcDisplayValue(entry.getValue())
                                && !isQcInlineEvaluationValue(entry.getValue()))) {
                    renderQcProcessingSectionValue(doc, heading, rows.getFirst(), depth);
                } else {
                    addQcEvaluationHeading(doc, heading, depth);
                    renderQcEvaluationRows(doc, null, rows, depth + 1);
                }
            } else if (collection.stream().allMatch(this::isQcScalarValue)) {
                addQcEvaluationHeading(doc, heading, depth);
                doc.add(new Paragraph(qcDisplayValue(collection), BODY_FONT));
            }
            return;
        }

        if (!(value instanceof Map<?, ?> rawMap)) {
            addQcEvaluationHeading(doc, heading, depth);
            doc.add(new Paragraph(qcDisplayValue(value), BODY_FONT));
            return;
        }

        Map<String, Object> map = stringKeyMap(rawMap);
        Map<String, Object> scalarFields = new LinkedHashMap<>();
        Map<String, Object> nestedFields = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (isQcWorkflowField(entry.getKey()) || !hasQcDisplayValue(entry.getValue())) continue;
            if (isQcInlineEvaluationValue(entry.getValue())) {
                scalarFields.put(entry.getKey(), entry.getValue());
            } else {
                nestedFields.put(entry.getKey(), entry.getValue());
            }
        }

        if (!scalarFields.isEmpty()) {
            addQcEvaluationHeading(doc, heading, depth);
            renderQcEvaluationTable(
                    doc, List.of(scalarFields), new ArrayList<>(scalarFields.keySet()));
        }
        for (Map.Entry<String, Object> nested : nestedFields.entrySet()) {
            renderQcProcessingSectionValue(
                    doc, qcProcessingSectionTitle(nested.getKey()), nested.getValue(), depth + 1);
        }
    }

    private void renderQcProcessingChecks(
            Document doc,
            List<Map<String, Object>> checks) throws DocumentException {
        if (checks == null || checks.isEmpty()) return;
        boolean hasRemarks = checks.stream().anyMatch(check -> hasQcDisplayValue(qcValue(
                check, "remarks", "observation", "observations")));
        addQcEvaluationHeading(doc, "QC Checks", 0);
        PdfPTable table = createTable(hasRemarks
                ? new float[]{34, 28, 18, 20}
                : new float[]{42, 34, 24});
        table.setHeaderRows(1);
        if (hasRemarks) {
            addHeaders(table, "Parameter", "Specification", "Result", "Remarks");
        } else {
            addHeaders(table, "Parameter", "Specification", "Result");
        }
        boolean alternate = false;
        java.util.LinkedHashSet<String> certificates = new java.util.LinkedHashSet<>();
        for (Map<String, Object> check : checks) {
            Color background = alternate ? ROW_ALT : WHITE;
            addCell(table, str(qcValue(check,
                    "parameter", "parameterName")), BODY_FONT, background);
            addCell(table, qcSpecification(qcValue(check,
                    "specification", "specs", "referenceRange")), BODY_FONT, background);
            addCell(table, str(qcValue(check,
                            "actualValue", "result", "analysedResult", "value")),
                    BODY_FONT, background);
            if (hasRemarks) {
                addCell(table, str(qcValue(check,
                        "remarks", "observation", "observations")), BODY_FONT, background);
            }
            collectQcCertificates(qcValue(
                            check, "qcCertificate", "certificates", "attachments", "filePaths"),
                    certificates);
            alternate = !alternate;
        }
        doc.add(table);
        renderQcCertificateLine(doc, certificates);
    }

    private boolean isQcProcessingChecksKey(String key) {
        String normalized = normalizePdfKey(key);
        return normalized.contains("QCCHECK")
                || normalized.contains("QUALITYCHECK")
                || normalized.equals("PARAMETERS")
                || normalized.equals("PARAMETERCHECKS");
    }

    private String qcProcessingSectionTitle(String key) {
        return isQcProcessingChecksKey(key) ? "QC Checks" : qcEvaluationTitle(key);
    }

    private void renderQcCompactFields(
            Document doc,
            Map<String, Object> fields) throws DocumentException {
        if (fields == null || fields.isEmpty()) return;
        PdfPTable table = createTable(new float[]{22, 28, 22, 28});
        int fieldCount = 0;
        for (Map.Entry<String, Object> field : fields.entrySet()) {
            addCell(table, qcColumnLabel(field.getKey()), BODY_BOLD, ROW_ALT);
            String value = "MATERIALTYPE".equals(normalizePdfKey(field.getKey()))
                    ? qcMaterialTypeLabel(field.getValue())
                    : qcDisplayValue(field.getValue());
            addCell(table, value, BODY_FONT, WHITE);
            fieldCount++;
        }
        if (fieldCount % 2 != 0) {
            addCell(table, "", BODY_BOLD, ROW_ALT);
            addCell(table, "", BODY_FONT, WHITE);
        }
        doc.add(table);
    }

    private List<Map<String, Object>> qcMapList(Object value) {
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }

        return list.stream()
                .filter(Objects::nonNull)
                .map(item -> objectMapper.convertValue(
                        item,
                        new TypeReference<Map<String, Object>>() {
                        }
                ))
                .toList();
    }

    private void appendQcMaps(List<Map<String, Object>> target, Object value) {
        target.addAll(qcMapList(value));
    }

    private Object qcValue(Map<String, Object> source, String... candidateKeys) {
        if (source == null || source.isEmpty()) return null;
        for (String candidate : candidateKeys) {
            if (source.containsKey(candidate)) return source.get(candidate);
        }
        for (String candidate : candidateKeys) {
            String normalizedCandidate = normalizePdfKey(candidate);
            for (Map.Entry<String, Object> entry : source.entrySet()) {
                if (normalizedCandidate.equals(normalizePdfKey(entry.getKey()))) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private String qcText(Object value) {
        if (value == null) return null;
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private String qcSpecification(Object specification) {
        if (specification == null) return "—";
        Map<String, Object> range;
        if (specification instanceof Map<?, ?> rawRange) {
            range = stringKeyMap(rawRange);
        } else if (isQcScalarValue(specification)) {
            return str(specification);
        } else {
            try {
                range = mapper.convertValue(
                        specification,
                        new TypeReference<Map<String, Object>>() {
                        });
            } catch (IllegalArgumentException ex) {
                return str(specification);
            }
        }
        if (range.isEmpty()) return "—";
        boolean specificationRange = isQcSpecificationValue(specification, range);
        Object min = qcValue(range, "minValue", "min");
        Object max = qcValue(range, "maxValue", "max");
        Object unit = qcValue(range, "unit");
        if (min == null && max == null) return specificationRange ? "—" : str(specification);
        StringBuilder result = new StringBuilder();
        if (min != null) result.append(qcRangeValue(min));
        if (min != null && max != null) result.append(" - ");
        if (max != null) result.append(qcRangeValue(max));
        if (unit != null && !unit.toString().isBlank()) result.append(" ").append(unit);
        return result.toString();
    }

    private String qcRangeValue(Object value) {
        if (!(value instanceof Number number)) return str(value);
        try {
            return new java.math.BigDecimal(number.toString())
                    .stripTrailingZeros()
                    .toPlainString();
        } catch (NumberFormatException ex) {
            return str(value);
        }
    }

    private boolean isQcSpecificationValue(
            Object value,
            Map<String, Object> converted) {
        if (value == null) return false;
        if (value instanceof SpecificationRangeDto) return true;
        Map<String, Object> map = converted != null ? converted : Collections.emptyMap();
        return map.keySet().stream()
                .map(this::normalizePdfKey)
                .anyMatch(key -> java.util.Set.of(
                        "MINVALUE", "MAXVALUE", "MIN", "MAX", "UNIT").contains(key));
    }

    private Map<String, Object> qcObjectMap(Object value) {
        if (value instanceof Map<?, ?> map) return stringKeyMap(map);
        if (value == null || isQcScalarValue(value) || value instanceof Collection<?>) {
            return Collections.emptyMap();
        }
        try {
            return mapper.convertValue(
                    value,
                    new TypeReference<Map<String, Object>>() {
                    });
        } catch (IllegalArgumentException ex) {
            return Collections.emptyMap();
        }
    }

    private void collectQcCertificates(
            Object value,
            java.util.Set<String> certificates) {
        if (value == null) return;
        if (value instanceof CharSequence text) {
            if (!text.toString().isBlank()) certificates.add(text.toString());
            return;
        }
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> collectQcCertificates(item, certificates));
            return;
        }
        if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = stringKeyMap(rawMap);
            Object label = qcValue(map, "fileName", "documentId", "name", "fileUrl");
            if (label != null && !label.toString().isBlank()) {
                certificates.add(label.toString());
            } else {
                map.values().forEach(item -> collectQcCertificates(item, certificates));
            }
        }
    }

    private void renderQcCertificateLine(
            Document doc,
            java.util.Set<String> certificates) throws DocumentException {
        if (certificates == null || certificates.isEmpty()) return;
        Paragraph paragraph = new Paragraph(
                "Certificates: " + String.join(", ", certificates), SUBTITLE_FONT);
        paragraph.setSpacingBefore(2);
        paragraph.setSpacingAfter(4);
        doc.add(paragraph);
    }

    private void renderQcDivisionStatusSummary(
            Document doc,
            QcDivisionFormResponse data) throws DocumentException {
        if (data.getDivisionStatuses() == null || data.getDivisionStatuses().isEmpty()) return;

        addSection(doc, "Division Approval Summary", "");
        PdfPTable table = createTable(new float[]{8, 27, 25, 15, 15, 10});
        table.setHeaderRows(1);
        addHeaders(table, "Seq.", "Division", "Status", "Approved", "Pending", "Total");
        boolean alternate = false;
        for (var status : data.getDivisionStatuses()) {
            Color background = alternate ? ROW_ALT : WHITE;
            addCell(table, str(status.getSequence()), BODY_FONT, background);
            addCell(table, qcDivisionDisplayName(status.getDivision()), BODY_FONT, background);
            addWorkflowStatusCell(table, status.getStatus());
            addCell(table, str(status.getApprovedUnitCount()), BODY_FONT, background);
            addCell(table, str(status.getPendingUnitCount()), BODY_FONT, background);
            addCell(table, str(status.getTotalUnitCount()), BODY_FONT, background);
            alternate = !alternate;
        }
        doc.add(table);
    }

    private void renderQcDivisionMotorStatusSummary(
            Document doc,
            QcDivisionFormResponse data) throws DocumentException {
        List<DivisionDetailDto> details = data.getDivisionDetails() == null
                ? Collections.emptyList() : data.getDivisionDetails();
        List<DivisionStatusDto> divisionStatuses = data.getDivisionStatuses() == null
                ? Collections.emptyList() : data.getDivisionStatuses();
        List<QcMotorStatusDto> motorStatuses = data.getMotorStatuses() == null
                ? Collections.emptyList() : data.getMotorStatuses();

        LinkedHashMap<String, String[]> divisions = new LinkedHashMap<>();
        for (DivisionDetailDto detail : details) {
            if (detail == null) continue;
            String division = qcEffectiveDivision(detail.getDivision(), detail.getSubType());
            String key = normalizePdfKey(division);
            if (!key.isBlank()) {
                Object status = detail.getDivisionSubmissionStatus() != null
                        ? detail.getDivisionSubmissionStatus() : detail.getStatus();
                // The detail embedded in the form can lag behind the current
                // division status summary. Keep it as a fallback only.
                divisions.putIfAbsent(key, new String[]{division, workflowStatusLabel(status)});
            }
        }
        for (DivisionStatusDto status : divisionStatuses) {
            if (status == null) continue;
            String key = normalizePdfKey(status.getDivision());
            if (!key.isBlank()) divisions.put(key, new String[]{
                    status.getDivision(), workflowStatusLabel(status.getStatus())});
        }
        for (QcMotorStatusDto motor : motorStatuses) {
            if (motor == null) continue;
            String division = qcEffectiveDivision(motor.getDivision(), motor.getSubType());
            String key = normalizePdfKey(division);
            if (!key.isBlank()) divisions.putIfAbsent(key, new String[]{division, "-"});
        }
        if (divisions.isEmpty()) return;

        addSection(doc, "Division and Motor Status", "");
        PdfPTable table = createTable(new float[]{34, 25, 20, 21});
        table.setHeaderRows(1);
        addHeaders(table, "Division", "Motor", "Motor Status", "Division Status");
        boolean alternate = false;
        for (Map.Entry<String, String[]> entry : divisions.entrySet()) {
            String key = entry.getKey();
            String[] info = entry.getValue();
            List<QcMotorStatusDto> matching = motorStatuses.stream()
                    .filter(Objects::nonNull)
                    .filter(motor -> key.equals(normalizePdfKey(
                            qcEffectiveDivision(motor.getDivision(), motor.getSubType()))))
                    .toList();
            if (matching.isEmpty()) {
                addCell(table, qcDivisionDisplayName(info[0]), BODY_FONT, alternate ? ROW_ALT : WHITE);
                addCell(table, "-", BODY_FONT, alternate ? ROW_ALT : WHITE);
                addCell(table, "-", BODY_FONT, alternate ? ROW_ALT : WHITE);
                addWorkflowStatusCell(table, info[1]);
                alternate = !alternate;
                continue;
            }
            for (QcMotorStatusDto motor : matching) {
                Color background = alternate ? ROW_ALT : WHITE;
                addCell(table, qcDivisionDisplayName(info[0]), BODY_FONT, background);
                addCell(table, str(motor.getMotorId()), BODY_FONT, background);
                addWorkflowStatusCell(table, motor.getMotorSubmissionStatus());
                addWorkflowStatusCell(table, info[1]);
                alternate = !alternate;
            }
        }
        doc.add(table);
    }

    private void renderQcUnitStatusSummaries(
            Document doc,
            QcDivisionFormResponse data) throws DocumentException {
        if (data.getMotorStatuses() != null && !data.getMotorStatuses().isEmpty()) {
            addSection(doc, "Motor Approval Summary", "");
            PdfPTable table = createTable(new float[]{24, 22, 22, 32});
            table.setHeaderRows(1);
            addHeaders(table, "Division", "Motor ID", "Submission Type", "Status");
            boolean alternate = false;
            for (var status : data.getMotorStatuses()) {
                Color background = alternate ? ROW_ALT : WHITE;
                addCell(table, qcDivisionDisplayName(status.getDivision()), BODY_FONT, background);
                addCell(table, str(status.getMotorId()), BODY_FONT, background);
                addCell(table,
                        workflowStatusLabel(status.getMotorSubmissionType()),
                        BODY_FONT,
                        background);
                addWorkflowStatusCell(table, status.getMotorSubmissionStatus());
                alternate = !alternate;
            }
            doc.add(table);
        }

        if (data.getPremixStatuses() != null && !data.getPremixStatuses().isEmpty()) {
            addSection(doc, "Premix and Mixing Approval Summary", "");
            PdfPTable table = createTable(new float[]{24, 17, 12, 20, 27});
            table.setHeaderRows(1);
            addHeaders(table, "Division", "Stage", "Premix", "Submission Type", "Status");
            boolean alternate = false;
            for (var status : data.getPremixStatuses()) {
                Color background = alternate ? ROW_ALT : WHITE;
                addCell(table, qcDivisionDisplayName(status.getDivision()), BODY_FONT, background);
                addCell(table, str(status.getStageType()), BODY_FONT, background);
                addCell(table, str(status.getPremixNo()), BODY_FONT, background);
                addCell(table,
                        workflowStatusLabel(status.getPremixSubmissionType()),
                        BODY_FONT,
                        background);
                addWorkflowStatusCell(table, status.getPremixSubmissionStatus());
                alternate = !alternate;
            }
            doc.add(table);
        }
    }

    private void renderQcDivisionValue(
            Document doc,
            String heading,
            Object value,
            int depth) throws DocumentException {
        if (depth > 12) {
            addQcDivisionHeading(doc, heading, depth);
            doc.add(new Paragraph(qcDisplayValue(value), BODY_FONT));
            return;
        }

        if (value instanceof Map<?, ?> rawMap) {
            if (!rawMap.isEmpty()) {
                renderQcDivisionMap(doc, heading, stringKeyMap(rawMap), depth);
            }
            return;
        }

        if (value instanceof Collection<?> collection) {
            if (collection.isEmpty()) return;
            addQcDivisionHeading(doc, heading, depth);

            boolean scalarCollection = collection.stream().allMatch(this::isQcScalarValue);
            if (scalarCollection) {
                String joined = collection.stream().map(this::qcDisplayValue)
                        .collect(java.util.stream.Collectors.joining(", "));
                doc.add(new Paragraph(joined, BODY_FONT));
                return;
            }

            int index = 1;
            for (Object item : collection) {
                if (item instanceof Map<?, ?> itemMap) {
                    Map<String, Object> mappedItem = stringKeyMap(itemMap);
                    renderQcDivisionMap(
                            doc,
                            qcCollectionItemHeading(heading, index, mappedItem),
                            mappedItem,
                            depth + 1);
                } else {
                    renderQcDivisionValue(doc, "Entry " + index, item, depth + 1);
                }
                index++;
            }
            return;
        }

        if (isQcScalarValue(value)) {
            if (!hasQcDisplayValue(value)) return;
            addQcDivisionHeading(doc, heading, depth);
            doc.add(new Paragraph(str(value), BODY_FONT));
            return;
        }

        try {
            Map<String, Object> converted = mapper.convertValue(
                    value, new TypeReference<Map<String, Object>>() {
                    });
            renderQcDivisionMap(doc, heading, converted, depth);
        } catch (IllegalArgumentException conversionError) {
            addQcDivisionHeading(doc, heading, depth);
            doc.add(new Paragraph(qcDisplayValue(value), BODY_FONT));
        }
    }

    private void renderQcDivisionMap(
            Document doc,
            String heading,
            Map<String, Object> value,
            int depth) throws DocumentException {
        if (value == null || value.isEmpty()) return;

        Map<String, Object> scalarFields = new LinkedHashMap<>();
        Map<String, Object> nestedFields = new LinkedHashMap<>();
        Object qaRemarks = null;
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            if (normalizePdfKey(entry.getKey()).endsWith("QAREMARKS")) {
                qaRemarks = entry.getValue();
                continue;
            }
            if (isQcWorkflowField(entry.getKey()) || !hasQcDisplayValue(entry.getValue())) {
                continue;
            }
            if (isQcScalarValue(entry.getValue())
                    || isQcScalarCollection(entry.getValue())) {
                scalarFields.put(entry.getKey(), entry.getValue());
            } else {
                nestedFields.put(entry.getKey(), entry.getValue());
            }
        }

        if (scalarFields.isEmpty() && nestedFields.isEmpty() && !hasQcDisplayValue(qaRemarks)) return;
        addQcDivisionHeading(doc, heading, depth);

        if (!scalarFields.isEmpty()) {
            PdfPTable table = createTable(new float[]{34, 66});
            for (Map.Entry<String, Object> field : scalarFields.entrySet()) {
                addCell(table, qcColumnLabel(field.getKey()), BODY_BOLD, ROW_ALT);
                addCell(table, qcDisplayValue(field.getValue()), BODY_FONT, WHITE);
            }
            doc.add(table);
        }

        for (Map.Entry<String, Object> nested : nestedFields.entrySet()) {
            renderQcDivisionValue(doc, nested.getKey(), nested.getValue(), depth + 1);
        }
        renderQcQaRemarks(doc, qaRemarks);
    }

    private void addQcDivisionHeading(Document doc, String heading, int depth)
            throws DocumentException {
        if (heading == null || heading.isBlank()) return;
        boolean rowLabel = heading.matches("(?i)^row\\s+\\d+$");
        Font font = rowLabel
                ? new Font(Font.HELVETICA, 7, Font.ITALIC, TEXT_GRAY)
                : depth == 0 ? BODY_BOLD : new Font(
                Font.HELVETICA, 8, depth <= 2 ? Font.BOLD : Font.ITALIC,
                depth <= 2 ? BRAND_BLUE : TEXT_GRAY);
        Paragraph paragraph = new Paragraph(prettyText(heading), font);
        paragraph.setSpacingBefore(depth == 0 ? 7 : 4);
        paragraph.setSpacingAfter(3);
        paragraph.setIndentationLeft(Math.min(depth, 5) * 6f);
        doc.add(paragraph);
    }

    private String qcCollectionItemHeading(
            String collectionName,
            int index,
            Map<String, Object> item) {
        StringBuilder identityHeading = new StringBuilder();
        for (String identityKey : List.of(
                "motorIdNo", "motorId", "premixNo", "stageType", "process",
                "processType", "sectionId", "materialName", "ingredient", "srNo")) {
            Object identity = item.get(identityKey);
            if (identity != null && !identity.toString().isBlank()) {
                if (!identityHeading.isEmpty()) identityHeading.append(" | ");
                identityHeading.append(prettyText(identityKey)).append(": ").append(identity);
                if (!"premixNo".equals(identityKey)) break;
            }
        }
        return identityHeading.isEmpty()
                ? "Row " + index
                : identityHeading.toString();
    }

    private boolean isQcScalarValue(Object value) {
        return value == null
                || value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Enum<?>
                || value instanceof java.time.temporal.TemporalAccessor
                || value instanceof java.util.Date;
    }

    private boolean isQcScalarCollection(Object value) {
        return value instanceof Collection<?> collection
                && collection.stream().allMatch(this::isQcScalarValue);
    }

    private boolean hasQcDisplayValue(Object value) {
        if (value == null) return false;
        if (value instanceof CharSequence text) return !text.toString().isBlank();
        if (value instanceof Collection<?> collection) return !collection.isEmpty();
        if (value instanceof Map<?, ?> map) return !map.isEmpty();
        return true;
    }

    private boolean isQcWorkflowField(String key) {
        return java.util.Set.of(
                        "MOTORSUBMISSIONTYPE", "MOTORSUBMISSIONSTATUS",
                        "PREMIXSUBMISSIONTYPE", "PREMIXSUBMISSIONSTATUS",
                        "DIVISIONSUBMISSIONTYPE", "DIVISIONSUBMISSIONSTATUS",
                        "QAREMARKS", "LOTDETAILSQAREMARKS",
                        "SUBMITTEDBY", "SUBMITTEDAT", "APPROVEDBY", "APPROVEDAT",
                        "ACTIONBY", "ACTIONAT", "UPDATEDBY", "UPDATEDAT")
                .contains(normalizePdfKey(key))
                || normalizePdfKey(key).endsWith("QAREMARKS");
    }

    private String qcDisplayValue(Object value) {
        if (value == null) return "—";
        Map<String, Object> converted = qcObjectMap(value);
        if (isQcSpecificationValue(value, converted)) {
            return qcSpecification(value);
        }
        if (value instanceof Collection<?> collection) {
            if (collection.isEmpty()) return "—";
            return collection.stream().map(this::qcDisplayValue)
                    .collect(java.util.stream.Collectors.joining(", "));
        }
        if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = stringKeyMap(rawMap);
            Object file = qcValue(map, "fileName", "documentId", "fileUrl");
            if (file != null) return str(file);
            String displayed = map.entrySet().stream()
                    .filter(entry -> !isQcWorkflowField(entry.getKey()))
                    .filter(entry -> hasQcDisplayValue(entry.getValue()))
                    .map(entry -> prettyText(entry.getKey()) + ": "
                            + qcDisplayValue(entry.getValue()))
                    .collect(java.util.stream.Collectors.joining("; "));
            return displayed.isBlank() ? "—" : displayed;
        }
        if (!isQcScalarValue(value) && !converted.isEmpty()) {
            return qcDisplayValue(converted);
        }
        if (!isQcScalarValue(value)) return "—";
        return str(value);
    }

    private String qcPersonName(in.co.bel.acempp.dto.general.PersonInfo person) {
        if (person == null) return "—";
        if (person.getFullName() != null && !person.getFullName().isBlank()) {
            return person.getFullName();
        }
        return str(person.getId());
    }

    @SuppressWarnings("unchecked")
    private void renderLegacyQcDivisionDetails(Document doc, QcDivisionFormResponse data) throws DocumentException {
        if (data == null || data.getDivisionDetails() == null || data.getDivisionDetails().isEmpty()) {
            doc.add(new Paragraph("No structural records found for QC Division Quality Control Form.", BODY_FONT));
            return;
        }

        // ==========================================
        // 1. MASTER METADATA SUMMARY HEADER
        // ==========================================
        addSection(doc, "Quality Assurance Division Inspection Report", "");

        PdfPTable metaTable = createTable(new float[]{20, 30, 20, 30});
        addCell(metaTable, "Batch ID", BODY_BOLD, ROW_ALT);
        addCell(metaTable, str(data.getBatchId()), BODY_FONT, WHITE);
        addCell(metaTable, "Sub Department ID", BODY_BOLD, ROW_ALT);
        addCell(metaTable, str(data.getSubDepartmentId()), BODY_FONT, WHITE);
        addCell(metaTable, "Submission Type", BODY_BOLD, ROW_ALT);
        addCell(metaTable, workflowStatusLabel(data.getFormSubmissionType()), BODY_FONT, WHITE);
        addCell(metaTable, "Form Status", BODY_BOLD, ROW_ALT);
        addWorkflowStatusCell(metaTable, data.getStatus());
        addCell(metaTable, "Certified At", BODY_BOLD, ROW_ALT);
        addCell(metaTable, str(data.getSubmittedAt()), BODY_FONT, WHITE);
        addCell(metaTable, "—", BODY_BOLD, ROW_ALT);
        addCell(metaTable, "—", BODY_FONT, WHITE);
        doc.add(metaTable);
        spacer(doc, 10);

        // ==========================================
        // 2. PARSE AND ITERATE DIVISION LOOKUPS
        // ==========================================
        for (DivisionDetailDto detail : data.getDivisionDetails()) {
            String divisionName = detail.getDivision();
            Map<String, Object> dataMap = detail.getData() == null
                    ? Collections.emptyMap()
                    : mapper.convertValue(
                    detail.getData(),
                    new TypeReference<Map<String, Object>>() {
                    });
            List<Map<String, Object>> sections = qcDivisionSections(dataMap);
            if (sections.isEmpty()) continue;

            Paragraph divHeader = new Paragraph("QC Evaluation Stage: " + divisionName.replace("_", " "), SECTION_FONT);
            divHeader.setSpacingBefore(12);
            divHeader.setSpacingAfter(6);
            doc.add(divHeader);

            for (Map<String, Object> section : sections) {
                String sectionId = str(section.get("sectionId"));
                String motorId = section.containsKey("motorId") ? str(section.get("motorId")) : null;
                Integer premixNo = section.containsKey("premixNo") ? (Integer) section.get("premixNo") : null;

                // Sub-context header block inside divisions
                StringBuilder contextTitle = new StringBuilder(sectionId.replace("_", " "));
                if (motorId != null) contextTitle.append(" (Motor ID: ").append(motorId).append(")");
                if (premixNo != null) contextTitle.append(" (Premix No: ").append(premixNo).append(")");

                Paragraph secTitle = new Paragraph(contextTitle.toString(), BODY_BOLD);
                secTitle.setSpacingBefore(4);
                secTitle.setSpacingAfter(4);
                doc.add(secTitle);

                List<Map<String, Object>> sectionDataList = (List<Map<String, Object>>) section.get("sectionData");
                if (sectionDataList == null || sectionDataList.isEmpty()) continue;

                // Target individual schemas dynamically
                Map<String, Object> operationalPayload = sectionDataList.get(0);

                // SCHEMA 1: RAW_MATERIAL_DETAILS
                if (operationalPayload.containsKey("RAW_MATERIAL_DETAILS")) {
                    List<Map<String, Object>> rows = (List<Map<String, Object>>) operationalPayload.get("RAW_MATERIAL_DETAILS");
                    PdfPTable table = createTable(new float[]{8, 15, 20, 20, 22, 15});
                    addHeaders(table, "Sr.", "Ingredient", "Lot/Batch No", "Parameter Tested", "Specification Limits", "Result");
                    for (Map<String, Object> row : rows) {
                        addCell(table, str(row.get("SR_NO")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("INGREDIENT")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("LOT_BATCH_NUMBER")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("PARAMETER")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("SPECIFICATION")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("RESULT")), BODY_FONT, WHITE);
                    }
                    doc.add(table);
                }

                // SCHEMA 2: SOLID_PROCESSING_DETAILS
                else if (operationalPayload.containsKey("SOLID_PROCESSING_DETAILS")) {
                    List<Map<String, Object>> rows = (List<Map<String, Object>>) operationalPayload.get("SOLID_PROCESSING_DETAILS");
                    PdfPTable table = createTable(new float[]{8, 15, 15, 20, 22, 20});
                    addHeaders(table, "Sr.", "Material", "Grade", "Operation", "Parameter Type", "Actual Value");
                    for (Map<String, Object> row : rows) {
                        addCell(table, str(row.get("SR_NO")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("RAW_MATERIAL")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("GRADE")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("OPERATION")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("PARAMETER")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("ACTUAL_VALUE")), BODY_FONT, WHITE);
                    }
                    doc.add(table);
                }

                // SCHEMA 3: LIQUID_PROCESSING_DETAILS
                else if (operationalPayload.containsKey("LIQUID_PROCESSING_DETAILS")) {
                    List<Map<String, Object>> rows = (List<Map<String, Object>>) operationalPayload.get("LIQUID_PROCESSING_DETAILS");
                    PdfPTable table = createTable(new float[]{8, 17, 20, 25, 15, 15});
                    addHeaders(table, "Sr.", "Material", "Operation", "Parameter Tested", "Specification", "Actual");
                    for (Map<String, Object> row : rows) {
                        addCell(table, str(row.get("SR_NO")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("RAW_MATERIAL")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("OPERATION")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("PARAMETER")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("SPECIFICATION")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("ACTUAL_VALUE")), BODY_FONT, WHITE);
                    }
                    doc.add(table);
                }

                // SCHEMA 4: PREHEATING_DETAILS
                else if (operationalPayload.containsKey("PREHEATING_DETAILS")) {
                    List<Map<String, Object>> rows = (List<Map<String, Object>>) operationalPayload.get("PREHEATING_DETAILS");
                    PdfPTable table = createTable(new float[]{15, 15, 15, 15, 15, 25});
                    addHeaders(table, "Log Date", "Start", "End Time", "Temp (°C)", "Vac Level", "Observations");
                    for (Map<String, Object> row : rows) {
                        addCell(table, str(row.get("DATE")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("START_TIME")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("END_TIME")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("TEMPERATURE")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("VACUUM_LEVEL")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("OBSERVATIONS")), BODY_FONT, WHITE);
                    }
                    doc.add(table);
                }

                // SCHEMA 5: PREMIX_DETAILS
                else if (operationalPayload.containsKey("PREMIX_DETAILS")) {
                    List<Map<String, Object>> rows = (List<Map<String, Object>>) operationalPayload.get("PREMIX_DETAILS");
                    PdfPTable table = createTable(new float[]{12, 14, 14, 12, 20, 7, 7, 7, 7});
                    addHeaders(table, "Bowl", "Date", "Bldg", "Qty", "Parameter", "V1", "V2", "V3", "V4");
                    for (Map<String, Object> row : rows) {
                        addCell(table, str(row.get("BOWL_NO")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("DATE_OF_PREMIX")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("MIXER_BLDG_NO")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("PREMIX_QTY")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("PARAMETER")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("VALUE_1")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("VALUE_2")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("VALUE_3")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("VALUE_4")), BODY_FONT, WHITE);
                    }
                    doc.add(table);
                }

                // SCHEMA 6: LOOSE_FLAP_FILLING COMPONENT ARRAY UNROLLING
                else if (sectionId.equals("LOOSE_FLAP_FILLING")) {
                    // Compound Section Layout containing Bellow Removal, Qualification, and Filling matrices
                    if (operationalPayload.containsKey("BELLOW_REMOVAL_DETAILS")) {
                        Paragraph sub = new Paragraph("• Mandrel/Bellow Removal Details", BODY_BOLD);
                        sub.setSpacingAfter(2);
                        doc.add(sub);
                        List<Map<String, Object>> brRows = (List<Map<String, Object>>) operationalPayload.get("BELLOW_REMOVAL_DETAILS");
                        PdfPTable brTable = createTable(new float[]{20, 25, 25, 30});
                        addHeaders(brTable, "Location Side", "From Date", "To Date", "Observations");
                        for (Map<String, Object> r : brRows) {
                            addCell(brTable, str(r.get("LOCATION")), BODY_FONT, WHITE);
                            addCell(brTable, str(r.get("FROM_DATE")), BODY_FONT, WHITE);
                            addCell(brTable, str(r.get("TO_DATE")), BODY_FONT, WHITE);
                            addCell(brTable, str(r.get("OBSERVATIONS")), BODY_FONT, WHITE);
                        }
                        doc.add(brTable);
                        spacer(doc, 4);
                    }
                    if (operationalPayload.containsKey("LF_EPOXY_QUALIFICATION")) {
                        Paragraph sub = new Paragraph("• Epoxy qualification Metrics", BODY_BOLD);
                        sub.setSpacingAfter(2);
                        doc.add(sub);
                        List<Map<String, Object>> qRows = (List<Map<String, Object>>) operationalPayload.get("LF_EPOXY_QUALIFICATION");
                        PdfPTable qTable = createTable(new float[]{40, 30, 30});
                        addHeaders(qTable, "Qualification Parameter", "Specification Limit", "Test Result");
                        for (Map<String, Object> r : qRows) {
                            addCell(qTable, str(r.get("PARAMETER")), BODY_FONT, WHITE);
                            addCell(qTable, str(r.get("SPECIFICATION")), BODY_FONT, WHITE);
                            addCell(qTable, str(r.get("RESULT")), BODY_FONT, WHITE);
                        }
                        doc.add(qTable);
                        spacer(doc, 4);
                    }
                    if (operationalPayload.containsKey("LF_EPOXY_FILLING_DETAILS")) {
                        Paragraph sub = new Paragraph("• Epoxy Injection Log", BODY_BOLD);
                        sub.setSpacingAfter(2);
                        doc.add(sub);
                        List<Map<String, Object>> fRows = (List<Map<String, Object>>) operationalPayload.get("LF_EPOXY_FILLING_DETAILS");
                        PdfPTable fTable = createTable(new float[]{20, 20, 20, 15, 25});
                        addHeaders(fTable, "Location Side", "Start Date", "End Date", "Qty (kg)", "Observations");
                        for (Map<String, Object> r : fRows) {
                            addCell(fTable, str(r.get("LOCATION")), BODY_FONT, WHITE);
                            addCell(fTable, str(r.get("FROM_DATE")), BODY_FONT, WHITE);
                            addCell(fTable, str(r.get("TO_DATE")), BODY_FONT, WHITE);
                            addCell(fTable, str(r.get("QTY_FILLED")), BODY_FONT, WHITE);
                            addCell(fTable, str(r.get("OBSERVATIONS")), BODY_FONT, WHITE);
                        }
                        doc.add(fTable);
                    }
                }

                // SCHEMA 7: NDT STRUCTURAL PAYLOAD DISPATCH (RADIOGRAPHY & VISUALS)
                else if (sectionId.equals("RADIOGRAPHY_DETAILS")) {
                    List<Map<String, Object>> rows = (List<Map<String, Object>>) operationalPayload.get("RADIOGRAPHY_DETAILS");
                    PdfPTable table = createTable(new float[]{20, 16, 16, 12, 12, 12, 12});
                    addHeaders(table, "Machine No", "From Date", "To Date", "Sections", "Orients", "Norm Exp", "Tang Exp");
                    for (Map<String, Object> row : rows) {
                        addCell(table, str(row.get("MACHINE_NO")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("FROM_DATE")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("TO_DATE")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("NO_OF_SECTIONS")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("NO_OF_ORIENTATIONS")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("NORMAL_EXPOSURES")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("TANGENTIAL_EXPOSURES")), BODY_FONT, WHITE);
                    }
                    doc.add(table);
                } else if (sectionId.equals("RADIOGRAPHY_OBSERVATIONS")) {
                    List<Map<String, Object>> rows = (List<Map<String, Object>>) operationalPayload.get("RADIOGRAPHY_OBSERVATIONS");
                    PdfPTable table = createTable(new float[]{15, 50, 35});
                    addHeaders(table, "Sr No.", "Type of Discontinuity / Defect", "Observation Analysis Location");
                    for (Map<String, Object> row : rows) {
                        addCell(table, str(row.get("SR_NO")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("TYPE_OF_DEFECT")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("OBSERVATION_LOCATION")), BODY_FONT, WHITE);
                    }
                    doc.add(table);
                } else if (sectionId.equals("VISUAL_INSPECTION")) {
                    List<Map<String, Object>> rows = (List<Map<String, Object>>) operationalPayload.get("VISUAL_INSPECTION");
                    PdfPTable table = createTable(new float[]{45, 15, 40});
                    addHeaders(table, "Physical Examination Characteristic Type", "Status", "Inspected Structural Location");
                    for (Map<String, Object> row : rows) {
                        addCell(table, str(row.get("OBSERVATION_TYPE")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("OBSERVATION")), BODY_FONT, WHITE);
                        addCell(table, str(row.get("LOCATION")), BODY_FONT, WHITE);
                    }
                    doc.add(table);
                }
                spacer(doc, 6);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> qcDivisionSections(Map<String, Object> data) {
        if (data == null) return Collections.emptyList();
        List<Map<String, Object>> result = new ArrayList<>();
        Object directSections = data.get("sections");
        if (directSections instanceof Collection<?> sections) {
            sections.stream().filter(Map.class::isInstance)
                    .map(section -> new LinkedHashMap<>((Map<String, Object>) section))
                    .forEach(result::add);
        }
        appendQcUnitSections(result, data.get("motors"), "motorId");
        appendQcUnitSections(result, data.get("premixes"), "premixNo");
        return result;
    }

    @SuppressWarnings("unchecked")
    private void appendQcUnitSections(
            List<Map<String, Object>> target,
            Object unitsValue,
            String identityKey) {
        if (!(unitsValue instanceof Collection<?> units)) return;
        for (Object unitValue : units) {
            if (!(unitValue instanceof Map<?, ?> rawUnit)) continue;
            Map<String, Object> unit = (Map<String, Object>) rawUnit;
            Object sectionsValue = unit.get("sections");
            if (!(sectionsValue instanceof Collection<?> sections)) continue;
            for (Object sectionValue : sections) {
                if (!(sectionValue instanceof Map<?, ?> rawSection)) continue;
                Map<String, Object> section = new LinkedHashMap<>((Map<String, Object>) rawSection);
                section.put(identityKey, unit.get(identityKey));
                target.add(section);
            }
        }
    }

    public void renderStfDetails(Document doc, StfResponse data) throws DocumentException {
        if (data == null || data.getMotors() == null || data.getMotors().isEmpty()) {
            doc.add(new Paragraph("No structural evaluation records found for Static Testing (STF) Form.", BODY_FONT));
            return;
        }

        addSection(doc, "Batch information", "");
        PdfPTable metaTable = createTable(new float[]{25, 25, 25, 25});
        addInfoStripCell(metaTable, "Batch ID", str(data.getBatchId()));
        addInfoStripCell(metaTable, "Form ID", str(data.getFormId()));
        addInfoStripCell(metaTable, "Submission Type", workflowStatusLabel(data.getFormSubmissionType()));
        addInfoStripCell(metaTable, "Submitted By", qcPersonName(data.getSubmittedBy()));
        Paragraph formStatusParagraph = new Paragraph();
        formStatusParagraph.add(new Chunk("Form Status: ",
                new Font(Font.HELVETICA, 8, Font.BOLD, Color.BLACK)));
        formStatusParagraph.add(new Chunk(workflowStatusLabel(data.getFormStatus()),
                new Font(Font.HELVETICA, 8, Font.BOLD,
                        workflowStatusColor(data.getFormStatus()))));
        PdfPCell formStatusCell = new PdfPCell();
        formStatusCell.addElement(formStatusParagraph);
        formStatusCell.setColspan(4);
        formStatusCell.setPadding(6);
        formStatusCell.setBackgroundColor(ROW_ALT);
        formStatusCell.setBorderColor(BORDER_LIGHT);
        metaTable.addCell(formStatusCell);
        doc.add(metaTable);
        spacer(doc, 8);

        addSection(doc, "Static Testing Facility (STF) Evaluation Report", "");

        for (StaticMotorData motorOuter : data.getMotors()) {
            String motorId = motorOuter.getMotorId();
            MotorStatusResponse savedMotorStatus = findMotorStatus(data.getMotorStatuses(), motorId);
            addPlainMotorStatusHeader(
                    doc,
                    motorId,
                    savedMotorStatus != null
                            ? savedMotorStatus.getMotorSubmissionStatus()
                            : motorOuter.getMotorSubmissionStatus());

            PdfPTable identity = createTable(new float[]{25, 25, 25, 25});
            addCell(identity, "Motor Type", BODY_BOLD, ROW_ALT);
            addCell(identity, motorOuter.getSubType() != null
                    ? prettyText(motorOuter.getSubType().toString()) : "-", BODY_FONT, WHITE);
            addCell(identity, "STF Test No.", BODY_BOLD, ROW_ALT);
            addCell(identity, str(motorOuter.getStfTestNo()), BODY_FONT, WHITE);
            doc.add(identity);

            StaticTestingDetails staticTestingDetails = motorOuter.getStaticTestingDetails();
            if (staticTestingDetails == null) {
                doc.add(new Paragraph("No static testing details recorded for this motor.", BODY_FONT));
                continue;
            }

            if (staticTestingDetails instanceof MainMotorStaticTestingDetails mainDetails) {
                renderStfMainDetails(doc, mainDetails);
            } else if (staticTestingDetails instanceof BemStaticTestingDetails bemDetails) {
                renderStfBemDetails(doc, bemDetails);
            }
            spacer(doc, 8);
        }
    }

    public void renderBemMotorDetails(Document doc, BemMotorDetailsResponseDto data) throws DocumentException {
        if (data == null || data.getMotorId() == null) {
            doc.add(new Paragraph("No structural evaluation records found for Static Testing (STF) Form.", BODY_FONT));
            return;
        }

        addSection(doc, "Batch information", "");
        PdfPTable metaTable = createTable(new float[]{22, 28, 22, 28});

        addCell(metaTable, "Form ID", BODY_BOLD, ROW_ALT);
        addCell(metaTable, str(data.getFormId()), BODY_FONT, WHITE);
        addCell(metaTable, "Status", BODY_BOLD, ROW_ALT);
        addWorkflowStatusCell(metaTable, data.getStatus());
        addCell(metaTable, "Created by", BODY_BOLD, ROW_ALT);
        addCell(metaTable, str(data.getCreatedBy().getFullName()), BODY_FONT, WHITE);
        addCell(metaTable, "Created On", BODY_BOLD, ROW_ALT);
        addCell(metaTable, str(data.getCreatedAt()), BODY_FONT, WHITE);
        addCell(metaTable, "Submitted By", BODY_BOLD, ROW_ALT);
        addCell(metaTable, qcPersonName(data.getCreatedBy()), BODY_FONT, WHITE);
        addCell(metaTable, "Submitted On", BODY_BOLD, ROW_ALT);
        addCell(metaTable, str(data.getCreatedAt()), BODY_FONT, WHITE);
        addCell(metaTable, "Last Updated By", BODY_BOLD, ROW_ALT);
        addCell(metaTable, qcPersonName(data.getUpdatedBy()), BODY_FONT, WHITE);
        addCell(metaTable, "Last Updated On", BODY_BOLD, ROW_ALT);
        addCell(metaTable, str(data.getUpdatedAt()), BODY_FONT, WHITE);
        doc.add(metaTable);
        spacer(doc, 8);

        addSection(doc, "Static Testing Facility (STF) Evaluation Report",
                "Typed motor evaluation details");

        String motorId = data.getMotorId();
        addPlainMotorStatusHeader(
                doc,
                motorId,
                data.getStatus());

        PdfPTable identity = createTable(new float[]{25, 25, 25, 25});
        addCell(identity, "Motor Type", BODY_BOLD, ROW_ALT);
        addCell(identity, data.getSubType() != null
                ? prettyText(data.getSubType().toString()) : "-", BODY_FONT, WHITE);
        addCell(identity, "STF Test No.", BODY_BOLD, ROW_ALT);
        addCell(identity, str(data.getStfTestNo()), BODY_FONT, WHITE);
        doc.add(identity);

        StaticTestingDetails staticTestingDetails = data.getStaticTestingDetails();


        if (staticTestingDetails instanceof MainMotorStaticTestingDetails mainDetails) {
            renderStfMainDetails(doc, mainDetails);
            spacer(doc, 8);
        } else if (staticTestingDetails instanceof BemStaticTestingDetails bemDetails) {
            renderStfBemDetails(doc, bemDetails);
            spacer(doc, 8);
        }
    }

    private void renderStfMainDetails(Document doc, MainMotorStaticTestingDetails details) throws DocumentException {
        if (details == null) return;
        renderStfMainTestHeader(doc, details.getTestHeader());
        renderStfMainNozzle(doc, details.getNozzleDetails());
        renderStfMainMotorData(doc, details.getMainMotorData());
        renderStfMainBallisticParameters(doc, details.getBallisticParameters());
        renderStfMainBurnRate(doc, details.getBurnRate());
    }

    private void renderStfMainTestHeader(Document doc, MainTestHeader details) throws DocumentException {
        if (details == null) return;
        // Match UI: header fields without a "Test Header" section title.
        PdfPTable table = createTable(new float[]{50, 50});
        addHeaders(table, "Firing Date & Time", "Amb. Temp.");
        addCell(table, str(details.getFiringDateTime()), BODY_FONT, WHITE);
        addCell(table, str(details.getAmbientTemperature()), BODY_FONT, WHITE);
        doc.add(table);
        addStfQaRemarksStrip(doc, details.getQaRemarks());
    }

    private void renderStfMainNozzle(Document doc, MainMotorNozzleDetails details) throws DocumentException {
        if (details == null) return;
        addCcTitle(doc, "Nozzle");
        PdfPTable table = createTable(new float[]{15, 15, 15, 15, 40});
        addHeaders(table, "Before Dt", "Before De", "After Dt", "After De", "Nozzle Erosion Rate (mm/s)");
        addCell(table, str(details.getBeforeDt()), BODY_FONT, WHITE);
        addCell(table, str(details.getBeforeDe()), BODY_FONT, WHITE);
        addCell(table, str(details.getAfterDt()), BODY_FONT, WHITE);
        addCell(table, str(details.getAfterDe()), BODY_FONT, WHITE);
        addCell(table, str(details.getNozzleErosionRateMmS()), BODY_FONT, WHITE);
        doc.add(table);
        addStfQaRemarksStrip(doc, details.getQaRemarks());
    }

    private void renderStfMainMotorData(Document doc, MainMotorData details) throws DocumentException {
        if (details == null) return;
        addCcTitle(doc, "Main Motor Data");
        PdfPTable table = createTable(new float[]{20, 22, 14, 22, 22});
        addHeaders(table, "Wt of Propellant", "Avg. BEM Burn Rate", "n value", "Propellant Density", "Gamma");
        addCell(table, str(details.getPropellantWeightKg()), BODY_FONT, WHITE);
        addCell(table, str(details.getAvgBemBurnRateMmS()), BODY_FONT, WHITE);
        addCell(table, str(details.getNValue()), BODY_FONT, WHITE);
        addCell(table, str(details.getPropellantDensity()), BODY_FONT, WHITE);
        addCell(table, str(details.getGamma()), BODY_FONT, WHITE);
        doc.add(table);
        addStfQaRemarksStrip(doc, details.getQaRemarks());
    }

    private void renderStfMainBallisticParameters(Document doc, List<MainBallisticParameter> rows)
            throws DocumentException {
        if (rows == null || rows.isEmpty()) return;
        addCcTitle(doc, "Ballistic Parameter");
        boolean showQa = rows.stream().anyMatch(r -> r != null && !isBlank(r.getQaRemarks()));
        PdfPTable table = createTable(showQa ? new float[]{46, 27, 27} : new float[]{60, 40});
        if (showQa) {
            addHeaders(table, "Parameter", "Channel 1", "QA Remarks");
        } else {
            addHeaders(table, "Parameter", "Channel 1");
        }
        for (MainBallisticParameter row : rows) {
            if (row == null) continue;
            addCell(table, stfBallisticParameterLabel(row.getParameter()), BODY_FONT, WHITE);
            addCell(table, str(row.getChannel1()), BODY_FONT, WHITE);
            if (showQa) {
                addCell(table, str(row.getQaRemarks()), BODY_FONT, WHITE);
            }
        }
        doc.add(table);
    }

    private void renderStfMainBurnRate(Document doc, MainBurnRate details) throws DocumentException {
        if (details == null) return;
        addCcTitle(doc, "Burn Rate");
        PdfPTable table = createTable(new float[]{34, 33, 33});
        addHeaders(table, "Burn Rate (mm/s)", "Pressure Abs (ksc)", "n value");
        addCell(table, str(details.getBurnRateMmS()), BODY_FONT, WHITE);
        addCell(table, str(details.getPressureAbsKsc()), BODY_FONT, WHITE);
        addCell(table, str(details.getNValue()), BODY_FONT, WHITE);
        doc.add(table);
        addStfQaRemarksStrip(doc, details.getQaRemarks());
    }

    private void renderStfBemDetails(Document doc, BemStaticTestingDetails details) throws DocumentException {
        if (details == null) return;
        renderStfBemTestHeader(doc, details.getTestHeader());
        renderStfBemIgniter(doc, details.getIgniterDetails());
        renderStfBemNozzle(doc, details.getNozzleDetails());
        renderStfBemData(doc, details.getBemData());
        renderStfBemBallisticParameters(doc, details.getBallisticParameters());
        renderStfBemBurnRate(doc, details.getBurnRate());
    }

    private void renderStfBemTestHeader(Document doc, BemTestHeader details) throws DocumentException {
        if (details == null) return;
        // Match UI: header fields without a "Test Header" section title.
        PdfPTable table = createTable(new float[]{14, 14, 14, 12, 10, 14, 22});
        addHeaders(table, "Loading No.", "Firing No.", "Firing Date & Time", "Temp", "RH", "Wt. of Grain", "Grain Config");
        addCell(table, str(details.getLoadingNo()), BODY_FONT, WHITE);
        addCell(table, str(details.getFiringNo()), BODY_FONT, WHITE);
        addCell(table, str(details.getFiringDateTime()), BODY_FONT, WHITE);
        addCell(table, str(details.getTemperature()), BODY_FONT, WHITE);
        addCell(table, str(details.getRh()), BODY_FONT, WHITE);
        addCell(table, str(details.getWtOfGrain()), BODY_FONT, WHITE);
        addCell(table, str(details.getGrainConfiguration()), BODY_FONT, WHITE);
        doc.add(table);
        addStfQaRemarksStrip(doc, details.getQaRemarks());
    }

    private void renderStfBemIgniter(Document doc, BemIgniterDetails details) throws DocumentException {
        if (details == null) return;
        addCcTitle(doc, "Igniter");
        PdfPTable table = createTable(new float[]{20, 30, 20, 30});
        addHeaders(table, "Type", "Sr No. & Lot No.", "Charge Mass (gm)", "Composition");
        addCell(table, str(details.getType()), BODY_FONT, WHITE);
        addCell(table, str(details.getSrNoAndLotNo()), BODY_FONT, WHITE);
        addCell(table, str(details.getChargeMassGm()), BODY_FONT, WHITE);
        addCell(table, str(details.getComposition()), BODY_FONT, WHITE);
        doc.add(table);
        addStfQaRemarksStrip(doc, details.getQaRemarks());
    }

    private void renderStfBemNozzle(Document doc, BemNozzleDetails details) throws DocumentException {
        if (details == null) return;
        addCcTitle(doc, "Nozzle");
        PdfPTable table = createTable(new float[]{15, 15, 15, 15, 40});
        addHeaders(table, "Before Dt", "Before De", "After Dt", "After De", "Nozzle Closure Thickness (mm)");
        addCell(table, str(details.getBeforeDt()), BODY_FONT, WHITE);
        addCell(table, str(details.getBeforeDe()), BODY_FONT, WHITE);
        addCell(table, str(details.getAfterDt()), BODY_FONT, WHITE);
        addCell(table, str(details.getAfterDe()), BODY_FONT, WHITE);
        addCell(table, str(details.getNozzleClosureThicknessMm()), BODY_FONT, WHITE);
        doc.add(table);
        addStfQaRemarksStrip(doc, details.getQaRemarks());
    }

    private void renderStfBemData(Document doc, BemData details) throws DocumentException {
        if (details == null) return;
        addCcTitle(doc, "BEM Data");
        PdfPTable table = createTable(new float[]{18, 20, 14, 24, 24});
        addHeaders(table, "Wt of Propellant", "Cond. Temp", "n value", "Web Thick Avg/Min", "Web Thick Std Dev");
        addCell(table, str(details.getPropellantWeightKg()), BODY_FONT, WHITE);
        addCell(table, str(details.getConditioningTemperature()), BODY_FONT, WHITE);
        addCell(table, str(details.getNValue()), BODY_FONT, WHITE);
        addCell(table, str(details.getWebThicknessAverageMin()), BODY_FONT, WHITE);
        addCell(table, str(details.getWebThicknessStdDev()), BODY_FONT, WHITE);
        doc.add(table);
        addStfQaRemarksStrip(doc, details.getQaRemarks());
    }

    private void renderStfBemBallisticParameters(Document doc, List<BemBallisticParameter> rows)
            throws DocumentException {
        if (rows == null || rows.isEmpty()) return;
        addCcTitle(doc, "Ballistic Parameter");
        boolean showQa = rows.stream().anyMatch(r -> r != null && !isBlank(r.getQaRemarks()));
        PdfPTable table = createTable(showQa ? new float[]{34, 22, 22, 22} : new float[]{40, 30, 30});
        if (showQa) {
            addHeaders(table, "Parameter", "Channel 1", "Channel 2", "QA Remarks");
        } else {
            addHeaders(table, "Parameter", "Channel 1", "Channel 2");
        }
        for (BemBallisticParameter row : rows) {
            if (row == null) continue;
            addCell(table, stfBallisticParameterLabel(row.getParameter()), BODY_FONT, WHITE);
            addCell(table, str(row.getChannel1()), BODY_FONT, WHITE);
            addCell(table, str(row.getChannel2()), BODY_FONT, WHITE);
            if (showQa) {
                addCell(table, str(row.getQaRemarks()), BODY_FONT, WHITE);
            }
        }
        doc.add(table);
    }

    private void renderStfBemBurnRate(Document doc, BemBurnRate details) throws DocumentException {
        if (details == null) return;
        addCcTitle(doc, "Burn Rate");
        PdfPTable table = createTable(new float[]{50, 50});
        addHeaders(table, "Burn Rate (mm/s)", "Pressure Abs (ksc)");
        addCell(table, str(details.getBurnRateMmS()), BODY_FONT, WHITE);
        addCell(table, str(details.getPressureAbsKsc()), BODY_FONT, WHITE);
        doc.add(table);
        addStfQaRemarksStrip(doc, details.getQaRemarks());
    }

    /**
     * Sheet labels for MAIN/BEM ballistic parameter keys (matches FE presets).
     */
    private String stfBallisticParameterLabel(String parameter) {
        if (isBlank(parameter)) return "-";
        String key = parameter.trim();
        return switch (key) {
            case "CHAMBER_PRESSURE_AVG" -> "Chamber Pressure (Avg)";
            case "CHAMBER_PRESSURE_MAX" -> "Chamber Pressure (Max)";
            case "AVERAGE_THRUST_KGF" -> "Average Thrust (kgf)";
            case "MAX_THRUST_KGF" -> "Max Thrust (kgf)";
            case "IGNITION_DELAY_MSEC" -> "Ignition delay (msec)";
            case "ACTION_TIME_SEC" -> "Action Time (sec)";
            case "TOTAL_IMPULSE_TON_SEC" -> "Total Impulse (Ton. Sec)";
            case "CSTAR_CORRECTED" -> "Cstar (corrected)";
            case "ISP_DELIVERED_SEC" -> "Isp Delivered (sec)";
            case "ISP_70_1_033" -> "Isp (70:1.033)";
            case "PRESSURE_THRUST" -> "Pressure / Thrust";
            case "SENSOR_SENSITIVITY" -> "Sensor sensitivity";
            case "IGNITION_DELAY" -> "Ignition delay";
            case "ACTION_TIME" -> "Action time";
            case "BURN_TIME" -> "Burn time";
            case "AVERAGE_IN_BURN_TIME" -> "Average in Burn time";
            case "AVERAGE_IN_ACTION_TIME" -> "Average in Action time";
            case "AREA_IN_BURN_TIME" -> "Area in Burn Time";
            case "AREA_IN_ACTION_TIME" -> "Area in Action time";
            case "CSTAR_ISP" -> "Cstar/Isp";
            default -> key;
        };
    }

    private void addStfQaRemarksStrip(Document doc, String qaRemarks) throws DocumentException {
        if (isBlank(qaRemarks)) return;
        PdfPTable table = createTable(new float[]{100});
        addHeaders(table, "QA Remarks");
        addCell(table, str(qaRemarks), BODY_FONT, WHITE);
        doc.add(table);
    }

    public void renderDispatchDetails(Document doc, DispatchDetailsResponse data) throws DocumentException {
        if (data == null || data.getMotors() == null || data.getMotors().isEmpty()) {
            doc.add(new Paragraph("No dispatch details available.", BODY_FONT));
            return;
        }

        // ------------------------------------------------------------------
        // 1. FORM SUMMARY
        // ------------------------------------------------------------------
        addSection(doc, "Dispatch", "");

        PdfPTable summary = createTable(new float[]{25, 25, 25, 25});
        addInfoStripCell(summary, "Batch ID", str(data.getBatchId()));
        addInfoStripCell(summary, "Batch Type", str(data.getBatchType()));
        addInfoStripCell(summary, "Created By", str(data.getCreatedBy()));
        addInfoStripCell(summary, "Submitted By", qcPersonName(data.getSubmittedBy()));

        // Status
        PdfPCell statusCell = new PdfPCell();
        statusCell.setColspan(4);
        statusCell.setBorderColor(BORDER_LIGHT);
        statusCell.setPadding(6);
        statusCell.setBackgroundColor(ROW_ALT);
        Paragraph sp = new Paragraph();
        sp.add(new Chunk("Form Status: ", BODY_BOLD));
        sp.add(createStatusPhrase(
                data.getFormStatus() != null ? data.getFormStatus().toString() : "-"));
        statusCell.addElement(sp);
        summary.addCell(statusCell);

        doc.add(summary);
        spacer(doc, 12);

        // ------------------------------------------------------------------
        // 2. MOTORS
        // ------------------------------------------------------------------
        for (DispatchMotor motor : data.getMotors()) {
            String motorId = motor.getMotorId();
            String approvalStatus = dispatchMotorStatus(data, motor, motorId);

            // Motor header + coloured status
            Paragraph motorTitle = new Paragraph();
            motorTitle.add(new Chunk("Motor · " + str(motorId) + "   ", SECTION_FONT));
            motorTitle.add(createStatusPhrase(approvalStatus));
            motorTitle.setSpacingBefore(10);
            motorTitle.setSpacingAfter(6);
            doc.add(motorTitle);

            var dispatch = motor.getDispatchDetails();
            if (dispatch == null) {
                doc.add(new Paragraph("No dispatch details recorded for this motor.", BODY_FONT));
                spacer(doc, 10);
                continue;
            }

            var ndtClearance = dispatch.getNdtClearance();
            var finalAcceptance = dispatch.getFinalAcceptanceCommitteeClearance();
            var safetyClearance = dispatch.getSafetyClearance();

            // ---- Dispatch Setup ----
            addSection(doc, "Dispatch Setup", "");
            PdfPTable setup = createKeyValueTable();
            addKV(setup, "Stage", dispatchDisplay(dispatch.getStage()));
            addKV(setup, "Date & Time of Casting", str(dispatch.getCastingDate()));
            addKV(setup, "Date & Time of Dispatch", str(dispatch.getDispatchDate()));
            addKV(setup, "Dispatch Location", str(dispatch.getDispatchLocation()));
            addKV(setup, "NDT Clearance Accorded",
                    dispatchYesNo(ndtClearance != null ? ndtClearance.getAccorded() : null));
            addKV(setup, "NDT MOM No.",
                    str(ndtClearance != null ? ndtClearance.getMomNo() : null));
            addKV(setup, "Final Acceptance Committee Clearance",
                    dispatchYesNo(finalAcceptance != null ? finalAcceptance.getAccorded() : null));
            addKV(setup, "Final Acceptance MOM No.",
                    str(finalAcceptance != null ? finalAcceptance.getMomNo() : null));
            if (safetyClearance != null) {
                addKV(setup, "Safety Clearance Accorded",
                        dispatchYesNo(safetyClearance.getAccorded()));
                addKV(setup, "Safety Clearance Certificate",
                        documentReference(safetyClearance.getClearanceCertificate()));
            }
            doc.add(setup);
            spacer(doc, 8);

            // ---- Propellant Properties ----
            List<PropellantProperty> properties = dispatch.getPropellantProperties();
            if (properties != null && !properties.isEmpty()) {
                addSection(doc, "Propellant Properties", "");

                List<Integer> finalMixNumbers = properties.stream()
                        .filter(Objects::nonNull)
                        .map(PropellantProperty::getFinalMixResults)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .filter(Objects::nonNull)
                        .map(FinalMixResult::getFinalMixNo)
                        .filter(Objects::nonNull)
                        .distinct()
                        .sorted()
                        .toList();

                float[] widths = new float[3 + finalMixNumbers.size()];
                widths[0] = 8;
                widths[1] = 32;
                widths[2] = 22;
                float mixWidth = 38f / Math.max(1, finalMixNumbers.size());
                for (int i = 3; i < widths.length; i++) {
                    widths[i] = mixWidth;
                }

                PdfPTable table = createTable(widths);
                List<String> headers = new ArrayList<>(List.of("SR.", "PROPERTY", "SPECIFICATION"));
                for (Integer fm : finalMixNumbers) {
                    headers.add("FM " + fm);
                }
                addHeaders(table, headers.toArray(String[]::new));

                boolean alt = false;
                int rowNo = 1;
                for (PropellantProperty property : properties) {
                    Color bg = alt ? ROW_ALT : WHITE;
                    addCell(table,
                            str(property.getSrNo() != null ? property.getSrNo() : rowNo),
                            BODY_FONT, bg);
                    rowNo++;
                    addCell(table, dispatchDisplay(property.getProperty()), BODY_FONT, bg);
                    addCell(table, str(property.getSpecification()), BODY_FONT, bg);

                    for (Integer finalMixNo : finalMixNumbers) {
                        String value = property.getFinalMixResults() == null ? null
                                : property.getFinalMixResults().stream()
                                .filter(Objects::nonNull)
                                .filter(r -> finalMixNo.equals(r.getFinalMixNo()))
                                .map(FinalMixResult::getValue)
                                .findFirst()
                                .orElse(null);
                        addCell(table, str(value), BODY_FONT, bg);
                    }
                    alt = !alt;
                }
                doc.add(table);
                spacer(doc, 8);
            }

            // ---- Waiver Details ----
            var waiver = dispatch.getWaiverDetails();
            if (waiver != null) {
                addSection(doc, "Waiver Details", "");
                PdfPTable table = createKeyValueTable();
                addKV(table, "Waiver Available", dispatchYesNo(waiver.getAvailable()));
                if (hasQcDisplayValue(waiver.getDetails())) {
                    addKV(table, "Waiver Details", waiver.getDetails());
                }
                if (waiver.getUploadedDocuments() != null && !waiver.getUploadedDocuments().isEmpty()) {
                    addKV(table, "Uploaded Documents",
                            String.join(", ", waiver.getUploadedDocuments()));
                }
                doc.add(table);
                spacer(doc, 8);
            }

            // ---- Rocket Motor Inspection ----
            List<InspectionRow> inspections = dispatch.getRocketMotorInspection();
            if (inspections != null && !inspections.isEmpty()) {
                addSection(doc, "Rocket Motor Inspection", "");
                PdfPTable table = createTable(new float[]{10, 55, 35});
                addHeaders(table, "SR.", "PARAMETER", "OBSERVATION");

                boolean alt = false;
                int rowNo = 1;
                for (InspectionRow inspection : inspections) {
                    Color bg = alt ? ROW_ALT : WHITE;
                    addCell(table,
                            str(inspection.getSrNo() != null ? inspection.getSrNo() : rowNo),
                            BODY_FONT, bg);
                    rowNo++;
                    addCell(table, dispatchDisplay(inspection.getParameter()), BODY_FONT, bg);
                    addCell(table, str(inspection.getObservation()), BODY_FONT, bg);
                    alt = !alt;
                }
                doc.add(table);
                spacer(doc, 8);
            }

            // ---- Vehicle Details ----
            List<VehicleRow> vehicles = dispatch.getVehicleDetails();
            if (vehicles != null && !vehicles.isEmpty()) {
                addSection(doc, "Vehicle Details", "");
                PdfPTable table = createTable(new float[]{10, 55, 35});
                addHeaders(table, "SR.", "CHECK POINT", "REMARKS");

                boolean alt = false;
                int rowNo = 1;
                for (VehicleRow vehicle : vehicles) {
                    Color bg = alt ? ROW_ALT : WHITE;
                    addCell(table,
                            str(vehicle.getSrNo() != null ? vehicle.getSrNo() : rowNo),
                            BODY_FONT, bg);
                    rowNo++;
                    addCell(table, dispatchDisplay(vehicle.getCheckPoint()), BODY_FONT, bg);
                    addCell(table, str(vehicle.getObservation()), BODY_FONT, bg);
                    alt = !alt;
                }
                doc.add(table);
                spacer(doc, 8);
            }

            // ---- Rocket Motor Packing ----
            List<PackingRow> packing = dispatch.getRocketMotorPackingDetails();
            if (packing != null && !packing.isEmpty()) {
                addSection(doc, "Rocket Motor Packing", "");
                PdfPTable table = createTable(new float[]{10, 55, 35});
                addHeaders(table, "SR.", "NOMENCLATURE", "REMARKS");

                boolean alt = false;
                int rowNo = 1;
                for (PackingRow item : packing) {
                    Color bg = alt ? ROW_ALT : WHITE;
                    addCell(table,
                            str(item.getSrNo() != null ? item.getSrNo() : rowNo),
                            BODY_FONT, bg);
                    rowNo++;
                    addCell(table, dispatchDisplay(item.getNomenclature()), BODY_FONT, bg);
                    addCell(table, str(item.getObservation()), BODY_FONT, bg);
                    alt = !alt;
                }
                doc.add(table);
                spacer(doc, 8);
            }

            // ---- Dispatch Team ----
            var team = dispatch.getDispatchTeam();
            if (team != null) {
                addSection(doc, "Dispatch Team", "");
                PdfPTable table = createTable(new float[]{33, 33, 34});
                addHeaders(table, "QA REPRESENTATIVE", "SAFETY REPRESENTATIVE", "PROJECT REPRESENTATIVE");
                addCell(table, str(team.getQaRepresentative()), BODY_FONT, WHITE);
                addCell(table, str(team.getSafetyRepresentative()), BODY_FONT, WHITE);
                addCell(table, str(team.getProjectRepresentative()), BODY_FONT, WHITE);
                doc.add(table);
                spacer(doc, 8);
            }

            // ---- Dispatch Photos ----
            if (dispatch.getUploadDispatchPhotos() != null
                    && !dispatch.getUploadDispatchPhotos().isEmpty()) {
                addSection(doc, "Dispatch Photos", "");
                String fileNames = dispatch.getUploadDispatchPhotos().stream()
                        .map(DocumentDto::getFileName)
                        .filter(name -> name != null && !name.isBlank())
                        .collect(Collectors.joining(", "));
                if (!fileNames.isEmpty()) {
                    PdfPTable photoTable = createKeyValueTable();
                    addKV(photoTable, "Files", fileNames);
                    doc.add(photoTable);
                }
            }

            spacer(doc, 12);
        }
    }

    private String dispatchMotorStatus(
            DispatchDetailsResponse data,
            DispatchMotor motor,
            String motorId) {
        if (motor.getMotorSubmissionStatus() != null) {
            return workflowStatusLabel(motor.getMotorSubmissionStatus());
        }
        if (data.getMotorStatuses() != null) {
            for (MotorStatusResponse status : data.getMotorStatuses()) {
                if (motorId.equals(str(status.getMotorId()))) {
                    return workflowStatusLabel(status.getMotorSubmissionStatus());
                }
            }
        }
        return "—";
    }

    private void addDispatchSetupRow(PdfPTable table, String parameter, String value) {
        addCell(table, parameter, BODY_BOLD, ROW_ALT);
        addCell(table, value, BODY_FONT, WHITE);
    }

    private String dispatchYesNo(Object value) {
        if (value == null) return "—";
        if (value instanceof Boolean bool) return bool ? "YES" : "NO";
        String text = value.toString().trim();
        if ("true".equalsIgnoreCase(text) || "yes".equalsIgnoreCase(text)) return "YES";
        if ("false".equalsIgnoreCase(text) || "no".equalsIgnoreCase(text)) return "NO";
        return text.isEmpty() ? "—" : text.toUpperCase(java.util.Locale.ROOT);
    }

    private String dispatchDisplay(Object value) {
        String text = qcDisplayValue(value);
        if ("—".equals(text)) return text;
        return text.matches("[A-Z0-9_]+") ? prettyText(text) : text;
    }

    private void renderDynamicSectionTable(
            Document doc,
            List<Map<String, Object>> rows,
            String contextKey) throws DocumentException {
        if (rows == null || rows.isEmpty()) return;

        java.util.LinkedHashSet<String> allKeys = new java.util.LinkedHashSet<>();
        rows.stream()
                .filter(Objects::nonNull)
                .forEach(row -> allKeys.addAll(row.keySet()));
        if (allKeys.isEmpty()) return;

        List<String> scalarKeys = allKeys.stream()
                .filter(key -> rows.stream()
                        .map(row -> row != null ? row.get(key) : null)
                        .filter(Objects::nonNull)
                        .allMatch(value -> isQcScalarValue(value)
                                || isQcScalarCollection(value)))
                .toList();

        if (!scalarKeys.isEmpty()) {
            PdfPTable table = new PdfPTable(scalarKeys.size());
            table.setWidthPercentage(100);
            table.setSpacingBefore(2);
            addHeaders(table, scalarKeys.stream()
                    .map(this::prettyText)
                    .toArray(String[]::new));

            boolean alternate = false;
            for (Map<String, Object> row : rows) {
                Color background = alternate ? ROW_ALT : WHITE;
                for (String key : scalarKeys) {
                    Object value = row != null ? row.get(key) : null;
                    addCell(table, qcDisplayValue(value), BODY_FONT, background);
                }
                alternate = !alternate;
            }
            doc.add(table);
        }

        for (String nestedKey : allKeys) {
            if (scalarKeys.contains(nestedKey)) continue;

            List<Map<String, Object>> nestedRows = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                if (row == null) continue;
                collectRawMaterialRows(row.get(nestedKey), nestedRows);
            }
            if (nestedRows.isEmpty()) continue;

            if (!normalizePdfKey(nestedKey).equals(normalizePdfKey(contextKey))) {
                addCcTitle(doc, prettyText(nestedKey));
            }
            renderDynamicSectionTable(doc, nestedRows, nestedKey);
        }
    }

    private void collectRawMaterialRows(
            Object value,
            List<Map<String, Object>> target) {
        if (value instanceof Map<?, ?> map) {
            target.add(stringKeyMap(map));
            return;
        }
        if (!(value instanceof Collection<?> collection)) return;
        for (Object item : collection) {
            if (item instanceof Map<?, ?> map) {
                target.add(stringKeyMap(map));
            } else if (item != null && !isQcScalarValue(item)) {
                Map<String, Object> converted = qcObjectMap(item);
                if (!converted.isEmpty()) target.add(converted);
            }
        }
    }

    private String prettyText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        // Replace underscores with spaces
        text = text.replace("_", " ");
        // Split camelCase
        text = text.replaceAll("([a-z])([A-Z])", "$1 $2");
        StringBuilder sb = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (word.isBlank()) {
                continue;
            }
            word = word.toLowerCase();
            sb.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1))
                    .append(" ");
        }
        return sb.toString().trim();
    }

    // Helper to format a ThermalPropertyValue
    private String formatThermalValue(PropertyValue tv) {
        if (tv == null || tv.getReported() == null) return "N/A";
        String s = tv.getReported() + " " + (tv.getUnit() != null ? tv.getUnit() : "");
        if (tv.getAcemSpec() != null) s += " (ACEM: " + tv.getAcemSpec() + ")";
        return s;
    }

    // Overloaded formatMeasurement for old Measurement (still used by weightment)
    private String formatMeasurement(MeasurementValue m) {
        if (m == null || m.getValue() == null) return "N/A";
        return m.getValue() + " " + (m.getUnit() != null ? m.getUnit() : "");
    }

    private String formatMeasurement(MockTrialMeasurementValue m) {
        if (m == null || m.getValue() == null) return "-";
        return m.getValue() + " " + (m.getUnit() != null ? m.getUnit() : "");
    }


    // ═════════════════════════════════════════════════════════════
    // COMMON PDF BUILDING HELPERS
    // ═════════════════════════════════════════════════════════════


    private String formatRange(ReferenceRange range) {
        if (range == null) return "N/A";
        if (range.getMinValue() == null && range.getMaxValue() == null) return "N/A";
        String unit = range.getUnit() != null && !range.getUnit().isBlank()
                ? " " + range.getUnit().trim()
                : "";
        if (range.getMinValue() == null) return "≤ " + range.getMaxValue() + unit;
        if (range.getMaxValue() == null) return "≥ " + range.getMinValue() + unit;
        return range.getMinValue() + " – " + range.getMaxValue() + unit;
    }

    private String formatInsulationRange(ReferenceRange range) {
        if (range == null) return "N/A";
        Double min = range.getMinValue();
        Double max = range.getMaxValue();
        if (min == null && max == null) return "N/A";
        if (min == null) return "≤ " + formatPdfNumber(max);
        if (max == null) return "≥ " + formatPdfNumber(min);
        return formatPdfNumber(min) + " - " + formatPdfNumber(max);
    }


    /**
     * Main title block with divider line
     */
    private void addDocHeader(Document doc, String title, String subtitle, String approver)
            throws DocumentException {
        Paragraph t = new Paragraph(title.toUpperCase() + " REPORT", TITLE_FONT);
        t.setAlignment(Element.ALIGN_CENTER);
        doc.add(t);

        Paragraph s = new Paragraph(subtitle + "  ·  Approved by: " + approver, SUBTITLE_FONT);
        s.setAlignment(Element.ALIGN_CENTER);
        s.setSpacingAfter(6);
        doc.add(s);

        LineSeparator ls = new LineSeparator(1.5f, 100, BRAND_BLUE, Element.ALIGN_CENTER, -2);
        doc.add(new Chunk(ls));
        spacer(doc, 8);
    }

    /**
     * Common form-level summary strip used by every sub-department.
     * Fields that are not available for a form will show "—".
     */
    private void addCommonFormSummary(
            Document doc,
            String batchId,
            String batchType,
            String submittedBy,
            LocalDateTime submittedAt,
            String status) throws DocumentException {

        PdfPTable summary = createTable(new float[]{25, 25, 25, 25});

        addInfoStripCell(summary, "Batch ID", str(batchId));
        addInfoStripCell(summary, "Batch Type", str(batchType));
        addInfoStripCell(summary, "Submitted By", str(submittedBy));
        addInfoStripCell(summary, "Submitted On", str(submittedAt));

        // Status row (full width)
        PdfPCell statusCell = new PdfPCell();
        statusCell.setColspan(4);
        statusCell.setBorderColor(BORDER_LIGHT);
        statusCell.setPadding(6);
        statusCell.setBackgroundColor(ROW_ALT);

        Paragraph sp = new Paragraph();
        sp.add(new Chunk("Form Status: ", BODY_BOLD));
        sp.add(createStatusPhrase(status != null ? status : "-"));
        statusCell.addElement(sp);

        summary.addCell(statusCell);
        doc.add(summary);
        spacer(doc, 10);
    }

    /**
     * Blue bar section heading
     */
    private void addSection(Document doc, String title, String sub) throws DocumentException {
        Paragraph p = new Paragraph();
        p.add(new Chunk("| ", new Font(Font.HELVETICA, 11, Font.BOLD, BRAND_BLUE)));
        p.add(new Chunk(title, SECTION_FONT));
        if (sub != null && !sub.isBlank()) {
            p.add(new Chunk("   |   " + sub, new Font(Font.HELVETICA, 9, Font.ITALIC, TEXT_GRAY)));
        }
        p.setSpacingBefore(12);
        p.setSpacingAfter(5);
        doc.add(p);
    }

    /**
     * Consistent identity banner for every motor-level workflow section.
     */
    private void addMotorStatusHeader(Document doc, String motorId, Object status)
            throws DocumentException {
        addWorkflowUnitHeader(doc, "Motor · " + str(motorId), status);
    }

    private void addPlainMotorStatusHeader(Document doc, String motorId, Object status)
            throws DocumentException {
        Paragraph motorTitle = new Paragraph();
        motorTitle.add(new Chunk("Motor · " + str(motorId) + "   ", SECTION_FONT));
        motorTitle.add(createStatusPhrase(workflowStatusLabel(status)));
        motorTitle.setSpacingBefore(10);
        motorTitle.setSpacingAfter(6);
        doc.add(motorTitle);
    }

    /**
     * Renders a workflow unit name and its status as one banner. Statuses must not be
     * passed through {@link #addSection(Document, String, String)}, whose subtitle is
     * intentionally gray and italic for descriptive context only.
     */
    private void addWorkflowUnitHeader(Document doc, String unitLabel, Object status)
            throws DocumentException {
        PdfPTable banner = createTable(new float[]{76, 24});
        banner.setSpacingBefore(12);
        banner.setSpacingAfter(5);
        banner.setKeepTogether(true);

        PdfPCell identityCell = new PdfPCell(new Phrase(
                str(unitLabel),
                new Font(Font.HELVETICA, 10, Font.BOLD, WHITE)));
        identityCell.setBackgroundColor(BRAND_BLUE);
        identityCell.setBorderColor(BRAND_BLUE);
        identityCell.setPadding(7);
        identityCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        banner.addCell(identityCell);

        Color statusColor = workflowStatusColor(status);
        PdfPCell statusCell = new PdfPCell(new Phrase(
                workflowStatusLabel(status),
                new Font(Font.HELVETICA, 8, Font.BOLD, statusColor)));
        statusCell.setBackgroundColor(WHITE);
        statusCell.setBorderColor(BORDER_LIGHT);
        statusCell.setPadding(7);
        statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        statusCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        banner.addCell(statusCell);

        doc.add(banner);
    }

    private PremixStatusResponse findPremixStatus(
            List<PremixStatusResponse> statuses,
            int premixNo) {
        if (statuses == null) return null;
        return statuses.stream()
                .filter(status -> status.getPremixNo() == premixNo)
                .findFirst()
                .orElse(null);
    }

    private MotorStatusResponse findMotorStatus(
            List<MotorStatusResponse> statuses,
            String motorId) {
        if (statuses == null || motorId == null) return null;
        return statuses.stream()
                .filter(status -> motorId.equals(status.getMotorId()))
                .findFirst()
                .orElse(null);
    }

    private void addWorkflowStatusCell(PdfPTable table, Object status) {
        Color statusColor = workflowStatusColor(status);
        PdfPCell cell = new PdfPCell(new Phrase(
                workflowStatusLabel(status),
                new Font(Font.HELVETICA, 8, Font.BOLD, statusColor)));
        cell.setPadding(5);
        cell.setBackgroundColor(WHITE);
        cell.setBorderColor(BORDER_LIGHT);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private Color workflowStatusColor(Object status) {
        return switch (workflowStatusKey(status)) {
            case "APPROVED", "COMPLETELY_APPROVED", "COMPLETED" -> PASS_GREEN;
            case "REJECTED" -> FAIL_RED;
            case "WAITING_FOR_APPROVAL", "WAITING_FOR_PARTIAL_APPROVAL",
                 "WAITING_FOR_COMPLETE_APPROVAL" -> STATUS_PENDING;
            case "IN_PROGRESS", "DRAFT" -> BRAND_BLUE;
            default -> PENDING_GRAY;
        };
    }

    private String workflowStatusKey(Object status) {
        return status == null
                ? ""
                : String.valueOf(status)
                .trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(java.util.Locale.ROOT);
    }

    private String workflowStatusLabel(Object status) {
        String key = workflowStatusKey(status);
        return key.isEmpty() ? "—" : prettyText(key);
    }

    /**
     * Status + Completion summary box
     */
    private void addInsightsSummary(Document doc, int passed, int total, int pct, String status)
            throws DocumentException {
        spacer(doc, 6);
        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{34, 33, 33});

        PdfPCell c1 = new PdfPCell(new Phrase("Tests: " + passed + " / " + total, BODY_BOLD));
        PdfPCell c2 = new PdfPCell(new Phrase("Completion: " + pct + "%", BODY_BOLD));
        PdfPCell c3 = statusBadgeCell(status);
        for (PdfPCell c : new PdfPCell[]{c1, c2}) {
            c.setPadding(6);
            c.setBackgroundColor(new Color(240, 245, 252));
            c.setBorderColor(BORDER_LIGHT);
        }
        t.addCell(c1);
        t.addCell(c2);
        t.addCell(c3);
        doc.add(t);
        spacer(doc, 4);
    }

    private PdfPCell statusBadgeCell(String status) {
        Color bg = workflowStatusColor(status);
        PdfPCell c = new PdfPCell(new Phrase("Status: " + workflowStatusLabel(status),
                new Font(Font.HELVETICA, 8, Font.BOLD, bg)));
        c.setBackgroundColor(WHITE);
        c.setPadding(6);
        c.setBorderColor(BORDER_LIGHT);
        return c;
    }

    private PdfPTable createTable(float[] widths) throws DocumentException {
        PdfPTable table = new PdfPTable(widths.length);
        table.setWidthPercentage(100);
        table.setWidths(widths);
        table.setSpacingBefore(2);
        return table;
    }

    private void addHeaders(PdfPTable table, String... headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, HEADER_FONT));
            cell.setBackgroundColor(BRAND_BLUE);
            cell.setPadding(5);
            cell.setBorderColor(BRAND_BLUE);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell);
        }
        table.setHeaderRows(1);
    }

    private void addCenteredHeaders(PdfPTable table, String... headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_FONT));
            cell.setBackgroundColor(BRAND_BLUE);
            cell.setPadding(5);
            cell.setBorderColor(BRAND_BLUE);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell);
        }
        table.setHeaderRows(1);
    }

    private void addCenteredCell(PdfPTable table, String value, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(str(value), BODY_FONT));
        cell.setPadding(5);
        cell.setBackgroundColor(background);
        cell.setBorderColor(BORDER_LIGHT);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private PdfPCell mixingHeaderCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(BRAND_BLUE);
        cell.setPadding(5);
        cell.setBorderColor(BRAND_BLUE);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private void addCell(PdfPTable table, String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(str(text), font));
        cell.setPadding(5);
        cell.setBackgroundColor(bg);
        cell.setBorderColor(BORDER_LIGHT);
        if (hasBlueColumnHeader(table)) {
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        }
        table.addCell(cell);
    }

    private boolean hasBlueColumnHeader(PdfPTable table) {
        if (table.getHeaderRows() == 0 || table.size() == 0) return false;
        PdfPCell[] headerCells = table.getRow(0).getCells();
        if (headerCells == null) return false;
        for (PdfPCell headerCell : headerCells) {
            if (headerCell != null && BRAND_BLUE.equals(headerCell.getBackgroundColor())) {
                return true;
            }
        }
        return false;
    }

    private void addLeftAlignedCell(PdfPTable table, String text, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(str(text), BODY_FONT));
        cell.setPadding(5);
        cell.setBackgroundColor(bg);
        cell.setBorderColor(BORDER_LIGHT);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private void addKV(PdfPTable table, String key, String value) {
        boolean even = table.size() % 2 == 0;
        Color bg = even ? WHITE : ROW_ALT;
        addCell(table, key, BODY_BOLD, bg);
        addCell(table, value, BODY_FONT, bg);
    }

    private void addKV3(PdfPTable table, String a, String b, String c) {
        boolean even = table.size() % 2 == 0;
        Color bg = even ? WHITE : ROW_ALT;
        addCell(table, a, BODY_BOLD, bg);
        addCell(table, b, BODY_FONT, bg);
        addCell(table, c, BODY_FONT, bg);
    }

    private void addMotorPairRow(PdfPTable table, String label, String m1, String m2, boolean alt) {
        Color bg = alt ? ROW_ALT : WHITE;
        addCell(table, label, BODY_BOLD, bg);
        addCell(table, str(m1), BODY_FONT, bg);
        addCell(table, str(m2), BODY_FONT, bg);
    }

    private void spacer(Document doc, float height) throws DocumentException {
        Paragraph sp = new Paragraph(" ");
        sp.setSpacingAfter(height);
        doc.add(sp);
    }

    // ── Util helpers ──────────────────────────────────────────────
    private String str(Object o) {
        if (o == null) return "—";
        if (o instanceof LocalDate date) {
            return date.format(PDF_DATE_FORMAT);
        }
        if (o instanceof LocalDateTime dateTime) {
            return DateTimeConversionUtil.formatLocalDateTime(dateTime);
        }
        String s = o.toString().trim();
        if (s.indexOf('T') > 0) {
            try {
                return formatPdfDateTime(DateTimeConversionUtil.parseLocalDateTime(s));
            } catch (DateTimeParseException ignored) {
                // Preserve non-date strings that happen to contain a capital T.
            }
        }
        return s.isEmpty() ? "—" : s;
    }

    /**
     * Propellant Wt (C−B−A); blank when any weight is missing/non-numeric.
     */
    private String computeInhibitionPropellantWeight(InhibitionTableDTO row) {
        if (row == null) {
            return "";
        }
        Double a = parseOptionalDouble(row.getLinerCoatedSleeveWeight());
        Double b = parseOptionalDouble(row.getWeightBeforeInhibition());
        Double c = parseOptionalDouble(row.getWeightAfterInhibition());
        if (a == null || b == null || c == null) {
            return "";
        }
        double value = c - b - a;
        if (Double.isFinite(value) && value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private Double parseOptionalDouble(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            double value = Double.parseDouble(raw.trim());
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String documentReference(DocumentDto doc) {
        if (doc == null) {
            return "—";
        }
        if (doc.getFileName() != null && !doc.getFileName().isBlank()) {
            return doc.getFileName();
        }
        return str(doc.getFileId());
    }

    private String yesNo(boolean value) {
        return value ? "Yes" : "No";
    }

    private String formatPdfDateTime(LocalDateTime value) {
        return value == null
                ? "—"
                : DateTimeConversionUtil.formatLocalDateTime(value);
    }

    private String formatPdfDateTimeValue(Object value) {
        if (value == null) return "—";
        if (value instanceof LocalDateTime localDateTime) {
            return formatPdfDateTime(localDateTime);
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) return "—";
        try {
            return formatPdfDateTime(DateTimeConversionUtil.toLocalDateTime(text));
        } catch (RuntimeException ignored) {
            return text;
        }
    }

    private String formatRange(SpecificationRangeDto r) {
        if (r == null) return "—";
        String min = r.getMinValue() != null ? r.getMinValue().toString() : "—";
        String max = r.getMaxValue() != null ? r.getMaxValue().toString() : "—";
        String unit = r.getUnit() != null ? " " + r.getUnit() : "";
        return min + " – " + max + unit;
    }

    private String defObs(Object d) {
        if (d == null) return "—";
        try {
            var m = (com.lowagie.text.pdf.PdfPCell) null; // unused — just type-safe cast hint
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<?, ?> map = om.convertValue(d, Map.class);
            Object obs = map.get("observation");
            return obs != null && !obs.toString().isBlank() ? obs.toString() : "—";
        } catch (Exception e) {
            return "—";
        }
    }

    private String defFiles(Object d) {
        if (d == null) return "—";
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<?, ?> map = om.convertValue(d, Map.class);
            Object files = map.get("filePaths");
            if (files instanceof List<?> list) return list.size() + " file(s)";
        } catch (Exception e) { /* ignore */ }
        return "—";
    }

    private String today() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }

    private FormPdfResponse buildResponse(FormPdfRequest request, ByteArrayOutputStream baos) {
        byte[] bytes = baos.toByteArray();
        String fileName = "Form_" + request.getFormId() + "_"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";
        return FormPdfResponse.builder()
                .formId(request.getFormId())
                .subDepartmentId(request.getSubDepartmentId())
                .fileName(fileName)
                .fileUrl("/api/v1/approver/form/pdf/download/" + fileName)
                .base64(Base64.getEncoder().encodeToString(bytes))
                .fileSizeBytes(bytes.length)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private String resolveFormLabel(Long id) {
        return switch (id.intValue()) {
            case 1 -> "Raw Material Sourcing";
            case 2 -> "Rocket Motor Casing";
            case 3 -> "Raw Material Preparation";
            case 4 -> "Case Preparation";
            case 5 -> "Mixing";
            case 6 -> "Casting and Curing";
            case 7 -> "Post-Cure Operations";
            case 8 -> "Subscale";
            case 9 -> "Trimming";
            case 10 -> "NDT Report";
            case 11 -> "Quality Assurance";
            case 12 -> "Static Test Facility";
            case 13 -> "Dispatch";
            default -> "QC Form";
        };
    }


    private Color getStatusColor(String status) {
        if (status == null) return STATUS_DEFAULT;

        String s = status.trim().toLowerCase();

        // Approved variants
        if (s.equals("approved") || s.equals("cleared")
                || s.equals("pass") || s.contains("completely_approved")) {
            return STATUS_APPROVED;
        }
        // Rejected variants
        if (s.contains("reject") || s.contains("fail") || s.equals("rejected")) {
            return STATUS_REJECTED;
        }
        // Pending / Waiting variants
        if (s.contains("waiting") || s.contains("pending") || s.contains("approval")
                || s.contains("in progress")) {
            return STATUS_PENDING;
        }
        return STATUS_DEFAULT;
    }

    /**
     * Creates a coloured status Phrase (useful for headers / identification cards).
     */
    private Phrase createStatusPhrase(String status) {
        Color color = getStatusColor(status);
        Font statusFont = new Font(Font.HELVETICA, 8, Font.BOLD, color);
        String label = status == null || status.isBlank() || "-".equals(status.trim())
                ? "-"
                : workflowStatusLabel(status);
        return new Phrase(label, statusFont);
    }

    /**
     * Adds a status cell with coloured text (and optional light background).
     */
    private void addStatusCell(PdfPTable table, String status, Color bg) {
        Color textColor = getStatusColor(status);
        Font statusFont = new Font(Font.HELVETICA, 8, Font.BOLD, textColor);

        PdfPCell cell = new PdfPCell(new Phrase(str(status), statusFont));
        cell.setBackgroundColor(bg != null ? bg : WHITE);
        cell.setPadding(5);
        cell.setBorderColor(BORDER_LIGHT);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    private void addInfoStripCell(PdfPTable table, String label, String value) {
        addInfoStripCell(table, label, value, 1);
    }

    private void addInfoStripCell(PdfPTable table, String label, String value, int colspan) {
        PdfPCell cell = new PdfPCell();
        cell.setColspan(colspan);
        cell.setBorderColor(BORDER_LIGHT);
        cell.setPadding(6);
        cell.setBackgroundColor(WHITE);

        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n", LABEL_FONT));
        p.add(new Chunk(value != null ? value : "-", BODY_BOLD));
        cell.addElement(p);
        table.addCell(cell);
    }

    /**
     * Adds a completely empty cell (no hyphen, no text)
     */
    private void addEmptyCell(PdfPTable table, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(""));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setBorderColor(BORDER_LIGHT);
        table.addCell(cell);
    }
}
