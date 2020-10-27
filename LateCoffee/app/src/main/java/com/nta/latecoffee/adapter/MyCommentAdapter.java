package com.nta.latecoffee.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nta.latecoffee.model.CommentModel;
import com.nta.latecoffee.R;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyCommentAdapter extends RecyclerView.Adapter<MyCommentAdapter.MyViewHolder> {

    Context context;
    List<CommentModel> commentModelList;

    public MyCommentAdapter(Context context, List<CommentModel> commentModelList) {
        this.context = context;
        this.commentModelList = commentModelList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.layout_comment_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Long timeStamp = Long.valueOf(commentModelList.get(position).getCommentTimeStamp().get("timeStamp").toString());


        holder.txt_comment_date.setText(DateUtils.getRelativeTimeSpanString(timeStamp));
        holder.txt_comment.setText(commentModelList.get(position).getComment());
        holder.txt_comment_name.setText(commentModelList.get(position).getName());
        holder.rating_bar_cmt.setRating(commentModelList.get(position).getRatingValue());
    }

    @Override
    public int getItemCount() {
        return commentModelList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.txt_comment_date)
        TextView txt_comment_date;
        @BindView(R.id.txt_comment)
        TextView txt_comment;
        @BindView(R.id.txt_comment_name)
        TextView txt_comment_name;
        @BindView(R.id.rating_bar_cmt)
        RatingBar rating_bar_cmt;
        private Unbinder unbinder;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this, itemView);
        }
    }
}
