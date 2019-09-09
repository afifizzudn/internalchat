/**
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.afifizzudn.InternalChat;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class SignInActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;

    private SignInButton mSignInButton;
    private GoogleApiClient mGoogleApiClient;

    //Variable Untuk Komponen-komponen Yang Diperlukan
    private EditText NoTelepon, SetKode;
    private Button Masuk, Verifikasi, Resend;
    private TextView PhoneID;

    //Variable yang Dibutuhkan Untuk Autentikasi
    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener stateListener;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    private String VerifikasiID;
    private String No_Telepon;

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Assign fields
        mSignInButton = (SignInButton) findViewById(R.id.sign_in_button);
        NoTelepon = (EditText) findViewById(R.id.phone);
        SetKode = (EditText) findViewById(R.id.setVertifi);
        Masuk = (Button) findViewById(R.id.login);
        Verifikasi = (Button) findViewById(R.id.verifi);
        Resend = (Button) findViewById(R.id.resend);
        PhoneID = (TextView) findViewById(R.id.no_id);

        // Set click listeners
        mSignInButton.setOnClickListener(this);
        Masuk.setOnClickListener(this);
        Verifikasi.setOnClickListener(this);
        Resend.setOnClickListener(this);
        Resend.setEnabled(false);


        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        // Initialize FirebaseAuth
        mFirebaseAuth = FirebaseAuth.getInstance();
        stateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                //Meneteksi Apakah Ada User Yang Sedang Login (Belum Logout)
                if(user != null){
                    //Jika Ada, User Tidak perlu Login Lagi, dan Langsung Menuju Acivity Yang Dituju
                    startActivity(new Intent(SignInActivity.this, MainActivity.class));
                    finish();
                }
            }
        };
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            case R.id.login:
                No_Telepon = PhoneID.getText()+NoTelepon.getText().toString();
                setupVerificationCallback();
                PhoneAuthProvider.getInstance().verifyPhoneNumber(
                        No_Telepon, //NO Telepon Untuk Di Verifikasi
                        60, //Durasi Waktu Habis
                        TimeUnit.SECONDS, //Unit Timeout
                        this, //Activity
                        callbacks); // OnVerificationStateChangedCallbacks
                Toast.makeText(getApplicationContext(), "Memverifikasi, Mohon Tunggu", Toast.LENGTH_SHORT).show();
                NoTelepon.setText("");
                break;

            case R.id.verifi:
                String verifiCode = SetKode.getText().toString();
                if(TextUtils.isEmpty(verifiCode)){
                    //Memberi Pesan pada user bahwa kode verifikasi tidak boleh kosong saat menekan Tombol Verifikasi
                    Toast.makeText(getApplicationContext(),"Masukan Kode Verifikasi", Toast.LENGTH_SHORT).show();
                }else{
                    //Memverifikasi Nomor Telepon, Saat Tombol Verifikasi Ditekan
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(VerifikasiID, verifiCode);
                    signInWithPhoneAuthCredential(credential);
                    Toast.makeText(getApplicationContext(),"Sedang Memverifikasi", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.resend:
                No_Telepon = PhoneID.getText()+NoTelepon.getText().toString();
                setupVerificationCallback();
                PhoneAuthProvider.getInstance().verifyPhoneNumber(
                        No_Telepon, //NO Telepon Untuk Di Vertifikai
                        60, //Durasi Waktu Habis
                        TimeUnit.SECONDS, //Unit Timeout
                        this, //Activity
                        callbacks, // OnVerificationStateChangedCallbacks
                        resendToken); // Digunakan untuk mengirim ulang kembali kode verifikasi
                Toast.makeText(getApplicationContext(), "Mengirim Ulang Kode Verifikasi", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Melampirkan Listener pada FirebaseAuth saat Activity Dimulai
        mFirebaseAuth.addAuthStateListener(stateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(stateListener != null){
            //Menghapus Listener pada FirebaseAuth saat Activity Dihentikan
            mFirebaseAuth.removeAuthStateListener(stateListener);
        }
    }

    private void setupVerificationCallback(){
        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                // Callback didalam sini akan dipanggil/dieksekusi saat terjadi proses pengiriman kode
                // Dan User Diminta untuk memasukan kode verifikasi

                // Untuk Menyimpan ID verifikasi dan kirim ulang token
                VerifikasiID = verificationId;
                resendToken = token;
                Resend.setEnabled(true);
                Toast.makeText(getApplicationContext(), "Mendapatkan Kode Verifikasi", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onVerificationCompleted(PhoneAuthCredential Credential) {
                // Callback disini akan dipanggil saat Verifikasi Selseai atau Berhasil
                Toast.makeText(getApplicationContext(), "Verifikasi Selesai", Toast.LENGTH_SHORT).show();
                signInWithPhoneAuthCredential(Credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                // Callback disini akan dipanggil saat permintaan tidak valid atau terdapat kesalahan
                Toast.makeText(getApplicationContext(), "Verifikasi Gagal, Silakan Coba Lagi", Toast.LENGTH_SHORT).show();
            }
        };
    }
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential){
        mFirebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            //Sign In Berhasil
                            startActivity(new Intent(SignInActivity.this, MainActivity.class));
                            finish();
                        }else{
                            //Sign In Gagal
                            if(task.getException() instanceof FirebaseAuthInvalidCredentialsException){
                                // Kode Yang Dimasukan tidal Valid.
                                Toast.makeText(getApplicationContext(), "Kode yang dimasukkan tidak valid", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }



    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign-In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                // Google Sign-In failed
                Log.e(TAG, "Google Sign-In failed.");
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGooogle:" + acct.getId());
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mFirebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            Log.d(TAG, "WAT");
                        } else {
                            Log.d(TAG, "i am working");
                        }
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(SignInActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "i am working");
                            startActivity(new Intent(SignInActivity.this, MainActivity.class));
                            finish();
                        }
                    }
                });
    }
}
