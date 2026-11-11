# iART Feature Assessment for the Image Team

Updated: 2026-11-11

This note covers the image-team work. Text retrieval and tagging are being handled by the teammate; the video team's
TIBaVA work is outside this scope.

## Current vitrivr baseline

- **CLIP retrieval:** the FES configuration uses OpenCLIP ViT-B/32 with 512-dimensional descriptors. The same field
  supports text-to-image and image-to-image retrieval.
- **Image query UI:** the local 50-image page has an image-upload query path. A portable page is also available at
  `example-configs/fes/search-demo.html`; it offers CLIP, iART BYOL, and AverageColor image search.
- **Color baseline:** the FES schema and ingestion pipeline already include `AverageColor`, a three-value RGB summary.
  This is useful for a simple comparison, but it is not equivalent to iART's color plug-ins.
- **Clustering:** the project has spherical k-means for dense vector fields, with CLIP as the default and AverageColor
  available as a second space in the portable demo. The demo now has a two-dimensional UMAP map of up to 500 vectors.
- **Classification/tagging:** a basic OpenCLIP classification field exists in the FES setup. The teammate owns the
  tagging work, so this assessment does not propose duplicating it.

## iART capabilities and transfer path

The iART paper describes ImageNet ResNet features, Wikimedia-trained BYOL features, Painter-by-Numbers features for
style and genre, CLIP, art-specific classifiers, color criteria, plug-in weighting, k-means clustering, and UMAP result
visualization. It supports image-query retrieval across feature spaces; CLIP additionally maps text and images into a
shared space. See the [iART paper](https://arxiv.org/abs/2108.01542) and the [official iART repository](https://github.com/TIBHannover/iart).

| iART capability | Current status here | What transfer would require |
| --- | --- | --- |
| CLIP text and image search | Available with OpenCLIP ViT-B/32; image upload is now represented in both demo UIs | Keep this as the baseline and compare results on art images |
| Color similarity | AverageColor is available as a basic baseline | Compare it with iART's actual color extractor before claiming equivalence |
| ImageNet ResNet features | Not enabled in the current team pipeline | Confirm the iART model/checkpoint, preprocessing, and output dimension; store it in its own field |
| BYOL art features | Implemented as a separate 256-dimensional image-only field using iART's Wikipedia-trained checkpoint | Supply the local checkpoint, run the BYOL FES service, and reindex the collection; see `BYOL_IMAGE_SEARCH.md` |
| Painter style/genre features | Not enabled in the current team pipeline | Obtain the model and labels/checkpoint; add a separate feature/classification path and reindex |
| Iconclass/iMet/Painter classifiers | Not equivalent to the current OpenCLIP top-label setup; tagging belongs to the teammate | Coordinate with the teammate and compare the original label sets and model outputs |
| Clustering and UMAP | Spherical k-means works on selected dense-vector fields; the portable demo projects up to 500 vectors with UMAP | Evaluate the map on a larger art collection and compare feature spaces |

## Integration constraints

The feature vectors from ResNet, BYOL, Painter, and CLIP cannot safely share one database field unless their model,
preprocessing, and vector space are identical. Each distinct extractor needs a separate field and a fresh indexing pass
for the collection. Image-only models such as BYOL or Painter can support query-by-image, but they do not provide
text-to-image search unless a matching text encoder exists.

The public iART repository is a complete application with separate backend, indexer, and frontend components plus
model downloads and index-building steps. Its paper describes a plug-in architecture; it does not define a ready-made
embedding endpoint that can simply replace our FES service. The low-risk path is to integrate one extractor at a time
behind the existing vitrivr field/query interfaces, while keeping CLIP intact.

## UI comparison

- The current small demo is the quickest M1 path and now includes reference-image search.
- The official [vitrivr-web README](https://github.com/vitrivr/vitrivr-web/blob/main/README.md) describes a React UI
  with a configurable engine URL and schema selector. The query and schema-list routes match vitrivr-engine.
- The current [vitrivr-web query UI](https://github.com/vitrivr/vitrivr-web/blob/main/vitrivr-web/src/components/QueryBuilderComponents/QueryBlock.tsx)
  has its image-query choice commented out. A dormant image branch exists in `SearchCard`, but it currently builds a
  `TEXT` input, so it must be changed to submit an `IMAGE` data URL for this project's CLIP endpoint. The result
  thumbnails also expect a separate static media server; this project can instead use the engine's
  `/api/{schema}/fetch/thumbnail/{retrievableId}` endpoint. DRES should be removed for a standalone project UI.
  Thus, vitrivr-web is a promising later UI base, but its image workflow and thumbnail loading need adaptation before
  it can replace the current page.
- The older [vitrivr-ng UI](https://github.com/vitrivr/vitrivr-ng) is built for Cineast, so it is not the right direct
  integration target for this engine.

## Recommended order

1. Complete M1 by running the tracked UI against the FES-backed schema with the team's image collection and confirming
   both CLIP query modes.
2. Keep AverageColor as a comparison baseline; do not call it the original iART color model.
3. For M I2/M I3, evaluate BYOL image matches on an art collection and confirm model-specific checkpoint terms. Then
   compare the other iART extractors and classifier label sets before integrating them.
4. Evaluate the new UMAP map and field-selectable clustering in M I4 on a larger art collection.
5. Leave custom models and retrieval experiments for M I5.
