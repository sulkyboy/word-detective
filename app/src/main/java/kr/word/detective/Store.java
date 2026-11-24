package kr.word.detective;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import kr.word.detective.core.Data;
import kr.word.detective.core.Word;

/** 저장: 앱 안의 파일 하나(data.json)에 모든 것을 둔다. 화면들은 같은 Data 를 함께 쓴다. */
final class Store {
    private Store() {}

    private static Data data;
    /** 사진에서 읽었거나 붙여넣은 단어들을 '단어 고치기' 화면으로 넘길 때 잠깐 담아 둔다 */
    static List<Word> pending = new ArrayList<Word>();

    static synchronized Data get(Context c) {
        if (data == null) data = Data.fromJson(read(new File(c.getFilesDir(), "data.json")));
        return data;
    }

    static synchronized void save(Context c) {
        if (data == null) return;
        try {
            File tmp = new File(c.getFilesDir(), "data.json.tmp");
            FileOutputStream out = new FileOutputStream(tmp);
            out.write(data.toJson().getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
            out.close();
            // 쓰다가 꺼져도 예전 파일이 남도록, 다 쓴 다음에 이름을 바꾼다
            if (!tmp.renameTo(new File(c.getFilesDir(), "data.json"))) tmp.delete();
        } catch (Exception e) {
            // 저장 실패: 다음 저장 때 다시 시도된다
        }
    }

    private static String read(File f) {
        if (!f.exists()) return "";
        try {
            InputStream in = new FileInputStream(f);
            byte[] buf = new byte[(int) f.length()];
            int off = 0, n;
            while (off < buf.length && (n = in.read(buf, off, buf.length - off)) > 0) off += n;
            in.close();
            return new String(buf, 0, off, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    /** 오늘의 날짜 번호 (휴대폰 시간대 기준, 자정에 바뀐다) */
    static long today() {
        long now = System.currentTimeMillis();
        return (now + TimeZone.getDefault().getOffset(now)) / 86400000L;
    }

    /** 앱 안 업데이트 확인 결과 (Updater 가 쓴다) */
    static void saveRemote(Context c, long code, String name) {
        c.getSharedPreferences("wd", Context.MODE_PRIVATE).edit().putLong("remoteCode", code).putString("remoteName", name).apply();
    }
    static long remoteCode(Context c) { return c.getSharedPreferences("wd", Context.MODE_PRIVATE).getLong("remoteCode", 0); }
    static String remoteName(Context c) { return c.getSharedPreferences("wd", Context.MODE_PRIVATE).getString("remoteName", ""); }
}
