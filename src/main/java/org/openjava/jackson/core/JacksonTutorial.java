package org.openjava.jackson.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.openjava.jackson.pojo.Student;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * @author: brenthuang
 * @date: 2022/10/10
 */
public class JacksonTutorial {

    public static void main(String[] args) throws Exception {
        // @see https://www.baeldung.com/jackson
        var student = new Student();
        student.setId(10L);
        student.setName("Hello");
        student.setBirthday(LocalDateTime.now());

        var objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        objectMapper.setTimeZone(TimeZone.getTimeZone(ZoneOffset.of("+8")));
        // Json串的属性无JavaBean字段对应时，避免抛出异常
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // JavaBean中primitive类型的字段无Json属性时，避免抛出异常
        objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        // Json串数字类型属性，赋值JavaBean中Enum字段时，避免抛出异常
        objectMapper.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, false);
        objectMapper.configure(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, true);

        // JDK8时间类型模块
        var timeModule = new JavaTimeModule();
        var dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        var timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        // 添加自定义序列化
        timeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        timeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        timeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
        // 添加自定义反序列化
        timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
        timeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));
        timeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));
        objectMapper.registerModule(timeModule);

        var json = objectMapper.writeValueAsString(student);
        System.out.println(json);
        student = objectMapper.readValue(json, Student.class);
        System.out.println(student.getName());
        var studentMap = objectMapper.readValue(json, new TypeReference<Map<String, Object>>(){});
        System.out.println(String.format("id: %s, name: %s, birthday: %s", studentMap.get("id"), studentMap.get("name"), studentMap.get("birthday")));
        JsonNode rootNode = objectMapper.readTree(json);
        JsonNode nameNode = rootNode.get("name");
        nameNode.asText("default name if null");

        String jsonCarArray =
            "[{\"id\": \"10\", \"name\": \"Brent\", \"birthday\": \"2022-10-10 14:20:21\"}, {\"id\": \"11\", \"name\": \"Winton\", \"birthday\": \"2022-10-11 14:20:21\"}]";
        var students = objectMapper.readValue(jsonCarArray, new TypeReference<List<Student>>(){});
        students.forEach(s -> System.out.println(String.format("id: %s, name: %s, birthday: %s", s.getId(), s.getName(), s.getBirthday())));
        var studentArray = objectMapper.readValue(jsonCarArray, Student[].class);
        System.out.println(studentArray.length);

    }
}
