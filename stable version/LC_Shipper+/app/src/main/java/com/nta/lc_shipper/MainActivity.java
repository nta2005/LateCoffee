package com.nta.lc_shipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nta.lc_shipper.common.Common;
import com.nta.lc_shipper.model.ShipperUserModel;

import java.util.Arrays;
import java.util.List;

import dmax.dialog.SpotsDialog;
import io.paperdb.Paper;

public class MainActivity extends AppCompatActivity {

    private static int APP_REQUEST_CODE = 7171;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;
    private AlertDialog dialog;
    private DatabaseReference serverRef;
    private List<AuthUI.IdpConfig> providers;

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(listener);
    }

    @Override
    protected void onStop() {
        if (listener != null)
            firebaseAuth.removeAuthStateListener(listener);
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        init();

        //Remember comment it affter has been delete, if not do that, data always be delete :(
        //Delete data offline
//        Paper.init(this);
//        Paper.book().delete(Common.TRIP_START);
//        Paper.book().delete(Common.SHIPPING_ORDER_DATA);
    }

    private void init() {
        providers = Arrays.asList(new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.EmailBuilder().build());

        serverRef = FirebaseDatabase.getInstance().getReference(Common.SHIPPER_REF);
        firebaseAuth = FirebaseAuth.getInstance();
        dialog = new SpotsDialog.Builder().setContext(this).setCancelable(false).build();
        listener = firebaseAuthLocal -> {
            FirebaseUser user = firebaseAuthLocal.getCurrentUser();
            if (user != null) {
                //Check user from Firebase
                checkServerUserFromFirebase(user);
            } else {
                phoneLogin();
            }
        };
    }

    private void checkServerUserFromFirebase(FirebaseUser user) {
        dialog.show();
        serverRef.child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            ShipperUserModel userModel = dataSnapshot.getValue(ShipperUserModel.class);
                            if (userModel.isActive()) {
                                goToHomeActivity(userModel);
                            } else {
                                dialog.dismiss();
                                Toast.makeText(MainActivity.this, "You must be allowed from Admin to access this app", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            //User not exists in database
                            dialog.dismiss();
                            showRegisterDialog(user);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showRegisterDialog(FirebaseUser user) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Regiseter");
        builder.setMessage("Please fill information \n Admin will accept your account late");

        View itemview = LayoutInflater.from(this).inflate(R.layout.layout_register, null);
        TextInputLayout phone_input_layout = itemview.findViewById(R.id.phone_input_layout);
        EditText edt_name = itemview.findViewById(R.id.edt_name);
        EditText edt_phone = itemview.findViewById(R.id.edt_phone);


        if (user.getPhoneNumber() == null || TextUtils.isEmpty(user.getPhoneNumber())) {
            phone_input_layout.setHint("Email");
            edt_phone.setText(user.getEmail());
            edt_name.setText(user.getDisplayName());
        } else
            edt_phone.setText(user.getPhoneNumber());

        builder.setNegativeButton("CANCEL", (dialogInterface, i) -> {
            dialogInterface.dismiss();
        })
                .setPositiveButton("REGISTER", (dialogInterface, i) -> {
                    if (TextUtils.isEmpty(edt_name.getText().toString())) {
                        Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ShipperUserModel shipperUserModel = new ShipperUserModel();
                    shipperUserModel.setUid(user.getUid());
                    shipperUserModel.setName(edt_name.getText().toString());
                    shipperUserModel.setPhone(edt_phone.getText().toString());
                    //shipperUserModel.setActive(false); //default failed, we must active user by manual in firebase
                    shipperUserModel.setActive(true); //use when dev test
                    dialog.show();

                    serverRef.child(shipperUserModel.getUid())
                            .setValue(shipperUserModel)
                            .addOnFailureListener(e -> {
                                dialog.dismiss();
                                Toast.makeText(MainActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            })
                            .addOnCompleteListener(task -> {
                                dialog.dismiss();
                                Toast.makeText(this, "Congratulation! Register success! Admin will check and active you soon", Toast.LENGTH_SHORT).show();
                                goToHomeActivity(shipperUserModel); //use when dev test
                            });

                });

        builder.setView(itemview);

        androidx.appcompat.app.AlertDialog registerDialog = builder.create();
        registerDialog.show();
    }

    private void goToHomeActivity(ShipperUserModel shipperUserModel) {
        dialog.dismiss();
        Common.currentShipperUser = shipperUserModel;
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    private void phoneLogin() {
        startActivityForResult(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setLogo(R.mipmap.ic_launcher)
                .setTheme(R.style.LoginTheme)
                .build(), APP_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == APP_REQUEST_CODE) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            } else {
                Toast.makeText(this, "Failed to signin", Toast.LENGTH_SHORT).show();
            }
        }
    }
}