package com.example.karutasuke.entity;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The 札 on the field and where each one is. No two 札 share a position. */
public final class Layout {

    public static final int MAX_PER_JIN = 25;

    private final Map<Fuda, Position> positions = new LinkedHashMap<>();
    private final Map<Position, Fuda> occupied = new HashMap<>();

    /** @throws IllegalArgumentException if another 札 is already at that position */
    public void place(Fuda fuda, Position position) {
        Fuda there = occupied.get(position);
        if (there != null && !there.equals(fuda)) {
            throw new IllegalArgumentException(position + " is already taken by " + there);
        }
        remove(fuda);
        positions.put(fuda, position);
        occupied.put(position, fuda);
    }

    public void remove(Fuda fuda) {
        Position old = positions.remove(fuda);
        if (old != null) occupied.remove(old);
    }

    @Nullable
    public Position getPosition(Fuda fuda) { return positions.get(fuda); }

    public boolean contains(Fuda fuda) { return positions.containsKey(fuda); }

    public Map<Fuda, Position> asMap() { return Collections.unmodifiableMap(positions); }

    public int size() { return positions.size(); }

    public int count(Jin jin) {
        int n = 0;
        for (Position p : positions.values()) if (p.getJin() == jin) n++;
        return n;
    }

    /** 札 in this layout whose position differs from the target (exact match), for 暗記 scoring. */
    public List<Fuda> compare(Layout target) {
        List<Fuda> misplaced = new ArrayList<>();
        for (Map.Entry<Fuda, Position> e : target.positions.entrySet()) {
            if (!e.getValue().equals(positions.get(e.getKey()))) misplaced.add(e.getKey());
        }
        return misplaced;
    }

    public Layout copy() {
        Layout c = new Layout();
        c.positions.putAll(positions);
        c.occupied.putAll(occupied);
        return c;
    }
}
