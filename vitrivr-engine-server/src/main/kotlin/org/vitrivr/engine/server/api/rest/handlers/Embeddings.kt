package org.vitrivr.engine.server.api.rest.handlers

import io.javalin.http.Context
import io.javalin.http.bodyAsClass
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiRequestBody
import io.javalin.openapi.OpenApiResponse
import kotlinx.serialization.Serializable
import org.vitrivr.engine.core.model.descriptor.vector.FloatVectorDescriptor
import org.vitrivr.engine.core.model.metamodel.Schema
import org.vitrivr.engine.server.api.rest.model.ErrorStatus
import org.vitrivr.engine.server.api.rest.model.ErrorStatusException

private const val MAX_EMBEDDING_POINTS = 1000

@Serializable
data class EmbeddingsRequest(
    val field: String? = null,
    val limit: Int = 500
)

@Serializable
data class EmbeddingPointResponse(
    val retrievableId: String,
    val vector: List<Float>
)

@Serializable
data class EmbeddingsResponse(
    val field: String,
    val returnedCount: Int,
    val truncated: Boolean,
    val embeddings: List<EmbeddingPointResponse>
)

@OpenApi(
    path = "/api/{schema}/embeddings",
    methods = [HttpMethod.POST],
    summary = "Returns a bounded sample of dense vectors for visualization.",
    operationId = "postListEmbeddings",
    tags = ["Retrieval"],
    pathParams = [
        OpenApiParam("schema", type = String::class, description = "The schema to read embeddings from.", required = true)
    ],
    requestBody = OpenApiRequestBody([OpenApiContent(EmbeddingsRequest::class)], required = false),
    responses = [
        OpenApiResponse("200", [OpenApiContent(EmbeddingsResponse::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun listEmbeddings(ctx: Context, schema: Schema) {
    val request = try {
        if (ctx.body().isBlank()) EmbeddingsRequest() else ctx.bodyAsClass<EmbeddingsRequest>()
    } catch (e: Exception) {
        throw ErrorStatusException(400, "Invalid embeddings request: ${e.message}")
    }

    if (request.limit !in 2..MAX_EMBEDDING_POINTS) {
        throw ErrorStatusException(400, "limit must be between 2 and $MAX_EMBEDDING_POINTS.")
    }

    val fieldName = request.field?.trim()?.takeIf { it.isNotEmpty() } ?: "clip"
    val vectorField = schema[fieldName]
        ?: throw ErrorStatusException(404, "Schema '${schema.name}' has no '$fieldName' field.")
    val sampledDescriptors = vectorField.getReader().getAll()
        .filterIsInstance<FloatVectorDescriptor>()
        .take(request.limit + 1)
        .toList()

    if (sampledDescriptors.isEmpty()) {
        throw ErrorStatusException(400, "Schema '${schema.name}' contains no vector descriptors in field '$fieldName'.")
    }

    val truncated = sampledDescriptors.size > request.limit
    val embeddings = sampledDescriptors.take(request.limit).map { descriptor ->
        val retrievableId = descriptor.retrievableId
            ?: throw ErrorStatusException(400, "A vector descriptor in field '$fieldName' has no retrievable ID.")
        EmbeddingPointResponse(retrievableId.toString(), descriptor.vector.value.toList())
    }

    ctx.json(
        EmbeddingsResponse(
            field = fieldName,
            returnedCount = embeddings.size,
            truncated = truncated,
            embeddings = embeddings
        )
    )
}
