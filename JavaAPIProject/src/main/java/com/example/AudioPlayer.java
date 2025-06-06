package com.example;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;

import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.managers.AudioManager;

public class AudioPlayer {
    private static final AudioPlayerManager playerManager;
    private static final com.sedmelluq.discord.lavaplayer.player.AudioPlayer audioPlayer;
    private static final TrackScheduler scheduler;
    private static Guild currentGuild;
    
    static {
        playerManager = new DefaultAudioPlayerManager();
        // Register audio sources
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
        
        audioPlayer = playerManager.createPlayer();
        scheduler = new TrackScheduler(audioPlayer);
        audioPlayer.addListener(scheduler);
    }
    
    public static void setGuild(Guild guild) {
        currentGuild = guild;
    }
    
    public static void playAudio(String filePath) {
        if (currentGuild == null || !kchanVC.INVC()) {
            System.out.println("K-chan is not in a voice channel!");
            return;
        }
        
        AudioManager audioManager = currentGuild.getAudioManager();
        audioManager.setSendingHandler(new AudioPlayerSendHandler(audioPlayer));
        
        // Load the audio file
        playerManager.loadItem(filePath, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                System.out.println("Playing audio: " + track.getInfo().title);
                scheduler.queue(track);
            }
            
            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                // Not used for single files
            }
            
            @Override
            public void noMatches() {
                System.out.println("No matches found for: " + filePath);
            }
            
            @Override
            public void loadFailed(FriendlyException e) {
                System.err.println("Could not play: " + e.getMessage());
            }
        });
    }
    
    public static void stopAudio() {
        audioPlayer.stopTrack();
    }
    
    private static class TrackScheduler extends AudioEventAdapter {
        private final com.sedmelluq.discord.lavaplayer.player.AudioPlayer player;
        private final BlockingQueue<AudioTrack> queue;
        
        public TrackScheduler(com.sedmelluq.discord.lavaplayer.player.AudioPlayer player) {
            this.player = player;
            this.queue = new LinkedBlockingQueue<>();
        }
        
        public void queue(AudioTrack track) {
            if (!player.startTrack(track, true)) {
                queue.offer(track);
            }
        }
        
        @Override
        public void onTrackEnd(com.sedmelluq.discord.lavaplayer.player.AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
            if (endReason.mayStartNext) {
                AudioTrack nextTrack = queue.poll();
                if (nextTrack != null) {
                    player.startTrack(nextTrack, false);
                }
            }
            
            // Delete the audio file after playing
            try {
                String identifier = track.getIdentifier();
                File file = new File(identifier);
                if (file.exists() && file.getName().startsWith("tts_")) {
                    file.delete();
                    System.out.println("Deleted audio file: " + identifier);
                }
            } catch (Exception e) {
                // Ignore deletion errors
            }
        }
    }
    
    private static class AudioPlayerSendHandler implements AudioSendHandler {
        private final com.sedmelluq.discord.lavaplayer.player.AudioPlayer audioPlayer;
        private AudioFrame lastFrame;
        
        public AudioPlayerSendHandler(com.sedmelluq.discord.lavaplayer.player.AudioPlayer audioPlayer) {
            this.audioPlayer = audioPlayer;
        }
        
        @Override
        public boolean canProvide() {
            lastFrame = audioPlayer.provide();
            return lastFrame != null;
        }
        
        @Override
        public ByteBuffer provide20MsAudio() {
            return ByteBuffer.wrap(lastFrame.getData());
        }
        
        @Override
        public boolean isOpus() {
            return true;
        }
    }
}
