package com.aion.brain.providers

import com.aion.brain.ProviderFailure

/** Shared HTTP-status → [ProviderFailure] mapping (DOC-013 §2 failure classes), used by every adapter. */
fun mapHttpError(
    status: Int,
    body: String,
): ProviderFailure =
    when (status) {
        401, 403 -> ProviderFailure.Auth(body.ifBlank { "HTTP $status" })
        402, 429 ->
            if (body.contains("quota", ignoreCase = true)) {
                ProviderFailure.Quota(body)
            } else {
                ProviderFailure.RateLimit(body)
            }
        408 -> ProviderFailure.Timeout(body.ifBlank { "HTTP $status" })
        in 500..599 -> ProviderFailure.Server(body.ifBlank { "HTTP $status" })
        else -> ProviderFailure.BadOutput("HTTP $status: $body")
    }
