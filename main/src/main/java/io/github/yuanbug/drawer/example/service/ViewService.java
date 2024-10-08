package io.github.yuanbug.drawer.example.service;

import io.github.yuanbug.drawer.domain.ast.AstIndex;
import io.github.yuanbug.drawer.domain.info.MethodCalling;
import io.github.yuanbug.drawer.domain.info.MethodId;
import io.github.yuanbug.drawer.domain.info.MethodInfo;
import io.github.yuanbug.drawer.domain.view.graph.method.MethodCallingView;
import io.github.yuanbug.drawer.domain.view.graph.method.MethodLinkView;
import io.github.yuanbug.drawer.domain.view.graph.method.MethodListItemView;
import io.github.yuanbug.drawer.domain.view.graph.method.MethodView;
import io.github.yuanbug.drawer.example.config.WebViewConfig;
import io.github.yuanbug.drawer.example.utils.MermaidFlowChartGenerator;
import io.github.yuanbug.drawer.example.utils.MermaidFlowChartLineType;
import io.github.yuanbug.drawer.parser.MethodParser;
import io.github.yuanbug.drawer.utils.StopwatchTimer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * @author yuanbug
 */
@Slf4j
@Service
public class ViewService {

    private final MethodParser methodParser;
    private final WebViewConfig webViewConfig;

    @Getter
    private final List<MethodListItemView> methodList;

    public ViewService(MethodParser methodParser, WebViewConfig webViewConfig, AstIndex astIndex) {
        this.methodParser = methodParser;
        this.webViewConfig = webViewConfig;
        log.info("开始构建方法列表");
        StopwatchTimer timer = StopwatchTimer.start();
        var methods = webViewConfig.getMethodListLoader().apply(astIndex);
        log.info("共计{}个方法，方法列表获取耗时{}ms", methods.size(), timer.next());
        this.methodList = methods.stream()
                .sorted(webViewConfig.getMethodListSorter())
                .toList();
        log.info("方法列表构建完成，排序耗时{}ms", timer.next());
    }

    public MethodLinkView getMethodLink(String methodId) {
        MethodInfo methodInfo = methodParser.parseMethod(methodId);
        return MethodLinkView.builder()
                .rootMethodId(methodId)
                .methods(flatMethods(methodInfo))
                .callings(flatCallings(methodInfo))
                .recursions(flatRecursions(methodInfo))
                .overrides(flatOverrides(methodInfo))
                .build();
    }

    private <T, R> R flatMethodInfo(MethodInfo methodInfo, Supplier<R> defaultResultSupplier, BiConsumer<MethodInfo, R> accumulator) {
        R result = defaultResultSupplier.get();
        Queue<MethodInfo> queue = new LinkedList<>();
        Set<String> finished = new HashSet<>();
        queue.add(methodInfo);
        while (!queue.isEmpty()) {
            MethodInfo current = queue.poll();
            String id = current.getId().toString();
            if (finished.contains(id)) {
                continue;
            }
            accumulator.accept(current, result);
            queue.addAll(current.getOverrides());
            queue.addAll(current.getDependencies().stream().map(MethodCalling::getCallee).filter(Objects::nonNull).toList());
            finished.add(id);
        }
        return result;
    }

    private Map<String, MethodView> flatMethods(MethodInfo methodInfo) {
        return flatMethodInfo(
                methodInfo,
                HashMap::new,
                (method, result) -> result.put(method.getId().toString(), webViewConfig.mapMethodInfoToMethodView(method))
        );
    }

    private List<MethodCallingView> flatCallings(MethodInfo methodInfo) {
        return flatMethodInfo(
                methodInfo,
                ArrayList::new,
                (method, result) -> result.addAll(method.getDependencies().stream()
                        .filter(calling -> null != calling.getCallee())
                        .map(calling -> MethodCallingView.builder()
                                .from(method.getId().toString())
                                .to(calling.getCallee().getId().toString())
                                .type(calling.getCallingType())
                                .build())
                        .toList())
        );
    }

    private List<MethodCallingView> flatRecursions(MethodInfo methodInfo) {
        return flatMethodInfo(
                methodInfo,
                ArrayList::new,
                (method, result) -> method.getDependencies().stream()
                        .filter(calling -> StringUtils.isNotBlank(calling.getRecursiveAt()))
                        .forEach(calling -> result.add(MethodCallingView.builder()
                                .from(method.getId().toString())
                                .to(calling.getRecursiveAt())
                                .type(calling.getCallingType())
                                .build()))
        );
    }

    private Map<String, List<String>> flatOverrides(MethodInfo methodInfo) {
        return flatMethodInfo(
                methodInfo,
                HashMap::new,
                (method, result) -> {
                    List<String> overrides = method.getOverrides().stream()
                            .map(MethodInfo::getId)
                            .map(MethodId::toString)
                            .toList();
                    if (overrides.isEmpty()) {
                        return;
                    }
                    result.computeIfAbsent(method.getId().toString(), k -> new ArrayList<>()).addAll(overrides);
                }
        );
    }

    /**
     * TODO 考虑用subgraph做一下按类分组
     */
    public String getMermaid(String methodId) {
        MermaidFlowChartGenerator generator = new MermaidFlowChartGenerator();
        MethodLinkView methodLink = getMethodLink(methodId);
        generator.addNode(methodId);
        methodLink.getCallings().forEach(calling -> generator.addEdge(calling.getFrom(), calling.getTo()));
        methodLink.getRecursions().forEach(calling -> generator.addEdge(calling.getFrom(), calling.getTo(), MermaidFlowChartLineType.DOTTED, "递归"));
        methodLink.getOverrides().forEach((parent, subs) -> subs.forEach(sub -> generator.addEdge(parent, sub, MermaidFlowChartLineType.MULTI_CIRCLE, "实现")));
        return generator.getChart();
    }

}
