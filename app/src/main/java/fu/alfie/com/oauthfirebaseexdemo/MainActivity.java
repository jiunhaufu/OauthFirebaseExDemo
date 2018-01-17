package fu.alfie.com.oauthfirebaseexdemo;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 123;
    private List<AuthUI.IdpConfig> providers;
    private FirebaseAuth mAuth;


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
