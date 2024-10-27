package com.example.voice.service;

import com.local.dev.model.ParsedVoiceCommand;

public interface TranslateService {
    ParsedVoiceCommand translate(ParsedVoiceCommand original);
}
