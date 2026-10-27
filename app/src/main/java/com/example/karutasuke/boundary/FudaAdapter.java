package com.example.karutasuke.boundary;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.karutasuke.R;
import com.example.karutasuke.entity.Fuda;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Shows one 取り札 card per Fuda in FudaPage's grid. */
class FudaAdapter extends RecyclerView.Adapter<FudaAdapter.Row> {

    private static final float UNSELECTED_ALPHA = 0.3f;

    interface OnFudaClickListener {
        void onFudaClick(Fuda fuda);
    }

    private final List<Fuda> items = new ArrayList<>();
    private final OnFudaClickListener listener;
    @Nullable private Set<Fuda> selection;

    FudaAdapter(OnFudaClickListener listener) {
        this.listener = listener;
    }

    /**
     * Shows which 札 are selected (SetEditorPage): selected cards are bright with a ✓, the rest faded.
     * The set is read live, so after changing it call refresh(fuda). null turns selection display off.
     */
    @SuppressLint("NotifyDataSetChanged")
    void setSelection(@Nullable Set<Fuda> selection) {
        this.selection = selection;
        notifyDataSetChanged();
    }

    /** Redraws one card, e.g. after its selection changed. */
    void refresh(Fuda fuda) {
        int i = items.indexOf(fuda);
        if (i >= 0) notifyItemChanged(i);
    }

    @SuppressLint("NotifyDataSetChanged") // the whole list is re-sorted, so every row changes
    void submit(List<Fuda> fuda) {
        items.clear();
        items.addAll(fuda);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Row onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fuda, parent, false);
        Row row = new Row(view);
        view.setOnClickListener(v -> {
            int position = row.getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) listener.onFudaClick(items.get(position));
        });
        return row;
    }

    @Override
    public void onBindViewHolder(@NonNull Row row, int position) {
        Fuda f = items.get(position);
        row.card.setLines(f.getTorifuda());
        boolean selected = selection != null && selection.contains(f);
        row.itemView.setAlpha(selection == null || selected ? 1f : UNSELECTED_ALPHA);
        row.caption.setText(row.caption.getContext().getString(
                selected ? R.string.fuda_caption_selected : R.string.fuda_caption, f.getNumber(), f.getKimariji()));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class Row extends RecyclerView.ViewHolder {
        final TorifudaView card;
        final TextView caption;

        Row(View v) {
            super(v);
            card = v.findViewById(R.id.fuda_card);
            caption = v.findViewById(R.id.fuda_caption);
        }
    }
}
