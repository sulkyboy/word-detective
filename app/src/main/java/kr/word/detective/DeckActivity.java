package kr.word.detective;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import kr.word.detective.core.Data;
import kr.word.detective.core.Deck;
import kr.word.detective.core.Word;

/** 단어장 하나: 단어 목록과 진행 상황. 여기서 이 단어장만 공부하거나 고칠 수 있다. */
public class DeckActivity extends SubActivity {
    private boolean armed;   // [지우기]를 한 번 누른 상태

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        final Data d = Store.get(this);
        final Deck deck = d.deck(getIntent().getLongExtra("deck", 0));
        if (deck == null) { finish(); return; }
        setTitle(deck.name);
        LinearLayout page = Ui.page(this);

        int ready = 0;
        for (Word w : deck.words) if (!w.ko.isEmpty()) ready++;
        if (getIntent().getBooleanExtra("fresh", false)) {
            Ui.say(page, R.drawable.ch_jjoko, "쪼꼬가 몰래 보낸 쪽지", ready > 0
                    ? "멍! 단어장이 준비됐어요. 아래 [이 단어장으로 도전장 받기]를 누르면 문제가 시작돼요!"
                    : "뜻이 들어 있는 단어가 없어요. [단어 고치기·더하기]에서 뜻을 넣어 주세요.", Ui.ACCENT);
        }
        LinearLayout top = Ui.card(page);
        top.addView(Ui.heading(this, deck.name));
        top.addView(Ui.note(this, "단어 " + deck.words.size() + "개  ·  다 외운 단어 " + deck.mastered() + "개"));
        Ui.bar(top, deck.words.isEmpty() ? 0 : 100 * deck.mastered() / deck.words.size(), Ui.GOOD);
        if (ready > 0) {
            Ui.button(top, "이 단어장으로 도전장 받기", Ui.BTN_PRIMARY,
                    () -> startActivity(new Intent(this, StudyActivity.class).putExtra("deck", deck.id)));
            if (ready >= 4) Ui.button(top, "이 단어장으로 짝 맞추기", Ui.BTN_GOOD,
                    () -> startActivity(new Intent(this, MatchActivity.class).putExtra("deck", deck.id)));
        }
        Ui.button(top, "단어 고치기·더하기", Ui.BTN_PLAIN,
                () -> startActivity(new Intent(this, EditActivity.class).putExtra("deck", deck.id)));

        LinearLayout list = Ui.card(page);
        list.addView(Ui.heading(this, "단어 목록"));
        list.addView(Ui.note(this, "★ 이 많을수록 잘 아는 단어예요. 틀리면 ★ 이 다시 줄어듭니다."));
        for (Word w : deck.words) {
            LinearLayout row = Ui.line(this);
            LinearLayout col = Ui.column(this);
            col.addView(Ui.text(this, w.en, 16, true, Ui.INK));
            col.addView(Ui.text(this, w.ko.isEmpty() ? "(뜻이 비어 있어요 — 고치기에서 넣어 주세요)" : w.ko, 13, false, w.ko.isEmpty() ? Ui.DANGER : Ui.SUB));
            row.addView(col, Ui.fill());
            StringBuilder star = new StringBuilder();
            for (int i = 0; i < 5; i++) star.append(i < w.box ? "★" : "☆");
            TextView st = Ui.text(this, w.seen ? star.toString() : "새 단어", 13, true, w.seen ? 0xFFB57F00 : Ui.ACCENT);
            row.addView(st, Ui.after(this, 8));
            row.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
            list.addView(row);
        }

        Ui.button(page, armed ? "정말 지울까요? 한 번 더 누르면 지워집니다" : "이 단어장 지우기", armed ? Ui.BTN_DANGER : Ui.BTN_PLAIN, () -> {
            if (!armed) { armed = true; render(); return; }
            d.decks.remove(deck);
            Store.save(this);
            finish();
        });
    }
}
