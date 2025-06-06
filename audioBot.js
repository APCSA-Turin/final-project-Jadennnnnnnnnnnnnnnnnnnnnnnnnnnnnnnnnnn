const { Client, GatewayIntentBits, Partials } = require('discord.js');
const { joinVoiceChannel, VoiceConnectionStatus, entersState } = require('@discordjs/voice');
const prism = require('prism-media');
const fs = require('fs');
const axios = require('axios');
const FormData = require('form-data');
const wav = require('wav');

const TOKEN = 
const KCHAN_ID = 

const VOLUME_THRESHOLD = 17000;
const SILENCE_DURATION_MS = 1500;

const client = new Client({
  intents: [
    GatewayIntentBits.Guilds,
    GatewayIntentBits.GuildVoiceStates,
    GatewayIntentBits.GuildMessages
  ],
  partials: [Partials.Channel]
});

let connection = null;
let speakingInterval = null;
const recentlyFinished = new Set();

client.once('ready', () => {
  console.log(`Logged in as ${client.user.tag}`);
});

const activeRecordings = {};  // userId -> recording info

client.on('voiceStateUpdate', async (oldState, newState) => {
  // K-chan joined a VC
  if (newState.id === KCHAN_ID && newState.channelId && !connection) {
    const vc = newState.channel;
    connection = joinVoiceChannel({
      channelId: vc.id,
      guildId: vc.guild.id,
      adapterCreator: vc.guild.voiceAdapterCreator,
      selfDeaf: false
    });

    // Wait for connection to be ready
    try {
      await entersState(connection, VoiceConnectionStatus.Ready, 30_000);
    } catch (error) {
      console.error("Voice connection failed:", error);
      connection.destroy();
      return;
    }

    // Use polling to check for speaking users
    speakingInterval = setInterval(() => {
      if (!connection || connection.state.status !== VoiceConnectionStatus.Ready) return;
      
      // Get a fresh receiver each time
      const receiver = connection.receiver;
      const speakingUsers = receiver.speaking.users;
      
      speakingUsers.forEach((speaking, userId) => {
        // Skip recording K-chan herself
        if (userId === KCHAN_ID) {
          return;
        }

        if (speaking && !activeRecordings[userId] && !recentlyFinished.has(userId)) {
          startRecording(userId, receiver);
        }
      });
    }, 100);

    console.log(`K-chan joined ${vc.name}, audio bot joined too.`);
  }

  // K-chan left VC
  if (oldState.id === KCHAN_ID && oldState.channelId && !newState.channelId && connection) {
    if (speakingInterval) {
      clearInterval(speakingInterval);
      speakingInterval = null;
    }
    
    // Clean up all active recordings
    for (const userId in activeRecordings) {
      cleanupRecording(userId);
    }
    
    connection.destroy();
    connection = null;
    recentlyFinished.clear();
    console.log('K-chan left VC, audio bot left too.');
  }
});

function cleanupRecording(userId) {
  const recording = activeRecordings[userId];
  if (!recording) return;
  
  try {
    if (recording.audioStream) {
      recording.audioStream.unpipe();
      recording.audioStream.destroy();
    }
    if (recording.pcmStream) {
      recording.pcmStream.removeAllListeners();
      recording.pcmStream.destroy();
    }
    if (recording.output && !recording.output.closed) {
      recording.output.end();
    }
  } catch (e) {
    console.error('Error cleaning up recording:', e);
  }
  
  delete activeRecordings[userId];
}

