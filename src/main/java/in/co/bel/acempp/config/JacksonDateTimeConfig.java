package in.co.bel.acempp.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers.LocalDateTimeValue;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers.LocalDateValue;
import in.co.bel.acempp.util.DateTimeConversionUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonDateTimeConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, LocalDate>() {
            @Override
            public LocalDate convert(String source) {
                return DateTimeConversionUtil.parseLocalDate(source);
            }
        });
    }

    @Bean
    public Module flexibleDateInputModule() {
        SimpleModule module = new SimpleModule("flexible-date-input-module");
        module.addDeserializer(LocalDate.class, new LocalDateValue());
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeValue());
        module.addSerializer(
                LocalDate.class,
                new LocalDateSerializer(
                        DateTimeFormatter.ofPattern(DateTimeConversionUtil.UI_DATE_PATTERN)));
        module.addSerializer(
                LocalDateTime.class,
                new LocalDateTimeSerializer(
                        DateTimeFormatter.ofPattern(DateTimeConversionUtil.UI_DATE_TIME_PATTERN)));
        module.addSerializer(String.class, new JsonSerializer<>() {
            @Override
            public void serialize(
                    String value,
                    JsonGenerator generator,
                    SerializerProvider serializers) throws IOException {
                generator.writeString(DateTimeConversionUtil.formatLocalDateTimeString(value));
            }
        });
        return module;
    }
}
