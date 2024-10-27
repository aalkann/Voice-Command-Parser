package com.example.voice;

import org.apache.kafka.streams.KafkaStreams;

import com.local.dev.config.ConfigReader;
import com.example.voice.service.MockSttClient;
import com.example.voice.service.MockTranslateClient;

public class VoiceCommandParserApp {
    public static void main(String[] args) {
        VoiceCommandParserTopology topology = new VoiceCommandParserTopology(new MockSttClient(), new MockTranslateClient(), 0.90);

        KafkaStreams kafkaStreams = new KafkaStreams(topology.createTopology(), ConfigReader.getProperties());
        kafkaStreams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(kafkaStreams::close));
    }
}
