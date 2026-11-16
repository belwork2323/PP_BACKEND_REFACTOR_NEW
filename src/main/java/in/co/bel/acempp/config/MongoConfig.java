package in.co.bel.acempp.config;

import in.co.bel.acempp.util.DateTimeConversionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
                new NumberToStringConverter(),
                new StringToBigDecimalConverter(),
                new StringToLocalDateConverter(),
                new StringToLocalDateTimeConverter()));
    }

    @ReadingConverter
    static class NumberToStringConverter implements Converter<Number, String> {
        @Override
        public String convert(Number source) {
            if (source == null) {
                return null;
            }
            double value = source.doubleValue();
            if (value == Math.rint(value) && !Double.isInfinite(value)) {
                return String.valueOf((long) value);
            }
            return source.toString();
        }
    }

    /**
     * Tolerates legacy or invalid string values stored in numeric fields (e.g. mixQuantity).
     * Returns null instead of failing batch reads when the value is not a valid decimal.
     */
    @Slf4j
    @ReadingConverter
    static class StringToBigDecimalConverter implements Converter<String, BigDecimal> {
        @Override
        public BigDecimal convert(String source) {
            if (source == null) {
                return null;
            }
            String trimmed = source.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                return new BigDecimal(trimmed);
            } catch (NumberFormatException ex) {
                log.warn("Ignoring invalid BigDecimal value in MongoDB document: [{}]", source);
                return null;
            }
        }
    }

    @ReadingConverter
    static class StringToLocalDateConverter implements Converter<String, LocalDate> {
        @Override
        public LocalDate convert(String source) {
            try {
                return DateTimeConversionUtil.parseLocalDate(source);
            } catch (DateTimeParseException dateParseException) {
                LocalDateTime dateTime = DateTimeConversionUtil.parseLocalDateTime(source);
                if (dateTime == null) {
                    throw dateParseException;
                }
                return dateTime.toLocalDate();
            }
        }
    }

    @ReadingConverter
    static class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {
        @Override
        public LocalDateTime convert(String source) {
            return DateTimeConversionUtil.parseLocalDateTime(source);
        }
    }
}
