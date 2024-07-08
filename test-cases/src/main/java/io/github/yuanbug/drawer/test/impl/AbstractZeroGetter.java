package io.github.yuanbug.drawer.test.impl;

/**
 * @author yuanbug
 */
public abstract class AbstractZeroGetter implements ZeroGetter {

    @Override
    public int getZero() {
        return this.doGetZero();
    }

    protected abstract int doGetZero();

}
