package io.github.yuanbug.drawer.domain.info;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import io.github.yuanbug.drawer.domain.convertor.MethodIdDeserializer;
import io.github.yuanbug.drawer.utils.AstUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author yuanbug
 */
@Getter
@AllArgsConstructor
@JsonDeserialize(using = MethodIdDeserializer.class)
public class MethodId {

    private String className;

    private String methodName;

    private List<String> paramTypes;

    @Override
    public String toString() {
        return String.format(
                "%s#%s(%s)",
                className,
                methodName,
                String.join(",", paramTypes)
        );
    }

    public static MethodId parse(String methodId) {
        String className = methodId.substring(0, methodId.indexOf("#"));
        String methodName = methodId.substring(className.length() + 1, methodId.indexOf("("));
        List<String> paramTypes = Stream.of(methodId.substring(className.length() + 1 + methodName.length() + 1, methodId.length() - 1).split(","))
                .filter(StringUtils::isNotBlank)
                .toList();
        return new MethodId(className, methodName, paramTypes);
    }

    public static MethodId from(MethodDeclaration method) {
        return new MethodId(
                AstUtils.getName(AstUtils.findDeclaringType(method)),
                method.getNameAsString(),
                method.getParameters().stream()
                        .map(Parameter::getType)
                        .map(AstUtils::getName)
                        .toList()
        );
    }

    public static MethodId from(ResolvedMethodDeclaration method) {
        return new MethodId(
                AstUtils.getName(method.declaringType()),
                method.getName(),
                IntStream.range(0, method.getNumberOfParams())
                        .mapToObj(method::getParam)
                        .map(ResolvedValueDeclaration::getType)
                        .map(AstUtils::getName)
                        .toList()
        );
    }

    public static MethodId from(Method method) {
        return new MethodId(
                method.getDeclaringClass().getName(),
                method.getName(),
                Stream.of(method.getParameterTypes()).map(Class::getName).toList()
        );
    }

    public static String toString(Method method) {
        return from(method).toString();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        return Objects.equals(toString(), object.toString());
    }

    public static Map<String, MethodDeclaration> group(List<MethodDeclaration> methods) {
        return methods.stream().collect(Collectors.toUnmodifiableMap(
                method -> MethodId.from(method).toString(),
                Function.identity()
        ));
    }

}
