package hipad.coapservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import hipad.coapservice.cmd.CmdNext;
import hipad.coapservice.cmd.CmdPause;
import hipad.coapservice.cmd.CmdPlay;
import hipad.coapservice.cmd.CmdPrevious;
import hipad.coapservice.cmd.ICmd;
import hipad.coapservice.note.INote;
import hipad.coapservice.note.NoteNormal;
import hipad.coapservice.note.NoteSub;

public class MainActivity extends AppCompatActivity {
    private TextView mReceiverText;
    private EditText et_node;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mReceiverText = (TextView) findViewById(R.id.text);
        et_node = (EditText) findViewById(R.id.et_node);
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

            case R.id.btnNormal:
                //创建普通节点
                String nodeNor = et_node.getText().toString().trim();
                if (nodeNor != null)
                    createNote(new NoteNormal(nodeNor));
                break;
            case R.id.btnSub:
                //创建可订阅节点
                String nodeSub = et_node.getText().toString().trim();
                if (nodeSub != null)
                    createNote(new NoteSub(nodeSub));
                break;
        }
    }

    /**
     * 创建节点
     *
     * @param note
     */
    public void createNote(INote note) {
        Intent intent = new Intent(Const.ACTION_COAP_NOTE);
        intent.putExtra(Const.KEY_CMD, note);
        sendBroadcast(intent);
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
