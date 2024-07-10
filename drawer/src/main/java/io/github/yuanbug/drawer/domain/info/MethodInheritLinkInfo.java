package io.github.yuanbug.drawer.domain.info;

import com.github.javaparser.ast.body.MethodDeclaration;
import io.github.yuanbug.drawer.domain.ast.AstIndex;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author yuanbug
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MethodInheritLinkInfo {

    private MethodDeclaration current;

    private List<MethodDeclaration> fromExtend;

    private List<MethodDeclaration> fromImpl;

    private AstIndex astIndex;

    public boolean isCompatibleMethod(MethodId methodId) {
        if (!current.getNameAsString().equals(methodId.getMethodName())) {
            return false;
        }
        List<String> expectedTypes = methodId.getParamTypes();
        var parameters = current.getParameters();
        if (parameters.size() != expectedTypes.size()) {
            return false;
        }
        for (int i = 0; i < parameters.size(); i++) {
            if (!isParamTypeCompatible(i, expectedTypes.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean isParamTypeCompatible(int i, String typeName) {
        return Stream.concat(Stream.of(current), Stream.concat(fromExtend.stream(), fromImpl.stream()))
                .map(method -> method.getParameter(i).getType())
                .anyMatch(type -> astIndex.isTypeOf(type, typeName, true));
    }

}
