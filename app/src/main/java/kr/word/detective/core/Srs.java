package kr.word.detective.core;

/** 간격 복습: 맞히면 다음에 물어볼 날을 점점 멀리, 틀리면 처음부터 다시. */
public final class Srs {
    private Srs() {}

    public static final int TOP = 5;
    /** 복습 단계별로 며칠 뒤에 다시 물어볼지 */
    static final int[] GAP = {0, 1, 2, 4, 7, 15};

    public static void right(Word w, long today) {
        w.right++;
        w.box = Math.min(TOP, w.box + 1);
        w.due = today + GAP[w.box];
    }

    public static void wrong(Word w, long today) {
        w.wrong++;
        w.box = 0;
        w.due = today;
    }

    /** 오늘 다시 봐야 하는 단어인지 (한 번이라도 공부한 단어 가운데) */
    public static boolean due(Word w, long today) {
        return w.seen && w.due <= today && w.box < TOP;
    }

    /** 미해결 사건: 틀린 적이 있고 아직 잘 모르는 단어 */
    public static boolean unsolved(Word w) {
        return w.seen && w.wrong > 0 && w.box <= 1;
    }
}
