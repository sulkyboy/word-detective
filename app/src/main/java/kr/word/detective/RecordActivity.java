package kr.word.detective;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import kr.word.detective.core.Data;
import kr.word.detective.core.Rank;
import kr.word.detective.core.Srs;
import kr.word.detective.core.Word;

/** 수사 기록 (부모님용): 얼마나 공부했고 어떤 단어를 자주 틀리는지. */
public class RecordActivity extends SubActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Data d = Store.get(this);
        long today = Store.today();
        LinearLayout page = Ui.page(this);

        List<Word> all = d.allWords();
        int seen = 0, mastered = 0, unsolved = 0, due = 0;
        for (Word w : all) {
            if (w.seen) seen++;
            if (w.box >= 3) mastered++;
            if (Srs.unsolved(w)) unsolved++;
            if (Srs.due(w, today)) due++;
        }
        LinearLayout sum = Ui.card(page);
        sum.addView(Ui.heading(this, "한눈에 보기"));
        row(sum, "탐정 등급", Rank.name(d.points) + "  (단서 " + d.points + "개)");
        row(sum, "연속 출석", d.liveStreak(today) + "일");
        row(sum, "끝낸 공부(사건)", d.solved + "번");
        row(sum, "괴도의 요구를 들어준 횟수", d.stage + "번");
        row(sum, "쪼꼬를 만난 횟수 (80% 이상)", d.meets + "번");
        row(sum, "전체 단어", all.size() + "개");
        row(sum, "한 번 이상 공부한 단어", seen + "개");
        row(sum, "다 외운 단어 (★ 3개 이상)", mastered + "개");
        row(sum, "오늘 복습할 단어", due + "개");
        row(sum, "자꾸 틀리는 단어 (미해결)", unsolved + "개");

        LinearLayout days = Ui.card(page);
        days.addView(Ui.heading(this, "최근 2주 공부"));
        SimpleDateFormat f = new SimpleDateFormat("M월 d일 (E)", Locale.KOREA);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));     // 날짜 번호는 이미 휴대폰 시간대로 센 것
        boolean any = false;
        for (long day = today; day > today - 14; day--) {
            int[] h = d.history.get(day);
            if (h == null) continue;
            any = true;
            String what = h[0] == 0 ? "게임만 했어요" : "문제 " + h[0] + "개 · 정답 " + h[1] + "개 (" + (100 * h[1] / h[0]) + "%)";
            row(days, f.format(new Date(day * 86400000L)), what);
        }
        if (!any) days.addView(Ui.note(this, "아직 공부한 기록이 없어요."));

        List<Word> hard = new ArrayList<Word>();
        for (Word w : all) if (w.wrong > 0) hard.add(w);
        Collections.sort(hard, new Comparator<Word>() {
            public int compare(Word a, Word c) { return c.wrong != a.wrong ? c.wrong - a.wrong : a.box - c.box; }
        });
        LinearLayout hc = Ui.card(page);
        hc.addView(Ui.heading(this, "자주 틀리는 단어"));
        if (hard.isEmpty()) hc.addView(Ui.note(this, "아직 틀린 단어가 없어요."));
        for (int i = 0; i < hard.size() && i < 15; i++) {
            Word w = hard.get(i);
            row(hc, w.en + "  —  " + w.ko, "틀림 " + w.wrong + " · 맞음 " + w.right);
        }
    }

    private void row(LinearLayout parent, String left, String right) {
        LinearLayout r = Ui.line(this);
        r.addView(Ui.text(this, left, 14, false, Ui.INK), Ui.fill());
        TextView v = Ui.text(this, right, 14, true, Ui.INK);
        r.addView(v, Ui.after(this, 10));
        r.setPadding(0, Ui.dp(this, 7), 0, Ui.dp(this, 7));
        parent.addView(r);
    }
}
