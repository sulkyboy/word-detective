package kr.word.detective.core;

import java.util.ArrayList;
import java.util.List;

/** 단어장 하나(= 사건 파일 하나). */
public final class Deck {
    public long id;
    public String name = "";
    public final List<Word> words = new ArrayList<Word>();

    /** 다 외운 단어 수 (복습 단계 3 이상) */
    public int mastered() {
        int n = 0;
        for (Word w : words) if (w.box >= 3) n++;
        return n;
    }
}
