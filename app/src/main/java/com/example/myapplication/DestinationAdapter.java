package com.example.myapplication;

import android.content.Context;
import android.view.*;
import android.widget.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class DestinationAdapter extends BaseAdapter {

    private Context context;
    private List<Destination> list;
    private LayoutInflater inflater;

    private SimpleDateFormat format =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public DestinationAdapter(Context context, List<Destination> list) {
        this.context = context;
        this.list = list;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if(convertView == null){
            convertView = inflater.inflate(R.layout.destination_item,parent,false);
        }

        TextView tvCity = convertView.findViewById(R.id.tvCity);
        TextView tvTime = convertView.findViewById(R.id.tvTime);

        Destination item = list.get(position);

        tvCity.setText("📍 " + item.city);

        if(item.dateTime != null){
            tvTime.setText("🕒 " + format.format(item.dateTime));
        }

        return convertView;
    }
}