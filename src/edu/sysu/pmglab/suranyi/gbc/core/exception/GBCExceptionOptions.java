package edu.sysu.pmglab.suranyi.gbc.core.exception;

import edu.sysu.pmglab.suranyi.check.exception.*;

/**
 * @author suranyi
 * @description 普通异常参数
 */
public enum GBCExceptionOptions implements IRuntimeExceptionOptions {
    /**
     * 普通异常类型
     */
    FileFormatException,
    GTBComponentException;

    @Override
    public void throwException(String reason) {
        switch (this) {
            case FileFormatException:
                throw new FileFormatException(reason);
            case GTBComponentException:
                throw new GTBComponentException(reason);
            default:
                throw new AssertionError(reason);
        }
    }
}