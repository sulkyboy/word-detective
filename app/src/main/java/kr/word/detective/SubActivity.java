package kr.word.detective;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

/** 설정의 하위 화면: 위쪽 제목 줄의 ← 를 누르면 닫고 메뉴로 돌아간다. */
abstract class SubActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.backArrow(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
