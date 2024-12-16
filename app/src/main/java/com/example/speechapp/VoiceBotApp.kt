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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

@Composable
fun VoiceBotApp() {
    val context = LocalContext.current
    val tts = remember { TextToSpeech(context, null) }
    var spokenText by remember { mutableStateOf("") }
    var gptResponse by remember { mutableStateOf("") }
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
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "si-LK") // Sinhala language code for Sri Lanka
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
                    messages.add(Pair(response, false))
                    // Speak GPT response
                    tts.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)

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
    val apiVersion = "2024-05-01-preview"

    // Create OkHttpClient instance
    val client = OkHttpClient()

    // Create request body
    val jsonBody = JSONObject()
    jsonBody.put("prompt", userInput)
    jsonBody.put("max_tokens", 200) // Set token limit as needed
    jsonBody.put("temperature", 0.7) // Adjust as required
    jsonBody.put("top_p", 1.0)

    val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

    // Build the request
    val request = Request.Builder()
        .url("$endpoint/openai/deployments/$deploymentName/completions?api-version=$apiVersion")
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
                    val generatedText = jsonResponse.optJSONArray("choices")?.optJSONObject(0)?.optString("text")
                    onResponse(generatedText)
                }
            }
        }
    })
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


