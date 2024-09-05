plugins {
	java
	id("ch.ubique.linth")
}

linthPlugin {
	apkFile = getLayout().buildDirectory.file("outputs/apk/dev/debug/example.apk")
	uploadKey = "712d6c5e-b23a-4354-8c77-a8440d436ede"

	proxy = "192.168.8.167:8888"
}
