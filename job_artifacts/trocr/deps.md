# trocr-models.tar.gz
```sh
curl -OJ https://drive.usercontent.google.com/download?id=1Jk4eGD7crsqCCg9C9VjCLkMN3ze8kutZ&confirm=t
curl -OJ https://drive.usercontent.google.com/download?id=1XSaFwBkOaFOdtk4Ane3DFyJGPRw6v5bO&confirm=t
git lfs install
git clone https://huggingface.co/microsoft/trocr-base-handwritten
tar -czvf trocr-models.tar.gz -C trocr-models .
```

# craft-pytorch.tar.gz
```sh
git clone https://github.com/clovaai/CRAFT-pytorch.git
tar -czvf craft-pytorch.tar.gz -C CRAFT-pytorch .
```