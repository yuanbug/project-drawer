package io.github.yuanbug.drawer.parser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import com.github.javaparser.symbolsolver.javassistmodel.JavassistMethodDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import io.github.yuanbug.drawer.config.AstParsingConfig;
import io.github.yuanbug.drawer.domain.ast.AstIndexContext;
import io.github.yuanbug.drawer.domain.ast.JavaTypeInfo;
import io.github.yuanbug.drawer.domain.info.MethodCalling;
import io.github.yuanbug.drawer.domain.info.MethodId;
import io.github.yuanbug.drawer.utils.AstUtils;
import io.github.yuanbug.drawer.utils.PrimitiveTypeUtils;
import io.github.yuanbug.drawer.utils.ReflectUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.yuanbug.drawer.utils.AstUtils.tryGetTypeByClassLoader;
import static io.github.yuanbug.drawer.utils.AstUtils.tryParseTypeByImports;
import static io.github.yuanbug.drawer.utils.PrimitiveTypeUtils.*;

/**
 * 当 resolve 得不到结果时，大概率是对库方法的调用，能处理则处理，不能就放弃
 * （大部分是老代码，待整理重构）
 *
 * @author yuanbug
 */
@Slf4j
@AllArgsConstructor
@SuppressWarnings({"squid:S3776", "squid:S3516"})
public class UnsolvedParser {
    private final AstIndexContext context;
    private final AstParsingConfig config;

    private static class NameParsingContext {

        private final Set<String> staticImportSearchLink = new LinkedHashSet<>();

    }

    public MethodCalling buildUnsolveMethodCalling(MethodCallExpr expr, MethodDeclaration callerMethod) {
        if (!config.enableUnsolvedParser()) {
            return null;
        }
        // TODO 这里没必要拿到准确类型，null之类的也应该支持
        List<JavaTypeInfo> paramTypes = tryFindParamTypes(expr);
        if (paramTypes.size() != expr.getArguments().size()) {
            return null;
        }
        List<String> paramTypeNames = paramTypes.stream().map(JavaTypeInfo::getClassQualifiedName).toList();
        String calleeTypeName = parseCalleeType(expr, callerMethod, paramTypes);
        if (StringUtils.isBlank(calleeTypeName)) {
            return null;
        }
        return MethodCalling.library(
                new MethodId(calleeTypeName, expr.getNameAsString(), paramTypeNames),
                null
        );
    }

    private String parseCalleeType(MethodCallExpr expr, MethodDeclaration callerMethod, List<JavaTypeInfo> paramTypes) {
        TypeDeclaration<?> callerType = AstUtils.findDeclaringType(callerMethod);
        String byScope = expr.getScope()
                .map(scope -> parseTypeByExpr(scope, callerType))
                .map(JavaTypeInfo::getClassQualifiedName)
                .orElse(null);
        if (StringUtils.isNotBlank(byScope)) {
            return byScope;
        }
        String methodName = expr.getNameAsString();
        // this调用
        if (isMethodExistInThisType(callerType, methodName, paramTypes)) {
            return AstUtils.getName(callerType);
        }
        // super调用
        List<JavaTypeInfo> superTypes = new ArrayList<>(context.getAllParentTypes(callerType).values());
        for (JavaTypeInfo superType : superTypes) {
            if (isMethodExistInThisType(superType, methodName, paramTypes)) {
                return superType.getClassQualifiedName();
            }
        }
        // 静态调用
        return null;
    }

    private Method findMethodInInheritLink(Class<?> byteCode, String methodName, List<JavaTypeInfo> paramTypes, boolean superOnly) {
        return ReflectUtils.findInInheritLink(
                byteCode,
                superOnly,
                method -> {
                    if (!method.getName().equals(methodName)) {
                        return false;
                    }
                    Class<?>[] checkingParameterTypes = method.getParameterTypes();
                    if (checkingParameterTypes.length != paramTypes.size()) {
                        return false;
                    }
                    for (int i = 0; i < checkingParameterTypes.length; i++) {
                        if (!context.isAssignable(checkingParameterTypes[i], paramTypes.get(i).getClassQualifiedName())) {
                            return false;
                        }
                    }
                    return true;
                },
                current -> Arrays.asList(current.getDeclaredMethods())
        );
    }

    private boolean isMethodExistInThisType(JavaTypeInfo type, String methodName, List<JavaTypeInfo> paramTypes) {
        Class<?> byteCode = type.getByteCode();
        if (null != byteCode) {
            return null != findMethodInInheritLink(byteCode, methodName, paramTypes, false);
        }
        TypeDeclaration<?> declaration = type.getTypeDeclaration();
        if (null == declaration) {
            return false;
        }
        return isMethodExistInThisType(declaration, methodName, paramTypes);
    }

    private boolean isMethodExistInThisType(TypeDeclaration<?> type, String methodName, List<JavaTypeInfo> paramTypes) {
        return type.findFirst(MethodDeclaration.class, method -> {
            if (!method.getNameAsString().equals(methodName)) {
                return false;
            }
            int paramNum = method.getParameters().size();
            if (paramTypes.size() != paramNum) {
                return false;
            }
            for (int i = 0; i < paramNum; i++) {
                Type checkingType = method.getParameter(i).getType();
                JavaTypeInfo expectedType = paramTypes.get(i);
                if (!context.isAssignable(checkingType, expectedType.getClassQualifiedName())) {
                    return false;
                }
            }
            return true;
        }).isPresent();
    }

