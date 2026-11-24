package kr.word.detective;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import kr.word.detective.core.Data;
import kr.word.detective.core.Deck;
import kr.word.detective.core.Judge;
import kr.word.detective.core.Mission;
import kr.word.detective.core.Quiz;
import kr.word.detective.core.Rank;
import kr.word.detective.core.Srs;
import kr.word.detective.core.Word;

/**
 * 괴도의 도전장: 단어 공부 한 판(= 사건 하나).
 * 괴도아빠가 세 가지로 섞어서 묻는다 — 영어를 들려주고 뜻 묻기, 뜻을 말해 주고 영어 묻기, 스펠링 묻기.
 * 답은 말로 하거나, 글자로 쓰거나, 보기에서 고른다. 처음 보는 단어는 쪼꼬가 단서 카드로 먼저 보여 준다.
 */
public class StudyActivity extends SubActivity {
    private Data d;
    private long today;
    private List<Quiz.Q> qs = new ArrayList<Quiz.Q>();
    private final Set<Quiz.Q> retries = new HashSet<Quiz.Q>();   // 틀려서 끝에 한 번 더 나오는 문제
    private int idx, asked, right, earned, startLevel;

    // 지금 문제의 상태
    private int spokenIdx = -1, timedIdx = -1;
    private boolean answered, lastRight, hinted, showChoices, listening, timedOut;
    private boolean meetShown;    // 이번 판의 면회 장면을 이미 띄웠는지
    private int voiceTries, lastGain, pendingVoice;
    private String note, heardText, draft = "";
    private EditText typed;
    private TextView timerText;

