package in.co.bel.acempp.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "form_id_counters")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormIdCounter {

    @Id
    private String id;

    private long sequence;
}
