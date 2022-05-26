![ARCH](https://user-images.githubusercontent.com/218561/163210935-fba83e09-56f5-486d-a13f-368a63a66b82.png)

# Archives Research Compute Hub

[![Scala version](https://img.shields.io/badge/Scala%20version-2.12.8-blue)](https://scala-lang.org/)
[![Scalatra version](https://img.shields.io/badge/Scalatra%20version-2.5.4-blue)](https://scalatra.org/)
[![LICENSE](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)](./LICENSE)

## About

Web application for distributed compute analysis of Archive-It web archive collections.

## Building

### Production

* `sbt "prod/clean" "prod/assembly" "prod/assemblyPackageDependency"`

### Docker

1. Create a config (`config/config.json`) for your Docker setup, e.g., by copying the included template: `cp config/docker.json config/config.json`
2. `docker build --no-cache -t arch .`
3. `docker run -it --rm -p 12341:12341 -p 54040:54040 -v /home/nruest/Projects/au/sample-data/arch:/data -v /home/nruest/Projects/au/arch:/app arch`

Web application will be available at: [http://localhost:12341/ait](http://localhost:12341/ait), and Apache Spark interface will be available at [http://localhost:54040](http://localhost:54040).

## Citing ARCH

How to cite the ARCH in your research:

> Helge Holzmann, Nick Ruest, Jefferson Bailey, Alex Dempsey, Samantha Fritz, Peggy Lee, and Ian Milligan. ABCDEF - The 6 key features behind scalable, multi-tenant web archive processing with ARCH: Archive, Big Data, Concurrent, Distributed, Efficient, Flexible. Proceedings of the 2022 IEEE/ACM Joint Conference on Digital Libraries (JCDL 2022), Cologne, Germany.

Your citations help to further the recognition of using open-source tools for scientific inquiry, assists in growing the web archiving community, and acknowledges the efforts of contributors to this project.

## License

[MIT](/LICENSE)

## Acknowledgments

This work is primarily supported by the [Andrew W. Mellon Foundation](https://mellon.org/). Other financial and in-kind support comes from the [Social Sciences and Humanities Research Council](http://www.sshrc-crsh.gc.ca/), [Compute Canada](https://www.computecanada.ca/), [York University Libraries](https://www.library.yorku.ca/web/), [Start Smart Labs](http://www.startsmartlabs.com/), and the [Faculty of Arts](https://uwaterloo.ca/arts/) at the [University of Waterloo](https://uwaterloo.ca/).

Any opinions, findings, and conclusions or recommendations expressed are those of the researchers and do not necessarily reflect the views of the sponsors.
