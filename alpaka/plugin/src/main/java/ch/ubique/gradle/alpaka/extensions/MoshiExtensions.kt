package ch.ubique.gradle.alpaka.extensions

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

fun moshi() = Moshi.Builder()
	.addLast(KotlinJsonAdapterFactory())
	.build()

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T> Moshi.fromJson(json: String?): T? = json?.let { adapter<T>().fromJson(it) }

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Any> Moshi.fromJsonNotNull(json: String): T = requireNotNull(adapter<T>().fromJson(json))

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T> Moshi.toJson(value: T): String = adapter<T>().toJson(value)

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T> Moshi.toPrettyJson(value: T): String = adapter<T>().indent("  ").serializeNulls().toJson(value)

inline fun <reified T> Moshi.listAdapter(): JsonAdapter<List<T>> = adapter(Types.newParameterizedType(List::class.java, T::class.java))
