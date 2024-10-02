package ch.ubique.gradle.alpaka.network

import ch.ubique.gradle.alpaka.extensions.toJson
import ch.ubique.gradle.alpaka.model.UploadRequest
import ch.ubique.gradle.alpaka.network.moshi.MoshiBuilder
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File

class BackendRepository {

	companion object {

		private var _service: BackendService? = null
		private var serviceCreationException: Exception? = null

		private var service: BackendService
			get() = _service ?: throw (serviceCreationException ?: UninitializedPropertyAccessException())
			set(value) {
				_service = value
			}

		fun resetService() {
			try {
				val httpClient = OkHttpInstance.getPreconfiguredClient()
				_service = Retrofit.Builder()
					.baseUrl("https://linth-ws.ubique.ch/v1/") // TODO Update base url to Alpaka once backend is migrated
					.addConverterFactory(ScalarsConverterFactory.create())
					.client(httpClient)
					.build()
					.create(BackendService::class.java)
			} catch (e: Exception) {
				serviceCreationException = e
			}
		}
	}

	init {
		resetService()
	}

	fun appsUpload(uploadRequest: UploadRequest, uploadKey: String) {
		val data = MoshiBuilder.createMoshi()
			.toJson(uploadRequest.toUploadDataJson(uploadKey = uploadKey))
			.toByteArray()
			.toRequestBody("application/json".toMediaType())

		val response = service.appsUpload(
			apk = uploadRequest.apk.toPartMap("apk", "application/octet-stream"),
			icon = uploadRequest.appIcon.toPartMap("icon", "image/png"),
			data = data
		).execute()

		if (response.isSuccessful.not()) throw HttpException(response)
	}

	private fun File.toPartMap(partName: String, mimeType: String): Map<String, RequestBody> {
		val payload = asRequestBody(contentType = mimeType.toMediaType())
		return mapOf("$partName\"; filename=\"$name" to payload)
	}

}