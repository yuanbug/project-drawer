package io.github.yuanbug.drawer.domain.info;

import com.github.javaparser.ast.body.MethodDeclaration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import java.util.Collections;

/**
 * @author yuanbug
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MethodCalling {

    @Nullable
    private MethodInfo callee;

    private MethodCallingType callingType;

    @Nullable
    private String recursiveAt;

    public static MethodCalling library(MethodId calleeMethodId, @Nullable MethodDeclaration calleeMethodDeclaration) {
        return MethodCalling.builder()
                .callee(MethodInfo.builder()
                        .id(calleeMethodId)
                        .declaration(calleeMethodDeclaration)
                        .dependencies(Collections.emptyList())
                        .overrides(Collections.emptyList())
                        .build())
                .callingType(MethodCallingType.LIBRARY)
                .build();
    }

    public static MethodCalling recursive(MethodInfo recursion, MethodCallingType callingType) {
        return MethodCalling.builder()
                .callingType(callingType)
                .recursiveAt(recursion.getId().toString())
                .build();
    }

}
