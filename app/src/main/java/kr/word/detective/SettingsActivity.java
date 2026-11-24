package kr.word.detective;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import kr.word.detective.core.Data;

/** 설정: 탐정 이름, 한 번에 푸는 단어 수, 시간 제한, 소리, 앱 업데이트. */
public class SettingsActivity extends SubActivity {
    private String status = "";
    private boolean working;
    private EditText name;

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        final Data d = Store.get(this);
        String keep = name == null ? d.hero : name.getText().toString();
        LinearLayout page = Ui.page(this);

        LinearLayout who = Ui.card(page);
        who.addView(Ui.heading(this, "탐정 이름"));
        name = Ui.input(this, "예: 지우", false);
        name.setText(keep);
        LinearLayout r = Ui.line(this);
        r.addView(name, Ui.fill());
        r.addView(Ui.chip(this, "저장", false, () -> {
            String n = name.getText().toString().trim();
            d.hero = n.isEmpty() ? "꼬마" : n;
            Store.save(this);
            Toast.makeText(this, d.hero + " 탐정으로 바꿨어요.", Toast.LENGTH_SHORT).show();
        }), Ui.after(this, 8));
        who.addView(r);

        LinearLayout study = Ui.card(page);
        study.addView(Ui.heading(this, "한 번에 푸는 단어 수"));
        for (final int n : new int[] {5, 10, 15, 20}) {
            Ui.choice(study, n + "개" + (n == 10 ? " (기본)" : ""), d.perSession == n, () -> { d.perSession = n; Store.save(this); render(); });
        }

        LinearLayout opt = Ui.card(page);
        opt.addView(Ui.heading(this, "문제 풀 때"));
        Ui.choice(opt, "단어를 소리로 읽어 주기", d.sound, () -> { d.sound = !d.sound; Store.save(this); render(); });
        Ui.choice(opt, "시간 제한 두기 (번개 사건: 한 문제 20초, 스펠링 30초)", d.timer, () -> { d.timer = !d.timer; Store.save(this); render(); });

        LinearLayout up = Ui.card(page);
        up.addView(Ui.heading(this, "앱 버전"));
        up.addView(Ui.note(this, "지금 버전 " + Updater.currentName(this)
                + (Updater.hasUpdate(this) ? "  →  새 버전 " + Store.remoteName(this) + " 이 있어요" : "")));
        if (!status.isEmpty()) up.addView(Ui.note(this, status));
        if (Updater.hasUpdate(this)) Ui.button(up, working ? "받는 중…" : "새 버전 설치", Ui.BTN_PRIMARY, this::install);
        Ui.button(up, working ? "확인 중…" : "새 버전 확인", Ui.BTN_PLAIN, this::check);
    }

    private void check() {
        if (working) return;
        working = true;
        status = "";
        render();
        new Thread(() -> {
            final Updater.Remote rm = Updater.check(getApplicationContext());
            runOnUiThread(() -> {
                working = false;
                if (rm == null) status = "확인하지 못했어요. 인터넷 연결을 확인해 주세요.";
                else status = Updater.hasUpdate(this) ? "" : "지금이 가장 새 버전이에요.";
                if (!isFinishing()) render();
            });
        }).start();
    }

    private void install() {
        if (working) return;
        if (!Updater.ensureInstallPermission(this)) {
            status = "'이 출처 허용'을 켠 다음 돌아와서 [새 버전 설치]를 다시 눌러 주세요.";
            render();
            return;
        }
        working = true;
        status = "새 버전을 받고 있어요…";
        render();
        new Thread(() -> {
            final String err = Updater.downloadAndInstall(getApplicationContext());
            runOnUiThread(() -> {
                working = false;
                status = err == null ? "설치 확인 창이 뜨면 [업데이트]를 눌러 주세요." : err;
                if (!isFinishing()) render();
            });
        }).start();
    }
}
