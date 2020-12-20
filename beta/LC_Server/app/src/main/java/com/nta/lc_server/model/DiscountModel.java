package com.nta.lc_server.model;

public class DiscountModel {
    private String key;
    private int percent;
    private long untilDate;

    public DiscountModel() {
    }

    public DiscountModel(String key, int percent, long untilDate) {
        this.key = key;
        this.percent = percent;
        this.untilDate = untilDate;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public long getUntilDate() {
        return untilDate;
    }

    public void setUntilDate(long untilDate) {
        this.untilDate = untilDate;
    }
}
