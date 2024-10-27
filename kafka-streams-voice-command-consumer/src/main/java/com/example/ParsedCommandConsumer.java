package com.example;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Serdes;

import com.local.dev.config.ConfigReader;
import com.local.dev.model.ParsedVoiceCommand;
import com.local.dev.serdes.JsonSerde;

public class ParsedCommandConsumer {
     public static void main(String[] args) {
        Properties configProps = ConfigReader.getProperties();

        Map<String, Object> props = new HashMap<>();
        for (String string : configProps.stringPropertyNames()) {
            props.put(string, configProps.getProperty(string));
        }

        try (var commandConsumer = new KafkaConsumer<>(props, Serdes.String().deserializer(), new JsonSerde<>(ParsedVoiceCommand.class).deserializer())) {
            commandConsumer.subscribe(List.of(ConfigReader.RECOGNIZED_COMMAND_TOPIC, ConfigReader.UNRECOGNIZED_COMMAND_TOPIC));
            Runtime.getRuntime().addShutdownHook(new Thread(() -> close(commandConsumer)));

            while (true)  {
                commandConsumer.poll(Duration.ofSeconds(1))
                        .forEach(record -> System.out.println("""
                            Topic: %s
                            Result: %s
                            """.formatted(record.topic(), record.value().toString())));
                commandConsumer.commitAsync();
            }
        }

    }

    private static void close(KafkaConsumer<String, ParsedVoiceCommand> commandConsumer) {
        commandConsumer.wakeup();
    }

}
