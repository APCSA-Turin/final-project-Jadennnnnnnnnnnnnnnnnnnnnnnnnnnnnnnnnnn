package events;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;

public class joinVC extends ListenerAdapter {
    
    public joinVC() {
    }
    
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("kjoin")) {

            Guild g = event.getGuild();
            if (g == null) {
                event.reply("You fucked up something").setEphemeral(true).queue();
                return;
            }

            Member m = event.getMember();
            if (m == null) {
                event.reply("You fucked up something").setEphemeral(true).queue();
                return;
            }

            GuildVoiceState vs = m.getVoiceState();
            if (vs == null) {
                event.reply("You fucked up something").setEphemeral(true).queue();
                return;
            }

            AudioChannel c = vs.getChannel();
            if (c == null) {
                event.reply("You're not in a voice channel dumbass.").setEphemeral(true).queue();
                return;
            }

            AudioManager audioManager = g.getAudioManager();
            audioManager.openAudioConnection(c);
            event.reply("K-chan has joined the call.").queue();
        }
    }
}
