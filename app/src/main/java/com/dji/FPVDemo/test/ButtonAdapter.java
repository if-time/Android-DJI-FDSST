package com.dji.FPVDemo.test;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.recyclerview.widget.RecyclerView;

import com.dji.FPVDemo.R;

import java.util.List;

/**
 * @author dongsiyuan
 * @date 2020/11/13 16:38
 */
public class ButtonAdapter extends RecyclerView.Adapter<ButtonAdapter.ViewHolder> {

    private List<String> buttonList;

    static class ViewHolder extends RecyclerView.ViewHolder {

        Button btnOpen;

        public ViewHolder(View view) {
            super(view);
            btnOpen = view.findViewById(R.id.btnOpen);
        }
    }

    public ButtonAdapter(List<String> buttonList, OnItemClickListener listener) {
        this.buttonList = buttonList;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_test_recyclerview, null, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String btnOpenString = buttonList.get(position);
        holder.btnOpen.setText(btnOpenString);
        holder.btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return buttonList.size();
    }

    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onClick(int pos);
    }
}
