package io.github.yuanbug.drawer.domain.ast;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithExtends;
import com.github.javaparser.ast.nodeTypes.NodeWithImplements;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserClassDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserEnumDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserInterfaceDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import io.github.yuanbug.drawer.domain.CodeModule;
import io.github.yuanbug.drawer.utils.AstUtils;
import io.github.yuanbug.drawer.utils.MiscUtils;
import io.github.yuanbug.drawer.utils.PrimitiveTypeUtils;
import io.github.yuanbug.drawer.utils.ReflectUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

/**
 * @author yuanbug
 */
@Slf4j
@Getter
public class AstIndex {

    /**
     * 类限定名 -> 文件AST信息
     */
    private Map<String, JavaFileAstInfo> classNameToFileInfo = new HashMap<>(16);
    /**
     * Java文件 -> 所在文件AST信息
     */
    private Map<File, JavaFileAstInfo> fileToFileInfo = new HashMap<>(16);

    private final JavaParser javaParser;
    private final TypeSolver typeSolver;

    public AstIndex(JavaParser javaParser, TypeSolver typeSolver) {
        this.javaParser = javaParser;
        this.typeSolver = typeSolver;
    }

    public void seal() {
        this.classNameToFileInfo = Collections.unmodifiableMap(classNameToFileInfo);
        this.fileToFileInfo = Collections.unmodifiableMap(fileToFileInfo);
    }

    /**
     * 把文件添加到索引
     */
    public void addFileToIndex(File javaFile, CodeModule ofModule) {
        CompilationUnit ast = AstUtils.parseAst(javaFile, javaParser);
        if (null == ast) {
            return;
        }
        var typeDeclarations = ast.findAll(TypeDeclaration.class);
        if (typeDeclarations.isEmpty()) {
            return;
        }
        JavaFileAstInfo info = JavaFileAstInfo.builder()
                .file(javaFile)
                .ast(ast)
                .moduleName(ofModule.name)
                .build();
        for (TypeDeclaration<?> typeDeclaration : typeDeclarations) {
            classNameToFileInfo.put(AstUtils.getName(typeDeclaration), info);
        }
        fileToFileInfo.put(javaFile, info);
    }

    /**
     * 通过类名获取文件的信息
     */
    public JavaFileAstInfo getInfoByClassName(String qualifiedName) {
        return classNameToFileInfo.get(qualifiedName);
    }

    public TypeDeclaration<?> getTypeDeclarationByClassName(String typeName) {
        return Optional.ofNullable(classNameToFileInfo.get(typeName))
                .map(JavaFileAstInfo::getAst)
                .flatMap(ast -> ast.findFirst(TypeDeclaration.class, declaration -> typeName.equals(AstUtils.getName(declaration))))
                .orElse(null);
    }

    /**
     * 通过文件获取文件信息
     */
    public JavaFileAstInfo getInfoByFile(File javaFile) {
        return fileToFileInfo.get(javaFile);
    }

    public Optional<TypeDeclaration<?>> findTypeInIndex(String qualifiedName) {
        return Optional.ofNullable(classNameToFileInfo.get(qualifiedName))
                .map(JavaFileAstInfo::getAst)
                .flatMap(ast -> ast.findFirst(MiscUtils.castClass(TypeDeclaration.class), declaration -> AstUtils.getName(declaration).equals(qualifiedName)));
    }

    public Optional<TypeDeclaration<?>> findTypeInIndex(ResolvedReferenceTypeDeclaration declaration) {
        return findTypeInIndex(AstUtils.getName(declaration));
    }

    public Map<String, JavaTypeInfo> getAllParentTypes(TypeDeclaration<?> typeDeclaration) {
        if ("java.lang.Object".equals(AstUtils.getName(typeDeclaration))) {
            return Collections.emptyMap();
        }
        Map<String, JavaTypeInfo> result = new HashMap<>(8);
        result.put(JavaTypeInfo.JAVA_LANG_OBJECT.getName(), JavaTypeInfo.JAVA_LANG_OBJECT);
        List<ClassOrInterfaceType> extendedTypes = typeDeclaration instanceof NodeWithExtends<?> nodeWithExtends ? nodeWithExtends.getExtendedTypes() : Collections.emptyList();
        List<ClassOrInterfaceType> implementedTypes = typeDeclaration instanceof NodeWithImplements<?> nodeWithImplements ? nodeWithImplements.getImplementedTypes() : Collections.emptyList();
        List<ClassOrInterfaceType> parentTypes = Stream.concat(extendedTypes.stream(), implementedTypes.stream()).toList();
        for (ClassOrInterfaceType parentType : parentTypes) {
            result.putAll(getAllParentTypes(parentType));
        }
        return result;
    }

