import sys
import whisper
import json
import torch

is_cuda = torch.cuda.is_available()
device = "cuda" if is_cuda else "cpu"

model_filename = sys.argv[1]
out_pipe_path = sys.argv[-1]

model = whisper.load_model(model_filename, device=device)

def process(audio_file):
    result = model.transcribe(audio_file, fp16=is_cuda)
    return result

with open(out_pipe_path, 'w') as pipe:
    while True:
        print("##", file=pipe, flush=True)
        input_file = sys.stdin.readline().strip()
        try:
            result = process(input_file)
            print(json.dumps(result["segments"]), file=pipe, flush=True)
        except: pass
