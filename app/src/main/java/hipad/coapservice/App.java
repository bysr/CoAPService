package hipad.coapservice;

import android.support.multidex.MultiDexApplication;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;

/**
 * Created by zhb on 2017/6/22.
 */

public class App extends MultiDexApplication {
	@Override
	public void onCreate() {
		super.onCreate();
		Logger.addLogAdapter(new AndroidLogAdapter());
	}
}
