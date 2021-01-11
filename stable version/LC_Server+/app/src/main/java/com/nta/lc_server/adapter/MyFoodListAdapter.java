package com.nta.lc_server.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.FirebaseDatabase;
import com.nta.lc_server.R;
import com.nta.lc_server.callback.IRecyclerClickListener;
import com.nta.lc_server.common.Common;
import com.nta.lc_server.model.BestDealsModel;
import com.nta.lc_server.model.FoodModel;
import com.nta.lc_server.model.MostPopularModel;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyFoodListAdapter extends RecyclerView.Adapter<MyFoodListAdapter.MyViewHolder> {

    private Context context;
    private List<FoodModel> foodModelList;

    private ExpandableLayout lastExpandable;

    public MyFoodListAdapter(Context context, List<FoodModel> foodModelList) {
        this.context = context;
        this.foodModelList = foodModelList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.layout_food_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Glide.with(context).load(foodModelList.get(position).getImage()).into(holder.img_food_image);
        holder.txt_food_price.setText(new StringBuilder("")
                .append(foodModelList.get(position).getPrice() + " đ"));
        holder.txt_food_name.setText(new StringBuilder("")
                .append(foodModelList.get(position).getName()));

        //Event
        holder.setListener((view, pos) -> {
            Common.selectedFood = foodModelList.get(pos);
            //To assign this data for 'key' of food, we will assign when we retrieve foods from Category
            Common.selectedFood.setKey(String.valueOf(pos));

            //Show expandable
            if (lastExpandable != null && lastExpandable.isExpanded()) lastExpandable.collapse();
            if (!holder.expandable_layout.isExpanded()) {
                holder.expandable_layout.setSelected(true);
                holder.expandable_layout.expand();
            } else {
                holder.expandable_layout.collapse();
                holder.expandable_layout.setSelected(false);
            }
            lastExpandable = holder.expandable_layout;

        });

        holder.btn_best_deal.setOnClickListener(view -> {
            makeFoodToBestDeal(foodModelList.get(position));
        });

        holder.btn_most_popular.setOnClickListener(view -> {
            makeFoodToPopular(foodModelList.get(position));
        });
    }

    //Thay code 20/12 Q.Huy
//    private void makeFoodToPopular(FoodModel foodModel) {
//        MostPopularModel mostPopularModel = new MostPopularModel();
//        mostPopularModel.setName(foodModel.getName());
//        mostPopularModel.setMenu_id(Common.categorySelected.getMenu_id());
//        mostPopularModel.setFood_id(foodModel.getId());
//        mostPopularModel.setImage(foodModel.getImage());
//
//        FirebaseDatabase.getInstance()
//                .getReference(Common.MOST_POPULAR)
//                .child(new StringBuilder(mostPopularModel.getMenu_id())
//                        .append("_")
//                        .append(mostPopularModel.getFood_id())
//                        .toString()) // Use menu_food_id to key
//                .setValue(mostPopularModel)
//                .addOnFailureListener(e -> {
//                    Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
//                })
//                .addOnSuccessListener(aVoid -> {
//                    Toast.makeText(context, "Tạo mục phổ biến thành công!", Toast.LENGTH_SHORT).show();
//                });
//    }

    private void makeFoodToPopular(FoodModel foodModel) {
        MostPopularModel mostPopularModel = new MostPopularModel();

        mostPopularModel.setName(foodModel.getName());

        mostPopularModel.setMenu_id(Common.categorySelected.getMenu_id());
        mostPopularModel.setImage(foodModel.getImage());
        mostPopularModel.setFood_id(foodModel.getId());

        String menu_food_id = new StringBuilder(mostPopularModel.getFood_id())
                .append("_")
                .append(mostPopularModel.getFood_id())
                .toString();
        FirebaseDatabase.getInstance()
                .getReference(Common.MOST_POPULAR)
                .child(menu_food_id) // Use menu_food_id to key

                .setValue(mostPopularModel)
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Vui lòng kiểm tra mục phổ biến!", Toast.LENGTH_SHORT).show();
                });
    }

    private void makeFoodToBestDeal(FoodModel foodModel) {
        BestDealsModel bestDealsModel = new BestDealsModel();

        bestDealsModel.setName(foodModel.getName());
        bestDealsModel.setMenu_id(Common.categorySelected.getMenu_id());
        bestDealsModel.setFood_id(foodModel.getId());
        bestDealsModel.setImage(foodModel.getImage());

        String menu_food_id = new StringBuilder(bestDealsModel.getMenu_id())
                .append("_")
                .append(bestDealsModel.getFood_id())
                .toString();

        FirebaseDatabase.getInstance()
                .getReference(Common.BEST_DEALS)
                .child(menu_food_id) // Use menu_food_id to key
                .setValue(bestDealsModel)
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Vui lòng kiểm tra mục bán chạy!", Toast.LENGTH_SHORT).show();
                });

    }

    @Override
    public int getItemCount() {
        return foodModelList.size();
    }

    public FoodModel getItemAtPosition(int pos) {
        return foodModelList.get(pos);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.txt_food_name)
        TextView txt_food_name;
        @BindView(R.id.txt_food_price)
        TextView txt_food_price;
        @BindView(R.id.img_food_image)
        ImageView img_food_image;
        @BindView(R.id.expandable_layout)
        ExpandableLayout expandable_layout;
        @BindView(R.id.btn_best_deal)
        Button btn_best_deal;
        @BindView(R.id.btn_most_popular)
        Button btn_most_popular;
        IRecyclerClickListener listener;
        private Unbinder unbinder;


        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        public void setListener(IRecyclerClickListener listener) {
            this.listener = listener;
        }

        @Override
        public void onClick(View view) {
            listener.onItemClickListener(view, getAdapterPosition());
        }
    }
}