package com.hideji.hayakawa.ble

import android.app.*
import android.content.*
import android.graphics.Color
import android.os.BatteryManager
import android.os.IBinder
import android.os.PowerManager
import android.widget.Toast
import com.hideji.hayakawa.ble.BleUtils.Companion.sendData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.app.PendingIntent

import android.content.Intent




enum class Actions
{
    START,
    STOP
}

enum class ServiceState {
    STARTED,
    STOPPED,
}

private const val name = "BLESERVICE_KEY"
private const val key = "BLESERVICE_STATE"

fun setServiceState(context: Context, state: ServiceState) {
    val sharedPrefs = getPreferences(context)
    sharedPrefs.edit().let {
        it.putString(key, state.name)
        it.apply()
    }
}

private fun getPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(name, 0)
}

fun getServiceState(context: Context): ServiceState {
    val sharedPrefs = getPreferences(context)
    val value = sharedPrefs.getString(key, ServiceState.STOPPED.name)
    return ServiceState.valueOf(value!!)
}

class BleService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false
    private var isCheckingStatusRunning = false
    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                Actions.START.name -> startService()
                Actions.STOP.name -> stopService()
            }
        }

        return START_STICKY
    }

    private fun startService() {
        if (isServiceStarted) return
        Toast.makeText(this, "Service starting its task", Toast.LENGTH_SHORT).show()
        isServiceStarted = true
        setServiceState(this, ServiceState.STARTED)

        // we need this lock so our service gets not affected by Doze Mode
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Ble::lock").apply {
                    acquire()
                }
            }

        GlobalScope.launch(Dispatchers.IO) {
            while (isServiceStarted) {
                if (!isCheckingStatusRunning){
                    isCheckingStatusRunning = true
                    launch(Dispatchers.IO) {
                        checkBatteryStatus()
                    }

                }
                delay(60 *1000)
            }
        }
    }

    private fun checkBatteryStatus(){
        val batteryStatus: Intent? = registerReceiver(null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val level: Int = batteryStatus!!.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING

        if (level * 100 / scale.toFloat() > 84 && isCharging) {
                sendData(this,"desligar\n")
        } else if (level * 100 / scale.toFloat() < 26 && !isCharging) {
                sendData(this,"ligar\n")
        }
        isCheckingStatusRunning = false
    }

    private fun stopService() {
        Toast.makeText(this, "Service stopping", Toast.LENGTH_SHORT).show()
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
        }
        isServiceStarted = false
        setServiceState(this, ServiceState.STOPPED)
    }

    override fun onCreate() {
        super.onCreate()
        val notification = createNotification()
        startForeground(1, notification)
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "BLE SERVICE CHANNEL"

        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
        val channel = NotificationChannel(
            notificationChannelId,
            "Ble Service notifications channel",
            NotificationManager.IMPORTANCE_HIGH
        ).let {
            it.description = "Ble Service channel"
            it.enableLights(true)
            it.lightColor = Color.RED
            it.enableVibration(true)
            it.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            it
        }
        notificationManager.createNotificationChannel(channel)

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, 0)
        }

        val builder: Notification.Builder = Notification.Builder(
            this,
            notificationChannelId
        )

        val stopSelf = Intent(this, this::class.java)
        val ACTION_STOP_SERVICE = "STOP"
        stopSelf.action = ACTION_STOP_SERVICE

        val pStopSelf = PendingIntent
            .getService(
                this, 0, stopSelf, PendingIntent.FLAG_CANCEL_CURRENT
            )

        return builder
            .setContentTitle("Ble Service")
            .setContentText("Ble Service working")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setTicker("Ticker text")
            .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
            .addAction(R.drawable.ic_launcher_foreground, "stop service",pStopSelf)
            .build()
    }


}