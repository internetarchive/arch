.PHONY: lint
lint: node_modules
	# Project has no *.js files, so skip lint which will complain
	# yarn run lint
	yarn run ts-lint

.PHONY: format
format: node_modules
	# TODO: Make sure prettier doesn't make multiline html string formatting
	# worse:
	# @Mike Wilson, when you  when you run prettier on the web component files,
	# it's gonna do ugly stuff to the embedded multi-line HTML strings.
	# prettier does support proper formatting of html tagged templates, so I've
	# hacked my way into this feature in the partner-site using this dummy html
	# template function (example usage and prettier result). (edited)
	# see: https://internetarchive.slack.com/archives/C02DPK2TKQT/p1646764411485789
	yarn run format

.PHONY: ck-format
ck-format: node_modules
	yarn run ck-format

node_modules: package.json yarn.lock
	yarn install --network-timeout 10000000

.PHONY: dev-bundle
dev-bundle: node_modules
	yarn roll:watch

.PHONY: bundle
bundle: node_modules
	yarn roll

.PHONY: ck-bundle
ck-bundle: bundle
	ifneq ($(shell echo $(shell git diff --name-only dist/index.js | wc -l)), 0)
		$(error "Running 'make bundle' changed 'dist/index.js'. Please commit the production bundle")
	else
		@echo Bundle looks good
	endif


.PHONY: test
test: node_modules
	yarn test:setup
	yarn test



.PHONY: component
component: node_modules
ifndef NAME
	$(error  Please provide a name for the component by passign the NAME env, like `make component NAME=my-component-name`)
endif
	node scripts/new-component.mjs $(NAME)
