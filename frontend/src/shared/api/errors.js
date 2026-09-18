/** Field level messages come back under error.details.fieldErrors, keyed by path. */
export function fieldMessage(error, field) {
    return error?.fieldErrors?.[field]?.[0] || "";
}

export function formMessage(error) {
    return error?.formErrors?.[0] || error?.message || "";
}

export function hasFieldErrors(error) {
    return Boolean(error?.fieldErrors && Object.keys(error.fieldErrors).length);
}
