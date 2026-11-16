package in.co.bel.acempp.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "departments")
public class DepartmentMaster {

    @Id
    private String id;

    private Long departmentId;

    private String departmentName;

    private List<SubDepartmentMaster> subDepartments;
}
