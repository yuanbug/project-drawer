package io.github.yuanbug.drawer.parser;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import io.github.yuanbug.drawer.config.AstParsingConfig;
import io.github.yuanbug.drawer.domain.ast.AstIndex;
import io.github.yuanbug.drawer.domain.info.MethodCalling;
import io.github.yuanbug.drawer.domain.info.MethodCallingType;
import io.github.yuanbug.drawer.domain.info.MethodId;
import io.github.yuanbug.drawer.domain.info.MethodInfo;
import io.github.yuanbug.drawer.utils.AstUtils;
import io.github.yuanbug.drawer.utils.MiscUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yuanbug
 */
@Slf4j
@Component
public class MethodParser {

    private final AstIndex astIndex;
    private final AstParsingConfig config;
    private final UnsolvedParser unsolvedMethodParser;
    private final InheritMethodParser inheritMethodParser;

    public MethodParser(AstIndex astIndex, AstParsingConfig config) {
        this.astIndex = astIndex;
        this.config = config;
        this.unsolvedMethodParser = new UnsolvedParser(astIndex, config);
        this.inheritMethodParser = new InheritMethodParser(astIndex);
    }

    private final Map<String, MethodInfo> methods = new ConcurrentHashMap<>(64);

    protected static class MethodParsingContext {

        private final Map<String, MethodInfo> recursionLink = new LinkedHashMap<>();

        private final Set<String> parsedCalling = new HashSet<>();

    }

    public MethodInfo parseMethod(String methodId) {
        if (methods.containsKey(methodId)) {
            return methods.get(methodId);
        }
        MethodInfo methodInfo = findMethod(MethodId.parse(methodId))
                .map(declaration -> parseMethod(declaration, new MethodParsingContext()))
                .orElseThrow(() -> new IllegalStateException("找不到方法声明：" + methodId));
        methods.put(methodId, methodInfo);
        return methodInfo;
    }

    protected MethodInfo parseMethod(MethodDeclaration method, MethodParsingContext parsingContext) {
        MethodId methodId = MethodId.from(method);
        String methodIdStr = methodId.toString();
        if (methods.containsKey(methodIdStr)) {
            return methods.get(methodIdStr);
        }
        log.info("正在解析方法 {}", methodId);
        MethodInfo methodInfo = MethodInfo.builder()
                .id(methodId)
                .declaration(method)
                .dependencies(Collections.emptyList())
                .overrides(Collections.emptyList())
                .build();
        methods.put(methodIdStr, methodInfo);
        parsingContext.recursionLink.put(methodIdStr, methodInfo);
        methodInfo.setDependencies(parseMethodDependencies(method, parsingContext));
        methodInfo.setOverrides(parseOverrides(method, parsingContext));
        parsingContext.recursionLink.remove(methodIdStr);
        return methodInfo;
    }

    protected Optional<MethodDeclaration> findMethod(MethodId methodId) {
        // TODO 支持lombok
        return astIndex.findTypeInIndex(methodId.getClassName()).map(type -> findMethod(type, methodId));
    }

