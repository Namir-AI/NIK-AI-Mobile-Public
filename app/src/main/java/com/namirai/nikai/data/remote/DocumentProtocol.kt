package com.namirai.nikai.data.remote

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class DocumentItem(
    val id: Long,
    val filename: String,
    val createdAt: String?,
    val charCount: Int,
    val chunkCount: Int,
)

data class DocumentUploadResult(
    val success: Boolean,
    val document: DocumentItem,
)

enum class DocumentScope(val apiValue: String) {
    Selected("selected"),
    All("all"),
}

enum class DocumentMode(val apiValue: String) {
    Normal("normal"),
    Strict("strict"),
}

data class DocumentChatConfig(
    val useDocuments: Boolean,
    val documentScope: String,
    val documentIds: List<Long>,
    val documentMode: String,
)

class DocumentProtocolException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

internal object DocumentJsonParser {
    fun parseList(json: String): List<DocumentItem> = try {
        val array = JSONArray(json)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index)
                    ?: throw DocumentProtocolException("Expected a document object.")
                add(parseDocument(item, requireCreatedAt = true))
            }
        }
    } catch (error: JSONException) {
        throw DocumentProtocolException("Invalid document list.", error)
    }

    fun parseUploadResult(json: String): DocumentUploadResult = try {
        val root = JSONObject(json)
        val document = root.optJSONObject("document")
            ?: throw DocumentProtocolException("Uploaded document metadata is missing.")
        DocumentUploadResult(
            success = root.requireBoolean("success"),
            document = parseDocument(document, requireCreatedAt = false),
        )
    } catch (error: JSONException) {
        throw DocumentProtocolException("Invalid document upload response.", error)
    }

    fun parseDeleteSuccess(json: String): Boolean = try {
        JSONObject(json).requireBoolean("success")
    } catch (error: JSONException) {
        throw DocumentProtocolException("Invalid document delete response.", error)
    }

    private fun parseDocument(
        item: JSONObject,
        requireCreatedAt: Boolean,
    ): DocumentItem = DocumentItem(
        id = item.requireLong("id"),
        filename = item.requireString("filename"),
        createdAt = if (requireCreatedAt) {
            item.requireString("created_at")
        } else {
            item.optionalString("created_at")
        },
        charCount = item.requireInt("char_count"),
        chunkCount = item.requireInt("chunk_count"),
    )

    private fun JSONObject.requireLong(name: String): Long {
        if (!has(name) || isNull(name)) throw DocumentProtocolException("Missing $name.")
        return try {
            getLong(name)
        } catch (error: JSONException) {
            throw DocumentProtocolException("Invalid $name.", error)
        }
    }

    private fun JSONObject.requireInt(name: String): Int {
        if (!has(name) || isNull(name)) throw DocumentProtocolException("Missing $name.")
        return try {
            getInt(name)
        } catch (error: JSONException) {
            throw DocumentProtocolException("Invalid $name.", error)
        }
    }

    private fun JSONObject.requireString(name: String): String {
        val value = opt(name)
        if (value !is String) throw DocumentProtocolException("Missing $name.")
        return value
    }

    private fun JSONObject.optionalString(name: String): String? =
        opt(name).takeUnless { it == null || it == JSONObject.NULL } as? String

    private fun JSONObject.requireBoolean(name: String): Boolean {
        if (!has(name) || isNull(name)) throw DocumentProtocolException("Missing $name.")
        return try {
            getBoolean(name)
        } catch (error: JSONException) {
            throw DocumentProtocolException("Invalid $name.", error)
        }
    }
}

internal object DocumentStateReducer {
    fun isSupportedFilename(filename: String): Boolean =
        filename.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase() in setOf("pdf", "txt", "md")

    fun toggleSelection(
        selectedIds: Set<Long>,
        documentId: Long,
    ): Set<Long> = selectedIds.toMutableSet().apply {
        if (!add(documentId)) remove(documentId)
    }

    fun removeDeletedSelection(
        selectedIds: Set<Long>,
        deletedId: Long,
    ): Set<Long> = selectedIds - deletedId

    fun reconcileSelection(
        selectedIds: Set<Long>,
        authoritativeDocuments: List<DocumentItem>,
    ): Set<Long> {
        val validIds = authoritativeDocuments.mapTo(mutableSetOf(), DocumentItem::id)
        return selectedIds.intersect(validIds)
    }

    fun refreshFailure(current: List<DocumentItem>): List<DocumentItem> = current

    fun deleteSuccess(
        current: List<DocumentItem>,
        deletedId: Long,
    ): List<DocumentItem> = current.filterNot { it.id == deletedId }

    fun deleteFailure(current: List<DocumentItem>): List<DocumentItem> = current

    fun chatConfig(
        useDocuments: Boolean,
        scope: DocumentScope,
        selectedIds: Set<Long>,
        mode: DocumentMode,
    ): DocumentChatConfig = DocumentChatConfig(
        useDocuments = useDocuments,
        documentScope = scope.apiValue,
        documentIds = if (useDocuments && scope == DocumentScope.Selected) {
            selectedIds.sorted()
        } else {
            emptyList()
        },
        documentMode = mode.apiValue,
    )

    fun canSendWithRag(
        useDocuments: Boolean,
        scope: DocumentScope,
        selectedIds: Set<Long>,
    ): Boolean = !useDocuments || scope == DocumentScope.All || selectedIds.isNotEmpty()
}
