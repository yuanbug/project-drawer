package io.github.yuanbug.drawer.parser.lombok;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AnnotationExpr;
import io.github.yuanbug.drawer.config.AstParsingConfig;
import io.github.yuanbug.drawer.domain.ast.JavaTypeInfo;
import io.github.yuanbug.drawer.domain.info.MethodId;
import io.github.yuanbug.drawer.domain.info.TypeLombokInfo;
import io.github.yuanbug.drawer.utils.AstUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author yuanbug
 */
@AllArgsConstructor
public class LombokParser {

    private final AstParsingConfig config;

    private static final Map<Class<? extends Annotation>, String> PREFIX = Map.of(
            Getter.class, "get",
            Setter.class, "set"
    );

    private static final Map<Class<? extends Annotation>, Integer> PARAM_NUM = Map.of(
            Getter.class, 0,
            Setter.class, 1
    );

    public TypeLombokInfo parse(TypeDeclaration<?> type) {
        return TypeLombokInfo.builder()
                .typeDeclaration(type)
                .getters(parseFieldAccessMethod(type, Getter.class))
                .setters(parseFieldAccessMethod(type, Setter.class))
                .build();
    }

    private List<MethodDeclaration> parseFieldAccessMethod(TypeDeclaration<?> type, Class<? extends Annotation> annotationType) {
        String prefix = Objects.requireNonNull(PREFIX.get(annotationType));
        Integer paramNum = Objects.requireNonNull(PARAM_NUM.get(annotationType));
        AnnotationExpr getterAnnotation = type.getAnnotationByClass(annotationType).orElse(null);
        AnnotationExpr dataAnnotation = type.getAnnotationByClass(Data.class).orElse(null);
        boolean typeScopeAnnotationExisted = null != getterAnnotation || null != dataAnnotation;
        // 只考虑大驼峰命名，不考虑类型等其它因素
        Map<String, VariableDeclarator> fields = type.getFields().stream()
                .filter(field -> typeScopeAnnotationExisted || field.isAnnotationPresent(annotationType))
                .map(FieldDeclaration::getVariables)
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableMap(variable -> StringUtils.capitalize(variable.getNameAsString()), Function.identity()));
        Set<String> methodExistedFields = type.getMethods().stream()
                .filter(method -> method.getParameters().size() == paramNum)
                .filter(method -> method.getNameAsString().startsWith(prefix))
                .map(method -> method.getNameAsString().substring(prefix.length()))
                .filter(fields::containsKey)
                .collect(Collectors.toUnmodifiableSet());
        return fields.entrySet().stream()
                .filter(kv -> !methodExistedFields.contains(kv.getKey()))
                .map(kv -> buildFieldAccessMethodDeclaration(type, annotationType, kv.getKey(), kv.getValue()))
                .toList();
    }

    private MethodDeclaration buildFieldAccessMethodDeclaration(TypeDeclaration<?> type,
                                                                Class<? extends Annotation> annotationType,
                                                                String capitalizedName,
                                                                VariableDeclarator variable) {
        MethodDeclaration declaration = new MethodDeclaration(
                NodeList.nodeList(Modifier.publicModifier()),
                variable.getType(),
                Objects.requireNonNull(PREFIX.get(annotationType)) + capitalizedName
        );
        declaration.setParentNode(type);
        if (Setter.class.equals(annotationType)) {
            declaration.setParameters(NodeList.nodeList(new Parameter(variable.getType(), variable.getNameAsString())));
        }
        return declaration;
    }

    public MethodDeclaration findMethod(TypeDeclaration<?> type, MethodId methodId) {
        return getIfParsingEnable(() -> parse(type).getMethods().get(methodId.toString()));
    }

    public MethodDeclaration findMethod(TypeDeclaration<?> type, String methodName, List<JavaTypeInfo> paramTypes) {
        return getIfParsingEnable(() -> {
            MethodId methodId = new MethodId(
                    AstUtils.getName(type),
                    methodName,
                    paramTypes.stream().map(JavaTypeInfo::getName).toList()
            );
            return parse(type).getMethods().get(methodId.toString());
        });
    }

    private <T> T getIfParsingEnable(Supplier<T> supplier) {
        if (!config.enableLombokParser()) {
            return null;
        }
        return supplier.get();
    }

}
