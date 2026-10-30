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
import org.vitrivr.engine.core.model.metamodel.Schema
import org.vitrivr.engine.core.model.descriptor.vector.FloatVectorDescriptor
import org.vitrivr.engine.module.features.feature.external.implementations.clip.CLIPKMeansClusterer
import org.vitrivr.engine.server.api.rest.model.ErrorStatus
import org.vitrivr.engine.server.api.rest.model.ErrorStatusException

@Serializable
data class ClusteringRequest(val k: Int? = null)

@Serializable
data class ClusterAssignmentResponse(
    val retrievableId: String,
    val clusterId: Int,
    val cosineDistance: Double
)

@Serializable
data class ClusteringResponse(
    val requestedK: Int,
    val clusterCount: Int,
    val imageCount: Int,
    val assignments: List<ClusterAssignmentResponse>
)

@OpenApi(
    path = "/api/{schema}/clusters",
    methods = [HttpMethod.POST],
    summary = "Clusters all CLIP image descriptors in a schema.",
    operationId = "postClusterImages",
    tags = ["Retrieval"],
    pathParams = [
        OpenApiParam("schema", type = String::class, description = "The schema to cluster.", required = true)
    ],
    requestBody = OpenApiRequestBody([OpenApiContent(ClusteringRequest::class)], required = false),
    responses = [
        OpenApiResponse("200", [io.javalin.openapi.OpenApiContent(ClusteringResponse::class)]),
        OpenApiResponse("400", [io.javalin.openapi.OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("404", [io.javalin.openapi.OpenApiContent(ErrorStatus::class)])
    ]
)
fun executeClustering(ctx: Context, schema: Schema) {
    val request = try {
        if (ctx.body().isBlank()) ClusteringRequest() else ctx.bodyAsClass<ClusteringRequest>()
    } catch (e: Exception) {
        throw ErrorStatusException(400, "Invalid clustering request: ${e.message}")
    }

    val clipField = schema["clip"]
        ?: throw ErrorStatusException(404, "Schema '${schema.name}' has no CLIP field.")
    val descriptors = clipField.getReader().getAll().filterIsInstance<FloatVectorDescriptor>().toList()
    if (descriptors.isEmpty()) {
        throw ErrorStatusException(400, "Schema '${schema.name}' contains no CLIP descriptors to cluster.")
    }

    val requestedK = request.k ?: 6
    val assignments = try {
        CLIPKMeansClusterer.cluster(descriptors, requestedK)
    } catch (e: IllegalArgumentException) {
        throw ErrorStatusException(400, e.message ?: "Invalid clustering request.")
    }

    ctx.json(
        ClusteringResponse(
            requestedK = requestedK,
            clusterCount = assignments.map { it.clusterId }.distinct().size,
            imageCount = assignments.size,
            assignments = assignments.map {
                ClusterAssignmentResponse(
                    retrievableId = it.retrievableId.toString(),
                    clusterId = it.clusterId,
                    cosineDistance = it.cosineDistance
                )
            }
        )
    )
}