    private Speaker speaker;
    private Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int timeLeft;
    private final Runnable tick = new Runnable() {
        public void run() {
            if (answered || isFinishing()) return;
            if (!listening) timeLeft--;      // 듣는 동안에는 시간을 멈춘다
            if (timeLeft <= 0) { timedOut = true; answer(false, false); return; }
            if (timerText != null) timerText.setText("남은 시간 " + timeLeft + "초");
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        d = Store.get(this);
        today = Store.today();
        speaker = new Speaker(this);
        listener = new Listener(this);
        startLevel = Rank.level(d.points);
        boolean unsolved = getIntent().getBooleanExtra("unsolved", false);
        Deck deck = d.deck(getIntent().getLongExtra("deck", 0));
        if (unsolved) setTitle("미해결 사건");
        else if (deck != null) setTitle(deck.name);
        qs = Quiz.build(deck != null ? deck.words : d.allWords(), d.allWords(), today, d.perSession, unsolved, new Random());
        render();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(tick);
        speaker.close();
        listener.close();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (listening) { listener.cancel(); listening = false; }
        speaker.stop();
    }

    private String hero() { return (d.hero.isEmpty() ? "꼬마" : d.hero) + " 탐정"; }

    // ---------- 화면 ----------

    private void render() {
        if (typed != null) draft = typed.getText().toString();
        typed = null;
        timerText = null;
        LinearLayout page = Ui.page(this);

        if (qs.isEmpty()) {
            Ui.say(page, R.drawable.ch_jjoko, "쪼꼬", "지금은 풀 단어가 없어요. 사건 파일에서 단어장을 만들거나 뜻을 채워 주세요!", Ui.ACCENT);
            Ui.button(page, "돌아가기", Ui.BTN_PLAIN, this::finish);
            return;
        }
        if (idx >= qs.size()) { renderEnd(page); return; }

        final Quiz.Q q = qs.get(idx);
        TextView step = Ui.text(this, (idx + 1) + " / " + qs.size() + "   ·   이번 사건에서 찾은 단서 " + earned + "개", 13, true, Ui.SUB);
        page.addView(step);
        Ui.bar(page, 100 * idx / qs.size(), Ui.ACCENT);

        if (q.type == Quiz.INTRO) { renderIntro(page, q); return; }

        final Word w = q.word;
        String ask;
        if (q.type == Quiz.EN_KO) ask = "이 영어 단어의 뜻을 맞혀 보거라!";
        else if (q.type == Quiz.KO_EN) ask = "'" + w.ko + "' — 영어로는 무엇이라 하지?";
        else ask = "이 단어의 스펠링을 한 글자씩 대 보거라!";
        if (retries.contains(q) && !answered) ask = "아까 틀린 문제다. 이번엔 맞힐 수 있겠나?  " + ask;
        Ui.say(page, R.drawable.ch_thief, "괴도아빠", ask, Ui.DANGER);

        LinearLayout card = Ui.card(page);
        if (q.type == Quiz.EN_KO) {
            card.addView(Ui.big(this, w.en, 34, Ui.INK));
        } else {
            card.addView(Ui.big(this, w.ko, 26, Ui.INK));
            if (q.type == Quiz.SPELL) card.addView(center(Ui.note(this, d.sound ? "단어를 듣고 스펠링을 써 보세요" : "이 뜻의 영어 단어를 스펠링으로 써 보세요")));
        }
        if (d.sound) {
            LinearLayout r = Ui.line(this);
            r.setGravity(Gravity.CENTER);
            r.addView(Ui.chip(this, "🔊 다시 듣기", false, () -> speakQuestion(q)));
            card.addView(r, Ui.below(this, 10));
        }
        if (spokenIdx != idx) { spokenIdx = idx; speakQuestion(q); }

        if (answered) { renderFeedback(page, q); return; }

        if (d.timer) {
            if (timedIdx != idx) {
                timedIdx = idx;
                timeLeft = q.type == Quiz.SPELL ? 30 : 20;
                handler.removeCallbacks(tick);
                handler.postDelayed(tick, 1000);
            }
            timerText = Ui.text(this, "남은 시간 " + timeLeft + "초", 13, true, Ui.DANGER);
            timerText.setGravity(Gravity.CENTER);
            page.addView(timerText, Ui.below(this, 8));
        }

        if (note != null) {
            TextView n = Ui.text(this, note, 14, true, Ui.ACCENT);
            n.setGravity(Gravity.CENTER);
            page.addView(n, Ui.below(this, 10));
        }

        if (listening) {
            Ui.say(page, R.drawable.ch_jjoko, "쪼꼬가 몰래 보낸 쪽지", "듣고 있어요. 또박또박 말해 주세요!", Ui.ACCENT);
            Ui.button(page, "그만 듣기", Ui.BTN_PLAIN, () -> { listener.cancel(); listening = false; render(); });
            return;
        }

        if (q.type == Quiz.EN_KO) {
            Ui.button(page, "🎤 말로 답하기", Ui.BTN_PRIMARY, () -> voice(false));
        } else {
            typed = Ui.input(this, q.type == Quiz.SPELL ? "스펠링을 써 주세요" : "영어 단어를 써 주세요", true);
            typed.setText(draft);
            typed.setSelection(draft.length());
            typed.setImeOptions(EditorInfo.IME_ACTION_DONE);
            typed.setOnEditorActionListener((v, action, ev) -> { check(q); return true; });
            LinearLayout r = Ui.line(this);
            r.addView(typed, Ui.fill());
            r.addView(Ui.chip(this, "확인", false, () -> check(q)), Ui.after(this, 8));
            page.addView(r, Ui.below(this, 12));
            Ui.button(page, q.type == Quiz.SPELL ? "🎤 한 글자씩 말하기" : "🎤 영어로 말하기", Ui.BTN_PRIMARY, () -> voice(true));
        }

        if (q.type != Quiz.SPELL && q.choices.size() >= 2) {
            if (!showChoices) {
                Ui.button(page, "보기에서 고르기 (단서가 절반)", Ui.BTN_PLAIN, () -> { showChoices = true; render(); });
            } else {
                final String want = q.type == Quiz.EN_KO ? w.ko : w.en;
                for (final String c : q.choices) Ui.button(page, c, Ui.BTN_PLAIN, () -> answer(c.equals(want), true));
            }
        }

        LinearLayout tools = Ui.line(this);
        tools.setGravity(Gravity.CENTER);
        tools.addView(Ui.chip(this, hinted ? "힌트: " + Quiz.hint(q) : "쪼꼬의 비밀 쪽지 (힌트)", false, () -> { hinted = true; render(); }));
        tools.addView(Ui.chip(this, "모르겠어요", false, () -> answer(false, false)), Ui.after(this, 8));
        page.addView(tools, Ui.below(this, 14));
    }

    private TextView center(TextView t) { t.setGravity(Gravity.CENTER); return t; }

    /** 처음 보는 단어: 쪼꼬가 물어 온 단서 카드 */
    private void renderIntro(LinearLayout page, final Quiz.Q q) {
        final Word w = q.word;
        Ui.say(page, R.drawable.ch_jjoko, "쪼꼬가 몰래 보낸 쪽지", "멍! 새 단서 카드예요. 괴도아빠가 곧 물어볼 테니 잘 봐 두세요!", Ui.ACCENT);
        LinearLayout card = Ui.card(page);
        card.addView(Ui.big(this, w.en, 36, Ui.INK));
        card.addView(Ui.big(this, w.ko, 22, Ui.ACCENT), Ui.below(this, 8));
        if (d.sound) {
            LinearLayout r = Ui.line(this);
            r.setGravity(Gravity.CENTER);
            r.addView(Ui.chip(this, "🔊 다시 듣기", false, () -> speaker.say(w.en, w.ko)));
            card.addView(r, Ui.below(this, 12));
            if (spokenIdx != idx) { spokenIdx = idx; speaker.say(w.en, w.ko); }
        }
        Ui.button(page, "기억했어요!", Ui.BTN_PRIMARY, () -> {
            w.seen = true;
            Store.save(this);
            next();
        });
    }

    private void renderFeedback(LinearLayout page, Quiz.Q q) {
        Word w = q.word;
        if (lastRight) {
            Ui.say(page, R.drawable.ch_detective, hero(), "정답! 단서 " + lastGain + "개를 찾았다!", Ui.GOOD);
        } else {
            String said = heardText != null ? "(\"" + heardText + "\" 라고 들렸어요)  " : timedOut ? "시간이 다 됐어요!  " : "";
            Ui.say(page, R.drawable.ch_helper, "괴도엄마", said + "호호, 아쉽네요~ 정답은 이거랍니다. 조금 뒤에 한 번 더 나올 거예요.", Ui.DANGER);
        }
        LinearLayout ans = Ui.card(page);
        ans.addView(Ui.big(this, w.en, 30, lastRight ? Ui.GOOD : Ui.DANGER));
        ans.addView(Ui.big(this, w.ko, 20, Ui.INK), Ui.below(this, 6));
        Ui.button(page, idx + 1 >= qs.size() ? "사건 마무리하기" : "다음 문제", lastRight ? Ui.BTN_GOOD : Ui.BTN_PRIMARY, this::next);
    }

    private void renderEnd(LinearLayout page) {
        setTitle("사건 종료");
        int pct = asked == 0 ? 0 : 100 * right / asked;
        page.addView(Ui.big(this, pct >= 80 ? "사건 해결!" : "사건 종료", 28, pct >= 80 ? Ui.GOOD : Ui.INK));
        String line;
        if (pct == 100) line = "크윽… 하나도 못 틀리게 하다니! 다음엔 더 어려운 문제를 가져오마!";
        else if (pct >= 80) line = "제법이군, " + hero() + ". 이번에는 내가 물러나 주지!";
        else if (pct >= 50) line = "하하하! 아슬아슬했다. 틀린 단어는 '미해결 사건'으로 남겨 두마!";
        else line = "하하하! 아직 멀었구나. 미해결 사건부터 풀고 다시 도전해라!";
        Ui.say(page, R.drawable.ch_thief, "괴도아빠", line, Ui.DANGER);

        LinearLayout card = Ui.card(page);
        LinearLayout row = Ui.line(this);
        row.addView(Ui.image(this, R.drawable.ch_detective, 96));
        LinearLayout col = Ui.column(this);
        col.addView(Ui.text(this, "문제 " + asked + "개 중 " + right + "개 정답", 17, true, Ui.INK));
        col.addView(Ui.text(this, "찾은 단서 +" + earned + "개  (모두 " + d.points + "개)", 14, true, 0xFFB57F00));
        col.addView(Ui.text(this, "연속 출석 " + d.liveStreak(today) + "일", 14, false, Ui.SUB));
        col.addView(Ui.text(this, Mission.done(d) ? "괴도의 요구를 해냈다! 쪼꼬를 풀어 달라고 하자" : "괴도의 요구: " + Mission.task(d.stage), 14, true, Ui.DANGER));
        LinearLayout.LayoutParams lp = Ui.fill();
        lp.leftMargin = Ui.dp(this, 10);
        row.addView(col, lp);
        card.addView(row);
        if (Rank.level(d.points) > startLevel) {
            card.addView(Ui.big(this, "등급이 올랐어요!  " + Rank.name(d.points), 18, Ui.ACCENT), Ui.below(this, 10));
        } else {
            Ui.bar(card, Rank.progress(d.points), Ui.GOLD);
            if (Rank.toNext(d.points) > 0) card.addView(Ui.note(this, "'" + Rank.nextName(d.points) + "'까지 단서 " + Rank.toNext(d.points) + "개"));
        }
        // 80% 이상 맞히면 쪼꼬를 잠깐 만난다
        boolean canMeet = Mission.canMeet(asked, right);
        card.addView(Ui.text(this, canMeet
                ? Mission.MEET_PERCENT + "% 이상 맞혀서 쪼꼬를 잠깐 만났어요!  (지금까지 " + d.meets + "번)"
                : "한 판에서 " + Mission.MEET_PERCENT + "% 이상 맞히면 쪼꼬를 잠깐 만날 수 있어요. (이번엔 " + pct + "%)", 14, true, canMeet ? Ui.GOOD : Ui.ACCENT), Ui.below(this, 10));
        if (canMeet) Ui.button(page, "쪼꼬 한 번 더 보기", Ui.BTN_GOOD, () -> startActivity(new android.content.Intent(this, IntroActivity.class).putExtra("meet", true).putExtra("again", true)));
        Ui.button(page, "한 판 더!", Ui.BTN_PRIMARY, this::recreate);
        Ui.button(page, "사무소로 돌아가기", Ui.BTN_PLAIN, this::finish);
        if (canMeet && !meetShown) {
            meetShown = true;
            startActivity(new android.content.Intent(this, IntroActivity.class).putExtra("meet", true));
        }
    }

    // ---------- 진행 ----------

    private void speakQuestion(Quiz.Q q) {
        if (!d.sound) return;
        if (q.type == Quiz.KO_EN) speaker.say(null, q.word.ko);
        else speaker.say(q.word.en, null);
    }

    private void next() {
        speaker.stop();
        handler.removeCallbacks(tick);
        idx++;
        answered = lastRight = hinted = showChoices = listening = timedOut = false;
        voiceTries = 0;
        note = heardText = null;
        draft = "";
        if (idx >= qs.size() && asked > 0) {
            d.solved++;
            Store.save(this);
        }
        render();
    }

    /** 글자로 쓴 답 확인 */
    private void check(Quiz.Q q) {
        String text = typed == null ? draft : typed.getText().toString();
        if (text.trim().isEmpty()) return;
        List<String> one = Arrays.asList(text);
        answer(q.type == Quiz.SPELL ? Judge.spelling(q.word.en, one) : Judge.english(q.word.en, one), false);
    }

    /** 말로 답하기. english: 영어로 들을지(뜻→영어, 스펠링) 한국어로 들을지(영어→뜻) */
    private void voice(boolean english) {
        if (!listener.ensurePermission()) { pendingVoice = english ? 1 : 2; return; }
        speaker.stop();
        listening = true;
        note = null;
        final int at = idx;
        render();
        listener.listen(english, (heard, why) -> {
            if (isFinishing() || at != idx || answered) return;
            listening = false;
            if (heard.isEmpty()) {
                note = why != null ? why : "잘 못 들었어요. 다시 한 번 말해 볼까요?";
                render();
                return;
            }
            Quiz.Q q = qs.get(idx);
            boolean ok = q.type == Quiz.EN_KO ? Judge.meaning(q.word.ko, heard)
                    : q.type == Quiz.KO_EN ? Judge.english(q.word.en, heard)
                    : Judge.spelling(q.word.en, heard);
            if (ok) { answer(true, false); return; }
            // 음성 인식이 잘못 들었을 수 있으니 한 번은 다시 말할 기회를 준다
            if (voiceTries == 0) {
                voiceTries = 1;
                note = "\"" + heard.get(0) + "\" 라고 들렸어요. 한 번 더 말해 볼까요?";
                render();
            } else {
                heardText = heard.get(0);
                answer(false, false);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req != Listener.REQ_MIC) return;
        int want = pendingVoice;
        pendingVoice = 0;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            if (want != 0 && !answered) voice(want == 1);
        } else {
            note = "마이크를 허용하지 않으면 말로 답할 수 없어요. 글자로 쓰거나 보기에서 골라 주세요.";
            render();
        }
    }

    private void answer(boolean ok, boolean byChoice) {
        if (answered || idx >= qs.size()) return;
        Quiz.Q q = qs.get(idx);
        answered = true;
        lastRight = ok;
        handler.removeCallbacks(tick);
        if (listening) { listener.cancel(); listening = false; }
        boolean retry = retries.contains(q);
        if (!retry) {
            asked++;
            if (ok) { right++; Srs.right(q.word, today); }
            else {
                Srs.wrong(q.word, today);
                retries.add(q);
                qs.add(q);              // 끝에 한 번 더 낸다
            }
            d.record(today, 1, ok ? 1 : 0);
        }
        lastGain = !ok ? 0 : retry ? 3 : Quiz.reward(q.type, byChoice, hinted);
        earned += lastGain;
        d.points += lastGain;
        q.word.seen = true;
        Store.save(this);
        if (d.sound) speaker.say(q.word.en, q.word.ko);
        render();
    }
}
