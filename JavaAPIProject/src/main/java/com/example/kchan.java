package com.example;

import events.joinVC;
import events.ready;
import events.sleep;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class kchan {
    public static void main(String[] args) {
        JDABuilder jda = JDABuilder.createDefault("");
        
        jda
            .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_VOICE_STATES)
            .addEventListeners(new ready(), new sleep(), new joinVC(), new message())
            .build();
    }
}
