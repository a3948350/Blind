package com.example.blind;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

public class MyTextView extends AppCompatTextView {
    public MyTextView(@NonNull Context context) {
        super(context);
    }

    public MyTextView(Context context, AttributeSet attributeSet){
        super(context,attributeSet);
    }

    public MyTextView(Context context, AttributeSet attributeSet, int defStyleAttr){
        super(context,attributeSet,defStyleAttr);
    }

    @Override
    public boolean isFocused(){
        return true;
    }
}