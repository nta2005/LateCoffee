package com.nta.lc_server.eventbus;

import com.nta.lc_server.model.OrderModel;

public class PrintOrderEvent {
    private String path;
    private OrderModel orderModel;

    public PrintOrderEvent(String path, OrderModel orderModel) {
        this.path = path;
        this.orderModel = orderModel;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public OrderModel getOrderModel() {
        return orderModel;
    }

    public void setOrderModel(OrderModel orderModel) {
        this.orderModel = orderModel;
    }
}
