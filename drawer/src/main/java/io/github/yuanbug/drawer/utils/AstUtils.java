package io.github.yuanbug.drawer.utils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserAnnotationDeclaration;
import com.github.javaparser.symbolsolver.javassistmodel.JavassistAnnotationDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionAnnotationDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionInterfaceDeclaration;
import io.github.yuanbug.drawer.domain.ast.JavaTypeInfo;
import javassist.CtClass;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author yuanbug
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AstUtils {

    public static CompilationUnit parseAst(File file, JavaParser javaParser) {
        if (!file.exists() || !file.getName().endsWith(".java")) {
            return null;
        }
        try {
            ParseResult<CompilationUnit> parseResult = javaParser.parse(file);
            if (!parseResult.isSuccessful()) {
                parseResult.getProblems().forEach(System.err::println);
                throw new IllegalStateException("解析AST失败 %s".formatted(file));
            }
            return parseResult.getResult().orElse(null);
        } catch (Exception e) {
            throw new IllegalStateException("解析AST出错 %s".formatted(file), e);
        }
    }

    @Nonnull
    public static <T extends Node> T findNodeInParent(Node node, @Nonnull Class<T> exceptedNodeType) {
        return Optional.ofNullable(tryfindNodeInParent(node, exceptedNodeType))
                .orElseThrow(() -> new IllegalArgumentException("在%s结点的父结点中不存在%s %s".formatted(node.getClass().getSimpleName(), exceptedNodeType.getSimpleName(), node)));
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T extends Node> T tryfindNodeInParent(Node node, @Nonnull Class<T> exceptedNodeType) {
        Optional<Node> current = node.getParentNode();
        while (current.isPresent()) {
            Node parent = current.get();
            if (exceptedNodeType.isInstance(parent)) {
                return (T) parent;
            }
            current = parent.getParentNode();
        }
        return null;
    }

    public static TypeDeclaration<?> findDeclaringType(MethodDeclaration method) {
        return (TypeDeclaration<?>) findNodeInParent(method, TypeDeclaration.class);
    }

    public static ResolvedType tryResolve(ClassOrInterfaceType type) {
        try {
            return type.resolve();
        } catch (Exception ignored) {
            return null;
        }
    }

    public static Class<?> forName(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        try {
            return Class.forName(name, false, AstUtils.class.getClassLoader());
        } catch (Exception ignored) {
            return null;
        }
    }

    public static JavaTypeInfo tryGetTypeByClassLoader(String name) {
        Class<?> byteCode = AstUtils.forName(name);
        if (null == byteCode) {
            return null;
        }
        return JavaTypeInfo.byByteCode(byteCode);
    }

    public static JavaTypeInfo tryParseTypeByImports(String name, CompilationUnit ast) {
        var imports = ast.getImports();
        for (var importation : imports) {
            String importName = importation.getNameAsString();
            if (importation.isAsterisk()) {
                JavaTypeInfo typeImported = tryGetTypeByClassLoader(importName + "." + name);
                if (null != typeImported) {
                    return typeImported;
                }
                continue;
            }
            if (name.equals(importName) || importName.endsWith("." + name)) {
                JavaTypeInfo typeImported = tryGetTypeByClassLoader(importName);
                if (null != typeImported) {
                    return typeImported;
                }
                return JavaTypeInfo.builder()
                        .name(name)
                        .classQualifiedName(importName)
                        .build();
            }
        }
        return null;
    }

    public static String getName(NodeWithSimpleName<?> type, Node parentNode) {
        if (null == parentNode) {
            return type.getNameAsString();
        }
        CompilationUnit ast = AstUtils.getAst(parentNode);
        if (parentNode instanceof TypeDeclaration<?> typeDeclaration) {
            return ast.getPackageDeclaration()
                    .map(NodeWithName::getNameAsString)
                    .map(packageName -> packageName + "." + type.getNameAsString())
                    .orElseGet(type::getNameAsString);
        }
        return Optional.ofNullable(tryParseTypeByImports(type.getNameAsString(), ast))
                .map(JavaTypeInfo::getClassQualifiedName)
                .orElseGet(type::getNameAsString);
    }

    public static String getName(Type type) {
        ResolvedType resolvedType = tryResolve(type);
        if (null != resolvedType) {
            return getName(resolvedType);
        }
        if (type instanceof NodeWithSimpleName<?> nodeWithSimpleName) {
            return getName(nodeWithSimpleName, type.getParentNode().orElse(null));
        }
        if (type instanceof ArrayType arrayType) {
            return getName(arrayType.getComponentType()) + "[]";
        }
        if (type instanceof PrimitiveType primitiveType) {
            return primitiveType.asString();
        }
        if (type instanceof VoidType) {
            return "void";
        }
        if (type instanceof VarType) {
            return "var";
        }
        if (type instanceof UnionType unionType) {
            return unionType.getElements().stream().map(AstUtils::getName).collect(Collectors.joining("|"));
        }
        if (type instanceof IntersectionType intersectionType) {
            return intersectionType.getElements().stream().map(AstUtils::getName).collect(Collectors.joining("&"));
        }
        if (type instanceof WildcardType wildcardType) {
            var extendedType = wildcardType.getExtendedType();
            if (extendedType.isPresent()) {
                return extendedType.map(AstUtils::getName).map(name -> "? extends " + name).orElseThrow();
            }
            var superType = wildcardType.getSuperType();
            if (superType.isPresent()) {
                return superType.map(AstUtils::getName).map(name -> "? super " + name).orElseThrow();
            }
            return "?";
        }
        throw new UnsupportedOperationException("未处理的类型 " + type);
    }

    public static String getName(ResolvedTypeDeclaration typeDeclaration) {
        if (typeDeclaration instanceof ReflectionClassDeclaration || typeDeclaration instanceof ReflectionInterfaceDeclaration) {
            return AstUtils.getReflectionClass(typeDeclaration).getName();
        }
        String qualifiedName = typeDeclaration.getQualifiedName();
        if (isInnerType(typeDeclaration)) {
            int lastDocPosition = qualifiedName.lastIndexOf(".");
            if (lastDocPosition > 0) {
                return qualifiedName.substring(0, lastDocPosition) + "$" + qualifiedName.substring(lastDocPosition + 1);
            }
        }
        return qualifiedName;
    }

    /**
     * JavaParser返回的内部类QualifiedName没有用$符号，需要特别处理
     */
    public static String getName(ResolvedReferenceType referenceType) {
        ResolvedReferenceTypeDeclaration typeDeclaration = referenceType.getTypeDeclaration().orElse(null);
        if (null == typeDeclaration) {
            return referenceType.getQualifiedName();
        }
        return getName(typeDeclaration);
    }

    public static boolean isInnerType(ResolvedTypeDeclaration declaration) {
        return checkWhetherInnerTypeForJavaParserType(declaration) || checkWhetherInnerTypeForJavassistType(declaration);
    }

    private static boolean checkWhetherInnerTypeForJavaParserType(ResolvedTypeDeclaration declaration) {
        try {
            Node node = getJavaParserWrappedNode(declaration, Node.class);
            if (null != node && null != AstUtils.tryfindNodeInParent(node, ClassOrInterfaceDeclaration.class)) {
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static boolean checkWhetherInnerTypeForJavassistType(ResolvedTypeDeclaration declaration) {
        try {
            CtClass ctClass = getJavassistCtClass(declaration);
            return null != ctClass && ctClass.getClassFile().getInnerAccessFlags() != -1;
        } catch (Exception ignored) {}
        return false;
    }

    public static <T extends Node> T getJavaParserWrappedNode(ResolvedTypeDeclaration declaration, Class<T> nodeType) {
        if (!declaration.getClass().getSimpleName().startsWith("JavaParser")) {
            return null;
        }
        try {
            Field field = declaration.getClass().getDeclaredField("wrappedNode");
            field.setAccessible(true);
            Object wrappedNode = field.get(declaration);
            if (wrappedNode instanceof Node node) {
                return nodeType.cast(node);
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static CtClass getJavassistCtClass(ResolvedTypeDeclaration declaration) {
        if (!declaration.getClass().getSimpleName().startsWith("Javassist")) {
            return null;
        }
        try {
            Field field = declaration.getClass().getDeclaredField("ctClass");
            field.setAccessible(true);
            Object value = field.get(declaration);
            if (value instanceof CtClass ctClass) {
                return ctClass;
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static String getName(ResolvedType type) {
        if (type.isReferenceType()) {
            return getName(type.asReferenceType());
        }
        if (type.isArray()) {
            return getName(type.asArrayType().getComponentType()) + "[]";
        }
        if (type.isPrimitive()) {
            return type.asPrimitive().describe();
        }
        if (type.isVoid()) {
            return "void";
        }
        if (type.isTypeVariable()) {
            return type.asTypeVariable().asTypeParameter().getName();
        }
        if (type.isUnionType()) {
            return type.asUnionType().getElements().stream().map(AstUtils::getName).collect(Collectors.joining("|"));
        }
        return type.describe();
    }

    public static boolean isNested(TypeDeclaration<?> inner, TypeDeclaration<?> parent) {
        return parent.findFirst(ClassOrInterfaceDeclaration.class, inner::equals).isPresent();
    }

    public static <T> T tryResolve(Resolvable<T> resolvable) {
        if (null == resolvable) {
            return null;
        }
        try {
            return resolvable.resolve();
        } catch (Exception ignored) {
            return null;
        }
    }

    public static CompilationUnit getAst(Node node) {
        return node instanceof CompilationUnit compilationUnit
                ? compilationUnit
                : AstUtils.findNodeInParent(node, CompilationUnit.class);
    }

    public static ResolvedType tryResolveExpression(Expression expression) {
        try {
            return expression.calculateResolvedType();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String getFullQualifiedNameByPackage(TypeDeclaration<?> typeDeclaration) {
        return AstUtils.getAst(typeDeclaration).getPackageDeclaration()
                .map(NodeWithName::getNameAsString)
                .map(packageName -> packageName + "." + typeDeclaration.getNameAsString())
                .orElseGet(typeDeclaration::getNameAsString);
    }

    public static String getName(TypeDeclaration<?> typeDeclaration) {
        try {
            ResolvedReferenceTypeDeclaration resolved = typeDeclaration.resolve();
            if (null != resolved) {
                return getName(resolved);
            }
        } catch (Exception ignored) {}
        return typeDeclaration.getFullyQualifiedName().orElseGet(() -> getFullQualifiedNameByPackage(typeDeclaration));
    }

    @SneakyThrows
    public static Class<?> getReflectionClass(ResolvedTypeDeclaration declaration) {
        if (!declaration.getClass().getSimpleName().startsWith("Reflection")) {
            throw new UnsupportedOperationException();
        }
        Field field = declaration.getClass().getDeclaredField("clazz");
        field.setAccessible(true);
        return (Class<?>) field.get(declaration);
    }

    public static ResolvedTypeDeclaration tryResolveTypeDeclaration(TypeDeclaration<?> type) {
        try {
            if (type instanceof ClassOrInterfaceDeclaration declaration) {
                return declaration.resolve();
            }
            throw new IllegalStateException("未处理的类型 %s".formatted(type.getClass().getSimpleName()));
        } catch (Exception ignored) {}
        return null;
    }

    public static ResolvedType tryGetReturnType(ResolvedMethodDeclaration resolvedMethodDeclaration) {
        try {
            return resolvedMethodDeclaration.getReturnType();
        } catch (Exception ignored) {
            return null;
        }
    }

    public static boolean isAssignableCheckSupported(ResolvedReferenceTypeDeclaration typeDeclaration) {
        if (typeDeclaration instanceof JavaParserAnnotationDeclaration) {
            return false;
        }
        if (typeDeclaration instanceof JavassistAnnotationDeclaration) {
            return false;
        }
        return !(typeDeclaration instanceof ReflectionAnnotationDeclaration);
    }

}
