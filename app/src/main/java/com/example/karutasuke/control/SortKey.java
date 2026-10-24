package com.example.karutasuke.control;

/** What FudaCatalog.sortBy() can sort on. Order here matches the sort menu on FudaPage. */
public enum SortKey {
    NUMBER,     // 歌番号
    KIMARIJI,   // 決まり字
    KAMINOKU,   // 上の句 (by hiragana)
    SHIMONOKU   // 下の句 (by hiragana)
}
