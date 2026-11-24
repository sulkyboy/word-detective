package kr.word.detective;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import kr.word.detective.core.Data;
import kr.word.detective.core.Deck;
import kr.word.detective.core.Word;

/**
 * 단어 확인·고치기. 두 가지로 쓰인다.
 *  - 사진에서 읽었거나 붙여넣은 단어(Store.pending)를 확인해서 새 단어장으로 저장
 *  - 이미 있는 단어장(deck)의 단어를 고치거나 더하기
 */
public class EditActivity extends SubActivity {
    private Deck deck;                       // 고치는 단어장 (새로 만들 때는 null)
    private EditText name;
    private LinearLayout rows;
    private final List<EditText[]> fields = new ArrayList<EditText[]>();
    private final List<Word> origin = new ArrayList<Word>();   // 각 줄의 원래 단어 (공부 기록을 잇기 위해). 새 줄은 null

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Data d = Store.get(this);
        deck = d.deck(getIntent().getLongExtra("deck", 0));
        List<Word> src = deck != null ? deck.words : Store.pending;

        LinearLayout page = Ui.page(this);
        LinearLayout top = Ui.card(page);
        if (deck == null) {
            top.addView(Ui.heading(this, "읽어 온 단어 " + src.size() + "개"));
            top.addView(Ui.note(this, "잘못 읽은 글자는 눌러서 고치고, 필요 없는 줄은 ✕ 로 지워 주세요. 뜻이 빈 줄은 뜻을 넣어야 문제로 나옵니다. 다 보았으면 맨 아래 [저장]을 눌러 주세요."));
            Ui.button(top, "이대로 저장", Ui.BTN_PRIMARY, this::save);
        }
        top.addView(Ui.label(this, "단어장 이름"));
        name = Ui.input(this, "예: Day 1, 3단원", false);
        name.setText(deck != null ? deck.name : "단어장 " + new SimpleDateFormat("M월 d일", Locale.KOREA).format(new Date()));
        top.addView(name);

        LinearLayout list = Ui.card(page);
        list.addView(Ui.heading(this, "단어"));
        rows = Ui.column(this);
        list.addView(rows);
        for (Word w : src) addRow(w, w.en, w.ko);
        if (src.isEmpty()) addRow(null, "", "");
        Ui.button(list, "뜻이 빈 줄 모두 지우기", Ui.BTN_PLAIN, () -> {
            for (int i = fields.size() - 1; i >= 0; i--) {
                if (!fields.get(i)[1].getText().toString().trim().isEmpty()) continue;
                rows.removeViewAt(i);
                fields.remove(i);
                origin.remove(i);
            }
        });
        Ui.button(list, "＋ 단어 한 줄 더하기", Ui.BTN_PLAIN, () -> {
            EditText[] f = addRow(null, "", "");
            f[0].requestFocus();
        });

        Ui.button(page, "저장", Ui.BTN_PRIMARY, this::save);
    }

    private EditText[] addRow(Word from, String en, String ko) {
        final LinearLayout row = Ui.line(this);
        final EditText e = Ui.input(this, "영어", true);
        e.setText(en);
        final EditText k = Ui.input(this, "뜻", false);
        k.setText(ko);
        row.addView(e, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams kp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f);
        kp.leftMargin = Ui.dp(this, 6);
        row.addView(k, kp);
        final EditText[] pair = {e, k};
        row.addView(Ui.chip(this, "✕", false, () -> {
            int i = fields.indexOf(pair);
            if (i >= 0) { fields.remove(i); origin.remove(i); }
            rows.removeView(row);
        }), Ui.after(this, 6));
        rows.addView(row, Ui.below(this, 6));
        fields.add(pair);
        origin.add(from);
        return pair;
    }

    private void save() {
        Data d = Store.get(this);
        List<Word> out = new ArrayList<Word>();
        for (int i = 0; i < fields.size(); i++) {
            String en = fields.get(i)[0].getText().toString().trim();
            String ko = fields.get(i)[1].getText().toString().trim();
            if (en.isEmpty()) continue;
            Word o = deck != null ? origin.get(i) : null;
            Word w = new Word(en, ko);
            if (o != null && o.en.equalsIgnoreCase(en)) {   // 같은 단어면 공부 기록을 잇는다
                w.seen = o.seen; w.box = o.box; w.due = o.due; w.right = o.right; w.wrong = o.wrong;
            }
            out.add(w);
        }
        if (out.isEmpty()) {
            Toast.makeText(this, "단어를 하나 이상 넣어 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        String title = name.getText().toString().trim();
        if (title.isEmpty()) title = "이름 없는 단어장";
        boolean fresh = deck == null;
        if (fresh) {
            deck = new Deck();
            deck.id = d.nextId++;
            d.decks.add(deck);
            Store.pending = new ArrayList<Word>();
        }
        deck.name = title;
        deck.words.clear();
        deck.words.addAll(out);
        Store.save(this);
        Toast.makeText(this, "단어 " + out.size() + "개를 저장했어요.", Toast.LENGTH_SHORT).show();
        // 새로 만든 단어장은 바로 그 단어장 화면으로 (거기서 [도전장 받기]로 문제를 푼다)
        if (fresh) startActivity(new android.content.Intent(this, DeckActivity.class).putExtra("deck", deck.id).putExtra("fresh", true));
        finish();
    }
}
