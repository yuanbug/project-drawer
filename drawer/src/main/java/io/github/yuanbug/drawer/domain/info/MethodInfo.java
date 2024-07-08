package io.github.yuanbug.drawer.domain.info;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.github.javaparser.ast.body.MethodDeclaration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author yuanbug
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MethodInfo {

    @JsonSerialize(using = ToStringSerializer.class)
    private MethodId id;

    @Nullable
    @JsonIgnore
    private MethodDeclaration declaration;

    private List<MethodCalling> dependencies;

    private List<MethodInfo> overrides;

}
