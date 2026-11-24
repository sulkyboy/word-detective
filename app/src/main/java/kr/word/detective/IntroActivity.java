package kr.word.detective;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import kr.word.detective.core.Data;

/**
 * 만화 장면. 두 가지로 쓰인다.
 *  - 인트로(기본): 괴도아빠와 괴도엄마가 쪼코를 데려가고 도전장을 남긴다. 단서를 모아야 쪼코를 찾을 수 있다.
 *  - 구출(rescue=true): 단서를 다 모았을 때. 쪼코를 되찾는다.
 * 장면은 저절로 넘어가고, 화면을 누르면 바로 다음 장면으로 간다. [넘기기]는 언제든 끝낸다.
 */
public class IntroActivity extends Activity {
    private static final int NIGHT = 0xFF16203A, GROUND = 0xFF24345C;

    private boolean rescue;
    private Data d;
    private FrameLayout stage;
    private TextView caption, next, bubble, bang, card, title;
    private ImageView hero, thief, mom, dog;
    private View moon;
    private int W, H, scene = -1, last;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Animator> loops = new ArrayList<Animator>();
    private final Runnable auto = new Runnable() { public void run() { go(scene + 1); } };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        d = Store.get(this);
        rescue = getIntent().getBooleanExtra("rescue", false);
        last = rescue ? 2 : 4;
        if (rescue && b == null && d.captive) {      // 구출은 장면을 넘겨도 이루어지도록 먼저 적어 둔다
            d.captive = false;
            d.rescued++;
            d.freeDay = Store.today();
            Store.save(this);
        }

        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(NIGHT);
        int p = Ui.dp(this, 16);
        root.setPadding(p, Ui.dp(this, 36), p, Ui.dp(this, 20));

        LinearLayout top = Ui.line(this);
        title = Ui.text(this, "", 14, true, 0xFFB9C4E4);
        top.addView(title, Ui.fill());
        TextView skip = Ui.text(this, "넘기기  ▶▶", 14, true, 0xFFFFFFFF);
        GradientDrawable sg = Ui.round(this, 0x33FFFFFF, 18);
        skip.setBackground(sg);
        skip.setPadding(Ui.dp(this, 16), Ui.dp(this, 9), Ui.dp(this, 16), Ui.dp(this, 9));
        Ui.click(skip, this::finish);
        top.addView(skip);
        root.addView(top);

        stage = new FrameLayout(this);
        stage.setClipChildren(true);
        Ui.click(stage, () -> { if (scene < last) go(scene + 1); });
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        sp.topMargin = Ui.dp(this, 12);
        root.addView(stage, sp);

        caption = Ui.text(this, "", 17, true, 0xFFFFFFFF);
        caption.setGravity(Gravity.CENTER);
        caption.setMinLines(3);
        caption.setLineSpacing(Ui.dp(this, 4), 1f);
        root.addView(caption, Ui.below(this, 12));

        next = Ui.button(root, "다음  ▶", Ui.BTN_PRIMARY, () -> { if (scene >= last) finish(); else go(scene + 1); });
        setContentView(root);

