package com.nta.lc_server.ui.best_deals;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.nta.lc_server.R;
import com.nta.lc_server.adapter.MyBestDealsAdapter;
import com.nta.lc_server.common.Common;
import com.nta.lc_server.common.MySwipeHelper;
import com.nta.lc_server.eventbus.ToastEvent;
import com.nta.lc_server.model.BestDealsModel;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;

public class BestDealsFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1234;
    //Copy from categoryFragment
    Unbinder unbinder;
    @BindView(R.id.recycler_best_deals)
    RecyclerView recycler_best_deals;
    AlertDialog dialog;
    LayoutAnimationController layoutAnimationController;
    MyBestDealsAdapter adapter;
    List<BestDealsModel> bestDealsModels;
    ImageView img_best_deals;
    FirebaseStorage storage;
    StorageReference storageReference;
    private BestDealsViewModel bestDealsViewModel;
    private Uri imageUri = null;

    public static BestDealsFragment newInstance() {
        return new BestDealsFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        bestDealsViewModel =
                new ViewModelProvider(this).get(BestDealsViewModel.class);
        View root = inflater.inflate(R.layout.fragment_best_deals, container, false);

        unbinder = ButterKnife.bind(this, root);
        initViews();
        bestDealsViewModel.getMessageError().observe(getViewLifecycleOwner(), s -> {
            Toast.makeText(getContext(), "" + s, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        bestDealsViewModel.getBestDealsListMutable().observe(getViewLifecycleOwner(), bestDealsModelList -> {
            dialog.dismiss();
            bestDealsModels = bestDealsModelList;
            adapter = new MyBestDealsAdapter(getContext(), bestDealsModels);
            recycler_best_deals.setAdapter(adapter);
            recycler_best_deals.setLayoutAnimation(layoutAnimationController);
        });

        return root;
    }

    private void initViews() {

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        dialog = new SpotsDialog.Builder().setContext(getContext()).setCancelable(false).build();
        //dialog.show(); remove it to fix loading show when resume fragment
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_item_from_left);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_best_deals.setLayoutManager(layoutManager);
        recycler_best_deals.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));

        /////Swipe Button/////
        MySwipeHelper mySwipeHelper = new MySwipeHelper(getContext(), recycler_best_deals, 200) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {

                //Delete
                buf.add(new MyButton(getContext(), "Delete", 35, 0, Color.parseColor("#333639"),
                        pos -> {
                            Common.bestDealsSelected = bestDealsModels.get(pos);

                            showDeleteDialog();
                        }));

                //Update
                buf.add(new MyButton(getContext(), "Update", 35, 0, Color.parseColor("#560027"),
                        pos -> {
                            Common.bestDealsSelected = bestDealsModels.get(pos);

                            showUpdateDialog();
                        }));
            }
        };

    }

    private void showDeleteDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("Delete");
        builder.setMessage("Do you really want to delete this item?");
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> {
            dialogInterface.dismiss();
        })
                .setPositiveButton("Delete", (dialogInterface, i) -> {
                    deleteBestDeals();
                });

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteBestDeals() {
        FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentServerUser.getRestaurant())
                .child(Common.BEST_DEALS)
                .child(Common.bestDealsSelected.getKey())
                .removeValue()
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> {
                    bestDealsViewModel.loadBestDeals();
                    EventBus.getDefault().postSticky(new ToastEvent(Common.ACTION.DELETE, true));

                });
    }

    private void showUpdateDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("Update");
        builder.setMessage("Please fill information");

        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.layout_update_category, null);
        EditText edt_category_name = itemView.findViewById(R.id.edt_category_name);
        img_best_deals = itemView.findViewById(R.id.img_category);

        //Set data
        edt_category_name.setText(new StringBuilder("").append(Common.bestDealsSelected.getName()));
        Glide.with(getContext()).load(Common.bestDealsSelected.getImage()).into(img_best_deals);

        //Set event
        img_best_deals.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        });

        builder.setNegativeButton("CANCEL", (dialogInterface, i) -> dialogInterface.dismiss());
        builder.setPositiveButton("UPDATE", (dialogInterface, i) -> {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("name", edt_category_name.getText().toString());

            if (imageUri != null) {
                //In this, we wil use Firebase Storage to upload image
                dialog.setMessage("Uploading...");
                dialog.show();

                String unique_name = UUID.randomUUID().toString();
                StorageReference imageFolder = storageReference.child("images/" + unique_name);

                imageFolder.putFile(imageUri)
                        .addOnFailureListener(e -> {
                            dialog.dismiss();
                            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }).addOnCompleteListener(task -> {
                    dialog.dismiss();
                    imageFolder.getDownloadUrl().addOnSuccessListener(uri -> {
                        updateData.put("image", uri.toString());
                        updateBestDeals(updateData);
                    });
                }).addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                    dialog.setMessage(new StringBuilder("Uploading: ").append(progress).append("%"));

                });
            } else {
                updateBestDeals(updateData);
            }
        });

        builder.setView(itemView);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateBestDeals(Map<String, Object> updateData) {
        FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentServerUser.getRestaurant())
                .child(Common.BEST_DEALS)
                .child(Common.bestDealsSelected.getKey())
                .updateChildren(updateData)
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> {
            bestDealsViewModel.loadBestDeals();
            EventBus.getDefault().postSticky(new ToastEvent(Common.ACTION.UPDATE, true));
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {

            if (data != null && data.getData() != null) {
                imageUri = data.getData();
                img_best_deals.setImageURI(imageUri);
            }
        }
    }

}