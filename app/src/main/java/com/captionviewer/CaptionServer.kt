package com.captionviewer

import fi.iki.elonen.NanoHTTPD
import org.json.JSONException
import org.json.JSONObject

/**
 * A lightweight HTTP server that listens on [PORT] for POST requests to /caption.
 *
 * Expected JSON payload: { "text": "caption text here" }
 *
 * The [onCaptionReceived] callback is invoked on the server thread whenever a valid
 * caption payload arrives.  The caller is responsible for switching to the main thread
 * before touching the UI.
 */
class CaptionServer(
    private val onCaptionReceived: (String) -> Unit,
) : NanoHTTPD(PORT) {

    override fun serve(session: IHTTPSession): Response {
        if (session.method != Method.POST || session.uri != "/caption") {
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_PLAINTEXT,
                "Not found. Send POST /caption with JSON body {\"text\":\"…\"}",
            )
        }

        return try {
            val files = HashMap<String, String>()
            session.parseBody(files)
            val body = files["postData"] ?: ""
            val json = JSONObject(body)
            val text = json.getString("text")
            onCaptionReceived(text)
            newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "OK")
        } catch (e: JSONException) {
            newFixedLengthResponse(
                Response.Status.BAD_REQUEST,
                MIME_PLAINTEXT,
                "Invalid JSON: expected {\"text\":\"…\"}",
            )
        } catch (e: Exception) {
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                MIME_PLAINTEXT,
                "Server error: ${e.message}",
            )
        }
    }

    companion object {
        const val PORT = 8080
    }
}
