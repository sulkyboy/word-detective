package kr.word.detective.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 답이 맞았는지 가린다. 말로 한 답은 음성 인식이 여러 후보를 주므로, 후보 가운데 하나라도 맞으면 정답. */
public final class Judge {
    private Judge() {}

    /** 영어 단어 답: 대소문자·띄어쓰기·기호는 따지지 않는다. */
    public static boolean english(String word, List<String> answers) {
        String w = letters(word);
        if (w.isEmpty()) return false;
        for (String a : answers) if (letters(a).equals(w)) return true;
        return false;
    }

    /**
     * 스펠링 답: 글자를 하나씩 말하거나("a p p l e", "에이 피 피 엘 이" 는 인식기가 영문자로 바꿔 준다) 자판으로 친 것.
     * "double p" 처럼 말한 것도 풀어 준다.
     */
    public static boolean spelling(String word, List<String> answers) {
        String w = letters(word);
        if (w.isEmpty()) return false;
        for (String a : answers) {
            String t = a.toLowerCase(Locale.US).replaceAll("double\\s+([a-z])\\b", "$1$1");
            if (letters(t).equals(w)) return true;
        }
        return false;
    }

    /** 한국어 뜻 답: 뜻이 여러 개면 그 가운데 하나만 말해도 정답. */
    public static boolean meaning(String ko, List<String> answers) {
        List<String> wants = meanings(ko);
        for (String a : answers) {
            String said = hangul(a);
            if (said.isEmpty()) continue;
            for (String want : wants) if (same(want, said)) return true;
        }
        return false;
    }

    /** "사과, 사죄(하다)" → [사과, 사죄하다, 사죄] */
    public static List<String> meanings(String ko) {
        List<String> out = new ArrayList<String>();
        for (String part : ko.split("[,;/·、]|\\s{2,}")) {
            String whole = hangul(part);
            if (!whole.isEmpty() && !out.contains(whole)) out.add(whole);
            String bare = hangul(part.replaceAll("[\\(\\[（][^\\)\\]）]*[\\)\\]）]", ""));
            if (!bare.isEmpty() && !out.contains(bare)) out.add(bare);
        }
        return out;
    }

    private static boolean same(String want, String said) {
        if (want.equals(said)) return true;
        if (want.length() >= 2 && said.contains(want)) return true;          // "사과요", "그건 사과"
        String ws = stem(want), ss = stem(said);
        if (ws.length() >= 2 && (ss.equals(ws) || said.startsWith(ws))) return true;   // 달리다 ↔ 달려요, 행복한 ↔ 행복하다
        return false;
    }

    /** 끝의 '하다/한/다/요' 따위를 뗀 줄기 */
    private static String stem(String s) {
        String[] ends = {"하다", "한", "하게", "이다", "입니다", "해요", "요", "다", "은", "는"};
        for (String e : ends) if (s.length() > e.length() + 1 && s.endsWith(e)) return s.substring(0, s.length() - e.length());
        return s;
    }

    static String letters(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toLowerCase(Locale.US).toCharArray()) if (c >= 'a' && c <= 'z') b.append(c);
        return b.toString();
    }

    static String hangul(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) if (c >= 0xAC00 && c <= 0xD7A3) b.append(c);
        return b.toString();
    }
}
