package io.github.yuanbug.drawer.parser;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;
import io.github.yuanbug.drawer.domain.ast.AstIndex;
import io.github.yuanbug.drawer.domain.ast.JavaTypeInfo;
import io.github.yuanbug.drawer.domain.info.MethodInheritLinkInfo;
import io.github.yuanbug.drawer.utils.AstUtils;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Objects;

/**
 * @author yuanbug
 */
@AllArgsConstructor
public class InheritMethodParser {

    private final AstIndex astIndex;

    public MethodInheritLinkInfo parseParentMethods(MethodDeclaration toParse) {
        TypeDeclaration<?> declaringType = AstUtils.findDeclaringType(toParse);

        List<MethodDeclaration> fromExtend = new SuperClassIterator(declaringType, astIndex).stream()
                .flatMap(type -> type.findAll(MethodDeclaration.class).stream())
                .filter(method -> isAssignable(method, toParse))
                .toList();

        List<MethodDeclaration> fromImpl = astIndex.getAllParentTypes(declaringType).values().stream()
                .map(JavaTypeInfo::getTypeDeclaration)
                .filter(ClassOrInterfaceDeclaration.class::isInstance)
                .map(ClassOrInterfaceDeclaration.class::cast)
                .filter(ClassOrInterfaceDeclaration::isInterface)
                .flatMap(type -> type.findAll(MethodDeclaration.class).stream())
                .filter(method -> isAssignable(method, toParse))
                .toList();

        return MethodInheritLinkInfo.builder()
                .current(toParse)
                .fromExtend(fromExtend)
                .fromImpl(fromImpl)
                .astIndex(astIndex)
                .build();
    }

    private boolean isAssignable(MethodDeclaration expectedSuper, MethodDeclaration expectedSub) {
        if (!Objects.equals(expectedSuper.getNameAsString(), expectedSub.getNameAsString())) {
            return false;
        }
        var superParameters = expectedSuper.getParameters();
        var subParameters = expectedSub.getParameters();
        if (superParameters.size() != subParameters.size()) {
            return false;
        }
        for (int i = 0; i < superParameters.size(); i++) {
            var superType = superParameters.get(i).getType();
            var subType = subParameters.get(i).getType();
            if (!isAssignable(superType, subType)) {
                return false;
            }
        }
        return true;
    }

    private boolean isAssignable(Type superType, Type subType) {
        if (astIndex.isAssignable(superType, AstUtils.getName(subType))) {
            return true;
        }
        ResolvedType resolvedSuperType = AstUtils.tryResolve(superType);
        ResolvedType resolvedSubType = AstUtils.tryResolve(subType);
        if (null != resolvedSuperType) {
            if (null != resolvedSubType) {
                return resolvedSuperType.isAssignableBy(resolvedSubType);
            }
            return isAssignable(resolvedSuperType, subType);
        }
        // TODO
        return false;
    }

    private boolean isAssignable(ResolvedType resolvedSuperType, Type unsolvedSubType) {
        // TODO
        return false;
    }

}
