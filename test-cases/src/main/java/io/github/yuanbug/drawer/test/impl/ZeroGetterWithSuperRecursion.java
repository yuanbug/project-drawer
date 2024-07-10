package io.github.yuanbug.drawer.test.impl;

/**
 * @author yuanbug
 */
public class ZeroGetterWithSuperRecursion extends AbstractZeroGetter {

    @Override
    public int getZero() {
        int zero = super.getZero();
        if (zero > 0) {
            return -zero;
        }
        return 0;
    }

    @Override
    protected int doGetZero() {
        return 0;
    }

}
