package io.github.yuanbug.drawer.parser;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import io.github.yuanbug.drawer.config.AstParsingConfig;
import io.github.yuanbug.drawer.domain.ast.AstIndexContext;
import io.github.yuanbug.drawer.domain.info.MethodCalling;
import io.github.yuanbug.drawer.domain.info.MethodCallingType;
import io.github.yuanbug.drawer.domain.info.MethodId;
import io.github.yuanbug.drawer.domain.info.MethodInfo;
import io.github.yuanbug.drawer.utils.AstUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yuanbug
 */
@Component
public class MethodParser {

    private final AstIndexContext context;
    private final AstParsingConfig config;
    private final UnsolvedParser unsolvedMethodParser;
    private final InheritMethodParser inheritMethodParser;

    public MethodParser(AstIndexContext context, AstParsingConfig config) {
        this.context = context;
        this.config = config;
        this.unsolvedMethodParser = new UnsolvedParser(context, config);
        this.inheritMethodParser = new InheritMethodParser(context);
    }

    private final Map<String, MethodInfo> methods = new ConcurrentHashMap<>(64);

    public MethodInfo parseMethod(String methodId) {
        if (methods.containsKey(methodId)) {
            return methods.get(methodId);
        }
        MethodInfo methodInfo = findMethod(MethodId.parse(methodId))
                .map(declaration -> parseMethod(declaration, new LinkedHashMap<>()))
                .orElseThrow(() -> new IllegalStateException("找不到方法声明：" + methodId));
        methods.put(methodId, methodInfo);
        return methodInfo;
    }

    protected MethodInfo parseMethod(MethodDeclaration method, Map<String, MethodInfo> link) {
        MethodId methodId = MethodId.from(method);
        MethodInfo methodInfo = MethodInfo.builder()
                .id(methodId)
                .dependencies(Collections.emptyList())
                .declaration(method)
                .overrides(parseOverrides(method))
                .build();
        link.put(methodId.toString(), methodInfo);
        methodInfo.setDependencies(parseMethodDependencies(method, link));
        link.remove(methodId.toString());
        return methodInfo;
    }

    protected Optional<MethodDeclaration> findMethod(MethodId methodId) {
        // TODO 支持lombok
        return context.findTypeInIndex(methodId.getClassName()).map(type -> findMethod(type, methodId));
    }

