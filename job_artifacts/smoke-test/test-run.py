import os
import sys
import json

out_pipe_path = sys.argv[-1]

with open(out_pipe_path, 'w') as pipe:
    while True:
        print("##", file=pipe, flush=True)
        input_file = sys.stdin.readline().strip()
        if not input_file: continue
        print(">>", file=pipe, flush=True)
        print(f"Length: {os.path.getsize(input_file)}", file=pipe, flush=True)
