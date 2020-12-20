package com.nta.lc_server.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.nta.lc_server.R;
import com.nta.lc_server.common.Common;
import com.nta.lc_server.eventbus.CategoryClick;
import com.nta.lc_server.eventbus.ChangeMenuClick;
import com.nta.lc_server.eventbus.ToastEvent;
import com.nta.lc_server.model.FCMSendData;
import com.nta.lc_server.remote.IFCMService;
import com.nta.lc_server.remote.RetrofitFCMClient;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int PICK_IMAGE_REQUEST = 1234;
    private AppBarConfiguration mAppBarConfiguration;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private NavController navController;
    private int menuClick = -1;

    private ImageView img_upload;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private IFCMService ifcmService;
    private Uri imgUri = null;

    private FirebaseStorage storage;
    private StorageReference storageReference;

    private AlertDialog dialog;

    @OnClick(R.id.fab_chat)
    void onOpenChatList() {
        startActivity(new Intent(this, ChatListActivity.class));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);

        init();

        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_category,
                R.id.nav_food_list,
                R.id.nav_order
        )
                .setOpenableLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.bringToFront();

        View headerView = navigationView.getHeaderView(0);
        TextView txt_user = headerView.findViewById(R.id.txt_user);
        Common.setSpanString("Chào, ", Common.currentServerUser.getName(), txt_user);

        menuClick = R.id.nav_category; //Default

        checkIsOpenFromActivity();
    }

    private void init() {
        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        subscribeToTopic(Common.createTopicOrder());
        updateToken();

        dialog = new AlertDialog.Builder(this).setCancelable(false)
                .setMessage("Vui lòng đợi...")
                .create();
    }

    private void checkIsOpenFromActivity() {
        boolean isOpenFromNewOrder = getIntent().getBooleanExtra(Common.IS_OPEN_ACTIVITY_NEW_ORDER, false);
        if (isOpenFromNewOrder) {
            navController.popBackStack();
            navController.navigate(R.id.nav_order);
            menuClick = R.id.nav_order;
        }
    }

    private void updateToken() {
        FirebaseInstanceId.getInstance()
                .getInstanceId()
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnSuccessListener(instanceIdResult -> {
                    Common.updateToken(HomeActivity.this, instanceIdResult.getToken(),
                            true,
                            false);

                    Log.d("MYTOKEN", instanceIdResult.getToken());
                });


    }

    private void subscribeToTopic(String topicOrder) {
        FirebaseMessaging.getInstance()
                .subscribeToTopic(topicOrder)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful())
                        Toast.makeText(this, "Thất bại: " + task.isSuccessful(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().removeAllStickyEvents(); // Fix event bus always called after onActivityResult
        EventBus.getDefault().unregister(this);
        compositeDisposable.clear();
        super.onStop();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onCategoryClick(CategoryClick event) {
        if (event.isSuccess()) {
            if (menuClick != R.id.nav_food_list) {
                navController.navigate(R.id.nav_food_list);
                menuClick = R.id.nav_food_list;
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onToastEvent(ToastEvent event) {
        if (event.getAction() == Common.ACTION.CREATE) {
            Toast.makeText(this, "Thêm thành công!", Toast.LENGTH_SHORT).show();

        } else if (event.getAction() == Common.ACTION.UPDATE) {
            Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(this, "Đã xoá!", Toast.LENGTH_SHORT).show();
        }

        EventBus.getDefault().postSticky(new ChangeMenuClick(event.isFromFoodList()));
    }

//    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
//    public void onChangeMenuClick(ChangeMenuClick event) {
//        if (event.isFromFoodList()) {
//            //Clear
//            navController.popBackStack(R.id.nav_category, true);
//            navController.navigate(R.id.nav_category);
//        } else {
//            //Clear
//            navController.popBackStack(R.id.nav_food_list, true);
//            navController.navigate(R.id.nav_food_list);
//        }
//        menuClick = -1;
//    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onChangeMenuClick(ChangeMenuClick event) {
        if (!event.isFromFoodList()) {
            //Clear
            navController.popBackStack(R.id.nav_category, true);
            navController.navigate(R.id.nav_category);
        }
        menuClick = -1;
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        menuItem.setChecked(true);
        drawer.closeDrawers();
        switch (menuItem.getItemId()) {
            case R.id.nav_category:
                if (menuItem.getItemId() != menuClick) {
                    navController.popBackStack(); //Remove all back stack
                    navController.navigate(R.id.nav_category);
                }
                break;

            case R.id.nav_order:
                if (menuItem.getItemId() != menuClick) {
                    navController.popBackStack(); //Remove all back stack
                    navController.navigate(R.id.nav_order);
                }
                break;

            case R.id.nav_best_deals:
                if (menuItem.getItemId() != menuClick) {
                    navController.popBackStack(); //Remove all back stack
                    navController.navigate(R.id.nav_best_deals);
                }
                break;

            case R.id.nav_most_popular:
                if (menuItem.getItemId() != menuClick) {
                    navController.popBackStack(); //Remove all back stack
                    navController.navigate(R.id.nav_most_popular);
                }
                break;

            case R.id.nav_discount:
                if (menuItem.getItemId() != menuClick) {
                    navController.popBackStack(); //Remove all back stack
                    navController.navigate(R.id.nav_discount);
                }
                break;

            case R.id.nav_send_news:
                showNewsDialog();
                break;

            case R.id.nav_sign_out:
                signOut();
                break;

            default:
                menuClick = -1;
                break;
        }

        menuClick = menuItem.getItemId();
        return true;
    }

    private void showNewsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Hệ thống thông báo");
        builder.setMessage("Gửi thông báo mới tới toàn bộ người dùng");

        View itemView = LayoutInflater.from(this).inflate(R.layout.layout_news_system, null);

        //Views
        EditText edt_title = itemView.findViewById(R.id.edt_title);
        EditText edt_content = itemView.findViewById(R.id.edt_content);
        EditText edt_link = itemView.findViewById(R.id.edt_link);

        img_upload = itemView.findViewById(R.id.img_upload);
        RadioButton rdi_none = itemView.findViewById(R.id.rdi_none);
        RadioButton rdi_link = itemView.findViewById(R.id.rdi_link);
        RadioButton rdi_upload = itemView.findViewById(R.id.rdi_image);

        //Event
        rdi_none.setOnClickListener(view -> {
            edt_link.setVisibility(View.GONE);
            img_upload.setVisibility(View.GONE);
        });

        rdi_link.setOnClickListener(view -> {
            edt_link.setVisibility(View.VISIBLE);
            img_upload.setVisibility(View.GONE);
        });

        rdi_upload.setOnClickListener(view -> {
            edt_link.setVisibility(View.GONE);
            img_upload.setVisibility(View.VISIBLE);
        });

        img_upload.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        });

        builder.setView(itemView);
        builder.setNegativeButton("Đóng", (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton("Gửi", (dialogInterface, i) -> {
                    if (rdi_none.isChecked()) {
                        sendNews(edt_title.getText().toString(), edt_content.getText().toString());
                    } else if (rdi_link.isChecked()) {
                        sendNews(edt_title.getText().toString(), edt_content.getText().toString(), edt_link.getText().toString());
                    } else if (rdi_upload.isChecked()) {
                        if (imgUri != null) {
                            AlertDialog dialog = new AlertDialog.Builder(this).setMessage("Uploading...").create();
                            dialog.show();

                            String file_name = UUID.randomUUID().toString();
                            StorageReference newsImage = storageReference.child("news/" + file_name);
                            newsImage.putFile(imgUri)
                                    .addOnFailureListener(e -> {
                                        dialog.dismiss();
                                        Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();

                                    })
                                    .addOnSuccessListener(taskSnapshot -> {
                                        dialog.dismiss();
                                        newsImage.getDownloadUrl().addOnSuccessListener(uri -> {
                                            sendNews(edt_title.getText().toString(),
                                                    edt_content.getText().toString(),
                                                    uri.toString());
                                        });

                                    }).addOnProgressListener(taskSnapshot -> {
                                double progress = Math.round((100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount()));
                                dialog.setMessage(new StringBuilder("Uploading: ").append(progress).append("%"));
                            });
                        }
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void sendNews(String title, String content, String url) {
        Map<String, String> notificationData = new HashMap<>();
        notificationData.put(Common.NOTI_TITLE, title);
        notificationData.put(Common.NOTI_CONTENT, content);
        notificationData.put(Common.IS_SEND_IMAGE, "true");
        notificationData.put(Common.IMAGE_URL, url);

        FCMSendData fcmSendData = new FCMSendData(Common.getNewsTopic(), notificationData);

        AlertDialog dialog = new AlertDialog.Builder(this).setMessage("Vui lòng đợi...").create();
        dialog.show();

        compositeDisposable.add(ifcmService.sendNotification(fcmSendData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fcmResponse -> {
                    dialog.dismiss();
                    if (fcmResponse.getMessage_id() != 0)
                        Toast.makeText(this, "Đã gửi thông báo!", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(this, "Gửi thông báo thất bại!", Toast.LENGTH_SHORT).show();

                }, throwable -> {
                    dialog.dismiss();
                    Toast.makeText(this, "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }));
    }

    private void sendNews(String title, String content) {
        Map<String, String> notificationData = new HashMap<>();
        notificationData.put(Common.NOTI_TITLE, title);
        notificationData.put(Common.NOTI_CONTENT, content);
        notificationData.put(Common.IS_SEND_IMAGE, "false");

        FCMSendData fcmSendData = new FCMSendData(Common.getNewsTopic(), notificationData);

        AlertDialog dialog = new AlertDialog.Builder(this).setMessage("Vui lòng đợi...").create();
        dialog.show();

        compositeDisposable.add(ifcmService.sendNotification(fcmSendData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fcmResponse -> {
                    dialog.dismiss();
                    if (fcmResponse.getMessage_id() != 0)
                        Toast.makeText(this, "Đã gửi thông báo!", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(this, "Gửi thông báo thất bại!", Toast.LENGTH_SHORT).show();

                }, throwable -> {
                    dialog.dismiss();
                    Toast.makeText(this, "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }));
    }

    private void signOut() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Đăng xuất")
                .setMessage("Bạn có muốn đăng xuất?")
                .setNegativeButton("Đóng", (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton("Đăng xuất", (dialogInterface, i) -> {
                    Common.selectedFood = null;
                    Common.categorySelected = null;
                    Common.currentServerUser = null;
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {

            if (data != null && data.getData() != null) {
                imgUri = data.getData();
                img_upload.setImageURI(imgUri);
            }
        }
    }
}