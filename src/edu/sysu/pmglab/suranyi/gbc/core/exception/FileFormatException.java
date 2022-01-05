package edu.sysu.pmglab.suranyi.gbc.core.exception;

/**
 * @Data        :2021/02/07
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :构建 GTB 时异常
 */

public class FileFormatException extends RuntimeException {
    public FileFormatException(String message) {
        super(message);
    }
}
