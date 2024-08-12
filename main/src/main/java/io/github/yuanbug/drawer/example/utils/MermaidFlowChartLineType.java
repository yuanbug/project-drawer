package io.github.yuanbug.drawer.example.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * TODO 支持边长度
 *
 * @author yuanbug
 */
@Getter
@AllArgsConstructor
public enum MermaidFlowChartLineType {

    ARROW_HEAD("-->"),
    OPEN("---"),
    DOTTED("-.->"),
    THICK("==>"),
    INVISIBLE("~~~"),
    CIRCLE("--o"),
    CROSS("--x"),
    MULTI_CIRCLE("o--o"),
    MULTI_CROSS("x--x"),
    MULTI_ARROW("<-->"),
    ;

    public final String symbol;

    public String format(String from, String to) {
        return from + " " + symbol + " " + to;
    }

    public String format(String from, String to, String label) {
        if (StringUtils.isBlank(label)) {
            return format(from, to);
        }
        return from + " " + symbol + " " + "|\"" + label + "\"|" + to;
    }

}
