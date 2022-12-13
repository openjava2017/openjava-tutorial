package org.openjava.jackson.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

public class JsonUtils {
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private static final String TIME_FORMAT = "HH:mm:ss";

    private static ObjectMapper objectMapper = initObjectMapper();

    private static ObjectMapper initObjectMapper(){
        Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder = new Jackson2ObjectMapperBuilder();
        initObjectMapperBuilder(jackson2ObjectMapperBuilder);
        ObjectMapper objectMapper = jackson2ObjectMapperBuilder.createXmlMapper(false).build();
        objectMapper.setSerializerFactory(objectMapper.getSerializerFactory());
        return objectMapper;
    }

    public static void initObjectMapperBuilder(Jackson2ObjectMapperBuilder builder) {
        //序列化java.util.Date类型
        builder.dateFormat(new SimpleDateFormat(DATE_TIME_FORMAT));
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
    }

    public static <T> T fromJsonString(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Deserialize json exception", ex);
        }
    }

    public static <T> T fromJsonString(String json, TypeReference<T> jsonTypeReference){
        try {
            return objectMapper.readValue(json, jsonTypeReference);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Deserialize json array exception", ex);
        }
    }

    public static <T> String toJsonString(T object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Serialize json exception", ex);
        }
    }
}
