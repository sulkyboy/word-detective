package kr.word.detective.core;

/**
 * 괴도의 요구(과제). 괴도아빠와 괴도엄마는 "이것만 하면 쪼꼬를 풀어 준다"고 하지만,
 * 해내면 핑계를 대고 다음 요구를 내놓는다. 그래서 요구는 끝없이 이어진다.
 *   0번째: 단어장 사진 찍기 → 1번째: 도전장 한 판 → 2번째: 단서 100개 → 그 뒤로는 다섯 가지가 돌아가며 나온다.
 */
public final class Mission {
    private Mission() {}

    public static final int PHOTO = 0, SOLVE = 1, POINTS = 2, UNSOLVED = 3, MATCH = 4, MASTER = 5;
    private static final int[] CYCLE = {SOLVE, UNSOLVED, MATCH, MASTER, POINTS};
    /** 요구를 하나 들어줄 때마다 받는 단서 (속았지만 빈손은 아니게) */
    public static final int BONUS = 20;
    /** 도전장 한 판에서 이만큼(%) 이상 맞히면 쪼꼬를 잠깐 만나게 해 준다 */
    public static final int MEET_PERCENT = 80;

    /** 이번 판 성적으로 쪼꼬를 만날 수 있는지 (문제가 너무 적으면 안 됨) */
    public static boolean canMeet(int asked, int right) {
        return asked >= 3 && right * 100 >= asked * MEET_PERCENT;
    }

    public static int type(int stage) {
        if (stage <= 0) return PHOTO;
        if (stage == 1) return SOLVE;
        if (stage == 2) return POINTS;
        return CYCLE[(stage - 3) % CYCLE.length];
    }

    /** 채워야 하는 양 */
    public static int need(int stage) {
        switch (type(stage)) {
            case SOLVE: return stage == 1 ? 1 : 2;
            case POINTS: return stage == 2 ? 100 : Math.min(400, 150 + 50 * ((stage - 3) / CYCLE.length));
            case MASTER: return 3;
            default: return 1;
        }
    }

    /** 지금까지 채운 양 (0 ~ need) */
    public static int have(Data d) {
        int need = need(d.stage), v;
        switch (type(d.stage)) {
            case PHOTO:
                v = 0;
                for (Deck k : d.decks) if (!k.words.isEmpty()) v = 1;
                break;
            case SOLVE: v = d.solved - d.mSolved; break;
            case POINTS: v = d.points - d.mPoints; break;
            case UNSOLVED: {
                int left = 0;
                for (Word w : d.allWords()) if (Srs.unsolved(w)) left++;
                v = d.solved > d.mSolved && left == 0 ? 1 : 0;
                break;
            }
            case MATCH: v = d.matches - d.mMatches; break;
            default: {
                int m = mastered(d), all = 0;
                for (Word w : d.allWords()) if (!w.ko.isEmpty()) all++;
                v = m - d.mMaster;
                if (all - d.mMaster < need && d.solved > d.mSolved) v = need;   // 더 외울 단어가 모자라면 한 판만 풀어도 된 것으로
                break;
            }
        }
        return Math.max(0, Math.min(need, v));
    }

    public static boolean done(Data d) { return have(d) >= need(d.stage); }

    static int mastered(Data d) {
        int n = 0;
        for (Word w : d.allWords()) if (w.box >= 3) n++;
        return n;
    }

    /** 요구를 들어준 것으로 하고 다음 요구로 넘어간다. 보너스 단서를 준다. */
    public static void advance(Data d) {
        d.points += BONUS;
        d.stage++;
        d.mSolved = d.solved;
        d.mPoints = d.points;
        d.mMatches = d.matches;
        d.mMaster = mastered(d);
    }

    /** 과제 이름 (첫 화면의 카드에 쓰는 짧은 말) */
    public static String task(int stage) {
        int n = need(stage);
        switch (type(stage)) {
            case PHOTO: return "단어장 사진 찍어 오기";
            case SOLVE: return "도전장 " + n + "판 끝내기";
            case POINTS: return "단서 " + n + "개 모으기";
            case UNSOLVED: return "미해결 사건을 모두 풀고 도전장 한 판 끝내기";
            case MATCH: return "짝 맞추기 한 판 이기기";
            default: return "★ 3개짜리 단어 " + n + "개 더 만들기";
        }
    }

    /** 괴도아빠가 요구하는 말 */
    public static String demand(int stage) {
        int n = need(stage);
        switch (type(stage)) {
            case PHOTO: return "쪼꼬를 찾고 싶으면 단어장 사진을 찍어 오너라! 그러면 바로 풀어 주마.";
            case SOLVE: return stage == 1 ? "내 도전장을 한 판 끝내 보거라. 그러면 이번엔 진짜 풀어 주마!"
                    : "도전장을 " + n + "판 더 끝내 오너라. 그러면 꼭 풀어 주마!";
            case POINTS: return "단서를 " + n + "개 더 모아 오너라. 이번엔 진짜다!";
            case UNSOLVED: return "미해결 사건을 하나도 남기지 말고 도전장을 한 판 끝내라. 약속하마!";
            case MATCH: return "짝 맞추기를 한 판 이기고 오너라. 그러면 생각해 보지!";
            default: return "★ 3개짜리 단어를 " + n + "개 더 만들어 오너라. 이번엔 정말이다!";
        }
    }

    /** 요구를 들어줬을 때 풀어 주지 않으려고 대는 핑계 */
    public static String excuse(int stage) {
        switch (type(stage)) {
            case PHOTO: return "사진은 제법 잘 찍어 왔군. 하지만 찍기만 하고 외우지 않으면 무슨 소용이지?";
            case SOLVE: return stage == 1 ? "한 판 풀었다고? 흥, 그 정도는 누구나 한다!" : "두 판이나 풀었군. 하지만 내 눈엔 아직 부족하다!";
            case POINTS: return "단서를 다 모아 왔군… 그런데 내가 '오늘' 풀어 준다고 했던가?";
            case UNSOLVED: return "미해결 사건을 다 풀었다고? 음… 쪼꼬가 여기가 더 좋다는구나!";
            case MATCH: return "짝 맞추기는 재미있었나? 재미있었으면 그건 놀이지!";
            default: return "단어를 제법 외웠군. 하지만 쪼꼬 간식값이 아직 모자란다!";
        }
    }
}
