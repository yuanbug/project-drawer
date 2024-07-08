package io.github.yuanbug.drawer.test.simple;

/**
 * @author yuanbug
 */
public class SimpleClass {

    public String simpleMethod(int qwe, String asd) {
        return asd + qwe;
    }

    public void doNothing() {
        // do nothing
    }

    public String callUtils() {
        return SimpleUtils.getRandomString();
    }

    public void forLoop() {
        for (int i = 0; i < 100; i++) {
            System.out.println(i + ": " + callUtils());
        }
        System.out.println(SimpleUtils.getRandomDouble());
    }

    public int recurse1(int n) {
        int c = 0;
        for (int i = 0; i < n; i++) {
            if (i % 2 == 0) {
                c += recurse2(i);
            }
            c += i;
        }
        return c;
    }

    public int recurse2(int i) {
        if (i + 1 % 7 == 0) {
            return i;
        }
        return recurse3(i);
    }

    public int recurse3(int i) {
        if (i * (i - 1) % 3 == 0) {
            return i;
        }
        return recurse1(i - 1);
    }

}
