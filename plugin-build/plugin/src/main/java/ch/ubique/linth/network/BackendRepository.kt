package ch.ubique.linth.network

import ch.ubique.linth.model.UploadRequest
import ch.ubique.linth.network.moshi.MoshiBuilder
import ch.ubique.linth.network.moshi.toJson
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

		private var _service: Backend? = null
		private var serviceCreationException: Exception? = null

		private var service: Backend
			get() = _service ?: throw (serviceCreationException ?: UninitializedPropertyAccessException())
			set(value) {
				_service = value
			}

		fun resetService() {
			try {
				val httpClient = OkHttpInstance.getPreconfiguredClient()
				_service = Retrofit.Builder()
					.baseUrl("https://linth-ws.ubique.ch/v1/")
					.addConverterFactory(ScalarsConverterFactory.create())
					.client(httpClient)
					.build()
					.create(Backend::class.java)
			} catch (e: Exception) {
				serviceCreationException = e
			}
		}

	}

	init {
		resetService()
	}

	suspend fun appsUpload(uploadRequest: UploadRequest, uploadKey: String) {
		val data = MoshiBuilder.createMoshi()
			.toJson(uploadRequest.toUploadDataJson(uploadKey = uploadKey))
			.toByteArray()
			.toRequestBody("application/json".toMediaType())

		val response = service.appsUpload(
			apk = uploadRequest.apk.toPartMap("apk", "application/octet-stream"),
			icon = uploadRequest.apk.toPartMap("icon", "image/png"),
			data = data
		)
		if (response.isSuccessful.not()) throw HttpException(response)
	}

	private fun File.toPartMap(partName: String, mimeType: String): Map<String, RequestBody> {
		val payload = asRequestBody(contentType = mimeType.toMediaType())
		return mapOf("$partName\"; filename=\"${name}" to payload)
	}

}