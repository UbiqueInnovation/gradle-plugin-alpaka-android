package ch.ubique.gradle.alpaka

import org.junit.Test
import java.io.File

class AlpakaPluginTest {

    private val appExample: File = File("")

    @Test
    fun `appexample upload task kotlin dsl`() {
        /*
        println(appExample.absolutePath)
        val projectpath = appExample.absoluteFile.parentFile.parentFile
        println(projectpath.absolutePath)

        GradleRunner.create()
            .withProjectDir(projectpath)
            .withArguments("appexample:generateAppIconDevDebug", "-PubiqueMavenRootUrl=abc", "-PubiqueMavenRepoName=abc", "-PubiqueMavenUser=abc", "-PubiqueMavenPass=abc")
            //.withPluginClasspath()
            .build()
         */
    }

    @Test
    fun `extension templateExampleConfig is created correctly`() {
        /*
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("ch.ubique.gradle.alpaka")

        assertNotNull(project.extensions.getByName("templateExampleConfig"))
         */
    }

    @Test
    fun `parameters are passed correctly from extension to task`() {
        /*
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("ch.ubique.gradle.alpaka")
        val aFile = File(project.projectDir, ".tmp")
        (project.extensions.getByName("templateExampleConfig") as AlpakaPluginConfig).apply {
            tag.set("a-sample-tag")
            message.set("just-a-message")
            outputFile.set(aFile)
        }

        val task = project.tasks.getByName("templateExample") as UploadToUbDiagTask

        assertEquals("a-sample-tag", task.tag.get())
        assertEquals("just-a-message", task.message.get())
        assertEquals(aFile, task.outputFile.get().asFile)
         */
    }

}
