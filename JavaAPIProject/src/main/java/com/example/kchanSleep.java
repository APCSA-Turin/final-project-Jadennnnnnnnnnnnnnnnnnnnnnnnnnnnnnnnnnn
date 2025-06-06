package com.example;

import java.io.File;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;

public class kchanSleep {
    private static boolean sleeping = true;

    public static boolean isSleeping() {
        return sleeping;
    }

    public static void sleep() {
        sleeping = true;
    }

    public static void wake() {
        sleeping = false;
    }

    public static boolean mentioned(MessageReceivedEvent event) {
        return event.getMessage().getMentions().isMentioned(event.getJDA().getSelfUser());
    }

    public static boolean wakeUp(MessageReceivedEvent event) {
        if (!sleeping) {
            return false;
        }

        if (mentioned(event)) {
            wake();

            File f;
            int num = (int) (Math.random() * 3) + 1;
            f = switch (num) {
                case 1 -> new File("");
                case 2 -> new File("");
                default -> new File("");
                };
            event.getChannel().sendFiles(FileUpload.fromData(f)).queue();

            return true;
        }
        return false;
    }
    
    public static boolean ignore(MessageReceivedEvent event) {
        if (!sleeping) {
            return false;
        }
        return !mentioned(event);
    }
}