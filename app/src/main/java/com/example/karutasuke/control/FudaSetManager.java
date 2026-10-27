package com.example.karutasuke.control;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.example.karutasuke.entity.Fuda;
import com.example.karutasuke.entity.FudaSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Built-in 札 sets plus the user's own sets (FR-02). User sets are saved as JSON
 * (poem numbers only) through a Storage, which is SharedPreferences in the app.
 */
public final class FudaSetManager {

    /** Where the user's sets are saved. An interface so the manager can be tested without Android. */
    public interface Storage {
        @Nullable
        String load();

        void save(String json);
    }

    private static final String PREFS_NAME = "fuda_sets";
    private static final String PREFS_KEY = "user_sets";
    private static final String USER_ID_PREFIX = "user:";

    private static FudaSetManager instance;

    private final FudaCatalog catalog;
    private final Storage storage;
    private final List<FudaSet> builtIn;
    private final List<FudaSet> userSets = new ArrayList<>();

    public FudaSetManager(FudaCatalog catalog, Storage storage) {
        this.catalog = catalog;
        this.storage = storage;
        this.builtIn = Collections.unmodifiableList(getBuiltInSets(catalog));
        load();
    }

    /** The shared manager, saving to the app's private SharedPreferences. */
    public static synchronized FudaSetManager getInstance(Context context) {
        if (instance == null) {
            SharedPreferences prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            instance = new FudaSetManager(FudaCatalog.getInstance(context), new Storage() {
                @Override
                public String load() { return prefs.getString(PREFS_KEY, null); }

                @Override
                public void save(String json) { prefs.edit().putString(PREFS_KEY, json).apply(); }
            });
        }
        return instance;
    }

    // --- reading ---

    /** Built-in sets first, then the user's sets in the order they were created. */
    public List<FudaSet> getAllSets() {
        List<FudaSet> all = new ArrayList<>(builtIn);
        all.addAll(userSets);
        return all;
    }

    public List<FudaSet> getUserSets() { return new ArrayList<>(userSets); }

    public List<FudaSet> getBuiltIn() { return builtIn; }

    @Nullable
    public FudaSet getById(String id) {
        for (FudaSet s : getAllSets()) if (s.getId().equals(id)) return s;
        return null;
    }

    // --- editing (user sets only) ---

    /** @throws IllegalArgumentException if the name is blank */
    public FudaSet createSet(String name, Collection<Fuda> fuda) {
        FudaSet set = new FudaSet(USER_ID_PREFIX + UUID.randomUUID(), cleanName(name), sorted(fuda), false);
        userSets.add(set);
        save();
        return set;
    }

    public FudaSet renameSet(FudaSet set, String name) {
        return replace(set, new FudaSet(set.getId(), cleanName(name), set.getFuda(), false));
    }

    /** Replaces the 札 in a user set. */
    public FudaSet setFuda(FudaSet set, Collection<Fuda> fuda) {
        return replace(set, new FudaSet(set.getId(), current(set).getName(), sorted(fuda), false));
    }

    public FudaSet addFuda(FudaSet set, Fuda fuda) {
        Set<Fuda> s = new LinkedHashSet<>(current(set).getFuda());
        s.add(fuda);
        return setFuda(set, s);
    }

    public FudaSet removeFuda(FudaSet set, Fuda fuda) {
        Set<Fuda> s = new LinkedHashSet<>(current(set).getFuda());
        s.remove(fuda);
        return setFuda(set, s);
    }

    public void deleteSet(FudaSet set) {
        userSets.remove(indexOf(set));
        save();
    }

    /** Copies any set (built-in or user) into a new, editable user set named "…のコピー". */
    public FudaSet duplicateSet(FudaSet set) {
        return createSet(set.getName() + "のコピー", set.getFuda());
    }

    // --- helpers ---

    private FudaSet replace(FudaSet set, FudaSet updated) {
        userSets.set(indexOf(set), updated);
        save();
        return updated;
    }

    private FudaSet current(FudaSet set) { return userSets.get(indexOf(set)); }

    private int indexOf(FudaSet set) {
        if (set.isBuiltIn()) throw new IllegalArgumentException("Built-in sets can't be edited: " + set.getName());
        for (int i = 0; i < userSets.size(); i++) {
            if (userSets.get(i).getId().equals(set.getId())) return i;
        }
        throw new IllegalArgumentException("No user set with id " + set.getId());
    }

    private static String cleanName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("Set name is blank");
        return trimmed;
    }

    private static List<Fuda> sorted(Collection<Fuda> fuda) {
        List<Fuda> list = new ArrayList<>(new LinkedHashSet<>(fuda));
        list.sort(Comparator.comparingInt(Fuda::getNumber));
        return list;
    }

    private void load() {
        userSets.clear();
        String json = storage.load();
        if (json == null) return;
        try {
            JSONArray array = new JSONObject(json).getJSONArray("sets");
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                JSONArray numbers = o.getJSONArray("fuda");
                List<Fuda> fuda = new ArrayList<>();
                for (int j = 0; j < numbers.length(); j++) {
                    int n = numbers.getInt(j);
                    if (n >= 1 && n <= 100) fuda.add(catalog.getByNumber(n));
                }
                userSets.add(new FudaSet(o.getString("id"), o.getString("name"), sorted(fuda), false));
            }
        } catch (JSONException e) {
            userSets.clear();   // unreadable data: start with no user sets rather than crash
        }
    }

    private void save() {
        try {
            JSONArray array = new JSONArray();
            for (FudaSet s : userSets) {
                JSONArray numbers = new JSONArray();
                for (Fuda f : s.getFuda()) numbers.put(f.getNumber());
                array.put(new JSONObject().put("id", s.getId()).put("name", s.getName()).put("fuda", numbers));
            }
            storage.save(new JSONObject().put("version", 1).put("sets", array).toString());
        } catch (JSONException e) {
            throw new IllegalStateException(e);   // only happens with invalid numbers, which can't occur here
        }
    }

    // --- built-in sets ---

    /**
     * 全札, then 一字決まり–六字決まり, then one set per opening sound with 2+ 札 (「あ」の札 …).
     */
    public static List<FudaSet> getBuiltInSets(FudaCatalog catalog) {
        List<Fuda> all = catalog.sortBy(SortKey.KIMARIJI, SortOrder.ASC);
        List<FudaSet> sets = new ArrayList<>();

        sets.add(new FudaSet("全札", catalog.sortBy(SortKey.NUMBER, SortOrder.ASC)));

        String[] lengthNames = {"一字決まり", "二字決まり", "三字決まり", "四字決まり", "五字決まり", "六字決まり"};
        for (int length = 1; length <= lengthNames.length; length++) {
            List<Fuda> matching = new ArrayList<>();
            for (Fuda f : all) if (f.getKimarijiLength() == length) matching.add(f);
            if (!matching.isEmpty()) sets.add(new FudaSet(lengthNames[length - 1], matching));
        }

        // Grouped by first sound, in 五十音 order because `all` is sorted by kimariji.
        Map<String, List<Fuda>> byFirstSound = new LinkedHashMap<>();
        for (Fuda f : all) {
            String first = f.getKimariji().substring(0, 1);
            List<Fuda> group = byFirstSound.get(first);
            if (group == null) {
                group = new ArrayList<>();
                byFirstSound.put(first, group);
            }
            group.add(f);
        }
        for (Map.Entry<String, List<Fuda>> e : byFirstSound.entrySet()) {
            if (e.getValue().size() >= 2) sets.add(new FudaSet("「" + e.getKey() + "」の札", e.getValue()));
        }
        return sets;
    }
}
