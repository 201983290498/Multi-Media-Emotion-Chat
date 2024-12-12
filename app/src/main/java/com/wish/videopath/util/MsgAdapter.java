package com.wish.videopath.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.wish.videopath.Emotion.entity.Msg;
import com.wish.videopath.R;

import java.util.List;

public class MsgAdapter extends RecyclerView.Adapter{
    class LeftViewHolder extends RecyclerView.ViewHolder {
        protected TextView msgText;
        public LeftViewHolder(@androidx.annotation.NonNull View itemView) {
            super(itemView);
            msgText = itemView.findViewById(R.id.leftMsg);
        }
    }

    class RightViewHolder extends RecyclerView.ViewHolder {
        protected TextView msgText;
        public RightViewHolder(@androidx.annotation.NonNull View itemView) {
            super(itemView);
            msgText = itemView.findViewById(R.id.rightMsg);
        }
    }

    private List<Msg> msgList;

    public MsgAdapter(List<Msg> msgList) {
        this.msgList = msgList;
    }

    @Override
    public int getItemViewType(int position) {
        Msg msg = msgList.get(position);
        return msg.type;
    }


    @androidx.annotation.NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@androidx.annotation.NonNull ViewGroup viewGroup, int i) {
        Context context = viewGroup.getContext();
        if(i == BaseString.TYPE_RECEIVE){
            View view = LayoutInflater.from(context).inflate(R.layout.msg_left_item, viewGroup, false);
            return new LeftViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.activity_right_item, viewGroup, false);
            return new RightViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@androidx.annotation.NonNull RecyclerView.ViewHolder viewHolder, int i) {
        Msg msg = msgList.get(i);
        if(viewHolder instanceof LeftViewHolder){
            ((LeftViewHolder)viewHolder).msgText.setText(msg.msg);
        } else {
            ((RightViewHolder)viewHolder).msgText.setText(msg.msg);
        }
    }


    @Override
    public int getItemCount() {
        if (msgList != null)
            return msgList.size();
        else
            return 0;
    }
}
