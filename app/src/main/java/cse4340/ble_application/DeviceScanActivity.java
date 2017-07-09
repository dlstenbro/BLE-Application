package cse4340.ble_application;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.content.ContentValues.TAG;

public class DeviceScanActivity extends Activity {

    private static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 1;

    private final static int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 30000;
    private static final long DISCOVERABLE_DURATION = 30000;
    private static final int REQUEST_CODE_LOC = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1 ;
    private boolean mScanning;
    private Handler mHandler;

    ListView listView;

    BluetoothAdapter mBluetoothAdapter;
    ArrayAdapter<BluetoothDevice> mBTDeviceListAdapter;

    ArrayList<BluetoothDevice> mBTDevices;

    Set<BluetoothDevice> pairedDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);
        Intent intent = getIntent();

        listView = (ListView) findViewById(R.id.list);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_DURATION);
        startActivity(discoverableIntent);

        startBTService();

        mBTDevices = new ArrayList<BluetoothDevice>();

        // Assign adapter to ListView
        mBTDeviceListAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,android.R.id.text1,mBTDevices);
        listView.setAdapter(mBTDeviceListAdapter);
        mBluetoothAdapter.startDiscovery();

        // ListView Item Click Listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                mBluetoothAdapter.cancelDiscovery();
                BluetoothDevice bluetoothDevice = mBTDeviceListAdapter.getItem(position);
                Log.d("Listview", "You pressed MAC: " + bluetoothDevice.getName());

                pairDevice(bluetoothDevice);

                IntentFilter intent = new IntentFilter(bluetoothDevice.ACTION_BOND_STATE_CHANGED);
                registerReceiver(mReceiver, intent);
                Toast.makeText(DeviceScanActivity.this, "Initiating connection with "+ "Name: " + bluetoothDevice.getName(), Toast.LENGTH_SHORT).show();

            }

        });

    }

    private void pairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Log.d("BroadcastReceiver","finding devices...");

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d("BroadcastReceiver","Found Device! Name: " + deviceName + " MAC: " + device.getAddress());
                mBTDeviceListAdapter.add(device);
            }

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state        = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState    = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    Toast.makeText(DeviceScanActivity.this, "Paired!", Toast.LENGTH_LONG);

                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                    Toast.makeText(DeviceScanActivity.this, "Unpaired!", Toast.LENGTH_LONG);
                }

            }
        }
    };

    @Override
    protected void onDestroy() {

        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(mReceiver);

    }

    private void startBTService() {

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
        else {
            Toast.makeText(DeviceScanActivity.this, "Cannot Start BlueTooth service or Device is not compatible!" , Toast.LENGTH_SHORT).show();
            System.exit(0); // close the application if device does not support ble or is not compatible
        }

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }

    private boolean checkDeviceExists(BluetoothDevice device) {
        for(int i = 0; i < mBTDeviceListAdapter.getCount(); i++){
            if(device.toString().equals(mBTDeviceListAdapter.getItem(i).toString()))
            {
                return true;
            }
        }
        return false;
    }




}

