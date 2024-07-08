package io.github.yuanbug.drawer.domain.view.graph.method;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author yuanbug
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MethodView {

    private String id;

    private String name;

    private String declaringClass;

    private List<ArgumentView> arguments;

}
