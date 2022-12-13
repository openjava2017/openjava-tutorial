package org.openjava.springboot.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.openjava.jackson.core.JsonUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Configuration
public class GlobalConfiguration {
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private static final String TIME_FORMAT = "HH:mm:ss";

    @Bean
    @ConditionalOnClass(JavaTimeModule.class)
    public Jackson2ObjectMapperBuilderCustomizer customizeJacksonConfig() {
        return JsonUtils::initObjectMapperBuilder;
    }

    @Bean
    public Converter<String, LocalDateTime> localDateTimeConverter() {
        return new Converter<String, LocalDateTime>() {
            @Override
            public LocalDateTime convert(String source) {
                return StringUtils.hasText(source) ? LocalDateTime.parse(source, DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)) : null;
            }
        };
    }

    @Bean
    public Converter<String, LocalDate> localDateConverter() {
        return new Converter<String, LocalDate>() {
            @Override
            public LocalDate convert(String source) {
                return StringUtils.hasText(source) ? LocalDate.parse(source, DateTimeFormatter.ofPattern(DATE_FORMAT)) : null;
            }
        };
    }

    @Bean
    public Converter<String, LocalTime> localTimeConverter() {
        return new Converter<String, LocalTime>() {
            @Override
            public LocalTime convert(String source) {
                return StringUtils.hasText(source) ? LocalTime.parse(source, DateTimeFormatter.ofPattern(TIME_FORMAT)) : null;
            }
        };
    }

    @Bean
    public Converter<String, Date> dateConverter() {
        return new Converter<String, Date>() {
            @Override
            public Date convert(String source) {
                try {
                    return StringUtils.hasText(source) ? new SimpleDateFormat(DATE_TIME_FORMAT).parse(source) : null;
                } catch (Exception ex) {
                    throw new IllegalArgumentException(String.format("Error parse %s to Date", source), ex);
                }
            }
        };
    }
}
