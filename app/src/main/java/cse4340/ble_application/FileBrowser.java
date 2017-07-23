package cse4340.ble_application;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Target;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.Channel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import static android.content.ContentValues.TAG;

public class FileBrowser extends Activity {

    String remoteDeviceAddress;

    ArrayList<String> deviceAddressList;

    String TargetStringAddress;
    int PORT_NUMBER = 8888;
    int FILE_TRANSFER_PORT = 7777;

    File file_location  =  new File(Environment.getExternalStorageDirectory() + "/shared");
    String file_names = "";
    private ListView file_list;
    private ArrayAdapter<String> file_array_adapter;
    private ArrayList<String> file_info;

    boolean sentFileName = false;
    private boolean receivedFile = false;
    boolean gotData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_browser);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        Bundle bundle = getIntent().getExtras();
        remoteDeviceAddress = bundle.getString("ipAddress").trim();

        file_info = new ArrayList<>();
        file_list = (ListView) findViewById(R.id.remoteDirectory_files);
        file_array_adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,android.R.id.text1,file_info);
        file_list.setAdapter(file_array_adapter);


        Log.d("IPAddress", "The ip address of the remote device" + remoteDeviceAddress);

        final FileServerAsyncTask fileServerRequest = new FileServerAsyncTask(file_list,file_array_adapter , file_info);

        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {

                fileServerRequest.doInBackground();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        file_info = fileServerRequest.getArrayList();
                        file_array_adapter.notifyDataSetChanged();

                    }
                });

            }

        });
        serverThread.start();


        Thread clientThread = new Thread(new Runnable() {
            @Override
            public void run() {
                setupClient(remoteDeviceAddress, PORT_NUMBER);
            }
        });

        clientThread.start();


        final FileServerAsyncTaskRetrieve fileServerRetrieve = new FileServerAsyncTaskRetrieve();
        Thread serverFileRetrieve = new Thread(new Runnable() {
            @Override
            public void run() {
                fileServerRetrieve.doInBackground();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        gotData = fileServerRetrieve.onPostExecute();
                        if(gotData){
                            Toast.makeText(FileBrowser.this, "File Transfer complete! Remote Location: " + file_location.toString(), Toast.LENGTH_LONG).show();
                        }


                    }
                });

            }
        });
        serverFileRetrieve.start();



        file_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {


                final String file = file_info.get(i);

                Thread sendFileNameThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendFileName(remoteDeviceAddress, FILE_TRANSFER_PORT, file);

                    }
                });

                sendFileNameThread.start();

                if(gotData){
                    Toast.makeText(FileBrowser.this, "File Transfer complete! Remote Location: " + file_location.toString(), Toast.LENGTH_LONG).show();
                }

            }
        });




    }

    @Override
    public void onResume() {
        super.onResume();
        file_array_adapter.notifyDataSetChanged();


    }

    private void getFile(Socket socket, String file_name) throws IOException {

        FileOutputStream fo;
        OutputStream outputStream;
        byte[] bytes = new byte[1024];

        int filesize=32768; // filesize temporary hardcoded

        long start = System.currentTimeMillis();
        int bytesRead;
        int current = 0;


        Log.d("receiveFileNameClient","recieving file....");
        // receive file
        byte [] mybytearray  = new byte [filesize];
        InputStream is = socket.getInputStream();
        FileOutputStream fos = new FileOutputStream(file_location + "/" + file_name); // destination path and name of file
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        bytesRead = is.read(mybytearray,0,mybytearray.length);
        current = bytesRead;

        // thanks to A. Cádiz for the bug fix
        do {
            bytesRead =
                    is.read(mybytearray, current, (mybytearray.length-current));
            if(bytesRead >= 0) current += bytesRead;
        } while(bytesRead > -1);

        bos.write(mybytearray, 0 , current);
        bos.flush();

        receivedFile = true;
        bos.close();

    }

    private void sendFileName(String remote_address, int port, String file_name) {
        Socket socket = new Socket();
        FileOutputStream fo;
        OutputStream outputStream;
        byte[] bytes = new byte[1024];

        try {
            /**
             * Create a client socket with the host,
             * port, and timeout information.
             */
            Log.d("sendFileNameClient","client connecting to server " + remoteDeviceAddress);
            socket.bind(null);
            socket.connect((new InetSocketAddress(remote_address, port)), 1000);
            Log.d("sendFileNameClient","Client Connected!");

            outputStream = socket.getOutputStream();
            outputStream.write(file_name.getBytes());
            sentFileName = true;

            if(sentFileName){
                while(true && !receivedFile){
                    getFile(socket,file_name);

                }

            }

            Log.d("sendFileNameClient","Closing client connection...");
            socket.close();



/*
            File targetFile = new File(file_location.getPath() +"/" + file_name);
            Log.d("sendFileClient","filePath: " + file_location.getPath() +"/" + file_name);

            Log.d("SharedDirectoryClient", "File " + targetFile.getName());
            byte[] mybytearray = new byte[(int) targetFile.length()];

            FileInputStream fis = new FileInputStream(targetFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            //bis.read(mybytearray, 0, mybytearray.length);

            DataInputStream dis = new DataInputStream(bis);
            dis.readFully(mybytearray, 0, mybytearray.length);

            OutputStream os = socket.getOutputStream();

            //Sending file name and file size to the server
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeUTF(targetFile.getName());
            dos.writeLong(mybytearray.length);
            dos.write(mybytearray, 0, mybytearray.length);
            dos.flush();

            //Sending file data to the server
            os.write(mybytearray, 0, mybytearray.length);
            os.flush();
*/
            //Closing socket


        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }


        /**
             * Create a byte stream from a JPEG file and pipe it to the output stream
             * of the socket. This data will be retrieved by the server device.
             */
        /*
            OutputStream outputStream = socket.getOutputStream();
            ContentResolver cr = context.getContentResolver();
            InputStream inputStream = null;
            inputStream = cr.openInputStream(Uri.parse("path/to/picture.jpg"));

            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
            inputStream.close();
            */


        /**
         * Clean up any open sockets when done
         * transferring or if an exception occurred.
         */
        finally {
            if (socket != null) {
                if (socket.isConnected()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        //catch logic
                    }
                }
            }
        }





    }

    public void getFileList(String dirLocation) {
        File dir = new File(dirLocation);
        File[] fileList = dir.listFiles();

        for (int i = 0; i < fileList.length; i++) {
            if (fileList[i].isFile()) {
                file_names = file_names + fileList[i].getName() + "\n";
                Log.d("SharedDirectoryClient", file_names);
                Log.d("SharedDirectoryClient", "File " + fileList[i].getName());
            } else if (fileList[i].isDirectory()) {
                Log.d("SharedDirectoryClient", "Directory " + fileList[i].getName());
            }


        }

    }

    @TargetApi(23)
    private void setupClient(String host, int port) {

        int len;
        Socket socket = new Socket();
        byte buf[]  = new byte[1024];

        try {
            /**
             * Create a client socket with the host,
             * port, and timeout information.
             */
            Log.d("setupClient","client connecting to server " + remoteDeviceAddress);
            socket.bind(null);
            socket.connect((new InetSocketAddress(host, port)), 1000);
            Log.d("setupClient","Client Connected!");


            createTestFile();

            getFileList(file_location.toString());

            Log.d("fileListClient","Sending file list..." + file_names);
            OutputStream outputStream = socket.getOutputStream();

            outputStream.write(file_names.getBytes());
            outputStream.close();

            /**
             * Create a byte stream from a JPEG file and pipe it to the output stream
             * of the socket. This data will be retrieved by the server device.
             */
        /*
            OutputStream outputStream = socket.getOutputStream();
            ContentResolver cr = context.getContentResolver();
            InputStream inputStream = null;
            inputStream = cr.openInputStream(Uri.parse("path/to/picture.jpg"));

            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
            inputStream.close();
            */
        } catch (FileNotFoundException e) {
            //catch logic
        } catch (IOException e) {
            //catch logic
        }

        /**
         * Clean up any open sockets when done
         * transferring or if an exception occurred.
         */
                finally {
                    if (socket != null) {
                        if (socket.isConnected()) {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                //catch logic
                            }
                        }
                    }
                }


    }

    public void createTestFile() throws IOException {

        // Create a file in the Internal Storage

        Log.d("ExternalStorageLocation","making directory" + Environment.getExternalStorageDirectory() + "/shared");
        file_location.mkdir();

        File file = new File(file_location + File.separator + android.os.Build.MODEL + ".txt");
        file.createNewFile();

        String data1="Hello world!";
        //write the bytes in file

        OutputStream fo = new FileOutputStream(file);
        fo.write(data1.getBytes());
        fo.close();

    }

    public boolean dir_exists(String dir_path)
    {
        boolean ret = false;
        File dir = new File(dir_path);
        if(dir.exists() && dir.isDirectory())
            ret = true;
        return ret;
    }

    public boolean file_exists(String dir_path)
    {
        boolean ret = false;
        File dir = new File(dir_path);
        if(dir.exists() && dir.isFile())
            ret = true;
        return ret;
    }

    public static String getIPFromMac(String MAC) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {

                String[] splitted = line.split(" +");
                if (splitted != null && splitted.length >= 4) {
                    // Basic sanity check
                    String device = splitted[5];
                    if (device.matches(".*p2p-p2p0.*")){
                        String mac = splitted[3];
                        if (mac.matches(MAC)) {
                            return splitted[0];
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}

