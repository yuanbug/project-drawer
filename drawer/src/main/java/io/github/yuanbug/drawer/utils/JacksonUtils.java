package io.github.yuanbug.drawer.utils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author yuanbug
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JacksonUtils {

    public static final ObjectMapper OBJECT_MAPPER = buildObjectMapper();

    public static ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);
        mapper.configure(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        // 让ObjectMapper对getter不可见，默认使用java类的字段名作为JSON字段名，如果不配置，默认情况下getter方法名开头为连续大写字母时，转换得到的JSON字段名开头会是连续小写
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
        return mapper;
    }

    public static String toJsonString(Object object) {
        return toJsonString(object, true, false);
    }

    public static String toJsonString(Object object, boolean pretty) {
        return toJsonString(object, true, pretty);
    }

    /**
     * 按字段转换成JSON
     *
     * @param stringTransform 如果对象本身就是字符串，是否再次转换（多包裹一层双引号）
     * @param pretty          是否整理格式
     * @apiNote 会应用@JsonProperty注解
     */
    public static String toJsonString(Object object, boolean stringTransform, boolean pretty) {
        if (!stringTransform && object instanceof String string) {
            return string;
        }
        try {
            if (pretty) {
                return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
            }
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static void println(Object object) {
        System.out.println(toJsonString(object));
    }

    public static void println(Object object, boolean pretty) {
        System.out.println(toJsonString(object, pretty));
    }

    public static ObjectNode parseToObjectNode(String json) {
        JsonNode jsonNode = parse(json);
        if (jsonNode instanceof ObjectNode objectNode) {
            return objectNode;
        }
        throw new IllegalArgumentException(String.format("%s不是对象JSON", json));
    }

    public static <T> List<T> parseList(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, clazz));
        } catch (Exception e) {
            log.warn("Json列表解析错误 {}", json, e);
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private static <T> T parsePrimitiveType(String json, Class<T> clazz) {
        try {
            return (T) PrimitiveTypeUtils.parseString(json, clazz);
        } catch (Exception e) {
            log.warn("解析基本类型出错 {}", json, e);
            return (T) PrimitiveTypeUtils.getZeroValue(clazz);
        }
    }

    public static <T> T transform(JsonNode json, Class<T> clazz) {
        try {
            if (null == clazz || null == json || json.isMissingNode() || json.isNull()) {
                return null;
            }
            if (clazz.isPrimitive()) {
                return parsePrimitiveType(json.toString(), clazz);
            }
            return OBJECT_MAPPER.treeToValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> T transform(Object object, JavaType type) {
        if (null == object || null == type) {
            return null;
        }
        return OBJECT_MAPPER.convertValue(object, type);
    }

    public static <T> T transform(Object object, TypeReference<T> type) {
        if (null == object || null == type) {
            return null;
        }
        return OBJECT_MAPPER.convertValue(object, type);
    }

    public static JsonNode parse(String json) {
        try {
            return OBJECT_MAPPER.readTree(Optional.ofNullable(json).orElse(""));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> T parse(String json, Class<T> clazz) {
        if (clazz.isPrimitive()) {
            return parsePrimitiveType(json, clazz);
        }
        return transform(parse(json), clazz);
    }

    public static <T> T parse(String json, JavaType type) {
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> T parse(String json, TypeReference<T> type) {
        try {
            if (null == type || null == json) {
                return null;
            }
            return OBJECT_MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static boolean isNull(JsonNode jsonNode) {
        return null == jsonNode || jsonNode.isNull();
    }

    public static boolean isNotNull(JsonNode jsonNode) {
        return null != jsonNode && !jsonNode.isNull() && !jsonNode.isMissingNode();
    }

    /**
     * 依次尝试使用mappers中的各个函数获取结果，直到取得的结果满足给定条件为止，若找不到，返回给定默认值
     */
    public static <T> T getInTurns(JsonNode jsonNode, Supplier<T> defaultValue, Predicate<T> predicate, List<Function<JsonNode, T>> mappers) {
        if (isNull(jsonNode)) {
            return defaultValue.get();
        }
        for (Function<JsonNode, T> mapper : mappers) {
            T result = mapper.apply(jsonNode);
            if (predicate.test(result)) {
                return result;
            }
        }
        return defaultValue.get();
    }

    public static <T> T getInTurns(JsonNode jsonNode, Supplier<T> defaultValue, Function<JsonNode, T> subNodeMapper, String... subNodeNames) {
        return getInTurns(
                jsonNode,
                defaultValue,
                Objects::nonNull,
                Arrays.stream(subNodeNames)
                        .map(name -> (Function<JsonNode, T>) node -> Optional.ofNullable(node.get(name)).map(subNodeMapper).orElse(null))
                        .toList()
        );
    }

    public static <T> T getInTurns(JsonNode jsonNode, Function<JsonNode, T> subNodeMapper, String... subNodeNames) {
        return getInTurns(jsonNode, () -> null, subNodeMapper, subNodeNames);
    }

}
