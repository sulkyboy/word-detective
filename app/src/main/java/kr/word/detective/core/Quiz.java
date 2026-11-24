package kr.word.detective.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** 한 번의 공부(= 사건 하나)에 낼 문제들을 고른다. */
public final class Quiz {
    private Quiz() {}

    public static final int INTRO = 0;    // 단서 카드: 처음 보는 단어를 먼저 보여 준다 (점수 없음)
    public static final int EN_KO = 1;    // 영어를 들려주고 한국어 뜻을 묻는다
    public static final int KO_EN = 2;    // 한국어 뜻을 말해 주고 영어 단어를 묻는다
    public static final int SPELL = 3;    // 스펠링을 묻는다

    public static final class Q {
        public final Word word;
        public final int type;
        /** 보기에서 고를 때 쓰는 보기들 (정답 포함, 섞여 있음). EN_KO 는 뜻, KO_EN 은 영어 단어 */
        public final List<String> choices = new ArrayList<String>();
        Q(Word word, int type) { this.word = word; this.type = type; }
    }

    /**
     * @param pool      이번에 공부할 단어장의 단어들
     * @param everything 보기(오답)를 만들 때 쓸 모든 단어
     * @param onlyUnsolved 미해결 사건(틀렸던 단어)만
     */
    public static List<Q> build(List<Word> pool, List<Word> everything, final long today, int count, boolean onlyUnsolved, Random rnd) {
        List<Word> cand = new ArrayList<Word>();
        for (Word w : pool) {
            if (w.en.isEmpty() || w.ko.isEmpty()) continue;
            if (onlyUnsolved && !Srs.unsolved(w)) continue;
            cand.add(w);
        }
        Collections.shuffle(cand, rnd);
        // 순서: ① 오늘 다시 볼 단어(잘 모르는 것부터) ② 처음 보는 단어 ③ 나머지(곧 볼 것부터)
        Collections.sort(cand, new Comparator<Word>() {
            public int compare(Word a, Word b) {
                int ga = group(a, today), gb = group(b, today);
                if (ga != gb) return ga - gb;
                if (ga == 0) return a.box - b.box;
                if (ga == 2) return Long.compare(a.due, b.due);
                return 0;
            }
        });
        List<Word> picked = new ArrayList<Word>(cand.subList(0, Math.min(count, cand.size())));
        Collections.shuffle(picked, rnd);

        List<Q> out = new ArrayList<Q>();
        for (Word w : picked) {
            int type;
            if (!w.seen) {
                out.add(new Q(w, INTRO));
                type = EN_KO;
            } else {
                int r = rnd.nextInt(100);
                if (w.box <= 1) type = r < 45 ? EN_KO : r < 80 ? KO_EN : SPELL;
                else type = r < 30 ? EN_KO : r < 65 ? KO_EN : SPELL;
            }
            Q q = new Q(w, type);
            if (type != SPELL) fillChoices(q, everything, rnd);
            out.add(q);
        }
        return out;
    }

    private static int group(Word w, long today) {
        if (Srs.due(w, today)) return 0;
        if (!w.seen) return 1;
        return 2;
    }

    private static void fillChoices(Q q, List<Word> everything, Random rnd) {
        boolean ko = q.type == EN_KO;
        String answer = ko ? q.word.ko : q.word.en;
        List<String> others = new ArrayList<String>();
        for (Word w : everything) {
            String v = ko ? w.ko : w.en;
            if (v.isEmpty() || v.equalsIgnoreCase(answer) || others.contains(v)) continue;
            if (w.en.equalsIgnoreCase(q.word.en)) continue;
            others.add(v);
        }
        Collections.shuffle(others, rnd);
        q.choices.add(answer);
        for (int i = 0; i < 3 && i < others.size(); i++) q.choices.add(others.get(i));
        Collections.shuffle(q.choices, rnd);
    }

    /** 맞혔을 때 주는 단서: 말이나 글자로 직접 답하면 10, 보기에서 고르면 5, 힌트를 봤으면 절반 */
    public static int reward(int type, boolean byChoice, boolean hinted) {
        int p = byChoice ? 5 : 10;
        if (type == SPELL) p += 2;
        return hinted ? Math.max(2, p / 2) : p;
    }

    /** 힌트: 첫 글자와 글자 수 ("a _ _ _ _"), 뜻은 첫 글자만 */
    public static String hint(Q q) {
        if (q.type == EN_KO) {
            String k = Judge.meanings(q.word.ko).isEmpty() ? q.word.ko : Judge.meanings(q.word.ko).get(0);
            StringBuilder b = new StringBuilder(k.substring(0, 1));
            for (int i = 1; i < k.length(); i++) b.append(" ○");
            return b.toString();
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < q.word.en.length(); i++) {
            char c = q.word.en.charAt(i);
            if (i > 0) b.append(' ');
            b.append(i == 0 || !Character.isLetter(c) ? c : '_');
        }
        return b.toString();
    }
}
