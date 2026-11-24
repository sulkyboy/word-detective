package kr.word.detective.core;

/** 단어 하나와 그 단어의 공부 기록. */
public final class Word {
    public String en = "";      // 영어 단어
    public String ko = "";      // 한국어 뜻 (여러 개면 쉼표로)
    public boolean seen;        // 단서 카드로 한 번 본 적이 있는지
    public int box;             // 복습 단계 0~5 (높을수록 잘 아는 단어)
    public long due;            // 다시 물어볼 날 (날짜 번호)
    public int right, wrong;    // 맞힌 횟수, 틀린 횟수

    public Word() {}
    public Word(String en, String ko) { this.en = en; this.ko = ko; }
}
