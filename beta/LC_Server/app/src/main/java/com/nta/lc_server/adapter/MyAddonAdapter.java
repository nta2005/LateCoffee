package com.nta.lc_server.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nta.lc_server.R;
import com.nta.lc_server.callback.IRecyclerClickListener;
import com.nta.lc_server.eventbus.SelectAddonModel;
import com.nta.lc_server.eventbus.SelectSizeModel;
import com.nta.lc_server.eventbus.UpdateAddonModel;
import com.nta.lc_server.model.AddonModel;
import com.nta.lc_server.model.SizeModel;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyAddonAdapter extends RecyclerView.Adapter<MyAddonAdapter.MyViewHolder> {

    Context context;
    List<AddonModel> addonModelList;
    UpdateAddonModel updateAddonModel;
    int editPos;

    public MyAddonAdapter(Context context, List<AddonModel> addonModelList) {
        this.context = context;
        this.addonModelList = addonModelList;
        editPos = -1;
        updateAddonModel = new UpdateAddonModel();
    }

    @NonNull
    @Override
    public MyAddonAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyAddonAdapter.MyViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.layout_size_addon_display, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyAddonAdapter.MyViewHolder holder, int position) {
        holder.txt_name.setText(addonModelList.get(position).getName());
        holder.txt_price.setText(String.valueOf(addonModelList.get(position).getPrice()));

        //Event
        holder.img_delete.setOnClickListener(view -> {
            //Delete item
            addonModelList.remove(position);
            notifyItemRemoved(position);
            updateAddonModel.setAddonModelList(addonModelList); //Set for event
            EventBus.getDefault().postSticky(updateAddonModel); //Send event

        });

        holder.setListener((view, pos) -> {
            editPos = position;
            EventBus.getDefault().postSticky(new SelectAddonModel(addonModelList.get(pos)));
        });
    }

    @Override
    public int getItemCount() {
        return addonModelList.size();
    }

    public void addNewAddon(AddonModel addonModel) {
        addonModelList.add(addonModel);
        notifyItemInserted(addonModelList.size() - 1);
        updateAddonModel.setAddonModelList(addonModelList);
        EventBus.getDefault().postSticky(updateAddonModel);
    }

    public void editAddon(AddonModel addonModel) {
        if (editPos != -1) {
            addonModelList.set(editPos, addonModel);
            notifyItemChanged(editPos);
            editPos = -1; //Reset variable after success
            //Send update
            updateAddonModel.setAddonModelList(addonModelList);
            EventBus.getDefault().postSticky(updateAddonModel);
        }
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.txt_name)
        TextView txt_name;
        @BindView(R.id.txt_price)
        TextView txt_price;
        @BindView(R.id.img_delete)
        ImageView img_delete;

        Unbinder unbinder;

        IRecyclerClickListener listener;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(view ->
                    listener.onItemClickListener(view, getAdapterPosition())
            );
        }

        public void setListener(IRecyclerClickListener listener) {
            this.listener = listener;
        }


    }
}
