package com.example.karutasuke.control;

import com.example.karutasuke.entity.Fuda;
import com.example.karutasuke.entity.FudaSet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides 札 sets. For now only the built-in ones; creating and editing sets
 * (SetEditorPage) comes later.
 */
public final class FudaSetManager {

    private FudaSetManager() { }

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
