package in.co.bel.acempp.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "roles")
public class RoleMaster {

    @Id
    private String id;

    private Long roleId;

    private String roleName;

    private Integer sort;
}