    public Map<String, JavaTypeInfo> getAllParentTypes(Type type) {
        // 借助符号解析器获取对应的实际类型
        ResolvedType resolvedType = AstUtils.tryResolve(type);
        if (null == resolvedType || !resolvedType.isReferenceType()) {
            return Collections.emptyMap();
        }
        Map<String, JavaTypeInfo> result = new HashMap<>(8);
        ResolvedReferenceType referenceType = resolvedType.asReferenceType();
        // 获取名称（可能带泛型参数）
        String name = referenceType.describe();
        // 获取类限定名
        String qualifiedName = AstUtils.getName(referenceType);
        // 从索引中取得父类型的声明，递归
        ClassOrInterfaceDeclaration superTypeDeclaration = this.findTypeInIndex(qualifiedName)
                .filter(ClassOrInterfaceDeclaration.class::isInstance)
                .map(ClassOrInterfaceDeclaration.class::cast)
                .orElse(null);
        Class<?> byteCode = AstUtils.forName(qualifiedName);
        if (null != superTypeDeclaration) {
            JavaTypeInfo self = JavaTypeInfo.builder()
                    .name(name)
                    .classQualifiedName(qualifiedName)
                    .sourceFile(classNameToFileInfo.get(qualifiedName))
                    .typeDeclaration(superTypeDeclaration)
                    .byteCode(byteCode)
                    .build();
            result.put(self.getName(), self);
            result.putAll(getAllParentTypes(superTypeDeclaration));
            return result;
        }
        // 索引中取不到AST，就尝试通过字节码进行处理
        if (null == byteCode) {
            return Collections.emptyMap();
        }
        result.put(name, JavaTypeInfo.builder()
                .name(name)
                .classQualifiedName(qualifiedName)
                .byteCode(byteCode)
                .build());
        getAllParentTypes(byteCode).stream()
                .map(JavaTypeInfo::byByteCode)
                .forEach(typeInfo -> result.put(typeInfo.getName(), typeInfo));
        return result;
    }

    public static Set<Class<?>> getAllParentTypes(Class<?> type) {
        if (null == type) {
            return Collections.emptySet();
        }
        Set<Class<?>> result = new HashSet<>(16);
        Optional.ofNullable(type.getSuperclass()).ifPresent(superClass -> {
            result.add(superClass);
            result.addAll(getAllParentTypes(superClass));
        });
        Class<?>[] interfaces = type.getInterfaces();
        for (Class<?> impl : interfaces) {
            result.add(impl);
            result.addAll(getAllParentTypes(impl));
        }
        return result;
    }

    public boolean isTypeOf(Type type, String typeName, boolean acceptTypeVariable) {
        ResolvedType resolvedType = AstUtils.tryResolve(type);
        if (null != resolvedType) {
            return isTypeOf(resolvedType, typeName, acceptTypeVariable);
        }
        if (type instanceof PrimitiveType primitiveType) {
            return String.valueOf(primitiveType).equals(typeName);
        }
        if (type instanceof ClassOrInterfaceType classOrInterfaceType) {
            if (AstUtils.getName(classOrInterfaceType).equals(typeName)) {
                return true;
            }
            if (typeName.endsWith("[]")) {
                return false;
            }
            return isAssignable(classOrInterfaceType, typeName);
        }
        if (type instanceof ArrayType arrayType) {
            if (!typeName.endsWith("[]")) {
                return false;
            }
            return isTypeOf(arrayType.getComponentType(), typeName.substring(0, typeName.length() - 2), acceptTypeVariable);
        }
        if (type instanceof TypeParameter typeParameter) {
            // TODO acceptTypeVariable
            return typeParameter.getNameAsString().equals(typeName);
        }
        throw new UnsupportedOperationException("未处理的类型判断 %s vs %s".formatted(type, typeName));
    }

