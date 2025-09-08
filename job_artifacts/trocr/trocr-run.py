import os
import sys
import json
import gc

import torch
import torch.nn as nn
import torch.backends.cudnn as cudnn
from torch.autograd import Variable

from PIL import Image

import cv2
import craft_utils
import imgproc


from skimage import io
from io import BytesIO
import numpy as np
import fitz

from craft import CRAFT
from refinenet import RefineNet

from collections import OrderedDict

from transformers import TrOCRProcessor, VisionEncoderDecoderModel

is_cuda = torch.cuda.is_available()
device = "cuda" if is_cuda else "cpu"

trocr_repo_dir = sys.argv[1]
craft_model = sys.argv[2]
craft_refiner_model = sys.argv[3]
out_pipe_path = sys.argv[-1]

def copyStateDict(state_dict):
    if list(state_dict.keys())[0].startswith("module"):
        start_idx = 1
    else:
        start_idx = 0
    new_state_dict = OrderedDict()
    for k, v in state_dict.items():
        name = ".".join(k.split(".")[start_idx:])
        new_state_dict[name] = v
    return new_state_dict

net = CRAFT()
net.load_state_dict(copyStateDict(torch.load(craft_model, map_location=device)))

net.eval()

refine_net = RefineNet()
refine_net.load_state_dict(copyStateDict(torch.load(craft_refiner_model, map_location=device)))
if is_cuda:
    refine_net = refine_net.cuda()
    refine_net = torch.nn.DataParallel(refine_net)

refine_net.eval()

processor = TrOCRProcessor.from_pretrained(trocr_repo_dir)
model = VisionEncoderDecoderModel.from_pretrained(trocr_repo_dir)

def detect(image): #, text_threshold, link_threshold, low_text, cuda, poly):
    #img_resized, target_ratio, size_heatmap = imgproc.resize_aspect_ratio(image, args.canvas_size, interpolation=cv2.INTER_LINEAR, mag_ratio=args.mag_ratio)
    # img_resized, target_ratio, size_heatmap = imgproc.resize_aspect_ratio(image, 1280, interpolation=cv2.INTER_LINEAR, mag_ratio=1.5)
    ratio_h = ratio_w = 1 #/ target_ratio

    x = imgproc.normalizeMeanVariance(image) #img_resized)
    x = torch.from_numpy(x).permute(2, 0, 1)    # [h, w, c] to [c, h, w]
    x = Variable(x.unsqueeze(0))                # [c, h, w] to [b, c, h, w]
    if is_cuda:
        x.cuda()

    with torch.no_grad():
        y, feature = net(x)

    score_text = y[0,:,:,0].cpu().data.numpy()
    score_link = y[0,:,:,1].cpu().data.numpy()

    with torch.no_grad():
        y_refiner = refine_net(y, feature)
        score_link = y_refiner[0,:,:,0].cpu().data.numpy()

    boxes, polys = craft_utils.getDetBoxes(score_text, score_link, 0.7, 0.4, 0.4, False) # text_threshold, link_threshold, low_text, poly)

    boxes = craft_utils.adjustResultCoordinates(boxes, ratio_w, ratio_h)

    return boxes

def ocr(image, bboxes):
    text = []
    for bbox in bboxes:
        cropped = image[int(bbox[0][1]):int(bbox[2][1]), int(bbox[0][0]):int(bbox[2][0])]
        copped_image = Image.fromarray(cropped).convert("RGB")
        pixel_values = processor(copped_image, return_tensors="pt").pixel_values
        generated_ids = model.generate(pixel_values)
        generated_text = processor.batch_decode(generated_ids, skip_special_tokens=True)[0]
        #print(f"$ {generated_text}")
        text.append(generated_text)
        del copped_image, pixel_values, generated_ids
    return text

def loadImage(img): # see https://github.com/clovaai/CRAFT-pytorch/blob/master/imgproc.py#L11C1-L18C15
    if img.shape[0] == 2: img = img[0]
    if len(img.shape) == 2 : img = cv2.cvtColor(img, cv2.COLOR_GRAY2RGB)
    if img.shape[2] == 4:   img = img[:,:,:3]
    img = np.array(img)
    return img

def loadImageFromFile(img_file):
    return loadImage(io.imread(img_file))

def loadImageFromBytes(image_bytes):
    return loadImage(io.imread(BytesIO(image_bytes)))

def processImage(image):
    bboxes = detect(image) #, args.text_threshold, args.link_threshold, args.low_text, args.cuda, args.poly, refine_net)
    texts = ocr(image, bboxes)
    result = '\n'.join(texts) #bboxes
    del image, bboxes, texts
    if is_cuda:
        torch.cuda.empty_cache()
    gc.collect()
    return result

def process(image_file):
    if image_file.endswith(".pdf"):
        with fitz.open(image_file) as doc:
            return '\n'.join(processPdf(doc))
    else:
        image = loadImageFromFile(image_file)
        return processImage(image)

def processPdf(doc, pagesToImg=True):
    pages = len(doc)
    if pagesToImg:
        zoom = 300 / 72
        mat = fitz.Matrix(zoom, zoom)
        for page_index in range(pages):
            print(f"processing page {page_index} of {pages}")
            page = doc.load_page(page_index)
            pix = page.get_pixmap(matrix=mat)
            image_bytes = pix.tobytes("png")
            image = loadImageFromBytes(image_bytes)
            text = processImage(image)
            print(f"detected: {text}")
            yield text
    else:
        for page_index in range(pages):
            print(f"processing page {page_index} of {pages}")
            for img_index, img in enumerate(doc.get_page_images(page_index)):
                print(f"processing image {img_index}")
                xref = img[0]
                base_image = doc.extract_image(xref)
                image_bytes = base_image["image"]
                image = loadImageFromBytes(image_bytes)
                text = processImage(image)
                print(f"detected: {text}")
                yield text

with open(out_pipe_path, 'w') as pipe:
    while True:
        print("##", file=pipe, flush=True)
        input_file = sys.stdin.readline().strip()
        if not input_file: continue
        print(">>", file=pipe, flush=True)
        # try:
        result = process(input_file)
        print(result, file=pipe, flush=True) #print(json.dumps(result.tolist()))
        # except: pass
