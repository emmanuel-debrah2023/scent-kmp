package org.scent.project.data.remote.api

/** Wire contract for the query parameters of GET /api/v1/fragrances, shared by client and server. */
object FragranceQueryParams {
    const val QUERY = "query"
    const val CURSOR = "cursor"
    const val LIMIT = "limit"
}