    private JavaTypeInfo parseTypeByExpr(Expression expression, Node contextNode) {
        ResolvedType resolvedReturnType = AstUtils.tryResolveExpression(expression);
        if (null != resolvedReturnType) {
            if (resolvedReturnType.isTypeVariable()) {
                return getUpperBound(resolvedReturnType.asTypeVariable().asTypeParameter(), contextNode);
            }
            if (resolvedReturnType.isNull()) {
                return null;
            }
            return parseTypeByName(AstUtils.getName(resolvedReturnType), contextNode);
        }
        if (expression instanceof NameExpr nameExpr) {
            return findTypeByNameExpr(nameExpr, contextNode, new NameParsingContext()).orElse(null);
        }
        if (expression instanceof ArrayAccessExpr arrayAccessExpr) {
            return parseTypeByExpr(arrayAccessExpr.getName(), contextNode);
        }
        if (expression instanceof ArrayCreationExpr arrayCreationExpr) {
            return parseTypeByType(arrayCreationExpr.getElementType(), contextNode, new NameParsingContext());
        }
        if (expression instanceof LiteralExpr literalExpr) {
            return parseLiteralExpr(literalExpr);
        }
        if (expression instanceof FieldAccessExpr fieldAccessExpr) {
            return parseFieldAccessExpr(fieldAccessExpr, contextNode);
        }
        if (expression instanceof ObjectCreationExpr objectCreationExpr) {
            return parseTypeByName(objectCreationExpr.getType().getNameAsString(), contextNode);
        }
        if (expression instanceof MethodCallExpr methodCallExpr) {
            String returnTypeName = getReturnTypeName(AstUtils.tryResolve(methodCallExpr));
            if (StringUtils.isNotBlank(returnTypeName)) {
                return parseTypeByName(returnTypeName, contextNode);
            }
            return Optional.ofNullable(parseMethodByClassLoader(methodCallExpr, contextNode))
                    .map(Method::getReturnType)
                    .map(JavaTypeInfo::byByteCode)
                    .orElse(null);
        }
        if (expression instanceof BinaryExpr binaryExpr) {
            return parseBinaryExprByClassLoader(binaryExpr, contextNode);
        }
        if (expression instanceof ConditionalExpr conditionalExpr) {
            return parseConditionalExprByClassLoader(conditionalExpr, contextNode);
        }
        if (expression instanceof EnclosedExpr enclosedExpr) {
            return parseTypeByExpr(enclosedExpr.getInner(), contextNode);
        }
        // TODO 方法引用和lambda表达式无法直接拿到准确类型，需要在上下文定位，并且涉及方法重载问题，暂不处理
        if (expression instanceof MethodReferenceExpr || expression instanceof LambdaExpr) {
            return null;
        }
        if (expression instanceof ClassExpr) {
            return JavaTypeInfo.byByteCode(Class.class);
        }
        if (expression instanceof CastExpr castExpr) {
            return parseTypeByType(castExpr.getType(), contextNode, new NameParsingContext());
        }
        if (expression instanceof ThisExpr) {
            return parseThisExpr(contextNode);
        }
        if (expression instanceof SuperExpr superExpr) {
            return parseSuperType(parseThisExpr(contextNode), superExpr, contextNode);
        }
        if (expression instanceof UnaryExpr unaryExpr) {
            if (UnaryExpr.Operator.LOGICAL_COMPLEMENT.equals(unaryExpr.getOperator())) {
                return JavaTypeInfo.byByteCode(boolean.class);
            }
            return parseTypeByExpr(unaryExpr.getExpression(), contextNode);
        }
        log.warn("未能解析表达式值({}): {}", expression.getClass().getSimpleName(), expression);
        return null;
    }

    private String getReturnTypeName(ResolvedMethodDeclaration methodDeclaration) {
        ResolvedType returnType = AstUtils.tryGetReturnType(methodDeclaration);
        if (null != returnType) {
            return AstUtils.getName(returnType);
        }
        if (methodDeclaration instanceof JavaParserMethodDeclaration declaration) {
            return AstUtils.getName(declaration.getWrappedNode().getType());
        }
        if (methodDeclaration instanceof JavassistMethodDeclaration declaration) {
            return AstUtils.getName(declaration.getReturnType());
        }
        return null;
    }

    private JavaTypeInfo parseSuperType(JavaTypeInfo thisType, SuperExpr superExpr, Node contextNode) {
        if (null == thisType) {
            return null;
        }
        Node parentNode = superExpr.getParentNode().orElse(null);
        if (parentNode instanceof MethodCallExpr methodCallExpr) {
            return parseSuperType(thisType, methodCallExpr, contextNode);
        }
        return null;
    }

    private JavaTypeInfo parseSuperType(JavaTypeInfo thisType, MethodCallExpr expr, Node contextNode) {
        List<JavaTypeInfo> paramTypes = tryFindParamTypes(expr);
        String methodName = expr.getNameAsString();
        if (paramTypes.size() != expr.getArguments().size()) {
            return null;
        }
        Class<?> byteCode = thisType.getByteCode();
        if (null != byteCode) {
            Method methodInSuper = findMethodInInheritLink(byteCode, methodName, paramTypes, true);
            if (null != methodInSuper) {
                return JavaTypeInfo.byByteCode(methodInSuper.getDeclaringClass());
            }
        }
        return Optional.ofNullable(thisType.getTypeDeclaration())
                .filter(ClassOrInterfaceDeclaration.class::isInstance)
                .map(ClassOrInterfaceDeclaration.class::cast)
                .map(declaration -> findInInheritLink(
                        declaration,
                        true,
                        method -> {
                            if (!methodName.equals(method.getNameAsString())) {
                                return false;
                            }
                            if (paramTypes.size() != method.getParameters().size()) {
                                return false;
                            }
                            for (int i = 0; i < paramTypes.size(); i++) {
                                if (!context.isAssignable(paramTypes.get(i), AstUtils.getName(method.getParameter(i).getType()))) {
                                    return false;
                                }
                            }
                            return true;
                        },
                        NodeWithMembers::getMethods
                ))
                .map(AstUtils::findDeclaringType)
                .map(declaration -> JavaTypeInfo.byDeclaration(declaration, context.getInfoByClassName(AstUtils.getName(declaration))))
                .orElse(null);
    }

