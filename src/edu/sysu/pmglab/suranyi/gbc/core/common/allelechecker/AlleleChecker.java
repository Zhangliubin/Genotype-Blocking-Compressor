package edu.sysu.pmglab.suranyi.gbc.core.common.allelechecker;

/**
 * @author suranyi
 * @description
 */

public interface AlleleChecker {
    /**
     * 碱基是否可以认为相同
     */
    boolean isEqual(double AC11, double AC12, double AC21, double AC22);
}
