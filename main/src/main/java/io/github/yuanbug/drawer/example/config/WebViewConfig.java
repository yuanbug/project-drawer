package io.github.yuanbug.drawer.example.config;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import io.github.yuanbug.drawer.domain.ast.AstIndex;
import io.github.yuanbug.drawer.domain.info.MethodId;
import io.github.yuanbug.drawer.domain.info.MethodInfo;
import io.github.yuanbug.drawer.domain.view.graph.method.ArgumentView;
import io.github.yuanbug.drawer.domain.view.graph.method.MethodListItemView;
import io.github.yuanbug.drawer.domain.view.graph.method.MethodView;
import io.github.yuanbug.drawer.utils.AstUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.github.yuanbug.drawer.parser.ParserConstants.GET_ALL_PUBLIC_INSTANCE_METHODS;
import static io.github.yuanbug.drawer.utils.MiscUtils.getSimpleName;

/**
 * @author yuanbug
 */
public interface WebViewConfig {

    default MethodView mapMethodInfoToMethodView(MethodInfo methodInfo) {
        return Optional.ofNullable(methodInfo.getDeclaration())
                .map(this::toMethodView)
                .orElseGet(() -> this.toMethodView(methodInfo.getId()));
    }

    default MethodView toMethodView(MethodId methodId) {
        List<String> paramTypes = methodId.getParamTypes();
        List<ArgumentView> arguments = IntStream.range(0, paramTypes.size())
                .mapToObj(i -> ArgumentView.builder()
                        .name("arg" + i)
                        .type(paramTypes.get(i))
                        .build())
                .toList();
        return MethodView.builder()
                .id(methodId.toString())
                .name("%s#%s(%s)".formatted(
                        getSimpleName(methodId.getClassName()),
                        methodId.getMethodName(),
                        arguments.stream()
                                .map(arg -> getSimpleName(arg.getType()) + " " + arg.getName())
                                .collect(Collectors.joining(", "))
                ))
                .declaringClass(methodId.getClassName())
                .arguments(arguments)
                .build();
    }

    default MethodView toMethodView(MethodDeclaration method) {
        TypeDeclaration<?> declaringType = AstUtils.findDeclaringType(method);
        List<ArgumentView> arguments = method.getParameters().stream()
                .map(parameter -> ArgumentView.builder()
                        .name(parameter.getNameAsString())
                        .type(parameter.getTypeAsString())
                        .build())
                .toList();
        return MethodView.builder()
                .id(MethodId.from(method).toString())
                .name("%s#%s(%s)".formatted(
                        declaringType.getNameAsString(),
                        method.getNameAsString(),
                        arguments.stream()
                                .map(arg -> arg.getType() + " " + arg.getName())
                                .collect(Collectors.joining(", "))
                ))
                .declaringClass(declaringType.getNameAsString())
                .arguments(arguments)
                .build();
    }

    default Function<AstIndex, List<MethodListItemView>> getMethodListLoader() {
        return GET_ALL_PUBLIC_INSTANCE_METHODS;
    }

    default Comparator<MethodListItemView> getMethodListSorter() {
        return Comparator.comparing(MethodListItemView::getGroupName)
                .thenComparing(MethodListItemView::getSubGroupName)
                .thenComparing(MethodListItemView::getName);
    }

}
