package com.example.voice.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import com.assemblyai.api.AssemblyAI;
import com.assemblyai.api.resources.transcripts.types.Transcript;
import com.assemblyai.api.resources.transcripts.types.TranscriptStatus;
import com.local.dev.model.ParsedVoiceCommand;
import com.local.dev.model.VoiceCommand;

public class SpechToTextClient implements ISpeechToTextService{

    private final AssemblyAI client = AssemblyAI.builder().apiKey("5866ae13440c4fb19ac60f1ef8a0d3b4").build();
    
    @Override
    public ParsedVoiceCommand speechToText(VoiceCommand voiceCommand) {
        File file = byteArrayToFile(voiceCommand.getAudio(), voiceCommand.getId());
        ParsedVoiceCommand parsedVoiceCommand = ParsedVoiceCommand.builder()
            .id(voiceCommand.getId())
            .audioCodec(voiceCommand.getAudioCodec())
            .language(voiceCommand.getLanguage())
            .build();

        try {
            Transcript transcript = client.transcripts().transcribe(file);
            if (transcript.getStatus() == TranscriptStatus.ERROR) {
                System.out.println("Transcript failed with error: " + transcript.getError().get());
                parsedVoiceCommand.setProbability(new Random().nextDouble() * (9/10));
                return parsedVoiceCommand;
            }

            parsedVoiceCommand.setText(transcript.getText().orElse(null));
            // Sentetic prabability value between 0.90 and 1.00
            parsedVoiceCommand.setProbability(new Random().nextDouble()/10 + 0.90);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return parsedVoiceCommand;
    }

    private File byteArrayToFile(byte[] audioData, String fileName){
        File file = new File(fileName);
        
        try(FileOutputStream fos = new FileOutputStream(file)){
            fos.write(audioData);
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return file;
    }
    
}
