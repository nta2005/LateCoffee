package com.nta.lc_server.activity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.FirebaseDatabase;
import com.nta.lc_server.R;
import com.nta.lc_server.adapter.MyAddonAdapter;
import com.nta.lc_server.adapter.MySizeAdapter;
import com.nta.lc_server.common.Common;
import com.nta.lc_server.eventbus.AddonSizeEditEvent;
import com.nta.lc_server.eventbus.SelectAddonModel;
import com.nta.lc_server.eventbus.SelectSizeModel;
import com.nta.lc_server.eventbus.UpdateAddonModel;
import com.nta.lc_server.eventbus.UpdateSizeModel;
import com.nta.lc_server.model.AddonModel;
import com.nta.lc_server.model.SizeModel;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SizeAddonEditActivity extends AppCompatActivity {

    //Butter Knife
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.edt_name)
    EditText edt_name;
    @BindView(R.id.edt_price)
    EditText edt_price;
    @BindView(R.id.btn_create)
    Button btn_create;
    @BindView(R.id.btn_edit)
    Button btn_edit;
    @BindView(R.id.recycler_addon_size)
    RecyclerView recycler_addon_size;

    //Variable
    MySizeAdapter adapter;
    MyAddonAdapter addonAdapter;
    private int foodEditPosition = -1;
    private boolean needSave = false;
    private boolean isAddon = false;

    //Event
    @OnClick(R.id.btn_create)
    void onCreateNew() {
        if (!isAddon) //Size
        {
            if (adapter != null) {
                if (TextUtils.isEmpty(edt_name.getText().toString().trim())) {
                    errDialog();
                    //Toast.makeText(SizeAddonEditActivity.this, "Bạn chưa nhập tên!", Toast.LENGTH_SHORT).show();
                    return;
                } else if (TextUtils.isEmpty(edt_price.getText().toString().trim())) {
//                    Toast.makeText(SizeAddonEditActivity.this, "Bạn chưa nhập giá!", Toast.LENGTH_SHORT).show();
                    errDialog();
                    return;
                }
                SizeModel sizeModel = new SizeModel();
                sizeModel.setName(edt_name.getText().toString());
                sizeModel.setPrice(Long.valueOf(edt_price.getText().toString()));

                adapter.addNewSize(sizeModel);
            }
        } else //Addon
        {
            if (addonAdapter != null) {
                if (TextUtils.isEmpty(edt_name.getText().toString().trim())) {
                    //Toast.makeText(SizeAddonEditActivity.this, "Bạn chưa nhập tên!", Toast.LENGTH_SHORT).show();
                    errDialog();
                    return;
                } else if (TextUtils.isEmpty(edt_price.getText().toString().trim())) {
                    //  Toast.makeText(SizeAddonEditActivity.this, "Bạn chưa nhập giá!", Toast.LENGTH_SHORT).show();
                    errDialog();
                    return;
                }
                AddonModel addonModel = new AddonModel();
                addonModel.setName(edt_name.getText().toString());
                addonModel.setPrice(Long.valueOf(edt_price.getText().toString()));
                addonAdapter.addNewAddon(addonModel);
            }
        }
    }

    private void errDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thông báo");
        builder.setMessage("Tên và Giá không được trống");
        builder.setPositiveButton("Đồng ý", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @OnClick(R.id.btn_edit)
    void onEdit() {
        if (!isAddon) //Size
        {
            if (adapter != null) {
                SizeModel sizeModel = new SizeModel();
                sizeModel.setName(edt_name.getText().toString());
                sizeModel.setPrice(Long.valueOf(edt_price.getText().toString()));
                adapter.editSize(sizeModel);
            }
        } else //Addon
        {
            if (addonAdapter != null) {
                AddonModel addonModel = new AddonModel();
                addonModel.setName(edt_name.getText().toString());
                addonModel.setPrice(Long.valueOf(edt_price.getText().toString()));
                addonAdapter.editAddon(addonModel);
            }
        }
    }

    //Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.addon_size_menu, menu);
        return true;
    }

    //Menu Options
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {

            //Choose icon Save:
            case R.id.action_save:
                saveData();
                break;

            //If don't save but choose icon back:
            case android.R.id.home:
                if (needSave) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Thoát?")
                            .setMessage("Bạn có muốn thoát mà không lưu?")
                            .setNegativeButton("Đóng", (dialogInterface, i) -> dialogInterface.dismiss())
                            .setPositiveButton("Ok", (dialogInterface, i) -> {
                                needSave = false;
                                closeActivity();
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    closeActivity();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //Save Data
    private void saveData() {
        if (foodEditPosition != -1) {
            Common.categorySelected.getFoods().set(foodEditPosition, Common.selectedFood); //Save food to category

            Map<String, Object> updateData = new HashMap<>();
            updateData.put("foods", Common.categorySelected.getFoods());

            FirebaseDatabase.getInstance()
                    .getReference(Common.CATEGORY_REF)
                    .child(Common.categorySelected.getMenu_id())
                    .updateChildren(updateData)
                    .addOnFailureListener(e -> Toast.makeText(SizeAddonEditActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Reload thành công!", Toast.LENGTH_SHORT).show();
                            needSave = false;
                            edt_price.setText("0");
                            edt_name.setText("");
                        }
                    });
        }
    }

    //Close Activity
    private void closeActivity() {
        edt_name.setText("");
        edt_price.setText("0");
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_size_addon_edit);
        init();
    }

    private void init() {
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        recycler_addon_size.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recycler_addon_size.setLayoutManager(layoutManager);
        recycler_addon_size.addItemDecoration(new DividerItemDecoration(this, layoutManager.getOrientation()));
    }

    //Register Event
    @Override
    protected void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().removeStickyEvent(UpdateSizeModel.class);
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        super.onStop();
    }

    //Receive Event
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onAddonSizeReceive(AddonSizeEditEvent event) {

        if (!event.isAddon()) //if event is size
        {
            if (Common.selectedFood.getSize() == null) //If size is not empty
                Common.selectedFood.setSize(new ArrayList<>());
            adapter = new MySizeAdapter(this, Common.selectedFood.getSize());
            foodEditPosition = event.getPos(); //Save food edit to update
            recycler_addon_size.setAdapter(adapter);

            isAddon = event.isAddon();

            getSupportActionBar().setTitle("Size");

        } else //is addon
        {
            if (Common.selectedFood.getAddon() == null) //If addon is not empty
                Common.selectedFood.setAddon(new ArrayList<>());
            addonAdapter = new MyAddonAdapter(this, Common.selectedFood.getAddon());
            foodEditPosition = event.getPos(); //Save food edit to update
            recycler_addon_size.setAdapter(addonAdapter);

            isAddon = event.isAddon();

            getSupportActionBar().setTitle("Thêm");
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onSizeModelUpdate(UpdateSizeModel event) {
        if (event.getSizeModelList() != null) {
            needSave = true;
            Common.selectedFood.setSize(event.getSizeModelList());
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onAddonModelUpdate(UpdateAddonModel event) {
        if (event.getAddonModelList() != null) {
            needSave = true;
            Common.selectedFood.setAddon(event.getAddonModelList());
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onSelectSizeModel(SelectSizeModel event) {
        if (event.getSizeModel() != null) {
            edt_name.setText(event.getSizeModel().getName());
            edt_price.setText(String.valueOf(event.getSizeModel().getPrice()));
            btn_edit.setEnabled(true);
        } else {
            btn_edit.setEnabled(false);
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onSelectAddonModel(SelectAddonModel event) {
        if (event.getAddonModel() != null) {
            edt_name.setText(event.getAddonModel().getName());
            edt_price.setText(String.valueOf(event.getAddonModel().getPrice()));
            btn_edit.setEnabled(true);
        } else {
            btn_edit.setEnabled(false);
        }
    }
}