    private JavaTypeInfo getUpperBound(ResolvedTypeParameterDeclaration typeParameter, Node contextNode) {
        try {
            return parseTypeByName(AstUtils.getName(typeParameter.getUpperBound()), contextNode);
        } catch (UnsolvedSymbolException exception) {
            return parseTypeByName(exception.getName(), contextNode);
        } catch (Exception ignored) {
            return null;
        }
    }

    private JavaTypeInfo parseFieldAccessExpr(FieldAccessExpr fieldAccessExpr, Node contextNode) {
        Expression scope = fieldAccessExpr.getScope();
        String fieldName = fieldAccessExpr.getNameAsString();
        if (null == scope) {
            return parseFieldAccessExpr(getClassOrInterfaceDeclaration(contextNode), fieldName);
        }
        if (scope instanceof NameExpr nameExpr) {
            JavaTypeInfo fieldDeclaringType = parseTypeByExpr(nameExpr, contextNode);
            if (null == fieldDeclaringType) {
                return null;
            }
            if (null != fieldDeclaringType.getByteCode()) {
                return parseFieldAccessExpr(fieldDeclaringType.getByteCode(), fieldName);
            }
            return parseFieldAccessExpr(fieldDeclaringType.getTypeDeclaration(), fieldName);
        }
        return null;
    }

