package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import static java.lang.Boolean.FALSE;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 */
public class GroupMessengerActivity extends Activity {

    private static final Boolean DEBUG = FALSE;

    private static final String TAG = GroupMessengerActivity.class.getSimpleName();
    private static final String KEY_COLUMN_NAME = "key";
    private static final String VALUE_COLUMN_NAME = "value";

    static final int[] REMOTE_PORTS = {11108, 11112, 11116, 11120, 11124};
    static final int SERVER_PORT = 10000;

    private TextView mTextView;
    private Button mButtonPTest, mSendButton;
    private EditText mEditText;

    static int messageCounter = 0;

    private ContentResolver mContentResolver;

    private Uri mUri;

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");

        mContentResolver = getContentResolver();
        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        mTextView = (TextView) findViewById(R.id.textView1);
        mTextView.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        mButtonPTest = (Button) findViewById(R.id.button1);
        mButtonPTest.setOnClickListener(new OnPTestClickListener(mTextView, getContentResolver()));

        mEditText = (EditText) findViewById(R.id.editText1);
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        mSendButton = (Button) findViewById(R.id.button4);

        mSendButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = mEditText.getText().toString() + "\n";
                mEditText.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
            }
        });

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    /***
     * ServerTask is an AsyncTask that should handle incoming messages. It is created by
     * ServerTask.executeOnExecutor() call in SimpleMessengerActivity.
     * <p>
     * Please make sure you understand how AsyncTask works by reading
     * http://developer.android.com/reference/android/os/AsyncTask.html
     *
     * @author stevko
     */
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            // Why while(true) ? Because the server needs to be running.
            // Moreover if we wont add this loop the AsyncTask will end and server wont listen to any of the messages as soon as application is launched.
            // This is not a good way to handle the server I suppose. Considering the case of multiple clients.
            // This will block the UI thread. Here its working as the messages are short.
            // This is the job for Services. More functionality can be provided by them too like adding notifications.
            // Or maybe Handlers can do a better job than AsyncTask.

            // NOTE : This is not thread safe too. Since here only one client is there its working fine.
            // But multi client will cause race condition.
            // References : 1) https://docs.oracle.com/javase/tutorial/networking/sockets/index.html ( How Server Sockets work )
            //              2) http://developer.android.com/reference/android/os/AsyncTask.html ( Read AsyncTask Life Cycle )
            //              3) https://docs.oracle.com/javase/tutorial/essential/concurrency/locksync.html
            //              4) https://developer.android.com/guide/topics/providers/content-provider-basics.html
            //              5) https://developer.android.com/guide/topics/providers/content-provider-creating.html
            try {
                synchronized (this) {
                    while (true) {

                        Socket clientSocket = serverSocket.accept();

                        OutputStream serverSocketOutputStream = clientSocket.getOutputStream();
                        InputStreamReader serverSocketInputStreamReader = new InputStreamReader(clientSocket.getInputStream());

                        PrintWriter serverOutputPrintWriter = new PrintWriter(serverSocketOutputStream, true);
                        BufferedReader serverInputBufferedReader = new BufferedReader(serverSocketInputStreamReader);

                        String msgFromClient;

                        while ((msgFromClient = serverInputBufferedReader.readLine()) != null) {
                            if (DEBUG) {
                                Log.e(TAG, "Server Side Msg " + msgFromClient.replace("\n", ""));
                            }
                            if (msgFromClient.equals("SYN")) {
                                serverOutputPrintWriter.println("SYN+ACK");
                            } else if (msgFromClient.equals("ACK")) {
                                serverOutputPrintWriter.println("ACK");
                            } else if (msgFromClient.equals("STOP")) {
                                serverOutputPrintWriter.println("STOPPED");
                                break;
                            } else {
                                if (msgFromClient.length() != 0) {
                                    ContentValues contentValues = new ContentValues();
                                    contentValues.put(KEY_COLUMN_NAME, Integer.toString(messageCounter));
                                    contentValues.put(VALUE_COLUMN_NAME, msgFromClient.trim());
                                    mContentResolver.insert(mUri, contentValues);
                                    messageCounter++;
                                    publishProgress(msgFromClient);
                                    serverOutputPrintWriter.println("OK");
                                }
                            }
                        }
                        serverSocketOutputStream.close();
                        serverInputBufferedReader.close();
                        serverOutputPrintWriter.close();
                        serverSocketInputStreamReader.close();
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "ServerTask socket IOException : " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                Log.e(TAG, "ServerTask socket Exception : " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        }

        protected void onProgressUpdate(String... strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            mTextView.append(strReceived + "\n");
        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko and vkumar25
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            for (int remotePort : REMOTE_PORTS) {

                try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), remotePort);

                    String msgToSend = msgs[0];

                    // Read and Writes data to the socket.
                    // References : https://docs.oracle.com/javase/tutorial/networking/sockets/index.html ( How Sockets work )

                    OutputStream clientOutputStream = socket.getOutputStream();
                    InputStreamReader clientInputStreamReader = new InputStreamReader(socket.getInputStream());

                    PrintWriter clientOutputPrintWriter = new PrintWriter(clientOutputStream, true);
                    BufferedReader clientInputBufferReader = new BufferedReader(clientInputStreamReader);

                    String msgFromServer;

                    clientOutputPrintWriter.println("SYN");

                    while ((msgFromServer = clientInputBufferReader.readLine()) != null) {
                        if (DEBUG) {
                            Log.e(TAG, "Client Side msg " + msgFromServer);
                        }
                        if (msgFromServer.equals("SYN+ACK")) {
                            clientOutputPrintWriter.println("ACK");
                        } else if (msgFromServer.equals("ACK")) {
                            clientOutputPrintWriter.println(msgToSend);
                        } else if (msgFromServer.equals("OK")) {
                            clientOutputPrintWriter.println("STOP");
                        } else if (msgFromServer.equals("STOPPED")) {
                            break;
                        }
                    }
                    clientOutputPrintWriter.close();
                    clientInputBufferReader.close();
                    socket.close();
                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException");
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
}
