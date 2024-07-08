package io.github.yuanbug.drawer.domain.ast;

import com.github.javaparser.ast.body.TypeDeclaration;
import io.github.yuanbug.drawer.utils.AstUtils;
import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.Optional;

/**
 * @author yuanbug
 */
@Getter
public class JavaTypeInfo {

    public static final JavaTypeInfo JAVA_LANG_OBJECT = byByteCode(Object.class);

    private final String name;

    private final String classQualifiedName;

    @Nullable
    private final JavaFileAstInfo sourceFile;

    @Nullable
    private final TypeDeclaration<?> typeDeclaration;

    @Nullable
    private final Class<?> byteCode;

    private final boolean array;

    private final JavaTypeInfo componentType;

    @Builder
    public JavaTypeInfo(String name,
                        String classQualifiedName,
                        @Nullable JavaFileAstInfo sourceFile,
                        @Nullable TypeDeclaration<?> typeDeclaration,
                        @Nullable Class<?> byteCode,
                        boolean array,
                        @Nullable JavaTypeInfo componentType) {
        this.name = name;
        this.classQualifiedName = classQualifiedName;
        this.sourceFile = sourceFile;
        this.typeDeclaration = typeDeclaration;
        this.byteCode = byteCode;
        this.array = array;
        this.componentType = componentType;
    }

    public static JavaTypeInfo byByteCode(Class<?> byteCode) {
        return JavaTypeInfo.builder()
                .name(byteCode.getName())
                .classQualifiedName(byteCode.getName())
                .byteCode(byteCode)
                .array(byteCode.isArray())
                .componentType(byteCode.isArray() ? JavaTypeInfo.byByteCode(byteCode.getComponentType()) : null)
                .build();
    }

    public static JavaTypeInfo byDeclaration(TypeDeclaration<?> declaration, JavaFileAstInfo sourceFile) {
        return JavaTypeInfo.builder()
                .name(declaration.getNameAsString())
                .classQualifiedName(declaration.getFullyQualifiedName().orElseGet(declaration::getNameAsString))
                .sourceFile(sourceFile)
                .byteCode(declaration.getFullyQualifiedName().map(AstUtils::forName).orElse(null))
                .typeDeclaration(declaration)
                .build();
    }

    public static JavaTypeInfo array(JavaTypeInfo componentType) {
        return JavaTypeInfo.builder()
                .name(componentType.getName() + "[]")
                .classQualifiedName("")
                .byteCode(Optional.ofNullable(componentType.getByteCode())
                        .map(type -> Array.newInstance(type, 0).getClass())
                        .orElse(null))
                .array(true)
                .componentType(componentType)
                .build();
    }

}
