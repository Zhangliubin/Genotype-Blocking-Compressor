package edu.sysu.pmglab.suranyi.gbc.setup.command;

/**
 * @author suranyi
 * @description
 */

class Coordinate {
    final String chromosome;
    final int startPos;
    final int endPos;

    public Coordinate(String chromosome, int startPos, int endPos) {
        this.chromosome = chromosome;
        this.startPos = startPos;
        this.endPos = endPos;
    }
}
