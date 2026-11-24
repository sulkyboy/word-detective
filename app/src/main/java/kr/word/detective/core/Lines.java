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

    /**
     * 사진에서 읽은 글자 덩어리들을 단어–뜻 짝으로.
     * 같은 높이에 있는 덩어리들을 한 줄로 모은 다음(영어 칸과 뜻 칸이 따로 읽히기 때문),
     *  - 예문이 딸린 교재 모양이면: 왼쪽 칸의 문제 단어와 그 줄의 뜻만 가져온다 (예문·해석·발음기호는 버림)
     *  - 단어와 뜻만 나란히 적힌 목록이면: 줄마다 짝을 찾는다
     */
    public static List<Word> fromBoxes(List<Box> boxes) {
        lastBoxes = new ArrayList<Box>(boxes);
        List<List<Box>> rows = rows(boxes);
        List<String> lines = new ArrayList<String>();
        for (List<Box> row : rows) {
            StringBuilder s = new StringBuilder();
            for (Box b : row) s.append(s.length() > 0 ? "  " : "").append(b.text);
            lines.add(s.toString());
        }
        if (hasSentences(boxes)) {
            List<Word> w = headwords(rows, boxes);
            if (w.size() >= 2) {
                lastLines = lines;
                lastMode = "교재(왼쪽 단어만)";
                return uniq(w);
            }
        }
        List<Word> out = fromLines(lines);
        lastMode = "목록";
        return out;
    }

    /** 같은 높이의 덩어리끼리 한 줄로 묶는다 (세로로 절반 이상 겹치면 같은 줄). 줄 안에서는 왼쪽부터. */
    static List<List<Box>> rows(List<Box> boxes) {
        List<Box> sorted = new ArrayList<Box>(boxes);
        Collections.sort(sorted, new Comparator<Box>() {
            public int compare(Box a, Box b) { return a.mid() - b.mid(); }
        });
        List<List<Box>> rows = new ArrayList<List<Box>>();
        for (Box b : sorted) {
            List<Box> hit = null;
            for (int i = rows.size() - 1; i >= 0 && i >= rows.size() - 3 && hit == null; i--) {
                for (Box o : rows.get(i)) {
                    int overlap = Math.min(b.bottom, o.bottom) - Math.max(b.top, o.top);
                    int small = Math.min(b.bottom - b.top, o.bottom - o.top);
                    boolean apart = b.right <= o.left || o.right <= b.left;     // 옆으로 나란한 것만 (위아래로 겹친 글은 다른 줄)
                    if (small > 0 && overlap * 2 >= small && apart) { hit = rows.get(i); break; }
                }
            }
            if (hit == null) { hit = new ArrayList<Box>(); rows.add(hit); }
            hit.add(b);
        }
        for (List<Box> row : rows) {
            Collections.sort(row, new Comparator<Box>() {
                public int compare(Box a, Box b) { return a.left - b.left; }
            });
        }
        return rows;
    }

    /** 영어 문장(낱말 5개 이상)이 두 줄 넘게 있으면 예문이 딸린 교재로 본다 */
    private static boolean hasSentences(List<Box> boxes) {
        int n = 0;
        for (Box b : boxes) {
            int words = 0;
            Matcher m = Pattern.compile("[A-Za-z]{2,}|\\b[aI]\\b").matcher(b.text);
            while (m.find()) words++;
            if (words >= 5) n++;
        }
        return n >= 2;
    }

    /** 교재 모양: 왼쪽 칸에서 시작하는 짧은 영어(문제 단어)와, 같은 줄 오른쪽의 한글 뜻 */
    private static List<Word> headwords(List<List<Box>> rows, List<Box> boxes) {
        List<Integer> lefts = new ArrayList<Integer>();
        int maxRight = 0;
        for (Box b : boxes) {
            if (firstHangul(b.text) < 0 && !hasLatin(b.text)) continue;    // 숫자·기호만 있는 덩어리는 빼고 잰다
            lefts.add(b.left);
            maxRight = Math.max(maxRight, b.right);
        }
        if (lefts.isEmpty()) return new ArrayList<Word>();
        Collections.sort(lefts);
        int minLeft = lefts.get(lefts.size() / 10);      // 맨 왼쪽 몇 개(쪽 번호, 잘못 읽힌 얼룩)에 휘둘리지 않게
        int limit = minLeft + (maxRight - minLeft) * 28 / 100;       // 왼쪽 칸의 범위
        List<Word> found = new ArrayList<Word>();
        List<Integer> heights = new ArrayList<Integer>();
        for (List<Box> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                Box b = row.get(i);
                if (!hasLatin(b.text)) {
                    if (b.left > limit) break;      // 오른쪽 칸에서 시작하는 줄(해석 따위)
                    continue;                       // 번호, 체크 칸(□ 가 '그'·'ㅁ' 으로 읽히기도 한다)
                }
                if (b.left > limit || isPronunciation(b.text)) break;
                String text = clean(b.text);
                int[] hw = headWord(text);
                if (hw == null) break;
                String en = english(text.substring(hw[0], hw[1]));
                if (en.isEmpty() || isHeader(en) || en.split(" ").length > 3) break;
                // 뜻: 같은 줄에서 오른쪽 칸에 있는 덩어리들의 처음 한글부터 (왼쪽 칸에 붙어 있는 한글은 쪽 아래 책 이름 따위)
                StringBuilder rest = new StringBuilder();
                for (int k = i + 1; k < row.size(); k++) if (row.get(k).left > limit) rest.append("  ").append(row.get(k).text);
                String ko = meaningOnly(rest.toString());
                if (!ko.isEmpty()) { found.add(new Word(en, ko)); heights.add(b.bottom - b.top); }
                break;
            }
        }
        // 문제 단어는 글자가 크다: 다른 것들보다 눈에 띄게 작은 것(잘못 읽힌 발음기호 따위)은 뺀다
        List<Integer> sortedH = new ArrayList<Integer>(heights);
        Collections.sort(sortedH);
        int median = sortedH.isEmpty() ? 0 : sortedH.get(sortedH.size() / 2);
        List<Word> out = new ArrayList<Word>();
        for (int i = 0; i < found.size(); i++) if (heights.get(i) * 4 >= median * 3) out.add(found.get(i));
        return out;
    }

    private static boolean hasLatin(String s) {
        for (int i = 0; i < s.length(); i++) if (latin(s.charAt(i))) return true;
        return false;
    }

    /** 발음기호 줄인지: 대괄호·빗금으로 시작하거나 발음기호 글자(æ ə ː 따위)가 들어 있다 */
    private static boolean isPronunciation(String s) {
        String t = s.trim();
        if (t.startsWith("[") || t.startsWith("/") || t.startsWith("(")) return true;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if ((c >= 0x00C0 && c <= 0x024F) || (c >= 0x0250 && c <= 0x036F) || c == 0x03B8) return true;
        }
        return false;
    }

    /** "ⓝ 숙제 ⊟ assignment" → "숙제": 처음 한글부터, 영어 낱말이나 기호가 나오기 전까지 */
    static String meaningOnly(String s) {
        int h = firstHangul(s);
        if (h < 0) return "";
        if (h > 0 && s.charAt(h - 1) == '(') h--;
        String t = s.substring(h);
        Matcher m = Pattern.compile("[A-Za-z]{2,}|[⊟⊕⊖⊜☰≡=↔←→▶◀■□●◎]|\\s{2,}[^\\uAC00-\\uD7A3(\\s]").matcher(t);
        if (m.find()) t = t.substring(0, m.start());
        return korean(t);
    }

    private static List<Word> uniq(List<Word> in) {
        List<Word> out = new ArrayList<Word>();
        for (Word w : in) {
            boolean dup = false;
            for (Word u : out) if (u.en.equalsIgnoreCase(w.en)) { dup = true; if (u.ko.isEmpty()) u.ko = w.ko; }
            if (!dup) out.add(w);
        }
        return out;
    }

    public static List<Word> fromText(String text) {
        List<String> lines = new ArrayList<String>();
        for (String l : text.split("\\r?\\n")) lines.add(l);
        return fromLines(lines);
    }

    /** 마지막으로 사진에서 모은 줄들 (시험 화면에서 확인용) */
    public static List<String> lastLines = new ArrayList<String>();
    public static List<Box> lastBoxes = new ArrayList<Box>();
    /** 마지막으로 사진을 어떤 모양으로 보았는지 (시험 화면에서 확인용) */
    public static String lastMode = "";

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
        return uniq(out);
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
        t = t.replaceAll("^[^A-Za-z0-9]{0,6}\\d{2,4}[\\)\\].:]?\\s*(?=[A-Za-z])", "");   // 체크 칸(□ 가 '그'·'ㅁ' 으로 읽힘) + 번호: "그1036 evaluate"
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
        String t = s.replaceAll("^[\\s\\.:=\\-–—>~\\)\\]]+", "").replaceAll("[\\s\\.:=\\-–—,;·•]+$", "");
        return t.replaceAll("\\s+", " ").trim();
    }
}
