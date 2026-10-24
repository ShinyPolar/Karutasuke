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

/** Shows one row per Fuda in FudaPage's list. */
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
        row.number.setText(String.valueOf(f.getNumber()));
        row.kimariji.setText(f.getKimariji());
        row.kaminoku.setText(f.getKaminoku());
        row.shimonoku.setText(f.getShimonoku());
        row.torifuda.setText(String.join(" ", f.getTorifuda()));
        row.poet.setText(f.getPoet());
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class Row extends RecyclerView.ViewHolder {
        final TextView number, kimariji, kaminoku, shimonoku, torifuda, poet;

        Row(View v) {
            super(v);
            number = v.findViewById(R.id.fuda_number);
            kimariji = v.findViewById(R.id.fuda_kimariji);
            kaminoku = v.findViewById(R.id.fuda_kaminoku);
            shimonoku = v.findViewById(R.id.fuda_shimonoku);
            torifuda = v.findViewById(R.id.fuda_torifuda);
            poet = v.findViewById(R.id.fuda_poet);
        }
    }
}
