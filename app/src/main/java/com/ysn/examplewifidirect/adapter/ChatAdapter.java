/*
 * Created by YSN Studio on 5/23/18 2:52 PM
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified 5/23/18 2:52 PM
 */

package com.ysn.examplewifidirect.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ysn.examplewifidirect.R;
import com.ysn.examplewifidirect.model.ChatDTO;

import java.io.File;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatHolder> {

    private Context context;
    private List<ChatDTO> chatList;

    public ChatAdapter(Context context, List<ChatDTO> chatList) {
        this.context = context;
        this.chatList = chatList;
    }

    @NonNull
    @Override
    public ChatHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_mine, parent, false);
            return new ChatHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_other, parent, false);
            return new ChatHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ChatHolder holder, int position) {
        ChatDTO chatDTO = chatList.get(position);
        boolean isImageMessage = chatDTO.isImageMessage();
        if (isImageMessage) {
            File fileImage = new File(chatDTO.getMessage());
            if (fileImage.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(fileImage.getAbsolutePath());
                holder.imageViewMessageChat.setImageBitmap(bitmap);
            } else {
                holder.imageViewMessageChat.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_launcher_background));
            }
            holder.textViewMessageChat.setVisibility(View.GONE);
            holder.imageViewMessageChat.setVisibility(View.VISIBLE);
        } else {
            holder.textViewMessageChat.setText(chatDTO.getMessage());
            holder.textViewMessageChat.setVisibility(View.VISIBLE);
            holder.imageViewMessageChat.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        ChatDTO chatDTO = chatList.get(position);
        return chatDTO.isMyChat() ? 0 : 1;
    }

    class ChatHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.text_view_message_chat)
        TextView textViewMessageChat;
        @BindView(R.id.image_view_message_chat)
        ImageView imageViewMessageChat;

        public ChatHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

}
