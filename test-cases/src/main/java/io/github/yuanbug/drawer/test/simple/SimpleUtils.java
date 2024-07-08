package io.github.yuanbug.drawer.test.simple;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * @author yuanbug
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SimpleUtils {

    public static String getRandomString() {
        return RandomStringUtils.randomAlphanumeric(32);
    }

    public static double getRandomDouble() {
        return Math.random();
    }

}
