/*
 * Created by YSN Studio on 5/23/18 2:52 PM
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified 5/23/18 2:52 PM
 */

package com.ysn.examplewifidirect.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ysn.examplewifidirect.R;
import com.ysn.examplewifidirect.model.DeviceDTO;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AdapterDevice extends RecyclerView.Adapter<AdapterDevice.ViewHolder> {

    private ArrayList<DeviceDTO> deviceDTOS;
    private ListenerAdapterDevice listenerAdapterDevice;

    public AdapterDevice(ArrayList<DeviceDTO> deviceDTOS, ListenerAdapterDevice listenerAdapterDevice) {
        this.deviceDTOS = deviceDTOS;
        this.listenerAdapterDevice = listenerAdapterDevice;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, null);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeviceDTO deviceDTO = deviceDTOS.get(position);
        holder.textViewDeviceName.setText(deviceDTO.getPlayerName() + "-" + deviceDTO.getDeviceName());
    }

    @Override
    public int getItemCount() {
        return deviceDTOS.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.text_view_device_name)
        TextView textViewDeviceName;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @OnClick({
                R.id.text_view_device_name
        })
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.text_view_device_name:
                    listenerAdapterDevice.onClickItemDevice(deviceDTOS.get(getAdapterPosition()));
                    break;
            }
        }
    }

    public interface ListenerAdapterDevice {

        void onClickItemDevice(DeviceDTO deviceDTO);

    }

}
