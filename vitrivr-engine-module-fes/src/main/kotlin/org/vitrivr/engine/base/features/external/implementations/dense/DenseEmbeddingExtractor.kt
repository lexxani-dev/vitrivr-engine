package org.vitrivr.engine.base.features.external.implementations.dense

import org.vitrivr.engine.base.features.external.api.AsrApi
import org.vitrivr.engine.base.features.external.api.ImageEmbeddingApi
import org.vitrivr.engine.base.features.external.api.TextEmbeddingApi
import org.vitrivr.engine.base.features.external.common.ExternalFesAnalyser
import org.vitrivr.engine.base.features.external.common.FesExtractor
import org.vitrivr.engine.core.context.Context
import org.vitrivr.engine.core.model.content.element.ContentElement
import org.vitrivr.engine.core.model.content.element.ImageContent
import org.vitrivr.engine.core.model.content.element.TextContent
import org.vitrivr.engine.core.model.descriptor.Descriptor
import org.vitrivr.engine.core.model.descriptor.vector.FloatVectorDescriptor
import org.vitrivr.engine.core.model.metamodel.Schema
import org.vitrivr.engine.core.model.retrievable.Retrievable
import org.vitrivr.engine.core.operators.Operator
import java.util.*

/**
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DenseEmbeddingExtractor : FesExtractor<ContentElement<*>, FloatVectorDescriptor> {

    constructor(
        input: Operator<out Retrievable>,
        field: Schema.Field<ContentElement<*>, FloatVectorDescriptor>,
        analyser: ExternalFesAnalyser<ContentElement<*>, FloatVectorDescriptor>,
        context: Context
    ) : super(input, field, analyser, context)

    constructor(
        input: Operator<out Retrievable>,
        name: String,
        analyser: ExternalFesAnalyser<ContentElement<*>, FloatVectorDescriptor>,
        context: Context
    ) : super(input, name, analyser, context)

    /** The [AsrApi] used to perform extraction with. */
    private val textApi by lazy { TextEmbeddingApi(this.host, model, this.timeoutMs, this.pollingIntervalMs, this.retries) }

    /** The [AsrApi] used to perform extraction with. */
    private val imageApi by lazy { ImageEmbeddingApi(this.host, model, this.timeoutMs, this.pollingIntervalMs, this.retries) }


    /**
     * Internal method to perform extraction on [Retrievable].
     **
     * @param retrievables The [Retrievable]s to process.
     * @return List of resulting [Descriptor]s grouped by [Retrievable].
     */
    override fun extract(batch: List<Retrievable>): List<List<FloatVectorDescriptor>> {
        /* Keep each content element associated with the retrievable it came from. */
        val textContent = batch.flatMapIndexed { retrievableIndex, retrievable ->
            retrievable.content.filterIsInstance<TextContent>().map { retrievableIndex to it }
        }
        val imageContent = batch.flatMapIndexed { retrievableIndex, retrievable ->
            retrievable.content.filterIsInstance<ImageContent>().map { retrievableIndex to it }
        }
        val descriptorsByRetrievable = Array(batch.size) { mutableListOf<FloatVectorDescriptor>() }

        if (textContent.isNotEmpty()) {
            val textResults = this.textApi.analyseBatched(textContent.map { it.second })
            check(textResults.size == textContent.size) {
                "FES returned ${textResults.size} text embeddings for ${textContent.size} inputs."
            }
            textContent.zip(textResults).forEach { (source, vector) ->
                val retrievableIndex = source.first
                descriptorsByRetrievable[retrievableIndex].add(
                    FloatVectorDescriptor(
                        UUID.randomUUID(),
                        batch[retrievableIndex].id,
                        vector,
                        this.field
                    )
                )
            }
        }

        if (imageContent.isNotEmpty()) {
            val imageResults = this.imageApi.analyseBatched(imageContent.map { it.second })
            check(imageResults.size == imageContent.size) {
                "FES returned ${imageResults.size} image embeddings for ${imageContent.size} inputs."
            }
            imageContent.zip(imageResults).forEach { (source, vector) ->
                val retrievableIndex = source.first
                descriptorsByRetrievable[retrievableIndex].add(
                    FloatVectorDescriptor(
                        UUID.randomUUID(),
                        batch[retrievableIndex].id,
                        vector,
                        this.field
                    )
                )
            }
        }

        return descriptorsByRetrievable.map { it.toList() }
    }


}