    private Method parseMethodByClassLoader(MethodCallExpr callExpr, Node contextNode) {
        Expression scope = callExpr.getScope().orElse(null);
        String methodName = callExpr.getNameAsString();
        CompilationUnit ast = AstUtils.getAst(contextNode);
        if (scope instanceof NameExpr nameScope) {
            if (config.getLogVariableNames().contains(nameScope.getNameAsString())) {
                return null;
            }
            // 尝试按实例调用解析
            Method byInstance = Optional.ofNullable(parseTypeByExpr(nameScope, contextNode))
                    .map(JavaTypeInfo::getByteCode)
                    .map(byteCode -> parseMethod(byteCode, callExpr, contextNode))
                    .orElse(null);
            if (null != byInstance) {
                return byInstance;
            }
            // 按类名静态调用解析
            return parseMethod(nameScope.toString(), callExpr, contextNode);
        }
        if (scope instanceof FieldAccessExpr fieldAccessExpr) {
            if (config.getLogVariableNames().contains(fieldAccessExpr.getNameAsString())) {
                return null;
            }
            return Optional.ofNullable(parseFieldAccessExpr(fieldAccessExpr, contextNode))
                    .map(JavaTypeInfo::getByteCode)
                    .map(type -> parseMethodInInheritLink(type, false, callExpr, contextNode))
                    .orElse(null);
        }
        Class<?> thisClass = Optional.ofNullable(getClassOrInterfaceDeclaration(contextNode))
                .flatMap(ClassOrInterfaceDeclaration::getFullyQualifiedName)
                .map(name -> parseTypeByName(name, contextNode))
                .map(JavaTypeInfo::getByteCode)
                .orElse(null);
        if (scope instanceof ThisExpr || scope instanceof SuperExpr) {
            return parseMethodInInheritLink(thisClass, scope instanceof SuperExpr, callExpr, contextNode);
        }
        // 不带点号的调用
        if (null == scope) {
            // this/super
            Method byInheritLink = parseMethodInInheritLink(thisClass, false, callExpr, contextNode);
            if (null != byInheritLink) {
                return byInheritLink;
            }
            // 静态导入
            return ast.getImports().stream()
                    .filter(ImportDeclaration::isStatic)
                    .map(declaration -> {
                        String name = declaration.getNameAsString();
                        if (declaration.isAsterisk()) {
                            return parseMethod(name, callExpr, contextNode);
                        }
                        if (!name.matches("\\.?" + methodName + "$")) {
                            return null;
                        }
                        return parseMethod(name.replaceAll("\\.?" + methodName + "$", ""), callExpr, contextNode);
                    })
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
        if (scope instanceof MethodCallExpr methodCallExpr) {
            Method method = parseMethodByClassLoader(methodCallExpr, callExpr);
            if (null == method) {
                return null;
            }
            return tryMatchMethod(contextNode, method.getReturnType(), methodName, callExpr.getArguments());
        }
        return null;
    }

    private JavaTypeInfo parseFieldAccessExpr(TypeDeclaration<?> declaration, String fieldName) {
        // TODO
        return null;
    }

    private JavaTypeInfo parseFieldAccessExpr(Class<?> byteCode, String fieldName) {
        return Optional.ofNullable(ReflectUtils.getFieldByName(fieldName, byteCode))
                .map(Field::getType)
                .map(JavaTypeInfo::byByteCode)
                .orElse(null);
    }

    private JavaTypeInfo parseLiteralExpr(LiteralExpr literalExpr) {
        if (literalExpr instanceof NullLiteralExpr) {
            return null;
        }
        return JavaTypeInfo.byByteCode(switch (literalExpr.getClass().getSimpleName()) {
            case "BooleanLiteralExpr" -> getPrimitiveClass(Boolean.class);
            case "CharLiteralExpr" -> getPrimitiveClass(Character.class);
            case "IntegerLiteralExpr" -> getPrimitiveClass(Integer.class);
            case "DoubleLiteralExpr" -> {
                String code = literalExpr.toString()
                        .replaceAll("^\\s*", "")
                        .replaceAll(";\\s*$", "");
                // 这里需要考虑十六进制
                if (!code.startsWith("0x") && !code.startsWith("0X") && (code.endsWith("f") || code.endsWith("F"))) {
                    yield getPrimitiveClass(Float.class);
                }
                yield getPrimitiveClass(Double.class);
            }
            case "LongLiteralExpr" -> getPrimitiveClass(Long.class);
            case "TextBlockLiteralExpr", "StringLiteralExpr" -> String.class;
            default -> throw new IllegalStateException("未处理的字面量表达式类型 " + literalExpr);
        });
    }

    private ClassOrInterfaceDeclaration getClassOrInterfaceDeclaration(Node contextNode) {
        return contextNode instanceof ClassOrInterfaceDeclaration declaration
                ? declaration
                : AstUtils.tryfindNodeInParent(contextNode, ClassOrInterfaceDeclaration.class);
    }

    private JavaTypeInfo parseThisExpr(Node contextNode) {
        ClassOrInterfaceDeclaration typeDeclaration = getClassOrInterfaceDeclaration(contextNode);
        if (null == typeDeclaration) {
            return null;
        }
        return JavaTypeInfo.byDeclaration(typeDeclaration, context.getInfoByClassName(AstUtils.getName(typeDeclaration)));
    }

    private Optional<JavaTypeInfo> findTypeByNameExpr(NameExpr nameExpr, Node contextNode, NameParsingContext nameParsingContext) {
        String name = nameExpr.getNameAsString();
        // 优先找变量/参数
        Node current = nameExpr.getParentNode().orElse(null);
        while (null != current) {
            if (current instanceof MethodDeclaration methodDeclaration) {
                String parameterTypeName = methodDeclaration.findFirst(Parameter.class, parameter -> name.equals(parameter.getNameAsString()))
                        .map(Parameter::getType)
                        .filter(ClassOrInterfaceType.class::isInstance)
                        .map(ClassOrInterfaceType.class::cast)
                        .map(NodeWithSimpleName::getNameAsString)
                        .orElse(null);
                if (null != parameterTypeName) {
                    return Optional.ofNullable(parseTypeByName(parameterTypeName, contextNode));
                }
            } else if (current instanceof LambdaExpr lambdaExpr) {
                Parameter lambdaParameter = lambdaExpr.getParameters().stream()
                        .filter(parameter -> name.equals(parameter.getNameAsString()))
                        .findFirst()
                        .orElse(null);
                if (null != lambdaParameter) {
                    if (lambdaParameter.getType() instanceof ClassOrInterfaceType lambdaParamType) {
                        return Optional.ofNullable(parseTypeByType(lambdaParamType, contextNode, nameParsingContext));
                    }
                    // TODO 暂不处理无显式类型声明的lambda表达式参数
                    return Optional.empty();
                }
            } else {
                VariableDeclarator variableDeclarator = current.findFirst(VariableDeclarator.class, variable -> Objects.equals(variable.getNameAsString(), name)).orElse(null);
                if (null != variableDeclarator) {
                    return Optional.ofNullable(parseTypeByType(variableDeclarator.getType(), contextNode, nameParsingContext));
                }
            }
            current = current.getParentNode().orElse(null);
        }
        // 可能是个类名
        JavaTypeInfo typeByName = parseTypeByName(name, contextNode);
        if (null != typeByName) {
            return Optional.of(typeByName);
        }
        // 可能是静态导入的变量或枚举，需要在另一个类里面找
        JavaTypeInfo fieldTypeFromStaticImport = findFieldTypeInStaticImports(name, contextNode, nameParsingContext);
        if (null != fieldTypeFromStaticImport) {
            return Optional.of(fieldTypeFromStaticImport);
        }
        // 找父类字段
        return Optional.ofNullable(getClassOrInterfaceDeclaration(contextNode))
                .map(type -> findFieldByName(type, name))
                .map(field -> {
                    var variables = field.getVariables();
                    for (VariableDeclarator variable : variables) {
                        if (name.equals(variable.getNameAsString())) {
                            return variable.getType();
                        }
                    }
                    return field.getElementType();
                })
                .map(type -> parseTypeByType(type, contextNode, nameParsingContext));
    }

    private List<JavaTypeInfo> tryFindParamTypes(MethodCallExpr expr) {
        return expr.getArguments().stream()
                .map(argument -> parseTypeByExpr(argument, expr))
                .filter(Objects::nonNull)
                .toList();
    }

    private JavaTypeInfo parseTypeByName(String name, Node contextNode) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        if (isPrimitiveTypeName(name)) {
            return JavaTypeInfo.byByteCode(getPrimitiveTypeByName(name));
        }
        JavaTypeInfo byIndex = context.findTypeInIndex(name).map(declaration -> JavaTypeInfo.byDeclaration(declaration, context.getInfoByClassName(name))).orElse(null);
        if (null != byIndex) {
            return byIndex;
        }
        if (name.contains(".") || name.contains("$")) {
            JavaTypeInfo tryGet = tryGetTypeByClassLoader(name);
            if (null != tryGet) {
                return tryGet;
            }
        }
        CompilationUnit ast = AstUtils.getAst(contextNode);
        JavaTypeInfo byImports = tryParseTypeByImports(name, ast);
        if (null != byImports) {
            return byImports;
        }
        // 没有import就是相同包或者java.lang
        String samePackageClassName = ast.getPackageDeclaration()
                .map(NodeWithName::getNameAsString)
                .map(packageName -> packageName + ".")
                .orElse("") + name;
        return Optional.ofNullable(tryGetTypeByClassLoader(samePackageClassName))
                .orElseGet(() -> JavaTypeInfo.builder()
                        .name(name)
                        .classQualifiedName(samePackageClassName)
                        .build());
    }

