package com.example.voice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.local.dev.config.ConfigReader;
import com.local.dev.model.ParsedVoiceCommand;
import com.local.dev.model.VoiceCommand;
import com.local.dev.serdes.JsonSerde;
import com.example.voice.service.SpeechToTextService;
import com.example.voice.service.TranslateService;

@ExtendWith(MockitoExtension.class)
public class VoiceCommandParserTopologyTest {
    @Mock
    private SpeechToTextService speechToTextService;
    @Mock
    private TranslateService translateService;
    private Double CERTAINTY_THRESHOLD = 0.9;

    // @InjectMocks
    private VoiceCommandParserTopology voiceCommandParserTopology;
    private TopologyTestDriver topologyTestDriver;
    private TestInputTopic<String, VoiceCommand> voiceCommandInputTopic;
    private TestOutputTopic<String, ParsedVoiceCommand> recognizedCommandOutputTopic;
    private TestOutputTopic<String, ParsedVoiceCommand> unrecognizedCommandOutputTopic;

    @BeforeEach
    void setUp(){
        voiceCommandParserTopology = new VoiceCommandParserTopology(speechToTextService, translateService, CERTAINTY_THRESHOLD);
        var topology = voiceCommandParserTopology.createTopology();

        var props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        
        topologyTestDriver = new TopologyTestDriver(topology,props);
        var voiceCommandJsonSerde = new JsonSerde<>(VoiceCommand.class);
        var parsedVoiceCommandJsonSerde = new JsonSerde<>(ParsedVoiceCommand.class);

        voiceCommandInputTopic = topologyTestDriver.createInputTopic(ConfigReader.VOICE_COMMANDS_TOPIC, Serdes.String().serializer(), voiceCommandJsonSerde.serializer());
        recognizedCommandOutputTopic = topologyTestDriver.createOutputTopic(ConfigReader.RECOGNIZED_COMMAND_TOPIC, Serdes.String().deserializer(), parsedVoiceCommandJsonSerde.deserializer());
        unrecognizedCommandOutputTopic = topologyTestDriver.createOutputTopic(ConfigReader.UNRECOGNIZED_COMMAND_TOPIC, Serdes.String().deserializer(), parsedVoiceCommandJsonSerde.deserializer());
    }

    @Test
    void testWhenEnglishDataParsedCorrectly() {
        // Given
        byte[] randomBytes = new byte[20];
        new Random().nextBytes(randomBytes);
        var voiceCommand = VoiceCommand.builder()
            .id(UUID.randomUUID().toString())
            .audio(randomBytes)
            .audioCodec("FLAC")
            .language("en-US")
            .build();

        var expectedParsedVoiceCommand = ParsedVoiceCommand.builder()
            .id(voiceCommand.getId())
            .text("call john")
            .language("en-US")
            .probability(0.98)
            .build();

        // When
        when(speechToTextService.speechToText(voiceCommand)).thenReturn(expectedParsedVoiceCommand);
        voiceCommandInputTopic.pipeInput(voiceCommand);
        var parsedVoiceCommand = recognizedCommandOutputTopic.readRecord().value();

        // Then
        assertEquals(voiceCommand.getId(), parsedVoiceCommand.getId(), "Voice Command and Parsed Voiced Command have same id");
        assertEquals("call john", parsedVoiceCommand.getText(), "Parsed Voice Command have 'call john' value");
    }

    @Test
    void testWhenTurkishDataParsedCorrectly() {
        // Given
        byte[] randomBytes = new byte[20];
        new Random().nextBytes(randomBytes);
        var inputVoiceCommand = VoiceCommand.builder()
            .id(UUID.randomUUID().toString())
            .audio(randomBytes)
            .audioCodec("FLAC")
            .language("es-AR")
            .build();

        var expectedParsedVoiceCommand = ParsedVoiceCommand.builder()
            .id(inputVoiceCommand.getId())
            .text("john'u ara")
            .language("es-AR")
            .probability(0.95)
            .build();

        var expectedTranslatedParsedVoiceCommand = ParsedVoiceCommand.builder()
            .id(inputVoiceCommand.getId())
            .text("call john")
            .language("en-US")
            .probability(0.95)
            .build();

        // When
        when(speechToTextService.speechToText(inputVoiceCommand)).thenReturn(expectedParsedVoiceCommand);
        when(translateService.translate(expectedParsedVoiceCommand)).thenReturn(expectedTranslatedParsedVoiceCommand);

        voiceCommandInputTopic.pipeInput(inputVoiceCommand);
        ParsedVoiceCommand  translatedParsedVoiceCommand = recognizedCommandOutputTopic.readRecord().value();

        // Then
        assertEquals(inputVoiceCommand.getId(), translatedParsedVoiceCommand.getId(), "Voice Command and Translated Parsed Voiced Command have same id");
        assertEquals("call john", translatedParsedVoiceCommand.getText(), "Translated Parsed Voiced Command have 'call john' value");
    }

    @Test
    void testWhenNoRecognizedData(){
        // Given
        byte[] randomBytes = new byte[20];
        new Random().nextBytes(randomBytes);
        var inputVoiceCommand = VoiceCommand.builder()
            .id(UUID.randomUUID().toString())
            .audio(randomBytes)
            .audioCodec("FLAC")
            .language("en-US")
            .build();

        var expectedParsedVoiceCommand = ParsedVoiceCommand.builder()
            .id(inputVoiceCommand.getId())
            .text("call john")
            .language("en-US")
            .probability(0.75)
            .build();
        
        // When
        when(speechToTextService.speechToText(inputVoiceCommand)).thenReturn(expectedParsedVoiceCommand);
        voiceCommandInputTopic.pipeInput(inputVoiceCommand);
        var unrecognizedParsedVoiceCommand = unrecognizedCommandOutputTopic.readRecord().value();

        // Then
        assertEquals(inputVoiceCommand.getId(), unrecognizedParsedVoiceCommand.getId(), "Voice Command and Unrecognized Parsed Voiced Command have same id");
        assertTrue(recognizedCommandOutputTopic.isEmpty(), "recognized-commands topic is empty");
        verify(translateService,never()).translate(any(ParsedVoiceCommand.class));
    }

    @Test
    void testWhenAudioTooShort(){
        // Given
        byte[] randomBytes = new byte[9];
        new Random().nextBytes(randomBytes);
        var inputVoiceCommand = VoiceCommand.builder()
            .id(UUID.randomUUID().toString())
            .audio(randomBytes)
            .audioCodec("FLAC")
            .language("en-US")
            .build();

        // When
        voiceCommandInputTopic.pipeInput(inputVoiceCommand);

        // Then
        assertTrue(recognizedCommandOutputTopic.isEmpty(), "recognized-commands is empty");
        assertTrue(unrecognizedCommandOutputTopic.isEmpty(), "unrecognized-commands is empty");

        verify(speechToTextService,never()).speechToText(any(VoiceCommand.class));
        verify(translateService,never()).translate(any(ParsedVoiceCommand.class));
    }
}
