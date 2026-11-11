# iART Feature Assessment for the Image Team

Updated: 2026-11-11

This note covers the image-team work. Text retrieval and tagging are being handled by the teammate; the video team's
TIBaVA work is outside this scope.

## Current vitrivr baseline

- **CLIP retrieval:** the FES configuration uses OpenCLIP ViT-B/32 with 512-dimensional descriptors. The same field
  supports text-to-image and image-to-image retrieval.
- **Image query UI:** the local 50-image page has an image-upload query path. A portable page is also available at
  `example-configs/fes/search-demo.html`; it offers CLIP image search and AverageColor image search.
- **Color baseline:** the FES schema and ingestion pipeline already include `AverageColor`, a three-value RGB summary.
  This is useful for a simple comparison, but it is not equivalent to iART's color plug-ins.
- **Clustering:** the project already has a CLIP spherical k-means endpoint. The local 50-image demo can browse and
  filter cluster assignments. A two-dimensional UMAP canvas is not implemented in the tracked UI.
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
| BYOL art features | Not enabled in the current team pipeline | Obtain the exact art-trained checkpoint and preprocessing; implement a separate image-only extractor and reindex |
| Painter style/genre features | Not enabled in the current team pipeline | Obtain the model and labels/checkpoint; add a separate feature/classification path and reindex |
| Iconclass/iMet/Painter classifiers | Not equivalent to the current OpenCLIP top-label setup; tagging belongs to the teammate | Coordinate with the teammate and compare the original label sets and model outputs |
| Clustering and UMAP | CLIP k-means exists; UMAP/canvas view is missing | Add a 2D projection and interactive view after the embedding choices are settled |

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
- The official [vitrivr-web README](https://github.com/vitrivr/vitrivr-web/blob/main/README.md) describes a React UI with
  text and image query components and configurable engine URLs. It also says image-to-image search needs adaptation
  and that DRES integration should be removed for a non-competition setup. It is a reasonable later candidate, not a
  drop-in replacement today.
- The older [vitrivr-ng UI](https://github.com/vitrivr/vitrivr-ng) is built for Cineast, so it is not the right direct
  integration target for this engine.

## Recommended order

1. Complete M1 by running the tracked UI against the FES-backed schema with the team's image collection and confirming
   both CLIP query modes.
2. Keep AverageColor as a comparison baseline; do not call it the original iART color model.
3. For M I2/M I3, request or locate the exact iART checkpoints, preprocessing details, label definitions, and licenses.
   Then select one image-only model for a separate-field proof of concept.
4. Add UMAP and richer cluster exploration in M I4 after the desired embedding space is chosen.
5. Leave custom models and retrieval experiments for M I5.
