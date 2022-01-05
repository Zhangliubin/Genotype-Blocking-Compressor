package edu.sysu.pmglab.suranyi.gbc.core.build;

/**
 * @Data        :2021/02/07
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :不同样本名文件合并异常
 */

public class RebuildException extends UnsupportedOperationException{
    public RebuildException(String message){
        super(message);
    }
}
