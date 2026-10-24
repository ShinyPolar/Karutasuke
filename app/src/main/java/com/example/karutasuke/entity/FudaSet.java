package com.example.karutasuke.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A named group of 札, e.g. 一字決まり. Immutable. */
public final class FudaSet {

    private final String name;
    private final List<Fuda> fuda;

    public FudaSet(String name, List<Fuda> fuda) {
        this.name = name;
        this.fuda = Collections.unmodifiableList(new ArrayList<>(fuda));
    }

    public String getName() { return name; }
    public List<Fuda> getFuda() { return fuda; }
    public int size() { return fuda.size(); }
}
