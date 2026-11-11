# iART Painter image search

The FES demo exposes the iART Painter-by-Numbers model as two independent image-to-image search fields:

- `painterstyle`: 256-dimensional style embeddings.
- `paintergenre`: 256-dimensional genre embeddings.

The style and genre heads share the original iART TorchScript checkpoint. Both are image-only; text search continues
to use CLIP. Each field has its own vector space and must be indexed separately.

## Supply the checkpoint

The checkpoint is not committed to Git or copied into the Docker image. Obtain the CPU model archive by following the
official [iART repository setup](https://github.com/TIBHannover/iart), then extract only the Painter embedding model
from the archive into the expected local path:

```powershell
New-Item -ItemType Directory -Force .\sandbox\models\kaggle_embedding | Out-Null
tar -xzf "$env:USERPROFILE\Downloads\models_cpu.tar.gz" -C .\sandbox models/kaggle_embedding/kaggle_embedding.pt
```

The resulting file must be `sandbox/models/kaggle_embedding/kaggle_embedding.pt`.

## Run and index

Build or restart the FES Compose stack so the `fes-byol` service also loads the Painter models:

```powershell
docker compose -p vitrivr-byol -f docker-compose.fes.yml up --build -d
```

Initialize the `sandbox` schema after the new `painterstyle` and `paintergenre` fields are present, then run the image
ingestion pipeline again. Existing records do not have these descriptors until they are reindexed. The ingestion flow
keeps CLIP, BYOL, and both Painter heads in one sequential image pipeline.

Open the search page against the FES-backed engine, for example:
`http://localhost:7072/search-demo.html?api=http://127.0.0.1:7074/api/sandbox`.
Choose **iART Painter style** or **iART Painter genre** for image upload search, or select the corresponding feature
under **Cluster vectors** to explore that vector space.
