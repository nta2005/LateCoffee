package com.nta.lc_server.ui.chat;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nta.lc_server.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import de.hdodenhof.circleimageview.CircleImageView;

public class ChatTextHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.txt_time)
    public TextView txt_time;
    @BindView(R.id.txt_email)
    public TextView txt_email;
    @BindView(R.id.txt_chat_message)
    public TextView txt_chat_message;
    @BindView(R.id.profile_image)
    public CircleImageView profile_image;
    private Unbinder unbinder;

    public ChatTextHolder(@NonNull View itemView) {
        super(itemView);
        unbinder = ButterKnife.bind(this, itemView);
    }
}
