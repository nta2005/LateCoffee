package com.nta.latecoffee.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.nta.latecoffee.R;
import com.nta.latecoffee.common.Common;
import com.nta.latecoffee.model.UserModel;

import java.util.Arrays;
import java.util.List;

import dmax.dialog.SpotsDialog;
import io.reactivex.disposables.CompositeDisposable;

public class MainActivity extends AppCompatActivity {

    private static final int APP_REQUEST_CODE = 7171;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final List<Place.Field> placeFields = Arrays.asList(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG);
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;
    private AlertDialog dialog;
    private DatabaseReference userRef;
    private List<AuthUI.IdpConfig> providers;
    private Place placeSelected;
    private AutocompleteSupportFragment places_fragment;
    private PlacesClient placesClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    //Ánh xạ
    private void init() {
        Places.initialize(this, getString(R.string.google_maps_key));
        placesClient = Places.createClient(this);

        providers = Arrays.asList(new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.EmailBuilder().build());


        userRef = FirebaseDatabase.getInstance().getReference(Common.USER_REFERENCES);
        firebaseAuth = FirebaseAuth.getInstance();
        dialog = new SpotsDialog.Builder().setCancelable(false).setContext(this).setMessage("Đang tải...").build();
        listener = firebaseAuth -> {
            //Yêu cầu cấp quyền khi mở app
            Dexter.withContext(this)
                    .withPermissions(
                            Arrays.asList(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.CAMERA)
                    )
                    .withListener(new MultiplePermissionsListener() {
                        @Override
                        public void onPermissionsChecked(MultiplePermissionsReport report) {
                            if (report.areAllPermissionsGranted()) {
                                FirebaseUser user = firebaseAuth.getCurrentUser();
                                if (user != null) {
                                    checkUserFromFirebase(user);
                                } else {
                                    phoneLogin();
                                }
                            } else
                                Toast.makeText(MainActivity.this, "Bạn cần phải cấp quyền để dùng app!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                            //Nothing
                        }
                    }).check();
        };
    }

    //Check user from Firebase
    private void checkUserFromFirebase(FirebaseUser user) {
        dialog.show();
        userRef.child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            //Check User
                            dialog.dismiss();
                            UserModel userModel = dataSnapshot.getValue(UserModel.class);
                            goToHomeActivity(userModel);
                        } else {
                            //showRegisterDialog(user);
                            showRegisterDialogWithPlaces(user); //use when app billing
                            dialog.dismiss();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    //Show Register
    private void showRegisterDialog(FirebaseUser user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_register);
        builder.setMessage(R.string.msg_fill_info);

        View itemView = LayoutInflater.from(this).inflate(R.layout.layout_register, null);

        if (itemView.getParent() != null) {
            ((ViewGroup) itemView.getParent()).removeView(itemView);
        }

        TextInputLayout phone_input_layout = itemView.findViewById(R.id.phone_input_layout);
        EditText edt_name = itemView.findViewById(R.id.edt_name);
        EditText edt_address = itemView.findViewById(R.id.edt_address);
        EditText edt_phone = itemView.findViewById(R.id.edt_phone);

        //Set data
        if (user.getPhoneNumber() == null || TextUtils.isEmpty(user.getPhoneNumber())) {
            phone_input_layout.setHint("Email");
            edt_phone.setText(user.getEmail());
            edt_name.setText(user.getDisplayName());
        } else
            edt_phone.setText(user.getPhoneNumber());

        builder.setView(itemView);
        builder.setNegativeButton(R.string.btn_cancel, (dialogInterface, i) -> {
            dialogInterface.dismiss();
        });
        builder.setPositiveButton("Đăng ký", (dialogInterface, i) -> {

            if (TextUtils.isEmpty(edt_name.getText().toString())) {
                Toast.makeText(MainActivity.this, "Bạn chưa nhập tên!", Toast.LENGTH_SHORT).show();
                showRegisterDialog(user);
                return;
            } else if (TextUtils.isEmpty(edt_address.getText().toString())) {
                Toast.makeText(this, "Bạn chưa nhập địa chỉ!", Toast.LENGTH_SHORT).show();
                showRegisterDialog(user);
                return;
            }
            UserModel userModel = new UserModel();
            userModel.setUid(user.getUid());
            userModel.setName(edt_name.getText().toString());
            userModel.setAddress(edt_address.getText().toString());
            userModel.setPhone(edt_phone.getText().toString());

            userRef.child(user.getUid())
                    .setValue(userModel)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseAuth.getInstance().getCurrentUser()
                                    .getIdToken(true)
                                    .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                                    .addOnCompleteListener(tokenResultTask -> {
                                        Common.authorizeKey = tokenResultTask.getResult().getToken();
                                        dialogInterface.dismiss();
                                        Toast.makeText(MainActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                                        goToHomeActivity(userModel);
                                    });
                        }
                    });
        });

