package com.example;

import static spark.Spark.port;
import static spark.Spark.post;

public class VoiceTranscription {
    public static void start() {
        port(4567); 

        // This endpoint receives the voice transcription from Node.js
        post("/addTranscribed", (req, res) -> {
            String username = req.queryParams("username");
            String message = req.queryParams("message");

            if (username == null || message == null || username.isBlank() || message.isBlank()) {
                res.status(400);
                return "Missing username or message!";
            }

                String formatted = "[" + username + "]: " + message;
                chatgpt.addMessage(formatted, "user");
                System.out.println("Added to chat history: " + formatted);

                String response = chatgpt.askChatGPT(chatgpt.formatForGPT());
                System.out.println("GPT Response: " + response);

                String audioFilePath = ElevenLabs.textToSpeech(response);

            if (audioFilePath != null) {
                AudioPlayer.playAudio(audioFilePath);
            }

                return "OK";
        });
        System.out.println("Voice transcription server started on port 4567");
    }
}