package org.vitrivr.engine.module.features.feature.external.implementations.clip

import org.vitrivr.engine.core.model.descriptor.vector.FloatVectorDescriptor
import java.util.UUID
import kotlin.math.sqrt

/**
 * Groups CLIP image descriptors with spherical k-means using cosine similarity.
 *
 * Each input descriptor must belong to exactly one retrievable, as expected for an image collection.
 * The returned cluster IDs are compact and start at zero.
 */
object CLIPKMeansClusterer {

    data class Assignment(
        val retrievableId: UUID,
        val clusterId: Int,
        val cosineDistance: Double
    )

    private data class Sample(val id: UUID, val vector: FloatArray)

    /**
     * @param descriptors image descriptors loaded from the schema's CLIP field
     * @param k requested number of clusters; identical vectors may produce fewer populated clusters
     * @param maxIterations maximum number of assignment/update rounds
     */
    fun cluster(
        descriptors: Collection<FloatVectorDescriptor>,
        k: Int,
        maxIterations: Int = 100
    ): List<Assignment> {
        require(descriptors.isNotEmpty()) { "At least one CLIP descriptor is required." }
        require(k in 1..descriptors.size) { "k must be between 1 and the number of descriptors." }
        require(maxIterations > 0) { "maxIterations must be positive." }

        val samples = descriptors.map { descriptor ->
            val id = descriptor.retrievableId
                ?: throw IllegalArgumentException("Every CLIP descriptor must have a retrievable ID.")
            Sample(id, normalize(descriptor.vector.value))
        }.sortedBy { it.id.toString() }

        require(samples.map { it.id }.toSet().size == samples.size) {
            "Expected one CLIP descriptor per retrievable."
        }
        val dimensions = samples.first().vector.size
        require(samples.all { it.vector.size == dimensions }) {
            "All CLIP descriptors must have the same dimensionality."
        }

        var centroids = initializeCentroids(samples.map { it.vector }, k)
        var assignments = IntArray(samples.size) { -1 }

        for (iteration in 0 until maxIterations) {
            val nextAssignments = IntArray(samples.size) { index ->
                nearestCentroid(samples[index].vector, centroids)
            }
            val updatedCentroids = updateCentroids(samples.map { it.vector }, nextAssignments, centroids)
            val converged = nextAssignments.contentEquals(assignments)
            assignments = nextAssignments
            centroids = updatedCentroids
            if (converged) break
        }

        val finalAssignments = IntArray(samples.size) { index ->
            nearestCentroid(samples[index].vector, centroids)
        }
        val activeClusterIds = finalAssignments.distinct().sorted()
        val compactIds = activeClusterIds.withIndex().associate { (compact, original) -> original to compact }

        return samples.indices.map { index ->
            val rawClusterId = finalAssignments[index]
            Assignment(
                retrievableId = samples[index].id,
                clusterId = compactIds.getValue(rawClusterId),
                cosineDistance = cosineDistance(samples[index].vector, centroids[rawClusterId])
            )
        }
    }

    private fun initializeCentroids(vectors: List<FloatArray>, k: Int): Array<FloatArray> {
        val selected = BooleanArray(vectors.size)
        val centroids = mutableListOf<FloatArray>()
        selected[0] = true
        centroids += vectors[0].copyOf()

        while (centroids.size < k) {
            var farthestIndex = -1
            var farthestDistance = Double.NEGATIVE_INFINITY
            for (index in vectors.indices) {
                if (selected[index]) continue
                val nearestSimilarity = centroids.maxOf { dot(vectors[index], it) }
                val distance = 1.0 - nearestSimilarity
                if (distance > farthestDistance) {
                    farthestDistance = distance
                    farthestIndex = index
                }
            }
            if (farthestIndex < 0) break
            selected[farthestIndex] = true
            centroids += vectors[farthestIndex].copyOf()
        }

        return Array(k) { index -> centroids[index].copyOf() }
    }

    private fun updateCentroids(
        vectors: List<FloatArray>,
        assignments: IntArray,
        previous: Array<FloatArray>
    ): Array<FloatArray> {
        val sums = Array(previous.size) { DoubleArray(vectors.first().size) }
        val counts = IntArray(previous.size)

        vectors.indices.forEach { index ->
            val cluster = assignments[index]
            counts[cluster]++
            vectors[index].indices.forEach { dimension ->
                sums[cluster][dimension] += vectors[index][dimension].toDouble()
            }
        }

        return Array(previous.size) { cluster ->
            if (counts[cluster] == 0) {
                previous[cluster]
            } else {
                normalize(sums[cluster]) ?: previous[cluster]
            }
        }
    }

    private fun nearestCentroid(vector: FloatArray, centroids: Array<FloatArray>): Int {
        var bestIndex = 0
        var bestSimilarity = Double.NEGATIVE_INFINITY
        centroids.indices.forEach { index ->
            val similarity = dot(vector, centroids[index])
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity
                bestIndex = index
            }
        }
        return bestIndex
    }

    private fun normalize(vector: FloatArray): FloatArray {
        require(vector.isNotEmpty()) { "CLIP vectors must not be empty." }
        require(vector.all { it.isFinite() }) { "CLIP vectors must contain only finite values." }
        val norm = sqrt(vector.sumOf { value -> value.toDouble() * value })
        require(norm > 0.0) { "CLIP vectors must not be zero vectors." }
        return FloatArray(vector.size) { index -> (vector[index].toDouble() / norm).toFloat() }
    }

    private fun normalize(vector: DoubleArray): FloatArray? {
        val norm = sqrt(vector.sumOf { value -> value * value })
        if (norm <= 1e-12) return null
        return FloatArray(vector.size) { index -> (vector[index] / norm).toFloat() }
    }

    private fun dot(first: FloatArray, second: FloatArray): Double =
        first.indices.sumOf { index -> first[index].toDouble() * second[index] }

    private fun cosineDistance(first: FloatArray, second: FloatArray): Double =
        1.0 - dot(first, second).coerceIn(-1.0, 1.0)
}
