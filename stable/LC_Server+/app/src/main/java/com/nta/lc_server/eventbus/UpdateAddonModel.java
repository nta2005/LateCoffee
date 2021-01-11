package com.nta.lc_server.eventbus;

import com.nta.lc_server.model.AddonModel;

import java.util.List;

public class UpdateAddonModel {
    private List<AddonModel> addonModelList;

    public UpdateAddonModel() {

    }

    public List<AddonModel> getAddonModelList() {
        return addonModelList;
    }

    public void setAddonModelList(List<AddonModel> addonModelList) {
        this.addonModelList = addonModelList;
    }
}
