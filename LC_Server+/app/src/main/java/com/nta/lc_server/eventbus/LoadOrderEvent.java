package com.nta.lc_server.eventbus;

public class LoadOrderEvent {

    private int status;

    public LoadOrderEvent(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
