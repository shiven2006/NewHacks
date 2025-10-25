package com.example.frontend;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ArrayList<TextView> _previousMessages;
    private ConstraintLayout _messageContainer;
    private EditText _editTextBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        _previousMessages = new ArrayList<>();
        _messageContainer = findViewById(R.id.messagesContainer);
        _editTextBox = findViewById(R.id.editTextBox);
        Button sendMessageButton = findViewById(R.id.sendMessageButton);
        sendMessageButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                /*Toast.makeText(MainActivity.this, "You clicked the button!", Toast.LENGTH_SHORT).show();*/
                SendMessage(_editTextBox.getText().toString());
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        GenerateResponse("Message 2");
                    }
                }, 2000);
            }
        });
    }

    private void SendMessage(String messageInput) {
        _editTextBox.setText("");
        TextView messageBox = CreateNewMessageBox(messageInput, true);
        AlignMessage(messageBox, true);
        _previousMessages.add(messageBox);
    }

    private void GenerateResponse(String response) {
        TextView messageBox = CreateNewMessageBox(response, false);
        AlignMessage(messageBox, false);
        _previousMessages.add(messageBox);
    }

    private TextView CreateNewMessageBox(String messageInput, Boolean userInput) {
        TextView messageBox = new TextView(this);
        messageBox.setId(View.generateViewId());
        messageBox.setText(messageInput);
        messageBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        GradientDrawable messageBoxBackground = new GradientDrawable();
        if (userInput) {
            messageBox.setTextColor(Color.WHITE);
            messageBoxBackground.setColor(Color.BLACK);
        }
        else {
            messageBox.setTextColor(Color.WHITE);
            messageBoxBackground.setColor(Color.DKGRAY);
        }
        messageBoxBackground.setCornerRadius(50f);
        messageBox.setBackground(messageBoxBackground);
        messageBox.setPadding(50, 25, 50, 25);
        ConstraintLayout.LayoutParams layoutParams =
                new ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                );
        _messageContainer.addView(messageBox, layoutParams);
        return messageBox;
    }

    private void AlignMessage(TextView messageBox, Boolean userMessage){
        ConstraintSet messageContainerConstraintSet = new ConstraintSet();
        messageContainerConstraintSet.clone(_messageContainer);
        if (userMessage)
            messageContainerConstraintSet.connect(messageBox.getId(),
                ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 10);
        else
            messageContainerConstraintSet.connect(messageBox.getId(),
                    ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 10);
        if (_previousMessages.isEmpty())
            messageContainerConstraintSet.connect(messageBox.getId(),
                    ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 20);
        else
        {
            int previousMessageIndex = _previousMessages.size() - 1;
            messageContainerConstraintSet.connect(messageBox.getId(),
                    ConstraintSet.TOP, _previousMessages.get(previousMessageIndex).getId(),
                    ConstraintSet.BOTTOM, 20);
        }
        messageContainerConstraintSet.constrainWidth(messageBox.getId(), ConstraintSet.MATCH_CONSTRAINT);
        messageContainerConstraintSet.constrainHeight(messageBox.getId(), ConstraintSet.WRAP_CONTENT);
        messageContainerConstraintSet.constrainPercentWidth(messageBox.getId(), 0.8f);
        messageContainerConstraintSet.applyTo(_messageContainer);
    }
}