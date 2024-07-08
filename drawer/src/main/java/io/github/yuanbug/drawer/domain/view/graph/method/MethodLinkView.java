package io.github.yuanbug.drawer.domain.view.graph.method;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @author yuanbug
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MethodLinkView {

    private String rootMethodId;

    private Map<String, MethodView> methods;

    private List<MethodCallingView> callings;

    private List<MethodCallingView> recursions;

    private Map<String, List<String>> overrides;

}
