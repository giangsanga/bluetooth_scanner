package com.example.final_gg;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class BluetoothDeviceAdapter extends RecyclerView.Adapter<BluetoothDeviceAdapter.ViewHolder> {

    private List<BluetoothDevice> devices = new ArrayList<>();
    private List<Integer> rssiValues = new ArrayList<>();
    private Context context;

    public BluetoothDeviceAdapter(Context context) {
        this.context = context;
    }

    public void setDevices(List<BluetoothDevice> devices, List<Integer> rssiValues) {
        this.devices = devices;
        this.rssiValues = rssiValues;
        notifyDataSetChanged();
    }

    public void addDevice(BluetoothDevice device, int rssi) {
        if (!devices.contains(device)) {
            devices.add(device);
            rssiValues.add(rssi);
            notifyItemInserted(devices.size() - 1);
        } else {
            int index = devices.indexOf(device);
            rssiValues.set(index, rssi);
            notifyItemChanged(index);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bluetooth_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BluetoothDevice device = devices.get(position);
        int rssi = rssiValues.get(position);

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            holder.deviceName.setText(device.getName());
        } else {
            holder.deviceName.setText("Unknown Device");
        }

        holder.deviceAddress.setText(device.getAddress());
        holder.deviceRssi.setText("RSSI: " + rssi + " dBm");
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRssi;

        ViewHolder(View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.device_name);
            deviceAddress = itemView.findViewById(R.id.device_address);
            deviceRssi = itemView.findViewById(R.id.device_rssi);
        }
    }
}