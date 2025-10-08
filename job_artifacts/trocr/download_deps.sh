mkdir trocr-models
cd trocr-models

mkdir weights
cd weights

# Download craft_mlt_25k.pth
curl -OJs 'https://drive.usercontent.google.com/download?id=1Jk4eGD7crsqCCg9C9VjCLkMN3ze8kutZ&confirm=t'
# Download craft_refiner_CTW1500.pth
curl -OJs 'https://drive.usercontent.google.com/download?id=1XSaFwBkOaFOdtk4Ane3DFyJGPRw6v5bO&confirm=t'

cd ..

# Clone trocr-base-handwritten repo
## git lfs install
git clone https://huggingface.co/microsoft/trocr-base-handwritten

cd ..

# Clone CRAFT-pytorch repo
git clone https://github.com/clovaai/CRAFT-pytorch.git