package com.example.karutasuke.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A named group of 札, e.g. 一字決まり. Immutable: editing a set through FudaSetManager
 * returns a new FudaSet with the same id.
 */
public final class FudaSet {

    private final String id;
    private final String name;
    private final List<Fuda> fuda;
    private final boolean builtIn;      // built-in sets can't be edited, only duplicated

    public FudaSet(String id, String name, List<Fuda> fuda, boolean builtIn) {
        this.id = id;
        this.name = name;
        this.fuda = Collections.unmodifiableList(new ArrayList<>(fuda));
        this.builtIn = builtIn;
    }

    /** A built-in set; its id is derived from the name. */
    public FudaSet(String name, List<Fuda> fuda) {
        this("builtin:" + name, name, fuda, true);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public List<Fuda> getFuda() { return fuda; }
    public int size() { return fuda.size(); }
    public boolean isBuiltIn() { return builtIn; }
}
