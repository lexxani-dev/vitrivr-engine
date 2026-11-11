"""Image embeddings from the locally supplied iART BYOL Wikipedia checkpoint."""

import os

import numpy as np
import torch

from feature_extraction_server.core.model import Model


class ByolWikipedia(Model):
    """Expose the iART BYOL image encoder through FES's image_embedding task."""

    def _load_model(self):
        model_path = os.environ.get(
            "BYOL_MODEL_PATH", "/models/byol_embedding/byol_wikipedia.pt"
        )
        if not os.path.isfile(model_path):
            raise FileNotFoundError(
                f"BYOL checkpoint not found at {model_path}. "
                "Mount the iART checkpoint and set BYOL_MODEL_PATH."
            )

        self.model = torch.jit.load(model_path, map_location="cpu")
        self.model.eval()

    def image_embedding(self, image, config=None):
        """Return one 256-dimensional descriptor for an RGB image."""
        pillow_image = image.to_pillow().convert("RGB")
        width, height = pillow_image.size
        short_side = min(width, height)
        if short_side < 244:
            scale = 244 / short_side
            new_size = (int(width * scale), int(height * scale))
            pillow_image = pillow_image.resize(new_size)

        pixels = np.asarray(pillow_image, dtype=np.uint8).copy()
        with torch.inference_mode():
            output = self.model(torch.from_numpy(pixels))

        vector = output.reshape(-1).to(dtype=torch.float32)
        if vector.numel() != 256:
            raise ValueError(
                f"BYOL returned {vector.numel()} values; expected 256."
            )
        return {"embedding": vector.cpu().tolist()}
