# ARCH Web Components

This directory contains the source code for ARCH's frontend [Typescript](https://www.typescriptlang.org/) / [Lit](https://lit.dev/) -based web components.

## Building the Components

### Prerequisites

- [GNU Make](https://www.gnu.org/software/make/)
- `node` (tested with v16.14.2)
- `npm` (tested with v8.5.0)

We recommend using [nvm](https://github.com/nvm-sh/nvm/blob/master/README.md) (node version manager) to install and manage your `node`/`npm` versions: [Installing and Updating](https://github.com/nvm-sh/nvm/blob/master/README.md#installing-and-updating)

### Install the Dependencies

Using a terminal, in this directory (i.e. `.../webapp/src`), execute:

```
npm install
```

### Run [eslint](https://eslint.org/) to check for coding errors

```
make lint
```

Correct or [explicitly ignore](https://eslint.org/docs/latest/use/configure/rules#disabling-rules) any identified warnings and errors.

### Run [prettier](https://prettier.io/) to automatically format the code

```
make format
```

Whether or not each file complies with prettier's notion of correctness will be printed to the console.

### Build the components and update the templates

```
make bundle
```

The built components will be written to the `../js/dist/` directory as `<some-hash>.js` and the templates in `.../webapp/WEB-INF` will be updated to
reflect any new filenames.
