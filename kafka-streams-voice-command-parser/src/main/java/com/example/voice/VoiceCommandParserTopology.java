package com.example.voice;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;

import com.local.dev.config.ConfigReader;
import com.local.dev.model.ParsedVoiceCommand;
import com.local.dev.model.VoiceCommand;
import com.local.dev.serdes.JsonSerde;
import com.example.voice.service.ISpeechToTextService;
import com.example.voice.service.TranslateService;

public class VoiceCommandParserTopology {
    public static final Integer MINIMUM_BYTE_LENGTH = 10;
    
    public final ISpeechToTextService speachToTextService;
    public final TranslateService translateService;
    public final Double certaintyThreshold;

    
    public VoiceCommandParserTopology(ISpeechToTextService speachToTextService, TranslateService translateService, Double certaintyThreshold) {
        this.speachToTextService = speachToTextService;
        this.translateService = translateService;
        this.certaintyThreshold = certaintyThreshold;
    }

    public Topology createTopology(){
        StreamsBuilder streamsBuilder = new StreamsBuilder();
        var voiceCommandJsonSerde = new JsonSerde<>(VoiceCommand.class);
        var parsedVoiceCommandSerde = new JsonSerde<>(ParsedVoiceCommand.class);

        var branches = streamsBuilder.stream(ConfigReader.VOICE_COMMANDS_TOPIC, Consumed.with(Serdes.String(), voiceCommandJsonSerde))
            .filter((key, value) -> value.getAudio().length >= MINIMUM_BYTE_LENGTH)
            .mapValues((readOnlyKey, value) -> speachToTextService.speechToText(value))
            .split(Named.as("branches-"))
            .branch((key, value) -> value.getProbability() > certaintyThreshold, Branched.as("recognized"))
            .defaultBranch(Branched.as("unrecognized"));
        
        branches.get("branches-unrecognized")
            .to(ConfigReader.UNRECOGNIZED_COMMAND_TOPIC,Produced.with(Serdes.String(), parsedVoiceCommandSerde));
        
        var streamsMap = branches.get("branches-recognized")
            .split(Named.as("language-"))
            .branch((key, value) -> value.getLanguage().startsWith("en"), Branched.as("english"))
            .defaultBranch(Branched.as("non-english"));

        streamsMap.get("language-non-english")
            .mapValues((readOnlyKey, value) -> translateService.translate(value))
            .merge(streamsMap.get("language-english"))
            .to(ConfigReader.RECOGNIZED_COMMAND_TOPIC, Produced.with(Serdes.String(), parsedVoiceCommandSerde));

        return streamsBuilder.build();
    }
}
