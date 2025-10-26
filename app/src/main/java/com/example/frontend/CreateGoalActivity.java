package com.example.frontend;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Arrays;

public class CreateGoalActivity extends AppCompatActivity {

    private LayoutInflater _layoutInflater;
    private ArrayList<ConstraintLayout> _previousMessages;
    private ConstraintLayout _messagesContainer;
    private NestedScrollView _messagesScroll;
    private EditText _editTextBox;
    private PopupWindow _subgoalsPopupWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_goal);
        _previousMessages = new ArrayList<>();
        _layoutInflater = LayoutInflater.from(this);
        _messagesContainer = findViewById(R.id.messagesContainer);
        _editTextBox = findViewById(R.id.editTextBox);
        _messagesScroll = findViewById(R.id.messagesScroll);
        Button sendMessageButton = findViewById(R.id.sendMessageButton);
        sendMessageButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                /*Toast.makeText(MainActivity.this, "You clicked the button!", Toast.LENGTH_SHORT).show();*/
                if (!_editTextBox.getText().toString().isEmpty()) {
                    SendMessage(_editTextBox.getText().toString());
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            GenerateResponse("Message 2");
                        }
                    }, 2000);
                }
            }
        });
    }

    private void SendMessage(String messageInput) {
        _editTextBox.setText("");
        ConstraintLayout messageBox = CreateNewUserMessageBox(messageInput);
        AlignMessage(messageBox, true);
        _previousMessages.add(messageBox);
        _messagesScroll.post(() -> _messagesScroll.smoothScrollTo(0, _messagesContainer.getBottom()));
    }

    private void GenerateResponse(String response) {
        ConstraintLayout messageBox = CreateNewResponseMessageBox(response);
        AlignMessage(messageBox, false);
        _previousMessages.add(messageBox);
        _messagesScroll.post(() -> _messagesScroll.smoothScrollTo(0, _messagesContainer.getBottom()));
    }

    private ConstraintLayout CreateNewUserMessageBox(String messageInput) {

        ConstraintLayout messageBox = (ConstraintLayout) _layoutInflater.inflate(R.layout.message_user,
                _messagesContainer, false);
        messageBox.setId(View.generateViewId());
        TextView messageBoxTextView = messageBox.findViewById(R.id.messageUserText);
        messageBoxTextView.setText(messageInput);
        _messagesContainer.addView(messageBox, messageBox.getLayoutParams());
        return messageBox;
    }

    private ConstraintLayout CreateNewResponseMessageBox(String messageInput) {
        ConstraintLayout messageBox = (ConstraintLayout) _layoutInflater.inflate(R.layout.message_response,
                _messagesContainer, false);
        messageBox.setId(View.generateViewId());
        TextView messageBoxTextView = messageBox.findViewById(R.id.messageResponseText);
        Button showSubgoalsButton = messageBox.findViewById(R.id.showSubgoalsButton);
        Button tryAgainButton = messageBox.findViewById(R.id.tryAgainButton);
        showSubgoalsButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                OpenSubGoalsPopUp(new MainGoalModel(
                        "Learn Android Development",
                        "Build a fully functional Android app from scratch",
                        new ArrayList<>(Arrays.asList(
                                new SubgoalModel("Set up Android Studio", "Install and configure the IDE"),
                                new SubgoalModel("Learn XML layouts", "Understand ConstraintLayout and view hierarchy"),
                                new SubgoalModel("Build a sample app", "Implement UI and basic logic")
                        ))
                ));
            }
        });
        tryAgainButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                GenerateResponse("Okay, I will try again");
            }
        });
        messageBoxTextView.setText(messageInput);
        _messagesContainer.addView(messageBox, messageBox.getLayoutParams());
        return messageBox;
    }

    private void AlignMessage(ConstraintLayout messageBox, Boolean userMessage){
        ConstraintSet messageContainerConstraintSet = new ConstraintSet();
        messageContainerConstraintSet.clone(_messagesContainer);
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
        messageContainerConstraintSet.applyTo(_messagesContainer);
    }

    private void OpenSubGoalsPopUp(MainGoalModel model) {
        MaterialCardView subgoalsPopupWindow = (MaterialCardView) _layoutInflater.inflate(
                R.layout.subgoals_popup_window, null);
        ConstraintLayout popupRoot = subgoalsPopupWindow.findViewById(R.id.subgoalsPopupRoot);
        TextView popupTitle = popupRoot.findViewById(R.id.popupTitle);
        popupTitle.setText(model.name);
        Button popupdismissButton = popupRoot.findViewById(R.id.popupDismissButton);
        popupdismissButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                CloseSubGoalsPopUp();
            }
        });
        LinearLayout subgoalsContainer = subgoalsPopupWindow.findViewById(R.id.subgoalsContainer);
        for (SubgoalModel subgoalModel : model.getSubgoals()) {
            ConstraintLayout subgoalBox = (ConstraintLayout) _layoutInflater.inflate(
                    R.layout.subgoal_box, subgoalsContainer, false);

            subgoalBox.setId(View.generateViewId());
            ((TextView) subgoalBox.findViewById(R.id.editTextSubgoalTitle))
                    .setText(subgoalModel.getName());
            ((TextView) subgoalBox.findViewById(R.id.editTextSubgoalDescription))
                    .setText(subgoalModel.getDescription());

            // Important: set LayoutParams for LinearLayout
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.topMargin = (int) (8 * getResources().getDisplayMetrics().density); // spacing between items
            subgoalBox.setLayoutParams(lp);

            subgoalsContainer.addView(subgoalBox);
        }

        int width = ViewGroup.LayoutParams.MATCH_PARENT;
        int height = ViewGroup.LayoutParams.MATCH_PARENT;
        boolean focusable = true;
        _subgoalsPopupWindow = new PopupWindow(subgoalsPopupWindow, width, height, focusable);
        View rootView = findViewById(android.R.id.content);
        _subgoalsPopupWindow.showAtLocation(rootView, Gravity.CENTER, 0, 0);
    }

    private void CloseSubGoalsPopUp() {
        _subgoalsPopupWindow.dismiss();
    }
}