/*
 * Created by YSN Studio on 5/23/18 2:52 PM
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified 5/23/18 2:52 PM
 */

package com.ysn.examplewifidirect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.ysn.examplewifidirect.adapter.ChatAdapter;
import com.ysn.examplewifidirect.model.ChatDTO;
import com.ysn.examplewifidirect.transfer.DataSender;
import com.ysn.examplewifidirect.utils.ConnectionUtils;
import com.ysn.examplewifidirect.utils.Utility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ChatActivity extends AppCompatActivity {

    public static final String ACTION_CHAT_RECEIVED = "chatreceived";
    public static final String KEY_CHAT_DATA = "chat_data_key";
    public static final String KEY_CHATTING_WITH = "chattingwith";
    public static final String KEY_CHAT_IP = "chatterip";
    public static final String KEY_CHAT_PORT = "chatterport";
    private static final int REQUEST_CODE_GALLERY = 1;
    private static final int REQUEST_CODE_CAMERA = 2;

    private List<ChatDTO> chatList;
    private ChatAdapter chatAdapter;
    private String chattingWith;
    private String destIP;
    private int destPort;

    @BindView(R.id.recycler_view_chat)
    RecyclerView recyclerViewChat;
    @BindView(R.id.edit_text_message_activity_chat)
    EditText editTextMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);
        initialize();

        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, chatList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerViewChat.setLayoutManager(linearLayoutManager);
        recyclerViewChat.setAdapter(chatAdapter);
    }

    private void initialize() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CHAT_RECEIVED);
        LocalBroadcastManager.getInstance(this).registerReceiver(chatReceiver, filter);

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Toast.makeText(this, "Invalid arguments to open chat", Toast.LENGTH_SHORT)
                    .show();
            finish();
        }
        chattingWith = extras.getString(KEY_CHATTING_WITH);
        destIP = extras.getString(KEY_CHAT_IP);
        destPort = extras.getInt(KEY_CHAT_PORT);
    }

    @OnClick({
            R.id.fab_send_message_activity_chat,
            R.id.fab_send_image_activity_chat
    })
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab_send_message_activity_chat:
                String message = editTextMessage.getText().toString().trim();
                ChatDTO myChat = new ChatDTO();
                myChat.setPort(ConnectionUtils.getPort(this));
                myChat.setFromIP(Utility.getString(this, "myip"));
                myChat.setLocalTimestamp(System.currentTimeMillis());
                myChat.setMessage(message);
                myChat.setSentBy(chattingWith);
                myChat.setMyChat(true);
                myChat.setImageMessage(false);
                DataSender.sendChatInfo(this, destIP, destPort, myChat);
                editTextMessage.setText("");
                chatList.add(myChat);
                chatAdapter.notifyDataSetChanged();
                recyclerViewChat.smoothScrollToPosition(chatList.size() - 1);
                break;
            case R.id.fab_send_image_activity_chat:
                AlertDialog.Builder builderAlertDialog = new AlertDialog.Builder(this)
                        .setTitle("Select Action")
                        .setItems(new String[]{"Select photo from gallery", "Capture photo from camera"},
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int which) {
                                        switch (which) {
                                            case 0:
                                                choosePhotoFromGallery();
                                                break;
                                            case 1:
                                                takePhotoFromCamera();
                                                break;
                                        }
                                    }
                                });
                AlertDialog alertDialog = builderAlertDialog.create();
                alertDialog.show();
                break;
        }
    }

    private void choosePhotoFromGallery() {
        /*Intent intentGallery = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intentGallery, REQUEST_CODE_GALLERY);*/
        Intent intentImagePicker = new Intent(Intent.ACTION_PICK);
        intentImagePicker.setType("image/*");
        startActivityForResult(intentImagePicker, REQUEST_CODE_GALLERY);
    }

    private void takePhotoFromCamera() {
        Intent intentCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intentCamera, REQUEST_CODE_CAMERA);
    }

    private BroadcastReceiver chatReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_CHAT_RECEIVED:
                    ChatDTO chat = (ChatDTO) intent.getSerializableExtra(KEY_CHAT_DATA);
                    chat.setMyChat(false);
                    chatList.add(chat);
                    chatAdapter.notifyDataSetChanged();
                    recyclerViewChat.smoothScrollToPosition(chatList.size() - 1);
                    break;
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            return;
        }
        if (requestCode == REQUEST_CODE_GALLERY) {
            if (data != null) {
                Uri contentUri = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), contentUri);
                    String path = saveImage(bitmap);
                    ChatDTO myChat = new ChatDTO();
                    myChat.setPort(ConnectionUtils.getPort(this));
                    myChat.setFromIP(Utility.getString(this, "myip"));
                    myChat.setLocalTimestamp(System.currentTimeMillis());
                    myChat.setMessage(path);
                    myChat.setSentBy(chattingWith);
                    myChat.setMyChat(true);
                    myChat.setImageMessage(true);
                    DataSender.sendChatInfoImage(this, destIP, destPort, myChat, contentUri);
                    chatList.add(myChat);
                    chatAdapter.notifyDataSetChanged();
                    recyclerViewChat.smoothScrollToPosition(chatList.size() - 1);
                    Toast.makeText(ChatActivity.this, "Image saved!", Toast.LENGTH_SHORT)
                            .show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(ChatActivity.this, "Image failed saved!", Toast.LENGTH_SHORT)
                            .show();
                }
                /*DataSender.sendFile(this, destIP, destPort, contentUri);*/
            }
        } else if (requestCode == REQUEST_CODE_CAMERA) {
            Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
            saveImage(thumbnail);
            Toast.makeText(ChatActivity.this, "Image saved!", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public String saveImage(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        File dir = new File(Environment.getExternalStorageDirectory() + "/ExampleWiFiDirect");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            File fileImage = new File(dir, Calendar.getInstance().getTimeInMillis() + ".jpg");
            fileImage.createNewFile();
            FileOutputStream fileOutputStream = new FileOutputStream(fileImage);
            fileOutputStream.write(baos.toByteArray());

            MediaScannerConnection.scanFile(this, new String[]{fileImage.getPath()}, new String[]{"image/jpeg"}, null);
            fileOutputStream.close();
            return fileImage.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
