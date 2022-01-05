package edu.sysu.pmglab.suranyi.gbc.core.edit;

/**
 * @Data        :2021/02/07
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :编辑异常
 */

public class EditException extends UnsupportedOperationException{
    public EditException(String message){
        super(message);
    }
}
