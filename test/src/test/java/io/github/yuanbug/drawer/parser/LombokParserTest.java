package io.github.yuanbug.drawer.parser;

import io.github.yuanbug.drawer.BaseTest;
import io.github.yuanbug.drawer.config.AstParsingConfig;
import io.github.yuanbug.drawer.domain.info.MethodId;
import io.github.yuanbug.drawer.domain.info.MethodInfo;
import io.github.yuanbug.drawer.domain.info.TypeLombokInfo;
import io.github.yuanbug.drawer.parser.lombok.LombokParser;
import io.github.yuanbug.drawer.test.lombok.DataAnnotationOnTypeParse;
import io.github.yuanbug.drawer.test.lombok.GetterAnnotationOnTypeParse;
import io.github.yuanbug.drawer.test.lombok.SetterAnnotationOnFieldParse;
import io.github.yuanbug.drawer.utils.AnswerCheckUtils;
import io.github.yuanbug.drawer.utils.JacksonUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yuanbug
 */
public class LombokParserTest extends BaseTest {

    private final LombokParser lombokParser = new LombokParser(config);

    @Override
    public AstParsingConfig defaultProjectDrawerConfig() {
        return new TestAstParsingConfig() {
            @Override
            public boolean enableLombokParser() {
                return true;
            }
        };
    }

    @Test
    void testParseGetterAnnotation() throws NoSuchMethodException {
        TypeLombokInfo info = lombokParser.parse(astIndex.getTypeDeclarationByClassName(GetterAnnotationOnTypeParse.class.getName()));
        Set<String> methods = info.getMethods().keySet();

        assertEquals(5, methods.size());
        assertTrue(methods.contains(MethodId.toString(GetterAnnotationOnTypeParse.class.getDeclaredMethod("getName"))));
        assertTrue(methods.contains(MethodId.toString(GetterAnnotationOnTypeParse.class.getDeclaredMethod("getNumber"))));
        assertTrue(methods.contains(MethodId.toString(GetterAnnotationOnTypeParse.class.getDeclaredMethod("getUpperCaseField"))));
        assertTrue(methods.contains(MethodId.toString(GetterAnnotationOnTypeParse.class.getDeclaredMethod("get_underlineField"))));
        assertFalse(methods.contains(MethodId.toString(GetterAnnotationOnTypeParse.class.getDeclaredMethod("getId"))));
        assertFalse(methods.contains(MethodId.toString(GetterAnnotationOnTypeParse.class.getDeclaredMethod("getNameAndNumber"))));
    }

    @Test
    void testParseSetterAnnotation() throws NoSuchMethodException {
        TypeLombokInfo info = lombokParser.parse(astIndex.getTypeDeclarationByClassName(SetterAnnotationOnFieldParse.class.getName()));
        Set<String> methods = info.getMethods().keySet();

        assertEquals(1, methods.size());
        assertFalse(methods.contains(new MethodId(SetterAnnotationOnFieldParse.class.getName(), "setId", List.of("java.lang.String")).toString()));
        assertFalse(methods.contains(MethodId.toString(SetterAnnotationOnFieldParse.class.getDeclaredMethod("setAge", Integer.class))));
        assertTrue(methods.contains(MethodId.toString(SetterAnnotationOnFieldParse.class.getDeclaredMethod("setName", String.class))));
    }

    @Test
    void testParseDataAnnotation() throws NoSuchMethodException {
        TypeLombokInfo info = lombokParser.parse(astIndex.getTypeDeclarationByClassName(DataAnnotationOnTypeParse.class.getName()));
        Set<String> methods = info.getMethods().keySet();

        assertEquals(4, methods.size());
        assertFalse(methods.contains(MethodId.toString(DataAnnotationOnTypeParse.class.getDeclaredMethod("getId"))));
        assertTrue(methods.contains(MethodId.toString(DataAnnotationOnTypeParse.class.getDeclaredMethod("setId", String.class))));
        assertTrue(methods.contains(MethodId.toString(DataAnnotationOnTypeParse.class.getDeclaredMethod("getName"))));
        assertFalse(methods.contains(MethodId.toString(DataAnnotationOnTypeParse.class.getDeclaredMethod("setName", String.class))));
        assertTrue(methods.contains(MethodId.toString(DataAnnotationOnTypeParse.class.getDeclaredMethod("getAge"))));
        assertTrue(methods.contains(MethodId.toString(DataAnnotationOnTypeParse.class.getDeclaredMethod("setAge", Integer.class))));
    }

    @Test
    void testParseGetNameAndNumberMethodInfo() throws NoSuchMethodException {
        MethodInfo methodInfo = methodParser.parseMethod(MethodId.toString(GetterAnnotationOnTypeParse.class.getDeclaredMethod("getNameAndNumber")));

        assertNotNull(methodInfo);
        JacksonUtils.println(methodInfo, true);
        AnswerCheckUtils.check(methodInfo, "answers/GetterAnnotationOnTypeParse#getNameAndNumber().json");
    }

}
