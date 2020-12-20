package com.nta.lc_server.ui.category;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.nta.lc_server.adapter.MyCategoriesAdapter;
import com.nta.lc_server.common.Common;
import com.nta.lc_server.common.MySwipeHelper;
import com.nta.lc_server.eventbus.ToastEvent;
import com.nta.lc_server.model.CategoryModel;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;

public class CategoryFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1234;

    Unbinder unbinder;
    @BindView(R.id.recycler_menu)
    RecyclerView recycler_menu;
    AlertDialog dialog;
    LayoutAnimationController layoutAnimationController;
    MyCategoriesAdapter adapter;

    List<CategoryModel> categoryModels;
    ImageView img_category;

    FirebaseStorage storage;
    StorageReference storageReference;
    MySwipeHelper swipeButton;
    private CategoryViewModel categoryViewModel;
    private Uri imageUri = null;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        View root = inflater.inflate(R.layout.fragment_category, container, false);

        unbinder = ButterKnife.bind(this, root);
        initViews();
        categoryViewModel.getMessageError().observe(getViewLifecycleOwner(), s -> {
            Toast.makeText(getContext(), "" + s, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        categoryViewModel.getCategoryListMutable().observe(getViewLifecycleOwner(), categoryModelList -> {
            dialog.dismiss();
            categoryModels = categoryModelList;
            adapter = new MyCategoriesAdapter(getContext(), categoryModels);
            recycler_menu.setAdapter(adapter);
            recycler_menu.setLayoutAnimation(layoutAnimationController);
        });
        return root;
    }

    private void initViews() {
        setHasOptionsMenu(true);

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        dialog = new SpotsDialog.Builder().setContext(getContext()).setMessage("Đang tải...").setCancelable(false).build();
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_item_from_left);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_menu.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));
        recycler_menu.setLayoutManager(layoutManager);

        swipeButton = new MySwipeHelper(getContext(), recycler_menu, 200) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {

                //Delete
                buf.add(new MyButton(getContext(), "Xoá", 35, 0, Color.parseColor("#FF3C30"),
                        pos -> {
                            Common.categorySelected = categoryModels.get(pos);
                            showDeleteDialog();
                        }));

                //Update
                buf.add(new MyButton(getContext(), "Cập nhật", 35, 0, Color.parseColor("#560027"),
                        pos -> {
                            Common.categorySelected = categoryModels.get(pos);
                            showUpdateDialog();
                        }));
            }
        };
    }

    private void showDeleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("XOÁ VẬT PHẨM");
        builder.setMessage("Bạn có muốn xoá vật phẩm này?");
        builder.setNegativeButton("Đóng", (dialogInterface, i) -> dialogInterface.dismiss());
        builder.setPositiveButton("Xoá", (dialogInterface, i) -> deleteCategory());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteCategory() {
        FirebaseDatabase.getInstance()
                .getReference(Common.CATEGORY_REF)
                .child(Common.categorySelected.getMenu_id())
                .removeValue()
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }).addOnCompleteListener(task -> {
            categoryViewModel.loadCategories();
            EventBus.getDefault().postSticky(new ToastEvent(Common.ACTION.DELETE, true));
        });
    }

    private void showUpdateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("CẬP NHẬT THÔNG TIN");
        builder.setMessage("Hãy điền thông tin bên dưới");

        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.layout_update_category, null);
        EditText edt_category_name = itemView.findViewById(R.id.edt_category_name);
        img_category = itemView.findViewById(R.id.img_category);

        //Set data
        edt_category_name.setText(new StringBuilder("").append(Common.categorySelected.getName()));
        Glide.with(getContext()).load(Common.categorySelected.getImage()).into(img_category);

        //Set event
        img_category.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), PICK_IMAGE_REQUEST);
        });

        builder.setNegativeButton("Đóng", (dialogInterface, i) -> dialogInterface.dismiss());
        builder.setPositiveButton("Cập nhật", (dialogInterface, i) -> {
            Map<String, Object> updateData = new HashMap<>();
            if (edt_category_name.getText().toString().trim().isEmpty()) {
                Toast.makeText(getContext(), "Tên Không được trống", Toast.LENGTH_SHORT).show();
                return;
            } else

                updateData.put("name", edt_category_name.getText().toString().trim());

            if (imageUri != null) {
                //In this, we wil use Firebase Storage to upload image
                dialog.setMessage("Đang tải lên...");
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
                        updateCategory(updateData);
                    });
                }).addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                    dialog.setMessage(new StringBuilder("Đang tải lên: ").append(progress).append("%"));
                });
            } else updateCategory(updateData);

        });

        builder.setView(itemView);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateCategory(Map<String, Object> updateData) {
        FirebaseDatabase.getInstance()
                .getReference(Common.CATEGORY_REF)
                .child(Common.categorySelected.getMenu_id())
                .updateChildren(updateData)
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }).addOnCompleteListener(task -> {
            categoryViewModel.loadCategories();
            EventBus.getDefault().postSticky(new ToastEvent(Common.ACTION.UPDATE, true));
        });
    }

    private void showAddDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("THÊM MỚI");
        builder.setMessage("Hãy điền thông tin bên dưới");

        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.layout_update_category, null);
        EditText edt_category_name = itemView.findViewById(R.id.edt_category_name);
        img_category = itemView.findViewById(R.id.img_category);

        //Set data
        Glide.with(getContext()).load(R.drawable.ic_default).into(img_category);

        //Set event
        img_category.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), PICK_IMAGE_REQUEST);
        });

        builder.setNegativeButton("Đóng", (dialogInterface, i) -> dialogInterface.dismiss());
        builder.setPositiveButton("Thêm", (dialogInterface, i) -> {

            CategoryModel categoryModel = new CategoryModel();

            if (TextUtils.isEmpty(edt_category_name.getText().toString().trim())) {
                Toast.makeText(getContext(), "Bạn chưa nhập tên!", Toast.LENGTH_SHORT).show();
            } else {
                categoryModel.setName(edt_category_name.getText().toString().trim());
                categoryModel.setFoods(new ArrayList<>()); //Create empty list for food list
                if (imageUri == null) {
                    Toast.makeText(getContext(), "Bạn chưa chọn ảnh!", Toast.LENGTH_SHORT).show();
                } else if (imageUri != null) {
                    //In this, we wil use Firebase Storage to upload image
                    dialog.setMessage("Đang tải lên...");
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
                            categoryModel.setImage(uri.toString());
                            addCategory(categoryModel);
                        });
                    }).addOnProgressListener(taskSnapshot -> {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                        dialog.setMessage(new StringBuilder("Đang tải lên: ").append(progress).append("%"));

                    });
                } else
                    addCategory(categoryModel);
            }
        });

        builder.setView(itemView);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void addCategory(CategoryModel categoryModel) {
        FirebaseDatabase.getInstance()
                .getReference(Common.CATEGORY_REF)
                .push()
                .setValue(categoryModel)
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }).addOnCompleteListener(task -> {
            categoryViewModel.loadCategories();
            EventBus.getDefault().postSticky(new ToastEvent(Common.ACTION.CREATE, true));
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                imageUri = data.getData();
                img_category.setImageURI(imageUri);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.action_bar_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_create) {
            showAddDialog();
        }
        return super.onOptionsItemSelected(item);
    }
}