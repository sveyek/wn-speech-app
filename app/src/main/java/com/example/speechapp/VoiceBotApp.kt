package com.example.speechapp

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

val systemPrompt = "You are a helpful telecom assistant. Always respond in ENGLISH language. Keep your responses SHORT and CONCISE. You can imagine things that are within the organization such as package details to respond to the user. Do not add any unspeakable markdown symbols to response. Do not say anything about the language you are responding."

val chatMessages = mutableListOf(
    mapOf("role" to "system", "content" to systemPrompt)
)

@Composable
fun VoiceBotApp() {
    val context = LocalContext.current
    val tts = remember { TextToSpeech(context, null) }
    var spokenText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val messages = remember { mutableStateListOf<Pair<String, Boolean>>() } // Pair: (Message, IsUser)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Permission required to use speech recognition", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "si-LK")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-HI") // English as fallback
        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
    }

    speechRecognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Toast.makeText(context, "Listening...", Toast.LENGTH_SHORT).show()
        }

        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onBeginningOfSpeech() {}

        override fun onEndOfSpeech() {
            isListening = false
        }

        override fun onError(error: Int) {
            Toast.makeText(context, "Speech recognition error: $error", Toast.LENGTH_SHORT).show()
            isListening = false
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            spokenText = matches?.joinToString(separator = " ") ?: ""
            messages.add(Pair(spokenText, true)) // Add user's message
            Log.d("VoiceChatbot", "Recognized text: $spokenText")

            val apiResponse = sendToAzureOpenAI(spokenText) { response ->
                if (response != null) {
                    // Translate GPT response from english to sinhala
                    val sinhalaResponse = translateText(response)
                    messages.add(Pair(sinhalaResponse, false))
                    Log.d("sinhala", sinhalaResponse)
                    // Speak GPT response
                    tts.speak(sinhalaResponse, TextToSpeech.QUEUE_FLUSH, null, null)

                } else Log.e("VoiceChatbot", "Couldn't receive openai response")
            }
        }
    })

    Scaffold(
        topBar = { TopAppBar(title = { Text("Voice Chatbot") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (!isListening) {
                    isListening = true
                    speechRecognizer.startListening(intent)
                } else {
                    isListening = false
                    speechRecognizer.stopListening()
                }
            }) {
                Text(if (isListening) "Stop" else "Speak")
            }
        }
    ) { padding ->
        ChatInterface(
            messages = messages,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun ChatInterface(messages: List<Pair<String, Boolean>>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        messages.forEach { (text, isUser) ->
            ChatBubble(text = text, isUser = isUser)
        }
    }
}

@Composable
fun ChatBubble(text: String, isUser: Boolean) {
    Row(
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier
                .background(
                    color = if (isUser) Color.Blue else Color.Gray,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp),
            color = Color.White
        )
    }
}

fun sendToAzureOpenAI(userInput: String, onResponse: (String?) -> Unit) {
    val apiKey = "b08d0124e8f644be843f152a1753b8e9"
    val endpoint = "https://openai-wn-rnd-vision.openai.azure.com/"
    val deploymentName = "gpt-4o-mini"
    val apiVersion = "2024-08-01-preview"

    chatMessages.add(mapOf("role" to "user", "content" to userInput))

    // Create OkHttpClient instance
    val client = OkHttpClient()

    // Create request body
    val jsonBody = JSONObject()
    jsonBody.put("messages", JSONArray(chatMessages))
    jsonBody.put("max_tokens", 100) // Set token limit as needed
    jsonBody.put("temperature", 0.7) // Adjust as required
    jsonBody.put("top_p", 1.0)

    val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

    // Build the request
    val request = Request.Builder()
        .url("$endpoint/openai/deployments/$deploymentName/chat/completions?api-version=$apiVersion")
        .addHeader("Content-Type", "application/json")
        .addHeader("api-key", apiKey)
        .post(requestBody)
        .build()

    // Make asynchronous API call
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
            onResponse(null)
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!response.isSuccessful) {
                    onResponse(null)
                } else {
                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody ?: "")
                    val generatedText = jsonResponse.optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("message")
                        ?.optString("content")

                    if (!generatedText.isNullOrEmpty()) {
                        chatMessages.add(mapOf("role" to "assistant", "content" to generatedText))
                        Log.d("sinhala-E", generatedText)
                    }

                    onResponse(generatedText)
                }
            }
        }
    })
}

