package com.example.karutasuke.entity;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Where a 札 sits on the field. side, dan and column are all counted from the
 * owner of the 陣, so a 定置 can be stored the same way for either side.
 *
 * dan:    1–3, 1 = 下段 (nearest the owner), 3 = 上段 (nearest the centre line)
 * column: 1–8, 1 = the outer edge of that side
 */
public final class Position {

    public static final int DAN_COUNT = 3;
    public static final int COLUMNS_PER_SIDE = 8;   // a 87 cm row fits about 16 札

    private final Jin jin;
    private final Side side;
    private final int dan;
    private final int column;

    public Position(Jin jin, Side side, int dan, int column) {
        if (dan < 1 || dan > DAN_COUNT) throw new IllegalArgumentException("dan must be 1–3, was " + dan);
        if (column < 1 || column > COLUMNS_PER_SIDE) {
            throw new IllegalArgumentException("column must be 1–" + COLUMNS_PER_SIDE + ", was " + column);
        }
        this.jin = jin;
        this.side = side;
        this.dan = dan;
        this.column = column;
    }

    public Jin getJin() { return jin; }
    public Side getSide() { return side; }
    public int getDan() { return dan; }
    public int getColumn() { return column; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position)) return false;
        Position p = (Position) o;
        return jin == p.jin && side == p.side && dan == p.dan && column == p.column;
    }

    @Override
    public int hashCode() { return Objects.hash(jin, side, dan, column); }

    @NonNull
    @Override
    public String toString() { return jin + "/" + side + "/段" + dan + "/列" + column; }
}
