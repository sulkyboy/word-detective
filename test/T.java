import java.util.*;
import kr.word.detective.core.*;

public class T {
    static int fails = 0;
    static void eq(String name, Object want, Object got) {
        if (!String.valueOf(want).equals(String.valueOf(got))) { fails++; System.out.println("FAIL " + name + "\n  want: " + want + "\n  got : " + got); }
    }
    static String show(List<Word> ws) { StringBuilder b = new StringBuilder(); for (Word w : ws) b.append(w.en).append('=').append(w.ko).append('|'); return b.toString(); }

    public static void main(String[] a) {
        // 붙여넣은 글
        eq("plain", "apple=사과|run=달리다, 뛰다|", show(Lines.fromText("1. apple 사과\n2. run  달리다, 뛰다\n")));
        eq("pron", "beautiful=아름다운|", show(Lines.fromText("03 beautiful [bjúːtəfəl] adj. 아름다운")));
        eq("two per line", "cat=고양이|dog=개|", show(Lines.fromText("cat 고양이   dog 개")));
        eq("next line", "library=도서관|", show(Lines.fromText("library\n도서관\n")));
        eq("phrase", "get up=일어나다|ice cream=아이스크림|", show(Lines.fromText("get up : 일어나다\nice cream - 아이스크림")));
        eq("header ignored", "book=책|", show(Lines.fromText("단어   뜻\nbook 책")));
        eq("garbled pron", "beautiful=아름다운|remember=기억하다|different=다른|", show(Lines.fromText("2 beautiful (bjuMtfXl) 아름다운\n6 remember [rimembrl 기억하다\n9 different [difXrXnt) 다른")));
        eq("title line", "apple=사과|", show(Lines.fromText("Day 1 영단어 테스트\n1 apple 사과")));
        eq("paren meaning", "drink=(물을) 마시다|", show(Lines.fromText("drink (물을) 마시다")));
        eq("korean only", "", show(Lines.fromText("이름:      반:")));
        eq("dup", "book=책|", show(Lines.fromText("book 책\nBook 책")));
        // 사진: 영어 칸과 뜻 칸이 따로 읽힌 경우
        List<Lines.Box> bx = new ArrayList<Lines.Box>();
        bx.add(new Lines.Box("사과", 400, 102, 480, 140));
        bx.add(new Lines.Box("apple", 100, 100, 220, 138));
        bx.add(new Lines.Box("banana", 100, 160, 240, 198));
        bx.add(new Lines.Box("바나나", 400, 163, 500, 200));
        bx.add(new Lines.Box("1", 40, 101, 60, 139));
        eq("boxes", "apple=사과|banana=바나나|", show(Lines.fromBoxes(bx)));

        // 사진: 예문이 딸린 교재 (왼쪽에 번호·문제 단어·발음기호, 오른쪽에 뜻·예문·해석)
        List<Lines.Box> tb = new ArrayList<Lines.Box>();
        tb.add(new Lines.Box("1051", 20, 540, 80, 570));
        tb.add(new Lines.Box("classroom", 105, 510, 360, 565));
        tb.add(new Lines.Box("n 교실", 500, 500, 630, 545));
        tb.add(new Lines.Box("[klæsrù:m]", 110, 575, 280, 610));
        tb.add(new Lines.Box("Two girls are cleaning the classroom.", 500, 560, 1240, 615));
        tb.add(new Lines.Box("두 명의 소녀가 교실을 청소하고 있다.", 500, 625, 1060, 665));
        tb.add(new Lines.Box("1052 chalk", 18, 745, 240, 800));
        tb.add(new Lines.Box("(n) 분필", 500, 735, 630, 780));
        tb.add(new Lines.Box("[tfo:k)", 108, 808, 196, 842));
        tb.add(new Lines.Box("The math teacher drew a triangle with chalk", 500, 800, 1430, 870));
        tb.add(new Lines.Box("a piece[stick] of chalk 분필 한 자루", 545, 990, 1210, 1040));
        tb.add(new Lines.Box("homework", 95, 1580, 380, 1635));
        tb.add(new Lines.Box("® 숙제 ⊟ assignment", 500, 1590, 950, 1635));
        tb.add(new Lines.Box("houmwark", 98, 1648, 300, 1680));
        tb.add(new Lines.Box("우리 영어 선생님은 항상 우리에게 숙제를 너무 많이 주신다.", 500, 1648, 1395, 1700));
        tb.add(new Lines.Box("Our English teacher always gives us too much", 500, 1660, 1440, 1640));
        tb.add(new Lines.Box("partner", 100, 1345, 290, 1395));
        tb.add(new Lines.Box("n 짝, 동료, 협력자", 500, 1345, 845, 1390));
        tb.add(new Lines.Box("Basic", 20, 420, 150, 470));
        tb.add(new Lines.Box("School", 345, 80, 670, 180));
        eq("textbook", "classroom=교실|chalk=분필|partner=짝, 동료, 협력자|homework=숙제|", show(Lines.fromBoxes(tb)));
        // 실제 사진에서 나온 모양: 체크 칸(□)이 '그'·'ㅁ' 으로 읽히고, 예문이 잘리고, 쪽 아래에 책 이름이 있다
        List<Lines.Box> ph = new ArrayList<Lines.Box>();
        ph.add(new Lines.Box("+ admission n 입장, 인정", 920, 70, 1440, 120));
        ph.add(new Lines.Box("Advanced", 390, 235, 660, 290));
        ph.add(new Lines.Box("그1036 evaluate", 355, 350, 725, 405));
        ph.add(new Lines.Box("0 평가하다", 910, 355, 1125, 400));
        ph.add(new Lines.Box("[ivæljuèit]", 488, 415, 655, 455));
        ph.add(new Lines.Box("The teacher used differ", 905, 430, 1450, 480));
        ph.add(new Lines.Box("reading skills.", 903, 495, 1185, 545));
        ph.add(new Lines.Box("그 교사는 학생들의 읽기 능력을 평", 900, 560, 1450, 610));
        ph.add(new Lines.Box("evaluation n 평가", 940, 635, 1310, 680));
        ph.add(new Lines.Box("ㅁ", 350, 742, 372, 764));
        ph.add(new Lines.Box("1037", 383, 738, 445, 768));
        ph.add(new Lines.Box("submit", 475, 728, 660, 780));
        ph.add(new Lines.Box("v 제출하다 · = hand in", 890, 742, 1345, 790));
        ph.add(new Lines.Box("[sabmit]", 478, 792, 612, 830));
        ph.add(new Lines.Box("I'm scheduled to submit a", 888, 812, 1440, 862));
        ph.add(new Lines.Box("나는 금요일까지 보고서를 제출할 예", 885, 878, 1450, 925));
        ph.add(new Lines.Box("□ 1040 absent", 322, 1560, 628, 1612));
        ph.add(new Lines.Box("@ 결석한 ↔ present 출석한", 852, 1578, 1378, 1630));
        ph.add(new Lines.Box("She was absent from school", 850, 1645, 1440, 1700));
        ph.add(new Lines.Box("be absent from ~에 결석하다", 888, 1772, 1440, 1830));
        ph.add(new Lines.Box("1039 instruct", 365, 1262, 660, 1312));
        ph.add(new Lines.Box("V 교수하다, 가르치다 learn", 865, 1285, 1450, 1345));
        ph.add(new Lines.Box("James 교수는 학생들에게 최선을 다하", 860, 1415, 1450, 1465));
        ph.add(new Lines.Box("216 Word Master 중등 실력", 275, 2372, 752, 2420));
        eq("photo", "evaluate=평가하다|submit=제출하다|instruct=교수하다, 가르치다|absent=결석한|", show(Lines.fromBoxes(ph)));
        eq("photo mode", "번호+단어+뜻", Lines.lastMode);
        // 번호 기준만으로: 예문이 한 줄뿐이라 교재로 보지 않는 경우에도 번호 줄은 찾는다
        List<Lines.Box> nb = new ArrayList<Lines.Box>();
        nb.add(new Lines.Box("D3040 empty", 237, 1584, 559, 1672)); nb.add(new Lines.Box("@비어 있는 + full 가득 찬", 798, 1607, 1333, 1671));
        nb.add(new Lines.Box("남3042", 217, 2223, 319, 2262)); nb.add(new Lines.Box("season", 349, 2240, 542, 2280)); nb.add(new Lines.Box("@ 계절", 775, 2236, 911, 2285));
        nb.add(new Lines.Box("[sí:zn]", 354, 2290, 454, 2323)); nb.add(new Lines.Box("216 My Word Book 중등 실력", 191, 2504, 806, 2570));
        nb.add(new Lines.Box("be empty of ~이 없다.", 842, 1811, 1275, 1872));
        eq("numbered", "empty=비어 있는|season=계절|", show(Lines.fromBoxes(nb)));
        eq("numbered mode", "번호+단어+뜻", Lines.lastMode);

        // 실제 글자 인식(ML Kit)이 시험 그림에서 돌려준 덩어리들 그대로
        eq("ocr photo-like", "measure=측정하다, 재다|deliver=배달하다|harbor=항구|protect=보호하다, 지키다|empty=비어 있는|repair=수리하다, 고치다|season=계절|", show(Lines.fromBoxes(load("boxes_photo_like.txt"))));
        eq("ocr textbook", "garden=정원, 뜰|bridge=다리|borrow=빌리다|quiet=조용한|neighbor=이웃, 이웃 사람|weekend=주말|", show(Lines.fromBoxes(load("boxes_textbook.txt"))));
        eq("textbook mode", "번호+단어+뜻", Lines.lastMode);
        Lines.fromBoxes(bx); eq("list mode", "목록", Lines.lastMode);
        eq("meaning only", "숙제", Lines.fromText("x").size() == 0 ? "숙제" : "");

        // 답 가리기
        eq("en", true, Judge.english("ice cream", Arrays.asList("Ice-cream")));
        eq("en wrong", false, Judge.english("apple", Arrays.asList("apply", "april")));
        eq("spell", true, Judge.spelling("apple", Arrays.asList("A P P L E")));
        eq("spell double", true, Judge.spelling("apple", Arrays.asList("a double p l e")));
        eq("spell wrong", false, Judge.spelling("apple", Arrays.asList("a p l e")));
        eq("ko", true, Judge.meaning("사과, 사죄", Arrays.asList("사죄")));
        eq("ko polite", true, Judge.meaning("달리다", Arrays.asList("달려", "달리다요")));
        eq("ko stem", true, Judge.meaning("행복한", Arrays.asList("행복하다")));
        eq("ko paren", true, Judge.meaning("(물을) 마시다", Arrays.asList("마시다")));
        eq("ko wrong", false, Judge.meaning("사과", Arrays.asList("배", "바나나")));
        eq("ko short", false, Judge.meaning("개", Arrays.asList("개나리")));
        eq("ko short ok", true, Judge.meaning("개", Arrays.asList("개")));

        // 복습 간격
        Word w = new Word("apple", "사과"); w.seen = true;
        Srs.right(w, 100); eq("box1", "1/101", w.box + "/" + w.due);
        Srs.right(w, 101); eq("box2", "2/103", w.box + "/" + w.due);
        Srs.wrong(w, 103); eq("box0", "0/103", w.box + "/" + w.due);
        eq("unsolved", true, Srs.unsolved(w));

        // 문제 만들기
        List<Word> pool = new ArrayList<Word>();
        String[][] src = {{"apple","사과"},{"book","책"},{"cat","고양이"},{"dog","개"},{"egg","달걀"},{"fish","물고기"}};
        for (String[] s : src) pool.add(new Word(s[0], s[1]));
        pool.get(0).seen = true; pool.get(0).due = 5; pool.get(0).box = 1;
        List<Quiz.Q> qs = Quiz.build(pool, pool, 10, 3, false, new Random(1));
        int intro = 0, quiz = 0; boolean hasDue = false;
        for (Quiz.Q q : qs) { if (q.type == Quiz.INTRO) intro++; else { quiz++; if (q.type != Quiz.SPELL) { eq("choices", 4, q.choices.size()); eq("has answer", true, q.choices.contains(q.type == Quiz.EN_KO ? q.word.ko : q.word.en)); } } if (q.word.en.equals("apple")) hasDue = true; }
        eq("quiz count", 3, quiz); eq("intro count", 2, intro); eq("due first", true, hasDue);
        eq("hint en", "a _ _ _ _", Quiz.hint(qs.get(0).word.en.equals("apple") ? new ArrayList<Quiz.Q>(Quiz.build(Arrays.asList(pool.get(0)), pool, 10, 1, false, new Random(3))).get(0).type == Quiz.EN_KO ? null == null ? fakeSpell(pool) : null : fakeSpell(pool) : fakeSpell(pool)));

        // 저장하고 다시 읽기
        Data d = new Data(); d.hero = "민\"지"; d.points = 120; Deck dk = new Deck(); dk.id = d.nextId++; dk.name = "Day 1"; dk.words.addAll(pool); d.decks.add(dk);
        d.intro = false;
        // 괴도의 요구: 사진 → 한 판 → 단서 100 → (두 판, 미해결 0, 짝 맞추기, 외운 단어 3, 단서 150) 되풀이
        eq("m0", "단어장 사진 찍어 오기/true", Mission.task(d.stage) + "/" + Mission.done(d));
        Mission.advance(d); eq("bonus", 140, d.points); eq("m1", false, Mission.done(d));
        d.solved++; eq("m1 done", true, Mission.done(d));
        Mission.advance(d); eq("m2", "단서 100개 모으기/0", Mission.task(d.stage) + "/" + Mission.have(d));
        d.points += 60; eq("m2 have", 60, Mission.have(d)); d.points += 60; eq("m2 done", true, Mission.done(d));
        Mission.advance(d); eq("m3", "도전장 2판 끝내기", Mission.task(d.stage));
        d.solved += 2; Mission.advance(d); eq("m4", Mission.UNSOLVED, Mission.type(d.stage)); eq("m4 not yet", false, Mission.done(d));
        d.solved++; eq("m4 done", true, Mission.done(d));
        Mission.advance(d); eq("m5", Mission.MATCH, Mission.type(d.stage)); d.matches++; eq("m5 done", true, Mission.done(d));
        Mission.advance(d); eq("m6", Mission.MASTER, Mission.type(d.stage)); pool.get(1).box = 3; pool.get(2).box = 4; eq("m6 have", 2, Mission.have(d));
        pool.get(3).box = 3; Mission.advance(d); eq("m7", "단서 150개 모으기", Mission.task(d.stage)); eq("m8", "도전장 2판 끝내기", Mission.task(d.stage + 1)); eq("m12", 200, Mission.need(d.stage + 5));
        for (int st = 0; st < 12; st++) if (Mission.demand(st).isEmpty() || Mission.excuse(st).isEmpty()) eq("texts " + st, "non-empty", "empty");
        d.record(200, 10, 8); d.record(201, 5, 5); d.record(203, 5, 1);
        Data e = Data.fromJson(d.toJson());
        eq("roundtrip", d.toJson(), e.toJson());
        eq("streak", 1, e.streak); eq("live streak", 0, e.liveStreak(205)); eq("stage kept", 7, e.stage);
        System.out.println(fails == 0 ? "ALL OK" : fails + " FAILED");
    }
    static List<Lines.Box> load(String file) {
        List<Lines.Box> out = new ArrayList<Lines.Box>();
        try {
            for (String l : new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(file)), "UTF-8").split("\n")) {
                if (!l.startsWith("box: ")) continue;
                int bar = l.indexOf(" | ");
                String[] n = l.substring(5, bar).split(",");
                out.add(new Lines.Box(l.substring(bar + 3), Integer.parseInt(n[0]), Integer.parseInt(n[1]), Integer.parseInt(n[2]), Integer.parseInt(n[3])));
            }
        } catch (Exception e) { throw new RuntimeException(e); }
        return out;
    }
    static Quiz.Q fakeSpell(List<Word> pool) {
        // 스펠링 문제가 나올 때까지 만든다
        Word w = pool.get(0);
        for (int s = 0; s < 200; s++) { List<Quiz.Q> q = Quiz.build(Arrays.asList(w), pool, 10, 1, false, new Random(s)); if (q.get(0).type == Quiz.SPELL) return q.get(0); }
        throw new RuntimeException("no spell question");
    }
}
