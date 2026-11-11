# iART BYOL image search

The image demo now has an optional iART BYOL image-to-image search path. BYOL is a separate 256-dimensional image
descriptor field; it does not replace CLIP or provide text search. Text queries continue to use the existing CLIP field.

## Model file

The BYOL checkpoint is intentionally not stored in Git or in the Docker image. Download the official
[iART model archive](https://tib.eu/cloud/s/idRNbs6EJ5LKea6/download/models_cpu.tar.gz) referenced by the
[iART installer](https://github.com/TIBHannover/iart/blob/master/install.sh), then place only
`models/byol_embedding/byol_wikipedia.pt` at:

```text
sandbox/models/byol_embedding/byol_wikipedia.pt
```

The archive is about 2 GB and contains other checkpoints. The compose setup only mounts the BYOL folder read-only.
The iART source repository is GPL-3.0; the checkpoint does not have a separately stated license in the inspected
configuration. Do not redistribute the checkpoint until its model-specific terms are confirmed.

On Windows, from the repository root, extract just the required checkpoint from the downloaded archive:

```powershell
New-Item -ItemType Directory -Force .\sandbox | Out-Null
tar -xzf "$env:USERPROFILE\Downloads\models_cpu.tar.gz" -C .\sandbox models/byol_embedding/byol_wikipedia.pt
```

## Run the isolated demo stack

From the repository root, start the FES-backed demo stack using `docker-compose.fes.yml`:

```powershell
$env:ENGINE_PORT = "7074"
$env:FES_MODEL_CACHE = "C:/path/to/sandbox/model-cache" # Optional: reuse a downloaded OpenCLIP cache.
docker compose -p vitrivr-byol -f docker-compose.fes.yml up --build -d
```

This starts an isolated stack on Engine port 7074 (or the selected `ENGINE_PORT`), CLIP FES port 8889, BYOL FES port
8890, and database port 5434, leaving the existing 7070 stack untouched. The BYOL FES service requires the model file
above. Once the stack is ready, initialize the `sandbox` schema, ingest the image collection again so the new `byol`
descriptors are stored, and open `example-configs/fes/search-demo.html` with the API set to
`http://127.0.0.1:7074/api/sandbox`. Choose **iART BYOL (image only)** to search by an uploaded image, or choose
**iART BYOL** under Cluster vectors to cluster/project that feature space.

CLIP remains the text-search feature. AverageColor remains available as a simple baseline. A fresh ingestion is
required because existing database entries do not contain BYOL descriptors.

## Implementation notes

- The checkpoint is loaded with TorchScript on CPU and produces 256 values per image.
- Preprocessing follows iART's minimum short-side resize of 244 pixels; no text encoder is part of this model.
- The FES adapter is local integration code. It does not copy the iART indexer implementation.
- The default model path can be changed with `BYOL_MODEL_PATH`.
