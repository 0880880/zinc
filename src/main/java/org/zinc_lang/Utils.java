package org.zinc_lang;

public class Utils {

    private static final String[] ESC = new String[128];
    static {
        ESC['\n'] = "\\n"; ESC['\r'] = "\\r"; ESC['\t'] = "\\t";
        ESC['\b'] = "\\b"; ESC['\f'] = "\\f"; ESC['\\'] = "\\\\";
    }

    public static boolean equalsAny(String target, String... candidates) {
        for (String candidate : candidates) {
            if (target.equals(candidate)) return true;
        }
        return false;
    }

    public static String escapeControlChars(String in) {
        int len = in.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = in.charAt(i);
            // skip a newline if it's the last character
            if (c == '\n' && i == len - 1) continue;
            if (c < 128 && ESC[c] != null) {
                sb.append(ESC[c]);
            } else if (Character.isISOControl(c)) {
                sb.append("\\u")
                        .append(String.format("%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

}
