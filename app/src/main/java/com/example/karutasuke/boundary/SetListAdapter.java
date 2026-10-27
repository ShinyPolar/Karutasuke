package com.example.karutasuke.boundary;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.karutasuke.R;
import com.example.karutasuke.entity.FudaSet;

import java.util.ArrayList;
import java.util.List;

/** One row per 札 set in SetEditorPage: name, size, and whether it's built in. */
class SetListAdapter extends RecyclerView.Adapter<SetListAdapter.Row> {

    interface OnSetListener {
        void onSet(FudaSet set);
    }

    private final List<FudaSet> items = new ArrayList<>();
    private final OnSetListener onClick;
    private final OnSetListener onLongClick;

    SetListAdapter(OnSetListener onClick, OnSetListener onLongClick) {
        this.onClick = onClick;
        this.onLongClick = onLongClick;
    }

    @SuppressLint("NotifyDataSetChanged")
    void submit(List<FudaSet> sets) {
        items.clear();
        items.addAll(sets);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Row onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fuda_set, parent, false);
        Row row = new Row(view);
        view.setOnClickListener(v -> {
            int p = row.getBindingAdapterPosition();
            if (p != RecyclerView.NO_POSITION) onClick.onSet(items.get(p));
        });
        view.setOnLongClickListener(v -> {
            int p = row.getBindingAdapterPosition();
            if (p != RecyclerView.NO_POSITION) onLongClick.onSet(items.get(p));
            return true;
        });
        return row;
    }

    @Override
    public void onBindViewHolder(@NonNull Row row, int position) {
        FudaSet s = items.get(position);
        row.name.setText(s.getName());
        row.info.setText(row.info.getContext().getString(
                s.isBuiltIn() ? R.string.set_info_builtin : R.string.set_info_user, s.size()));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class Row extends RecyclerView.ViewHolder {
        final TextView name, info;

        Row(View v) {
            super(v);
            name = v.findViewById(R.id.set_name);
            info = v.findViewById(R.id.set_size);
        }
    }
}
