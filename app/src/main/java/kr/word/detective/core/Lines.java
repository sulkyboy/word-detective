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

    // 영어 덩어리 다음에 한글이 든 덩어리가 오는 모양을 줄 안에서 차례로 찾는다 (한 줄에 두 짝이 있어도 됨)
    private static final Pattern PAIR = Pattern.compile("([A-Za-z][A-Za-z '\\-]*)([^A-Za-z]*[\\uAC00-\\uD7A3][^A-Za-z]*)");

    static List<Word> fromLines(List<String> lines) {
        List<Word> out = new ArrayList<Word>();
        Word open = null;   // 뜻이 아직 없는 단어 (뜻이 다음 줄에 있는 경우)
        for (String raw : lines) {
            String line = clean(raw);
            if (line.isEmpty()) continue;
            boolean hasHangul = line.matches(".*[\\uAC00-\\uD7A3].*");
            boolean hasLatin = line.matches(".*[A-Za-z]{2,}.*") || line.matches("(?i)\\s*(a|i)\\s*");
            if (hasHangul && hasLatin) {
                Matcher m = PAIR.matcher(line);
                boolean any = false;
                while (m.find()) {
                    String en = english(m.group(1)), ko = korean(m.group(2));
                    if (en.isEmpty() || ko.isEmpty()) continue;
                    out.add(new Word(en, ko));
                    any = true;
                }
                if (any) { open = null; continue; }
            }
            if (hasLatin && !hasHangul) {
                String en = english(line);
                if (en.isEmpty()) continue;
                open = new Word(en, "");
                out.add(open);
            } else if (hasHangul && open != null) {
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

    /** 번호, 발음기호, 체크 표시 따위를 지운다 */
    static String clean(String s) {
        String t = s.replace('\t', ' ').replace('|', ' ');
        t = t.replaceAll("\\[[^\\]]*\\]|/[^/]*/", " ");                 // [ǽpl], /æpl/
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
        String t = s.replaceAll("^[\\s\\.:=\\-–—>~]+", "").replaceAll("[\\s\\.:=\\-–—,;]+$", "");
        return t.replaceAll("\\s+", " ").trim();
    }
}