    protected MethodDeclaration findMethod(TypeDeclaration<?> type, MethodId methodId) {
        MethodDeclaration method = findMethodInThisType(type, methodId);
        if (null != method) {
            return method;
        }
        return astIndex.getAllParentTypes(type).values().stream()
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

    protected List<MethodCalling> parseMethodDependencies(MethodDeclaration method, MethodParsingContext parsingContext) {
        List<MethodCallExpr> methodCallExpressions = method.getBody()
                .map(block -> block.findAll(MethodCallExpr.class))
                .orElse(null);
        if (CollectionUtils.isEmpty(methodCallExpressions)) {
            return Collections.emptyList();
        }
        List<MethodCalling> result = new ArrayList<>();
        for (MethodCallExpr callExpr : methodCallExpressions) {
            MethodCalling calling = parseCalling(callExpr, method, parsingContext);
            if (null == calling) {
                continue;
            }
            String calleeMethodId = Optional.ofNullable(calling.getCallee()).map(MethodInfo::getId).map(MethodId::toString).orElse(null);
            if (null != calleeMethodId) {
                if (parsingContext.parsedCalling.contains(calleeMethodId)) {
                    continue;
                }
                parsingContext.parsedCalling.add(calleeMethodId);
            }
            if (config.getMethodCallingFilter().test(calling, astIndex)) {
                result.add(calling);
            }
        }
        parsingContext.parsedCalling.clear();
        return result;
    }

    protected MethodCalling parseCalling(MethodCallExpr expr, MethodDeclaration callerMethod, MethodParsingContext parsingContext) {
        ResolvedMethodDeclaration calleeMethod = AstUtils.tryResolve(expr);
        if (null == calleeMethod) {
            return adjustUnsolved(unsolvedMethodParser.buildUnsolveMethodCalling(expr, callerMethod), callerMethod);
        }
        ResolvedReferenceTypeDeclaration calleeType = calleeMethod.declaringType();
        MethodId calleeMethodId = MethodId.from(calleeMethod);
        String calleeMethodIdStr = calleeMethodId.toString();
        if (parsingContext.parsedCalling.contains(calleeMethodIdStr)) {
            return null;
        }
        MethodInfo recursion = parsingContext.recursionLink.get(calleeMethodIdStr);
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
                .callee(parseMethod(calleeMethodDeclaration, parsingContext))
                .callingType(judgeMethodCallingType(calleeType, AstUtils.findDeclaringType(callerMethod)))
                .build();
    }

    protected MethodCallingType judgeMethodCallingType(ResolvedTypeDeclaration calleeType, TypeDeclaration<?> callerType) {
        if (ParserConstants.isJdkType(calleeType)) {
            return MethodCallingType.JDK;
        }
        if (calleeType.equals(AstUtils.tryResolveTypeDeclaration(callerType))) {
            return MethodCallingType.SELF;
        }
        if (astIndex.isAssignable(calleeType, AstUtils.getName(callerType))) {
            return MethodCallingType.SUPER;
        }
        String calleeModule = astIndex.getModuleNameByTypeName(AstUtils.getName(calleeType));
        String callerModule = astIndex.getModuleNameByTypeName(AstUtils.getName(callerType));
        if (StringUtils.isNotBlank(calleeModule) && StringUtils.isNotBlank(callerModule)) {
            return Objects.equals(calleeModule, callerModule) ? MethodCallingType.BROTHER : MethodCallingType.OUT;
        }
        return MethodCallingType.LIBRARY;
    }

    protected MethodCallingType judgeMethodCallingType(String calleeTypeName, TypeDeclaration<?> callerType) {
        var calleeType = astIndex.trySolveReferenceTypeDeclaration(calleeTypeName).orElse(null);
        if (null != calleeType) {
            return judgeMethodCallingType(calleeType, callerType);
        }
        String callerTypeName = AstUtils.getName(callerType);
        if (calleeTypeName.equals(callerTypeName)) {
            return MethodCallingType.SELF;
        }
        if (MiscUtils.isPresentAnd(AstUtils.forName(calleeTypeName), byteCode -> astIndex.isAssignable(byteCode, callerTypeName))) {
            return MethodCallingType.SUPER;
        }
        return MethodCallingType.LIBRARY;
    }

    protected List<MethodInfo> parseOverrides(MethodDeclaration method, MethodParsingContext parsingContext) {
        MethodId methodId = MethodId.from(method);
        TypeDeclaration<?> declaringType = AstUtils.findDeclaringType(method);
        if (!(declaringType instanceof ClassOrInterfaceDeclaration classOrInterfaceDeclaration)) {
            return Collections.emptyList();
        }
        return config.getDirectlySubTypeParser().apply(classOrInterfaceDeclaration, astIndex)
                .stream()
                .map(subType -> findMethodInThisType(subType, methodId))
                .filter(Objects::nonNull)
                .map(methodDeclaration -> parseMethod(methodDeclaration, parsingContext))
                .toList();
    }

    protected MethodCalling adjustUnsolved(MethodCalling methodCalling, MethodDeclaration callerMethod) {
        if (null == methodCalling) {
            return null;
        }
        MethodInfo callee = methodCalling.getCallee();
        if (null == callee || null != callee.getDeclaration()) {
            return methodCalling;
        }
        MethodDeclaration methodDeclaration = findMethod(callee.getId()).orElse(null);
        if (null == methodDeclaration) {
            methodCalling.setCallingType(judgeMethodCallingType(callee.getId().getClassName(), AstUtils.findDeclaringType(callerMethod)));
            return methodCalling;
        }
        callee.setDeclaration(methodDeclaration);
        callee.setId(MethodId.from(methodDeclaration));
        TypeDeclaration<?> declaringType = AstUtils.findDeclaringType(methodDeclaration);
        ResolvedTypeDeclaration calleeType = AstUtils.tryResolveTypeDeclaration(declaringType);
        if (null != calleeType) {
            methodCalling.setCallingType(judgeMethodCallingType(calleeType, AstUtils.findDeclaringType(callerMethod)));
        }
        try {
            MethodInfo methodInfo = parseMethod(callee.getId().toString());
            callee.setDependencies(methodInfo.getDependencies());
            callee.setOverrides(methodInfo.getOverrides());
        } catch (Exception ignored) {}
        return methodCalling;
    }

}
