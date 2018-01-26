package fu.alfie.com.oauthfirebaseexdemo;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 1;
    private static final int GOOGLE_SIGN_IN = 2;
    private List<AuthUI.IdpConfig> providers;
    private FirebaseAuth mAuth;
    //phone
    public PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    public String mVerificationId;
    public PhoneAuthProvider.ForceResendingToken mResendToken;
    //google
    public GoogleSignInClient mGoogleSignInClient;
    //facebook
    public CallbackManager mCallbackManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //printhashkey();

        mAuth = FirebaseAuth.getInstance();
        providers = Arrays.asList(
                new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                new AuthUI.IdpConfig.Builder(AuthUI.PHONE_VERIFICATION_PROVIDER).build(),
                new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build(),
                new AuthUI.IdpConfig.Builder(AuthUI.FACEBOOK_PROVIDER).build(),
                new AuthUI.IdpConfig.Builder(AuthUI.TWITTER_PROVIDER).build()
        );

        initPhoneLogin();
        initGoogleLogin();
        initFacebookLogin();
    }

    @Override
    protected void onStart() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
        super.onStart();
    }

    private void updateUI(FirebaseUser user) {
        TextView textView = (TextView)findViewById(R.id.textView);
        ImageView imageView = (ImageView)findViewById(R.id.imageView);

        if (user != null){
            textView.setText(
                     user.getDisplayName()+"\n"
                    +user.getEmail()+"\n"
                    +user.getUid());
            if (user.getPhotoUrl() != null){
                new DownloadImageTask(imageView).execute(String.valueOf(user.getPhotoUrl()));
            }
        }else{
            textView.setText("Logout");
            imageView.setImageResource(R.mipmap.ic_launcher);
        }
    }

    public void onLoginClick(View view) {
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setIsSmartLockEnabled(false)
                        .setLogo(R.mipmap.ic_launcher_round) // Set logo drawable
                        .setTheme(R.style.Theme_AppCompat_Light_NoActionBar) // Set theme
                        .setTosUrl("https://superapp.example.com/terms-of-service.html")  //Set terms of service
                        .setPrivacyPolicyUrl("https://superapp.example.com/privacy-policy.html")  //Set privacy policy
                        .build(),
                RC_SIGN_IN);
    }

    public void onLogoutClick(View view) {
        AuthUI.getInstance()
                .signOut(this) //登出帳號
                //.delete(this)  //刪除帳號
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                        updateUI(null);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN){
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK){
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                updateUI(user);
            }else {
                updateUI(null);
            }
        }
        if (requestCode == GOOGLE_SIGN_IN){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // ...
            }
        }
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    public void emailLogin(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            updateUI(null);
                        }
                    }
                });
    }

    public void emailRegister(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            updateUI(null);
                        }
                    }
                });
    }

    public void onEmailLoginClick(View view){
        EditText email = (EditText)findViewById(R.id.editText2);
        EditText password = (EditText)findViewById(R.id.editText1);
        emailLogin(String.valueOf(email.getText()),String.valueOf(password.getText()));
    }

    public void onEmailRegisterClick(View view){
        EditText email = (EditText)findViewById(R.id.editText2);
        EditText password = (EditText)findViewById(R.id.editText1);
        emailRegister(String.valueOf(email.getText()),String.valueOf(password.getText()));
    }

    private void initPhoneLogin() {
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    Toast.makeText(MainActivity.this,"驗證碼錯誤",Toast.LENGTH_SHORT).show();
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    Toast.makeText(MainActivity.this,"超過驗證次數",Toast.LENGTH_SHORT).show();
                }
                // Show a message and update the UI
            }

            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;
            }
        };
    }

    public void phoneSendSMS(String phoneNumber){
        //mAuth.setLanguageCode("zh-tw");
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                        phoneNumber,  // Phone number to verify
                        60,                 // Timeout duration
                        TimeUnit.SECONDS,   // Unit of timeout
                        this,               // Activity (for callback binding)
                        mCallbacks);
    }

    public void phoneVerification(String verificationCode){
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, verificationCode);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = task.getResult().getUser();
                            updateUI(user);
                        } else {
                            // Sign in failed, display a message and update the UI
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                updateUI(null);
                            }
                        }
                    }
                });
    }

    public void onPhoneSendSMSClick(View view){
        EditText phone = (EditText)findViewById(R.id.editText0);
        phoneSendSMS(String.valueOf(phone.getText()));
    }

    public void onPhoneVerificationClick(View view){
        EditText code = (EditText)findViewById(R.id.editText3);
        phoneVerification(String.valueOf(code.getText()));
    }

    private void initGoogleLogin() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            updateUI(null);
                        }
                    }
                });
    }

    public void onGoogleSigninClick(View view){
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN);
    }

    private void initFacebookLogin(){
        mCallbackManager = CallbackManager.Factory.create();
        final LoginButton loginButton = findViewById(R.id.button_facebook_login);
        loginButton.setReadPermissions("email", "public_profile");
        loginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                // ...
            }

            @Override
            public void onError(FacebookException error) {
                // ...
            }
        });
        //客製按鈕取代原生按鈕
        Button button = (Button)findViewById(R.id.button9);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginButton.performClick();
            }
        });

    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            updateUI(null);
                        }
                    }
                });
    }

    //facebook
    public void printhashkey(){
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "fu.alfie.com.oauthfirebaseexdemo",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }

    }
}
