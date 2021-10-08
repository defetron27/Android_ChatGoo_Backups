package com.deffe.max.chatgoo;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatTextView;
import android.view.View;
import android.widget.CompoundButton;

public class WelcomeActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        AppCompatCheckBox acceptConditionCheckBox = findViewById(R.id.accept_condition_check_box);
        AppCompatTextView termsAndConditions = findViewById(R.id.terms_and_conditions);
        final AppCompatTextView acceptAndContinueBtn = findViewById(R.id.accept_and_continue);

        acceptConditionCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked)
                {
                    acceptAndContinueBtn.setEnabled(true);
                    acceptAndContinueBtn.setTextColor(Color.BLACK);
                }
                else
                {
                    acceptAndContinueBtn.setEnabled(false);
                    acceptAndContinueBtn.setTextColor(Color.GRAY);
                }
            }
        });

        acceptAndContinueBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(WelcomeActivity.this,RegisterActivity.class));
            }
        });

        termsAndConditions.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

            }
        });
    }
}
