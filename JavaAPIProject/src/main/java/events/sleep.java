package events;

import com.example.chatgpt;
import com.example.kchanSleep;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class sleep extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("sleep")) {
            if (kchanSleep.isSleeping()) {
                event.reply("She's already inactive").queue();
                return;
            }

            String msg = "[" + event.getUser().getName() + "] has set K-chan to sleep.";
            chatgpt.addMessage(msg, "user");
            event.reply("K-chan 53 is now inactive (Ping her to reactivate her)").queue();
            kchanSleep.sleep();
        }
    }
}