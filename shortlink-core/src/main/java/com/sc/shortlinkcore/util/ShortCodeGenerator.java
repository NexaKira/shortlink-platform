package com.sc.shortlinkcore.util;

import java.security.SecureRandom;

/**
 * 已弃用
 */
public class ShortCodeGenerator {

    // 可用字符集合：小写字母 + 数字
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 6;   // 短码长度
    private static final SecureRandom random = new SecureRandom();  // 安全的随机数生成器

    public static String generate() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            // 随机从 CHARACTERS 里取一个字符
            char c = CHARACTERS.charAt(random.nextInt(CHARACTERS.length()));
            sb.append(c);
        }
        return sb.toString();
    }
}