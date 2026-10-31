"""
TalentMatch AI - Sentence Transformers Embedding Service
Model: all-MiniLM-L6-v2 (384 dimensions)
"""

import os
# Ensure PyTorch is used cleanly without Keras 3 conflicts
os.environ["USE_TF"] = "0"
os.environ["TF_ENABLE_ONEDNN_OPTS"] = "0"
os.environ["HF_HUB_OFFLINE"] = "1"
os.environ["TRANSFORMERS_OFFLINE"] = "1"

from typing import List, Union
import numpy as np
from sentence_transformers import SentenceTransformer

from app.config import settings


class Embedder:
    """
    Singleton embeddings generator utilizing all-MiniLM-L6-v2.
    Yields 384-dimensional unit-normalized dense vectors for cosine similarity computation.
    """

    def __init__(self, model_name: str = settings.MODEL_NAME):
        self.model_name = model_name
        self._model = None

    @property
    def model(self) -> SentenceTransformer:
        if self._model is None:
            try:
                self._model = SentenceTransformer(self.model_name, local_files_only=True)
            except Exception:
                self._model = SentenceTransformer(self.model_name)
        return self._model

    def encode(self, texts: Union[str, List[str]], normalize: bool = True) -> np.ndarray:
        """
        Encodes single or batch texts into normalized embeddings.
        Returns NumPy array of shape (N, 384) or (384,).
        """
        if isinstance(texts, str):
            single = True
            texts = [texts]
        else:
            single = False

        embeddings = self.model.encode(
            texts,
            normalize_embeddings=normalize,
            show_progress_bar=False,
            convert_to_numpy=True,
        )

        return embeddings[0] if single else embeddings

    @staticmethod
    def cosine_similarity(v1: np.ndarray, v2: np.ndarray) -> float:
        """
        Computes cosine similarity between two 1D vectors.
        Assumes normalized vectors or calculates dot product / (norm * norm).
        """
        dot = np.dot(v1, v2)
        norm1 = np.linalg.norm(v1)
        norm2 = np.linalg.norm(v2)
        if norm1 == 0.0 or norm2 == 0.0:
            return 0.0
        sim = float(dot / (norm1 * norm2))
        return max(0.0, min(1.0, sim))


# Global singleton instance
embedder = Embedder()
