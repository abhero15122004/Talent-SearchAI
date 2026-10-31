"""
TalentMatch AI — Ranking and Extraction Metrics Engine

Provides mathematically rigorous, reproducible metrics for candidate ranking
(NDCG@K, Precision@K, Recall@K, MRR) and entity extraction (Precision, Recall, F1).
Explicitly handles edge cases (empty pools, zero relevant candidates, ties).
"""

from typing import Dict, List, Optional, Sequence, Tuple
import math
import numpy as np


def ndcg_at_k(y_true: Sequence[float], y_pred: Sequence[float], k: int) -> float:
    """
    Computes Normalized Discounted Cumulative Gain at rank K.
    Uses exponential gain (2^rel - 1) to emphasize placing highly relevant items at the top.

    Args:
        y_true: Ground truth human relevance grades (e.g. 0, 1, 2, 3)
        y_pred: Predicted scores/probabilities from the ranking model
        k: Cutoff rank (e.g. 1, 3, 5, 10)

    Returns:
        Float in [0.0, 1.0] representing ranking quality at K.
    """
    if not y_true or not y_pred or len(y_true) != len(y_pred) or k <= 0:
        return 0.0

    k = min(k, len(y_true))

    # Sort ground truth relevance according to model's predicted ranking (descending)
    # Tie-breaking is deterministic (stable sort by index)
    ranked_indices = sorted(range(len(y_pred)), key=lambda i: y_pred[i], reverse=True)
    actual_relevance_at_k = [y_true[i] for i in ranked_indices[:k]]

    # Compute DCG@K
    dcg = sum((math.pow(2, rel) - 1.0) / math.log2(rank + 2) for rank, rel in enumerate(actual_relevance_at_k))

    # Compute Ideal DCG@K (sorted by true relevance descending)
    ideal_relevance_at_k = sorted(y_true, reverse=True)[:k]
    idcg = sum((math.pow(2, rel) - 1.0) / math.log2(rank + 2) for rank, rel in enumerate(ideal_relevance_at_k))

    # Degenerate case handling:
    # If all items in candidate pool have 0 ground truth relevance, any ordering is equally uninformative
    if idcg <= 0.0:
        return 1.0 if dcg <= 0.0 else 0.0

    return round(dcg / idcg, 4)


def precision_at_k(y_true: Sequence[float], y_pred: Sequence[float], k: int, threshold: float = 2.0) -> float:
    """
    Calculates Precision@K: fraction of top-K retrieved candidates that are relevant (>= threshold).

    Args:
        y_true: Ground truth relevance grades
        y_pred: Predicted model ranking scores
        k: Cutoff rank
        threshold: Minimum relevance grade to be considered a positive match (default 2.0: Relevant or Highly Relevant)
    """
    if not y_true or not y_pred or len(y_true) != len(y_pred) or k <= 0:
        return 0.0

    k = min(k, len(y_true))
    ranked_indices = sorted(range(len(y_pred)), key=lambda i: y_pred[i], reverse=True)
    top_k_labels = [y_true[i] for i in ranked_indices[:k]]

    relevant_count = sum(1 for label in top_k_labels if label >= threshold)
    return round(relevant_count / k, 4)


def recall_at_k(y_true: Sequence[float], y_pred: Sequence[float], k: int, threshold: float = 2.0) -> float:
    """
    Calculates Recall@K: fraction of all relevant candidates in the pool that appear in the top-K.

    Args:
        y_true: Ground truth relevance grades
        y_pred: Predicted model ranking scores
        k: Cutoff rank
        threshold: Minimum grade considered relevant
    """
    if not y_true or not y_pred or len(y_true) != len(y_pred) or k <= 0:
        return 0.0

    total_relevant = sum(1 for label in y_true if label >= threshold)
    if total_relevant == 0:
        # Edge case: If there are no relevant candidates in the pool, recall is trivially satisfied
        return 1.0

    k = min(k, len(y_true))
    ranked_indices = sorted(range(len(y_pred)), key=lambda i: y_pred[i], reverse=True)
    top_k_labels = [y_true[i] for i in ranked_indices[:k]]

    relevant_in_top_k = sum(1 for label in top_k_labels if label >= threshold)
    return round(relevant_in_top_k / total_relevant, 4)