    public boolean isTypeOf(ResolvedType type, String typeName, boolean acceptTypeVariable) {
        if (type.isTypeVariable()) {
            ResolvedTypeParameterDeclaration typeParameter = type.asTypeVariable().asTypeParameter();
            if (typeName.equals(typeParameter.getName())) {
                return true;
            }
            try {
                return isTypeOf(typeParameter.getUpperBound(), typeName, acceptTypeVariable);
            } catch (IllegalStateException ignored) {
                return acceptTypeVariable || typeParameter.getName().equals(typeName);
            } catch (Exception ignored) {
                return false;
            }
        }
        if (type.isPrimitive()) {
            return type.describe().equals(typeName);
        }
        if (type.isArray()) {
            if (!typeName.endsWith("[]")) {
                return false;
            }
            return isTypeOf(type.asArrayType().getComponentType(), typeName.substring(0, typeName.length() - 2), acceptTypeVariable);
        }
        if (type.isReferenceType()) {
            if (PrimitiveTypeUtils.isPrimitiveTypeName(typeName)) {
                return type.asReferenceType().getTypeDeclaration()
                        .filter(ReflectionClassDeclaration.class::isInstance)
                        .map(ReflectionClassDeclaration.class::cast)
                        .map(AstUtils::getReflectionClass)
                        .map(byteCode -> byteCode.isAssignableFrom(PrimitiveTypeUtils.getWrapperClass(PrimitiveTypeUtils.getPrimitiveTypeByName(typeName))))
                        .orElse(Boolean.FALSE);
            }
            return isAssignable(type.asReferenceType(), typeName);
        }
        throw new UnsupportedOperationException("未处理的类型判断 %s vs %s".formatted(type, typeName));
    }

    public Optional<ResolvedReferenceTypeDeclaration> trySolveReferenceTypeDeclaration(String typeName) {
        try {
            // JavaParser的内部类限定名没有用美元符号，用的是点
            return Optional.of(typeSolver.solveType(typeName.replace("$", ".")));
        } catch (Exception e) {
            return Optional.ofNullable(getTypeDeclarationByClassName(typeName))
                    .filter(ClassOrInterfaceDeclaration.class::isInstance)
                    .map(ClassOrInterfaceDeclaration.class::cast)
                    .map(AstUtils::tryResolve);
        }
    }

    public boolean isAssignable(ResolvedReferenceType expectedSuperType, String expectedSubTypeName) {
        if (AstUtils.getName(expectedSuperType).equals(expectedSubTypeName)) {
            return true;
        }
        ResolvedReferenceTypeDeclaration typeDeclaration = expectedSuperType.getTypeDeclaration().orElse(null);
        if (null == typeDeclaration) {
            return false;
        }
        if (typeDeclaration instanceof ReflectionClassDeclaration reflectionClassDeclaration) {
            return Optional.ofNullable(AstUtils.forName(expectedSubTypeName))
                    .map(expectedType -> AstUtils.getReflectionClass(reflectionClassDeclaration).isAssignableFrom(expectedType))
                    .orElse(Boolean.FALSE);
        }
        if (!AstUtils.isAssignableCheckSupported(typeDeclaration)) {
            return false;
        }
        return trySolveReferenceTypeDeclaration(expectedSubTypeName)
                .map(typeDeclaration::isAssignableBy)
                .orElse(Boolean.FALSE);
    }

