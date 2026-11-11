<a id="btop"></a>
<!-- README setup inspired by https://github.com/othneildrew/Best-README-Template -->

<!-- === PROJECT LOGO === -->
<br />
<div align="center"><!-- github does not allow css, hence we use html -->
  <a href="">
    <img src="images/vitrivr_512.png" width="512" alt="vitrivr log"/><br /><br /><br />
    <img src="images/vengine-256.png" alt="vitrivr-engine logo"/>
  </a>
 <br/>
 <br/>

[![GitHub Release](https://img.shields.io/github/release/vitrivr/vitrivr-engine?include_prereleases=&sort=semver&color=blue&style=for-the-badge&label=Release)](https://github.com/vitrivr/vitrivr-engine/releases)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](#license)
[![issues - vitrivr-engine](https://img.shields.io/github/issues/vitrivr/vitrivr-engine?style=for-the-badge)](https://github.com/vitrivr/vitrivr-engine/issues)

[vitrivr](https://vitrivr.org)'s next-generation retrieval engine.

[Read The Docs](https://github.com/vitrivr/vitrivr-engine/wiki)

</div>

## vitrivr-engine

vitrivr-engine is [vitrivr](https://vitrivr.org)'s next generation retrieval engine with a flexible, modular
architecture.
Based on the experiences with its predecessor, [Cineast](https://github.com/vitrivr/cineast),
vitrivr engine's data model, ingestion pipeline and retrieval logic have been reworked from the ground.
Essentially, vitrivr-engine enables the analysis (i.e. ingestion) and querying (i.e. retrieval ) of
multimedia data.

## Built With

* [Kotlin](https://kotlinlang.org) for the JVM, e.g. [OpenJDK](https://openjdk.org/)
* [OpenApi](https://www.openapis.org/)
* [PosgreSQL with pgvector](https://www.postgresql.org/)
* [CottontailDB](https://github.com/vitrivr/CottontailDB)
* ... and more ...

## Getting Started and Example Usage

This guide shows how to build and run vitrivr-engine either by [manually building](#build-and-extract) it or using the [Docker setup](#docker-setup).

### Getting Started

This guide shows how to build and run `vitrivr-engine` using the provided ZIP distribution and a schema configuration
file.

#### Prerequisites

* JDK 21 or higher (e.g., [OpenJDK](https://openjdk.org))
* A running database:
    * [PostgreSQL with `pgVector`](https://github.com/pgvector/pgvector) (recommended) **or**
    * [CottontailDB](https://github.com/vitrivr/cottontaildb) (v0.16.5+)

* Some multimedia content (images or video)

---

##### Build and Extract

1. Clone the repository and build the distribution:

   ```bash
   git clone https://github.com/vitrivr/vitrivr-engine.git
   cd vitrivr-engine
   ./gradlew distZip
   ```

2. Unzip the distribution:

   ```bash
   unzip -d vitrivr-engine-server/build/distributions/vitrivr-engine-server-*.zip
   ```

3. Prepare a folder structure with your media (e.g. `sandbox/media/`):

   ```
   vitrivr-engine/
   └── vitrivr-engine-server-<version>/
       ├── bin/
       └── lib/
   └── sandbox/
       └── media/
           ├── image1.png
           └── video.mp4
   ```
4. Clone and run the external feature extraction service if you want to use CLIP or DINO for feature extraction:
    ```bash
   git clone https://github.com/vitrivr/vitrivr-python-descriptor-server
   cd vitrivr-python-descriptor-server
   python3 -m venv features
   source features/bin/activate
   pip install -r requirements.txt
   python3 main.py
   ```

---

##### Docker Setup

Alternatively, you can use the Docker image to run `vitrivr-engine`: With the help of the docker-compose file, you can
set up a PostgreSQL database with `pgVector`, an external instance to extract features and the `vitrivr-engine` server. 
Note: The video feature extraction using the docker image is currently not supported when running on Apple Silicon 
(e.g., M1, M2, M3) due to architecture compatibility issues.

Run the following command to start the Docker containers:

   ```bash
   docker compose up --build
   ```

This command will start the PostgreSQL database, the feature extraction service, and the `vitrivr-engine` server.
All configuration files are mounted from the `vitrivr-engine-server/config` directory, so you can easily modify them and
restart the containers to apply changes. Also, all data is mounted to the `vitrivr-engine-server/data` directory, so you
can easily access the data files.

##### Docker Setup with FES-backed CLIP

To try the current FES integration without replacing the existing descriptor-server setup, use the separate Compose file:

```bash
docker compose -p vitrivr-byol -f docker-compose.fes.yml up --build -d
```

This builds the engine from this checkout and starts the FES OpenCLIP ViT B32 service. The engine is available at
`http://localhost:7074/swagger-ui`; CLIP FES is exposed at `http://localhost:8889`, and BYOL/Painter FES at
`http://localhost:8890`. Put images in `sandbox/media`, then
initialize the `sandbox` schema and run the image ingestion pipeline as described below. The FES schema configuration
uses `DenseEmbedding` with a 512-dimensional vector field, and the existing retrieval pipeline can continue querying
the `clip` field with text or an image.

#### Text and Image Retrieval

The FES `DenseEmbedding` field supports both `TEXT` and `IMAGE` query inputs. The text request body is available in
[`example-configs/fes/retrieval-text.json`](example-configs/fes/retrieval-text.json) and can be pasted into Swagger's
`POST /api/{schema}/query` request body. Set the `schema` path value to `sandbox`.

For an image query, run the PowerShell helper from the repository root:

```powershell
./example-configs/fes/query-image.ps1 -ImagePath ./sandbox/media/demo-fox.png -Schema sandbox
```

The helper encodes the image as a Base64 data URL and submits it to the same `clip` field. Use `-Limit 20` to change
the number of returned results. Query responses contain retrievable IDs and scores; to preview a result, request
`GET /api/sandbox/fetch/thumbnail/{retrievableId}` using its returned ID.

#### Simple Search UI

A demo page at [`example-configs/fes/search-demo.html`](example-configs/fes/search-demo.html) supports text
search, reference-image search with CLIP, iART BYOL, iART Painter style/genre, or the AverageColor feature, cluster
filtering, and browsing the indexed collection. AverageColor is a basic color-similarity baseline; it is not a copy
of iART's original color plugins. The BYOL and Painter checkpoints are supplied separately and are not stored in Git;
see [`BYOL_IMAGE_SEARCH.md`](BYOL_IMAGE_SEARCH.md) and [`PAINTER_IMAGE_SEARCH.md`](PAINTER_IMAGE_SEARCH.md). With the
FES Compose stack running and images already indexed in the `sandbox` schema, serve the page from a
second PowerShell window at the repository root:

```powershell
py -m http.server 7072 --directory example-configs/fes
```

Open `http://localhost:7072/search-demo.html`. The page uses `http://127.0.0.1:7074/api/sandbox` by default. To point
it at a different API and schema, add an `api` query parameter, for example:
`http://localhost:7072/search-demo.html?api=http://127.0.0.1:7070/api/clusteringdemo`.

For either query type, the pipeline is the same after embedding: FES creates a 512-dimensional CLIP vector, the
`clip` field compares it with indexed image vectors using cosine distance, and pgvector returns the nearest images
first. The engine attaches a similarity `score` to each result. It is a ranking score, not a percentage or a
probability. The result ID can be used with the configured thumbnail exporter to fetch the image bytes.

The image-only BYOL and Painter fields require fresh ingestion after their fields are added to the schema. Text search
continues to use CLIP. Painter style and genre are separate 256-dimensional image descriptors.

The FES image pipeline also supports zero-shot image classification. It uses the `ImageClassification` field and the
`open-clip-vit-b32` model to compare each image with the candidate classes listed in
`example-configs/fes/ingestion-image.json`. The pipeline stores the highest-scoring label and its confidence for each
image. Change the comma-separated `classes` value to match the categories for your collection.

To see the label in query results, add a `FieldLookup` operation after CLIP retrieval in Swagger:

```json
{
  "inputs": {
    "txt": { "type": "TEXT", "data": "a fox in a forest" }
  },
  "operations": {
    "clip": {
      "field": "clip",
      "inputs": { "txt": "txt" },
      "parameters": { "limit": "5" }
    },
    "classification": {
      "factory": "FieldLookup",
      "inputs": { "in": "clip" },
      "parameters": { "field": "classification" }
    }
  },
  "output": "classification"
}
```

The response includes `classification.label` and `classification.confidence` for each retrieved image. The confidence is
the model score, not a percentage.

#### Setup Configuration

Create a `config-schema.json` file to define your schema, fields, and backend. Make sure to adjust the database
connection parameters to match your PostgreSQL or CottontailDB setup. The following example uses PostgreSQL with
`pgVector` and defines fields for image and video ingestion, including a CLIP feature extractor and a thumbnail
exporter.

If you are using Docker, please review the config files in `example-configs/docker`.

<details>
<summary><code>config-schema.json</code></summary>

```json
{
  "schemas": {
    "sandbox": {
      "connection": {
        "database": "PgVectorConnectionProvider",
        "parameters": {
          "host": "127.0.0.1",
          "port": "5432",
          "database": "postgres",
          "username": "postgres",
          "password": "secret"
        }
      },
      "fields": {
        "averagecolor": {
          "factory": "AverageColor"
        },
        "file": {
          "factory": "FileSourceMetadata"
        },
        "time": {
          "factory": "TemporalMetadata"
        },
        "clip": {
          "factory": "CLIP",
          "parameters": {
            "host": "http://127.0.0.1:8888"
          }
        }
      },
      "resolvers": {
        "disk": {
          "factory": "DiskResolver",
          "parameters": {
            "location": "./sandbox/thumbnails/",
            "mimeType": "JPG"
          }
        }
      },
      "exporters": {
        "thumbnail": {
          "factory": "ThumbnailExporter",
          "parameters": {
            "maxSideResolution": "400",
            "mimeType": "JPG",
            "resolver": "disk"
          }
        }
      },
      "extractionPipelines": {
        "image": {
          "path": "/vitrivr-engine/example-configs/native/image-ingest.json"
        },
        "video": {
          "path": "/vitrivr-engine/example-configs/native/video-ingest.json"
        }
      }
    }
  }
}


```

</details>

---

#### Setup Pipelines

Depending on your media type, you can create individual ingestion pipelines for images and videos. The following example
shows how to set up pipelines for image and video ingestion:
The fields used here are defined in the `config-schema.json` file, and the pipelines are already defined in the
`extractionPipelines` section of the `config-schema.json`.
<details>
<summary><code>image-ingest.json</code></summary>

```json
{
  "schema": "sandbox",
  "context": {
    "contentFactory": "InMemoryContentFactory",
    "resolvers": [
      "disk"
    ],
    "local": {
      "enumerator": {
        "path": "./media",
        "depth": "1"
      },
      "thumbnail": {
        "maxSideResolution": "400",
        "mimeType": "JPG"
      }
    }
  },
  "operators": {
    "enumerator": {
      "type": "ENUMERATOR",
      "factory": "FileSystemEnumerator",
      "mediaTypes": [
        "IMAGE"
      ]
    },
    "decoder": {
      "type": "DECODER",
      "factory": "ImageDecoder"
    },
    "persister": {
      "type": "TRANSFORMER",
      "factory": "PersistRetrievableTransformer"
    },
    "thumbnail": {
      "type": "EXPORTER",
      "exporterName": "thumbnail"
    },
    "clip": {
      "type": "EXTRACTOR",
      "fieldName": "clip"
    },
    "file": {
      "type": "EXTRACTOR",
      "fieldName": "file"
    },
    "averagecolor": {
      "type": "EXTRACTOR",
      "fieldName": "averagecolor"
    }
  },
  "operations": {
    "enumerator": {
      "operator": "enumerator"
    },
    "decoder": {
      "operator": "decoder",
      "inputs": [
        "enumerator"
      ]
    },
    "persist": {
      "operator": "persister",
      "inputs": [
        "decoder"
      ]
    },
    "thumbnail": {
      "operator": "thumbnail",
      "inputs": [
        "persist"
      ]
    },
    "clip": {
      "operator": "clip",
      "inputs": [
        "thumbnail"
      ]
    },
    "averagecolor": {
      "operator": "averagecolor",
      "inputs": [
        "clip"
      ]
    },
    "file": {
      "operator": "file",
      "inputs": [
        "averagecolor"
      ]
    }
  },
  "output": [
    "file"
  ]
}


```

</details>

<details>
<summary><code>video-ingest.json</code></summary>

```json
{
  "schema": "sandbox",
  "context": {
    "contentFactory": "InMemoryContentFactory",
    "resolvers": [
      "disk"
    ],
    "local": {
      "enumerator": {
        "path": "/sandbox/media",
        "depth": "1"
      },
      "thumbnail": {
        "maxSideResolution": "400",
        "mimeType": "JPG"
      },
      "filter": {
        "type": "SOURCE:VIDEO"
      },
      "decoder": {
        "timeWindowMs": "5000"
      }
    }
  },
  "operators": {
    "enumerator": {
      "type": "ENUMERATOR",
      "factory": "FileSystemEnumerator",
      "mediaTypes": [
        "VIDEO"
      ]
    },
    "decoder": {
      "type": "DECODER",
      "factory": "VideoDecoder"
    },
    "persister": {
      "type": "TRANSFORMER",
      "factory": "PersistRetrievableTransformer"
    },
    "aggregator": {
      "type": "TRANSFORMER",
      "factory": "LastContentAggregator"
    },
    "time": {
      "type": "EXTRACTOR",
      "fieldName": "time"
    },
    "thumbnail": {
      "type": "EXPORTER",
      "exporterName": "thumbnail"
    },
    "clip": {
      "type": "EXTRACTOR",
      "fieldName": "clip"
    },
    "file": {
      "type": "EXTRACTOR",
      "fieldName": "file"
    },
    "averagecolor": {
      "type": "EXTRACTOR",
      "fieldName": "averagecolor"
    },
    "filter": {
      "type": "TRANSFORMER",
      "factory": "TypeFilterTransformer"
    }
  },
  "operations": {
    "enumerator": {
      "operator": "enumerator"
    },
    "decoder": {
      "operator": "decoder",
      "inputs": [
        "enumerator"
      ]
    },
    "aggregator": {
      "operator": "aggregator",
      "inputs": [
        "decoder"
      ]
    },
    "persist": {
      "operator": "persister",
      "inputs": [
        "aggregator"
      ]
    },
    "thumbnail": {
      "operator": "thumbnail",
      "inputs": [
        "persist"
      ]
    },
    "clip": {
      "operator": "clip",
      "inputs": [
        "thumbnail"
      ]
    },
    "averagecolor": {
      "operator": "averagecolor",
      "inputs": [
        "clip"
      ]
    },
    "time": {
      "operator": "time",
      "inputs": [
        "averagecolor"
      ]
    },
    "file": {
      "operator": "file",
      "inputs": [
        "time"
      ]
    }
  },
  "output": [
    "file"
  ]
}

```

</details>

---

#### Start the Server

Navigate to the distribution:

```bash
cd ../vitrivr-engine-server-<version> 
./bin/vitrivr-engine-server config-schema.json
```

Inside the interactive CLI:

```bash
v> sandbox init         # Initializes the schema (sandbox = your schema name)
```


```bash
v> sandbox about         # Check the initialization state the schema (sandbox = your schema name)
```

If you want to ingest media files, you can use the following commands:

```bash
v> sandbox extract -n image # Ingest all images through the specified pipeline
v> sandbox extract -n video # Ingest all videos through the specified pipeline
```

Or if you are using Docker, you can run the following command:

```bash
docker attach vitrivr-engine-vitrivr-engine-1
sandbox init
sandbox extract -n image
sandbox extract -n video
```

You can now access the REST API at [http://localhost:7070](http://localhost:7070) and query the system.

---

#### Example Query

Use the OpenAPI Swagger UI or send a query. A simple example query to retrieve the multimedia content based on a text
input could look like this. For this, the CLIP feature is used. Inside `input-text`, you can provide a description of
the content you want to retrieve, such as "an orange starfish on the seafloor". The query will return the relevant media
files based on the CLIP feature extraction.

```bash

```json
{
  "inputs": {
    "input-text": {"type": "TEXT", "data": "an orange starfish on the seafloor"}
  },
  "operations": {
    "clip" : {"type": "RETRIEVER", "field": "clip", "input": "input-text"},
    "relations" : {"type": "TRANSFORMER", "transformerName": "RelationExpander", "input": "clip"},
    "lookup" : {"type": "TRANSFORMER", "transformerName": "FieldLookup", "input": "relations"},
    "aggregator" : {"type": "TRANSFORMER", "transformerName": "ScoreAggregator",  "input": "lookup"},
    "filelookup" : {"type": "TRANSFORMER", "transformerName": "FieldLookup", "input": "aggregator"}
  },
  "context": {
    "global": {
      "limit": "1000"
    },
    "local" : {
      "lookup":{"field": "time", "keys": "start, end"},
      "relations" :{"outgoing": "partOf"},
      "filelookup": {"field": "file", "keys": "path"}
    }
  },
  "output": "filelookup"
}
```

---

## Project Structure

The project is set up as a multi-module Kotlin project:

| Module                                                                                                                | Description                                                                                                                                                                                                                              | Maven Dependency |
|-----------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|
| [`vitrivr-engine-core`](https://github.com/vitrivr/vitrivr-engine//vitrivr-engine-core)                               | The core library of the project, which provides basic interfaces & classes.                                                                                                                                                              | Yes              |
| [`vtirivr-engine-query`](https://github.com/vitrivr/vitrivr-engine//vitrivr-engine-query)                             | Query / retrieval related extension to the core library with various retrievers and data manipulation operators.                                                                                                                         | Yes              |
| [`vitrivr-engine-index`](https://github.com/vitrivr/vitrivr-engine//vitrivr-engine-index)                             | Indexing / ingestion related extension to the core library with various decoders and segmenters.                                                                                                                                         | Yes              |
| [`vitrivr-engine-module-cottontaildb`](https://github.com/vitrivr/vitrivr-engine//vitrivr-engine-plugin-cottontaildb) | The database driver for the [CottontailDB](https://github.com/vitrivr/cottontaildb) database, used for NNNS and other queries.                                                                                                           | Yes              |
| [`vitrivr-engine-module-features`](https://github.com/vitrivr/vitrivr-engine//vitrivr-engine-plugin-features)         | Extension that contains specific indexing and retrieval implementations such as fulltext, colour, etc.                                                                                                                                   | Yes              |
| [`vitrivr-engine-module-m3d`](https://github.com/vitrivr/vitrivr-engine//vitrivr-engine-module-m3d)                   | Extension related to 3d model indexing and retrieval. Contains various feature modules and capability to process meshes.                                                                                                                 | Yes              |
| [`vitrivr-engine-module-fes`](https://github.com/vitrivr/vitrivr-engine//vitrivr-engine-module-fes)                   | Extension that can be used to harnes feature extraction provided by an external ML model server. **Requires local generation of bindings:** `./gradlew :vitrivr-engine-module-fes:generateFESClient`                                     | Yes              |
| [`vitrivr-engine-server`](https://github.com/vitrivr/vitrivr-engine//vitrivr-engine-server)                           | A [Javalin](https://javalin.io) powered server providing an [OpenApi](https://openapis.org) [documented REST API](vitrivr-engine-server/doc/oas.json) for both, ingestion and querying and a CLI, essentially the runtime of the project | No               |

## Contributing

We welcome contributors. Please fork the repo and open a pull-request with your work.
A good starting point are the 'good first issue' issues.

## Contributors

* @ppanopticon
* @lucaro
* @sauterl
* @net-csscience-raphel
* @rahelarnold98
* @faberf

## Citation

See [Citation](https://github.com/vitrivr/vitrivr-engine/wiki/Home#Citation)

## Acknowledgement
This work has received funding from the European Union’s Horizon Europe Research and Innovation Programme under grant agreement No 101070250 [XReco](https://xreco.eu/).
