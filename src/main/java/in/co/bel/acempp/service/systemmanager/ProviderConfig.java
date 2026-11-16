package in.co.bel.acempp.service.systemmanager;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Configuration
public class ProviderConfig {

    @Bean
    public Map<Long, SubDepartmentFormProvider> formProviderMap(
            RawMaterialPrepFormProvider rawMaterial,
            CasePrepFormProvider casePrep, MixingFormProvider mixing, CastingCuringFormProvider castingCuring,
            PostCureFormProvider postCure, SubscaleProcessingFormProvider subscale,  TrimmingFormProvider trimming,
          NdtFormProvider ndt, DispatchFormProvider dispatch) {
        Map<Long, SubDepartmentFormProvider> map = new HashMap<>();
        map.put(3L, rawMaterial);
        map.put(4L, casePrep);
        map.put(5L, mixing);
        map.put(6L, castingCuring);
        map.put(7L, postCure);
        map.put(8L, subscale);
        map.put(9L, trimming);
        map.put(10L, ndt);
        map.put(13L, dispatch);





        return map;
    }
}