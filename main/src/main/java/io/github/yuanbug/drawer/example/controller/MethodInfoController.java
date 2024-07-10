package io.github.yuanbug.drawer.example.controller;

import io.github.yuanbug.drawer.domain.view.graph.method.MethodLinkView;
import io.github.yuanbug.drawer.domain.view.graph.method.MethodListItemView;
import io.github.yuanbug.drawer.example.service.ViewService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author yuanbug
 */
@Slf4j
@RestController
@RequestMapping("/method-info")
public class MethodInfoController {

    @Resource
    private ViewService viewService;

    @GetMapping("/list")
    public List<MethodListItemView> getMethodList() {
        return viewService.getMethodList();
    }

    @GetMapping("/method-link")
    public MethodLinkView getMethodLink(String methodId) {
        try {
            return viewService.getMethodLink(methodId);
        } catch (Throwable e) {
            log.error("{} 解析异常", methodId, e);
            throw new IllegalStateException("无法解析方法" + methodId);
        }
    }

}
