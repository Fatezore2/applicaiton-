package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.VH> {

    private final List<GroupItem> list;
    private final Context context;

    public GroupListAdapter(Context context, List<GroupItem> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        final GroupItem g = list.get(position);

        holder.tv.setText("👥 " + g.getName());

        holder.tv.setOnClickListener(v -> {
            // Open ChatMessageActivity
            Intent intent = new Intent(context, ChatMessageActivity.class);
            intent.putExtra("GROUP_ID", g.getId());
            intent.putExtra("GROUP_NAME", g.getName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv;

        public VH(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(android.R.id.text1);
        }
    }
}
