package com.example.voice.service;

import com.local.dev.model.ParsedVoiceCommand;
import com.local.dev.model.VoiceCommand;

public interface SpeechToTextService {
    ParsedVoiceCommand speechToText(VoiceCommand voiceCommand);
}