    private JavaTypeInfo parseTypeByType(Type type, Node contextNode, NameParsingContext nameParsingContext) {
        if (type instanceof ClassOrInterfaceType) {
            return parseTypeByName(AstUtils.getName(type), contextNode);
        }
        if (type instanceof PrimitiveType primitiveType) {
            return JavaTypeInfo.byByteCode(getPrimitiveTypeByName(primitiveType.asString()));
        }
        if (type instanceof VarType varType) {
            return parseVarType(varType, contextNode, nameParsingContext);
        }
        if (type instanceof ArrayType arrayType) {
            return buildArrayType(arrayType, contextNode);
        }
        throw new UnsupportedOperationException("未处理的类型(%s): %s".formatted(type.getClass().getSimpleName(), type));
    }

    private List<ClassOrInterfaceDeclaration> parseDeclarationByTypes(List<? extends Type> types, Node contextNode) {
        return types.stream()
                .map(type -> parseTypeByType(type, contextNode, new NameParsingContext()))
                .filter(Objects::nonNull)
                .map(JavaTypeInfo::getTypeDeclaration)
                .filter(ClassOrInterfaceDeclaration.class::isInstance)
                .map(ClassOrInterfaceDeclaration.class::cast)
                .toList();
    }

    private JavaTypeInfo parseVarType(VarType varType, Node contextNode, NameParsingContext nameParsingContext) {
        Node parent = varType.getParentNode().orElse(null);
        List<Expression> expressions = varType.getParentNode()
                .map(Node::getChildNodes)
                .stream()
                .flatMap(Collection::stream)
                .filter(Expression.class::isInstance)
                .map(Expression.class::cast)
                .toList();
        if (expressions.size() > 1) {
            throw new IllegalStateException("varType的父节点中表达式不止一条 " + parent);
        }
        if (expressions.size() == 1) {
            return parseTypeByExpr(expressions.get(0), contextNode);
        }
        ForEachStmt forEachStmt = AstUtils.tryfindNodeInParent(varType, ForEachStmt.class);
        if (null != forEachStmt) {
            return forEachStmt.findFirst(NameExpr.class)
                    .flatMap(nameExpr -> findTypeByNameExpr(nameExpr, contextNode, nameParsingContext))
                    .orElseThrow();
        }
        throw new UnsupportedOperationException("未处理的varType parent: %s".formatted(parent));
    }

    private JavaTypeInfo buildArrayType(ArrayType arrayType, Node contextNode) {
        return JavaTypeInfo.array(parseTypeByType(arrayType.getComponentType(), contextNode, new NameParsingContext()));
    }

    private Method parseMethod(Class<?> methodDeclaringClass, MethodCallExpr methodCallExpression, Node contextNode) {
        return tryMatchMethod(contextNode, methodDeclaringClass, methodCallExpression.getNameAsString(), methodCallExpression.getArguments());
    }

    private Method parseMethod(String calleeTypeName, MethodCallExpr methodCallExpression, Node contextNode) {
        return Optional.ofNullable(parseTypeByName(calleeTypeName, contextNode))
                .map(JavaTypeInfo::getByteCode)
                .map(byteCode -> parseMethodInInheritLink(byteCode, false, methodCallExpression, contextNode))
                .orElse(null);
    }

    private Method parseMethodInInheritLink(Class<?> type, boolean superOnly, MethodCallExpr methodCallExpression, Node contextNode) {
        return ReflectUtils.findInInheritLink(type, superOnly, Objects::nonNull, current -> List.of(parseMethod(current, methodCallExpression, contextNode)));
    }

