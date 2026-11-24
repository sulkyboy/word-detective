package kr.word.detective;

import android.app.Application;
import android.content.Context;

import java.io.PrintWriter;
import java.io.StringWriter;

/** 앱이 갑자기 멈추면 까닭을 적어 둔다. 다음에 켰을 때 첫 화면에 보여서, 그 화면을 찍어 보내면 고칠 수 있다. */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        final Thread.UncaughtExceptionHandler before = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try {
                StringWriter w = new StringWriter();
                error.printStackTrace(new PrintWriter(w));
                String text = w.toString();
                if (text.length() > 3000) text = text.substring(0, 3000);
                getSharedPreferences("wd", Context.MODE_PRIVATE).edit()
                        .putString("crash", "버전 " + Updater.currentName(this) + "\n" + text).commit();
            } catch (Throwable ignored) {
                // 적지 못해도 원래 하던 대로 멈춘다
            }
            if (before != null) before.uncaughtException(thread, error);
        });
    }

    static String lastCrash(Context c) { return c.getSharedPreferences("wd", Context.MODE_PRIVATE).getString("crash", ""); }
    static void clearCrash(Context c) { c.getSharedPreferences("wd", Context.MODE_PRIVATE).edit().remove("crash").apply(); }
}
