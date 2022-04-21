package edu.sysu.pmglab.gbc.core.gtbcomponent;

import edu.sysu.pmglab.easytools.ByteCode;

/**
 * @author suranyi
 * @description GTB 文件常量
 */

public class GTBConstant {
    /**
     * Structure of GTB:
     *     +--------------------+-------------------------+-----------------------+
     *     | Magic Code (2 byte)| GTBNode Numbers (3 byte)| Reference Information |
     *     +----+---------------+------+------------+-----+-----------------------+
     *     | \n | Subjects Information | Block Data | Block Abstract Information  |
     *     +----+----------------------+------------+-----------------------------+
     */
    public final static int MAGIC_CODE_LENGTH = 2;
    public final static int NODE_NUMBERS_LENGTH = 3;
    public final static byte[] SEPARATOR = new byte[]{ByteCode.NEWLINE};

    public final static String STRUCTURE_OF_GTB = "    +--------------------+-------------------------+-----------------------+\n" +
            "    | Magic Code (" + MAGIC_CODE_LENGTH + " byte)| GTBNode Numbers (" + NODE_NUMBERS_LENGTH + " byte)| Reference Information |\n" +
            "    +----+---------------+------+------------+-----+-----------------------+\n" +
            "    | \\n | Subjects Information | Block Data | Block Abstract Information  |\n" +
            "    +----+----------------------+------------+-----------------------------+";
}