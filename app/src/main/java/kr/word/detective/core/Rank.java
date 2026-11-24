package kr.word.detective.core;

/** 모은 단서 수에 따른 탐정 등급. */
public final class Rank {
    private Rank() {}

    static final int[] AT = {0, 100, 300, 700, 1500, 3000, 6000};
    static final String[] NAME = {"견습 탐정", "새내기 탐정", "동네 탐정", "소문난 탐정", "명탐정", "전설의 탐정", "세계 최고의 탐정"};

    public static int level(int points) {
        int lv = 0;
        for (int i = 0; i < AT.length; i++) if (points >= AT[i]) lv = i;
        return lv;
    }

    public static String name(int points) { return NAME[level(points)]; }

    /** 다음 등급까지 남은 단서. 최고 등급이면 0 */
    public static int toNext(int points) {
        int lv = level(points);
        return lv + 1 < AT.length ? AT[lv + 1] - points : 0;
    }

    /** 지금 등급 안에서 얼마나 왔는지 0~100 */
    public static int progress(int points) {
        int lv = level(points);
        if (lv + 1 >= AT.length) return 100;
        return (int) (100L * (points - AT[lv]) / (AT[lv + 1] - AT[lv]));
    }

    public static String nextName(int points) {
        int lv = level(points);
        return lv + 1 < NAME.length ? NAME[lv + 1] : "";
    }
}
