package com.example;

import com.local.dev.model.VoiceCommand;
import com.local.dev.config.ConfigReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.Serdes;
import com.local.dev.serdes.JsonSerde;

import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public class VoiceCommandProducer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @SneakyThrows
    public static void main(String[] args) {
        Properties configProps = ConfigReader.getProperties();

        Map<String, Object> props = new HashMap<>();
        for (String string : configProps.stringPropertyNames()) {
            props.put(string, configProps.getProperty(string));
        }

        var voiceCommandKafkaProducer = new KafkaProducer<>(props, Serdes.String().serializer(), new JsonSerde<>(VoiceCommand.class).serializer());

        Stream.of(OBJECT_MAPPER.readValue(VoiceCommandProducer.class.getClassLoader().getResourceAsStream("data/test-data.json"), VoiceCommand[].class))
                .map(voiceCommand -> new ProducerRecord<>(ConfigReader.VOICE_COMMANDS_TOPIC, voiceCommand.getId(), voiceCommand))
                .map(voiceCommandKafkaProducer::send)
                .forEach(VoiceCommandProducer::waitForProducer);

    }

    @SneakyThrows
    private static void waitForProducer(Future<RecordMetadata> recordMetadataFuture) {
        recordMetadataFuture.get();
    }


    // public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // @SneakyThrows
    // public static void main(String[] args) {
    //     // Set up the producer properties
    //     Properties props = new Properties();
    //     props.put("bootstrap.servers", "localhost:9092"); // Replace with your Kafka broker address
    //     props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    //     props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

    //     // Create a KafkaProducer
    //     KafkaProducer<String, VoiceCommand> producer = new KafkaProducer<>(props, Serdes.String().serializer(), );

    //     // Send messages every second
    //     try {
    //         while (true) {
    //             String message = "Hello, Kafka! Timestamp: " + System.currentTimeMillis();
    //             ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC_NAME, message);
                
    //             // Send the record asynchronously
    //             producer.send(record, new Callback() {
    //                 @Override
    //                 public void onCompletion(RecordMetadata metadata, Exception exception) {
    //                     if (exception != null) {
    //                         exception.printStackTrace();
    //                     } else {
    //                         System.out.println("Message sent to topic " + metadata.topic() + " partition " + metadata.partition() + " with offset " + metadata.offset());
    //                     }
    //                 }
    //             });

    //             // Sleep for 1 second
    //             Thread.sleep(1000);
    //         }
    //     } catch (InterruptedException e) {
    //         Thread.currentThread().interrupt();
    //         System.err.println("Producer was interrupted");
    //     } finally {
    //         producer.close();
    //     }

    // }
}