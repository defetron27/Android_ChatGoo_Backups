package com.deffe.max.chatgoo;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.deffe.max.chatgoo.Adapters.ChattingRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rilixtech.CountryCodePicker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ChatFragment extends Fragment
{
    private String onlineUserId;

    private View view;

    private ChattingRecyclerAdapter adapter;

    private Set<String> finalizedNumbers = new HashSet<>();

    private boolean mobileStatus;

    private Activity activity;

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    private PhoneAuthProvider.ForceResendingToken token;

    private String verificationId,phoneCode,countryName,userMobileNumber;;

    public ChatFragment()
    {

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        view = inflater.inflate(R.layout.fragment_chat, container, false);

        activity = getActivity();

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null)
        {
            onlineUserId = firebaseUser.getUid();
        }

        final DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        usersRef.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                mobileStatus = dataSnapshot.hasChild("mobile_number");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {
                Crashlytics.log(databaseError.getMessage());
            }
        });

        if (mobileStatus)
        {
            final Set<String> chattingUsers = new HashSet<>(getContactNumbers());

            final RecyclerView chattingUsersRecyclerView = view.findViewById(R.id.chatting_users_recycler_view);
            chattingUsersRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext(),LinearLayoutManager.VERTICAL,false));

            ValueEventListener eventListener = new ValueEventListener()
            {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot)
                {
                    finalizedNumbers.clear();

                    for (DataSnapshot dataSnapshot : snapshot.getChildren())
                    {
                        String userKey = dataSnapshot.getKey();

                        if (userKey != null && !userKey.equals(onlineUserId))
                        {
                            Object objectMobileNumber = dataSnapshot.child("user_number").getValue();
                            Object objectMobileNumberWithPlus = dataSnapshot.child("user_number_with_plus").getValue();

                            if (objectMobileNumber != null && objectMobileNumberWithPlus != null)
                            {
                                String mobileNumber = objectMobileNumber.toString();
                                String mobileNumberWithPlus = objectMobileNumberWithPlus.toString();

                                for (String number : chattingUsers)
                                {
                                    if (number.equals(mobileNumber) || number.equals(mobileNumberWithPlus))
                                    {
                                        finalizedNumbers.add(userKey);
                                    }
                                }
                            }
                        }
                    }
                    adapter = new ChattingRecyclerAdapter(finalizedNumbers, view.getContext(),getActivity());
                    chattingUsersRecyclerView.setAdapter(adapter);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError)
                {
                    Crashlytics.log(databaseError.getMessage());
                }
            };

            usersRef.addValueEventListener(eventListener);
        }
        else
        {
            View dialogView = LayoutInflater.from(view.getContext()).inflate(R.layout.dialog_mobile_number, null);

            TelephonyManager manager = (TelephonyManager) view.getContext().getSystemService(Context.TELEPHONY_SERVICE);
            String userDefaultCountry = manager != null ? manager.getSimCountryIso() : null;

            final CountryCodePicker countryCodePicker = dialogView.findViewById(R.id.dialog_user_country_picker);
            final AppCompatEditText mobileNumberEditText = dialogView.findViewById(R.id.dialog_user_mobile_number);

            countryCodePicker.setCountryPreference(userDefaultCountry);
            countryCodePicker.setCountryForNameCode(userDefaultCountry);
            countryCodePicker.setDefaultCountryUsingNameCode(userDefaultCountry);
            countryCodePicker.registerPhoneNumberTextView(mobileNumberEditText);

            DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    String number = mobileNumberEditText.getText().toString();

                    if (!number.equals("") && !number.equals(" "))
                    {
                        phoneCode = countryCodePicker.getSelectedCountryCodeWithPlus();
                        countryName = countryCodePicker.getSelectedCountryName();

                        userMobileNumber = number.replaceAll("\\s+","");

                        PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneCode + userMobileNumber , 60, TimeUnit.SECONDS, activity , callbacks);
                    }
                    else
                    {
                        dialog.dismiss();
                    }
                }
            };

            DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());

            builder.setTitle("Enter your mobile number for chat with friends").setCancelable(false).setView(dialogView)
                    .setPositiveButton("Ok",okListener)
                    .setNegativeButton("Cancel",cancelListener);

            final AlertDialog dialog = builder.create();
            dialog.show();

            callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks()
            {
                @Override
                public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential)
                {

                }

                @Override
                public void onVerificationFailed(FirebaseException e)
                {
                    if (e instanceof FirebaseAuthInvalidCredentialsException)
                    {
                        Toast.makeText(view.getContext(), "Invalid Phone Number", Toast.LENGTH_SHORT).show();
                    }
                    else if (e instanceof FirebaseTooManyRequestsException)
                    {
                        Toast.makeText(view.getContext(), "Quota exceeded", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken)
                {
                    super.onCodeSent(s, forceResendingToken);

                    verificationId = s;
                    token = forceResendingToken;

                    dialog.dismiss();

                    View otpCodeView = LayoutInflater.from(view.getContext()).inflate(R.layout.dialog_otp_code, null);

                    final AppCompatEditText otpCodeEditText = otpCodeView.findViewById(R.id.dialog_user_otp_code);

                    DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(final DialogInterface dialog, int which)
                        {
                            String otp = otpCodeEditText.getText().toString();

                            if (!otp.equals("") && !otp.equals(" "))
                            {
                                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId,otp);

                                if (otp.equals(credential.getSmsCode()))
                                {
                                    Map<String,Object> mobileNumber = new HashMap<>();

                                    mobileNumber.put("mobile_number",userMobileNumber);
                                    mobileNumber.put("mobile_number_with_plus",phoneCode+userMobileNumber);
                                    mobileNumber.put("user_country_name",countryName);

                                    usersRef.child(onlineUserId).updateChildren(mobileNumber).addOnCompleteListener(new OnCompleteListener<Void>()
                                    {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task)
                                        {
                                            if (task.isSuccessful())
                                            {
                                                Toast.makeText(activity, "Mobile verification has been completed", Toast.LENGTH_SHORT).show();
                                                dialog.dismiss();
                                            }
                                        }
                                    });
                                }
                                else
                                {
                                    Toast.makeText(view.getContext(), "Invalid otp code", Toast.LENGTH_SHORT).show();
                                }
                            }
                            else
                            {
                                dialog.dismiss();
                            }
                        }
                    };

                    DialogInterface.OnClickListener resendListener = new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneCode + userMobileNumber , 60, TimeUnit.SECONDS, activity , callbacks, token);
                        }
                    };

                    DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            dialog.dismiss();
                        }
                    };

                    AlertDialog.Builder otpBuilder = new AlertDialog.Builder(view.getContext());

                    otpBuilder.setTitle("Enter your received OTP").setCancelable(false).setView(otpCodeView)
                            .setPositiveButton("Ok",okListener)
                            .setNeutralButton("Resend",resendListener)
                            .setNegativeButton("Cancel",cancelListener);

                    final AlertDialog otpDialog = otpBuilder.create();
                    otpDialog.show();
                }
            };
        }

        return view;
    }

    private Set<String> getContactNumbers()
    {
        Set<String> mobileNumbers = new HashSet<>();

        Cursor cursor = view.getContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,null,null,null);

        if (cursor != null)
        {
            while (cursor.moveToNext())
            {
                String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                mobileNumbers.add(number);
            }
        }

        if (cursor != null)
        {
            cursor.close();
        }

        return mobileNumbers;
    }
}
