package kr.word.detective;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import kr.word.detective.core.Data;
import kr.word.detective.core.Deck;
import kr.word.detective.core.Lines;
import kr.word.detective.core.Word;

/** 사건 파일: 단어장 목록. 사진을 찍거나 골라서, 또는 글을 붙여넣어 새 단어장을 만든다. */
public class DecksActivity extends SubActivity {
    private static final int REQ_CAMERA = 11, REQ_GALLERY = 12;
    private Uri photo;          // 카메라가 사진을 써넣을 자리
    private boolean reading;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (b != null) photo = b.getParcelable("photo");
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putParcelable("photo", photo);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        Data d = Store.get(this);
        LinearLayout page = Ui.page(this);

        if (reading) {
            Ui.say(page, R.drawable.ch_jjoko, "쪼코", "킁킁… 사진에서 단어를 찾고 있어요. 잠깐만요!", Ui.ACCENT);
            return;
        }

        LinearLayout make = Ui.card(page);
        make.addView(Ui.heading(this, "새 단어장 만들기"));
        make.addView(Ui.note(this, "학교나 학원에서 받은 단어장을 찍으면 영어 단어와 뜻을 읽어 옵니다. 인쇄된 글자가 잘 읽히고, 손글씨는 잘 안 읽힐 수 있어요. 읽은 뒤에 고칠 수 있습니다."));
        Ui.button(make, "단어장 사진 찍기", Ui.BTN_PRIMARY, this::camera);
        Ui.button(make, "사진첩에서 고르기", Ui.BTN_PLAIN, this::gallery);
        Ui.button(make, "직접 입력하거나 붙여넣기", Ui.BTN_PLAIN, this::typeIn);

        if (d.decks.isEmpty()) {
            Ui.say(page, R.drawable.ch_jjoko, "쪼코", "아직 사건 파일이 없어요. 단어장을 찍어서 첫 사건을 시작해요!", Ui.ACCENT);
            return;
        }
        LinearLayout list = Ui.card(page);
        list.addView(Ui.heading(this, "내 단어장"));
        for (int i = d.decks.size() - 1; i >= 0; i--) {
            final Deck deck = d.decks.get(i);
            LinearLayout row = Ui.column(this);
            row.setBackground(Ui.pressable(this, 0xFFF7F8FA, Ui.CHIP_DOWN, 10));
            int p = Ui.dp(this, 12);
            row.setPadding(p, p, p, p);
            row.addView(Ui.text(this, deck.name, 16, true, Ui.INK));
            int n = deck.words.size();
            row.addView(Ui.note(this, "단어 " + n + "개  ·  다 외운 단어 " + deck.mastered() + "개"));
            Ui.bar(row, n == 0 ? 0 : 100 * deck.mastered() / n, Ui.GOOD);
            Ui.click(row, () -> startActivity(new Intent(this, DeckActivity.class).putExtra("deck", deck.id)));
            list.addView(row, Ui.below(this, 8));
        }
    }

    // ---------- 사진 ----------

    private void camera() {
        try {
            ContentValues v = new ContentValues();
            v.put(MediaStore.Images.Media.DISPLAY_NAME, "wordlist_" + System.currentTimeMillis() + ".jpg");
            v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            v.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/WordDetective");
            photo = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, photo);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(i, REQ_CAMERA);
        } catch (Exception e) {
            dropPhoto();
            Toast.makeText(this, "카메라를 열지 못했어요. '사진첩에서 고르기'를 써 주세요.", Toast.LENGTH_LONG).show();
        }
    }

    private void gallery() {
        try {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(i, REQ_GALLERY);
        } catch (Exception e) {
            Toast.makeText(this, "사진첩을 열지 못했어요.", Toast.LENGTH_LONG).show();
        }
    }

    /** 찍다가 그만둔 경우 빈 사진 자리를 지운다 */
    private void dropPhoto() {
        try { if (photo != null) getContentResolver().delete(photo, null, null); } catch (Exception e) { /* 무시 */ }
        photo = null;
    }

    @Override
    protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        Uri uri = null;
        if (req == REQ_CAMERA) {
            if (result == RESULT_OK) uri = photo; else dropPhoto();
        } else if (req == REQ_GALLERY && result == RESULT_OK && data != null) {
            uri = data.getData();
        }
        if (uri == null) return;
        reading = true;
        render();
        Ocr.read(this, uri, (words, why) -> {
            reading = false;
            if (isFinishing()) return;
            if (words == null || words.isEmpty()) {
                render();
                Toast.makeText(this, words == null ? why : "사진에서 단어를 찾지 못했어요. 글자가 크게 나오도록 다시 찍어 주세요.", Toast.LENGTH_LONG).show();
                return;
            }
            review(words);
        });
    }

    // ---------- 직접 입력 ----------

    private void typeIn() {
        final EditText e = new EditText(this);
        e.setHint("apple 사과\nrun 달리다\n…");
        e.setMinLines(6);
        e.setGravity(Gravity.TOP);
        e.setTextSize(15);
        LinearLayout box = Ui.column(this);
        int p = Ui.dp(this, 20);
        box.setPadding(p, Ui.dp(this, 8), p, 0);
        box.addView(Ui.note(this, "한 줄에 '영어 단어  한국어 뜻'을 적어 주세요. 다른 곳에서 복사한 단어 목록을 붙여넣어도 됩니다. 비워 두고 [다음]을 누르면 빈 단어장에서 하나씩 넣을 수 있어요."));
        box.addView(e, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        new AlertDialog.Builder(this)
                .setTitle("단어 입력")
                .setView(box)
                .setNegativeButton("취소", null)
                .setPositiveButton("다음", (dlg, w) -> review(Lines.fromText(e.getText().toString())))
                .show();
    }

    /** 읽은 단어들을 확인·고치기 화면으로 넘긴다 */
    private void review(List<Word> words) {
        Store.pending = words;
        render();
        startActivity(new Intent(this, EditActivity.class));
    }
}
