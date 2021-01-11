package com.nta.latecoffee.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
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
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.PermissionListener;
import com.nta.latecoffee.R;
import com.nta.latecoffee.common.Common;
import com.nta.latecoffee.model.UserModel;
import com.nta.latecoffee.remote.ICloudFunctions;
import com.nta.latecoffee.remote.RetrofitICloudClient;

import java.util.Arrays;
import java.util.List;

import dmax.dialog.SpotsDialog;
import io.reactivex.disposables.CompositeDisposable;

public class MainActivity extends AppCompatActivity {

    private static int APP_REQUEST_CODE = 7171;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;
    private AlertDialog dialog;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private ICloudFunctions cloudFunctions;
    private DatabaseReference userRef;
    private List<AuthUI.IdpConfig> providers;

    private Place placeSelected;
    private AutocompleteSupportFragment places_fragment;
    private PlacesClient placesClient;
    private List<Place.Field> placeFields = Arrays.asList(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG);

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(listener);
    }

    @Override
    protected void onStop() {
        if (listener != null)
            firebaseAuth.removeAuthStateListener(listener);
        compositeDisposable.clear();
        super.onStop();
    }

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
        dialog = new SpotsDialog.Builder().setCancelable(false).setContext(this).build();

        listener = firebaseAuth -> {

            //Request permission when start app
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
                            if (report.areAllPermissionsGranted())
                            {
                                FirebaseUser user = firebaseAuth.getCurrentUser();
                                if (user != null) {

                                    checkUserFromFirebase(user);
                                } else {

                                    phoneLogin();

                                }
                            }
                            else
                                Toast.makeText(MainActivity.this, "You must accept all permissions", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {

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

                            //Check user with BrainTree get token
                            FirebaseAuth.getInstance().getCurrentUser()
                                    .getIdToken(true)
                                    .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                                    .addOnCompleteListener(tokenResultTask -> {
                                        Common.authorizeKey = tokenResultTask.getResult().getToken();
                                                    dialog.dismiss();
                                                    UserModel userModel = dataSnapshot.getValue(UserModel.class);
                                                    goToHomeActivityWithBrainTree(userModel);
                                    });

                            //Check User
//                            dialog.dismiss();
//                            UserModel userModel = dataSnapshot.getValue(UserModel.class);
//                            goToHomeActivity(userModel);

                        } else {
                            showRegisterDialog(user);
                            //showRegisterDialogWithPlaces(user); //use when app billing
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
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.title_register);
        builder.setMessage(R.string.msg_fill_info);

        View itemView = LayoutInflater.from(this).inflate(R.layout.layout_register, null);
        TextInputLayout phone_input_layout = (TextInputLayout) itemView.findViewById(R.id.phone_input_layout);
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
        builder.setPositiveButton("REGISTER", (dialogInterface, i) -> {


            if (TextUtils.isEmpty(edt_name.getText().toString())) {
                Toast.makeText(MainActivity.this, "Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            } else if (TextUtils.isEmpty(edt_address.getText().toString())) {
                Toast.makeText(this, R.string.please_address, Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(MainActivity.this, "Congratulation ! Register success", Toast.LENGTH_SHORT).show();
                                        goToHomeActivity(userModel);
                                    });
                        }
                    });
        });

        builder.setView(itemView);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    //Show Register use places_fragment (use when billing app)
    private void showRegisterDialogWithPlaces(FirebaseUser user) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Register");
        builder.setMessage("Please fill information");

        View itemView = LayoutInflater.from(this).inflate(R.layout.layout_register_with_places, null);

        TextInputLayout phone_input_layout = (TextInputLayout) itemView.findViewById(R.id.phone_input_layout);
        EditText edt_name = (EditText) itemView.findViewById(R.id.edt_name);
        TextView txt_address_detail = (TextView) itemView.findViewById(R.id.txt_address_detail);
        EditText edt_phone = (EditText) itemView.findViewById(R.id.edt_phone);

        places_fragment = (AutocompleteSupportFragment) getSupportFragmentManager()
                .findFragmentById(R.id.places_autocomplete_fragment);
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

        builder.setNegativeButton("CANCEL", (dialogInterface, i) -> {
            dialogInterface.dismiss();
        });
        builder.setPositiveButton("REGISTER", (dialogInterface, i) -> {

            if (placeSelected != null) {
                if (TextUtils.isEmpty(edt_name.getText().toString())) {
                    Toast.makeText(MainActivity.this, "Please enter your name", Toast.LENGTH_SHORT).show();
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
                                            Toast.makeText(MainActivity.this, "Congratulation ! Register success", Toast.LENGTH_SHORT).show();
                                            goToHomeActivity(userModel);


                                        });


                            }
                        });
            } else {
                Toast.makeText(this, "Please select address", Toast.LENGTH_SHORT).show();
                return;
            }
        });

        builder.setView(itemView);


        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(dialogInterface -> {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.remove(places_fragment);
            fragmentTransaction.commit();
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
                    startActivity(new Intent(MainActivity.this, HomeActivity.class));
                    finish();
                });

    }

    //Go HomeActivity with BrainTree Token
    private void goToHomeActivityWithBrainTree(UserModel userModel) {

        FirebaseInstanceId.getInstance()
                .getInstanceId()
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();

                    Common.currentUser = userModel; //important, you need always assign value for it before use

                    startActivity(new Intent(MainActivity.this, HomeActivity.class));
                    finish();
                })
                .addOnCompleteListener(task -> {

                    Common.currentUser = userModel; //important, you need always assign value for it before use
                    Common.updateToken(MainActivity.this, task.getResult().getToken());
                    startActivity(new Intent(MainActivity.this, HomeActivity.class));
                    finish();
                });

    }

    private void phoneLogin() {

        startActivityForResult(AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setLogo(R.mipmap.ic_launcher)
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
            }
        }
    }

}