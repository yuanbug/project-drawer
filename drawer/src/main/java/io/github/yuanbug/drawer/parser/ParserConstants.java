package io.github.yuanbug.drawer.parser;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import io.github.yuanbug.drawer.domain.ast.AstIndex;
import io.github.yuanbug.drawer.domain.info.MethodId;
import io.github.yuanbug.drawer.domain.view.graph.method.MethodListItemView;
import io.github.yuanbug.drawer.utils.AstUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author yuanbug
 */
public class ParserConstants {

    public static final List<String> PACKAGE_TO_IGNORE = List.of("java.", "javax.", "sun.");

    public static final BiPredicate<ResolvedMethodDeclaration, ResolvedReferenceTypeDeclaration> METHOD_FILTER_NOT_JDK_TYPE = (method, type) -> !isJdkType(type);

    public static boolean isJdkType(ResolvedTypeDeclaration typeDeclaration) {
        String packageName = typeDeclaration.getPackageName();
        return PACKAGE_TO_IGNORE.stream().anyMatch(packageName::startsWith);
    }

    /**
     * 返回所有直接子类（不包括枚举和record）
     * TODO 直接在AstIndexContext里建立索引
     */
    public static final BiFunction<ClassOrInterfaceDeclaration, AstIndex, List<ClassOrInterfaceDeclaration>> ALL_DIRECTLY_SUB_TYPE_PARSER =
            (type, context) -> context.getAllTypeDeclaration(ClassOrInterfaceDeclaration.class)
                    .filter(checkingType -> isDirectlySuperType(checkingType, type))
                    .toList();

    public static boolean isDirectlySuperType(ClassOrInterfaceDeclaration checkingType, ClassOrInterfaceDeclaration superType) {
        if (superType.isInterface()) {
            if (checkingType.isInterface()) {
                return checkingType.getExtendedTypes().stream().anyMatch(extendsType -> Objects.equals(AstUtils.getName(extendsType), AstUtils.getName(superType)));
            }
            return checkingType.getImplementedTypes().stream().anyMatch(extendsType -> Objects.equals(AstUtils.getName(extendsType), AstUtils.getName(superType)));
        }
        if (checkingType.isInterface()) {
            return false;
        }
        return checkingType.getExtendedTypes().stream().anyMatch(extendsType -> Objects.equals(AstUtils.getName(extendsType), AstUtils.getName(superType)));
    }

    /**
     * 当且仅当只有一个直接子类时，将其返回（不包括枚举和record）
     */
    public static final BiFunction<ClassOrInterfaceDeclaration, AstIndex, List<ClassOrInterfaceDeclaration>> SINGLE_DIRECTLY_SUB_TYPE_PARSER = (type, context) -> {
        List<ClassOrInterfaceDeclaration> subTypes = ALL_DIRECTLY_SUB_TYPE_PARSER.apply(type, context);
        if (subTypes.size() == 1) {
            return subTypes;
        }
        return Collections.emptyList();
    };

    private static MethodListItemView toMethodListItemView(String moduleName, MethodDeclaration method) {
        return MethodListItemView.builder()
                .methodId(MethodId.from(method).toString())
                .name("%s(%s)".formatted(method.getNameAsString(), method.getParameters().stream()
                        .map(parameter -> parameter.getTypeAsString() + " " + parameter.getNameAsString())
                        .collect(Collectors.joining(", "))
                ))
                .groupName(moduleName)
                .subGroupName(AstUtils.findDeclaringType(method).getNameAsString())
                .build();
    }

    public static final Function<AstIndex, List<MethodListItemView>> GET_ALL_PUBLIC_INSTANCE_METHODS = context ->
            context.groupAllTypeByModule().entrySet().stream()
                    .flatMap(kv -> {
                        String moduleName = kv.getKey();
                        return kv.getValue().stream()
                                .map(NodeWithMembers::getMethods)
                                .flatMap(Collection::stream)
                                .filter(method -> method.isPublic() && !method.isStatic())
                                .map(method -> toMethodListItemView(moduleName, method));
                    })
                    .toList();

    private static final Set<Class<? extends Annotation>> CONTROLLER_ANNOTATION = Set.of(
            RestController.class,
            Controller.class
    );

    private static final Predicate<ClassOrInterfaceDeclaration> IS_CONTROLLER = type -> CONTROLLER_ANNOTATION.stream().anyMatch(type::isAnnotationPresent);

    private static final Set<Class<? extends Annotation>> REQUEST_MAP_ANNOTATION = Set.of(
            RequestMapping.class,
            GetMapping.class,
            PostMapping.class,
            DeleteMapping.class,
            PutMapping.class,
            PatchMapping.class
    );

    private static final Predicate<MethodDeclaration> IS_REQUEST_MAP_METHOD = method -> REQUEST_MAP_ANNOTATION.stream().anyMatch(method::isAnnotationPresent);

    /**
     * 这里没有处理注解加在父类和注解类上的情况
     */
    public static final Function<AstIndex, List<MethodListItemView>> GET_CONTROLLER_REQUEST_MAP_METHODS = context ->
            context.groupAllTypeByModule().entrySet().stream()
                    .flatMap(kv -> {
                        String moduleName = kv.getKey();
                        return kv.getValue().stream()
                                .filter(ClassOrInterfaceDeclaration.class::isInstance)
                                .map(ClassOrInterfaceDeclaration.class::cast)
                                .filter(IS_CONTROLLER)
                                .map(NodeWithMembers::getMethods)
                                .flatMap(Collection::stream)
                                .filter(IS_REQUEST_MAP_METHOD)
                                .map(method -> toMethodListItemView(moduleName, method));
                    })
                    .toList();

}
