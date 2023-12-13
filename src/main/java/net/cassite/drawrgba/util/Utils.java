package net.cassite.drawrgba.util;

public class Utils {
    private Utils() {
    }

    public static boolean isValid255(String s) {
        return isValidNum(s, 0, 255);
    }

    public static boolean isValidNum(String s, int min, int max) {
        try {
            var n = Integer.parseInt(s);
            return min <= n && n <= max;
        } catch (NumberFormatException ignore) {
            return false;
        }
    }
}