    protected MethodDeclaration findMethod(TypeDeclaration<?> type, MethodId methodId) {
        MethodDeclaration method = findMethodInThisType(type, methodId);
        if (null != method) {
            return method;
        }
        return context.getAllParentTypes(type).values().stream()
                .map(superType -> findMethodInThisType(type, methodId))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    protected MethodDeclaration findMethodInThisType(TypeDeclaration<?> type, MethodId methodId) {
        List<MethodDeclaration> candidates = type.findAll(MethodDeclaration.class, method -> inheritMethodParser.parseParentMethods(method).isCompatibleMethod(methodId));
        if (CollectionUtils.isEmpty(candidates)) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        return pickBest(candidates, methodId);
    }

    /**
     * 挑选参数类型匹配程度最高的方法
     */
    protected MethodDeclaration pickBest(List<MethodDeclaration> candidates, MethodId methodId) {
        List<String> expectedTypes = methodId.getParamTypes();
        candidates.sort((winner, challenger) -> {
            var winnerParameters = winner.getParameters();
            var challengerParameters = challenger.getParameters();
            for (int i = 0; i < winnerParameters.size(); i++) {
                var winnerParameter = winnerParameters.get(i).getType();
                var challengerParameter = challengerParameters.get(i).getType();
                var expectedType = expectedTypes.get(i);
                if (AstUtils.getName(winnerParameter).equals(expectedType)) {
                    return -1;
                }
                if (AstUtils.getName(challengerParameter).equals(expectedType)) {
                    return 1;
                }
                var resolvedChallengerParameter = AstUtils.tryResolve(challengerParameter);
                if (resolvedChallengerParameter.isTypeVariable()) {
                    return -1;
                }
                int compareByDistance = Integer.compare(unsolvedMethodParser.getInheritDistance(winnerParameter, expectedType), unsolvedMethodParser.getInheritDistance(challengerParameter, expectedType));
                if (compareByDistance != 0) {
                    return compareByDistance;
                }
            }
            return 0;
        });
        return candidates.get(0);
    }


    protected List<MethodCalling> parseMethodDependencies(MethodDeclaration method, Map<String, MethodInfo> link) {
        List<MethodCallExpr> methodCallExpressions = method.getBody()
                .map(block -> block.findAll(MethodCallExpr.class))
                .orElse(null);
        if (CollectionUtils.isEmpty(methodCallExpressions)) {
            return Collections.emptyList();
        }
        return methodCallExpressions.stream()
                .map(expr -> parseCalling(expr, method, link))
                .filter(Objects::nonNull)
                .filter(calling -> config.getMethodCallingFilter().test(calling, context))
                .toList();
    }

    protected MethodCalling parseCalling(MethodCallExpr expr, MethodDeclaration callerMethod, Map<String, MethodInfo> link) {
        ResolvedMethodDeclaration calleeMethod = AstUtils.tryResolve(expr);
        if (null == calleeMethod) {
            return adjustUnsolved(unsolvedMethodParser.buildUnsolveMethodCalling(expr, callerMethod), callerMethod);
        }
        ResolvedReferenceTypeDeclaration calleeType = calleeMethod.declaringType();
        MethodId calleeMethodId = MethodId.from(calleeMethod);
        MethodInfo recursion = link.get(calleeMethodId.toString());
        if (null != recursion) {
            return MethodCalling.recursive(recursion, judgeMethodCallingType(calleeType, AstUtils.findDeclaringType(callerMethod)));
        }
        if (!config.getMethodFilter().test(calleeMethod, calleeType)) {
            return MethodCalling.builder()
                    .callee(MethodInfo.builder()
                            .id(calleeMethodId)
                            .declaration(null)
                            .dependencies(Collections.emptyList())
                            .overrides(Collections.emptyList())
                            .build())
                    .callingType(judgeMethodCallingType(calleeType, AstUtils.findDeclaringType(callerMethod)))
                    .build();
        }
        MethodDeclaration calleeMethodDeclaration = findMethod(calleeMethodId).orElse(null);
        if (null == calleeType || null == calleeMethodDeclaration) {
            return MethodCalling.library(calleeMethodId, calleeMethodDeclaration);
        }
        return MethodCalling.builder()
                .callee(parseMethod(calleeMethodDeclaration, link))
                .callingType(judgeMethodCallingType(calleeType, AstUtils.findDeclaringType(callerMethod)))
                .build();
    }

    protected MethodCallingType judgeMethodCallingType(ResolvedTypeDeclaration calleeType, TypeDeclaration<?> typeDeclaration) {
        if (ParserConstants.isJdkType(calleeType)) {
            return MethodCallingType.JDK;
        }
        if (calleeType.equals(AstUtils.tryResolveTypeDeclaration(typeDeclaration))) {
            return MethodCallingType.SELF;
        }
        String calleeModule = context.getModuleNameByTypeName(AstUtils.getName(calleeType));
        String callerModule = context.getModuleNameByTypeName(AstUtils.getName(typeDeclaration));
        if (StringUtils.isNotBlank(calleeModule) && StringUtils.isNotBlank(callerModule)) {
            return Objects.equals(calleeModule, callerModule) ? MethodCallingType.BROTHER : MethodCallingType.OUT;
        }
        return MethodCallingType.LIBRARY;
    }

    protected List<MethodInfo> parseOverrides(MethodDeclaration method) {
        MethodId methodId = MethodId.from(method);
        TypeDeclaration<?> declaringType = AstUtils.findDeclaringType(method);
        if (!(declaringType instanceof ClassOrInterfaceDeclaration classOrInterfaceDeclaration)) {
            return Collections.emptyList();
        }
        return config.getDirectlySubTypeParser().apply(classOrInterfaceDeclaration, context)
                .stream()
                .map(subType -> findMethodInThisType(subType, methodId))
                .filter(Objects::nonNull)
                .map(MethodId::from)
                .map(MethodId::toString)
                .map(this::parseMethod)
                .filter(Objects::nonNull)
                .toList();
    }

    protected MethodCalling adjustUnsolved(MethodCalling methodCalling, MethodDeclaration callerMethod) {
        if (null == methodCalling) {
            return null;
        }
        MethodInfo callee = methodCalling.getCallee();
        if (null == callee) {
            return null;
        }
        if (null == callee.getDeclaration()) {
            findMethod(callee.getId()).ifPresent(declaration -> {
                callee.setDeclaration(declaration);
                callee.setId(MethodId.from(declaration));
                TypeDeclaration<?> declaringType = AstUtils.findDeclaringType(declaration);
                ResolvedTypeDeclaration calleeType = AstUtils.tryResolveTypeDeclaration(declaringType);
                if (null != calleeType) {
                    methodCalling.setCallingType(judgeMethodCallingType(calleeType, AstUtils.findDeclaringType(callerMethod)));
                }
                try {
                    MethodInfo methodInfo = parseMethod(callee.getId().toString());
                    callee.setDependencies(methodInfo.getDependencies());
                    callee.setOverrides(methodInfo.getOverrides());
                } catch (Exception ignored) {}
            });
        }
        return methodCalling;
    }

}
