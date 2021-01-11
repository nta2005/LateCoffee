package com.nta.latecoffee.eventbus;

import com.nta.latecoffee.model.BestDealModel;

public class BestDealItemClick {
    private BestDealModel bestDealModel;

    public BestDealItemClick(BestDealModel bestDealModel) {
        this.bestDealModel = bestDealModel;
    }

    public BestDealModel getBestDealModel() {
        return bestDealModel;
    }

    public void setBestDealModel(BestDealModel bestDealModel) {
        this.bestDealModel = bestDealModel;
    }
}
