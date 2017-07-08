package cse4340.ble_application;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PairedDevices extends DeviceScanActivity {

    private static final String TAG = "MY_APP_DEBUG_TAG";
    private Handler mHandler; // handler that gets info from Bluetooth service

    private OutputStream outputStream;
    private InputStream inStream;

    ArrayList<BluetoothDevice> device_list;
    static ListView listView_paired;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paired_devices);

        Intent intent = getIntent();

        device_list = new ArrayList<BluetoothDevice>();
        findPairedDevices(device_list);
        listView_paired = (ListView) findViewById(R.id.paired_devices_list);

        MyListAdapter customPairAdapter = new MyListAdapter(PairedDevices.this, R.layout.paired_device_list_item, device_list);
        customPairAdapter.setCustomButtonListener(new MyListAdapter.customButtonListener() {
            @Override
            public void onButtonClickListener(int position, BluetoothDevice device, Button button_name) {
                Toast.makeText(PairedDevices.this, "Button " + button_name.getText().toString() + " clicked! " + position,
                        Toast.LENGTH_LONG).show();
            }
        });

        listView_paired.setAdapter(customPairAdapter);


    }

    public void findPairedDevices(ArrayList<BluetoothDevice> device_list) {

        pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            //paired_BTDeviceListAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,android.R.id.text1, pairedDevices.toArray());

            for (BluetoothDevice device : pairedDevices) {
                device_list.add(device);

                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d("PairedDevices","You have paired with Name: " + deviceName + " MAC: " + device.getAddress());
            }
        }
    }

    public void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class MyListAdapter extends ArrayAdapter<BluetoothDevice> {

        private List<BluetoothDevice> devices;
        private int layoutResourceId;
        private Context context;

        customButtonListener customListener;

        public interface customButtonListener {
            public void onButtonClickListener(int position, BluetoothDevice device, Button button_name);
        }
        public void setCustomButtonListener(customButtonListener listener) {
            this.customListener = listener;
        }

        public MyListAdapter(Context context, int layoutResourceId, List<BluetoothDevice> devices) {
            super(context, layoutResourceId, devices);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.devices = devices;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View row = convertView;
            DeviceHolder holder;

            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new DeviceHolder();
            holder.paired_device = devices.get(position);

            holder.send_button = (Button)row.findViewById(R.id.send_button);
            holder.unpair_button = (Button)row.findViewById(R.id.unpair_button);

            holder.device_name = (TextView)row.findViewById(R.id.device_name);
            holder.device_mac = (TextView)row.findViewById(R.id.device_mac);

            final BluetoothDevice temp = getItem(position);

            final View finalRow = row;
            holder.send_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(customListener != null){
                        customListener.onButtonClickListener(position, temp, (Button) finalRow.findViewById(R.id.send_button));
                    }
                }
            });

            holder.unpair_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(customListener != null){
                        customListener.onButtonClickListener(position, temp, (Button) finalRow.findViewById(R.id.unpair_button));
                    }
                }
            });

            row.setTag(holder);

            setupItem(holder);
            return row;
        }

        private void setupItem(DeviceHolder holder) {
            holder.device_name.setText(holder.paired_device.getName());
            holder.device_mac.setText(holder.paired_device.getAddress());

        }

        public class DeviceHolder {
            BluetoothDevice  paired_device;
            TextView device_name;
            TextView device_mac;
            Button send_button;
            Button unpair_button;
        }
    }

    // Defines several constants used when transmitting messages between the
    // service and the UI.
    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

        // ... (Add other message types here as needed.)
    }

    private void init() throws IOException {
        BluetoothAdapter blueAdapter = BluetoothAdapter.getDefaultAdapter();
        if (blueAdapter != null) {
            if (blueAdapter.isEnabled()) {
                Set<BluetoothDevice> bondedDevices = blueAdapter.getBondedDevices();

                if(bondedDevices.size() > 0) {
                    Object[] devices = (Object []) bondedDevices.toArray();
                    BluetoothDevice device = (BluetoothDevice) devices[0];
                    ParcelUuid[] uuids = device.getUuids();
                    BluetoothSocket socket = device.createRfcommSocketToServiceRecord(uuids[0].getUuid());
                    socket.connect();
                    outputStream = socket.getOutputStream();
                    inStream = socket.getInputStream();
                }

                Log.e("error", "No appropriate paired devices.");
            } else {
                Log.e("error", "Bluetooth is disabled.");
            }
        }
    }

    public void write(String s) throws IOException {
        outputStream.write(s.getBytes());
    }

    public void run() {
        final int BUFFER_SIZE = 1024;
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytes = 0;
        int b = BUFFER_SIZE;

        while (true) {
            try {
                bytes = inStream.read(buffer, bytes, BUFFER_SIZE - bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
