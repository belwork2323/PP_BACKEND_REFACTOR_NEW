package in.co.bel.acempp.service;

import in.co.bel.acempp.entity.dispatch.Dispatch;
import in.co.bel.acempp.entity.manufacturing.*;
import in.co.bel.acempp.entity.qualitycontrol.Ndt;
import in.co.bel.acempp.entity.qualitycontrol.QcDivision;
import in.co.bel.acempp.entity.qualitycontrol.StaticTesting;
import in.co.bel.acempp.repository.dispatch.DispatchRepository;
import in.co.bel.acempp.repository.manufacturing.*;
import in.co.bel.acempp.repository.qualitycontrol.NdtRepository;
import in.co.bel.acempp.repository.qualitycontrol.QcDivisionRepository;
import in.co.bel.acempp.repository.qualitycontrol.StaticTestingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BatchFormResolver {

    private final RawMaterialPreparationRepository rawMaterialPrepRepo;
    private final CasePreparationRepository casePrepRepo;
    private final MixingRepository mixingRepository;
    private final CastingCuringRepository castingCuringRepository;
    private final PostCureRepository postCureRepository;
    private final SubscaleProcessingRepository subscaleProcessingRepository;
    private final TrimmingRepository trimmingRepository;
    private final NdtRepository ndtRepository;
    private final QcDivisionRepository qcDivisionRepository;
    private final StaticTestingRepository staticTestingRepository;
    private final DispatchRepository dispatchRepository;

    // ... add more repos as you create forms for other sub-departments

    /**
     * Returns the formId for a batch in a specific sub-department, or null if no form exists yet.
     */
    public String resolveFormId(String batchId, Long subDeptId) {
        return switch (subDeptId.intValue()) {
            case 3  -> rawMaterialPrepRepo.findByBatchIdAndIsDeletedFalse(batchId)
                    .map(RawMaterialPreparation::getFormId).orElse(null);
            case 4  -> casePrepRepo.findByBatchIdAndIsDeletedFalse(batchId)
                    .map(CasePreparation::getFormId).orElse(null);
            case 5  -> mixingRepository.findByBatchIdAndIsDeletedFalse(batchId).
                    map(Mixing::getFormId).orElse(null);
            case 6  -> castingCuringRepository.findByBatchIdAndIsDeletedFalse(batchId).
                    map(CastingCuring::getFormId).orElse(null);
            case 7  -> postCureRepository.findByBatchIdAndIsDeletedFalse(batchId).
                    map(PostCureOperations::getFormId).orElse(null);
            case 8  -> subscaleProcessingRepository.findByBatchIdAndIsDeletedFalse(batchId).
                    map(SubscaleProcessing::getFormId).orElse(null);
            case 9  -> trimmingRepository.findByBatchIdAndIsDeletedFalse(batchId).
                    map(Trimming::getFormId).orElse(null);
            case 10 -> ndtRepository.findByBatchIdAndIsDeletedFalse(batchId).
                    map(Ndt::getFormId).orElse(null);
            case 11 -> qcDivisionRepository.findByBatchIdAndIsDeletedFalse(batchId).
                    map(QcDivision::getFormId).orElse(null);
            case 12 -> staticTestingRepository.findByBatchIdAndIsDeletedFalse(batchId).
                    map(StaticTesting::getFormId).orElse(null);
            case 13 -> dispatchRepository.findByBatchIdAndIsDeletedFalse(batchId).
                    map(Dispatch::getFormId).orElse(null);
            default -> null;
        };
    }
}