        builder.setView(itemView);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //Show Register use places_fragment (use when billing app)
    private void showRegisterDialogWithPlaces(FirebaseUser user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ĐĂNG KÝ");
        builder.setMessage("Hãy điền thông tin của bạn");

        View itemView = LayoutInflater.from(this).inflate(R.layout.layout_register_with_places, null);

        if (itemView.getParent() != null) {
            ((ViewGroup) itemView.getParent()).removeView(itemView);
        }

        TextInputLayout phone_input_layout = itemView.findViewById(R.id.phone_input_layout);
        EditText edt_name = itemView.findViewById(R.id.edt_name);
        TextView txt_address_detail = itemView.findViewById(R.id.txt_address_detail);
        EditText edt_phone = itemView.findViewById(R.id.edt_phone);

        if (places_fragment == null)
            places_fragment = (AutocompleteSupportFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.places_autocomplete_fragment);
        assert places_fragment != null; //add new 11/01/2021
        places_fragment.setPlaceFields(placeFields);
        places_fragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                placeSelected = place;
                txt_address_detail.setText(place.getAddress());
            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(MainActivity.this, "" + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });


        //Set Data
        if (user.getPhoneNumber() == null || TextUtils.isEmpty(user.getPhoneNumber())) {
            phone_input_layout.setHint("Email");
            edt_phone.setText(user.getEmail());
            edt_name.setText(user.getDisplayName());
        } else
            edt_phone.setText(user.getPhoneNumber());

        builder.setNegativeButton("Đóng", (dialogInterface, i) -> {
            dialogInterface.dismiss();
        });
        builder.setPositiveButton("Đăng ký", (dialogInterface, i) -> {

            if (placeSelected != null) {
                if (TextUtils.isEmpty(edt_name.getText().toString())) {
                    Toast.makeText(MainActivity.this, "Hãy nhập tên của bạn", Toast.LENGTH_SHORT).show();
                    showRegisterDialogWithPlaces(user);
                    return;
                }
                UserModel userModel = new UserModel();
                userModel.setUid(user.getUid());
                userModel.setName(edt_name.getText().toString());
                userModel.setAddress(txt_address_detail.getText().toString());
                userModel.setPhone(edt_phone.getText().toString());
                userModel.setLat(placeSelected.getLatLng().latitude);
                userModel.setLng(placeSelected.getLatLng().longitude);

                userRef.child(user.getUid())
                        .setValue(userModel)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {

                                FirebaseAuth.getInstance().getCurrentUser()
                                        .getIdToken(true)
                                        .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                                        .addOnCompleteListener(tokenResultTask -> {
                                            Common.authorizeKey = tokenResultTask.getResult().getToken();
                                            dialogInterface.dismiss();
                                            Toast.makeText(MainActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                                            goToHomeActivity(userModel);
                                        });


                            }
                        });
            } else {
                Toast.makeText(this, "Bạn chưa nhập địa chỉ", Toast.LENGTH_SHORT).show();
                showRegisterDialog(user);
            }
        });

        builder.setView(itemView);

        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(dialogInterface -> {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.remove(places_fragment);
            transaction.commit();
        });
        dialog.show();
    }

    //Go HomeActivity
    private void goToHomeActivity(UserModel userModel) {
        FirebaseInstanceId.getInstance()
                .getInstanceId()
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();

                    Common.currentUser = userModel; //important, you need always assign value for it before use
                    startActivity(new Intent(MainActivity.this, HomeActivity.class));
                    finish();
                })
                .addOnCompleteListener(task -> {
                    Common.currentUser = userModel; //important, you need always assign value for it before use
                    Common.updateToken(MainActivity.this, task.getResult().getToken());

                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                    intent.putExtra(Common.IS_OPEN_ORDER, getIntent().getBooleanExtra(Common.IS_OPEN_ORDER, false));
                    startActivity(intent);

                    //startActivity(new Intent(MainActivity.this, HomeActivity.class));
                    finish();
                });

    }

    private void phoneLogin() {
        startActivityForResult(AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setLogo(R.drawable.lc_logo)
                        .setTheme(R.style.LoginTheme)
                        .setAvailableProviders(providers).build(),
                APP_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == APP_REQUEST_CODE) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            } else {
                Toast.makeText(this, R.string.signin_error, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(listener);
    }

    @Override
    protected void onStop() {
        if (listener != null) {
            firebaseAuth.removeAuthStateListener(listener);
            compositeDisposable.clear();
        }
        super.onStop();
    }
}