package com.nta.latecoffee.activity;

import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.andremion.counterfab.CounterFab;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.nta.latecoffee.R;
import com.nta.latecoffee.common.Common;
import com.nta.latecoffee.database.CartDataSource;
import com.nta.latecoffee.database.CartDatabase;
import com.nta.latecoffee.database.LocalCartDataSource;
import com.nta.latecoffee.eventbus.BestDealItemClick;
import com.nta.latecoffee.eventbus.CategoryClick;
import com.nta.latecoffee.eventbus.CounterCartEvent;
import com.nta.latecoffee.eventbus.FoodItemClick;
import com.nta.latecoffee.eventbus.HideFABCart;
import com.nta.latecoffee.eventbus.MenuInflateEvent;
import com.nta.latecoffee.eventbus.MenuItemBack;
import com.nta.latecoffee.eventbus.MenuItemEvent;
import com.nta.latecoffee.eventbus.PopularCategoryClick;
import com.nta.latecoffee.model.CategoryModel;
import com.nta.latecoffee.model.FoodModel;
import com.nta.latecoffee.remote.ICloudFunctions;
import com.nta.latecoffee.remote.RetrofitICloudClient;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dmax.dialog.SpotsDialog;
import io.paperdb.Paper;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    android.app.AlertDialog dialog;
    int menuClickId = -1;

    @BindView(R.id.fab)
    CounterFab fab;
    @BindView(R.id.fab_chat)
    CounterFab fab_chat;

    @OnClick(R.id.fab_chat)
    void onFabChatClick(){
        startActivity(new Intent(this,ChatActivity.class));
    }

    private Place placeSelected;
    private AutocompleteSupportFragment places_fragment;
    private PlacesClient placesClient;
    private List<Place.Field> placeFields = Arrays.asList(Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG);
    private AppBarConfiguration mAppBarConfiguration;
    private DrawerLayout drawer;
    private NavController navController;
    private CartDataSource cartDataSource;
    private NavigationView navigationView;
    private ICloudFunctions cloudFunctions;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onResume() {
        super.onResume();
        //countCartItem();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        checkGPS();

        initPlaceClient();

        dialog = new SpotsDialog.Builder().setContext(this).setCancelable(false).build();

        ButterKnife.bind(this);

        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(this).cartDAO());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> navController.navigate(R.id.nav_cart));
        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_restaurant,
                R.id.nav_home,
                R.id.nav_menu,
                R.id.nav_food_detail,
                R.id.nav_view_orders,
                R.id.nav_cart,
                R.id.nav_food_list)
                .setOpenableLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.bringToFront(); //Fixed

        View headerView = navigationView.getHeaderView(0);
        TextView txt_user = headerView.findViewById(R.id.txt_user);
        Common.setSpanString("Hey, ", Common.currentUser.getName(), txt_user);
        //menuClickId = R.id.nav_home;

        //Hide fab because in Restaurant list, we can't show cart
        EventBus.getDefault().postSticky(new HideFABCart(true));

    }

    private void checkGPS() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGPSAlert();
        }
    }

    private void showGPSAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Bạn hãy bật GPS để sử dụng app")
                .setCancelable(false)
                .setPositiveButton("Bật GPS", (dialog, id) -> {
                    Intent callGPSSettingIntent = new Intent(
                            android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(callGPSSettingIntent);
                });
        builder.setNegativeButton("Thoát", (dialog, id) -> {
            dialog.cancel();
            //finish();
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void initPlaceClient() {
        Places.initialize(this, getString(R.string.google_maps_key));
        placesClient = Places.createClient(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onNavigationItemSelected(@androidx.annotation.NonNull MenuItem menuItem) {
        menuItem.setChecked(true);
        drawer.closeDrawers();
        switch (menuItem.getItemId()) {

            case R.id.nav_restaurant:
                if (menuItem.getItemId() != menuClickId)
                    navController.navigate(R.id.nav_restaurant);
                break;

            case R.id.nav_home:
                if (menuItem.getItemId() != menuClickId) {
                    //Display detail menu of restaurant
                    navController.navigate(R.id.nav_home);
                    EventBus.getDefault().postSticky(new MenuInflateEvent(true));
                }
                break;

            case R.id.nav_menu:
                if (menuItem.getItemId() != menuClickId) {
                    navController.navigate(R.id.nav_menu);
                    EventBus.getDefault().postSticky(new MenuInflateEvent(true));
                }
                break;

            case R.id.nav_cart:
                if (menuItem.getItemId() != menuClickId) {
                    navController.navigate(R.id.nav_cart);
                    EventBus.getDefault().postSticky(new MenuInflateEvent(true));
                }
                break;

            case R.id.nav_view_orders:
                if (menuItem.getItemId() != menuClickId) {
                    navController.navigate(R.id.nav_view_orders);
                    EventBus.getDefault().postSticky(new MenuInflateEvent(true));
                }
                break;

            case R.id.nav_sign_out:
                signOut();
                break;

            case R.id.nav_news:
                showSubscribeNews();
                break;

            case R.id.nav_update_info:
                showUpdateInfoDialog();
                //showUpdateInfoDialogWithPlaces(); //use when app billing
                break;
        }
        menuClickId = menuItem.getItemId();
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().removeAllStickyEvents();
        EventBus.getDefault().unregister(this);
        compositeDisposable.clear();
        super.onStop();
    }

    //EventBus: receive notify

    //Event when click to Category
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onCategorySelected(CategoryClick event) {
        if (event.isSuccess()) {
            //Toast.makeText(this, "Click to " + event.getCategoryModel().getName(), Toast.LENGTH_SHORT).show();
            navController.navigate(R.id.nav_food_list);
        }
    }

    //Event when click to Food
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onFoodItemClick(FoodItemClick event) {
        if (event.isSuccess()) {
            navController.navigate(R.id.nav_food_detail);
        }
    }

    //Event hide FAB button
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onHideFABEvent(HideFABCart event) {
        if (event.isHidden()) {
            fab.hide();
            fab_chat.hide();
        } else {
            fab.show();
            fab_chat.show();
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onCartCounter(CounterCartEvent event) {
//        if (event.isSuccess()) {
//            countCartItem();
//        }
    }

    //Event click BestDeal item
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onBestDealItemClick(BestDealItemClick event) {
        if (event.getBestDealModel() != null) {
            dialog.show();
            FirebaseDatabase.getInstance()
                    .getReference(Common.RESTAURANT_REF)
                    .child(Common.currentRestaurant.getUid())
                    .child(Common.CATEGORY_REF)
                    .child(event.getBestDealModel().getMenu_id())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@androidx.annotation.NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                Common.categorySelected = dataSnapshot.getValue(CategoryModel.class);
                                Common.categorySelected.setMenu_id(dataSnapshot.getKey());
                                //Load food
                                FirebaseDatabase.getInstance()
                                        .getReference(Common.RESTAURANT_REF)
                                        .child(Common.currentRestaurant.getUid())
                                        .child(Common.CATEGORY_REF)
                                        .child(event.getBestDealModel().getMenu_id())
                                        .child("foods")
                                        .orderByChild("id")
                                        .equalTo(event.getBestDealModel().getFood_id())
                                        .limitToLast(1)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@androidx.annotation.NonNull DataSnapshot dataSnapshot) {
                                                if (dataSnapshot.exists()) {
                                                    for (DataSnapshot itemSnapShot : dataSnapshot.getChildren()) {
                                                        Common.selectedFood = itemSnapShot.getValue(FoodModel.class);
                                                        Common.selectedFood.setKey(itemSnapShot.getKey());
                                                    }
                                                    navController.navigate(R.id.nav_food_detail);
                                                } else {

                                                    Toast.makeText(HomeActivity.this, "Item doesn't exists", Toast.LENGTH_SHORT).show();

                                                }
                                                dialog.dismiss();
                                            }

                                            @Override
                                            public void onCancelled(@androidx.annotation.NonNull DatabaseError databaseError) {
                                                dialog.dismiss();
                                                Toast.makeText(HomeActivity.this, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();

                                            }
                                        });
                            } else {
                                dialog.dismiss();
                                Toast.makeText(HomeActivity.this, "Item doesn't exists", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@androidx.annotation.NonNull DatabaseError databaseError) {
                            dialog.dismiss();
                            Toast.makeText(HomeActivity.this, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    //Event click Popular item
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onPopularItemClick(PopularCategoryClick event) {
        if (event.getPopularCategoryModel() != null) {
            dialog.show();
            FirebaseDatabase.getInstance()
                    .getReference(Common.RESTAURANT_REF)
                    .child(Common.currentRestaurant.getUid())
                    .child(Common.CATEGORY_REF)
                    .child(event.getPopularCategoryModel().getMenu_id())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@androidx.annotation.NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                Common.categorySelected = dataSnapshot.getValue(CategoryModel.class);
                                Common.categorySelected.setMenu_id(dataSnapshot.getKey());
                                //Load food
                                FirebaseDatabase.getInstance()
                                        .getReference(Common.RESTAURANT_REF)
                                        .child(Common.currentRestaurant.getUid())
                                        .child(Common.CATEGORY_REF)
                                        .child(event.getPopularCategoryModel().getMenu_id())
                                        .child("foods")
                                        .orderByChild("id")
                                        .equalTo(event.getPopularCategoryModel().getFood_id())
                                        .limitToLast(1)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@androidx.annotation.NonNull DataSnapshot dataSnapshot) {
                                                if (dataSnapshot.exists()) {
                                                    for (DataSnapshot itemSnapShot : dataSnapshot.getChildren()) {
                                                        Common.selectedFood = itemSnapShot.getValue(FoodModel.class);
                                                        Common.selectedFood.setKey(itemSnapShot.getKey());
                                                    }
                                                    navController.navigate(R.id.nav_food_detail);
                                                } else {

                                                    Toast.makeText(HomeActivity.this, "Item doesn't exists", Toast.LENGTH_SHORT).show();

                                                }
                                                dialog.dismiss();
                                            }

                                            @Override
                                            public void onCancelled(@androidx.annotation.NonNull DatabaseError databaseError) {
                                                dialog.dismiss();
                                                Toast.makeText(HomeActivity.this, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();

                                            }
                                        });
                            } else {
                                dialog.dismiss();
                                Toast.makeText(HomeActivity.this, "Item doesn't exists", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@androidx.annotation.NonNull DatabaseError databaseError) {
                            dialog.dismiss();
                            Toast.makeText(HomeActivity.this, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    //Count Cart item
    private void countCartItem() {

        cartDataSource.countItemInCart(Common.currentUser.getUid(),
                Common.currentRestaurant.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Integer>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onSuccess(@NonNull Integer integer) {
                        fab.setCount(integer);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        if (!e.getMessage().contains("Query returned empty")) {
                            Toast.makeText(HomeActivity.this, "[COUNT CART]" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        } else
                            fab.setCount(0);
                    }
                });
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void countCartAgain(CounterCartEvent event) {
        if (event.isSuccess()) {
            if (Common.currentRestaurant != null)
                countCartItem();
        }

    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMenuItemBack(MenuItemBack event) {
        menuClickId = -1;
        if (getSupportFragmentManager().getBackStackEntryCount() > 0)
            getSupportFragmentManager().popBackStack();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onRestaurantClick(MenuItemEvent event) {

        cloudFunctions = RetrofitICloudClient.getInstance().create(ICloudFunctions.class);
        //cloudFunctions = RetrofitICloudClient.getInstance(event.getRestaurantModel().getPaymentUrl()).create(ICloudFunctions.class);

        dialog.show();
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", Common.buildToken(Common.authorizeKey));
        compositeDisposable.add(cloudFunctions.getToken(headers)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(braintreeToken -> {

                    dialog.dismiss();
                    Common.currentToken = braintreeToken.getToken();

                    Bundle bundle = new Bundle();
                    bundle.putString("restaurant", event.getRestaurantModel().getUid());
                    navController.navigate(R.id.nav_home, bundle);
                    EventBus.getDefault().postSticky(new MenuInflateEvent(true));
                    EventBus.getDefault().postSticky(new HideFABCart(false)); // Show CART button when user click select restaurant
                    countCartItem();

                }, throwable -> {
                    dialog.dismiss();
                    Toast.makeText(this, "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }));
    }


    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onInflateMenu(MenuInflateEvent event) {

        navigationView.getMenu().clear();
        if (event.isShowDetail())
            navigationView.inflateMenu(R.menu.restaurant_detail_menu);
        else
            navigationView.inflateMenu(R.menu.activity_main_drawer);
    }


    private void showSubscribeNews() {
        Paper.init(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("News System");
        builder.setMessage("Do you want to subscribe news from our restaurant");

        View itemView = LayoutInflater.from(this).inflate(R.layout.layout_subscribe_news, null);
        CheckBox ckb_news = itemView.findViewById(R.id.ckb_subscribe_news);
        boolean isSubscribeNews = Paper.book().read(Common.currentRestaurant.getUid(), false);
        if (isSubscribeNews)
            ckb_news.setChecked(true);
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> {
            dialogInterface.dismiss();
        })
                .setPositiveButton("Send", (dialogInterface, i) -> {
                    if (ckb_news.isChecked()) {
                        Toast.makeText(this, "Topic: "+Common.createTopicNews(), Toast.LENGTH_SHORT).show();
                        Paper.book().write(Common.currentRestaurant.getUid(), true);
                        FirebaseMessaging.getInstance()
                                .subscribeToTopic(Common.createTopicNews())
                                .addOnFailureListener(e -> Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Subscribe successfully!", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Paper.book().delete(Common.currentRestaurant.getUid());
                        FirebaseMessaging.getInstance()
                                .unsubscribeFromTopic(Common.createTopicNews())
                                .addOnFailureListener(e -> Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Unsubscribe successfully!", Toast.LENGTH_SHORT).show();
                                });
                    }
                });
        builder.setView(itemView);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //Update info
    private void showUpdateInfoDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Cập nhật thông tin");
        builder.setMessage("Điền thông tin mới bên dưới");

        View itemView = LayoutInflater.from(this).inflate(R.layout.layout_register, null);

        EditText edt_name = itemView.findViewById(R.id.edt_name);
        EditText edt_address = itemView.findViewById(R.id.edt_address);
        EditText edt_phone = itemView.findViewById(R.id.edt_phone);

        //Set data
        edt_name.setText(Common.currentUser.getName());
        edt_address.setText(Common.currentUser.getAddress());
        edt_phone.setText(Common.currentUser.getPhone());

        builder.setView(itemView);
        builder.setNegativeButton("Đóng", (dialogInterface, i) -> {
            dialogInterface.dismiss();
        });
        builder.setPositiveButton("Cập nhật", (dialogInterface, i) -> {

            if (TextUtils.isEmpty(edt_name.getText().toString())) {
                Toast.makeText(this, "Bạn chưa nhập tên!", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> update_data = new HashMap<>();
            update_data.put("name", edt_name.getText().toString());
            update_data.put("address", edt_address.getText().toString());

            FirebaseDatabase.getInstance()
                    .getReference(Common.USER_REFERENCES)
                    .child(Common.currentUser.getUid())
                    .updateChildren(update_data)
                    .addOnFailureListener(e -> {
                        dialogInterface.dismiss();
                        Toast.makeText(HomeActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    })
                    .addOnSuccessListener(aVoid -> {
                        dialogInterface.dismiss();
                        Toast.makeText(HomeActivity.this, "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
                        Common.currentUser.setName(update_data.get("name").toString());
                        Common.currentUser.setAddress(update_data.get("address").toString());
                    });
        });

        builder.setView(itemView);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    //Update info with places fragment
    private void showUpdateInfoDialogWithPlaces() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cập nhật thông tin");
        builder.setMessage("Điền thông tin mới bên dưới");

        View itemView = LayoutInflater.from(this).inflate(R.layout.layout_register_with_places, null);

        EditText edt_name = itemView.findViewById(R.id.edt_name);
        TextView txt_address_detail = itemView.findViewById(R.id.txt_address_detail);
        EditText edt_phone = itemView.findViewById(R.id.edt_phone);

        places_fragment = (AutocompleteSupportFragment) getSupportFragmentManager()
                .findFragmentById(R.id.places_autocomplete_fragment);
        places_fragment.setPlaceFields(placeFields);
        places_fragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@androidx.annotation.NonNull Place place) {
                placeSelected = place;
                txt_address_detail.setText(place.getAddress());
            }

            @Override
            public void onError(@androidx.annotation.NonNull Status status) {
                Toast.makeText(HomeActivity.this, "" + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        //Set data
        edt_name.setText(Common.currentUser.getName());
        txt_address_detail.setText(Common.currentUser.getAddress());
        edt_phone.setText(Common.currentUser.getPhone());

        builder.setView(itemView);
        builder.setNegativeButton("Đóng", (dialogInterface, i) -> {
            dialogInterface.dismiss();
        });
        builder.setPositiveButton("Cập nhật", (dialogInterface, i) -> {
            if (placeSelected != null) {
                if (TextUtils.isEmpty(edt_name.getText().toString())) {
                    Toast.makeText(this, "Bạn chưa nhập tên!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> update_data = new HashMap<>();
                update_data.put("name", edt_name.getText().toString());
                update_data.put("address", txt_address_detail.getText().toString());
                update_data.put("lat", placeSelected.getLatLng().latitude);
                update_data.put("lng", placeSelected.getLatLng().longitude);

                FirebaseDatabase.getInstance()
                        .getReference(Common.USER_REFERENCES)
                        .child(Common.currentUser.getUid())
                        .updateChildren(update_data)
                        .addOnFailureListener(e -> {
                            dialogInterface.dismiss();
                            Toast.makeText(HomeActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        })
                        .addOnSuccessListener(aVoid -> {
                            dialogInterface.dismiss();
                            Toast.makeText(HomeActivity.this, "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
                            Common.currentUser.setName(update_data.get("name").toString());
                            Common.currentUser.setAddress(update_data.get("address").toString());
                            Common.currentUser.setLat(Double.parseDouble(update_data.get("lat").toString()));
                            Common.currentUser.setLng(Double.parseDouble(update_data.get("lng").toString()));

                        });
            } else {
                Toast.makeText(this, "Bạn chưa chọn địa chỉ!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setView(itemView);

        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(dialogInterface -> {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.remove(places_fragment);
            fragmentTransaction.commit();
        });
        dialog.show();
    }

    //Sign Out
    private void signOut() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_signout)
                .setMessage(R.string.msg_signout)
                .setNegativeButton(R.string.signout_cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton(R.string.signout_ok, (dialogInterface, i) -> {
                    Common.selectedFood = null;
                    Common.categorySelected = null;
                    Common.currentUser = null;
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}