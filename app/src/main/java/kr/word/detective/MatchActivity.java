package kr.word.detective;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kr.word.detective.core.Data;
import kr.word.detective.core.Deck;
import kr.word.detective.core.Word;

/** 짝 맞추기 게임: 영어 단어 카드와 뜻 카드를 짝지어 누른다. 괴도아빠가 흩어 놓은 증거를 맞추는 놀이. */
public class MatchActivity extends SubActivity {
    private static final int PAIRS = 6;
    private final List<String> texts = new ArrayList<String>();   // 카드 글자
    private final List<Integer> pairOf = new ArrayList<Integer>(); // 카드가 속한 짝 번호
    private final List<TextView> cards = new ArrayList<TextView>();
    private boolean[] done;
    private int picked = -1, misses, found;
    private long startAt;
    private boolean locked;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Data d = Store.get(this);
        Deck deck = d.deck(getIntent().getLongExtra("deck", 0));
        List<Word> pool = new ArrayList<Word>();
        for (Word w : deck != null ? deck.words : d.allWords()) {
            if (w.ko.isEmpty()) continue;
            boolean dup = false;
            for (Word p : pool) if (p.en.equalsIgnoreCase(w.en) || p.ko.equals(w.ko)) dup = true;
            if (!dup) pool.add(w);
        }
        Collections.shuffle(pool);
        int n = Math.min(PAIRS, pool.size());
        List<int[]> order = new ArrayList<int[]>();
        for (int i = 0; i < n; i++) { order.add(new int[] {i, 0}); order.add(new int[] {i, 1}); }
        Collections.shuffle(order);
        for (int[] o : order) {
            texts.add(o[1] == 0 ? pool.get(o[0]).en : pool.get(o[0]).ko);
            pairOf.add(o[0]);
        }
        done = new boolean[texts.size()];
        startAt = SystemClock.elapsedRealtime();
        render();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void render() {
        LinearLayout page = Ui.page(this);
        cards.clear();
        if (texts.size() < 4) {
            Ui.say(page, R.drawable.ch_jjoko, "쪼코", "짝 맞추기는 단어가 2개 이상 있어야 할 수 있어요.", Ui.ACCENT);
            Ui.button(page, "돌아가기", Ui.BTN_PLAIN, this::finish);
            return;
        }
        if (found * 2 >= texts.size()) { renderEnd(page); return; }
        Ui.say(page, R.drawable.ch_thief, "괴도아빠", "증거 카드를 뒤섞어 놓았다! 영어 단어와 뜻을 짝지어 보거라.", Ui.DANGER);
        for (int i = 0; i < texts.size(); i += 2) {
            LinearLayout row = Ui.line(this);
            for (int k = i; k < i + 2 && k < texts.size(); k++) {
                final int at = k;
                TextView t = Ui.text(this, texts.get(k), 16, true, Ui.INK);
                t.setGravity(Gravity.CENTER);
                t.setMinHeight(Ui.dp(this, 64));
                t.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));
                Ui.click(t, () -> tap(at));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                if (k > i) lp.leftMargin = Ui.dp(this, 8);
                row.addView(t, lp);
                cards.add(t);
                paint(k, 0);
            }
            page.addView(row, Ui.below(this, 8));
        }
    }

    /** state 0 보통, 1 고름, 2 틀림 — 맞춘 카드는 초록 */
    private void paint(int i, int state) {
        TextView t = cards.get(i);
        int fill = done[i] ? 0xFFDFF3E6 : state == 1 ? 0xFFEAF2FF : state == 2 ? 0xFFFDE3E1 : Ui.CARD;
        int edge = done[i] ? Ui.GOOD : state == 1 ? Ui.ACCENT : state == 2 ? Ui.DANGER : Ui.LINE;
        GradientDrawable g = Ui.round(this, fill, 12);
        g.setStroke(Ui.dp(this, 2), edge);
        t.setBackground(g);
        t.setTextColor(done[i] ? Ui.GOOD : Ui.INK);
    }

    private void tap(final int i) {
        if (locked || done[i]) return;
        if (picked == i) { picked = -1; paint(i, 0); return; }
        if (picked < 0) { picked = i; paint(i, 1); return; }
        final int a = picked;
        picked = -1;
        if (pairOf.get(a).equals(pairOf.get(i))) {
            done[a] = done[i] = true;
            found++;
            paint(a, 0);
            paint(i, 0);
            if (found * 2 >= texts.size()) handler.postDelayed(this::render, 500);
        } else {
            misses++;
            locked = true;
            paint(a, 2);
            paint(i, 2);
            handler.postDelayed(() -> { locked = false; paint(a, 0); paint(i, 0); }, 600);
        }
    }

    private void renderEnd(LinearLayout page) {
        Data d = Store.get(this);
        int secs = (int) ((SystemClock.elapsedRealtime() - startAt) / 1000);
        int gain = found * 2 + (misses == 0 ? 6 : 0);
        if (startAt != 0) {          // 점수는 한 번만
            d.points += gain;
            d.record(Store.today(), 0, 0);
            Store.save(this);
            startAt = 0;
            getIntent().putExtra("secs", secs).putExtra("gain", gain);
        }
        secs = getIntent().getIntExtra("secs", secs);
        gain = getIntent().getIntExtra("gain", gain);
        page.addView(Ui.big(this, "증거를 모두 맞췄어요!", 24, Ui.GOOD));
        Ui.say(page, R.drawable.ch_detective, (d.hero.isEmpty() ? "꼬마" : d.hero) + " 탐정",
                secs + "초 만에 " + found + "쌍을 찾았다!  틀린 횟수 " + misses + "번.  단서 +" + gain + "개" + (misses == 0 ? " (한 번도 안 틀려서 +6)" : ""), Ui.GOOD);
        Ui.button(page, "한 판 더!", Ui.BTN_PRIMARY, this::recreate);
        Ui.button(page, "돌아가기", Ui.BTN_PLAIN, this::finish);
    }
}
