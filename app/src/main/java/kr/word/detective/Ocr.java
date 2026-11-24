package kr.word.detective;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;

import kr.word.detective.core.Lines;
import kr.word.detective.core.Word;

/** 사진 속 글자 읽기 (구글 ML Kit, 휴대폰 안에서 동작). 읽은 줄을 단어–뜻 짝으로 바꿔 돌려준다. */
final class Ocr {
    private Ocr() {}

    interface Result {
        /** words 가 null 이면 실패 (why 에 까닭) */
        void done(List<Word> words, String why);
    }

    static void read(Context c, Uri photo, final Result cb) {
        try {
            InputImage image = InputImage.fromFilePath(c, photo);   // 사진의 회전 정보도 여기서 맞춰진다
            final TextRecognizer rec = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
            rec.process(image)
                    .addOnSuccessListener(text -> {
                        List<Lines.Box> boxes = new ArrayList<Lines.Box>();
                        for (Text.TextBlock block : text.getTextBlocks()) {
                            for (Text.Line line : block.getLines()) {
                                Rect r = line.getBoundingBox();
                                if (r == null) continue;
                                boxes.add(new Lines.Box(line.getText(), r.left, r.top, r.right, r.bottom));
                            }
                        }
                        rec.close();
                        cb.done(Lines.fromBoxes(boxes), null);
                    })
                    .addOnFailureListener(e -> {
                        rec.close();
                        cb.done(null, "사진에서 글자를 읽지 못했어요.");
                    });
        } catch (Exception e) {
            cb.done(null, "사진을 열지 못했어요.");
        }
    }
}