def reciprocal_rank(y_true: Sequence[float], y_pred: Sequence[float], threshold: float = 2.0) -> float:
    """
    Calculates Reciprocal Rank (RR): 1 / rank of the first relevant candidate.
    Returns 0.0 if no relevant candidates are found.
    """
    if not y_true or not y_pred or len(y_true) != len(y_pred):
        return 0.0

    ranked_indices = sorted(range(len(y_pred)), key=lambda i: y_pred[i], reverse=True)
    for rank_idx, cand_idx in enumerate(ranked_indices):
        if y_true[cand_idx] >= threshold:
            return round(1.0 / (rank_idx + 1), 4)

    return 0.0


def calculate_query_metrics(
    y_true: Sequence[float], y_pred: Sequence[float], k_values: Sequence[int] = (1, 3, 5), threshold: float = 2.0
) -> Dict[str, float]:
    """
    Computes all standard ranking metrics for a single job requisition query.
    """
    metrics: Dict[str, float] = {}
    for k in k_values:
        metrics[f"NDCG@{k}"] = ndcg_at_k(y_true, y_pred, k)
        metrics[f"P@{k}"] = precision_at_k(y_true, y_pred, k, threshold)
        metrics[f"R@{k}"] = recall_at_k(y_true, y_pred, k, threshold)

    metrics["RR"] = reciprocal_rank(y_true, y_pred, threshold)
    return metrics


def aggregate_ranking_metrics(query_metrics_list: List[Dict[str, float]]) -> Dict[str, float]:
    """
    Aggregates metrics across multiple job queries by computing arithmetic mean.
    Includes MRR (Mean Reciprocal Rank).
    """
    if not query_metrics_list:
        return {}

    keys = query_metrics_list[0].keys()
    aggregated: Dict[str, float] = {}

    for key in keys:
        values = [m[key] for m in query_metrics_list if key in m]
        mean_val = float(np.mean(values)) if values else 0.0
        if key == "RR":
            aggregated["MRR"] = round(mean_val, 4)
        else:
            aggregated[key] = round(mean_val, 4)

    return aggregated


def skill_extraction_metrics(
    extracted_skills: Sequence[str], ground_truth_skills: Sequence[str]
) -> Dict[str, float]:
    """
    Computes set-based Precision, Recall, and F1 score for skill extraction on a single candidate resume.
    """
    pred_set = {s.strip().lower() for s in extracted_skills if s and s.strip()}
    gt_set = {s.strip().lower() for s in ground_truth_skills if s and s.strip()}

    if not pred_set and not gt_set:
        return {"precision": 1.0, "recall": 1.0, "f1": 1.0}
    if not pred_set or not gt_set:
        return {"precision": 0.0, "recall": 0.0, "f1": 0.0}

    true_positives = len(pred_set.intersection(gt_set))
    precision = true_positives / len(pred_set) if pred_set else 0.0
    recall = true_positives / len(gt_set) if gt_set else 0.0

    f1 = (2.0 * precision * recall / (precision + recall)) if (precision + recall) > 0.0 else 0.0

    return {
        "precision": round(precision, 4),
        "recall": round(recall, 4),
        "f1": round(f1, 4),
    }


def aggregate_extraction_metrics(sample_metrics: List[Dict[str, float]]) -> Dict[str, float]:
    """
    Averages extraction metrics across all labeled resume samples.
    """
    if not sample_metrics:
        return {"precision": 0.0, "recall": 0.0, "f1": 0.0}

    return {
        "precision": round(float(np.mean([m["precision"] for m in sample_metrics])), 4),
        "recall": round(float(np.mean([m["recall"] for m in sample_metrics])), 4),
        "f1": round(float(np.mean([m["f1"] for m in sample_metrics])), 4),
    }
