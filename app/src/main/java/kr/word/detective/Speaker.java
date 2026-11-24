package kr.word.detective;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/** 소리로 읽어 주기 (휴대폰에 들어 있는 음성 합성 사용). 영어와 한국어를 이어서 읽을 수 있다. */
final class Speaker {
    private TextToSpeech tts;
    private boolean ready;
    private String waitingEn, waitingKo;   // 준비되기 전에 들어온 요청
    private int seq;

    Speaker(Context c) {
        tts = new TextToSpeech(c.getApplicationContext(), status -> {
            ready = status == TextToSpeech.SUCCESS;
            if (ready && (waitingEn != null || waitingKo != null)) say(waitingEn, waitingKo);
            waitingEn = waitingKo = null;
        });
    }

    /** 영어를 읽고 이어서 한국어를 읽는다. 둘 중 하나는 null 이어도 된다. */
    void say(String english, String korean) {
        if (tts == null) return;
        if (!ready) { waitingEn = english; waitingKo = korean; return; }
        try {
            boolean first = true;
            if (english != null && !english.isEmpty()) {
                tts.setLanguage(Locale.US);
                tts.setSpeechRate(0.85f);
                tts.speak(english, TextToSpeech.QUEUE_FLUSH, null, "wd" + (seq++));
                first = false;
            }
            if (korean != null && !korean.isEmpty()) {
                // 언어를 바꾸는 것은 다음 말부터 적용되므로, 잠깐 쉬었다가 한국어로 읽는다
                if (!first) tts.playSilentUtterance(350, TextToSpeech.QUEUE_ADD, "wd" + (seq++));
                final String ko = korean;
                if (first) {
                    tts.setLanguage(Locale.KOREAN);
                    tts.setSpeechRate(1.0f);
                    tts.speak(ko, TextToSpeech.QUEUE_FLUSH, null, "wd" + (seq++));
                } else {
                    tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                        boolean done;
                        @Override public void onStart(String id) {}
                        @Override public void onError(String id) {}
                        @Override public void onDone(String id) {
                            if (done || tts == null) return;
                            done = true;
                            tts.setOnUtteranceProgressListener(null);
                            tts.setLanguage(Locale.KOREAN);
                            tts.setSpeechRate(1.0f);
                            tts.speak(ko, TextToSpeech.QUEUE_ADD, null, "wd" + (seq++));
                        }
                    });
                }
            }
        } catch (Exception e) {
            // 음성 합성이 없는 기기: 소리 없이 진행
        }
    }

    void stop() {
        try { if (tts != null) tts.stop(); } catch (Exception e) { /* 무시 */ }
    }

    void close() {
        try { if (tts != null) { tts.stop(); tts.shutdown(); } } catch (Exception e) { /* 무시 */ }
        tts = null;
    }
}
