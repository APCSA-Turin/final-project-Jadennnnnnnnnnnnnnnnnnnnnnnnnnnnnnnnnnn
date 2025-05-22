package com.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class chatgpt {
    private static final String API = "";
    private static final String MODEL = "";
    private static final String SYSTEM = "";
    
    private static final ArrayList<JSONObject> chatHistory = new ArrayList<>();

    public static String formatMessage(MessageReceivedEvent event, String input) {
        if (chatHistory.isEmpty()) {
            JSONObject o = new JSONObject().put("role", "system").put("content", SYSTEM);
            chatHistory.add(o);
        }

        while (chatHistory.size() > 13) {
            chatHistory.remove(1);
        }

        JSONObject o2 = new JSONObject().put("role", "user").put("content",
                "[" + event.getAuthor().getName() + "]: " + input);
        chatHistory.add(o2);
        
        JSONObject r = new JSONObject().put("model", MODEL).put("messages", chatHistory);
        return r.toString();
    }



    public static void addMessage(String msg, String roleV) {
        if (chatHistory.isEmpty()) {
            JSONObject o = new JSONObject().put("role", "system").put("content", SYSTEM);
            chatHistory.add(o);
        }

        while (chatHistory.size() > 13) {
            chatHistory.remove(1);
        }

        JSONObject o2 = new JSONObject().put("role", roleV).put("content", msg);
        chatHistory.add(o2);
    }



    //https://www.youtube.com/watch?v=TkJ2dFtD0ho
    //https://www.youtube.com/watch?v=aC544kkYtBs 
    public static String askChatGPT(String str) throws Exception {
        HttpClient hc = HttpClient.newHttpClient();

        HttpRequest hr = HttpRequest.newBuilder().uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Authorization", "Bearer " + API)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(str, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> result = hc.send(hr, HttpResponse.BodyHandlers.ofString());
        String response = result.body();
        System.out.println("GPT response:\n" + response);
        
        JSONObject r = new JSONObject(response);
        JSONArray c = r.getJSONArray("choices");
        JSONObject reply = c.getJSONObject(0).getJSONObject("message");
        String reply2 = reply.getString("content");

        JSONObject o = new JSONObject().put("role", "assistant").put("content", reply2);
        chatHistory.add(o);

        return reply2;
    }
}