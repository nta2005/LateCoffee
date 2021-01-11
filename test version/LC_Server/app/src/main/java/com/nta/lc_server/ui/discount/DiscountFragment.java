package com.nta.lc_server.ui.discount;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
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

import com.google.firebase.database.FirebaseDatabase;
import com.nta.lc_server.R;
import com.nta.lc_server.adapter.MyDiscountAdapter;
import com.nta.lc_server.common.Common;
import com.nta.lc_server.common.MySwipeHelper;
import com.nta.lc_server.eventbus.ToastEvent;
import com.nta.lc_server.model.DiscountModel;

import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;

public class DiscountFragment extends Fragment {

    @BindView(R.id.recycler_discount)
    RecyclerView recycler_discount;
    AlertDialog dialog;
    LayoutAnimationController layoutAnimationController;
    MyDiscountAdapter adapter;
    List<DiscountModel> discountModelList;
    private DiscountViewModel discountViewModel;
    private Unbinder unbinder;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        discountViewModel = new ViewModelProvider(this).get(DiscountViewModel.class);
        View root = inflater.inflate(R.layout.fragment_discount, container, false);
        unbinder = ButterKnife.bind(this, root);
        initViews();
        discountViewModel.getMessageError().observe(getViewLifecycleOwner(), s -> {
            Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        discountViewModel.getDiscountMutableLiveData().observe(getViewLifecycleOwner(), list -> {
            dialog.dismiss();
            if (list == null)
                discountModelList = new ArrayList<>();
            else
                discountModelList = list;
            adapter = new MyDiscountAdapter(getContext(), discountModelList);
            recycler_discount.setAdapter(adapter);
            recycler_discount.setLayoutAnimation(layoutAnimationController);
        });
        return root;
    }

    private void initViews() {
        dialog = new SpotsDialog.Builder().setContext(getContext()).setCancelable(false).build();
        setHasOptionsMenu(true);

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_item_from_left);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_discount.setLayoutManager(layoutManager);
        recycler_discount.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));

        MySwipeHelper mySwipeHelper = new MySwipeHelper(getContext(), recycler_discount, 200) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {
                buf.add(new MyButton(getContext(), "Delete", 35, 0, Color.parseColor("#333639"),
                        pos -> {
                            Common.discountSelected = discountModelList.get(pos);
                            showDeleteDialog();
                        }));

                buf.add(new MyButton(getContext(), "Update", 35, 0, Color.parseColor("#414243"),
                        pos -> {
                            Common.discountSelected = discountModelList.get(pos);
                            showUpdateDialog();
                        }));
            }
        };
    }

    private void showAddDialog() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Calendar selectedDate = Calendar.getInstance();
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Create");
        builder.setMessage("Please fill information");

        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.layout_update_discount, null);
        EditText edt_code = itemView.findViewById(R.id.edt_code);
        EditText edt_percent = itemView.findViewById(R.id.edt_percent);
        EditText edt_valid = itemView.findViewById(R.id.edt_valid);
        ImageView img_calendar = itemView.findViewById(R.id.pickDate);

        //Create don't have set default data

        //Event
        DatePickerDialog.OnDateSetListener listener = ((view, year, month, dayOfMonth) -> {
            selectedDate.set(Calendar.YEAR, year);
            selectedDate.set(Calendar.MONTH, month);
            selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            edt_valid.setText(simpleDateFormat.format(selectedDate.getTime()));
        });

        img_calendar.setOnClickListener(view -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(getContext(), listener, calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH))
                    .show(); //Don't forget it
        });

        builder.setNegativeButton("Cancel", ((dialogInterface, i) -> dialogInterface.dismiss()))
                .setPositiveButton("Create", ((dialogInterface, i) -> {

                    DiscountModel discountModel = new DiscountModel();
                    discountModel.setKey(edt_code.getText().toString().toLowerCase()); //fix v103
                    discountModel.setPercent(Integer.parseInt(edt_percent.getText().toString()));
                    discountModel.setUntilDate(selectedDate.getTimeInMillis());

                    createDiscount(discountModel);

                }));

        builder.setView(itemView);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void createDiscount(DiscountModel discountModel) {
        FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentServerUser.getRestaurant())
                .child(Common.DISCOUNT_REF)
                .child(discountModel.getKey())
               .setValue(discountModel)
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        discountViewModel.loadDiscount();
                        adapter.notifyDataSetChanged();
                        EventBus.getDefault().postSticky(new ToastEvent(Common.ACTION.CREATE, true));
                    }
                });
    }

    private void showUpdateDialog() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Calendar selectedDate = Calendar.getInstance();
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Update");
        builder.setMessage("Please fill information");

        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.layout_update_discount, null);
        EditText edt_code = itemView.findViewById(R.id.edt_code);
        EditText edt_percent = itemView.findViewById(R.id.edt_percent);
        EditText edt_valid = itemView.findViewById(R.id.edt_valid);
        ImageView img_calendar = itemView.findViewById(R.id.pickDate);

        //Set data
        edt_code.setText(Common.discountSelected.getKey());
        edt_code.setEnabled(false); //Lock key

        edt_percent.setText(new StringBuilder().append(Common.discountSelected.getPercent()));
        edt_valid.setText(simpleDateFormat.format(Common.discountSelected.getUntilDate()));

        //Event
        DatePickerDialog.OnDateSetListener listener = ((view, year, month, dayOfMonth) -> {
            selectedDate.set(Calendar.YEAR, year);
            selectedDate.set(Calendar.MONTH, month);
            selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            edt_valid.setText(simpleDateFormat.format(selectedDate.getTime()));
        });

        img_calendar.setOnClickListener(view -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(getContext(), listener, calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH))
                    .show(); //Don't forget it
        });

        builder.setNegativeButton("Cancel", ((dialogInterface, i) -> dialogInterface.dismiss()))
                .setPositiveButton("Update", ((dialogInterface, i) -> {

                    Map<String,Object> updateData = new HashMap<>();
                    updateData.put("percent",Integer.parseInt(edt_percent.getText().toString()));
                    updateData.put("untilDate",selectedDate.getTimeInMillis());

                    updateDiscount(updateData);

                }));

        builder.setView(itemView);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateDiscount(Map<String, Object> updateData) {
        FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentServerUser.getRestaurant())
                .child(Common.DISCOUNT_REF)
                .child(Common.discountSelected.getKey())
                .updateChildren(updateData)
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        discountViewModel.loadDiscount();
                        adapter.notifyDataSetChanged();
                        EventBus.getDefault().postSticky(new ToastEvent(Common.ACTION.UPDATE, true));
                    }
                });
    }

    private void showDeleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Delete");
        builder.setMessage("Do you really want to delete this item?");

        builder.setNegativeButton("Cancel", ((dialogInterface, i) -> dialogInterface.dismiss()))
                .setPositiveButton("Delete", ((dialogInterface, i) -> {

                    deleteDiscount();

                }));

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteDiscount() {
        FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentServerUser.getRestaurant())
                .child(Common.DISCOUNT_REF)
                .child(Common.discountSelected.getKey())
                .removeValue()
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        discountViewModel.loadDiscount();
                        adapter.notifyDataSetChanged();
                        EventBus.getDefault().postSticky(new ToastEvent(Common.ACTION.DELETE, true));
                    }
                });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.discount_menu,menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_create)
        showAddDialog();
        return super.onOptionsItemSelected(item);
    }
}