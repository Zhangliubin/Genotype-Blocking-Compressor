package edu.sysu.pmglab.suranyi.gbc.core.extract;

/**
 * @Data        :2021/02/07
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :构建任务时没有指定输入文件名错误
 */

public class ExtractException extends UnsupportedOperationException{
    public ExtractException(String message){
        super(message);
    }
}
