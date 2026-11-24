package kr.word.detective;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;

import java.io.File;

import kr.word.detective.core.Word;

/**
 * 화면 확인용(-PuiTest 빌드에만 들어감): 시험용 단어장 그림을 실제 글자 인식에 넣어 결과를 화면에 적는다.
 *   adb push tools/ocr_test.png /sdcard/Android/data/kr.word.detective/files/test.png
 *   adb shell am start -n kr.word.detective/.OcrTestActivity
 */
public class OcrTestActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        final LinearLayout page = Ui.page(this);
        File f = new File(getExternalFilesDir(null), getIntent().getStringExtra("file") == null ? "test.png" : getIntent().getStringExtra("file"));
        page.addView(Ui.note(this, "file " + f + " exists=" + f.exists()));
        Ocr.read(this, Uri.fromFile(f), (words, why) -> {
            if (words == null) { page.addView(Ui.note(this, "FAILED " + why)); return; }
            page.addView(Ui.note(this, "words " + words.size() + " mode " + kr.word.detective.core.Lines.lastMode));
            for (Word w : words) page.addView(Ui.text(this, w.en + " = " + w.ko, 13, false, Ui.INK));
            for (String l : kr.word.detective.core.Lines.lastLines) page.addView(Ui.text(this, "raw: " + l, 11, false, Ui.SUB));
        });
    }
}
