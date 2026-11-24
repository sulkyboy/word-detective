package kr.word.detective.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 사진에서 읽은 글자 줄(또는 붙여넣은 글)을 '영어 단어 – 한국어 뜻' 짝으로 바꾼다. */
public final class Lines {
    private Lines() {}

    /** 사진 속 글자 한 줄과 그 위치 */
    public static final class Box {
        public final String text;
        public final int left, top, right, bottom;
        public Box(String text, int left, int top, int right, int bottom) {
            this.text = text; this.left = left; this.top = top; this.right = right; this.bottom = bottom;
        }
        int mid() { return (top + bottom) / 2; }
    }

    /** 사진: 같은 높이에 있는 글자들을 한 줄로 모은 다음(영어 칸과 뜻 칸이 따로 읽히기 때문) 짝을 찾는다. */
    public static List<Word> fromBoxes(List<Box> boxes) {
        List<Box> sorted = new ArrayList<Box>(boxes);
        Collections.sort(sorted, new Comparator<Box>() {
            public int compare(Box a, Box b) { return a.mid() - b.mid(); }
        });
        List<List<Box>> rows = new ArrayList<List<Box>>();
        int rowTop = 0, rowBottom = 0;
        for (Box b : sorted) {
            List<Box> row = rows.isEmpty() ? null : rows.get(rows.size() - 1);
            if (row != null && b.mid() >= rowTop && b.mid() <= rowBottom) {
                row.add(b);
            } else {
                row = new ArrayList<Box>();
                row.add(b);
                rows.add(row);
                // 줄의 높이는 첫 글자 덩어리 기준 (가운데 60%만 같은 줄로 본다)
                int h = b.bottom - b.top;
                rowTop = b.top + h / 5;
                rowBottom = b.bottom - h / 5;
            }
        }
        List<String> lines = new ArrayList<String>();
        for (List<Box> row : rows) {
            Collections.sort(row, new Comparator<Box>() {
                public int compare(Box a, Box b) { return a.left - b.left; }
            });
            StringBuilder s = new StringBuilder();
            for (Box b : row) s.append(s.length() > 0 ? "  " : "").append(b.text);
            lines.add(s.toString());
        }
        return fromLines(lines);
    }

    public static List<Word> fromText(String text) {
        List<String> lines = new ArrayList<String>();
        for (String l : text.split("\\r?\\n")) lines.add(l);
        return fromLines(lines);
    }

    /** 마지막으로 사진에서 모은 줄들 (시험 화면에서 확인용) */
    public static List<String> lastLines = new ArrayList<String>();

    // 단어장 맨 위에 흔히 있는 제목 줄의 첫 낱말
    private static final String HEADERS = "|day|unit|lesson|chapter|part|step|name|class|date|score|no|word|words|test|voca|vocabulary|english|korean|meaning|";

    static List<Word> fromLines(List<String> lines) {
        lastLines = new ArrayList<String>(lines);
        List<Word> out = new ArrayList<Word>();
        Word open = null;   // 뜻이 아직 없는 단어 (뜻이 다음 줄에 있는 경우)
        for (String raw : lines) {
            String line = clean(raw);
            if (line.isEmpty()) continue;
            boolean any = false;
            // 한 줄에서 '영어 … 뜻' 을 차례로 떼어 낸다 (한 줄에 두 짝이 있어도 됨)
            String rest = line;
            while (!rest.isEmpty()) {
                int[] head = headWord(rest);
                if (head == null) break;
                String en = english(rest.substring(head[0], head[1]));
                String after = rest.substring(head[1]);
                int h = firstHangul(after);
                if (h < 0) {
                    // 뜻이 없는 줄: 영어만 있는 줄이면 다음 줄의 뜻을 기다린다
                    if (!any && !en.isEmpty() && !isHeader(en) && firstHangul(rest) < 0) {
                        open = new Word(en, "");
                        out.add(open);
                        any = true;
                    }
                    break;
                }
                // 영어와 뜻 사이(발음기호·품사가 잘못 읽힌 글자)는 버린다. 뜻은 다음 영어 낱말이 나오기 전까지
                if (h > 0 && after.charAt(h - 1) == '(') h--;     // "(물을) 마시다" 의 여는 괄호는 뜻의 일부
                String tail = after.substring(h);
                int next = nextPair(tail);
                String ko = korean(next < 0 ? tail : tail.substring(0, next));
                if (!en.isEmpty() && !ko.isEmpty() && !isHeader(en)) {
                    out.add(new Word(en, ko));
                    open = null;
                }
                any = true;
                rest = next < 0 ? "" : tail.substring(next);
            }
            if (!any && open != null && firstHangul(line) >= 0 && headWord(line) == null) {
                open.ko = korean(line);
                open = null;
            }
        }
        // 같은 단어가 두 번 읽힌 경우 하나만
        List<Word> uniq = new ArrayList<Word>();
        for (Word w : out) {
            boolean dup = false;
            for (Word u : uniq) if (u.en.equalsIgnoreCase(w.en)) { dup = true; if (u.ko.isEmpty()) u.ko = w.ko; }
            if (!dup) uniq.add(w);
        }
        return uniq;
    }

