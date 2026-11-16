package in.co.bel.acempp.dto.form;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class FormPdfRequest {
    @NotBlank
    private String formId;
    @NotNull  private Long   subDepartmentId;
    private boolean download;
}