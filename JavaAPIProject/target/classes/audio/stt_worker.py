from flask import Flask, request
import whisper
import os
import tempfile

app = Flask(__name__)
model = whisper.load_model("tiny")  

@app.route('/transcribe', methods=['POST'])
def transcribe():
    
    # Check for audio file in request
    if 'audio' not in request.files:
        print("No audio file in request")
        # Try to debug what's in the request
        print("Request data length:", request.content_length)
        return "No audio file", 400
    
    file = request.files['audio']
    if file.filename == '':
        return "No file selected", 400
    
    # Save to a temporary file
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as tmp_file:
        filename = tmp_file.name
        file.save(filename)
    
    try:
        # Transcribe the audio
        result = model.transcribe(filename)
        text = result['text']
        print("Transcribed:", text)
        
        # Clean up temp file
        os.unlink(filename)
        
        return text, 200
    except Exception as e:
        print("Error during transcription:", str(e))
        # Clean up temp file on error
        if os.path.exists(filename):
            os.unlink(filename)
        return "Internal error", 500

if __name__ == '__main__':
    # Set max content length to handle larger files
    app.config['MAX_CONTENT_LENGTH'] = 100 * 1024 * 1024  # 100MB
    app.run(host='0.0.0.0', port=5005, debug=True)