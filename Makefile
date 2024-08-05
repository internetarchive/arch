
# Read the current user's ID so that we can assign the same ID to "arch" Docker user.
UID = $$(id -u)

IMAGE_NAME=ait-arch

config/config.json:
	cp config/docker.json config/config.json

.PHONY: build-docker-image
build-docker-image: config/config.json
	docker build --build-arg UID=$(UID) . -t $(IMAGE_NAME)

shared:
	mkdir -p shared/in/collections; \
	mkdir shared/log; \
	mkdir -p shared/out/custom-collections; \
	mkdir shared/out/datasets;

.PHONY: run-docker-image
run-docker-image: shared
	docker run --rm -it -v ./shared:/opt/arch/shared -p 12341:12341 $(IMAGE_NAME)

lib/.symlinks-copied:
	docker cp $$(docker create --name arch-tmp $(IMAGE_NAME)):/opt/arch/lib . \
	&& docker rm arch-tmp \
	&& touch lib/.symlinks-copied

.PHONY: run-docker-image-dev
run-docker-image-dev: shared lib/.symlinks-copied
	docker run --rm -it -v ./:/opt/arch -p 12341:12341 $(IMAGE_NAME)

.PHONY: docker-shell
docker-shell: shared lib/.symlinks-copied
	docker run --rm -it -v ./:/opt/arch -p 12341:12341 $(IMAGE_NAME) bash
