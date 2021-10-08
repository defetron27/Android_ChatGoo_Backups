package com.deffe.max.chatgoo;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class LoginActivity extends AppCompatActivity
{
    private FirebaseAuth firebaseAuth;
    private DatabaseReference loginUserRef;

    private boolean status;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();

        loginUserRef = FirebaseDatabase.getInstance().getReference().child("Users");
        loginUserRef.keepSynced(true);

        final CircleImageView loginUserImage = findViewById(R.id.login_user_image);
        final AppCompatEditText loginUserEmailId = findViewById(R.id.login_user_email_id);
        final AppCompatEditText loginUserPassword = findViewById(R.id.login_user_password);
        AppCompatButton loginDoneBtn = findViewById(R.id.login_done_btn);
        AppCompatTextView registerOption = findViewById(R.id.register_option);

        registerOption.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(LoginActivity.this,RegisterActivity.class));
                overridePendingTransition(R.anim.slide_in_up,R.anim.slide_out_up);
            }
        });

        loginDoneBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                final String email = loginUserEmailId.getText().toString();
                String password = loginUserPassword.getText().toString();

                if (TextUtils.isEmpty(email))
                {
                    Toast.makeText(LoginActivity.this, "Please enter your email id", Toast.LENGTH_SHORT).show();
                }
                else if (TextUtils.isEmpty(password))
                {
                    Toast.makeText(LoginActivity.this, "Please enter your password", Toast.LENGTH_SHORT).show();
                }
                else if (password.length() < 8)
                {
                    Toast.makeText(LoginActivity.this, "Please enter your password atleast 8 characters", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    firebaseAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>()
                    {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task)
                        {
                            if (!task.isSuccessful())
                            {
                                try
                                {
                                    Exception exception = task.getException();

                                    if (exception != null)
                                    {
                                        throw task.getException();
                                    }
                                }
                                catch (FirebaseAuthInvalidUserException invalidEmail)
                                {
                                    Toast.makeText(LoginActivity.this, "Invalid Email-Id, Please check or try register option", Toast.LENGTH_SHORT).show();
                                }
                                catch (FirebaseAuthInvalidCredentialsException wrongPassword)
                                {
                                    Toast.makeText(LoginActivity.this, "You entered wrong password, Please check", Toast.LENGTH_SHORT).show();
                                }
                                catch (Exception e)
                                {
                                    Toast.makeText(LoginActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }

                            if (task.isSuccessful())
                            {
                                final FirebaseUser user = firebaseAuth.getCurrentUser();

                                if (user != null)
                                {
                                    FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>()
                                    {
                                        @Override
                                        public void onComplete(@NonNull Task<InstanceIdResult> task)
                                        {
                                            String onlineUserId = user.getUid();

                                            String deviceToken = task.getResult().getToken();

                                            Map<String, Object> login = new HashMap<>();

                                            login.put("device_token",deviceToken);

                                            loginUserRef.child(onlineUserId).updateChildren(login).addOnCompleteListener(new OnCompleteListener<Void>()
                                            {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task)
                                                {
                                                    if (task.isSuccessful())
                                                    {
                                                        signInUser();
                                                    }
                                                }
                                            }).addOnFailureListener(new OnFailureListener()
                                            {
                                                @Override
                                                public void onFailure(@NonNull Exception e)
                                                {
                                                    Toast.makeText(LoginActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    });
                                }
                            }
                        }
                    });
                }
            }
        });

        final FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null)
        {
            loginUserRef.child(currentUser.getUid()).addValueEventListener(new ValueEventListener()
            {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                {
                    if (dataSnapshot.getValue() != null)
                    {
                        Object objectThumb = dataSnapshot.child("user_profile_thumb_img").getValue();

                        if (objectThumb != null)
                        {
                            final String image = objectThumb.toString();

                            Picasso.with(LoginActivity.this).load(image).networkPolicy(NetworkPolicy.OFFLINE).into(loginUserImage, new Callback()
                            {
                                @Override
                                public void onSuccess()
                                {

                                }

                                @Override
                                public void onError()
                                {
                                    Picasso.with(LoginActivity.this).load(image).placeholder(R.drawable.img_sel).into(loginUserImage);
                                }
                            });
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError)
                {
                    Crashlytics.log(databaseError.getMessage());
                }
            });
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null)
        {
           signInUser();
        }
    }

    private void signInUser()
    {
        Intent mainIntent = new Intent(LoginActivity.this,MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mainIntent);
        finish();
    }
}