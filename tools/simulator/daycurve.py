"""Synthetic occupancy curve of a university campus weekday.

Sprint 1 placeholder: report decisions D2/D3 replace it with a replay of the City of Melbourne
on-street sensor data in Sprint 2.
"""

from __future__ import annotations

# (hour of day, share of occupied spaces). Arrivals peak 07:00-10:00 for the first classes,
# the lot stays near full until lunch, then empties through the afternoon with a smaller
# evening group for night classes.
CAMPUS_WEEKDAY: tuple[tuple[float, float], ...] = (
    (0.0, 0.02),
    (6.0, 0.03),
    (7.0, 0.20),
    (8.0, 0.60),
    (9.0, 0.82),
    (10.0, 0.92),
    (12.0, 0.93),
    (13.0, 0.80),
    (14.0, 0.85),
    (16.0, 0.72),
    (18.0, 0.50),
    (19.0, 0.45),
    (20.0, 0.35),
    (21.0, 0.20),
    (22.0, 0.08),
    (24.0, 0.02),
)


def occupancy_target(hour: float, curve: tuple[tuple[float, float], ...] = CAMPUS_WEEKDAY) -> float:
    """Share of occupied spaces (0..1) at ``hour`` (any real number, wrapped into 0..24)."""
    hour = hour % 24.0
    for (start_hour, start_share), (end_hour, end_share) in zip(curve, curve[1:]):
        if start_hour <= hour <= end_hour:
            span = end_hour - start_hour
            weight = 0.0 if span == 0 else (hour - start_hour) / span
            return start_share + (end_share - start_share) * weight
    return curve[-1][1]
