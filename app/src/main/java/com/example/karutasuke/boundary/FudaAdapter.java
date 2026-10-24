package com.example.karutasuke.boundary;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.karutasuke.R;
import com.example.karutasuke.entity.Fuda;

import java.util.ArrayList;
import java.util.List;

/** Shows one 取り札 card per Fuda in FudaPage's grid. */
class FudaAdapter extends RecyclerView.Adapter<FudaAdapter.Row> {

    private final List<Fuda> items = new ArrayList<>();

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
        return new Row(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Row row, int position) {
        Fuda f = items.get(position);
        row.card.setLines(f.getTorifuda());
        row.caption.setText(row.caption.getContext().getString(
                R.string.fuda_caption, f.getNumber(), f.getKimariji()));
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
