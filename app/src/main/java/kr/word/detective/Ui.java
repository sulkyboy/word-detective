package kr.word.detective;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** 화면들이 같이 쓰는 모양 (화면은 모두 코드로 만든다). */
final class Ui {
    private Ui() {}

    static final int PAGE = 0xFFF4F1EA;     // 화면 바탕 (크림색)
    static final int CARD = 0xFFFFFFFF;     // 묶음 바탕
    static final int INK = 0xFF1F2A44;      // 글자 (남색)
    static final int SUB = 0xFF6B7280;      // 설명 글자
    static final int LINE = 0xFFD9DEE5;     // 테두리
    static final int CHIP = 0xFFE9EDF2;     // 작은 단추 바탕
    static final int CHIP_DOWN = 0xFFD5DBE3;
    static final int ACCENT = 0xFF2F6FDE;   // 주요 단추 (탐정의 파랑)
    static final int ACCENT_DOWN = 0xFF2457B5;
    static final int DANGER = 0xFFD7372F;   // 괴도의 빨강, 삭제, 오답
    static final int DANGER_DOWN = 0xFFB02B25;
    static final int GOOD = 0xFF2E9E5B;     // 정답
    static final int GOOD_DOWN = 0xFF24804A;
    static final int GOLD = 0xFFF2B01E;     // 단서(점수)
    static final int NIGHT = 0xFF24345C;    // 짙은 남색 묶음

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /** 화면 틀: 회색 바탕에 세로로 쌓는 상자(스크롤). 상자를 돌려준다. */
    static LinearLayout page(Activity a) {
        ScrollView scroll = new ScrollView(a);
        scroll.setBackgroundColor(PAGE);
        scroll.setFillViewport(true);
        LinearLayout box = column(a);
        int p = dp(a, 14);
        box.setPadding(p, p, p, dp(a, 28));
        scroll.addView(box, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        a.setContentView(scroll);
        return box;
    }

    /** 하위 화면: 위쪽 제목 줄에 ← 를 보인다 (누르면 SubActivity 가 닫는다) */
    static void backArrow(Activity a) {
        ActionBar bar = a.getActionBar();
        if (bar != null) bar.setDisplayHomeAsUpEnabled(true);
    }

    static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout line(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    /** 흰색 둥근 묶음. parent 에 붙여서 돌려준다. */
    static LinearLayout card(LinearLayout parent) {
        Context c = parent.getContext();
        LinearLayout l = column(c);
        l.setBackground(round(c, CARD, 14));
        int p = dp(c, 14);
        l.setPadding(p, dp(c, 12), p, dp(c, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 12);
        parent.addView(l, lp);
        return l;
    }

    static TextView text(Context c, String s, float sp, boolean bold, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    /** 묶음 제목 */
    static TextView heading(Context c, String s) {
        TextView t = text(c, s, 16, true, INK);
        t.setPadding(0, 0, 0, dp(c, 4));
        return t;
    }

    /** 설명 글 */
    static TextView note(Context c, String s) {
        TextView t = text(c, s, 13, false, SUB);
        t.setPadding(0, dp(c, 2), 0, dp(c, 4));
        t.setLineSpacing(dp(c, 2), 1f);
        return t;
    }

    /** 항목 이름 (입력칸·색 줄 위) */
    static TextView label(Context c, String s) {
        TextView t = text(c, s, 14, true, INK);
        t.setPadding(0, dp(c, 12), 0, dp(c, 4));
        return t;
    }

    static GradientDrawable round(Context c, int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    /** 누르면 색이 짙어지는 바탕 */
    static Drawable pressable(Context c, int color, int down, float radiusDp) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[] {android.R.attr.state_pressed}, round(c, down, radiusDp));
        s.addState(new int[0], round(c, color, radiusDp));
        return s;
    }

    static final int BTN_PRIMARY = 0, BTN_PLAIN = 1, BTN_DANGER = 2, BTN_GOOD = 3;

    /** 큰 단추 (가로로 꽉 참). parent 에 붙여서 돌려준다. */
    static TextView button(LinearLayout parent, String label, int kind, final Runnable action) {
        Context c = parent.getContext();
        TextView t = text(c, label, 16, true, kind == BTN_PLAIN ? INK : 0xFFFFFFFF);
        style(t, kind);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 12), dp(c, 15), dp(c, 12), dp(c, 15));
        click(t, action);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 10);
        parent.addView(t, lp);
        return t;
    }

    static void style(TextView t, int kind) {
        Context c = t.getContext();
        if (kind == BTN_PRIMARY) { t.setBackground(pressable(c, ACCENT, ACCENT_DOWN, 10)); t.setTextColor(0xFFFFFFFF); }
        else if (kind == BTN_DANGER) { t.setBackground(pressable(c, DANGER, DANGER_DOWN, 10)); t.setTextColor(0xFFFFFFFF); }
        else if (kind == BTN_GOOD) { t.setBackground(pressable(c, GOOD, GOOD_DOWN, 10)); t.setTextColor(0xFFFFFFFF); }
        else { t.setBackground(pressable(c, CHIP, CHIP_DOWN, 10)); t.setTextColor(INK); }
    }

