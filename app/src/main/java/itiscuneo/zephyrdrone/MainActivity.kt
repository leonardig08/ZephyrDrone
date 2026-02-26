package itiscuneo.zephyrdrone

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {

    private var server: DroneHttpServer? = null
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var tvStatus: TextView
    private lateinit var tvIp: TextView
    private lateinit var tvDrone: TextView
    private lateinit var tvLog: TextView
    private lateinit var btnToggle: Button
    private lateinit var scrollLog: ScrollView

    // Aggiorna stato drone ogni 3 secondi
    private val statusRunnable = object : Runnable {
        override fun run() {
            updateDroneStatus()
            handler.postDelayed(this, 3000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus  = findViewById(R.id.tvStatus)
        tvIp      = findViewById(R.id.tvIp)
        tvDrone   = findViewById(R.id.tvDrone)
        tvLog     = findViewById(R.id.tvLog)
        btnToggle = findViewById(R.id.btnToggle)
        scrollLog = findViewById(R.id.scrollLog)

        tvIp.text = "IP: ${getLocalIpAddress() ?: "non trovato"}"

        btnToggle.setOnClickListener {
            if (!isRunning) startServer() else stopServer()
        }

        handler.post(statusRunnable)
    }

    private fun startServer() {
        server = DroneHttpServer(8080) { msg ->
            runOnUiThread { appendLog(msg) }
        }
        server?.start()
        isRunning = true
        btnToggle.text = "⏹ STOP SERVER"
        btnToggle.setBackgroundColor(getColor(android.R.color.holo_red_dark))
        tvStatus.text = "🟢 Server attivo — porta 8080"
        appendLog("Server avviato su :8080")
    }

    private fun stopServer() {
        server?.stop()
        server = null
        isRunning = false
        btnToggle.text = "▶ START SERVER"
        btnToggle.setBackgroundColor(getColor(android.R.color.holo_green_dark))
        tvStatus.text = "🔴 Server fermo"
        appendLog("Server fermato")
    }

    private fun updateDroneStatus() {
        val connected  = ZephyrDroneApp.isDroneConnected
        val registered = ZephyrDroneApp.sdkRegistered
        tvDrone.text = "${if (registered) "SDK ✅" else "SDK ❌"}   ${if (connected) "Drone 🟢" else "Drone 🔴"}"
    }

    private fun appendLog(msg: String) {
        tvLog.append("\n› $msg")
        scrollLog.post { scrollLog.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun getLocalIpAddress(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.contains('.') == true }
                ?.hostAddress
        } catch (e: Exception) { null }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(statusRunnable)
        stopServer()
    }
}