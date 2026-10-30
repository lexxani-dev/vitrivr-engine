# CLIP Image Search and Clustering Demo

## Project status

This prototype uses CLIP image descriptors stored by vitrivr-engine to support text-to-image search and image clustering. The demo collection contains 50 images. Search results are ranked by the existing retrieval API; clustering groups all images indexed in the selected schema.

The clustering endpoint and the engine changes are in the `codex/clip-clustering` branch, based on `team/image`.

## What is implemented

- **CLIP clustering:** `CLIPKMeansClusterer` applies spherical k-means to normalized CLIP vectors and compares them with cosine similarity.
- **REST endpoint:** `POST /api/{schema}/clusters` returns a cluster assignment for each image in the schema.
- **Configurable cluster count:** callers can provide `k`; the default is 6.
- **Headless server startup:** `--headless` starts the REST server without opening the interactive CLI.
- **Search demo:** the local demo page sends text searches and cluster requests to the engine. Its page and CSV metadata are local demo files and are not included in this repository branch.

Cluster IDs are numeric and zero-based. Each assignment includes the image's `retrievableId` and its cosine distance to the assigned centroid. The algorithm does not use directory names or labels to form clusters, and a cluster number does not itself represent a human-readable category.

## Running the demo

Start PostgreSQL and the CLIP descriptor service, and make sure the schema has indexed images with a `clip` descriptor field. From the repository root, start the server in PowerShell:

```powershell
.\gradlew.bat :vitrivr-engine-server:run --args="<path-to-config-schema.json> --headless"
```

The default REST API port is `7070`. For the current 50-image demo configuration, the schema name is `clusteringdemo`.

To request six clusters:

```powershell
$body = '{"k":6}'
Invoke-RestMethod -Method Post `
  -Uri 'http://127.0.0.1:7070/api/clusteringdemo/clusters' `
  -ContentType 'application/json' `
  -Body $body
```

The request body is optional. Omitting it uses `k=6`. To display a result image, use its `retrievableId` with the thumbnail route:

```text
GET /api/{schema}/fetch/thumbnail/{retrievableId}
```

The endpoint clusters every indexed CLIP descriptor in the schema; it does not cluster only the current text-search results.

## API response

Example request:

```json
{"k": 6}
```

Example response shape:

```json
{
  "requestedK": 6,
  "clusterCount": 6,
  "imageCount": 50,
  "assignments": [
    {
      "retrievableId": "...",
      "clusterId": 0,
      "cosineDistance": 0.12
    }
  ]
}
```

`clusterCount` reports the number of non-empty clusters. `cosineDistance` is a distance, so smaller values mean an image is closer to its assigned centroid.

## Initial evaluation

These measurements were collected on the 50-image demo set. Silhouette uses cosine distance. Folder NMI compares the resulting clusters with the source-folder labels only after clustering; those labels were not inputs to the algorithm.

| `k` | Cosine silhouette | Folder NMI |
| ---: | ---: | ---: |
| 4 | 0.169 | 0.222 |
| 6 | 0.157 | 0.419 |
| 8 | 0.166 | 0.439 |
| 10 | 0.159 | 0.473 |

The silhouette scores are low, which suggests the images do not form sharply separated groups in this small collection. `k=6` is a convenient initial demo setting because the source data has six folders; it is not a proven optimum. `k=8` scores slightly higher on both measures. Try several values on a larger, representative collection before selecting a production setting.

## Implementation locations

- Algorithm: `vitrivr-engine-module-features/src/main/kotlin/org/vitrivr/engine/module/features/feature/external/implementations/clip/CLIPKMeansClusterer.kt`
- REST handler: `vitrivr-engine-server/src/main/kotlin/org/vitrivr/engine/server/api/rest/handlers/Clustering.kt`
- REST route registration: `vitrivr-engine-server/src/main/kotlin/org/vitrivr/engine/server/api/rest/Routes.kt`
- Headless startup option: `vitrivr-engine-server/src/main/kotlin/org/vitrivr/engine/server/Main.kt`

## Current limitations and next steps

- The endpoint loads all CLIP descriptors and recomputes clusters for every request. This is acceptable for the 50-image demo, but larger datasets should cache or persist assignments and define when they become stale.
- Results require indexed CLIP descriptors. Empty schemas, invalid `k`, or a missing CLIP field return API errors.
- The current output has numeric cluster IDs, not generated names or semantic descriptions.
- The demo quality should be evaluated with more varied images and human review. Source-folder agreement alone does not prove that clusters are useful to users.
- Before merging with the team's API work, compare the route and startup changes with the teammate's branch to resolve any overlap.
