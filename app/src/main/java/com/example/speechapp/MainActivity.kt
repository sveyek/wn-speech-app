package com.example.speechapp

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.example.speechapp.ui.theme.SpeechAppTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpeechAppTheme {
                MainApp()
            }
        }
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun MainApp() {
    val selectedTab = remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TabRow(selectedTabIndex = selectedTab.value) {
                Tab(
                    text = { Text("Speech to Text") },
                    selected = selectedTab.value == 0,
                    onClick = { selectedTab.value = 0 }
                )
                Tab(
                    text = { Text("Text to Speech") },
                    selected = selectedTab.value == 1,
                    onClick = { selectedTab.value = 1 }
                )
                Tab(
                    text = { Text("Voice Chatbot") },
                    selected = selectedTab.value == 2,
                    onClick = { selectedTab.value = 2 }
                )
            }
        }
    ) {
        when (selectedTab.value) {
            0 -> SpeechToTextApp()
            1 -> TextToSpeechApp()
            2 -> VoiceBotApp()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSpeechToTextApp() {
    SpeechAppTheme {
        MainApp()
    }
}
