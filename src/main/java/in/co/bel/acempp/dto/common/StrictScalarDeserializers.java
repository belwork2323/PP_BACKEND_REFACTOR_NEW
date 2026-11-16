package in.co.bel.acempp.dto.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import in.co.bel.acempp.util.DateTimeConversionUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Shared strict deserializers for nullable scalar DTO fields.
 */
public final class StrictScalarDeserializers {

    private StrictScalarDeserializers() {
    }

    public static final class BigDecimalValue extends JsonDeserializer<BigDecimal> {
        @Override
        public BigDecimal deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonToken token = parser.currentToken();
            if (token == JsonToken.VALUE_NULL) return null;
            if (token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT) {
                return parser.getDecimalValue();
            }
            if (token == JsonToken.VALUE_STRING) {
                String value = parser.getText().trim();
                if (value.isEmpty()) return null;
                try {
                    return new BigDecimal(value);
                } catch (NumberFormatException ignored) {
                    // Fall through to the DTO-friendly mapping error below.
                }
            }
            throw JsonMappingException.from(parser, "Expected a numeric value or null");
        }
    }

    /** Accepts only a JSON number (or null), without string coercion. */
    public static final class JsonNumberBigDecimalValue extends JsonDeserializer<BigDecimal> {
        @Override
        public BigDecimal deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonToken token = parser.currentToken();
            if (token == JsonToken.VALUE_NULL) return null;
            if (token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT) {
                return parser.getDecimalValue();
            }
            throw JsonMappingException.from(parser, "Expected a JSON numeric value or null");
        }
    }

    public static final class IntegerValue extends JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonToken token = parser.currentToken();
            if (token == JsonToken.VALUE_NULL) return null;
            if (token == JsonToken.VALUE_NUMBER_INT) return parser.getIntValue();
            if (token == JsonToken.VALUE_STRING) {
                String value = parser.getText().trim();
                if (value.isEmpty()) return null;
                try {
                    return Integer.valueOf(value);
                } catch (NumberFormatException ignored) {
                    // Fall through to the DTO-friendly mapping error below.
                }
            }
            throw JsonMappingException.from(parser, "Expected an integer value or null");
        }
    }

    /** Accepts only a JSON integer (or null), without string coercion. */
    public static final class JsonNumberIntegerValue extends JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonToken token = parser.currentToken();
            if (token == JsonToken.VALUE_NULL) return null;
            if (token == JsonToken.VALUE_NUMBER_INT) return parser.getIntValue();
            throw JsonMappingException.from(parser, "Expected a JSON integer value or null");
        }
    }

    /** Accepts only a JSON integer (or null), without string coercion. */
    public static final class JsonNumberLongValue extends JsonDeserializer<Long> {
        @Override
        public Long deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonToken token = parser.currentToken();
            if (token == JsonToken.VALUE_NULL) return null;
            if (token == JsonToken.VALUE_NUMBER_INT) return parser.getLongValue();
            throw JsonMappingException.from(parser, "Expected a JSON integer or null");
        }
    }

    public static final class StringValue extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonToken token = parser.currentToken();
            if (token == JsonToken.VALUE_NULL) return null;
            if (token == JsonToken.VALUE_STRING) return parser.getText();
            throw JsonMappingException.from(parser, "Expected a string value or null");
        }
    }

    public static final class LocalDateTimeValue extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser parser, DeserializationContext context)
                throws IOException {
            JsonToken token = parser.currentToken();
            if (token == JsonToken.VALUE_NULL) return null;
            if (token == JsonToken.VALUE_STRING) {
                String value = parser.getText().trim();
                if (value.isEmpty()) return null;
                return DateTimeConversionUtil.parseLocalDateTime(value);
            }
            // Accept numeric / array / object forms produced by valueToTree round-trips.
            Object raw = parser.readValueAs(Object.class);
            try {
                return DateTimeConversionUtil.toLocalDateTime(raw);
            } catch (RuntimeException exception) {
                throw JsonMappingException.from(
                        parser,
                        "Expected " + DateTimeConversionUtil.SUPPORTED_DATE_TIME_FORMATS + ", or null",
                        exception);
            }
        }
    }

    public static final class LocalDateValue extends JsonDeserializer<LocalDate> {
        @Override
        public LocalDate deserialize(JsonParser parser, DeserializationContext context)
                throws IOException {
            JsonToken token = parser.currentToken();
            if (token == JsonToken.VALUE_NULL) return null;
            if (token == JsonToken.VALUE_STRING) {
                String value = parser.getText().trim();
                if (value.isEmpty()) return null;
                return DateTimeConversionUtil.parseLocalDate(value);
            }
            throw JsonMappingException.from(
                    parser,
                    "Expected " + DateTimeConversionUtil.SUPPORTED_DATE_FORMATS + ", or null");
        }
    }

}
