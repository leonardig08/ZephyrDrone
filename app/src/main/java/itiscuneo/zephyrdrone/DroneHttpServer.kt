package itiscuneo.zephyrdrone

import android.util.Log
import fi.iki.elonen.NanoHTTPD

class DroneHttpServer(
    port: Int,
    private val onLog: (String) -> Unit
) : NanoHTTPD(port) {

    companion object {
        const val TAG = "DroneHttpServer"
    }

    override fun serve(session: IHTTPSession): Response {
        val uri    = session.uri
        val method = session.method.name
        log("$method $uri")

        return try {
            when {
                uri == "/ping"        -> respondJson("""{"pong": true}""")
                uri == "/status"      -> respondJson(DroneController.getStatusJson())
                uri == "/takeoff"     -> actionResponse(DroneController.takeoff())
                uri == "/land"        -> actionResponse(DroneController.land())
                uri == "/gohome"      -> actionResponse(DroneController.goHome())
                uri == "/gohome/stop" -> actionResponse(DroneController.stopGoHome())
                uri == "/emergency"   -> actionResponse(DroneController.emergencyStop())
                else -> errorResponse(404, "Endpoint non trovato: $uri")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore: ${e.message}")
            errorResponse(500, "Errore interno: ${e.message}")
        }
    }

    private fun actionResponse(result: Pair<Boolean, String>): Response {
        val json   = """{"success": ${result.first}, "message": "${result.second}"}"""
        val status = if (result.first) Response.Status.OK else Response.Status.INTERNAL_ERROR
        return respondJson(json, status)
    }

    private fun respondJson(json: String, status: Response.Status = Response.Status.OK): Response =
        newFixedLengthResponse(status, "application/json", json)

    private fun errorResponse(code: Int, msg: String): Response {
        val status = if (code == 404) Response.Status.NOT_FOUND else Response.Status.INTERNAL_ERROR
        return respondJson("""{"success": false, "error": "$msg"}""", status)
    }

    private fun log(msg: String) {
        Log.d(TAG, msg)
        onLog(msg)
    }
}