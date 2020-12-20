package com.nta.lc_server.view_holder;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nta.lc_server.R;
import com.nta.lc_server.callback.IRecyclerClickListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class ChatListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private Unbinder unbinder;

    @BindView(R.id.txt_email)
    public TextView txt_email;
    @BindView(R.id.txt_chat_message)
    public TextView txt_chat_message;

    IRecyclerClickListener listener;

    public void setListener(IRecyclerClickListener listener) {
        this.listener = listener;
    }

    public ChatListViewHolder(@NonNull View itemView) {
        super(itemView);
        unbinder= ButterKnife.bind(this,itemView);
        itemView.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        listener.onItemClickListener(view,getAdapterPosition());
    }
}