    /**
     * 不会做充分的检查
     */
    private Method tryMatchMethod(Node contextNode, Class<?> methodDeclaringClass, String methodName, NodeList<Expression> args) {
        // 先过滤方法名
        List<Method> methods = Stream.of(methodDeclaringClass.getDeclaredMethods())
                .filter(method -> Objects.equals(method.getName(), methodName))
                .toList();
        if (methods.isEmpty()) {
            return null;
        }
        if (methods.size() == 1) {
            return methods.get(0);
        }
        // 再按能拿到的参数类型过滤
        List<Class<?>> argTypes = args.stream()
                .map(arg -> parseTypeByExpr(arg, contextNode))
                .filter(Objects::nonNull)
                .map(JavaTypeInfo::getByteCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
        boolean gotAllArgTypes = argTypes.stream().allMatch(Objects::nonNull);
        methods = methods.stream().filter(method -> filterByArgTypes(method, argTypes, true)).toList();
        if (methods.isEmpty()) {
            log.debug("在{}中匹配不到方法{}({})", methodDeclaringClass.getName(), methodName, args.stream().map(String::valueOf).collect(Collectors.joining(", ")));
            return null;
        }
        if (methods.size() == 1) {
            return methods.get(0);
        }
        if (gotAllArgTypes) {
            Method specific = findMostSpecificMethod(methods, argTypes);
            if (null != specific) {
                return specific;
            }
        }
        log.debug("无法区分{}中的多个重载方法{}({}) gotAllArgTypes:{} 候选：{}", methodDeclaringClass.getName(), methodName, args.stream().map(String::valueOf).collect(Collectors.joining(", ")), gotAllArgTypes, methods);
        return null;
    }

    private boolean filterByArgTypes(Method method, List<Class<?>> argTypes, boolean matchIfArgTypeLack) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        // 处理变长参数
        if (method.isVarArgs()) {
            Class<?> componentType = parameterTypes[parameterTypes.length - 1].getComponentType();
            if (!argTypes.stream().skip(parameterTypes.length - 1L).allMatch(argType -> null == argType && matchIfArgTypeLack || isSupportedType(componentType, argType))) {
                return false;
            }
            parameterTypes = Stream.of(parameterTypes).limit(parameterTypes.length - 1L).toArray(Class[]::new);
            argTypes = argTypes.stream().limit(parameterTypes.length).toList();
        }
        if (parameterTypes.length != argTypes.size()) {
            return false;
        }
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> argType = argTypes.get(i);
            if (null == argType) {
                if (matchIfArgTypeLack) {
                    continue;
                }
                return false;
            }
            if (!isSupportedType(parameterTypes[i], argType)) {
                return false;
            }
        }
        return true;
    }

    private boolean isSupportedType(Class<?> expected, Class<?> actual) {
        if (null == actual) {
            return false;
        }
        if (expected.equals(actual)) {
            return true;
        }
        if (PrimitiveTypeUtils.isCompatiblePrimitiveType(expected, actual)) {
            return true;
        }
        return boxIfPrimitive(expected).isAssignableFrom(boxIfPrimitive(actual));
    }

    private Class<?> boxIfPrimitive(Class<?> type) {
        return type.isPrimitive() ? getWrapperClass(type) : type;
    }

    private Method findMostSpecificMethod(List<Method> methods, List<Class<?>> argTypes) {
        if (methods.isEmpty()) {
            return null;
        }
        Method winner = methods.get(0);
        int argNum = argTypes.size();
        for (int i = 1; i < methods.size(); i++) {
            Method challenger = methods.get(i);
            Class<?>[] winnerParamTypes = winner.getParameterTypes();
            Class<?>[] challengerParamTypes = challenger.getParameterTypes();
            for (int j = 0; j < argNum; j++) {
                if (isChallengerMoreSpecific(argTypes.get(j), winnerParamTypes[j], challengerParamTypes[j])) {
                    winner = challenger;
                    break;
                }
            }
        }
        return winner;
    }

    private boolean isChallengerMoreSpecific(Class<?> actual, Class<?> winner, Class<?> challenger) {
        if (winner.equals(actual) || winner.equals(challenger)) {
            return false;
        }
        if (challenger.equals(actual)) {
            return true;
        }
        if (actual.isPrimitive()) {
            if (!winner.isPrimitive()) {
                return true;
            }
            if (isFloatLike(actual) && isIntegerLike(winner) && isFloatLike(challenger)) {
                return true;
            }
            return isIntegerLike(actual) && isFloatLike(winner) && isIntegerLike(challenger);
        }
        if (winner.isInterface()) {
            return !challenger.isInterface();
        }
        return winner.isAssignableFrom(challenger);
    }

    private FieldDeclaration findFieldByName(ClassOrInterfaceDeclaration typeDeclaration, String name) {
        return findInInheritLink(
                typeDeclaration,
                false,
                field -> field.getVariables().stream()
                        .map(NodeWithSimpleName::getNameAsString)
                        .anyMatch(name::equals),
                NodeWithMembers::getFields
        );
    }

    private ClassOrInterfaceDeclaration getSuperclass(ClassOrInterfaceDeclaration type) {
        if (type.isInterface()) {
            return null;
        }
        var extendedTypes = type.getExtendedTypes();
        if (extendedTypes.isEmpty()) {
            return null;
        }
        return Optional.ofNullable(parseTypeByType(extendedTypes.get(0), type, new NameParsingContext()))
                .map(JavaTypeInfo::getTypeDeclaration)
                .filter(ClassOrInterfaceDeclaration.class::isInstance)
                .map(ClassOrInterfaceDeclaration.class::cast)
                .orElse(null);
    }

    private List<ClassOrInterfaceDeclaration> findAllInterfaces(ClassOrInterfaceDeclaration type) {
        Set<ClassOrInterfaceDeclaration> checked = new HashSet<>(4);
        Set<ClassOrInterfaceDeclaration> result = new HashSet<>(4);
        Queue<ClassOrInterfaceDeclaration> queue = new LinkedList<>();
        queue.add(type);
        while (!queue.isEmpty()) {
            ClassOrInterfaceDeclaration current = queue.poll();
            if (checked.contains(current)) {
                continue;
            }
            checked.add(current);
            if (current.isInterface()) {
                result.add(current);
                parseDeclarationByTypes(current.getExtendedTypes(), type).forEach(currentType -> {
                    result.add(currentType);
                    queue.add(currentType);
                });
                continue;
            }
            parseDeclarationByTypes(current.getImplementedTypes(), type).forEach(currentType -> {
                result.add(currentType);
                queue.add(currentType);
            });
        }
        return new ArrayList<>(result);
    }

    private <T> T findInInheritLink(ClassOrInterfaceDeclaration type, boolean superOnly, Predicate<T> filter, Function<ClassOrInterfaceDeclaration, List<T>> getter) {
        if (null == type) {
            return null;
        }
        // 先在当前类型和父类里面找
        if (!type.isInterface()) {
            ClassOrInterfaceDeclaration current = superOnly ? getSuperclass(type) : type;
            while (null != current) {
                List<T> elements = getter.apply(current);
                for (T element : elements) {
                    if (filter.test(element)) {
                        return element;
                    }
                }
                current = getSuperclass(current);
            }
        }
        // 再找接口
        var interfaces = findAllInterfaces(type);
        for (ClassOrInterfaceDeclaration interfaceType : interfaces) {
            List<T> elements = getter.apply(interfaceType);
            for (T element : elements) {
                if (filter.test(element)) {
                    return element;
                }
            }
        }
        return null;
    }

    private JavaTypeInfo parseBinaryExprByClassLoader(BinaryExpr binaryExpr, Node contextNode) {
        if (isBooleanOperator(binaryExpr.getOperator())) {
            return JavaTypeInfo.byByteCode(boolean.class);
        }
        Expression left = binaryExpr.getLeft();
        Expression right = binaryExpr.getRight();
        if (isStringLikeLiteral(left) || isStringLikeLiteral(right)) {
            return JavaTypeInfo.byByteCode(String.class);
        }
        Class<?> leftType = Optional.ofNullable(parseTypeByExpr(left, contextNode)).map(JavaTypeInfo::getByteCode).orElse(null);
        Class<?> rightType = Optional.ofNullable(parseTypeByExpr(right, contextNode)).map(JavaTypeInfo::getByteCode).orElse(null);
        if (isNumberLike(leftType) && isNumberLike(rightType)) {
            return JavaTypeInfo.byByteCode(getPrimitiveClass(pickNumberType(leftType, rightType)));
        }
        if (String.class.equals(leftType) || String.class.equals(rightType)) {
            return JavaTypeInfo.byByteCode(String.class);
        }
        log.warn("未支持的BinaryExpr {}", binaryExpr);
        return null;
    }

    private boolean isBooleanOperator(BinaryExpr.Operator operator) {
        return switch (operator) {
            case OR, AND, EQUALS, NOT_EQUALS, LESS, GREATER, LESS_EQUALS, GREATER_EQUALS -> true;
            default -> false;
        };
    }

    private boolean isStringLikeLiteral(Expression expression) {
        return expression instanceof StringLiteralExpr || expression instanceof TextBlockLiteralExpr;
    }

    private boolean isNumberLikeLiteral(Expression expression) {
        return expression instanceof DoubleLiteralExpr || isIntegerLikeLiteral(expression);
    }

    private boolean isIntegerLikeLiteral(Expression expression) {
        return expression instanceof LongLiteralExpr
                || expression instanceof IntegerLiteralExpr
                || expression instanceof CharLiteralExpr;
    }

    private Class<?> pickNumberType(Class<?> left, Class<?> right) {
        if (null == left || null == right) {
            return null;
        }
        left = boxIfPrimitive(left);
        right = boxIfPrimitive(right);
        if (!Number.class.isAssignableFrom(left) || !Number.class.isAssignableFrom(right)) {
            throw new IllegalArgumentException("无法获取结果Number类型 %s %s".formatted(left, right));
        }
        if (Double.class.equals(left) || Double.class.equals(right)) {
            return Double.class;
        }
        if (Float.class.equals(left) || Float.class.equals(right)) {
            return Float.class;
        }
        if (Long.class.equals(left) || Long.class.equals(right)) {
            return Long.class;
        }
        return Integer.class;
    }

    private JavaTypeInfo parseConditionalExprByClassLoader(ConditionalExpr conditionalExpr, Node contextNode) {
        Class<?> thenType = Optional.ofNullable(parseTypeByExpr(conditionalExpr.getThenExpr(), contextNode)).map(JavaTypeInfo::getByteCode).orElse(null);
        Class<?> elseType = Optional.ofNullable(parseTypeByExpr(conditionalExpr.getElseExpr(), contextNode)).map(JavaTypeInfo::getByteCode).orElse(null);
        if (null == thenType) {
            if (null == elseType) {
                return null;
            }
            return JavaTypeInfo.byByteCode(elseType);
        }
        if (null == elseType) {
            return JavaTypeInfo.byByteCode(thenType);
        }
        if (thenType.equals(elseType)) {
            return JavaTypeInfo.byByteCode(thenType);
        }
        if (isSupportedType(thenType, elseType)) {
            return JavaTypeInfo.byByteCode(thenType);
        }
        if (isSupportedType(elseType, thenType)) {
            return JavaTypeInfo.byByteCode(elseType);
        }
        return JavaTypeInfo.byByteCode(findCommonSuperType(thenType, elseType));
    }

    private Class<?> findCommonSuperType(Class<?> one, Class<?> other) {
        List<Class<?>> oneSuper = getSuperTypeLink(one);
        List<Class<?>> otherSuper = getSuperTypeLink(other);
        Class<?> result = oneSuper.stream().filter(otherSuper::contains).findFirst().orElse(Object.class);
        if (!Object.class.equals(result)) {
            return result;
        }
        List<Class<?>> oneInterfaces = ReflectUtils.findAllInterfaces(one);
        List<Class<?>> otherInterfaces = ReflectUtils.findAllInterfaces(other);
        List<Class<?>> commonInterfaces = oneInterfaces.stream().filter(otherInterfaces::contains).toList();
        if (commonInterfaces.size() == 1) {
            return commonInterfaces.get(0);
        }
        return result;
    }

    private List<Class<?>> getSuperTypeLink(Class<?> type) {
        if (null == type || Object.class.equals(type)) {
            return Collections.emptyList();
        }
        List<Class<?>> result = new ArrayList<>(4);
        Class<?> current = type.getSuperclass();
        while (null != current) {
            result.add(current);
            current = current.getSuperclass();
        }
        return result;
    }

    private JavaTypeInfo findFieldTypeInStaticImports(String fieldName, Node contextNode, NameParsingContext nameParsingContext) {
        CompilationUnit ast = AstUtils.getAst(contextNode);
        var imports = ast.getImports();
        ast.getTypes().stream().map(AstUtils::getName).forEach(nameParsingContext.staticImportSearchLink::add);
        for (ImportDeclaration importDeclaration : imports) {
            if (!importDeclaration.isStatic()) {
                continue;
            }
            String importName = importDeclaration.getNameAsString();
            if (!importDeclaration.isAsterisk() && !importName.endsWith("." + fieldName)) {
                continue;
            }
            String otherClassName = importName.substring(0, importName.length() - 1 - fieldName.length());
            if (nameParsingContext.staticImportSearchLink.contains(otherClassName)) {
                continue;
            }
            JavaTypeInfo otherType = parseTypeByName(otherClassName, ast);
            if (null == otherType) {
                continue;
            }
            JavaTypeInfo fieldType = findFieldTypeByName(fieldName, otherType, contextNode, nameParsingContext);
            if (null != fieldType) {
                return fieldType;
            }
        }
        return null;
    }

    private JavaTypeInfo findFieldTypeByName(String fieldName, JavaTypeInfo type, Node contextNode, NameParsingContext nameParsingContext) {
        Class<?> byteCode = type.getByteCode();
        if (null != byteCode) {
            Field fieldByName = ReflectUtils.findInInheritLink(
                    byteCode,
                    false,
                    field -> fieldName.equals(field.getName()),
                    currentType -> Arrays.asList(currentType.getDeclaredFields())
            );
            if (null != fieldByName) {
                return JavaTypeInfo.byByteCode(fieldByName.getType());
            }
        }
        return Optional.ofNullable(type.getTypeDeclaration())
                .filter(ClassOrInterfaceDeclaration.class::isInstance)
                .map(ClassOrInterfaceDeclaration.class::cast)
                .map(typeDeclaration -> findInInheritLink(
                        typeDeclaration,
                        false,
                        variable -> fieldName.equals(variable.getNameAsString()),
                        currentType -> currentType.getFields().stream()
                                .map(FieldDeclaration::getVariables)
                                .flatMap(Collection::stream)
                                .toList()
                ))
                .map(VariableDeclarator::getType)
                .map(variableType -> parseTypeByType(variableType, contextNode, nameParsingContext))
                .orElse(null);
    }

    private JavaTypeInfo buildTypeInfo(Type type) {
        if (type.isPrimitiveType()) {
            return JavaTypeInfo.byByteCode(getPrimitiveTypeByName(type.asString()));
        }
        ResolvedType resolvedType = AstUtils.tryResolve(type);
        if (null == resolvedType || !resolvedType.isReferenceType()) {
            return JavaTypeInfo.builder()
                    .name(type.asString())
                    .classQualifiedName(AstUtils.getName(type))
                    .build();
        }
        ResolvedReferenceType referenceType = resolvedType.asReferenceType();
        ResolvedReferenceTypeDeclaration typeDeclaration = referenceType.getTypeDeclaration().orElse(null);
        if (null == typeDeclaration) {
            return null;
        }
        if (typeDeclaration instanceof ReflectionClassDeclaration declaration) {
            return JavaTypeInfo.byByteCode(AstUtils.getReflectionClass(declaration));
        }
        // TODO
        log.warn("未处理的类型 {}", typeDeclaration.getClass().getSimpleName());
        return null;
    }

    public int getInheritDistance(Type type, String expectedType) {
        return getInheritDistance(buildTypeInfo(type), expectedType);
    }

    public int getInheritDistance(JavaTypeInfo type, String expectedType) {
        if (null == type) {
            return Integer.MAX_VALUE;
        }
        if (type.getClassQualifiedName().equals(expectedType)) {
            return 0;
        }
        if (PrimitiveTypeUtils.isPrimitiveTypeName(expectedType)) {
            expectedType = PrimitiveTypeUtils.getWrapperClassByPrimitiveName(expectedType).getName();
            if (type.getClassQualifiedName().equals(expectedType)) {
                return 1;
            }
        }
        int inheritDistance = getInheritDistance(tryGetParentClass(type), expectedType);
        if (inheritDistance == Integer.MAX_VALUE || inheritDistance < 0) {
            return Integer.MAX_VALUE;
        }
        return 1 + inheritDistance;
    }

    public JavaTypeInfo tryGetParentClass(JavaTypeInfo type) {
        return Optional.ofNullable(tryGetParentClassByByteCode(type)).orElseGet(() -> tryGetParentClassByDeclaration(type));
    }

    private JavaTypeInfo tryGetParentClassByByteCode(JavaTypeInfo type) {
        Class<?> byteCode = type.getByteCode();
        if (null == byteCode || byteCode.isInterface()) {
            return null;
        }
        return Optional.ofNullable(byteCode.getSuperclass())
                .map(JavaTypeInfo::byByteCode)
                .orElse(null);
    }

    private JavaTypeInfo tryGetParentClassByDeclaration(JavaTypeInfo type) {
        TypeDeclaration<?> typeDeclaration = type.getTypeDeclaration();
        if (!(typeDeclaration instanceof ClassOrInterfaceDeclaration declaration)) {
            return null;
        }
        if (declaration.isInterface()) {
            return null;
        }
        NodeList<ClassOrInterfaceType> extendedTypes = declaration.getExtendedTypes();
        if (extendedTypes.isEmpty()) {
            return null;
        }
        return buildTypeInfo(extendedTypes.get(0));
    }

}
