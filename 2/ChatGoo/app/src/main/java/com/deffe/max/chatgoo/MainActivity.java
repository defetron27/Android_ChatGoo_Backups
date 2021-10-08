package com.deffe.max.chatgoo;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity
{
    static
    {
        System.loadLibrary("native-lib");
    }

    private DatabaseReference userRef;

    private String onlineUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        userRef.keepSynced(true);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null)
        {
            onlineUserId = firebaseUser.getUid();

            if (firebaseUser.isEmailVerified())
            {
                userRef.child(onlineUserId).child("email_verification_status").setValue("verified").addOnCompleteListener(new OnCompleteListener<Void>()
                {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        if (task.isSuccessful())
                        {
                            userRef.child(onlineUserId).addValueEventListener(new ValueEventListener()
                            {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                                {
                                    if (dataSnapshot.hasChild("expire_date"))
                                    {
                                        userRef.child(onlineUserId).child("expire_date").removeValue().addOnFailureListener(new OnFailureListener()
                                        {
                                            @Override
                                            public void onFailure(@NonNull Exception e)
                                            {

                                            }
                                        });
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError)
                                {

                                }
                            });
                        }
                    }
                }).addOnFailureListener(new OnFailureListener()
                {
                    @Override
                    public void onFailure(@NonNull Exception e)
                    {
                        Crashlytics.log(e.getLocalizedMessage());
                    }
                });
            }
            else {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                DialogInterface.OnClickListener laterListener = new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(final DialogInterface dialog, int which)
                    {
                        userRef.child(onlineUserId).child("email_verification_status").setValue("not_verified").addOnCompleteListener(new OnCompleteListener<Void>()
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
                                            Toast.makeText(MainActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }

                                if (task.isSuccessful())
                                {
                                    dialog.cancel();
                                }
                            }
                        });
                    }
                };

                builder.setTitle("Your email has not verified. Please verify your email")
                        .setCancelable(false)
                        .setNegativeButton("Later",laterListener);

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        final CircleImageView currentImageCircleImageView = findViewById(R.id.current_image_circle_image_view);

        int[] icons = {R.drawable.ic_bot, R.drawable.ic_friends,};

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        ViewPager viewPager = findViewById(R.id.main_tab_content);

        setupViewPager(viewPager);

        tabLayout.setupWithViewPager(viewPager);

        for (int i = 0; i < icons.length; i++)
        {
            tabLayout.getTabAt(i).setIcon(icons[i]);
        }

        tabLayout.getTabAt(0).select();

        userRef.child(onlineUserId).addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.hasChild("user_profile_thumb_img"))
                {
                    Object objectThumb = dataSnapshot.child("user_profile_thumb_img").getValue();

                    if (objectThumb != null)
                    {
                        final String image = objectThumb.toString();

                        if (!image.equals("default_profile_thumb_img"))
                        {
                            Picasso.with(MainActivity.this).load(image).networkPolicy(NetworkPolicy.OFFLINE).into(currentImageCircleImageView, new Callback()
                            {
                                @Override
                                public void onSuccess()
                                {

                                }

                                @Override
                                public void onError()
                                {
                                    Picasso.with(MainActivity.this).load(image).placeholder(R.drawable.user_icon).into(currentImageCircleImageView);
                                }
                            });
                        }
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

    private void setupViewPager(ViewPager viewPager)
    {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.insertNewFragment(new AssistantFragment());
        adapter.insertNewFragment(new ChatFragment());
        viewPager.setAdapter(adapter);
    }

    class ViewPagerAdapter extends FragmentStatePagerAdapter
    {
        private final List<Fragment> mFragmentList = new ArrayList<>();

        ViewPagerAdapter(FragmentManager manager)
        {
            super(manager);
        }

        @Override
        public Fragment getItem(int position)
        {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount()
        {
            return mFragmentList.size();
        }

        void insertNewFragment(Fragment fragment)
        {
            mFragmentList.add(fragment);
        }
    }


    @Override
    protected void onStart()
    {
        super.onStart();

        if (onlineUserId == null)
        {
            signOut();
        }
        else
        {
            userRef.child(onlineUserId).addValueEventListener(new ValueEventListener()
            {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                {
                    if (dataSnapshot.hasChild("expire_date"))
                    {
                        Object objectCreated = dataSnapshot.child("expire_date").getValue();
                        Object objectVerification = dataSnapshot.child("email_verification_status").getValue();

                        if (objectCreated != null && objectVerification != null)
                        {
                            final String verificationStatus = objectVerification.toString();

                            long time = (long) objectCreated;

                            Calendar calendar = Calendar.getInstance();
                            Date today = calendar.getTime();
                            DateFormat dateFormat = new SimpleDateFormat("hh:mm:ss_yyyy.MM.dd", Locale.getDefault());
                            String current = dateFormat.format(today);
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss_yyyy.MM.dd", Locale.getDefault());
                            try
                            {
                                Date currentDate = simpleDateFormat.parse(current);

                                long currentMillis = currentDate.getTime();

                                if (time <= currentMillis && verificationStatus.equals("not_verified"))
                                {
                                    signOut();
                                }
                            }
                            catch (ParseException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError)
                {
                    Crashlytics.log(databaseError.getMessage());
                }
            });

            userRef.child(onlineUserId).child("status").setValue("active").addOnFailureListener(new OnFailureListener()
            {
                @Override
                public void onFailure(@NonNull Exception e)
                {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void signOut()
    {
        Intent mainIntent = new Intent(MainActivity.this,LoginActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mainIntent);
        finish();
    }
}