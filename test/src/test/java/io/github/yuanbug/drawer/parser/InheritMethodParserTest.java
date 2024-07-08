package io.github.yuanbug.drawer.parser;

import com.github.javaparser.ast.body.MethodDeclaration;
import io.github.yuanbug.drawer.BaseTest;
import io.github.yuanbug.drawer.domain.info.MethodId;
import io.github.yuanbug.drawer.domain.info.MethodInheritLinkInfo;
import io.github.yuanbug.drawer.test.generic.GenericWithMethodTemplatePattern;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author yuanbug
 */
public class InheritMethodParserTest extends BaseTest {

    @Test
    @SneakyThrows
    void testParseDoHandle() {
        MethodId methodId = MethodId.from(GenericWithMethodTemplatePattern.LoginService.class.getDeclaredMethod("doHandle", GenericWithMethodTemplatePattern.LoginForm.class));
        MethodDeclaration doHandle = methodParser.findMethod(methodId).orElseThrow();

        MethodInheritLinkInfo result = new InheritMethodParser(context).parseParentMethods(doHandle);

        assertEquals(doHandle, result.getCurrent());
        assertEquals(context, result.getContext());
        assertEquals(1, result.getFromExtend().size());
        assertEquals(0, result.getFromImpl().size());
        assertEquals(
                "io.github.yuanbug.drawer.test.generic.GenericWithMethodTemplatePattern$AbstractService#doHandle(T)",
                MethodId.from(result.getFromExtend().get(0)).toString()
        );
    }

    @Test
    @SneakyThrows
    void testParseGetName() {
        MethodId methodId = MethodId.from(GenericWithMethodTemplatePattern.LoginService.class.getDeclaredMethod("getName"));
        MethodDeclaration doHandle = methodParser.findMethod(methodId).orElseThrow();

        MethodInheritLinkInfo result = new InheritMethodParser(context).parseParentMethods(doHandle);

        assertEquals(doHandle, result.getCurrent());
        assertEquals(context, result.getContext());
        assertEquals(0, result.getFromExtend().size());
        assertEquals(1, result.getFromImpl().size());
        assertEquals(
                MethodId.from(GenericWithMethodTemplatePattern.Service.class.getDeclaredMethod("getName")),
                MethodId.from(result.getFromImpl().get(0))
        );
    }

}
