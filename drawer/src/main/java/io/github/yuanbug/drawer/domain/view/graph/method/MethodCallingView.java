package io.github.yuanbug.drawer.domain.view.graph.method;

import io.github.yuanbug.drawer.domain.info.MethodCallingType;
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
public class MethodCallingView {

    private String from;

    private String to;

    private MethodCallingType type;

}
