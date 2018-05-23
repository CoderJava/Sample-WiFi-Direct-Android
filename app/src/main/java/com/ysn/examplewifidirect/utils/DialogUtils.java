package com.ysn.examplewifidirect.utils;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.ysn.examplewifidirect.ChatActivity;
import com.ysn.examplewifidirect.MainActivity;
import com.ysn.examplewifidirect.R;
import com.ysn.examplewifidirect.model.DeviceDTO;
import com.ysn.examplewifidirect.transfer.DataSender;

public class DialogUtils {

    public static final int CODE_PICK_IMAGE = 21;

    public static AlertDialog getServiceSelectionDialog(final Activity activity, final DeviceDTO selectedDevice) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
        alertDialog.setTitle(selectedDevice.getDeviceName());
        String[] types = new String[]{"Share image", "Chat", "Disconnect"};
        alertDialog.setItems(types, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                switch (i) {
                    case 0:
                        Intent intentImagePicker = new Intent(Intent.ACTION_PICK);
                        intentImagePicker.setType("image/*");
                        activity.startActivityForResult(intentImagePicker, CODE_PICK_IMAGE);
                        break;
                    case 1:
                        DataSender.sendChatRequest(activity, selectedDevice.getIp(), selectedDevice.getPort());
                        Toast.makeText(activity, "Chat request sent", Toast.LENGTH_SHORT)
                                .show();
                        break;
                    case 2:
                        if (activity instanceof MainActivity) {
                            ((MainActivity) activity).disconnect();
                        }
                        break;
                }
            }
        });
        return alertDialog.create();
    }

    public static AlertDialog getChatRequestDialog(final Activity activity, final DeviceDTO requesterDevice) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);

        String chatRequestTitle = activity.getString(R.string.chat_request_title);
        chatRequestTitle = String.format(chatRequestTitle, requesterDevice.getPlayerName() + "(" + requesterDevice.getDeviceName() + ")");
        alertDialog.setTitle(chatRequestTitle);
        String[] types = new String[]{"Accept", "Reject"};
        alertDialog.setItems(types, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                switch (i) {
                    case 0:
                        // request accepted
                        openChatActivity(activity, requesterDevice);
                        Toast.makeText(activity, "Chat requested accepted", Toast.LENGTH_SHORT)
                                .show();
                        DataSender.sendChatResponse(activity, requesterDevice.getIp(), requesterDevice.getPort(), true);
                        break;
                    case 1:
                        // request rejected
                        DataSender.sendChatResponse(activity, requesterDevice.getIp(), requesterDevice.getPort(), false);
                        Toast.makeText(activity, "Chat request rejected", Toast.LENGTH_SHORT)
                                .show();
                        break;
                }
            }
        });
        return alertDialog.create();
    }

    public static void openChatActivity(Activity activity, DeviceDTO device) {
        Intent intentChat = new Intent(activity, ChatActivity.class);
        intentChat.putExtra(ChatActivity.KEY_CHAT_IP, device.getIp());
        intentChat.putExtra(ChatActivity.KEY_CHAT_PORT, device.getPort());
        intentChat.putExtra(ChatActivity.KEY_CHATTING_WITH, device.getPlayerName());
        activity.startActivity(intentChat);
    }

}
