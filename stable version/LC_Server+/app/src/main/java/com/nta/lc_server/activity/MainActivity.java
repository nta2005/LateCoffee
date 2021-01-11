package com.nta.lc_server.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.nta.lc_server.R;
import com.nta.lc_server.common.Common;
import com.nta.lc_server.model.ServerUserModel;

import java.util.Arrays;
import java.util.List;

import dmax.dialog.SpotsDialog;

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
    }

    private void init() {
        providers = Arrays.asList(new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.EmailBuilder().build());

        serverRef = FirebaseDatabase.getInstance().getReference(Common.SERVER_REF);
        firebaseAuth = FirebaseAuth.getInstance();
        dialog = new SpotsDialog.Builder().setContext(this).setMessage("Đang tải...").setCancelable(false).build();

        listener = firebaseAuth -> {
            //Yêu cầu cấp quyền khi mở app
            Dexter.withContext(this)
                    .withPermissions(
                            Arrays.asList(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    )
                    .withListener(new MultiplePermissionsListener() {
                        @Override
                        public void onPermissionsChecked(MultiplePermissionsReport report) {
                            if (report.areAllPermissionsGranted()) {
                                FirebaseUser user = firebaseAuth.getCurrentUser();
                                if (user != null) {
                                    checkServerUserFromFirebase(user);
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

    private void checkServerUserFromFirebase(FirebaseUser user) {
        dialog.show();
        serverRef.child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            ServerUserModel userModel = dataSnapshot.getValue(ServerUserModel.class);
                            if (userModel.isActive()) {
                                goToHomeActivity(userModel);
                            } else {
                                dialog.dismiss();
                                Toast.makeText(MainActivity.this, "Bạn chưa được Admin cấp quyền sử dụng app!", Toast.LENGTH_SHORT).show();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ĐĂNG KÝ");
        builder.setMessage("Hãy điền thông tin \n Admin sẽ kích hoạt tài khoản sau");

        View itemview = LayoutInflater.from(this).inflate(R.layout.layout_register, null);
        TextInputLayout phone_input_layout = itemview.findViewById(R.id.phone_input_layout);
        EditText edt_name = itemview.findViewById(R.id.edt_name);
        EditText edt_phone = itemview.findViewById(R.id.edt_phone);


        //Set data
        if (user.getPhoneNumber() == null || TextUtils.isEmpty(user.getPhoneNumber())) {
            phone_input_layout.setHint("Email");
            edt_phone.setText(user.getEmail());
            edt_name.setText(user.getDisplayName());
        } else
            edt_phone.setText(user.getPhoneNumber());

        builder.setNegativeButton("Đóng", (dialogInterface, i) -> {
            dialogInterface.dismiss();
        })
                .setPositiveButton("Đăng ký", (dialogInterface, i) -> {
                    if (TextUtils.isEmpty(edt_name.getText().toString())) {
                        Toast.makeText(this, "Bạn chưa điền tên", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ServerUserModel serverUserModel = new ServerUserModel();
                    serverUserModel.setUid(user.getUid());
                    serverUserModel.setName(edt_name.getText().toString());
                    serverUserModel.setPhone(edt_phone.getText().toString());
                    serverUserModel.setActive(false); //default failed, we must active user by manual in firebase
                    //serverUserModel.setActive(true); //use when dev test
                    dialog.show();

                    serverRef.child(serverUserModel.getUid())
                            .setValue(serverUserModel)
                            .addOnFailureListener(e -> {
                                dialog.dismiss();
                                Toast.makeText(MainActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            })
                            .addOnCompleteListener(task -> {
                                dialog.dismiss();
                                Toast.makeText(this, "Chúc mừng! Đăng ký thành công! Hãy chờ admin active!", Toast.LENGTH_SHORT).show();
                                //goToHomeActivity(serverUserModel); //use when dev test
                            });

                });

        builder.setView(itemview);

        AlertDialog registerDialog = builder.create();
        registerDialog.show();
    }

    private void goToHomeActivity(ServerUserModel serverUserModel) {
        dialog.dismiss();

        Common.currentServerUser = serverUserModel;
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra(Common.IS_OPEN_ACTIVITY_NEW_ORDER,
                getIntent().getBooleanExtra(Common.IS_OPEN_ACTIVITY_NEW_ORDER, false));
        startActivity(intent);
        finish();
    }

    private void phoneLogin() {
        startActivityForResult(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setLogo(R.drawable.sv_logo)
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
                Toast.makeText(this, "Đăng nhập thất bại!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}