package com.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONObject;

public class ElevenLabs {
    private static final String API_KEY = 
    private static final String VOICE_ID = 
    private static final String BASE_URL = "https://api.elevenlabs.io/v1/text-to-speech/";
    private static final String MODEL_ID = "eleven_monolingual_v1";

    // Voice settings
    private static final double STABILITY = 0.5;
    private static final double SIMILARITY_BOOST = 0.5;

    private static final HttpClient httpClient = HttpClient.newHttpClient();

    
    // Converts text to speech using ElevenLabs API 
    public static String textToSpeech(String text) {
        try {
            // Build request body
            JSONObject requestBody = new JSONObject()
                    .put("text", text)
                    .put("model_id", MODEL_ID)
                    .put("voice_settings", new JSONObject()
                            .put("stability", STABILITY)
                            .put("similarity_boost", SIMILARITY_BOOST));

            // Build HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + VOICE_ID))
                    .header("Accept", "audio/mpeg")
                    .header("Content-Type", "application/json")
                    .header("xi-api-key", API_KEY)
                    .POST(BodyPublishers.ofString(requestBody.toString()))
                    .build();

            // Send request and get response
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                return saveAudioFile(response.body());
            } else {
                System.err.println("ElevenLabs API error: " + response.statusCode());
                System.err.println("Response: " + new String(response.body()));
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error calling ElevenLabs API: " + e.getMessage());
            return null;
        }
    }

    //Saves audio data to a file
    private static String saveAudioFile(byte[] audioData) throws Exception {
        // Create audio directory if it doesn't exist
        Path audioDir = Paths.get("./audio");
        Files.createDirectories(audioDir);

        // Generate unique filename
        String fileName = "tts_" + System.currentTimeMillis() + ".mp3";
        Path filePath = audioDir.resolve(fileName);

        // Write audio data to file
        Files.write(filePath, audioData);

        System.out.println("Audio file saved: " + filePath.toString());
        return filePath.toString();
    }
}