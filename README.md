![ARCH](https://user-images.githubusercontent.com/218561/163210935-fba83e09-56f5-486d-a13f-368a63a66b82.png)

# Archives Research Compute Hub

[![Scala version](https://img.shields.io/badge/Scala%20version-2.12.8-blue)](https://scala-lang.org/)
[![Scalatra version](https://img.shields.io/badge/Scalatra%20version-2.5.4-blue)](https://scalatra.org/)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg)](./LICENSE)

## About

Web application for distributed compute analysis of Archive-It web archive collections.

## Building

### Backend

#### Production

* `sbt "prod/clean" "prod/assembly" "prod/assemblyPackageDependency"`

#### Docker

1. Create a config (`config/config.json`) for your Docker setup, e.g., by copying the included template: `cp config/docker.json config/config.json`
2. Setup a `data` directory somewhere with the following sub-directories: `cache`, `collections`, `in`, `logging`, `out`, `tmp`
3. Build the container: `docker build --no-cache -t arch .`
4. Run the container (example): `docker run -it --rm -p 54040:54040 -p 12341:12341 -v "/home/nruest/Projects/au/sample-data/ars-cloud:/data" -v "/home/nruest/Projects/au/arch:/app" -v "/home/nruest/Projects/au/sample-data/ars-cloud/logging:/logging" arch`

Web application will be available at: [http://localhost:12341/ait](http://localhost:12341/ait), and Apache Spark interface will be available at [http://localhost:54040](http://localhost:54040).

For the `data/input` directory, an example directory structure looks like this:

```
├── in
│   ├── 13529
│   │   └── arcs
│   ├── 13709
│   │   └── arcs
│   ├── 14462
│   │   └── arcs
│   │       ├── ARCHIVEIT-14462-CRAWL_SELECTED_SEEDS-JOB1214854-SEED2299797-20200624234136833-00000-h3.warc.gz
│   │       ├── ARCHIVEIT-14462-CRAWL_SELECTED_SEEDS-JOB1214854-SEED2299798-20200624234136479-00000-h3.warc.gz
│   │       ├── ARCHIVEIT-14462-CRAWL_SELECTED_SEEDS-JOB1214854-SEED2299799-20200624234136645-00000-h3.warc.gz
```

### Frontend

See [webapp/src/README.md](webapp/src/README.md) for information about building the web application.

## Citing ARCH

How to cite ARCH in your research:

> Helge Holzmann, Nick Ruest, Jefferson Bailey, Alex Dempsey, Samantha Fritz, Peggy Lee, and Ian Milligan. 2022. ABCDEF: the 6 key features behind scalable, multi-tenant web archive processing with ARCH: archive, big data, concurrent, distributed, efficient, flexible. In Proceedings of the 22nd ACM/IEEE Joint Conference on Digital Libraries (JCDL '22). Association for Computing Machinery, New York, NY, USA, Article 13, 1–11. https://doi.org/10.1145/3529372.3530916

Your citations help to further the recognition of using open-source tools for scientific inquiry, assists in growing the web archiving community, and acknowledges the efforts of contributors to this project.

## License

[AGPL v3](/LICENSE)

## Open-source, not open-contribution

[Similar to SQLite](https://www.sqlite.org/copyright.html), ARCH is open source but closed to contributions.

The level of complexity of this project means that even simple changes can break a lot of other moving parts in our production environment. However, community involvement, bug reports and feature requests are [warmly accepted](https://support.archive-it.org/hc/en-us/articles/11182827989268-Submit-an-ARCH-support-request).

## Acknowledgments

This work is primarily supported by the [Andrew W. Mellon Foundation](https://mellon.org/). Other financial and in-kind support comes from the [Social Sciences and Humanities Research Council](http://www.sshrc-crsh.gc.ca/), [Compute Canada](https://www.computecanada.ca/), [York University Libraries](https://www.library.yorku.ca/web/), [Start Smart Labs](http://www.startsmartlabs.com/), and the [Faculty of Arts](https://uwaterloo.ca/arts/) at the [University of Waterloo](https://uwaterloo.ca/).

Any opinions, findings, and conclusions or recommendations expressed are those of the researchers and do not necessarily reflect the views of the sponsors.
