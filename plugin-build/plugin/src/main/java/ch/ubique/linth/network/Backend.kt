package ch.ubique.linth.network

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.PartMap

interface Backend {

	@Multipart
	@PUT("apps/upload")
	suspend fun appsUpload(
		@PartMap apk: Map<String, @JvmSuppressWildcards RequestBody>,
		@PartMap icon: Map<String, @JvmSuppressWildcards RequestBody>,
		@Part("data") data: RequestBody,
	): Response<Unit>

}