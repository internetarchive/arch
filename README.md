![ARCH](https://user-images.githubusercontent.com/218561/163210935-fba83e09-56f5-486d-a13f-368a63a66b82.png)

# Archives Research Compute Hub

[![Scala version](https://img.shields.io/badge/Scala%20version-2.12.8-blue)](https://scala-lang.org/)
[![Scalatra version](https://img.shields.io/badge/Scalatra%20version-2.5.4-blue)](https://scalatra.org/)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg)](./LICENSE)

## About

Web application for distributed compute analysis of Archive-It web archive collections.

### Run ARCH using Docker

#### Prerequisites

- [GNU Make](https://www.gnu.org/software/make/manual/make.html)
- [Docker](https://www.docker.com/)

#### Build and Run the Docker Image

##### 1. Build the image
```
make build-docker-image
```

##### 2. Run the image
```
make run-docker-image
```

##### 3. Surf on over to [http://127.0.0.1:12341](http://127.0.0.1:12341)

##### 4. Log in with username: `test` password: `password`

#### The "shared" Directory

The `run-docker-image` Make target will create a local `shared` subdirectory that, along with
the local ARCH source code directory itself, will be mounted within the running container to
serve as the storage destination for ARCH outputs, and as a place to add your own custom collections
of WARCs for analysis.

The `shared` directory has the structure:

```
shared/
├── in
│   └── collections
├── log
└── out
    ├── custom-collections
    └── datasets
```

These subdirectories are utilized as follows:
- `log`
  - ARCH job logs
- `out/custom-collections`
  - ARCH Custom Collection output files
- `out/datasets`
  - ARCH Dataset output files
- `in/collections`
  - A place to make your own WARCs available to ARCH as inputs - see "Analyze Your WARCs" below

##### Analyze Your WARCs

For each group of WARCs that you'd like to analyze as a collection:

1. Create a new subdirectory within `shared/in/collections` with a descriptive kebab-case style name like `my-test-collection` and copy your `*.warc.gz` into it, e.g.
```
shared/
├── in
    └── collections
        └── my-test-collection
            └── ARCHIVEIT-22994-CRAWL_SELECTED_SEEDS-JOB1965703-SEED3267421-h3.warc.gz
```

2. Start (or restart) the ARCH container to automatically include this new collection in your list of available collections. The name will be a title-cased version of the kebab-case directory name, e.g. `my-test-collection` becomes `My Test Collection`.

#### ARCH Development

Use the `docker-shell` Make target to get a `bash` shell inside a running container:
```
make docker-shell
```

Your local ARCH source code directory will be mounted at `/opt/arch`; from within this directory in the container shell, run ARCH using:
```
sbt dev/run
```

After modifying the ARCH source on your local machine, `ctrl-c` and rerun the `sbt dev/run` to assemble/run your new code.


### Modifying the Web App

See [webapp/src/README.md](webapp/src/README.md) for information about building the web application.

## Citing ARCH

How to cite ARCH in your research:

> Helge Holzmann, Nick Ruest, Jefferson Bailey, Alex Dempsey, Samantha Fritz, Peggy Lee, and Ian Milligan. 2022. ABCDEF: the 6 key features behind scalable, multi-tenant web archive processing with ARCH: archive, big data, concurrent, distributed, efficient, flexible. In Proceedings of the 22nd ACM/IEEE Joint Conference on Digital Libraries (JCDL '22). Association for Computing Machinery, New York, NY, USA, Article 13, 1–11. https://doi.org/10.1145/3529372.3530916

Your citations help to further the recognition of using open-source tools for scientific inquiry, assists in growing the web archiving community, and acknowledges the efforts of contributors to this project.

## License

[AGPL v3](/LICENSE)

## Open-source, not open-contribution

[Similar to SQLite](https://www.sqlite.org/copyright.html), ARCH is open source but closed to contributions.

The level of complexity of this project means that even simple changes can break a lot of other moving parts in our production environment. However, community involvement, bug reports and feature requests are [warmly accepted](https://arch-webservices.zendesk.com/hc/en-us/requests/new).

## Acknowledgments

This work is primarily supported by the [Andrew W. Mellon Foundation](https://mellon.org/). Other financial and in-kind support comes from the [Social Sciences and Humanities Research Council](http://www.sshrc-crsh.gc.ca/), [Compute Canada](https://www.computecanada.ca/), [York University Libraries](https://www.library.yorku.ca/web/), [Start Smart Labs](http://www.startsmartlabs.com/), and the [Faculty of Arts](https://uwaterloo.ca/arts/) at the [University of Waterloo](https://uwaterloo.ca/).

Any opinions, findings, and conclusions or recommendations expressed are those of the researchers and do not necessarily reflect the views of the sponsors.
