// Android IP多点组播MulticastSocket
// https://blog.csdn.net/androiddeveloper_lee/article/details/9299135
package com.abc;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class GameActivity extends AppCompatActivity {

    EditText editTextAddress, editTextPort;
    Button mbtnConnect , mbtnCancel , mbtnConsole , mbtnStart;
    TextView textViewState, textViewRx;

    TCPServerHandler tcpServerHandler;
    TCPServerThread tcpServerThread;
    TCPServerSendThread tcpServerSendThread;
    TCpClientReadThread tcpClientReadThread;
    TCpClientSendThread tcpClientSendThread;

    ServerSocket serverSocket = null;
    Socket clientSocket = null;

    String localAddress;
    String serverAddress;
    int serverPort = 8080;
    List<String> playerAddress;
    ArrayList<Socket> socketList ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextAddress = (EditText) findViewById(R.id.address);
        editTextPort = (EditText) findViewById(R.id.port);
        mbtnConnect = (Button) findViewById(R.id.btnConnect);
        mbtnCancel = (Button) findViewById(R.id.btnCancel);
        mbtnConsole = (Button) findViewById(R.id.btnConsole);
        mbtnStart = (Button) findViewById(R.id.btnStart);
        textViewState = (TextView)findViewById(R.id.state);
        textViewRx = (TextView)findViewById(R.id.received);

        mbtnConnect.setOnClickListener(mbtnConnectOnClickListener);
        mbtnCancel.setOnClickListener(mbtnCancelOnClickListener);
        mbtnConsole.setOnClickListener(mbtnConsoleOnClickListener);
        mbtnStart.setOnClickListener(mbtnStartOnClickListener);

        localAddress = getLocalAddress();
        editTextAddress.setText(localAddress);
        editTextPort.setText(String.valueOf(serverPort));
        textViewState.setText(localAddress + "  ");

    }

    View.OnClickListener mbtnConnectOnClickListener = new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {

                    tcpServerHandler = new TCPServerHandler(GameActivity.this);
                    tcpClientReadThread = new TCpClientReadThread(
                            editTextAddress.getText().toString() , Integer.parseInt(editTextPort.getText().toString()) ,
                            tcpServerHandler);
                    tcpClientReadThread.start();

                    tcpClientSendThread = new TCpClientSendThread(
                            editTextAddress.getText().toString() , Integer.parseInt(editTextPort.getText().toString()) ,
                            tcpServerHandler);
                    tcpClientSendThread.start();

                    mbtnConnect.setEnabled(false);
                    mbtnCancel.setEnabled(true);
                    mbtnConsole.setEnabled(false);
                    mbtnStart.setEnabled(false);
                }
     };

    View.OnClickListener mbtnCancelOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View arg0) {

            if (playerAddress != null)
                playerAddress = null;

            if (tcpServerThread != null) {
                tcpServerThread.running = false;
                tcpServerThread = null;
            }
            if (tcpServerSendThread != null) {
                tcpServerSendThread.running = false;
                tcpServerSendThread = null;
            }
            if (tcpClientReadThread != null) {
                tcpClientReadThread.running = false;
                tcpClientReadThread = null;
            }
            if (tcpClientSendThread != null) {
                tcpClientSendThread.running = false;
                tcpClientSendThread = null;
            }
            if (tcpServerHandler != null)
                tcpServerHandler = null;

            if (clientSocket != null) {
                try {
                     clientSocket.close();
                     clientSocket = null;
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (serverSocket != null) {
                try {
                    serverSocket.close();
                    serverSocket = null;
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            textViewState.setText("");
            textViewState.setText(localAddress + "  ");
            textViewRx.setText("");

            mbtnConnect.setEnabled(true);
            mbtnCancel.setEnabled(false);
            mbtnConsole.setEnabled(true);
            mbtnStart.setEnabled(false);
        }
    };

    View.OnClickListener mbtnConsoleOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View arg0) {
            playerAddress = new ArrayList<>();
            playerAddress.add(localAddress);

            socketList = new ArrayList<>();

            tcpServerHandler = new TCPServerHandler(GameActivity.this);
            tcpServerThread = new TCPServerThread(
                    editTextAddress.getText().toString() , Integer.parseInt(editTextPort.getText().toString()) ,
                    tcpServerHandler);
            tcpServerThread.start();

            mbtnConnect.setEnabled(false);
            mbtnCancel.setEnabled(true);
            mbtnConsole.setEnabled(false);
            mbtnStart.setEnabled(true);
        }
    };

    View.OnClickListener mbtnStartOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View arg0) {
            tcpServerSendThread = new TCPServerSendThread(
                    editTextAddress.getText().toString() , Integer.parseInt(editTextPort.getText().toString()) ,
                    tcpServerHandler);
            tcpServerSendThread.start();
        }
    };
    private void updateState(String state){
        textViewState.setText(state);
        //textViewState.append(state + "\n");
    }

    private void updateRxMsg(String rxmsg){
        textViewRx.append(rxmsg + "\n");
    }

    private void clientEnd(){
        tcpServerThread = null;
        //textViewState.setText("clientEnd()");
        textViewState.append("clientEnd()" + "\n");
        mbtnConnect.setEnabled(true);

    }

    public static class TCPServerHandler extends Handler {
        public static final int UPDATE_STATE = 0;
        public static final int UPDATE_MSG = 1;
        public static final int UPDATE_END = 2;
        private GameActivity parent;

        public TCPServerHandler(GameActivity parent) {
            super();
            this.parent = parent;
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what){
                case UPDATE_STATE:
                    parent.updateState((String)msg.obj);
                    break;
                case UPDATE_MSG:
                    parent.updateRxMsg((String)msg.obj);
                    break;
                case UPDATE_END:
                    parent.clientEnd();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private class TCPServerThread extends Thread{

        String dstAddress;
        int dstPort;
        private boolean running;
        GameActivity.TCPServerHandler handler;

        public InetAddress address;

        public TCPServerThread(String addr, int port, GameActivity.TCPServerHandler handler) {
            super();
            dstAddress = addr;
            dstPort = port;
            this.handler = handler;
        }

        public void setRunning(boolean running){
            this.running = running;
        }

        private void sendState(String state){
            handler.sendMessage(
                    Message.obtain(handler,
                            GameActivity.TCPServerHandler.UPDATE_STATE, state));
        }

        @Override
        public void run() {
            sendState("connecting...");

                try {
                    Log.v("Socket" ,"before new ServerSocket(dstPort)");
                    serverSocket = new ServerSocket(dstPort);
                    serverSocket.setReceiveBufferSize(1024);
                    Log.v("Socket" ,"after new ServerSocket(dstPort)");
                    handler.sendMessage(
                            Message.obtain(handler, GameActivity.TCPServerHandler.UPDATE_MSG, "等待連線中..."));
                    running = true;

                    while (running) {

                        //接收連線
                        Socket client = serverSocket.accept();

                        playerAddress.add(client.getInetAddress().getHostAddress());
                        socketList.add(client);
                        handler.sendMessage(
                                Message.obtain(handler, GameActivity.TCPServerHandler.UPDATE_MSG, "已連線..."));

                        //接收資料
                        DataInputStream in = new DataInputStream(client.getInputStream());
                        String recvStr = in.readUTF();

                        if(recvStr != null){
                            //recvStr += client.getInetAddress().getHostAddress() + " : " + client.getPort();
                            handler.sendMessage(
                                    Message.obtain(handler, GameActivity.TCPServerHandler.UPDATE_MSG, recvStr ));

                            DataOutputStream out = new DataOutputStream(client.getOutputStream());
                            String outStr = "一些回應..." + "\n";
                            out.writeUTF(outStr);

                        }else{    //在此抓取的是使用使用強制關閉app的客戶端(會不斷傳null給server)
                                  //當socket強制關閉app時移除客戶端
                            socketList.remove(client);
                            break;    //跳出迴圈結束該執行緒
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    private class TCPServerSendThread extends Thread{

        String dstAddress;
        int dstPort;
        private boolean running;
        GameActivity.TCPServerHandler handler;

        public InetAddress address;


        public TCPServerSendThread(String addr, int port, GameActivity.TCPServerHandler handler) {
            super();
            dstAddress = addr;
            dstPort = port;
            this.handler = handler;
        }

        public void setRunning(boolean running){
            this.running = running;
        }

        private void sendState(String state){
            handler.sendMessage(
                    Message.obtain(handler,
                            GameActivity.TCPServerHandler.UPDATE_STATE, state));
        }

        @Override
        public void run() {

            if (socketList.size() == 0 || serverSocket == null)
                return;

            running = true;

            try {
                 int cnt = 0;
                 while (cnt < socketList.size()) {
                     Socket client = socketList.get(cnt);
                     if (client.isConnected()) {
                         DataOutputStream out = new DataOutputStream(client.getOutputStream());
                         String outStr = "開始遊戲:Server 主動傳的訊息..." + "\n";
                         out.writeUTF(outStr);
                     }
                     cnt += 1;
                 }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class TCpClientReadThread extends Thread {

        String dstAddress;
        int dstPort;
        private boolean running;
        GameActivity.TCPServerHandler handler;

        public InetAddress address;

        public TCpClientReadThread(String addr, int port, GameActivity.TCPServerHandler handler) {
            super();
            dstAddress = addr;
            dstPort = port;
            this.handler = handler;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        private void sendState(String state) {
            handler.sendMessage(
                    Message.obtain(handler,
                            GameActivity.TCPServerHandler.UPDATE_STATE, state));
        }

        @Override
        public void run() {
            InetAddress serverAddr = null;
            SocketAddress sc_add = null;

            try {
                serverAddr = InetAddress.getByName(editTextAddress.getText().toString());
                //設定port:1234
                sc_add = new InetSocketAddress(serverAddr, Integer.parseInt(editTextPort.getText().toString()));

                clientSocket = new Socket();
                Log.v("Socket" ,"before clientSocket.connect()");
                //與 Server 連線，timeout 時間 2秒
                clientSocket.connect(sc_add, 2000);
                Log.v("Socket" ,"after clientSocket.connect()");
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            sendState("connecting...");

            running = true;
            while (running) {
                try {
                    DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                    String recvStr = in.readUTF();

                    if (recvStr != null) {
                        handler.sendMessage(
                                Message.obtain(handler, GameActivity.TCPServerHandler.UPDATE_MSG, recvStr));
                    } else
                        clientSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class TCpClientSendThread extends Thread{

        String dstAddress;
        int dstPort;
        private boolean running;
        GameActivity.TCPServerHandler handler;

        //public DatagramSocket socket;
        public InetAddress address;

        public TCpClientSendThread(String addr, int port, GameActivity.TCPServerHandler handler) {
            super();
            dstAddress = addr;
            dstPort = port;
            this.handler = handler;
        }

        public void setRunning(boolean running){
            this.running = running;
        }

        private void sendState(String state){
            handler.sendMessage(
                    Message.obtain(handler,
                            GameActivity.TCPServerHandler.UPDATE_STATE, state));
        }

        @Override
        public void run() {
            sendState("connecting...");
            int cnt = 0;

             while(tcpClientReadThread.running == false && cnt <= 15) {
                 cnt += 1;
                try {
                    Thread.sleep(500);
                }catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

             if (cnt > 15)
                 return;

            running = true;

            try {
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
                String outStr = "要傳的訊息..." + "\n";
                out.writeUTF(outStr);
             } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getLocalAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    //if (inetAddress.isSiteLocalAddress())
                    if (inetAddress.isSiteLocalAddress() && inetAddress instanceof Inet4Address) {
                         //ip += "Local IP Address : " + inetAddress.getHostAddress() ;
                        ip = inetAddress.getHostAddress() ;
                    }
                }
            }
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }
        return ip;
    }

}

//https://blog.csdn.net/wirelessqa/article/details/8589200
//【Android資料傳遞】Intent傳遞List和Object和List(附源碼)
//
//http://xxs4129.pixnet.net/blog/post/164402593-android-socket%E7%AF%84%E4%BE%8B
//Android Socket範例
//
//https://blog.johnsonlu.org/androidsocket/
//[Android]Sample Socket Server & Client