    private static boolean isHeader(String en) {
        String first = en.toLowerCase(java.util.Locale.US).split(" ")[0];
        return HEADERS.contains("|" + first + "|");
    }

    private static boolean latin(char c) { return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'); }
    private static boolean hangul(char c) { return c >= 0xAC00 && c <= 0xD7A3; }

    private static int firstHangul(String s) {
        for (int i = 0; i < s.length(); i++) if (hangul(s.charAt(i))) return i;
        return -1;
    }

    /** 줄 맨 앞의 영어 낱말(띄어 쓴 숙어 포함)의 {시작, 끝}. 한글이 먼저 나오면 null */
    private static int[] headWord(String s) {
        int i = 0;
        while (i < s.length() && !latin(s.charAt(i))) {
            if (hangul(s.charAt(i))) return null;
            i++;
        }
        if (i >= s.length()) return null;
        int j = i;
        while (j < s.length()) {
            char c = s.charAt(j);
            if (latin(c) || c == ' ' || c == '\'' || c == '-') j++; else break;
        }
        return new int[] {i, j};
    }

    /** 뜻 뒤에 또 다른 '영어 … 뜻' 이 이어지면 그 시작 위치, 아니면 -1 */
    private static int nextPair(String tail) {
        Matcher m = Pattern.compile("\\s[A-Za-z]{2,}").matcher(tail);
        while (m.find()) {
            if (firstHangul(tail.substring(m.end())) >= 0) return m.start() + 1;
        }
        return -1;
    }

    /** 번호, 발음기호, 체크 표시 따위를 지운다 */
    static String clean(String s) {
        String t = s.replace('\t', ' ').replace('|', ' ');
        t = t.replaceAll("\\[[^\\]]*\\]|/[^/]*/", " ");                 // [ǽpl], /æpl/
        t = t.replaceAll("\\([^\\)\\uAC00-\\uD7A3]*\\)", " ");            // (ǽpl) — 한글이 든 괄호는 뜻이므로 둔다
        t = t.replaceAll("^\\s*[\\(\\[]?\\d{1,4}[\\)\\].:]?\\s+", "");   // 1.  (2)  03
        t = t.replaceAll("^[\\s□■☐✓✔●○•·\\-*]+", "");
        return t.trim();
    }

    static String english(String s) {
        String t = s.replaceAll("[^A-Za-z '\\-]", " ");
        // 끝에 붙은 품사 표시(n. v. adj. …)는 뗀다
        t = t.replaceAll("\\s+(n|v|a|adj|adv|prep|conj|pron|vt|vi)\\s*$", "");
        t = t.replaceAll("\\s+", " ").trim();
        t = t.replaceAll("^[\\-' ]+|[\\-' ]+$", "");
        if (t.length() < 2 && !t.equalsIgnoreCase("a") && !t.equals("I")) return "";
        if (t.length() > 40) return "";
        return t;
    }

    static String korean(String s) {
        String t = s.replaceAll("^[\\s\\.:=\\-–—>~\\)\\]]+", "").replaceAll("[\\s\\.:=\\-–—,;]+$", "");
        return t.replaceAll("\\s+", " ").trim();
    }
}
