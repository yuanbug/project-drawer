package io.github.yuanbug.drawer.domain.info;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import io.github.yuanbug.drawer.utils.MiscUtils;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yuanbug
 */
@Data
public class TypeLombokInfo {

    private final TypeDeclaration<?> typeDeclaration;

    private final Map<String, MethodDeclaration> methods;

    private final Map<String, MethodDeclaration> getters;

    private final Map<String, MethodDeclaration> setters;

    @Builder
    @SuppressWarnings("unchecked")
    public TypeLombokInfo(TypeDeclaration<?> typeDeclaration,
                          List<MethodDeclaration> getters,
                          List<MethodDeclaration> setters) {
        this.typeDeclaration = typeDeclaration;
        this.getters = MethodId.group(getters);
        this.setters = MethodId.group(setters);
        this.methods = MiscUtils.merge(HashMap::new, this.getters, this.setters);
    }

}
