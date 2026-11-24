package kr.word.detective.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** 앱에 저장하는 모든 것: 단어장, 점수, 공부 기록, 설정. JSON 한 덩어리로 읽고 쓴다. */
public final class Data {
    public String hero = "";            // 탐정 이름 (아이가 짓는다)
    public int points;                  // 모은 단서(점수)
    public int solved;                  // 해결한 사건(끝낸 공부) 수
    public int streak;                  // 연속 출석 일수
    public long lastDay;                // 마지막으로 공부한 날
    public int perSession = 10;         // 한 번에 푸는 단어 수
    public boolean timer;               // 시간 제한(번개 사건)
    public boolean sound = true;        // 소리로 읽어 주기
    public boolean intro = true;        // 앱을 열 때 인트로 만화 보기
    // 괴도의 요구(core/Mission): 몇 번째 요구인지와, 그 요구가 시작됐을 때의 값들
    public int stage;                   // 지금까지 들어준 요구 수 (= 속은 횟수)
    public int mSolved, mPoints, mMatches, mMaster;
    public int matches;                 // 짝 맞추기를 끝낸 횟수
    public int meets;                   // 쪼꼬를 잠깐 만난 횟수 (도전장에서 80% 이상 맞혔을 때)
    public long nextId = 1;
    public final List<Deck> decks = new ArrayList<Deck>();
    /** 날짜 번호 → {푼 문제 수, 맞힌 수} */
    public final TreeMap<Long, int[]> history = new TreeMap<Long, int[]>();

    public Deck deck(long id) {
        for (Deck d : decks) if (d.id == id) return d;
        return null;
    }

    public List<Word> allWords() {
        List<Word> all = new ArrayList<Word>();
        for (Deck d : decks) all.addAll(d.words);
        return all;
    }

    /** 오늘 공부한 결과를 더한다. 연속 출석도 여기서 센다. */
    public void record(long today, int asked, int right) {
        int[] h = history.get(today);
        if (h == null) { h = new int[2]; history.put(today, h); }
        h[0] += asked;
        h[1] += right;
        if (lastDay != today) {
            streak = lastDay == today - 1 ? streak + 1 : 1;
            lastDay = today;
        }
    }

    /** 오늘 기준으로 이어지고 있는 연속 출석 일수 (어제까지 했으면 유지, 하루 넘게 쉬었으면 0) */
    public int liveStreak(long today) {
        return lastDay >= today - 1 ? streak : 0;
    }

    // ---------- 저장 ----------

    public String toJson() {
        StringBuilder b = new StringBuilder(4096);
        b.append("{\"hero\":").append(Json.quote(hero))
                .append(",\"points\":").append(points)
                .append(",\"solved\":").append(solved)
                .append(",\"streak\":").append(streak)
                .append(",\"lastDay\":").append(lastDay)
                .append(",\"perSession\":").append(perSession)
                .append(",\"timer\":").append(timer ? 1 : 0)
                .append(",\"sound\":").append(sound ? 1 : 0)
                .append(",\"intro\":").append(intro ? 1 : 0)
                .append(",\"stage\":").append(stage)
                .append(",\"mSolved\":").append(mSolved)
                .append(",\"mPoints\":").append(mPoints)
                .append(",\"mMatches\":").append(mMatches)
                .append(",\"mMaster\":").append(mMaster)
                .append(",\"matches\":").append(matches)
                .append(",\"meets\":").append(meets)
                .append(",\"nextId\":").append(nextId)
                .append(",\"history\":[");
        boolean first = true;
        for (Map.Entry<Long, int[]> e : history.entrySet()) {
            if (!first) b.append(',');
            first = false;
            b.append("{\"d\":").append(e.getKey()).append(",\"a\":").append(e.getValue()[0]).append(",\"r\":").append(e.getValue()[1]).append('}');
        }
        b.append("],\"decks\":[");
        for (int i = 0; i < decks.size(); i++) {
            Deck d = decks.get(i);
            if (i > 0) b.append(',');
            b.append("\n{\"id\":").append(d.id).append(",\"name\":").append(Json.quote(d.name)).append(",\"words\":[");
            for (int k = 0; k < d.words.size(); k++) {
                Word w = d.words.get(k);
                if (k > 0) b.append(',');
                b.append("\n{\"en\":").append(Json.quote(w.en)).append(",\"ko\":").append(Json.quote(w.ko))
                        .append(",\"seen\":").append(w.seen ? 1 : 0).append(",\"box\":").append(w.box)
                        .append(",\"due\":").append(w.due).append(",\"right\":").append(w.right)
                        .append(",\"wrong\":").append(w.wrong).append('}');
            }
            b.append("]}");
        }
        return b.append("]}").toString();
    }

    @SuppressWarnings("unchecked")
    public static Data fromJson(String text) {
        Data x = new Data();
        Object root = Json.parse(text);
        if (!(root instanceof Map)) return x;
        Map<String, Object> m = (Map<String, Object>) root;
        x.hero = Json.str(m, "hero");
        x.points = num(m, "points", 0);
        x.solved = num(m, "solved", 0);
        x.streak = num(m, "streak", 0);
        x.lastDay = num(m, "lastDay", 0);
        x.perSession = Math.max(5, Math.min(30, num(m, "perSession", 10)));
        x.timer = num(m, "timer", 0) == 1;
        x.sound = num(m, "sound", 1) == 1;
        x.intro = num(m, "intro", 1) == 1;
        x.stage = Math.max(0, num(m, "stage", 0));
        x.mSolved = num(m, "mSolved", 0);
        x.mPoints = num(m, "mPoints", 0);
        x.mMatches = num(m, "mMatches", 0);
        x.mMaster = num(m, "mMaster", 0);
        x.matches = num(m, "matches", 0);
        x.meets = num(m, "meets", 0);
        x.nextId = Math.max(1, num(m, "nextId", 1));
        for (Map<String, Object> h : Json.list(m.get("history")))
            x.history.put((long) num(h, "d", 0), new int[] {num(h, "a", 0), num(h, "r", 0)});
        for (Map<String, Object> dm : Json.list(m.get("decks"))) {
            Deck d = new Deck();
            d.id = num(dm, "id", 0);
            d.name = Json.str(dm, "name");
            for (Map<String, Object> wm : Json.list(dm.get("words"))) {
                Word w = new Word(Json.str(wm, "en"), Json.str(wm, "ko"));
                w.seen = num(wm, "seen", 0) == 1;
                w.box = Math.max(0, Math.min(Srs.TOP, num(wm, "box", 0)));
                w.due = num(wm, "due", 0);
                w.right = num(wm, "right", 0);
                w.wrong = num(wm, "wrong", 0);
                if (!w.en.isEmpty()) d.words.add(w);
            }
            x.decks.add(d);
            if (d.id >= x.nextId) x.nextId = d.id + 1;
        }
        return x;
    }

    private static int num(Map<String, Object> m, String k, int def) {
        Integer v = Json.integer(m, k);
        return v == null ? def : v;
    }
}
