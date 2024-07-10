package io.github.yuanbug.drawer.example.config;

import com.github.javaparser.ast.body.TypeDeclaration;
import io.github.yuanbug.drawer.config.DefaultAstParsingConfig;
import io.github.yuanbug.drawer.domain.ast.AstIndex;
import io.github.yuanbug.drawer.domain.info.MethodCalling;
import io.github.yuanbug.drawer.domain.info.MethodInfo;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * @author yuanbug
 */
@Component
public class MyAppConfig extends DefaultAstParsingConfig implements WebViewConfig {

    public MyAppConfig() {
        super(Optional.ofNullable(System.getProperty("work-path")).orElseGet(() -> System.getProperty("user.dir")));
    }

    @Override
    public BiPredicate<MethodCalling, AstIndex> getMethodCallingFilter() {
        return (methodCalling, context) -> {
            MethodInfo callee = methodCalling.getCallee();
            if (null == callee) {
                return StringUtils.isNotBlank(methodCalling.getRecursiveAt());
            }
            TypeDeclaration<?> typeDeclaration = context.getTypeDeclarationByClassName(callee.getId().getClassName());
            if (null == typeDeclaration) {
                return false;
            }
            return !typeDeclaration.isAnnotationPresent(Data.class);
        };
    }

}
