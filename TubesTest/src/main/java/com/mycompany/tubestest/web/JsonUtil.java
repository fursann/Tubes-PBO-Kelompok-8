package com.mycompany.tubestest.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utilitas JSON ringan tanpa library eksternal. */
public final class JsonUtil {

    private JsonUtil() {
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    public static String toJson(Map<String, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(escape(entry.getKey())).append("\":");
            appendValue(sb, entry.getValue());
        }
        sb.append('}');
        return sb.toString();
    }

    public static String toJson(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            appendValue(sb, list.get(i));
        }
        sb.append(']');
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void appendValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Boolean b) {
            sb.append(b);
        } else if (value instanceof Number n) {
            sb.append(n);
        } else if (value instanceof Map<?, ?> map) {
            sb.append(toJson((Map<String, ?>) map));
        } else if (value instanceof List<?> list) {
            sb.append(toJson(list));
        } else {
            sb.append('"').append(escape(String.valueOf(value))).append('"');
        }
    }

    public static Map<String, String> parseObject(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        if (json == null || json.isBlank()) {
            return result;
        }
        String body = json.trim();
        if (body.startsWith("{")) {
            body = body.substring(1, body.length() - 1).trim();
        }
        Pattern pattern = Pattern.compile(
                "\"([^\"]+)\"\\s*:\\s*(\"((?:\\\\.|[^\"\\\\])*)\"|true|false|null|-?\\d+(?:\\.\\d+)?)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(body);
        while (matcher.find()) {
            String key = matcher.group(1);
            String quoted = matcher.group(3);
            String raw = quoted != null ? unescape(quoted) : matcher.group(2);
            result.put(key, raw);
        }
        return result;
    }

    private static String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    public static String getString(Map<String, String> data, String key, String jsonFallback) {
        String fromMap = data.get(key);
        if (fromMap != null && !fromMap.isBlank()) {
            return fromMap;
        }
        if (jsonFallback == null) {
            return null;
        }
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(jsonFallback);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static List<Integer> parseIntArray(String json, String key) {
        List<Integer> values = new ArrayList<>();
        if (json == null) {
            return values;
        }
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[([^\\]]*)]");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return values;
        }
        String inner = matcher.group(1);
        if (inner.isBlank()) {
            return values;
        }
        for (String part : inner.split(",")) {
            try {
                values.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return values;
    }

    public static boolean parseBoolean(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(json == null ? "" : json);
        return matcher.find() && "true".equalsIgnoreCase(matcher.group(1));
    }

    public static double parseDouble(String json, String key) {
        if (json == null || json.isBlank()) {
            return 0;
        }
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return 0;
        }
        try {
            return Double.parseDouble(matcher.group(1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