    /** 작은 단추 (▲ ▼ ✕, 붙여넣기 등) */
    static TextView chip(Context c, String label, boolean hot, final Runnable action) {
        TextView t = text(c, label, 15, true, hot ? 0xFFFFFFFF : 0xFF3B4250);
        t.setBackground(hot ? pressable(c, DANGER, DANGER_DOWN, 8) : pressable(c, CHIP, CHIP_DOWN, 8));
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 13), dp(c, 9), dp(c, 13), dp(c, 9));
        click(t, action);
        return t;
    }

    /**
     * 여러 개 중 하나를 고르는 줄 (고른 것은 ✔ 와 파란 테두리). parent 에 붙여서 돌려준다.
     */
    static TextView choice(LinearLayout parent, String label, boolean on, final Runnable action) {
        Context c = parent.getContext();
        TextView t = text(c, (on ? "✔  " : "") + label, 14, on, on ? ACCENT : INK);
        GradientDrawable g = round(c, on ? 0xFFEAF2FF : CARD, 10);
        g.setStroke(dp(c, on ? 2 : 1), on ? ACCENT : LINE);
        t.setBackground(g);
        t.setPadding(dp(c, 12), dp(c, 12), dp(c, 12), dp(c, 12));
        click(t, action);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 8);
        parent.addView(t, lp);
        return t;
    }

    static void click(View v, final Runnable action) {
        v.setClickable(true);
        v.setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) { action.run(); }
        });
    }

    /** 가로 줄 안에서 남는 폭을 다 쓰는 자리 */
    static LinearLayout.LayoutParams fill() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    /** 가로 줄 안에서 왼쪽에 간격을 두는 자리 */
    static LinearLayout.LayoutParams after(Context c, int gapDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(c, gapDp);
        return lp;
    }

    // ---------- 단어 탐정에서 더한 것 ----------

    /** 그림(캐릭터). 높이를 정하면 폭은 비율대로. */
    static ImageView image(Context c, int res, int heightDp) {
        ImageView v = new ImageView(c);
        v.setImageResource(res);
        v.setAdjustViewBounds(true);
        v.setScaleType(ImageView.ScaleType.FIT_CENTER);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(c, heightDp)));
        return v;
    }

    /** 캐릭터가 말하는 줄: 왼쪽에 그림, 오른쪽에 이름과 말풍선. parent 에 붙이고 말풍선 글자를 돌려준다. */
    static TextView say(LinearLayout parent, int res, String who, String words, int color) {
        Context c = parent.getContext();
        LinearLayout row = line(c);
        row.setGravity(Gravity.BOTTOM);
        row.addView(image(c, res, 76));
        LinearLayout col = column(c);
        TextView name = text(c, who, 12, true, color);
        name.setPadding(dp(c, 6), 0, 0, dp(c, 3));
        col.addView(name);
        TextView t = text(c, words, 15, false, INK);
        GradientDrawable g = round(c, CARD, 14);
        g.setStroke(dp(c, 2), color);
        t.setBackground(g);
        t.setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10));
        t.setLineSpacing(dp(c, 3), 1f);
        col.addView(t);
        LinearLayout.LayoutParams lp = fill();
        lp.leftMargin = dp(c, 8);
        row.addView(col, lp);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.topMargin = dp(c, 12);
        parent.addView(row, rp);
        return t;
    }

    /** 진행 막대 (0~100). parent 에 붙인다. */
    static void bar(LinearLayout parent, int percent, int color) {
        Context c = parent.getContext();
        LinearLayout track = line(c);
        track.setBackground(round(c, CHIP, 5));
        int p = Math.max(0, Math.min(100, percent));
        View on = new View(c);
        on.setBackground(round(c, color, 5));
        track.addView(on, new LinearLayout.LayoutParams(0, dp(c, 10), p));
        track.addView(new View(c), new LinearLayout.LayoutParams(0, dp(c, 10), 100 - p));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 10));
        lp.topMargin = dp(c, 8);
        parent.addView(track, lp);
    }

    /** 글자 입력칸 */
    static EditText input(Context c, String hint, boolean english) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setTextSize(17);
        e.setTextColor(INK);
        e.setSingleLine(true);
        // 영어 칸은 자동 고침·추천 낱말이 답을 알려 주지 않게 끈다
        e.setInputType(english ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS : InputType.TYPE_CLASS_TEXT);
        GradientDrawable g = round(c, CARD, 10);
        g.setStroke(dp(c, 1), LINE);
        e.setBackground(g);
        e.setPadding(dp(c, 12), dp(c, 11), dp(c, 12), dp(c, 11));
        return e;
    }

    /** 세로로 쌓을 때 위쪽 간격을 둔 자리 (가로로 꽉 참) */
    static LinearLayout.LayoutParams below(Context c, int gapDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, gapDp);
        return lp;
    }

    /** 가운데 정렬 큰 글자 */
    static TextView big(Context c, String s, float sp, int color) {
        TextView t = text(c, s, sp, true, color);
        t.setGravity(Gravity.CENTER);
        return t;
    }
}
