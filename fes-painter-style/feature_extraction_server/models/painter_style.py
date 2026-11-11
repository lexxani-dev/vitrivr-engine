"""Expose the iART Painter-by-Numbers style embedding through FES."""

import os

import numpy as np
import torch

from feature_extraction_server.core.model import Model


class PainterStyle(Model):
    """Return the 256-dimensional style embedding from the iART model."""

    def _load_model(self):
        model_path = os.environ.get(
            "PAINTER_MODEL_PATH", "/models/kaggle_embedding/kaggle_embedding.pt"
        )
        if not os.path.isfile(model_path):
            raise FileNotFoundError(
                f"iART Painter checkpoint not found at {model_path}. "
                "Mount kaggle_embedding.pt and set PAINTER_MODEL_PATH."
            )

        self.model = torch.jit.load(model_path, map_location="cpu")
        self.model.eval()

    def image_embedding(self, image, config=None):
        """Return a style descriptor for an RGB image."""
        pillow_image = image.to_pillow().convert("RGB")
        width, height = pillow_image.size
        short_side = min(width, height)
        if short_side < 244:
            scale = 244 / short_side
            pillow_image = pillow_image.resize((int(width * scale), int(height * scale)))

        pixels = np.asarray(pillow_image, dtype=np.uint8).copy()
        with torch.inference_mode():
            style, _ = self.model(torch.from_numpy(pixels))

        vector = style.reshape(-1).to(dtype=torch.float32)
        if vector.numel() != 256:
            raise ValueError(
                f"iART Painter style head returned {vector.numel()} values; expected 256."
            )
        return {"embedding": vector.cpu().tolist()}
