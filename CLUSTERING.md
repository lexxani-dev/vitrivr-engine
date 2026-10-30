# CLIP Image Search and Clustering Prototype

## Project status

The 50-image demo milestone is implemented and ready for team review. The `codex/clip-clustering` branch, based on `team/image`, adds a CLIP clustering algorithm, a REST endpoint, headless server startup, and a fix for fetching stored JPEG thumbnails.

This is a prototype result, not a production quality claim. The demo collection is small and its clusters are not sharply separated. The next meaningful project step is to review this branch with the teammate working on the API, then evaluate it on a larger, representative image collection.

## Implemented

- **Clustering:** `CLIPKMeansClusterer` applies spherical k-means to normalized CLIP image descriptors and compares vectors with cosine similarity.
- **REST API:** `POST /api/{schema}/clusters` clusters all CLIP descriptors in the schema. The request can set `k`; it defaults to 6.
- **Headless startup:** `--headless` starts the REST server without the interactive CLI.
- **Thumbnail retrieval:** `GET /api/{schema}/fetch/{exporter}/{retrievableId}` fetches a stored JPEG. For the demo, the exporter is `thumbnail`; the handler resolves files with a `.jpg` suffix.

Cluster IDs are zero-based in the API. Each assignment contains the image's `retrievableId`, its `clusterId`, and its cosine distance to the assigned centroid. The algorithm does not use source folders or labels to form clusters; cluster numbers do not have human-readable meanings.

## Running the server

Start PostgreSQL and the CLIP descriptor service. Ensure the schema configuration defines a `clip` descriptor field and, for thumbnails, a `thumbnail` exporter with a resolver that points to the stored image files. From the repository root, start the server in PowerShell:

```powershell
.\gradlew.bat :vitrivr-engine-server:run --args="<path-to-config-schema.json> --headless"
```

The default REST API port is `7070`. The 50-image demo schema is named `clusteringdemo`.

## Clustering API

Request six clusters:

```powershell
$body = '{"k":6}'
Invoke-RestMethod -Method Post `
  -Uri 'http://127.0.0.1:7070/api/clusteringdemo/clusters' `
  -ContentType 'application/json' `
  -Body $body
```

The request body is optional; omitting it uses `k=6`.

Example response:

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

`clusterCount` reports the number of non-empty clusters. Smaller `cosineDistance` values mean an image is closer to its assigned centroid. Use an assignment's `retrievableId` with the configured thumbnail exporter to display the image.

The endpoint clusters every indexed CLIP descriptor in the schema. It does not cluster only the current text-search results.

## Initial evaluation

These measurements were collected on the 50-image demo set. Silhouette uses cosine distance. Folder NMI compares the resulting clusters with source-folder labels after clustering; those labels were not algorithm inputs.

| `k` | Cosine silhouette | Folder NMI |
| ---: | ---: | ---: |
| 4 | 0.169 | 0.222 |
| 6 | 0.157 | 0.419 |
| 8 | 0.166 | 0.439 |
| 10 | 0.159 | 0.473 |

The silhouette scores are low, so this collection does not form sharply separated groups. `k=6` is a convenient initial demo value because the source data has six folders; it is not a proven optimum. `k=8` is close and scores slightly higher on both measures. Compare several values on a larger, representative collection before choosing a production setting.

## Verification performed

- The server module compiled and the API started on port `7070`.
- Requests with `k=4`, `6`, `8`, and `10` each returned the expected cluster count and all 50 image assignments.
- An empty request body returned the default `k=6`; the generated OpenAPI document marks the request body optional and describes `ClusteringRequest`.
- All 50 thumbnail requests returned `image/jpeg` data that decoded as valid images.
- The demo page and its JavaScript loaded successfully from the local page server.

## Local learning page

The interactive learning page, its assignment CSV, color-coverage metadata, and demo thumbnails are local files outside this branch. The page can browse all 50 assignments and includes demo-only relevance and color filters. Those browser filters depend on this specific local dataset; they are not part of the server API or the tracked project UI.

## Implementation locations

- Algorithm: `vitrivr-engine-module-features/src/main/kotlin/org/vitrivr/engine/module/features/feature/external/implementations/clip/CLIPKMeansClusterer.kt`
- Clustering REST handler: `vitrivr-engine-server/src/main/kotlin/org/vitrivr/engine/server/api/rest/handlers/Clustering.kt`
- Thumbnail REST handler: `vitrivr-engine-server/src/main/kotlin/org/vitrivr/engine/server/api/rest/handlers/FetchExportData.kt`
- Route registration: `vitrivr-engine-server/src/main/kotlin/org/vitrivr/engine/server/api/rest/Routes.kt`
- Headless startup option: `vitrivr-engine-server/src/main/kotlin/org/vitrivr/engine/server/Main.kt`

## Limitations and next steps

- The endpoint loads all descriptors and recomputes clusters on every request. For larger collections, cache or persist assignments and define when they become stale.
- Clustering requires indexed CLIP descriptors. Empty schemas, invalid `k`, or a missing CLIP field return API errors.
- Cluster IDs are numeric and do not include generated names or semantic descriptions.
- Source-folder agreement alone does not prove that the groups are useful to users; review representative results manually.
- Before merging with the team's API work, compare route and startup changes with the teammate's branch to resolve overlap.
