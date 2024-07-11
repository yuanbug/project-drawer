package io.github.yuanbug.drawer.parser.module;

import io.github.yuanbug.drawer.domain.CodeModule;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author yuanbug
 */
public interface ModuleParser {

    default List<CodeModule> parseModules(@Nonnull File root) {
        return parseModules(root, false, module -> true);
    }

    List<CodeModule> parseModules(@Nonnull File root, boolean parseDependency, @Nonnull Predicate<CodeModule> predicate);

    boolean isModuleDir(File file);

}
