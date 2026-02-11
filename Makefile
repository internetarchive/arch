
# Read the current user's ID so that we can assign the same ID to "arch" Docker user.
UID = $$(id -u)

PWD=$(shell pwd)
IMAGE=ait-arch
BASE_IMAGE=$(shell grep '^ARG BASE_IMAGE=' Dockerfile | cut -d'=' -f2)
RUN_BASE_CMD=docker run --rm -it -v $(PWD)/shared:/opt/arch/shared -p 12341:12341
GPU_TEST_CMD=docker run --gpus all $(BASE_IMAGE)
CPU_MSG_CMD=printf '*\n* GPU not available: Whisper and TrOCR jobs will use the CPU\n*\n'

config/config.json:
	cp config/docker.json config/config.json

.PHONY: build-docker-image
build-docker-image: config/config.json
	cp .gitignore .dockerignore; \
	docker buildx build --platform linux/amd64 --build-arg UID=$(UID) . -t $(IMAGE) --load

# Note: For ARM64 build, you need to provide the SHA256 checksum for the ARM64 Miniconda installer.
# Get it with: curl -sL https://repo.anaconda.com/miniconda/Miniconda3-py310_25.1.1-2-Linux-aarch64.sh | sha256sum
.PHONY: build-docker-image-arm64
build-docker-image-arm64: config/config.json
	cp .gitignore .dockerignore; \
	docker buildx build --platform linux/arm64 \
		--build-arg CONDA_INSTALL_SCRIPT=Miniconda3-py310_25.1.1-2-Linux-aarch64.sh \
		--build-arg CONDA_INSTALL_SCRIPT_SHA256=5f61143e93d9d48a82aa99a1d7b1c77561f599b9a67ab954862e6e8d6a25c0cc \
		--build-arg UID=$(UID) . -t $(IMAGE) --load

shared:
	mkdir -p shared/in/collections; \
	mkdir shared/log; \
	mkdir -p shared/out/custom-collections; \
	mkdir shared/out/datasets;

# Define a function that first tests running the BASE_IMAGE with the --gpus option,
# and if that doesn't exit with an error, runs ARCH with GPU support, and otherwise
# displays a message indicating that AI jobs will use the CPU and ARCH without GPU support.
# usage: $(call run_docker_image_fn,<extraRunOptions>,<containerCommand>)
# The "$(or $(1),--it)" adds quotes with are necessary when a first argument value is
# specified and defaults to the redundant "-it" when one is not specified to prevent docker
# run from complaining about "invalid reference format".
define run_docker_image_fn
	$(eval GPU_RUN_CMD=$(RUN_BASE_CMD) "$(or $(1),-it)" --gpus all $(IMAGE) $(2))
	$(eval CPU_RUN_CMD=$(RUN_BASE_CMD) "$(or $(1),-it)" $(IMAGE) $(2))
	@$(GPU_TEST_CMD) 2>/dev/null && ($(GPU_RUN_CMD) || true) || ($(CPU_MSG_CMD) && $(CPU_RUN_CMD))
endef

.PHONY: run-docker-image
run-docker-image: shared
	$(call run_docker_image_fn,"-v $(PWD)/config/docker.json:/opt/arch/config/config.json:ro")

lib/.symlinks-copied:
	docker cp $$(docker create --name arch-tmp $(IMAGE)):/opt/arch/lib . \
	&& docker rm arch-tmp \
	&& touch lib/.symlinks-copied

.PHONY: run-docker-image-dev
run-docker-image-dev: shared lib/.symlinks-copied
	$(call run_docker_image_fn,"-v $(PWD):/opt/arch")

.PHONY: docker-shell
docker-shell: shared lib/.symlinks-copied
	$(call run_docker_image_fn,"-v $(PWD):/opt/arch","bash")
