package io.github.yuanbug.drawer.domain.info;

import io.github.yuanbug.drawer.domain.CodeModule;

/**
 * @author yuanbug
 */
public enum MethodCallingType {
    /**
     * 类自身的方法
     */
    SELF,
    /**
     * 父类的方法
     */
    SUPER,
    /**
     * 同模块其它类的方法
     */
    BROTHER,
    /**
     * 其它模块的方法
     */
    OUT,
    /**
     * JDK的方法
     */
    JDK,
    /**
     * 类库、框架的方法
     *
     * @apiNote 不属于已配置 {@link CodeModule} 的方法均属于此类
     */
    LIBRARY,
    ;

}
