package kr.word.detective;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import kr.word.detective.core.Data;
import kr.word.detective.core.Rank;
import kr.word.detective.core.Srs;
import kr.word.detective.core.Word;

/** 첫 화면: 탐정 사무소. 탐정의 등급·단서와, 오늘 할 일을 보여 준다. */
public class HomeActivity extends Activity {
    private boolean asked;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        // 새 버전이 있는지 조용히 확인 (결과는 다음에 화면을 그릴 때 보인다)
        new Thread(() -> {
            if (Updater.check(getApplicationContext()) != null) runOnUiThread(this::render);
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
        Data d = Store.get(this);
        if (d.hero.isEmpty() && !asked) { asked = true; askName(); }
    }

    private void render() {
        final Data d = Store.get(this);
        long today = Store.today();
        LinearLayout page = Ui.page(this);
        String hero = d.hero.isEmpty() ? "꼬마" : d.hero;
        setTitle("단어 탐정 사무소");

        // 탐정 카드
        LinearLayout card = Ui.card(page);
        LinearLayout row = Ui.line(this);
        row.addView(Ui.image(this, R.drawable.ch_detective, 104));
        row.addView(Ui.image(this, R.drawable.ch_jjoko, 76));
        LinearLayout info = Ui.column(this);
        info.addView(Ui.text(this, hero + " 탐정", 20, true, Ui.INK));
        info.addView(Ui.text(this, Rank.name(d.points), 14, true, Ui.ACCENT));
        info.addView(Ui.text(this, "단서 " + d.points + "개", 14, true, 0xFFB57F00));
        LinearLayout.LayoutParams ip = Ui.fill();
        ip.leftMargin = Ui.dp(this, 10);
        row.addView(info, ip);
        card.addView(row);
        Ui.bar(card, Rank.progress(d.points), Ui.GOLD);
        int toNext = Rank.toNext(d.points);
        card.addView(Ui.note(this, toNext > 0
                ? "단서 " + toNext + "개를 더 모으면 '" + Rank.nextName(d.points) + "'"
                : "최고 등급에 올랐어요!"));
        card.addView(Ui.note(this, "연속 출석 " + d.liveStreak(today) + "일  ·  해결한 사건 " + d.solved + "건"));

        // 오늘의 상황
        int total = 0, due = 0, fresh = 0, unsolved = 0;
        for (Word w : d.allWords()) {
            if (w.ko.isEmpty()) continue;
            total++;
            if (Srs.due(w, today)) due++;
            if (!w.seen) fresh++;
            if (Srs.unsolved(w)) unsolved++;
        }
        String line;
        if (total == 0) line = "후후후, 단어장도 없이 나를 잡겠다고? 먼저 '사건 파일'에서 단어장 사진을 찍어 오너라!";
        else if (due > 0) line = "오늘 다시 풀어야 할 단서가 " + due + "개나 남았다. 풀 수 있겠나, " + hero + " 탐정?";
        else if (fresh > 0) line = "아직 손도 못 댄 단어가 " + fresh + "개다. 내 도전장을 받아라!";
        else line = "흥, 오늘 것은 다 풀었군. 그래도 한 판 더 붙어 보겠나?";
        Ui.say(page, R.drawable.ch_thief, "괴도아빠", line, Ui.DANGER);

        final boolean any = total > 0;
        Ui.button(page, "괴도의 도전장 받기 (단어 공부)", Ui.BTN_PRIMARY, () -> {
            if (!any) { open(DecksActivity.class); return; }
            startActivity(new Intent(this, StudyActivity.class));
        });
        if (unsolved > 0) {
            Ui.button(page, "미해결 사건 풀기 (틀린 단어 " + unsolved + "개)", Ui.BTN_DANGER,
                    () -> startActivity(new Intent(this, StudyActivity.class).putExtra("unsolved", true)));
        }
        if (total >= 4) Ui.button(page, "짝 맞추기 게임", Ui.BTN_GOOD, () -> open(MatchActivity.class));
        Ui.button(page, "사건 파일 (단어장 " + d.decks.size() + "개)", Ui.BTN_PLAIN, () -> open(DecksActivity.class));
        Ui.button(page, "수사 기록 (부모님용)", Ui.BTN_PLAIN, () -> open(RecordActivity.class));
        Ui.button(page, Updater.hasUpdate(this) ? "설정  ·  새 버전이 있어요" : "설정", Ui.BTN_PLAIN, () -> open(SettingsActivity.class));

        TextView ver = Ui.note(this, "버전 " + Updater.currentName(this));
        ver.setGravity(Gravity.CENTER);
        page.addView(ver, Ui.below(this, 14));
    }

    private void open(Class<?> c) { startActivity(new Intent(this, c)); }

    /** 처음 켰을 때 탐정 이름을 묻는다 */
    private void askName() {
        final EditText e = Ui.input(this, "예: 지우", false);
        LinearLayout box = Ui.column(this);
        int p = Ui.dp(this, 20);
        box.setPadding(p, Ui.dp(this, 8), p, 0);
        box.addView(Ui.note(this, "괴도아빠를 쫓는 탐정의 이름을 지어 주세요. 나중에 설정에서 바꿀 수 있어요."));
        box.addView(e);
        new AlertDialog.Builder(this)
                .setTitle("탐정 이름 짓기")
                .setView(box)
                .setCancelable(false)
                .setPositiveButton("정했어요", (dlg, w) -> {
                    String name = e.getText().toString().trim();
                    Store.get(this).hero = name.isEmpty() ? "꼬마" : name;
                    Store.save(this);
                    render();
                })
                .show();
    }
}
