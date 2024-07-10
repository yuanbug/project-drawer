package io.github.yuanbug.drawer.config;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import io.github.yuanbug.drawer.domain.CodeModule;
import io.github.yuanbug.drawer.domain.ast.AstIndex;
import io.github.yuanbug.drawer.domain.info.MethodCalling;
import io.github.yuanbug.drawer.parser.ParserConstants;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * @author yuanbug
 */
public interface AstParsingConfig {

    /**
     * @return 需要解析的代码模块
     */
    List<CodeModule> getModules();

    /**
     * @return 需要加载的jar包路径
     */
    default List<Path> getJarPaths() {
        return Collections.emptyList();
    }

    default boolean shouldParseDependency() {
        return false;
    }

    /**
     * @return 判断方法是否需要进行解析的谓语，两个参数分别为方法声明及其所在类型
     */
    default BiPredicate<ResolvedMethodDeclaration, ResolvedReferenceTypeDeclaration> getMethodFilter() {
        return ParserConstants.METHOD_FILTER_NOT_JDK_TYPE;
    }

    /**
     * @return 判断方法调用是否需要返回给前端的谓语
     */
    default BiPredicate<MethodCalling, AstIndex> getMethodCallingFilter() {
        return (methodCalling, context) -> true;
    }

    default boolean enableUnsolvedParser() {
        return true;
    }

    default Set<String> getLogVariableNames() {
        return Set.of("log", "LOGGER", "logger");
    }

    /**
     * @return 用于获取直接子类的方法
     */
    BiFunction<ClassOrInterfaceDeclaration, AstIndex, List<ClassOrInterfaceDeclaration>> getDirectlySubTypeParser();

}
