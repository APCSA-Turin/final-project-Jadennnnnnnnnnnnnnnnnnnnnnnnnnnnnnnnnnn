package events;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.example.chatgpt;
import com.example.kchanSleep;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class message extends ListenerAdapter {
    private final ScheduledExecutorService s = Executors.newSingleThreadScheduledExecutor();
    private final Object lock = new Object();

    private ScheduledFuture<?> timer = null;

    public message() {}

    @Override 
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        if (kchanSleep.ignore(event) || kchanSleep.wakeUp(event)) {
            return;
        }

        synchronized (lock) {
            if (timer != null && !timer.isDone()) {
                return;
            }
            event.getChannel().sendTyping().queue();

            timer = s.schedule(() -> {
                try {
                    getMessage(event);
                } catch (Exception e) {
                    System.err.println("Error" + e);
                    event.getChannel().sendMessage("You fucking broke me dumbass.").queue();
                }
            }, 2, TimeUnit.SECONDS);
        }
    }



    private void getMessage(MessageReceivedEvent event) throws Exception {
        String user = event.getMessage().getContentDisplay();
        String toChatGPT = chatgpt.formatMessage(event, user);
        System.out.println("Prompt to GPT:\n" + toChatGPT);
        String fromChatGPT = chatgpt.askChatGPT(toChatGPT);
        event.getChannel().sendMessage(fromChatGPT).queue();
    }
}