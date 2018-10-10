package cn.x1ongzhu.admgr;

import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class IRSensorService extends Service implements SerialInputOutputManager.Listener {
    private static final String ACTION_USB_PERMISSION = "cn.x1ongzhu.admgr.USB_PERMISSION";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private BroadcastReceiver        mUsbPermissionActionReceiver;
    private StringBuffer             stringBuffer;
    private UsbSerialPort            serialPort;
    private SerialInputOutputManager serialIoManager;
    private IRBinder                 mBinder;
    private boolean                  running;
    private boolean                  opened;
    private int                      currentValue;
    private List<Integer>            recentValue;
    private int                      closeDistance;

    public IRSensorService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mBinder = new IRBinder();
        stringBuffer = new StringBuffer();
        mUsbPermissionActionReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ACTION_USB_PERMISSION.equals(action)) {
                    onStart();
                }
            }
        };
        IntentFilter usbIntentFilter = new IntentFilter();
        usbIntentFilter.addAction(ACTION_USB_PERMISSION);
        registerReceiver(mUsbPermissionActionReceiver, usbIntentFilter);
        recentValue = new ArrayList<>();
        closeDistance = getSharedPreferences("conf", MODE_PRIVATE).getInt("closeDistance", -1);
        onStart();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        onStop();
    }

    private void onStart() {
        onStop();
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            return;
        }
        UsbSerialDriver serialDriver = availableDrivers.get(0);
        if (manager.hasPermission(serialDriver.getDevice())) {
            initSerial();
        } else {
            PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
            manager.requestPermission(serialDriver.getDevice(), mPermissionIntent);
        }
    }

    private void initSerial() {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            return;
        }
        try {
            UsbSerialDriver serialDriver = availableDrivers.get(0);
            serialPort = serialDriver.getPorts().get(0);
            UsbDeviceConnection connection = manager.openDevice(serialDriver.getDevice());
            serialPort.open(connection);
            serialPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            serialIoManager = new SerialInputOutputManager(serialPort, this);
            mExecutor.submit(serialIoManager);
            running = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onStop() {
        running = false;
        if (serialIoManager != null) {
            serialIoManager.stop();
            serialIoManager = null;
        }
        if (serialPort != null) {
            try {
                serialPort.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int getStableValue() {
        if (recentValue.size() < 20) {
            return -1;
        } else {
            List<Integer> list = new ArrayList<>(recentValue);
            Collections.sort(list);
            list.remove(0);
            list.remove(list.size() - 1);
            int avg = 0;
            for (Integer i : list) {
                avg += i;
            }
            avg /= list.size();
            return avg;
        }
    }

    @Override
    public void onNewData(byte[] data) {
        for (byte b : data) {
            if (b == 10) {
                Log.d("distance:", stringBuffer.toString());
                int d = Integer.valueOf(stringBuffer.toString());
                stringBuffer.delete(0, stringBuffer.length());
                if (recentValue.size() == 20) {
                    recentValue.remove(0);
                }
                recentValue.add(d);

                if (closeDistance != -1 && Math.abs(d - closeDistance) > 20) {
                    if (!opened) {
                        int i = 16;
                        for (; i < 20; i++) {
                            if (Math.abs(recentValue.get(i) - closeDistance) <= 20) {
                                break;
                            }
                        }
                        if (i == 20) {
                            Intent intent = new Intent("com.example.playmedia.play");
                            intent.putExtra("command", "pause");
                            sendBroadcast(intent);
                            opened = true;
                        }
                    }
                } else if (closeDistance != -1) {
                    if (opened) {
                        int i = 16;
                        for (; i < 20; i++) {
                            if (Math.abs(recentValue.get(i) - closeDistance) >= 20) {
                                break;
                            }
                        }
                        if (i == 20) {
                            Intent intent = new Intent("com.example.playmedia.play");
                            intent.putExtra("command", "resume");
                            sendBroadcast(intent);
                            opened = false;
                        }
                    }
                }

            } else {
                stringBuffer.append((char) b);
            }
        }
    }

    @Override
    public void onRunError(Exception e) {
        e.printStackTrace();
    }

    public class IRBinder extends Binder {
        public void start() {
            onStart();
        }

        public void stop() {
            onStop();
        }

        public int getValue() {
            return getStableValue();
        }

        public void setCloseDistance(int distance) {
            getSharedPreferences("config", MODE_PRIVATE).edit().putInt("closeDistance", distance).apply();
            closeDistance = distance;
        }
    }
}
