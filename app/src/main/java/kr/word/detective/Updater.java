package kr.word.detective;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import kr.word.detective.core.Json;

/**
 * 앱 안에서 새 버전을 확인·설치한다.
 * 빌드할 때 GitHub 저장소의 apk 브랜치에 version.json 과 설치 파일을 올려두고, 여기서 내려받는다.
 */
final class Updater {
    private Updater() {}

    static final String BASE = "https://raw.githubusercontent.com/sulkyboy/word-detective/apk/";
    static final String VERSION_URL = BASE + "version.json";
    static final String APK_URL = BASE + "word-detective.apk";

    static final class Remote {
        final long code;
        final String name;
        Remote(long code, String name) { this.code = code; this.name = name; }
    }

    static long currentCode(Context c) {
        try {
            PackageInfo p = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return Build.VERSION.SDK_INT >= 28 ? p.getLongVersionCode() : p.versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    static String currentName(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    /** 최신 버전 정보. 확인할 수 없으면 null. 결과는 첫 화면 표시용으로 저장한다. */
    static Remote check(Context ctx) {
        String body = fetch(VERSION_URL + "?t=" + (System.currentTimeMillis() / 60000));
        Object root = Json.parse(body);
        if (!(root instanceof Map)) return null;
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) root;
        Integer code = Json.integer(m, "versionCode");
        if (code == null) return null;
        Remote r = new Remote(code, Json.str(m, "versionName"));
        Store.saveRemote(ctx, r.code, r.name);
        return r;
    }

    static boolean hasUpdate(Context ctx) {
        return Store.remoteCode(ctx) > currentCode(ctx);
    }

    /** 설치 권한 확인. 없으면 허용 화면을 열고 false. */
    static boolean ensureInstallPermission(Activity a) {
        if (Build.VERSION.SDK_INT >= 26 && !a.getPackageManager().canRequestPackageInstalls()) {
            Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + a.getPackageName()));
            a.startActivity(i);
            return false;
        }
        return true;
    }

    /** 내려받아 설치 요청. 성공하면 null, 실패하면 이유. 화면 밖 스레드에서 부른다. */
    static String downloadAndInstall(Context ctx) {
        File f = new File(ctx.getCacheDir(), "update.apk");
        if (!download(APK_URL + "?t=" + System.currentTimeMillis(), f)) return "설치 파일을 내려받지 못했습니다.";
        try {
            PackageInstaller pi = ctx.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params =
                    new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            params.setAppPackageName(ctx.getPackageName());
            int sid = pi.createSession(params);
            PackageInstaller.Session s = pi.openSession(sid);
            OutputStream out = s.openWrite("update.apk", 0, f.length());
            InputStream in = new FileInputStream(f);
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            s.fsync(out);
            out.close();
            int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
            PendingIntent cb = PendingIntent.getBroadcast(ctx, sid, new Intent(ctx, InstallReceiver.class), flags);
            s.commit(cb.getIntentSender());
            s.close();
            return null;
        } catch (Exception e) {
            return "설치를 시작하지 못했습니다: " + e.getMessage();
        }
    }

    static boolean download(String url, File out) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(10000);
            c.setReadTimeout(20000);
            c.setInstanceFollowRedirects(true);
            if (c.getResponseCode() != 200) return false;
            InputStream in = c.getInputStream();
            OutputStream o = new FileOutputStream(out);
            byte[] buf = new byte[65536];
            int n;
            long total = 0;
            while ((n = in.read(buf)) > 0) { o.write(buf, 0, n); total += n; }
            o.close();
            in.close();
            return total > 10000; // 오류 페이지가 아닌 실제 설치 파일인지 대략 확인
        } catch (Exception e) {
            return false;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    /** 짧은 글(버전 정보)을 받아 온다. 실패하면 null */
    static String fetch(String url) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            if (c.getResponseCode() != 200) return null;
            InputStream in = c.getInputStream();
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            in.close();
            return new String(bo.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }
}
