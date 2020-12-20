package com.nta.latecoffee.callback;

import com.nta.latecoffee.model.CommentModel;

import java.util.List;

public interface ICommentCallbackListener {
    void onCommentLoadSuccess(List<CommentModel> commentModels);

    void onCommentLoadError(String message);
}
