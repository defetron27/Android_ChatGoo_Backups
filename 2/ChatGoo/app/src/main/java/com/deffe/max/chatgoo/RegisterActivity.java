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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        final DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users");

        final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        final AppCompatEditText registerUserEmailId = findViewById(R.id.register_user_email_id);
        final AppCompatEditText registerUserPassword = findViewById(R.id.register_user_password);
        final AppCompatEditText registerUserConfirmPassword = findViewById(R.id.register_user_confirm_password);
        AppCompatButton registerDoneBtn = findViewById(R.id.register_done_btn);
        AppCompatTextView loginOption = findViewById(R.id.login_option);

        loginOption.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onBackPressed();
            }
        });

        registerDoneBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                final String email = registerUserEmailId.getText().toString();
                final String password = registerUserPassword.getText().toString();
                final String confirmPassword = registerUserConfirmPassword.getText().toString();

                if (TextUtils.isEmpty(email))
                {
                    Toast.makeText(RegisterActivity.this, "Please enter your email id", Toast.LENGTH_SHORT).show();
                }
                else if (TextUtils.isEmpty(password))
                {
                    Toast.makeText(RegisterActivity.this, "Please enter your password", Toast.LENGTH_SHORT).show();
                }
                else if (TextUtils.isEmpty(confirmPassword))
                {
                    Toast.makeText(RegisterActivity.this, "Please enter your confirm password", Toast.LENGTH_SHORT).show();
                }
                else if (password.length() < 8)
                {
                    Toast.makeText(RegisterActivity.this, "Please enter your password atleast 8 characters", Toast.LENGTH_SHORT).show();
                }
                else if (confirmPassword.length() < 8)
                {
                    Toast.makeText(RegisterActivity.this, "Please enter your confirm password atleast 8 characters", Toast.LENGTH_SHORT).show();
                }
                else if (!confirmPassword.equals(password))
                {
                    Toast.makeText(RegisterActivity.this, "Passwords mismatch", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    firebaseAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>()
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
                                catch (FirebaseAuthWeakPasswordException weakPassword)
                                {
                                    Toast.makeText(RegisterActivity.this, "Weak Password", Toast.LENGTH_SHORT).show();
                                }
                                catch (FirebaseAuthInvalidCredentialsException malformedEmail)
                                {
                                    Toast.makeText(RegisterActivity.this, malformedEmail.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                }
                                catch (FirebaseAuthUserCollisionException existEmail)
                                {
                                    Toast.makeText(RegisterActivity.this, "Your Email is already exist, Please try login or enter another email", Toast.LENGTH_SHORT).show();
                                }
                                catch (Exception e)
                                {
                                    Toast.makeText(RegisterActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
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
                                            final String onlineUserId = user.getUid();

                                            final String deviceToken = task.getResult().getToken();

                                            String[] userName = email.split("@");

                                            Calendar calendar = Calendar.getInstance();

                                            Date today = calendar.getTime();

                                            calendar.add(Calendar.DAY_OF_YEAR,1);

                                            final Date tomorrow = calendar.getTime();

                                            DateFormat dateFormat = new SimpleDateFormat("hh:mm:ss_yyyy.MM.dd", Locale.getDefault());

                                            String expire = dateFormat.format(tomorrow);

                                            String created = dateFormat.format(today);

                                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss_yyyy.MM.dd", Locale.getDefault());

                                            try
                                            {
                                                Date expireDate = simpleDateFormat.parse(expire);

                                                Date createdDate = simpleDateFormat.parse(created);

                                                final long expireMillis = expireDate.getTime();

                                                long createdMillis = createdDate.getTime();

                                                Map<String, Object> register = new HashMap<>();

                                                register.put("user_id",onlineUserId);
                                                register.put("user_name",userName[0]);
                                                register.put("user_email_id",email);
                                                register.put("user_email_password",password);
                                                register.put("device_token",deviceToken);
                                                register.put("created_time", createdMillis);
                                                register.put("user_profile_img","default_profile_img");
                                                register.put("user_profile_thumb_img","default_profile_thumb_img");
                                                register.put("status","active");

                                                userRef.child(onlineUserId).updateChildren(register).addOnCompleteListener(new OnCompleteListener<Void>()
                                                {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task)
                                                    {
                                                        if (task.isSuccessful())
                                                        {
                                                            user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>()
                                                            {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task)
                                                                {
                                                                    if (!task.isSuccessful())
                                                                    {
                                                                        Exception exception = task.getException();

                                                                        if (exception != null)
                                                                        {
                                                                            try
                                                                            {
                                                                                throw exception;
                                                                            }
                                                                            catch (Exception e)
                                                                            {
                                                                                Toast.makeText(RegisterActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                                                            }
                                                                        }
                                                                    }

                                                                    if (task.isSuccessful())
                                                                    {
                                                                        AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);

                                                                        DialogInterface.OnClickListener laterListener = new DialogInterface.OnClickListener()
                                                                        {
                                                                            @Override
                                                                            public void onClick(DialogInterface dialog, int which)
                                                                            {
                                                                                userRef.child(onlineUserId).child("email_verification_status").setValue("not_verified");
                                                                                userRef.child(onlineUserId).child("expire_date").setValue(expireMillis).addOnCompleteListener(new OnCompleteListener<Void>()
                                                                                {
                                                                                    @Override
                                                                                    public void onComplete(@NonNull Task<Void> task)
                                                                                    {
                                                                                        if (!task.isSuccessful())
                                                                                        {
                                                                                            Exception exception = task.getException();

                                                                                            if (exception != null)
                                                                                            {
                                                                                                try
                                                                                                {
                                                                                                    throw exception;
                                                                                                }
                                                                                                catch (Exception e)
                                                                                                {
                                                                                                    Toast.makeText(RegisterActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                                                                                }
                                                                                            }
                                                                                        }

                                                                                        if (task.isSuccessful())
                                                                                        {
                                                                                            Intent mainIntent = new Intent(RegisterActivity.this,MainActivity.class);
                                                                                            mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                                                            startActivity(mainIntent);
                                                                                            finish();
                                                                                        }
                                                                                    }
                                                                                });
                                                                            }
                                                                        };

                                                                        builder.setTitle("Verification has been sent to your email. Please verify.")
                                                                                .setCancelable(false)
                                                                                .setNegativeButton("LATER",laterListener);

                                                                        AlertDialog dialog = builder.create();
                                                                        dialog.show();
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    }
                                                }).addOnFailureListener(new OnFailureListener()
                                                {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e)
                                                    {
                                                        Toast.makeText(RegisterActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }
                                            catch (ParseException e)
                                            {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();

        overridePendingTransition(R.anim.slide_in_down,R.anim.slide_out_down);
    }
}
