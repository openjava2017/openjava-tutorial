package org.openjava.springboot.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

@Configuration
public class GlobalConfiguration {
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private static final String TIME_FORMAT = "HH:mm:ss";

    @Bean
    @ConditionalOnClass(JavaTimeModule.class)
    public Jackson2ObjectMapperBuilderCustomizer customizeJacksonConfig(){
        return builder -> {
            //序列化java.util.Date类型
            builder.dateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
            builder.timeZone(TimeZone.getTimeZone(ZoneOffset.of("+8")));
            builder.serializationInclusion(JsonInclude.Include.NON_NULL);
            builder.featuresToDisable(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, // Json串的属性无JavaBean字段对应时，避免抛出异常
                DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, // JavaBean中primitive类型的字段无Json属性时，避免抛出异常
                DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, // Json串数字类型属性，赋值JavaBean中Enum字段时，避免抛出异常
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                SerializationFeature.FAIL_ON_EMPTY_BEANS
            );
            builder.featuresToEnable(
                DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY
            );

            var dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
            var dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
            var timeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT);
            // 添加自定义序列化
            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
            builder.serializerByType(LocalDate.class, new LocalDateSerializer(dateFormatter));
            builder.serializerByType(LocalTime.class, new LocalTimeSerializer(timeFormatter));
            // 添加自定义反序列化
            builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
            builder.deserializerByType(LocalDate.class, new LocalDateDeserializer(dateFormatter));
            builder.deserializerByType(LocalTime.class, new LocalTimeDeserializer(timeFormatter));
        };
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
