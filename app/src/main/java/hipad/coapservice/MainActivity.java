package hipad.coapservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import hipad.coapservice.cmd.CmdNext;
import hipad.coapservice.cmd.CmdPause;
import hipad.coapservice.cmd.CmdPlay;
import hipad.coapservice.cmd.CmdPrevious;
import hipad.coapservice.cmd.ICmd;

public class MainActivity extends AppCompatActivity {
    private TextView mReceiverText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mReceiverText = (TextView) findViewById(R.id.text);
        startCoapService();
    }

    @Override
    protected void onStart() {
        IntentFilter filter = new IntentFilter(Const.ACTION_COAP_MSG);
        registerReceiver(coapMessageReceiver, filter);
        super.onStart();
    }

    @Override
    protected void onStop() {
        unregisterReceiver(coapMessageReceiver);
        super.onStop();
    }

    /*启动coap服务器*/
    private void startCoapService() {
        startService(new Intent(this, ZHCoapService.class));
    }

    private void send(ICmd cmd) {
        Intent intnet = new Intent(this, ZHCoapService.class);
        intnet.putExtra(Const.KEY_CMD, cmd);
        startService(intnet);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnPlay:
                send(new CmdPlay());
                break;
            case R.id.btnNext:
                send(new CmdNext());
                break;
            case R.id.btnPause:
                send(new CmdPause());
                break;
            case R.id.btnPrevious:
                send(new CmdPrevious());
                break;
        }
    }

    private BroadcastReceiver coapMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra(Const.KEY_MSG);
            mReceiverText.append(text);
            mReceiverText.append("\n");
        }

    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }




}
