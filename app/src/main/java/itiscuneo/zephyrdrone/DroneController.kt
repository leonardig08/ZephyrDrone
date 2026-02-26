package itiscuneo.zephyrdrone

import android.util.Log
import dji.sdk.keyvalue.key.BatteryKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.key.DJIKey
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.KeyManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object DroneController {

    private const val TAG     = "DroneController"
    private const val TIMEOUT = 5L // secondi

    // --- Stato drone ---

    fun getStatusJson(): String {
        val connected  = ZephyrDroneApp.isDroneConnected
        val registered = ZephyrDroneApp.sdkRegistered

        val battery = try {
            val key = KeyTools.createKey(BatteryKey.KeyChargeRemainingInPercent)
            KeyManager.getInstance().getValue(key)
        } catch (e: Exception) { null }

        return """{"connected": $connected, "sdk_registered": $registered, "battery": ${battery ?: "null"}}"""
    }

    // --- Azioni volo ---

    fun takeoff()       = performAction(KeyTools.createKey(FlightControllerKey.KeyStartTakeoff),       "takeoff")
    fun land()          = performAction(KeyTools.createKey(FlightControllerKey.KeyStartAutoLanding),   "land")
    fun goHome()        = performAction(KeyTools.createKey(FlightControllerKey.KeyStartGoHome),        "goHome")
    fun stopGoHome()    = performAction(KeyTools.createKey(FlightControllerKey.KeyStopGoHome),         "stopGoHome")
    fun emergencyStop() = performAction(KeyTools.createKey(FlightControllerKey.KeyEmergencyStopMotor), "emergencyStop")

    // --- Helper sincrono ---
    // NanoHttpd gestisce ogni request su un thread dedicato,
    // quindi possiamo bloccare con CountDownLatch senza problemi.

    private fun performAction(key: DJIKey<*>, name: String): Pair<Boolean, String> {
        if (!ZephyrDroneApp.isDroneConnected) {
            return Pair(false, "Drone non connesso")
        }

        val latch   = CountDownLatch(1)
        var success = false
        var message = "Timeout dopo ${TIMEOUT}s"

        KeyManager.getInstance().performAction(key, null,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    success = true
                    message = "$name eseguito con successo"
                    latch.countDown()
                }
                override fun onFailure(error: IDJIError) {
                    success = false
                    message = "Errore $name: ${error.description()}"
                    latch.countDown()
                }
            }
        )

        latch.await(TIMEOUT, TimeUnit.SECONDS)
        Log.d(TAG, "[$name] success=$success msg=$message")
        return Pair(success, message)
    }
}