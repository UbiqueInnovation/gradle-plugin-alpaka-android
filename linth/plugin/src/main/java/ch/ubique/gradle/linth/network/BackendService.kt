package ch.ubique.gradle.linth.network

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.PartMap

interface BackendService {

	@Multipart
	@PUT("apps/upload")
	fun appsUpload(
		@PartMap apk: Map<String, @JvmSuppressWildcards RequestBody>,
		@PartMap icon: Map<String, @JvmSuppressWildcards RequestBody>,
		@Part("data") data: RequestBody,
	): Call<Unit>

}