function startRecording(userId, receiver) {
  const user = client.users.cache.get(userId);
  if (!user) return;
  const username = user.username;

  console.log(`Starting recording for ${username}`);

  if (!fs.existsSync('./recordings')) fs.mkdirSync('./recordings');

  let filePath = null;
  let output = null;
  let started = false;
  let bufferChunks = [];
  let silenceStart = null;
  let isFinishing = false;

  try {
    const audioStream = receiver.subscribe(userId, {
      end: { behavior: 'manual' }
    });
    
    console.log(`Subscribed to audio stream for ${username}`);
    
    const pcmStream = new prism.opus.Decoder({
      rate: 48000,
      channels: 1,
      frameSize: 960
    });

    // Set up timeout to detect if no data is received
    const dataTimeout = setTimeout(() => {
      if (!started) {
        console.log(`No audio data received for ${username}, cleaning up`);
        cleanupRecording(userId);
      }
    }, 5000);

    audioStream.pipe(pcmStream);
    
    // Add error handlers
    audioStream.on('error', (err) => {
      console.error(`Audio stream error for ${username}:`, err);
      cleanupRecording(userId);
    });
    
    pcmStream.on('error', (err) => {
      console.error(`PCM stream error for ${username}:`, err);
      cleanupRecording(userId);
    });

    const dataHandler = (chunk) => {
      clearTimeout(dataTimeout);
      lastDataTime = Date.now();
      
      if (isFinishing) return;

      let maxSample = 0;
      for (let i = 0; i <= chunk.length - 2; i += 2) {
        const sample = Math.abs(chunk.readInt16LE(i));
        if (sample > maxSample) maxSample = sample;
      }

      const now = Date.now();

      if (!started) {
        if (maxSample >= VOLUME_THRESHOLD) {
          filePath = `./recordings/${username}-${now}.pcm`;
          output = fs.createWriteStream(filePath);
          started = true;
          for (const bufferedChunk of bufferChunks) {
            output.write(bufferedChunk);
          }
          output.write(chunk);
          bufferChunks = null;
          silenceStart = null;
          console.log(`Started recording for ${username}`);
          
          output.on('finish', () => {
            if (filePath && fs.existsSync(filePath)) {
              sendToWhisper(filePath, username);
            }
          });
        } else {
          bufferChunks.push(chunk);
          if (bufferChunks.length > 100) {
            bufferChunks.shift();
          }
        }
      } else {
        output.write(chunk);

        if (maxSample >= VOLUME_THRESHOLD) {
          silenceStart = null;
        } else {
          if (!silenceStart) silenceStart = now;
          if (now - silenceStart > SILENCE_DURATION_MS && !isFinishing) {
            isFinishing = true;
            console.log(`Silence for ${SILENCE_DURATION_MS}ms detected, stopped recording for ${username}`);
            
            if (activeRecordings[userId]) {
              activeRecordings[userId].isFinishing = true;
            }
            
            pcmStream.removeListener('data', dataHandler);
            
            cleanupRecording(userId);
            
            if (output && !output.closed) {
              output.end();
            }
            
            return;
          }
        }
      }
    };

    pcmStream.on('data', dataHandler);

    pcmStream.on('end', () => {
      console.log(`PCM stream ended for ${username}`);
      if (output && !output.closed) {
        output.end();
      }
    });

    activeRecordings[userId] = { 
      output, 
      filePath, 
      username, 
      audioStream, 
      pcmStream,
      isFinishing: false
    };
    
  } catch (error) {
    console.error(`Error starting recording for ${username}:`, error);
    cleanupRecording(userId);
  }
}

function pcmToWav(pcmPath, wavPath) {
  return new Promise((resolve, reject) => {
    const reader = fs.createReadStream(pcmPath);
    const writer = new wav.FileWriter(wavPath, {
      channels: 1,
      sampleRate: 48000,
      bitDepth: 16,
    });
    reader.pipe(writer);
    writer.on('finish', resolve);
    writer.on('error', reject);
  });
}

function isLoudEnough(wavPath, threshold = VOLUME_THRESHOLD) {
  const buf = fs.readFileSync(wavPath);
  for (let i = 44; i < buf.length; i += 2) {
    const sample = buf.readInt16LE(i);
    if (Math.abs(sample) > threshold) return true;
  }
  return false;
}

async function sendToWhisper(filePath, username) {
  const minBytes = 48000 * 2 * 1.0;
  const size = fs.statSync(filePath).size;
  if (size < minBytes) {
    console.log(`Audio file too short, skipping: ${filePath} (${size} bytes)`);
    fs.unlinkSync(filePath);
    return;
  }

  const wavPath = filePath.replace('.pcm', '.wav');
  await pcmToWav(filePath, wavPath);

  if (!isLoudEnough(wavPath, VOLUME_THRESHOLD)) {
    console.log(`Audio file is too quiet, skipping: ${wavPath}`);
    fs.unlinkSync(filePath);
    fs.unlinkSync(wavPath);
    return;
  }

  if (!fs.existsSync(wavPath)) {
    console.error(`WAV file missing at send time: ${wavPath}`);
    return;
  }
  console.log("About to send:", wavPath, fs.existsSync(wavPath) ? "exists" : "DOES NOT EXIST");

  const audioBuffer = fs.readFileSync(wavPath);
  
  const form = new FormData();
  form.append('audio', audioBuffer, {
    filename: 'audio.wav',
    contentType: 'audio/wav'
  });

  try {
      const res = await axios.post('http://localhost:5005/transcribe', form, {
        headers: {
          ...form.getHeaders()
        },
        maxContentLength: Infinity,
        maxBodyLength: Infinity,
      });
      const text = res.data;
      console.log(`Raw transcription: "${text}"`);
      if (typeof text === 'string' && text.trim()) {
        console.log(`[${username}]: ${text}`);
        
        await axios.post('http://localhost:4567/addTranscribed', null, {
          params: {
            username: username,
            message: text
          }
        });
      }
  } catch (err) {
      console.error("Transcription error:", err.response ? err.response.data : err);
  }
  
  fs.unlinkSync(filePath);
  fs.unlinkSync(wavPath);
  
  const userId = Object.keys(activeRecordings).find(id => 
    activeRecordings[id] && activeRecordings[id].username === username
  );
  if (userId) {
    console.log(`Adding ${username} to cooldown`);
    recentlyFinished.add(userId);
    setTimeout(() => {
      recentlyFinished.delete(userId);
      console.log(`${username} can now be recorded again`);
    }, 1000); // 1 second cooldown
  }
}

client.login(TOKEN);