![ARCH](https://user-images.githubusercontent.com/218561/163210935-fba83e09-56f5-486d-a13f-368a63a66b82.png)

# Archives Research Compute Hub

[![Scala version](https://img.shields.io/badge/Scala%20version-2.12.8-blue)](https://scala-lang.org/)
[![Scalatra version](https://img.shields.io/badge/Scalatra%20version-2.5.4-blue)](https://scalatra.org/)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg)](./LICENSE)

## About

ARCH is a job server for distributed compute analysis of [WARC](https://en.wikipedia.org/wiki/WARC_(file_format)) file collections.

## Run ARCH using Docker

If you'd like to interact with ARCH via its web-based client, head on over to the [Keystone](https://github.com/internetarchive/keystone) project. Otherwise, follow the steps below to get started using ARCH directly via its API.

### Prerequisites

- For building and running the Docker image
  - [GNU Make](https://www.gnu.org/software/make/manual/make.html)
  - [Docker](https://www.docker.com/)
- For interacting with the ARCH API
  - [curl](https://curl.se/) (or any flexible HTTP client)

### Build the Docker image

```bash
make build-docker-image
```

The `build-docker-image` Make target executes `docker build` while ensuring that:
- A config file (`config/config.json`) is initialized via the Docker config template (`config/docker.json`)
- The `arch` user created within the Docker image has the same `UID` (user ID) as your local user to ensure that any files created within container in a mounted volume will be owned and accessible by your local user

The built image will include a sample collection comprising the WARC file located at: https://archive.org/details/sample-warc-file and indicated by the identifier `SPECIAL-test-collection`. See [Workflow Example](#workflow-example) for an example job run against this collection.

### Run the Docker image

```bash
make run-docker-image
```

The `run-docker-image` Make target executes `docker run` while ensuring that:
- The `shared` directory has been created (see [The "shared" directory](#the-shared-directory))
- The `shared` directory is mounted as a volume within the running container
- ARCH's container server port (`12341`) is forwarded to `localhost`

ARCH is ready to receive API requests when you see the following line displayed in the console output:
```
[info] running (fork) org.archive.webservices.ars.Arch
```

#### Run the Docker image in development mode

```bash
make run-docker-image-dev
```

This execution mode is similar to `run-docker-image` but also mounts your local source code directory into the container which will result in the container running ARCH from the source on your local machine, including any modifications that you may have made.

For the same functionality as above, but with a `bash` session instead of automatically running ARCH, use `make docker-shell`. To then run ARCH, in `/opt/arch` execute `sbt dev/run`.


## Workflow Example

This section provides an example of running a `Domain frequency` job on the sample collection that's included in the Docker image.

See the [Analyze your WARCs](#analyze-your-warcs) for details on how to create your own input collections.

We're using [curl](https://curl.se/) here to interact with the ARCH API, but any flexible HTTP client will work. For a graphical client, you could download a standalone version of [Postman](https://www.postman.com) (note that the web-based version won't work because it can't access your local network).

### List the available ARCH jobs
_Note that we're using the credentials of a default admin user that's defined in `data/arch-users.json` (see [API Authentication](#api-authentication))_
```bash
curl -H "X-API-USER: ks:system" -H "X-API-KEY: supersecret" http://localhost:12341/api/available-jobs
```

Example response:
```
[
  ...
  {
    "categoryName" : "Collection",
    "categoryDescription" : "Discover domain-related patterns and high level information about the documents in a web archive.",
    "jobs" : [
      {
        "uuid" : "01894bc7-ff6a-7e25-a5b5-4570425a8ab7",
        "name" : "Domain frequency",
        "description" : "The number of unique documents collected from each domain in the collection. Output: one CSV file with columns for domain and count.",
        "publishable" : true,
        "internal" : false,
        "codeUrl" : "https://github.com/internetarchive/arch/blob/main/src/main/scala/org/archive/webservices/ars/processing/jobs/DomainFrequencyExtraction.scala",
        "infoUrl" : "https://arch-webservices.zendesk.com/hc/en-us/articles/14410734896148-ARCH-Collection-datasets#domain-frequency"
      },
      ...
    ]
  },
  ...
]
```

### Run a "Domain frequency" job on the default test collection
```bash
curl \
  -XPOST \
  -H "X-API-USER: ks:system" \
  -H "X-API-KEY: supersecret" \
  -H "Content-Type: application/json" \
  --data '{"inputSpec": {"type": "collection", "collectionId": "SPECIAL-test-collection"}}' \
  http://localhost:12341/api/runjob/01894bc7-ff6a-7e25-a5b5-4570425a8ab7
```

Example response:
```json
{
  "id" : "DomainFrequencyExtraction",
  "uuid" : "01912459-6b9a-70f9-be03-203fb1409900",
  "name" : "Domain frequency",
  "sample" : -1,
  "state" : "Running",
  "started" : true,
  "finished" : false,
  "failed" : false,
  "activeStage" : "Processing",
  "activeState" : "Running",
  "startTime" : "2024-08-05T20:57:30.148Z"
}
```

Note that the reported UUID of this example job run is `01912459-6b9a-70f9-be03-203fb1409900`, which we'll need to specify in our follow-up requests.

### Monitor the job's progress

Check the progress of the job by querying the `/api/job/{UUID}/state` endpoint:
```bash
curl \
  -H "X-API-USER: ks:system" \
  -H "X-API-KEY: supersecret" \
  http://localhost:12341/api/job/01912459-6b9a-70f9-be03-203fb1409900/state
```

Example response:
_See response format in [Run a "Domain frequency" job on the default test collection](#run-a-domain-frequency-job-on-the-default-test-collection) above_

The job is done when the reported `state` is `Finished`.

### List the job's output files

List the job's output files by querying the `/api/job/{UUID}/files` endpoint:

```bash
curl \
  -H "X-API-USER: ks:system" \
  -H "X-API-KEY: supersecret" \
  http://localhost:12341/api/job/01912459-6b9a-70f9-be03-203fb1409900/files
```

Example response:
```json
[
  {
    "filename" : "domain-frequency.csv.gz",
    "sizeBytes" : 131,
    "mimeType" : "application/gzip",
    "lineCount" : 3,
    "fileType" : "csv",
    "creationTime" : "2024-08-05T21:02:13.588Z",
    "md5Checksum" : "51e13b611a6fe2219faf9cc6da0be29c",
    "accessToken" : "F36HEDX3IMAVPUFU4AOYYKPPS2TNUMQE"
  }
]
```

### Download a job output file

Download a job output file using the endpoint `/api/job/{UUID}/download/{FILENAME}`:

```bash
curl \
  -H "X-API-USER: ks:system" \
  -H "X-API-KEY: supersecret" \
  http://localhost:12341/api/job/01912459-6b9a-70f9-be03-203fb1409900/download/domain-frequency.csv.gz \
  -o domain-frequency.csv.gz
```

View the output file contents using `zcat`:
```bash
zcat domain-frequency.csv.gz
```

Output:
```
domain, count
wordpress.com,14
wp.com,2
gmpg.org,1
```

## The "shared" directory

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
  - A place to make your own WARCs available to ARCH as inputs (see [Analyze your WARCs](#analyze-your-warcs))


## Analyze Your WARCs

For each group of WARCs that you'd like to analyze as a collection:

1. Create a new subdirectory within `shared/in/collections` with a descriptive kebab-case style name like `my-test-collection` and copy your `*.warc.gz` into it, e.g.
```
shared/
└── in
    └── collections
        └── my-test-collection
            └── ARCHIVEIT-22994-CRAWL_SELECTED_SEEDS-JOB1965703-SEED3267421-h3.warc.gz
```

2. Start (or restart) the ARCH container to automatically make this collection avaiable to the default `ks:system` user. The `/entrypoint.sh` script in the Docker image looks for any newly-added collections on container boot and automatically adds them to `data/special-collection.json`.

3. Follow the same procedure for running a job on this collection as is detailed above in [Run a "Domain frequency" job on the default test collection](#run-a-domain-frequency-job-on-the-default-test-collection), using `SPECIAL-{the_name_of_your_collection_directory}` (e.g. `SPECIAL-my-test-collection`) as the `collectionId`.


## API Authentication

ARCH authenticates API requests by inspecting the `X-API-USER` and `X-API-KEY` HTTP request headers. These values are checked against the entries in `data/arch-users.json`, the default of which defines a single `ks:system` admin user (with plaintext `apiKey` value of `supersecret`) and looks like:
```json
{
  "ks:system" : {
    "name" : "Keystone System",
    "admin" : true,
    "apiKey" : "$pbkdf2-sha512$120000$edRi7uf7Dg18ebkFm5lphcfOAiVVCvRB$vyl48k.uOahDCmTKOqViXpw8FG7fKzkVParjfOZ/60U"
  }
}
```
⚠️ **Be sure to delete this default admin user or roll its API key for any application of ARCH beyond local testing!** ⚠️

- An admin-type API key is authorized to make requests on behalf of any user (indicated by `X-API-USER`)
- A non-admin-type API key is only allowed to make requests on behalf of its owner

You can create additional users or roll the API key of an existing user by manually invoking the `ArchUser.create(name: String, admin: Boolean)` and `ArchUser.rollApiKey(name: String)` methods respectively.


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
