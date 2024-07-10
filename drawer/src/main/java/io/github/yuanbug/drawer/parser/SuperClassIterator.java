package io.github.yuanbug.drawer.parser;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import io.github.yuanbug.drawer.domain.ast.AstIndex;
import io.github.yuanbug.drawer.utils.AstUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author yuanbug
 */
public class SuperClassIterator implements Iterator<ClassOrInterfaceDeclaration> {

    private final List<ClassOrInterfaceDeclaration> parents;
    private int cursor;

    public SuperClassIterator(TypeDeclaration<?> type, AstIndex astIndex) {
        cursor = 0;
        parents = parseParents(type, astIndex);
    }

    @Override
    public boolean hasNext() {
        return cursor < parents.size();
    }

    @Override
    public ClassOrInterfaceDeclaration next() {
        if (!hasNext()) {
            return null;
        }
        return parents.get(cursor++);
    }

    public Stream<ClassOrInterfaceDeclaration> stream() {
        var result = parents.stream().skip(cursor);
        cursor = parents.size();
        return result;
    }

    private List<ClassOrInterfaceDeclaration> parseParents(TypeDeclaration<?> type, AstIndex astIndex) {
        if (null == astIndex || !(type instanceof ClassOrInterfaceDeclaration classOrInterfaceDeclaration)) {
            return Collections.emptyList();
        }
        if (classOrInterfaceDeclaration.isInterface()) {
            return Collections.emptyList();
        }
        List<ClassOrInterfaceDeclaration> result = new ArrayList<>(4);
        ClassOrInterfaceDeclaration currentDeclaration = classOrInterfaceDeclaration;
        var extendedTypes = currentDeclaration.getExtendedTypes();
        while (!extendedTypes.isEmpty()) {
            TypeDeclaration<?> typeDeclaration = astIndex.getTypeDeclarationByClassName(AstUtils.getName(extendedTypes.get(0)));
            if (!(typeDeclaration instanceof ClassOrInterfaceDeclaration declaration)) {
                break;
            }
            currentDeclaration = declaration;
            result.add(currentDeclaration);
            extendedTypes = currentDeclaration.getExtendedTypes();
        }
        return Collections.unmodifiableList(result);
    }

}
