package com.example.speechapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.speechapp.ui.theme.SpeechAppTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpeechAppTheme {
                SpeechToTextApp()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSpeechToTextApp() {
    SpeechAppTheme {
        SpeechToTextApp()
    }
}
