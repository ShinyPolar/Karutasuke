package com.example.karutasuke.control;

import android.content.Context;

import com.example.karutasuke.entity.Fuda;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads the 100 poems from assets/fuda.json once, then serves lookups and sorted lists.
 *
 * Usage: FudaCatalog.getInstance(context).sortBy(SortKey.KIMARIJI, SortOrder.ASC)
 */
public final class FudaCatalog {

    private static final String ASSET_FILE = "fuda.json";
    private static final int FUDA_COUNT = 100;

    private static FudaCatalog instance;

    private final List<Fuda> fuda;              // in 歌番号 order
    private final Map<Integer, Fuda> byNumber;

    private FudaCatalog(List<Fuda> fuda) {
        this.fuda = Collections.unmodifiableList(fuda);
        this.byNumber = new HashMap<>();
        for (Fuda f : fuda) byNumber.put(f.getNumber(), f);
    }

    /** Returns the shared catalog, loading it from assets the first time. */
    public static synchronized FudaCatalog getInstance(Context context) {
        if (instance == null) {
            try (InputStream in = context.getApplicationContext().getAssets().open(ASSET_FILE)) {
                instance = fromJson(readAll(in));
            } catch (IOException | JSONException e) {
                throw new IllegalStateException("Could not load " + ASSET_FILE, e);
            }
        }
        return instance;
    }

    /** Parses the catalog JSON. Separate from getInstance() so it doesn't need an Android Context. */
    static FudaCatalog fromJson(String json) throws JSONException {
        JSONArray array = new JSONObject(json).getJSONArray("fuda");
        List<Fuda> list = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);
            int number = o.getInt("number");

            JSONArray tf = o.getJSONArray("torifuda");
            List<String> torifuda = new ArrayList<>(tf.length());
            for (int j = 0; j < tf.length(); j++) torifuda.add(tf.getString(j));

            String audioName = String.format(Locale.ROOT, "%03d.mp3", number);
            list.add(new Fuda(
                    number,
                    o.getString("poet"),
                    o.getString("kaminoku"),
                    o.getString("kaminokuHiragana"),
                    o.getString("shimonoku"),
                    o.getString("shimonokuHiragana"),
                    o.getString("kimariji"),
                    torifuda,
                    "audio/kaminoku/" + audioName,
                    "audio/shimonoku/" + audioName));
        }
        if (list.size() != FUDA_COUNT) {
            throw new JSONException("Expected " + FUDA_COUNT + " fuda but found " + list.size());
        }
        list.sort(Comparator.comparingInt(Fuda::getNumber));
        return new FudaCatalog(list);
    }

    /** @throws IllegalArgumentException if n is not 1–100 */
    public Fuda getByNumber(int n) {
        Fuda f = byNumber.get(n);
        if (f == null) throw new IllegalArgumentException("No fuda with number " + n);
        return f;
    }

    /** Returns a new list of all 100 fuda, sorted. Ties are broken by 歌番号. */
    public List<Fuda> sortBy(SortKey key, SortOrder order) {
        // Japanese collation puts kana in 五十音 order and treats が next to か.
        Collator collator = Collator.getInstance(Locale.JAPANESE);
        Comparator<Fuda> comparator;
        switch (key) {
            case KIMARIJI:
                comparator = Comparator.comparing(Fuda::getKimariji, collator);
                break;
            case KAMINOKU:
                comparator = Comparator.comparing(Fuda::getKaminokuHiragana, collator);
                break;
            case SHIMONOKU:
                comparator = Comparator.comparing(Fuda::getShimonokuHiragana, collator);
                break;
            case NUMBER:
            default:
                comparator = (a, b) -> 0;
                break;
        }
        comparator = comparator.thenComparingInt(Fuda::getNumber);
        if (order == SortOrder.DESC) comparator = comparator.reversed();

        List<Fuda> sorted = new ArrayList<>(fuda);
        sorted.sort(comparator);
        return sorted;
    }

    public int size() { return fuda.size(); }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
