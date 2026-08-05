package ch.ubique.gradle.alpaka.network

import ch.ubique.gradle.alpaka.extensions.moshi
import ch.ubique.gradle.alpaka.extensions.toJson
import ch.ubique.gradle.alpaka.model.AppMetadata
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File

internal interface BackendRepositoryInterface {
	fun appsUpload(appMetadata: AppMetadata, apk: File, appIcon: File, uploadKey: String)
}

internal class BackendRepository(baseUrl: String) : BackendRepositoryInterface {

	private val service: BackendService

	init {
		val httpClient = OkHttpInstance.getPreconfiguredClient()
		service = Retrofit.Builder()
			.baseUrl(baseUrl)
			.addConverterFactory(ScalarsConverterFactory.create())
			.client(httpClient)
			.build()
			.create(BackendService::class.java)
	}

	override fun appsUpload(appMetadata: AppMetadata, apk: File, appIcon: File, uploadKey: String) {
		val data = moshi()
			.toJson(appMetadata.toUploadDataDto(uploadKey = uploadKey))
			.toByteArray()
			.toRequestBody("application/json".toMediaType())

		val response = service.appsUpload(
			apk = apk.toPartMap("apk", "application/octet-stream"),
			icon = appIcon.toPartMap("icon", "image/png"),
			data = data,
		).execute()

		if (response.isSuccessful.not()) throw HttpException(response)
	}

	private fun File.toPartMap(partName: String, mimeType: String): Map<String, RequestBody> {
		val payload = asRequestBody(contentType = mimeType.toMediaType())
		return mapOf("$partName\"; filename=\"$name" to payload)
	}

}

internal object DryRunBackendRepository : BackendRepositoryInterface {
	override fun appsUpload(appMetadata: AppMetadata, apk: File, appIcon: File, uploadKey: String) = Unit
}
