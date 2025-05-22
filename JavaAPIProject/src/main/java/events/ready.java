package events;


import com.example.chatgpt;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class ready implements net.dv8tion.jda.api.hooks.EventListener {

    public ready() {}
    
    @Override
    public void onEvent(GenericEvent event) {
        if (event instanceof ReadyEvent) {
            System.out.println("K-chan is ready");

            JDA jda = event.getJDA();

            jda.updateCommands()
                .addCommands(
                    Commands.slash("sleep", "Put K-chan to sleep").setDefaultPermissions(DefaultMemberPermissions.ENABLED),
                    Commands.slash("kjoin", "K-chan joins your vc").setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                    )
                .queue();
                
            TextChannel c1 = jda.getTextChannelById("");
            TextChannel c2 = jda.getTextChannelById("");
            String text = "K-chan 53 is running now (Ping to activate her).";
            
            if (c1 != null) {
                c1.sendMessage(text).queue();
            }
            if (c2 != null) {
                c2.sendMessage(text).queue();
            }

            chatgpt.addMessage(text, "assistant");
        }
    }
}