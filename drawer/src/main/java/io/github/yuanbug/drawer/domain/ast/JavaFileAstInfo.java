package io.github.yuanbug.drawer.domain.ast;

import com.github.javaparser.ast.CompilationUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.io.File;

/**
 * @author yuanbug
 */
@Getter
@Builder
@AllArgsConstructor
public class JavaFileAstInfo {

    /**
     * 对应的Java文件
     */
    public final File file;

    /**
     * 代码对应的抽象语法树
     */
    public final CompilationUnit ast;

    /**
     * 所在模块
     */
    public final String moduleName;

}
