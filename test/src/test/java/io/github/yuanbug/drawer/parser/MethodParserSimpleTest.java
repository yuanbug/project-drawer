package io.github.yuanbug.drawer.parser;

import com.github.javaparser.ast.body.MethodDeclaration;
import io.github.yuanbug.drawer.BaseTest;
import io.github.yuanbug.drawer.domain.info.MethodId;
import io.github.yuanbug.drawer.domain.info.MethodInfo;
import io.github.yuanbug.drawer.test.simple.SimpleClass;
import io.github.yuanbug.drawer.utils.AnswerCheckUtils;
import io.github.yuanbug.drawer.utils.JacksonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author yuanbug
 */
class MethodParserSimpleTest extends BaseTest {

    @Test
    void whenFindSimpleMethodThenFound() throws NoSuchMethodException {
        MethodDeclaration simpleMethod = methodParser.findMethod(MethodId.from(SimpleClass.class.getMethod("simpleMethod", int.class, String.class))).orElseThrow();
        assertEquals("simpleMethod", simpleMethod.getNameAsString());
        assertEquals(2, simpleMethod.getParameters().size());
    }

    @Test
    void whenFindDoNothingThenFound() throws NoSuchMethodException {
        MethodDeclaration doNothing = methodParser.findMethod(MethodId.from(SimpleClass.class.getMethod("doNothing"))).orElseThrow();
        assertEquals("doNothing", doNothing.getNameAsString());
        assertEquals(0, doNothing.getParameters().size());
    }

    @Test
    void testGetMethodInfo() throws NoSuchMethodException {
        MethodInfo methodInfo = methodParser.parseMethod(MethodId.from(SimpleClass.class.getMethod("forLoop")).toString());
        assertNotNull(methodInfo);
        JacksonUtils.println(methodInfo, true);
        AnswerCheckUtils.check(methodInfo, "answers/SimpleClass#forLoop().json");
    }

    @Test
    void testParseRecursion() throws NoSuchMethodException {
        MethodInfo methodInfo = methodParser.parseMethod(MethodId.from(SimpleClass.class.getMethod("recurse1", int.class)).toString());
        assertNotNull(methodInfo);
        JacksonUtils.println(methodInfo, true);
        AnswerCheckUtils.check(methodInfo, "answers/SimpleClass#recurse1(int).json");
    }

}
