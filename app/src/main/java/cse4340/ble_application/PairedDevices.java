package cse4340.ble_application;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.provider.SyncStateContract;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static android.widget.Toast.LENGTH_LONG;

public class PairedDevices extends DeviceScanActivity {

    private static final String TAG = "MY_APP_DEBUG_TAG";

    private OutputStream mainOutputStream;
    private InputStream mainInStream;

    BluetoothSocket mainSocket;
    ArrayList<BluetoothDevice> device_list;

    static ListView listView_paired;

    ServerThread acceptThread;
    String receivedMessage;
    TextView receivedText;

    /*
            When the user goes to the "Paired Devices" Screen, get a list of devices and show two buttons, send and receive button and unpair
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paired_devices);

        device_list = new ArrayList<BluetoothDevice>();
        findPairedDevices(device_list);
        listView_paired = (ListView) findViewById(R.id.paired_devices_list);

        MyListAdapter customPairAdapter = new MyListAdapter(PairedDevices.this, R.layout.paired_device_list_item, device_list);
        receivedText = (TextView) findViewById(R.id.receivedText);

        // create a new thread for accepting connections
        acceptThread = new ServerThread();

        // create another thread for the object above, the wait for messages to come in from device.
        Thread listenThread = new Thread(new Runnable() {

            public void run() {
                acceptThread.run();
                acceptThread.start();

            }
        });
        listenThread.start();

        // custom adapter that will hold our list and our button configuration.
        customPairAdapter.setCustomButtonListener(new MyListAdapter.customButtonListener() {
            @Override
            public void onButtonClickListener(int position, final BluetoothDevice device, Button button_name) {

                // if the use unpairs from the device, then start the activity again
                if (button_name.getText().toString().contains("Unpair")) {
                    unpairDevice(device);
                    device_list.remove(device);
                    finish();
                    startActivity(getIntent());

                }

                // When the user hits the send and receive button, then listen for incoming strings.
                if (button_name.getText().toString().contains("Send and Receive")) {

                    final ClientThread newConnect = new ClientThread(device);

                    Thread connectThread = new Thread(new Runnable() {

                        public void run() {
                            Log.d("connectThread", "listening for strings....");
                            newConnect.run();
                            //newConnect.start();

                        }
                    });
                    connectThread.start();

                    // if a message is received, then update the "received text" view on the activity.
                    if(receivedMessage != null){
                        Log.d("TextviewReceivedMessage", "updating Text view to say " + receivedMessage);
                        receivedText.append(" " + receivedMessage);
                        receivedText.invalidate();
                    }

                }

            }

        });

        listView_paired.setAdapter(customPairAdapter);
    }

    // array that will search for paired devices in the area.
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

    // unpair the device by removing the bond.
    public void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // custom list adapter that will hold each pair, with two buttons, "unpair" and "Send and Receive"
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

  //  public void write(String s) throws IOException {
  //      outputStream.write(s.getBytes());
   // }

    // When the device connects, then each device will create a server thread which will listen for incoming connections
    private class ServerThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public ServerThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("BlueToothApp", UUID.fromString("d52fd4f1-ee63-4aed-aa8e-b0ca744a3348"));
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                Log.d("AcceptThread", "waiting to accept connection...");
                try {

                    socket = mmServerSocket.accept();
                    Log.d("AcceptThread", "Accepted connection!!!");
                } catch (IOException e) {
                    Log.e("AcceptThread", "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.

                    writeThread(socket);


                    try {

                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    // When the device server socket opens, then each device will create a client thread which will lsend string data to the other device.
    private class ClientThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ClientThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = mmDevice.createRfcommSocketToServiceRecord(UUID.fromString("d52fd4f1-ee63-4aed-aa8e-b0ca744a3348"));
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();

            Log.d("ConnectThread","Looking for device....");

            try {
                Log.d("ConnectThread","Connected to " + mmDevice + "!");
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();

                mainSocket = mmSocket;

              } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.


            final IOThread listeningioThread = new IOThread(mmSocket);

            Thread listener = new Thread(new Runnable() {
                @Override
                 public void run() {
                      Log.d("listeningIOThreadRun", "listening IO Thread....");
                    listeningioThread.run();

                }
            });
            listener.start();



        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    // write a message to the device that's connected. The string will be "hello from ", then the device name
    private void writeThread(BluetoothSocket socket) {

        final IOThread writeioThread = new IOThread(socket);

        Thread writeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("AfternewIOThreadRun", "Attempting to write string...");
                writeioThread.write(("Hello from "+ android.os.Build.MODEL + "!").getBytes());

            }
        });
        writeThread.start();

    }

    // create a read and write thread that is seperate from the client thread (which will listen for incoming strings)
    // This thread will send and receive information back from each device.
    private class IOThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        final int BUFFER_SIZE = 1024;
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytes = 0;
        int b = BUFFER_SIZE;
        String message;

        public IOThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                Log.d("getInputStream", "getting input stream...");
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                Log.d("getOutputStream", "getting output stream...");
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {

            buffer = new byte[BUFFER_SIZE];
            int numBytes = 0; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {

                    Log.d("inBoundMessage", "reading string....");
                    //bytes = mmInStream.read(buffer, bytes, BUFFER_SIZE - bytes);
                    inBoundMessage(mmInStream);
                    //message = new String(buffer,"UTF-8");
                    //Toast.makeText(this,message,LENGTH_LONG);
                    //Log.d("inBoundMessage", message);
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                String sentMessage = new String(bytes, "UTF-8");
                Log.d("OutputStream", "Sending this message: " + sentMessage);
                // Share the sent message with the UI activity.

            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }


    private void outgoingMessage(final BluetoothSocket socket) throws IOException {

        Log.d("outgoingMessage", "sending string....");
        socket.getOutputStream().write(("Hello from "+ android.os.Build.MODEL + "!").getBytes());

    }

    // decode the incoming message by using a buffer and create a string of bytes received.
    private String inBoundMessage(final InputStream input) {

        final int BUFFER_SIZE = 1024;
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytes = 0;
        int b = BUFFER_SIZE;
        String message;


        // globally

        while (true) {
            try {
                if (input != null) {

                    Log.d("inBoundMessage", "reading string....");
                    bytes = input.read(buffer, bytes, BUFFER_SIZE - bytes);
                    message = new String(buffer,"UTF-8");

                    //Toast.makeText(this,message,LENGTH_LONG);
                    if(message != null || !message.startsWith("")){
                        //receivedText.setText(message);
                    }

                    Log.d("inBoundMessage", message);
                    receivedMessage = message;
                    //Toast.makeText(PairedDevices.this, receivedMessage, Toast.LENGTH_LONG).show();



                    //break;

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Toast.makeText(this, message, LENGTH_LONG);

    }

}
