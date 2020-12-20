package com.nta.latecoffee.model;

import java.util.Map;

public class CommentModel {
    private float ratingValue;
    private String comment, name, uid;
    private Map<String,Object> commentTimeStamp;

    public CommentModel() {
    }

    public float getRatingValue() {
        return ratingValue;
    }

    public CommentModel setRatingValue(float ratingValue) {
        this.ratingValue = ratingValue;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public CommentModel setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public String getName() {
        return name;
    }

    public CommentModel setName(String name) {
        this.name = name;
        return this;
    }

    public String getUid() {
        return uid;
    }

    public CommentModel setUid(String uid) {
        this.uid = uid;
        return this;
    }

    public Map<String, Object> getCommentTimeStamp() {
        return commentTimeStamp;
    }

    public CommentModel setCommentTimeStamp(Map<String, Object> commentTimeStamp) {
        this.commentTimeStamp = commentTimeStamp;
        return this;
    }
}
