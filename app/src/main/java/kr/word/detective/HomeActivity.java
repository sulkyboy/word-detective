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
import kr.word.detective.core.Mission;
import kr.word.detective.core.Rank;
import kr.word.detective.core.Srs;
import kr.word.detective.core.Word;

/** 첫 화면: 탐정 사무소. 탐정의 등급·단서와, 오늘 할 일을 보여 준다. */
public class HomeActivity extends Activity {
    private boolean asked;
    private boolean introUp;     // 인트로 만화를 막 띄웠다 (그 뒤에 가려진 동안에는 이름을 묻지 않는다)

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (b == null) {
            if (Store.get(this).intro) {
                introUp = true;
                startActivity(new Intent(this, IntroActivity.class));
            }
        }
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
        if (introUp) { introUp = false; return; }
        // 요구를 들어줬으면: 풀어 달라고 하는 장면 (그리고 또 속는다)
        if (Mission.done(d)) {
            introUp = true;
            startActivity(new Intent(this, IntroActivity.class).putExtra("trick", true));
            return;
        }
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

        // 괴도의 요구: 이것만 하면 쪼코를 풀어 준다는데…
        LinearLayout op = Ui.card(page);
        op.addView(Ui.heading(this, "쪼코를 구하려면  ·  괴도의 " + (d.stage + 1) + "번째 요구"));
        LinearLayout or = Ui.line(this);
        or.addView(Ui.image(this, R.drawable.ch_thief, 60));
        or.addView(Ui.image(this, R.drawable.ch_jjoko, 48));
        or.addView(Ui.image(this, R.drawable.ch_helper, 60));
        TextView ot = Ui.text(this, Mission.task(d.stage), 16, true, Ui.DANGER);
        ot.setLineSpacing(Ui.dp(this, 3), 1f);
        LinearLayout.LayoutParams op2 = Ui.fill();
        op2.leftMargin = Ui.dp(this, 10);
        or.addView(ot, op2);
        op.addView(or);
        int have = Mission.have(d), need = Mission.need(d.stage);
        Ui.bar(op, 100 * have / need, Ui.DANGER);
        op.addView(Ui.note(this, (need > 1 ? have + " / " + need + "  ·  " : "") + "\"이것만 하면 풀어 준다\"는 말에 속은 횟수 " + d.stage + "번"));

        // 오늘의 상황
        int total = 0, due = 0, unsolved = 0;
        for (Word w : d.allWords()) {
            if (w.ko.isEmpty()) continue;
            total++;
            if (Srs.due(w, today)) due++;
            if (Srs.unsolved(w)) unsolved++;
        }
        String line = Mission.demand(d.stage);
        if (d.stage > 0 && due > 0) line += "  (오늘 다시 풀 단서도 " + due + "개 있다!)";
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
