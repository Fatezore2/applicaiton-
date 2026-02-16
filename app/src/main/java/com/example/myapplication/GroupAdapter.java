package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.VH> {

    public interface OnGroupClick {
        void onClick(GroupItem group);
    }

    private final List<GroupItem> list;
    private final OnGroupClick listener;

    public GroupAdapter(List<GroupItem> list, OnGroupClick listener) {
        this.list = list;
        this.listener = listener;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv;

        VH(TextView v) {
            super(v);
            tv = v;
        }
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView tv = new TextView(parent.getContext());
        tv.setPadding(20, 20, 20, 20);
        tv.setTextSize(16f);
        tv.setBackgroundResource(android.R.drawable.list_selector_background);
        return new VH(tv);
    }


    @Override
    public void onBindViewHolder(VH holder, int position) {
        final GroupItem g = list.get(position);
        holder.tv.setText("👥 " + g.getName());

        holder.tv.setOnClickListener(new View.OnClickListener() {
            private long lastClickTime = 0;

            @Override
            public void onClick(View v) {
                long clickTime = System.currentTimeMillis();

                if (clickTime - lastClickTime < 400) {

                    String uid = FirebaseAuth.getInstance().getUid();
                    if (uid != null) {
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .update("currentGroupId", g.getId());
                    }

                    // Open chat
                    Intent intent = new Intent(v.getContext(), ChatMessageActivity.class);
                    intent.putExtra("GROUP_ID", g.getId());
                    intent.putExtra("GROUP_NAME", g.getName());
                    v.getContext().startActivity(intent);

                } else {
                    listener.onClick(g);
                }

                lastClickTime = clickTime;
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
