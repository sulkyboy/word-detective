package kr.word.detective;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.List;

/** 말로 한 답 듣기 (휴대폰의 음성 인식 사용). 결과는 후보 여러 개로 온다. */
final class Listener {
    interface Result {
        /** heard 가 비어 있으면 못 알아들은 것. why 는 그 까닭(없으면 null) */
        void done(List<String> heard, String why);
    }

    static final int REQ_MIC = 71;

    private final Activity act;
    private SpeechRecognizer rec;
    private boolean busy;

    Listener(Activity a) { act = a; }

    boolean available() { return SpeechRecognizer.isRecognitionAvailable(act); }

    /** 마이크 권한이 있는지. 없으면 권한을 묻고 false */
    boolean ensurePermission() {
        if (act.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) return true;
        act.requestPermissions(new String[] {Manifest.permission.RECORD_AUDIO}, REQ_MIC);
        return false;
    }

    /** @param english true 면 영어로, false 면 한국어로 듣는다 */
    void listen(boolean english, final Result cb) {
        if (busy) return;
        if (!available()) { cb.done(new ArrayList<String>(), "이 휴대폰에서는 음성 인식을 쓸 수 없어요."); return; }
        try {
            if (rec == null) rec = SpeechRecognizer.createSpeechRecognizer(act);
            busy = true;
            rec.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle b) {}
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float v) {}
                @Override public void onBufferReceived(byte[] bytes) {}
                @Override public void onEndOfSpeech() {}
                @Override public void onPartialResults(Bundle b) {}
                @Override public void onEvent(int i, Bundle b) {}
                @Override public void onError(int code) {
                    if (!busy) return;
                    busy = false;
                    String why = null;
                    if (code == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) why = "마이크 사용을 허용해 주세요.";
                    else if (code == SpeechRecognizer.ERROR_NETWORK || code == SpeechRecognizer.ERROR_NETWORK_TIMEOUT || code == SpeechRecognizer.ERROR_SERVER)
                        why = "음성 인식에 연결하지 못했어요. 인터넷을 확인해 주세요.";
                    cb.done(new ArrayList<String>(), why);
                }
                @Override public void onResults(Bundle b) {
                    if (!busy) return;
                    busy = false;
                    ArrayList<String> list = b == null ? null : b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    cb.done(list == null ? new ArrayList<String>() : list, null);
                }
            });
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, english ? "en-US" : "ko-KR");
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, english ? "en-US" : "ko-KR");
            i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 6);
            i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, act.getPackageName());
            rec.startListening(i);
        } catch (Exception e) {
            busy = false;
            cb.done(new ArrayList<String>(), "음성 인식을 시작하지 못했어요.");
        }
    }

    void cancel() {
        busy = false;
        try { if (rec != null) rec.cancel(); } catch (Exception e) { /* 무시 */ }
    }

    void close() {
        busy = false;
        try { if (rec != null) rec.destroy(); } catch (Exception e) { /* 무시 */ }
        rec = null;
    }
}