    public boolean isAssignable(Type expectedSuperType, String expectedSubTypeName) {
        String expectedSuperTypeName = AstUtils.getName(expectedSuperType);
        if (expectedSuperTypeName.equals(AstUtils.getName(expectedSuperType))) {
            return true;
        }

        Class<?> superTypeByteCode = AstUtils.forName(expectedSuperTypeName);
        Class<?> subTypeByteCode = AstUtils.forName(expectedSubTypeName);
        if (null != superTypeByteCode && null != subTypeByteCode) {
            return superTypeByteCode.isAssignableFrom(subTypeByteCode);
        }

        var superTypeDeclaration = trySolveReferenceTypeDeclaration(expectedSuperTypeName).orElse(null);
        var subTypeDeclaration = trySolveReferenceTypeDeclaration(expectedSubTypeName).orElse(null);
        if (null != superTypeDeclaration && null != subTypeDeclaration) {
            try {
                return superTypeDeclaration.isAssignableBy(subTypeDeclaration);
            } catch (Exception ignored) {}
        }

        return getAllParentTypes(expectedSuperType).values().stream().anyMatch(superType -> superType.getClassQualifiedName().equals(expectedSubTypeName));
    }

    public boolean isAssignable(TypeDeclaration<?> expectedSuperType, String expectedSubTypeName) {
        if (null == expectedSuperType) {
            return false;
        }
        return isAssignable(AstUtils.tryResolveTypeDeclaration(expectedSuperType), expectedSubTypeName);
    }

    public boolean isAssignable(Class<?> expectedSuperType, String expectedSubTypeName) {
        return StringUtils.isBlank(ReflectUtils.findInInheritLink(expectedSuperType, false, expectedSubTypeName::equals, current -> List.of(current.getName())));
    }

    public boolean isAssignable(JavaTypeInfo expectedSuperType, String expectedSubTypeName) {
        if (null != expectedSuperType.getByteCode()) {
            return isAssignable(expectedSuperType.getByteCode(), expectedSubTypeName);
        }
        if (null != expectedSuperType.getTypeDeclaration()) {
            return isAssignable(expectedSuperType.getTypeDeclaration(), expectedSubTypeName);
        }
        return expectedSuperType.getClassQualifiedName().equals(expectedSubTypeName);
    }

    public boolean isAssignable(ResolvedTypeDeclaration expectedSuperType, String expectedSubTypeName) {
        if (null == expectedSuperType) {
            return false;
        }
        if (expectedSuperType instanceof ResolvedReferenceType referenceType) {
            return isAssignable(referenceType, expectedSubTypeName);
        }
        if (expectedSuperType instanceof JavaParserClassDeclaration || expectedSuperType instanceof JavaParserInterfaceDeclaration) {
            return isAssignable(AstUtils.getJavaParserWrappedNode(expectedSuperType, ClassOrInterfaceDeclaration.class), expectedSubTypeName);
        }
        if (expectedSuperType instanceof JavaParserEnumDeclaration) {
            return isAssignable(AstUtils.getJavaParserWrappedNode(expectedSuperType, EnumDeclaration.class), expectedSubTypeName);
        }
        // TODO 完善分支
        log.warn("未处理的类型 {}", expectedSuperType.getClass().getSimpleName());
        return AstUtils.getName(expectedSuperType).equals(expectedSubTypeName);
    }

    public Stream<TypeDeclaration<?>> getAllTypeDeclaration() {
        return getClassNameToFileInfo().values().stream()
                .map(JavaFileAstInfo::getAst)
                .map(ast -> ast.findAll(TypeDeclaration.class))
                .flatMap(Collection::stream)
                .map(declaration -> (TypeDeclaration<?>) declaration);
    }

    public <T extends TypeDeclaration<?>> Stream<T> getAllTypeDeclaration(Class<T> type) {
        return getClassNameToFileInfo().values().stream()
                .map(JavaFileAstInfo::getAst)
                .map(ast -> ast.findAll(TypeDeclaration.class))
                .flatMap(Collection::stream)
                .filter(type::isInstance)
                .map(type::cast);
    }

    public Map<String, List<TypeDeclaration<?>>> groupAllTypeByModule() {
        return getClassNameToFileInfo().values().stream()
                .distinct()
                .collect(groupingBy(
                        JavaFileAstInfo::getModuleName,
                        flatMapping(
                                file -> file.getAst().findAll(TypeDeclaration.class).stream().map(MiscUtils::castClass),
                                toList()
                        )
                ));
    }

    public String getModuleNameByTypeName(String typeName) {
        return Optional.ofNullable(classNameToFileInfo.get(typeName))
                .map(JavaFileAstInfo::getModuleName)
                .orElse(null);
    }

}
