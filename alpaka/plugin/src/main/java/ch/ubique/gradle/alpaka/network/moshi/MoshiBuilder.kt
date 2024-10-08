package ch.ubique.gradle.alpaka.network.moshi

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object MoshiBuilder {

	fun createMoshi(): Moshi {
		return Moshi.Builder()
			.addLast(KotlinJsonAdapterFactory())
			.build()
	}

}