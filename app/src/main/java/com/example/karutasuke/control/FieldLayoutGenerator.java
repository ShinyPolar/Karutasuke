package com.example.karutasuke.control;

import com.example.karutasuke.entity.Fuda;
import com.example.karutasuke.entity.Jin;
import com.example.karutasuke.entity.Layout;
import com.example.karutasuke.entity.Position;
import com.example.karutasuke.entity.Side;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Lays 札 out on the field the way players do: split between 自陣 and 敵陣,
 * spread over the 3 段 and both sides, and packed in from the outer edges.
 */
public final class FieldLayoutGenerator {

    /** How likely each 段 is: players keep most 札 in the 下段, fewest in the 上段. */
    private static final double[] DAN_WEIGHTS = {0.45, 0.30, 0.25};   // 下段, 中段, 上段

    private FieldLayoutGenerator() { }

    /** @throws IllegalArgumentException if there are more than 50 札 */
    public static Layout generate(List<Fuda> fuda, Random random) {
        if (fuda.size() > 2 * Layout.MAX_PER_JIN) {
            throw new IllegalArgumentException("At most " + 2 * Layout.MAX_PER_JIN + " 札 fit on the field");
        }
        List<Fuda> shuffled = new ArrayList<>(fuda);
        Collections.shuffle(shuffled, random);

        int opponentCount = shuffled.size() / 2;   // with an odd number, 自陣 gets the extra one
        Layout layout = new Layout();
        placeJin(layout, shuffled.subList(0, opponentCount), Jin.OPPONENT, random);
        placeJin(layout, shuffled.subList(opponentCount, shuffled.size()), Jin.SELF, random);
        return layout;
    }

    private static void placeJin(Layout layout, List<Fuda> fuda, Jin jin, Random random) {
        Side[] sides = Side.values();
        int[][] used = new int[sides.length][Position.DAN_COUNT];   // 札 per side and 段 so far

        for (Fuda f : fuda) {
            // Pick a (side, 段) that still has room, weighted toward the 下段.
            double total = 0;
            for (int s = 0; s < sides.length; s++) {
                for (int d = 0; d < Position.DAN_COUNT; d++) {
                    if (used[s][d] < Position.COLUMNS_PER_SIDE) total += DAN_WEIGHTS[d];
                }
            }
            double roll = random.nextDouble() * total;
            int side = -1, dan = -1;
            outer:
            for (int s = 0; s < sides.length; s++) {
                for (int d = 0; d < Position.DAN_COUNT; d++) {
                    if (used[s][d] >= Position.COLUMNS_PER_SIDE) continue;
                    side = s;
                    dan = d;
                    roll -= DAN_WEIGHTS[d];
                    if (roll < 0) break outer;
                }
            }
            used[side][dan]++;
            // Columns fill from the outer edge inward, like a real 陣.
            layout.place(f, new Position(jin, sides[side], dan + 1, used[side][dan]));
        }
    }
}
