package io.github.yuanbug.drawer.parser;

import io.github.yuanbug.drawer.BaseTest;
import io.github.yuanbug.drawer.domain.info.MethodId;
import io.github.yuanbug.drawer.domain.info.MethodInfo;
import io.github.yuanbug.drawer.test.impl.ZeroGetter;
import io.github.yuanbug.drawer.utils.AnswerCheckUtils;
import io.github.yuanbug.drawer.utils.JacksonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author yuanbug
 */
class OverrideParseTest extends BaseTest {

    @Test
    void testParseZeroGetter() throws NoSuchMethodException {
        MethodInfo methodInfo = methodParser.parseMethod(MethodId.from(ZeroGetter.class.getMethod("getZero")).toString());

        assertNotNull(methodInfo);
        JacksonUtils.println(methodInfo, true);
        AnswerCheckUtils.check(methodInfo, "answers/ZeroGetter#getZero().json");
    }

}
