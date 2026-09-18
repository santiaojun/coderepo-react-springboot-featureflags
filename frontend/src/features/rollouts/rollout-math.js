export const ROLLOUT_STEPS = [5, 10, 25, 50, 75, 100];

export function weightsTotal(weights = []) {
    return weights.reduce((total, weight) => total + Number(weight.weight || 0), 0);
}

/**
 * Builds a rollout that gives {@code percent} to the treatment variation and the
 * remainder to the control, leaving every other variation at zero.
 */
export function buildRollout(variations, treatmentId, percent, controlId) {
    const clamped = Math.max(0, Math.min(100, Math.round(percent)));
    return {
        bucketBy: "key",
        weights: variations.map((variation) => {
            if (variation.id === treatmentId) return { variationId: variation.id, weight: clamped };
            if (variation.id === controlId) return { variationId: variation.id, weight: 100 - clamped };
            return { variationId: variation.id, weight: 0 };
        }),
    };
}

export function weightFor(weights, variationId) {
    return Number(weights?.find((weight) => weight.variationId === variationId)?.weight || 0);
}

/** The variation carrying the largest share, used to label the current step. */
export function treatmentOf(weights = []) {
    return weights.reduce((best, weight) => (weight.weight > (best?.weight || 0) ? weight : best), null);
}

export function evenSplit(variations) {
    const share = Math.floor(100 / variations.length);
    const remainder = 100 - share * variations.length;
    return {
        bucketBy: "key",
        weights: variations.map((variation, index) => ({
            variationId: variation.id,
            weight: share + (index === 0 ? remainder : 0),
        })),
    };
}
