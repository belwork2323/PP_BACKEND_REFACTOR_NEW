package in.co.bel.acempp.dto.user.operations;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

// UserBatchListRequest.java
@Data
public class UserBatchListRequest {
    private Long  subDepartmentId;
    private String  userId;
    @NotNull   private int    page;
    @NotNull   private int   limit;
    private List<String> status;
    private List<String> priority;
    private String       search;
    private List<String> batchIds;
    private List<String> batchTypes;
    private List<Object> motorStages;
    private List<String> motorIds;
    private List<String> formSubmittedBy;
    @JsonDeserialize(using = FilterDateDeserializer.class)
    private LocalDate fromDate;
    @JsonDeserialize(using = FilterDateDeserializer.class)
    private LocalDate toDate;

    public static class FilterDateDeserializer extends JsonDeserializer<LocalDate> {
        @Override
        public LocalDate deserialize(JsonParser parser, DeserializationContext context)
                throws IOException {
            if (parser.currentToken() == JsonToken.VALUE_NULL) return null;
            String value = parser.getValueAsString();
            if (value == null || value.isBlank()) return null;
            if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                throw JsonMappingException.from(parser, "Date must use yyyy-MM-dd format");
            }
            try {
                LocalDate date = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
                if (date.getYear() < 1) {
                    throw JsonMappingException.from(parser, "Date year must be between 0001 and 9999");
                }
                return date;
            } catch (DateTimeParseException exception) {
                throw JsonMappingException.from(parser, "Invalid date", exception);
            }
        }
    }
}
