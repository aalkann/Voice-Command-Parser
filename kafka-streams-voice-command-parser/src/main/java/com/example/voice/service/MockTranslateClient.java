package com.example.voice.service;

import com.local.dev.model.ParsedVoiceCommand;

public class MockTranslateClient implements TranslateService {

    @Override
    public ParsedVoiceCommand translate(ParsedVoiceCommand original) {
        return ParsedVoiceCommand.builder()
                .id(original.getId())
                .text("call juan")
                .probability(original.getProbability())
                .language(original.getLanguage())
                .build();
    }
}
