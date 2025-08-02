package org.zinc_lang;

import com.github.tommyettinger.ds.BooleanDeque;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Preprocessor {

    public static Pattern pattern = Pattern.compile("#(ifcfg|else|endif)(?:\\s+(?<name>\\w+)\\s*=\\s*\\\"(?<value>[^\\\"]*)\\\")?\\s*");
    public static Pattern escapePattern = Pattern.compile("(?<!\\\\)\\\\([nrtfb\"'])");

    public static boolean query(String key, String value) {
        if ("BUILD_OS".equals(key)) {
            String os = System.getProperty("os.name").toLowerCase();
            String baseOs;

            if (os.contains("win")) {
                baseOs = "windows";
            } else if (os.contains("mac") || os.contains("darwin")) {
                baseOs = "macos";
            } else if (os.contains("nix") || os.contains("nux")) {
                baseOs = "linux";
            } else if (os.contains("sunos") || os.contains("solaris")) {
                baseOs = "solaris";
            } else if (os.contains("freebsd")) {
                baseOs = "freebsd";
            } else if (os.contains("openbsd")) {
                baseOs = "openbsd";
            } else if (os.contains("netbsd")) {
                baseOs = "netbsd";
            } else if (os.contains("dragonfly")) {
                baseOs = "dragonflybsd";
            } else if (os.contains("hp-ux")) {
                baseOs = "hpux";
            } else if (os.contains("android")) {
                baseOs = "android";
            } else {
                baseOs = "unknown";
            }
            return baseOs.equals(value);
        }
        return value.equals(System.getenv(key));
    }

    public static String process(String source) {
        BooleanDeque conditions = new BooleanDeque();
        conditions.defaultValue = true;

        Matcher escapeMatcher = escapePattern.matcher(source);
        StringBuilder result = new StringBuilder();

        while (escapeMatcher.find()) {
            String replacement = switch (escapeMatcher.group(1)) {
                case "n" -> "\n";
                case "r" -> "\r";
                case "t" -> "\t";
                case "f" -> "\f";
                case "b" -> "\b";
                case "\"" -> "\"";
                case "'" -> "'";
                default -> escapeMatcher.group(0);
            };
            escapeMatcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        escapeMatcher.appendTail(result);
        source = result.toString();

        StringBuilder sb = new StringBuilder();

        source.lines().forEach(line -> {
            Matcher matcher = pattern.matcher(line);

            if (matcher.matches()) {
                if (line.startsWith("#ifcfg")) {
                    conditions.addLast(query(matcher.group("name"), matcher.group("value")));
                } else if (line.startsWith("#elsecfg")) {
                    boolean negative = !conditions.peekLast();
                    conditions.removeLast();
                    conditions.addLast(negative);
                } else if (line.startsWith("#endif")) {
                    conditions.removeLast();
                }
            } else if (conditions.peekLast()){
                sb.append(line);
                sb.append('\n');
            }
        });

        if (!conditions.isEmpty()) {
            System.err.printf("[Preprocessor] %d conditions were not closed.\n", conditions.size());
        }

        return sb.toString();
    }

}
