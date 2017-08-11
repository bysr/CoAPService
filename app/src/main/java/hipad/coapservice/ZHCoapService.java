package hipad.coapservice;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.IBinder;
import android.util.Log;


import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.proxy.DirectProxyCoapResolver;
import org.eclipse.californium.proxy.ProxyHttpServer;
import org.eclipse.californium.proxy.resources.ForwardingResource;
import org.eclipse.californium.proxy.resources.ProxyCoapClientResource;
import org.eclipse.californium.proxy.resources.ProxyHttpClientResource;

import java.io.IOException;
import java.io.Serializable;

import hipad.coapservice.cmd.ICmd;
import hipad.coapservice.note.INote;
import hipad.coapservice.note.NoteNormal;
import hipad.coapservice.note.NoteSub;

public class ZHCoapService extends Service {
    private static final String TAG = "WLCoapService";
    private static final boolean DEBUG = true;
    private static final int HTTP_SERVER_PROXY_PORT = 8080;
    private static final int PORT = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_PORT);

    private static CoapServer mCoapServer;
    private ProxyHttpServer mHttpServer = null;
    private ForwardingResource coap2coap = new ProxyCoapClientResource("coap2coap");
    private ForwardingResource coap2http = new ProxyHttpClientResource("coap2http");
    private ZHDataObserve mDataObserve;
    private ZHQueryResource mQueryResource;

    private boolean isConnected = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        initCoapResource();
        //监听网络状态变化
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        //监听创建节点命令
        intentFilter.addAction(Const.ACTION_COAP_NOTE);
        registerReceiver(connectReceiver, intentFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Serializable serializable = intent.getSerializableExtra(Const.KEY_CMD);
        if (serializable != null && serializable instanceof ICmd) {
            ICmd cmd = (ICmd) serializable;
            if (mDataObserve != null) {
                mDataObserve.change(cmd.getCmdString());
            }
        }
        return START_STICKY;
    }


    /**
     * 初始化CoapResource
     */
    private void initCoapResource() {
        mCoapServer = new CoapServer(PORT);
        mCoapServer.add(coap2coap);
        mCoapServer.add(coap2http);
        /**设置通讯节点*/
        mQueryResource = new ZHQueryResource(getApplicationContext(), "query");
        //开通消息订阅功能
        mDataObserve = new ZHDataObserve("notify");
        mCoapServer.add(mDataObserve);
        mCoapServer.add(mQueryResource);
    }

    /**
     * 开启coap服务
     */
    private void startCoapServer() {
        try {
            stopCoapServer();
            if (mHttpServer == null)
                try {
                    mHttpServer = new ProxyHttpServer(HTTP_SERVER_PROXY_PORT);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            mHttpServer.setProxyCoapResolver(new DirectProxyCoapResolver(coap2coap));
            mCoapServer.start();
            if (DEBUG)
                Log.d(TAG, "CoapServer start");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止coap服务
     */
    private void stopCoapServer() {
        if (mCoapServer != null) {
            mCoapServer.stop();
            if (DEBUG)
                Log.d(TAG, "CoapServer stop");
        }
    }

    private BroadcastReceiver connectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                /*网络状态变化*/
                case ConnectivityManager.CONNECTIVITY_ACTION:
                    State state = State.DISCONNECTED;
                    if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                        ConnectivityManager contectivityMananger = (ConnectivityManager) context
                                .getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo mNetworkInfo = contectivityMananger.getActiveNetworkInfo();
                        if (mNetworkInfo != null && mNetworkInfo.isConnected()) {
                            state = mNetworkInfo.getState();
                        }
                    }
                    if (state == State.CONNECTED && !isConnected) {
                        isConnected = true;
                        startCoapServer();
                    } else if (state == State.DISCONNECTED) {
                        isConnected = false;
                        stopCoapServer();
                    }
                    break;

                case Const.ACTION_COAP_NOTE:
                    Serializable serializable = intent.getSerializableExtra(Const.KEY_CMD);
                    if (serializable != null && serializable instanceof NoteNormal) {
                        INote note = (INote) serializable;

                        ZHQueryResource noteResource = new ZHQueryResource(getApplicationContext(), note.getNoteName());
                        mCoapServer.add(noteResource);
                    } else if (serializable != null && serializable instanceof NoteSub) {
                        INote note = (INote) serializable;
                        ZHTimeObserve noteResource = new ZHTimeObserve(note.getNoteName());
                        mCoapServer.add(noteResource);
                    }

                    break;
            }


        }
    };

    public void onDestroy() {
        unregisterReceiver(connectReceiver);
        super.onDestroy();
    }
}
