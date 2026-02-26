package itiscuneo.zephyrdrone

import android.app.Application
import android.util.Log
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.SDKManagerCallback

class ZephyrDroneApp : Application() {

    companion object {
        const val TAG = "ZephyrDroneApp"
        var isDroneConnected = false
        var sdkRegistered    = false
    }

    override fun onCreate() {
        super.onCreate()

        SDKManager.getInstance().init(this, object : SDKManagerCallback {

            override fun onInitProcess(event: DJISDKInitEvent, totalProcess: Int) {
                Log.d(TAG, "SDK Init: $event")
                if (event == DJISDKInitEvent.INITIALIZE_COMPLETE) {
                    Log.d(TAG, "Init completo, registro app...")
                    SDKManager.getInstance().registerApp()
                }
            }

            override fun onRegisterSuccess() {
                Log.d(TAG, "Registrazione DJI OK")
                sdkRegistered = true
            }

            override fun onRegisterFailure(error: IDJIError) {
                Log.e(TAG, "Registrazione fallita: ${error.description()}")
                sdkRegistered = false
            }

            override fun onProductConnect(productId: Int) {
                Log.d(TAG, "Drone connesso! ID: $productId")
                isDroneConnected = true
            }

            override fun onProductDisconnect(productId: Int) {
                Log.d(TAG, "Drone disconnesso")
                isDroneConnected = false
            }

            override fun onProductChanged(productId: Int) {
                Log.d(TAG, "Prodotto cambiato: $productId")
            }

            override fun onDatabaseDownloadProgress(current: Long, total: Long) {
                Log.d(TAG, "DB Download: $current/$total")
            }
        })
    }
}