        build();
        stage.post(() -> {
            W = stage.getWidth();
            H = stage.getHeight();
            go(0);
        });
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        stopLoops();
        super.onDestroy();
    }

    // ---------- 무대 ----------

    private void build() {
        // 밤하늘: 달과 별, 아래쪽 땅
        moon = new View(this);
        GradientDrawable mg = new GradientDrawable();
        mg.setShape(GradientDrawable.OVAL);
        mg.setColor(0xFFFFE9A6);
        moon.setBackground(mg);
        FrameLayout.LayoutParams mp = new FrameLayout.LayoutParams(Ui.dp(this, 54), Ui.dp(this, 54), Gravity.TOP | Gravity.RIGHT);
        mp.topMargin = Ui.dp(this, 18);
        mp.rightMargin = Ui.dp(this, 26);
        stage.addView(moon, mp);
        int[][] stars = {{8, 10}, {22, 26}, {37, 8}, {52, 20}, {66, 6}, {14, 40}, {45, 36}, {78, 34}, {90, 46}, {30, 52}};
        for (int[] s : stars) {
            View v = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(0xCCFFFFFF);
            v.setBackground(g);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Ui.dp(this, 4), Ui.dp(this, 4));
            v.setLayoutParams(lp);
            v.setTag(s);
            stage.addView(v);
        }
        View ground = new View(this);
        ground.setBackground(Ui.round(this, GROUND, 18));
        stage.addView(ground, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 70), Gravity.BOTTOM));

        hero = actor(R.drawable.ch_detective, 140, 208, 132);
        mom = actor(R.drawable.ch_helper, 138, 216, 132);
        dog = actor(R.drawable.ch_jjoko, 236, 286, 100);
        thief = actor(R.drawable.ch_thief, 150, 202, 132);

        bubble = label(15, 0xFF1F2A44, 0xFFFFFFFF, 0xFF1F2A44);
        bang = Ui.text(this, "!", 40, true, 0xFFFFD54A);
        bang.setAlpha(0f);
        stage.addView(bang, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        card = label(15, 0xFF5A4300, 0xFFFFFDF2, 0xFFC9A227);
        card.setGravity(Gravity.CENTER);
        card.setLineSpacing(Ui.dp(this, 4), 1f);
    }

    /** 등장인물 그림. 그림의 가로:세로 비율과 화면에서의 높이(dp)를 받아 크기를 정한다. */
    private ImageView actor(int res, int vw, int vh, int heightDp) {
        ImageView v = new ImageView(this);
        v.setImageResource(res);
        v.setScaleType(ImageView.ScaleType.FIT_XY);
        int h = Ui.dp(this, heightDp);
        stage.addView(v, new FrameLayout.LayoutParams(h * vw / vh, h));
        v.setAlpha(0f);
        return v;
    }

    private TextView label(float sp, int ink, int fill, int edge) {
        TextView t = Ui.text(this, "", sp, true, ink);
        GradientDrawable g = Ui.round(this, fill, 12);
        g.setStroke(Ui.dp(this, 2), edge);
        t.setBackground(g);
        t.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        t.setAlpha(0f);
        stage.addView(t, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return t;
    }

    // ---------- 움직임 도우미 (위치는 무대 폭에 대한 비율: 0 왼쪽 끝 ~ 1 오른쪽 끝, 그림의 가운데 기준) ----------

    private float xOf(View v, float fx) { return W * fx - v.getLayoutParams().width / 2f; }
    private float floorOf(View v) { return H - Ui.dp(this, 40) - v.getLayoutParams().height; }

    /** 그 자리에 세운다 */
    private void put(View v, float fx) {
        v.animate().cancel();
        v.setRotation(0f);
        v.setScaleX(1f);
        v.setScaleY(1f);
        v.setX(xOf(v, fx));
        v.setY(floorOf(v));
        v.setAlpha(1f);
    }

    private void hide(View v) { v.animate().cancel(); v.setAlpha(0f); }

    /** 걸어서(미끄러져) 옮긴다 */
    private void walk(View v, float fx, long delay, long dur) {
        v.animate().x(xOf(v, fx)).setStartDelay(delay).setDuration(dur).setInterpolator(new DecelerateInterpolator()).start();
    }

    /** 달아난다: 빠르게 화면 밖으로 */
    private void dash(View v, float fx, long delay) {
        v.animate().x(xOf(v, fx)).setStartDelay(delay).setDuration(650).setInterpolator(new AccelerateInterpolator()).start();
    }

    /** 제자리에서 통통 뛰기 / 살금살금 (되풀이) */
    private void bob(View v, float dyDp, long period, long delay) {
        ObjectAnimator a = ObjectAnimator.ofFloat(v, "translationY", v.getTranslationY(), v.getTranslationY() - Ui.dp(this, dyDp));
        a.setDuration(period);
        a.setStartDelay(delay);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.start();
        loops.add(a);
    }

    /** 부들부들 (되풀이) */
    private void shake(View v, long delay) {
        ObjectAnimator a = ObjectAnimator.ofFloat(v, "rotation", -7f, 7f);
        a.setDuration(90);
        a.setStartDelay(delay);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.start();
        loops.add(a);
    }

    private void stopLoops() {
        for (Animator a : loops) a.cancel();
        loops.clear();
    }

    /** 말풍선·느낌표를 그 인물 머리 위에 띄운다 */
    private void above(final TextView t, final View who, final float fx, String text, long delay) {
        t.animate().cancel();
        t.setText(text);
        t.setAlpha(0f);
        t.setScaleX(0.3f);
        t.setScaleY(0.3f);
        t.measure(View.MeasureSpec.makeMeasureSpec(W, View.MeasureSpec.AT_MOST), View.MeasureSpec.UNSPECIFIED);
        float x = Math.max(Ui.dp(this, 4), Math.min(W - t.getMeasuredWidth() - Ui.dp(this, 4), W * fx - t.getMeasuredWidth() / 2f));
        t.setX(x);
        t.setY(floorOf(who) - t.getMeasuredHeight() - Ui.dp(this, 6));
        t.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(delay).setDuration(260).setInterpolator(new DecelerateInterpolator()).start();
    }

    // ---------- 장면 ----------

    private void go(int n) {
        if (n > last || W == 0) return;
        scene = n;
        handler.removeCallbacks(auto);
        stopLoops();
        for (View v : new View[] {hero, thief, mom, dog, bubble, bang, card}) { hide(v); v.setTranslationY(0f); }
        for (int i = 0; i < stage.getChildCount(); i++) {       // 별 자리 잡기
            View v = stage.getChildAt(i);
            if (v.getTag() instanceof int[]) {
                int[] s = (int[]) v.getTag();
                v.setX(W * s[0] / 100f);
                v.setY(H * s[1] / 100f * 0.8f);
            }
        }
        String name = (d.hero.isEmpty() ? "꼬마" : d.hero) + " 탐정";
        long hold;
        if (rescue) hold = rescueScene(n, name); else hold = kidnapScene(n, name);
        next.setText(n >= last ? (rescue ? "야호!  쪼코와 돌아가기" : "쪼코 찾으러 가기!") : "다음  ▶");
        Ui.style(next, n >= last ? Ui.BTN_GOOD : Ui.BTN_PRIMARY);
        if (n < last) handler.postDelayed(auto, hold);
    }

    private long kidnapScene(int n, String name) {
        int chapter = d.rescued + 1;
        title.setText("제" + chapter + "장  ·  쪼코 납치 사건");
        switch (n) {
            case 0:
                caption.setText(chapter == 1 ? "어느 조용한 밤, 탐정 사무소…\n" + name + "과 강아지 쪼코가 쉬고 있었다." : "쪼코를 되찾고 평화롭던 밤…\n그런데 또 수상한 그림자가!");
                put(hero, 0.30f);
                put(dog, 0.60f);
                bob(dog, 6, 700, 0);
                return 4200;
            case 1:
                caption.setText("살금살금…\n괴도아빠와 괴도엄마가 나타났다!");
                put(hero, 0.30f);
                put(dog, 0.60f);
                put(thief, 1.25f);
                put(mom, -0.25f);
                walk(thief, 0.88f, 200, 1800);
                walk(mom, 0.08f, 600, 1800);
                bob(thief, 5, 220, 200);
                bob(mom, 5, 220, 600);
                above(bang, dog, 0.60f, "?", 2000);
                return 4600;
            case 2:
                caption.setText("\"쪼코는 우리가 데려간다, 후후후!\"");
                put(hero, 0.30f);
                put(dog, 0.60f);
                put(thief, 0.88f);
                put(mom, 0.08f);
                walk(thief, 0.78f, 0, 500);
                walk(mom, 0.44f, 0, 700);
                shake(dog, 500);
                above(bubble, dog, 0.60f, "멍멍! 멍!", 600);
                dash(thief, 1.5f, 2300);
                dash(dog, 1.4f, 2350);
                dash(mom, 1.3f, 2400);
                bubble.animate().alpha(0f).setStartDelay(2300).setDuration(200).start();
                return 4800;
            case 3:
                caption.setText("괴도아빠가 도전장을 남기고 사라졌다.");
                put(hero, 0.24f);
                above(bang, hero, 0.24f, "!", 300);
                card.setText("쪼코를 찾고 싶으면\n단어 단서 " + d.need() + "개를\n모아 보거라!\n— 괴도아빠");
                card.measure(View.MeasureSpec.makeMeasureSpec(W, View.MeasureSpec.AT_MOST), View.MeasureSpec.UNSPECIFIED);
                card.setX(W * 0.66f - card.getMeasuredWidth() / 2f);
                card.setY(-card.getMeasuredHeight());
                card.setRotation(-8f);
                card.setScaleX(1f);
                card.setScaleY(1f);
                card.setAlpha(1f);
                card.animate().y(H * 0.5f - card.getMeasuredHeight() / 2f).rotation(4f).setStartDelay(700).setDuration(1100).setInterpolator(new BounceInterpolator()).start();
                return 5600;
            default:
                caption.setText(name + ", 출동!\n단서 " + d.need() + "개를 모으면 쪼코를 찾을 수 있다.");
                put(hero, 0.5f);
                bob(hero, 10, 380, 0);
                above(bubble, hero, 0.5f, "쪼코야, 기다려!", 300);
                return 0;
        }
    }

    private long rescueScene(int n, String name) {
        title.setText("쪼코 구출 작전  ·  성공!");
        switch (n) {
            case 0:
                caption.setText("단서를 모두 모았다!\n" + name + "이 괴도의 비밀 아지트를 찾아냈다.");
                put(thief, 0.70f);
                put(mom, 0.90f);
                put(dog, 0.50f);
                put(hero, -0.25f);
                walk(hero, 0.16f, 300, 1500);
                above(bang, thief, 0.70f, "!", 1900);
                return 4400;
            case 1:
                caption.setText("\"멍멍!\"\n쪼코가 달려온다!");
                put(thief, 0.70f);
                put(mom, 0.90f);
                put(hero, 0.16f);
                put(dog, 0.50f);
                shake(thief, 0);
                shake(mom, 100);
                above(bubble, thief, 0.74f, "들켰다!", 100);
                walk(dog, 0.36f, 500, 900);
                bob(dog, 14, 200, 500);
                return 4400;
            default:
                caption.setText("쪼코 구출 성공!  (" + d.rescued + "번째)\n오늘은 쪼코와 함께 있어요.");
                put(hero, 0.38f);
                put(dog, 0.60f);
                put(thief, 0.70f);
                put(mom, 0.90f);
                dash(thief, 1.5f, 200);
                dash(mom, 1.4f, 300);
                bob(hero, 10, 360, 0);
                bob(dog, 12, 260, 100);
                above(bubble, thief, 0.74f, "다음엔 더 꼭꼭 숨겨 주마!", 0);
                bubble.animate().alpha(0f).setStartDelay(1500).setDuration(300).start();
                return 0;
        }
    }
}
