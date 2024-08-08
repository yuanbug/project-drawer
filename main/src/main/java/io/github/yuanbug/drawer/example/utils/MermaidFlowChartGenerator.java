package io.github.yuanbug.drawer.example.utils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author yuanbug
 */
public class MermaidFlowChartGenerator {

    private final Map<String, Integer> nodeIds;
    private final Set<String> edges;
    private int nodeCounter;

    public MermaidFlowChartGenerator() {
        this.nodeIds = new LinkedHashMap<>(16);
        this.edges = new LinkedHashSet<>(16);
        this.nodeCounter = 0;
    }

    public String getChart() {
        StringBuilder builder = new StringBuilder("flowchart TD");
        nodeIds.forEach((nodeName, nodeId) -> builder.append("\n    N%s[\"%s\"]".formatted(nodeId, nodeName)));
        edges.forEach(edge -> builder.append("\n    ").append(edge));
        return builder.toString();
    }

    public String addNode(String nodeName) {
        if (!nodeIds.containsKey(nodeName)) {
            nodeIds.put(nodeName, nodeCounter++);
        }
        return "N" + nodeIds.get(nodeName);
    }

    public void addEdge(String from, String to) {
        addEdge(from, to, MermaidFlowChartLineType.ARROW_HEAD, null);
    }

    public void addEdge(String from, String to, MermaidFlowChartLineType lineType) {
        addEdge(from, to, lineType, null);
    }

    public void addEdge(String from, String to, MermaidFlowChartLineType lineType, String label) {
        edges.add(lineType.format(addNode(from), addNode(to), label));
    }

}
