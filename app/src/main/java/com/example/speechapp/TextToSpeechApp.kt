package com.example.speechapp

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun TextToSpeechApp() {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    val tts = remember {
        TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) {
                Toast.makeText(context, "Text-to-Speech initialization failed!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
    LaunchedEffect(Unit) {
        tts.language = Locale("si", "LK")
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BasicTextField(
            value = inputText,
            onValueChange = { inputText = it },
            textStyle = TextStyle(fontSize = 18.sp, color = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.LightGray)
                .padding(16.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                if (inputText.isNotBlank()) {
                    tts.speak(inputText, TextToSpeech.QUEUE_FLUSH, null, null)
                } else {
                    Toast.makeText(context, "Enter text to speak", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Text("Speak")
        }
    }
}