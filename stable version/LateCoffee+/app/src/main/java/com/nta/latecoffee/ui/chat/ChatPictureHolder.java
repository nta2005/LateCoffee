package com.nta.latecoffee.ui.chat;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nta.latecoffee.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import de.hdodenhof.circleimageview.CircleImageView;

public class ChatPictureHolder extends RecyclerView.ViewHolder {

    private final Unbinder unbinder;
    @BindView(R.id.txt_time)
    public TextView txt_time;
    @BindView(R.id.txt_chat_message)
    public TextView txt_chat_message;
    @BindView(R.id.profile_image)
    public CircleImageView profile_image;
    @BindView(R.id.img_preview)
    public ImageView img_preview;
    @BindView(R.id.txt_email)
    public TextView txt_email;

    public ChatPictureHolder(@NonNull View itemView) {
        super(itemView);
        unbinder = ButterKnife.bind(this, itemView);
    }
}
