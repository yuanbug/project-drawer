package io.github.yuanbug.drawer.parser.module;

import io.github.yuanbug.drawer.domain.CodeModule;

import java.io.File;

/**
 * @author yuanbug
 */
public class ModuleParserDelegate extends SimpleModuleParser {

    private final MavenModuleParser mavenModuleParser = new MavenModuleParser();

    @Override
    protected CodeModule parseModule(File dir, boolean parseDependency) {
        if (mavenModuleParser.getPom(dir).isPresent()) {
            return mavenModuleParser.parseModule(dir, parseDependency);
        }
        return super.parseModule(dir, parseDependency);
    }

}
