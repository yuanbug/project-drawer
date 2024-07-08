package io.github.yuanbug.drawer.domain.view.graph.method;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author yuanbug
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArgumentView {

    private String name;

    private String type;

}
