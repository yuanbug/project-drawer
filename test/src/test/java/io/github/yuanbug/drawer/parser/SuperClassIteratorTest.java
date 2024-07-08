package io.github.yuanbug.drawer.parser;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import io.github.yuanbug.drawer.BaseTest;
import io.github.yuanbug.drawer.test.generic.GenericMethodFinding;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yuanbug
 */
public class SuperClassIteratorTest extends BaseTest {

    @Test
    void testGetParentClass() {
        TypeDeclaration<?> root = context.getTypeDeclarationByClassName(GenericMethodFinding.D.class.getName());
        assertNotNull(root);
        assertTrue(root instanceof ClassOrInterfaceDeclaration);
        SuperClassIterator iterator = new SuperClassIterator((ClassOrInterfaceDeclaration) root, context);
        assertEquals("C", iterator.next().getNameAsString());
        assertEquals("B", iterator.next().getNameAsString());
        assertEquals("A", iterator.next().getNameAsString());
        assertNull(iterator.next());
    }

}