fun translateText(text: String): String {
    // Add your key and endpoint
    val key = "EQTlUqfEBW7NTaGwlveHDnsbgRQ5VG6PeTnVFqcU4M7DpeMlnKvSJQQJ99ALAC8vTInXJ3w3AAAbACOG3etg"
    val endpoint = "https://api.cognitive.microsofttranslator.com"
    val location = "westus2"

    val path = "/translate"
    val constructedUrl = "$endpoint$path"

    // Query parameters
    val params = constructedUrl.toHttpUrlOrNull()?.newBuilder()
        ?.addQueryParameter("api-version", "3.0")
        ?.addQueryParameter("from", "en")
        ?.addQueryParameter("to", "si")
        ?.build()
        ?: throw IllegalArgumentException("Invalid URL")

    // Headers
    val headers = Headers.Builder()
        .add("Ocp-Apim-Subscription-Key", key)
        .add("Ocp-Apim-Subscription-Region", location)
        .add("Content-type", "application/json")
        .add("X-ClientTraceId", UUID.randomUUID().toString())
        .build()

    // Request body
    val body = JSONArray()
    val textObject = JSONObject().put("text", text)
    body.put(textObject)
    val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), body.toString())

    // HTTP client and request
    val client = OkHttpClient()
    val request = Request.Builder()
        .url(params)
        .headers(headers)
        .post(requestBody)
        .build()

    // Execute request
    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw IOException("Unexpected code $response")
        }

        val responseBody = response.body?.string() ?: throw IOException("Empty response body")

        // Parse the response string into a JSONArray
        val jsonArray = JSONArray(responseBody)

        // Build a single string from the translated texts
        val translatedTexts = StringBuilder()
        for (i in 0 until jsonArray.length()) {
            val translations = jsonArray.getJSONObject(i).getJSONArray("translations")
            for (j in 0 until translations.length()) {
                val text = translations.getJSONObject(j).getString("text")
                if (translatedTexts.isNotEmpty()) {
                    translatedTexts.append(" ") // Add space or other delimiter
                }
                translatedTexts.append(text) // Append the text
            }
        }

        return translatedTexts.toString()
    }
}



//fun sendToCompose(input: String, callback: (String) -> Unit) {
//    //callback("This is a GPT response to: $input")
//    CoroutineScope(Dispatchers.IO).launch {
//        try {
//            val url = URL("https://wnetdemos.globalwavenet.com/bot/api/channel/handle_request?ch_type=API&endpoint_id=mobile_app_rest_api_generic_flow&agent_id=3&company_id=1")
//            val connection = url.openConnection() as HttpURLConnection
//            connection.requestMethod = "GET"
//            connection.setRequestProperty("Content-Type", "application/json")
//            connection.doOutput = true
//
//            val jsonInput = JSONObject().apply {
//                put("chat_content", input)
//            }.toString()
//
//            OutputStreamWriter(connection.outputStream).use { it.write(jsonInput) }
//
//            val response = connection.inputStream.bufferedReader().use { it.readText() }
//            callback(response)
//        } catch (e: Exception) {
//            Log.e("sendToCompose", "Error: ${e.message}")
//            callback("Error: Unable to fetch response")
//        }
//    }
//}

//fun sendToCompose(input: String): String {
//    return try {
//        val url = URL("https://wnetdemos.globalwavenet.com/bot/api/channel/handle_request?ch_type=API&endpoint_id=mobile_app_rest_api_generic_flow&agent_id=3&company_id=1")
//        val connection = url.openConnection() as HttpURLConnection
//        connection.requestMethod = "GET"
//        connection.setRequestProperty("Content-Type", "application/json")
//        connection.doOutput = true
//
//        // Create JSON object for the body
//        val jsonBody = JSONObject()
//        jsonBody.put("chat_content", input)
//        Log.e("testPoint", "1")
//
//        // Send JSON body
//        val outputStream: OutputStream = connection.outputStream
//        outputStream.write(jsonBody.toString().toByteArray())
//        outputStream.flush()
//        Log.e("testPoint", "2")
//
//        // Get the response from the server
//        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
//            connection.inputStream.bufferedReader().use { it.readText() }
//        } else {
//            "Error: Server returned response code ${connection.responseCode}"
//        }
//    } catch (e: Exception) {
//        Log.e("sendToCompose", "Error: ${e.message}")
//        "Error: Unable to fetch response"
//    }
//}


