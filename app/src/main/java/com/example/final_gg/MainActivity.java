package com.example.final_gg;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_ENABLE_BT = 1; // Use a non-zero value
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 123;

    private TextView mStatusBleTv;
    private ImageView mBlueIV;
    private Button mOnBtn, mPauseBtn, mDiscoverBtn, mPairedBtn;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothDeviceAdapter deviceAdapter;

    private ActivityResultLauncher<Intent> enableBluetoothLauncher;
    private boolean isPaused = false;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        mBlueIV.setImageResource(R.drawable.bluetooth_off);
                        showToast("Bluetooth is turned off");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        showToast("Bluetooth is turning off...");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        mBlueIV.setImageResource(R.drawable.bluetooth_on);
                        showToast("Bluetooth is on");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        showToast("Bluetooth is turning on...");
                        break;
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                deviceAdapter.setDevices(new ArrayList<>(), new ArrayList<>());
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // Do nothing
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Không thêm thiết bị được phát hiện vào danh sách khi nhấn nút "Get Paired Devices"
                if (!isPaused) {
                    BluetoothDevice device;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                    } else {
                        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    }
                    int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                    if (device != null) {
                        deviceAdapter.addDevice(device, rssi);
                    }
                }
            }
        }
    };

    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (!isPaused) {
                BluetoothDevice device = result.getDevice();
                int rssi = result.getRssi();
                deviceAdapter.addDevice(device, rssi);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            if (!isPaused) {
                for (ScanResult result : results) {
                    BluetoothDevice device = result.getDevice();
                    int rssi = result.getRssi();
                    deviceAdapter.addDevice(device, rssi);
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE Scan Failed with code " + errorCode);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStatusBleTv = findViewById(R.id.statusBluetoothTv);
        mBlueIV = findViewById(R.id.bluetoothIv);
        mOnBtn = findViewById(R.id.onButn);
        mPauseBtn = findViewById(R.id.pauseButn);
        mDiscoverBtn = findViewById(R.id.discoverableBtn);
        mPairedBtn = findViewById(R.id.PairedBtn);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        deviceAdapter = new BluetoothDeviceAdapter(this);
        recyclerView.setAdapter(deviceAdapter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
        } else {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        enableBluetoothLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        mBlueIV.setImageResource(R.drawable.bluetooth_on);
                    } else {
                        showToast("Bluetooth is Off");
                    }
                }
        );

        if (bluetoothAdapter == null) {
            mStatusBleTv.setText("Bluetooth is not available");
        } else {
            mStatusBleTv.setText("Bluetooth is available");

            if (bluetoothAdapter.isEnabled()) {
                mBlueIV.setImageResource(R.drawable.bluetooth_on);
            } else {
                mBlueIV.setImageResource(R.drawable.bluetooth_off);
            }

            mOnBtn.setOnClickListener(v -> {
                stopDiscovery();
                if (!bluetoothAdapter.isEnabled()) {
                    showToast("Turning on Bluetooth..");
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSIONS);
                                return;
                            }
                        }
                        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        enableBluetoothLauncher.launch(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Error enabling Bluetooth", e);
                        showToast("Error enabling Bluetooth: " + e.getMessage());
                    }
                } else {
                    showToast("Bluetooth is already on");
                }
            });

            mDiscoverBtn.setOnClickListener(v -> {
                stopDiscovery();
                if (!bluetoothAdapter.isEnabled()) {
                    showToast("Please enable Bluetooth first");
                    return;
                }

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // Tiếp tục công việc quét
                    isPaused = false;
                    startDiscovery();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    }, REQUEST_BLUETOOTH_PERMISSIONS);
                }
            });

            mPauseBtn.setOnClickListener(v -> {
                if (isPaused) {
                    isPaused = false;
                    mPauseBtn.setText("Pause");
                    showToast("Resuming discovery updates...");
                } else {
                    isPaused = true;
                    mPauseBtn.setText("Resume");
                    showToast("Pausing discovery updates...");
                }
            });

            mPairedBtn.setOnClickListener(v -> {
                // Dừng tất cả hoạt động làm mới
                stopDiscovery();
                isPaused = true;

                // Làm mới danh sách thiết bị
                deviceAdapter.setDevices(new ArrayList<>(), new ArrayList<>());

                if (bluetoothAdapter.isEnabled()) {
                    // Lấy danh sách các thiết bị đã kết nối
                    List<BluetoothDevice> pairedDevices = new ArrayList<>(bluetoothAdapter.getBondedDevices());
                    List<Integer> rssiValues = new ArrayList<>();
                    for (int i = 0; i < pairedDevices.size(); i++) {
                        rssiValues.add(0); // Giá trị RSSI mặc định cho các thiết bị đã kết nối
                    }
                    deviceAdapter.setDevices(pairedDevices, rssiValues);
                } else {
                    showToast("Turn On bluetooth to get paired devices");
                }
            });

            // Đăng ký các broadcast khi phát hiện thiết bị
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(receiver, filter);
        }
    }

    private void startDiscovery() {
        try {
            // Làm mới danh sách thiết bị và bắt đầu quét
            deviceAdapter.setDevices(new ArrayList<>(), new ArrayList<>());
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            bluetoothAdapter.startDiscovery();
            bluetoothLeScanner.startScan(leScanCallback);
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for Bluetooth scan", e);
        }
    }

    private void stopDiscovery() {
        try {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothLeScanner.stopScan(leScanCallback);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for stopping Bluetooth scan", e);
        }
    }

    private void appendDeviceToTextView(BluetoothDevice device) {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                deviceAdapter.addDevice(device, 0); // Giá trị RSSI mặc định
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSIONS);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for Bluetooth connect", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Kích hoạt lại hành động yêu cầu quyền
                if (permissions[0].equals(Manifest.permission.BLUETOOTH_SCAN) || permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    startDiscovery();
                } else if (permissions[0].equals(Manifest.permission.BLUETOOTH_CONNECT)) {
                    if (!bluetoothAdapter.isEnabled()) {
                        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        enableBluetoothLauncher.launch(intent);
                    }
                }
            } else {
                showToast("Bluetooth permissions denied");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hủy đăng ký broadcast receiver
        try {
            if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothLeScanner.stopScan(leScanCallback);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for Bluetooth scan", e);
        }
        unregisterReceiver(receiver);
    }

    private void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
}