package kr.word.detective;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import kr.word.detective.core.Lines;
import kr.word.detective.core.Word;

/**
 * 화면 확인용(-PuiTest 빌드에만 들어감): 시험용 단어장 그림을 실제 글자 인식에 넣어 결과를 화면에 적는다.
 *   adb push tools/ocr_test.png /sdcard/Android/data/kr.word.detective/files/test.png
 *   adb shell am start -n kr.word.detective/.OcrTestActivity [--es file 이름] [--es out 결과파일]
 * out 을 주면 결과(단어, 줄, 덩어리 위치)를 파일로도 남긴다.
 */
public class OcrTestActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        final LinearLayout page = Ui.page(this);
        String name = getIntent().getStringExtra("file");
        final String out = getIntent().getStringExtra("out");
        File f = new File(getExternalFilesDir(null), name == null ? "test.png" : name);
        page.addView(Ui.note(this, "file " + f.getName() + " exists=" + f.exists()));
        Ocr.read(this, Uri.fromFile(f), (words, why) -> {
            StringBuilder sb = new StringBuilder();
            if (words == null) sb.append("FAILED ").append(why).append('\n');
            else {
                sb.append("words ").append(words.size()).append(" mode ").append(Lines.lastMode).append('\n');
                for (Word w : words) sb.append(w.en).append(" = ").append(w.ko).append('\n');
                for (String l : Lines.lastLines) sb.append("raw: ").append(l).append('\n');
                for (Lines.Box x : Lines.lastBoxes) sb.append("box: ").append(x.left).append(',').append(x.top).append(',').append(x.right).append(',').append(x.bottom).append(" | ").append(x.text).append('\n');
            }
            if (out != null) {
                try {
                    FileOutputStream o = new FileOutputStream(new File(getExternalFilesDir(null), out));
                    o.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                    o.close();
                } catch (Exception e) {
                    page.addView(Ui.note(this, "write failed " + e));
                }
            }
            for (String l : sb.toString().split("\n")) {
                if (l.startsWith("box: ")) continue;
                page.addView(Ui.text(this, l, l.startsWith("raw: ") ? 11 : 13, false, l.startsWith("raw: ") ? Ui.SUB : Ui.INK));
            }
        });
    }
}
