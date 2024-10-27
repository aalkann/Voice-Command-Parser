package com.example.voice;

import org.apache.kafka.streams.KafkaStreams;

import com.local.dev.config.ConfigReader;
import com.example.voice.service.MockTranslateClient;
import com.example.voice.service.SpechToTextClient;

public class VoiceCommandParserApp {
    public static void main(String[] args) {
        VoiceCommandParserTopology topology = new VoiceCommandParserTopology(new SpechToTextClient(), new MockTranslateClient(), 0.90);

        KafkaStreams kafkaStreams = new KafkaStreams(topology.createTopology(), ConfigReader.getProperties());
        kafkaStreams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(kafkaStreams::close));
    }
}
