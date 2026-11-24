package kr.word.detective.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 의존성 없는 최소 JSON 파서/작성기. 객체는 Map, 배열은 List, 숫자는 Double, 문자열은 String. */
public final class Json {
    private final String s;
    private int i;

    private Json(String s) { this.s = s; }

    public static Object parse(String text) {
        if (text == null) return null;
        Json p = new Json(text.trim());
        try {
            Object v = p.value();
            return v;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

    private Object value() {
        ws();
        if (i >= s.length()) throw new IllegalStateException("eof");
        char c = s.charAt(i);
        if (c == '{') return obj();
        if (c == '[') return arr();
        if (c == '"') return str();
        if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
        if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
        if (s.startsWith("null", i)) { i += 4; return null; }
        return num();
    }

    private Map<String, Object> obj() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        i++; ws();
        if (s.charAt(i) == '}') { i++; return m; }
        while (true) {
            ws();
            String k = str();
            ws();
            if (s.charAt(i) != ':') throw new IllegalStateException("colon");
            i++;
            m.put(k, value());
            ws();
            char c = s.charAt(i++);
            if (c == '}') return m;
            if (c != ',') throw new IllegalStateException("comma");
        }
    }

    private List<Object> arr() {
        List<Object> l = new ArrayList<Object>();
        i++; ws();
        if (s.charAt(i) == ']') { i++; return l; }
        while (true) {
            l.add(value());
            ws();
            char c = s.charAt(i++);
            if (c == ']') return l;
            if (c != ',') throw new IllegalStateException("comma");
        }
    }

    private String str() {
        if (s.charAt(i) != '"') throw new IllegalStateException("quote");
        i++;
        StringBuilder b = new StringBuilder();
        while (true) {
            char c = s.charAt(i++);
            if (c == '"') return b.toString();
            if (c == '\\') {
                char e = s.charAt(i++);
                switch (e) {
                    case 'n': b.append('\n'); break;
                    case 't': b.append('\t'); break;
                    case 'r': b.append('\r'); break;
                    case 'b': b.append('\b'); break;
                    case 'f': b.append('\f'); break;
                    case 'u': b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; break;
                    default: b.append(e);
                }
            } else {
                b.append(c);
            }
        }
    }

    private Double num() {
        int st = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        if (st == i) throw new IllegalStateException("num");
        return Double.valueOf(s.substring(st, i));
    }

    // ---------- 읽기 도우미 ----------

    @SuppressWarnings("unchecked")
    public static Object path(Object root, String... keys) {
        Object cur = root;
        for (String k : keys) {
            if (!(cur instanceof Map)) return null;
            cur = ((Map<String, Object>) cur).get(k);
        }
        return cur;
    }

    /** 배열이면 그대로, 객체 하나면 한 칸짜리 목록, 없으면 빈 목록. */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> list(Object v) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        if (v instanceof List) {
            for (Object o : (List<Object>) v) if (o instanceof Map) out.add((Map<String, Object>) o);
        } else if (v instanceof Map) {
            out.add((Map<String, Object>) v);
        }
        return out;
    }

    /** 숫자/문자 어느 쪽이든 문자열로. 정수형 숫자는 소수점 없이. 빈 값은 "". */
    public static String str(Map<String, Object> m, String k) {
        Object v = m == null ? null : m.get(k);
        if (v == null) return "";
        if (v instanceof Double) {
            double d = (Double) v;
            if (d == Math.rint(d) && Math.abs(d) < 1e15) return String.valueOf((long) d);
            return String.valueOf(d);
        }
        return String.valueOf(v).trim();
    }

    /** 정수로 읽기. 비었거나 숫자가 아니면 null. */
    public static Integer integer(Map<String, Object> m, String k) {
        String t = str(m, k);
        if (t.isEmpty()) return null;
        try { return (int) Math.round(Double.parseDouble(t)); } catch (NumberFormatException e) { return null; }
    }

    // ---------- 쓰기 ----------

    public static String quote(String v) {
        if (v == null) return "null";
        StringBuilder b = new StringBuilder("\"");
        for (int k = 0; k < v.length(); k++) {
            char c = v.charAt(k);
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c);
            }
        }
        return b.append('"').toString();
    }
}
