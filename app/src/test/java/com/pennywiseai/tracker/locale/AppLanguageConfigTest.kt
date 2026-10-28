package com.pennywiseai.tracker.locale

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The languages PennyWise offers are declared twice: [AppLanguage.supportedTags]
 * (the in-app picker, every Android version) and res/xml/locales_config.xml
 * (System settings → App languages, Android 13+). They must list the same set,
 * and each listed language must actually be translated — a values-* folder
 * that is mostly English shouldn't be offered as a language.
 */
class AppLanguageConfigTest {

    private val resDir = File("src/main/res").takeIf { it.isDirectory }
        ?: File("app/src/main/res")

    private fun parse(file: File) = DocumentBuilderFactory.newInstance().apply {
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        isExpandEntityReferences = false
    }.newDocumentBuilder().parse(file)

    private fun declaredLocales(): List<String> {
        val doc = parse(File(resDir, "xml/locales_config.xml"))
        val nodes = doc.getElementsByTagName("locale")
        return (0 until nodes.length).map { (nodes.item(it) as Element).getAttribute("android:name") }
    }

    /** `hi-IN` → `values-hi-rIN`, `mr` → `values-mr`; the unqualified folder is en-US. */
    private fun folderFor(tag: String): File {
        if (tag == "en-US") return File(resDir, "values")
        val locale = Locale.forLanguageTag(tag)
        val name = if (locale.country.isEmpty()) "values-${locale.language}"
        else "values-${locale.language}-r${locale.country}"
        return File(resDir, name)
    }

    /** Translatable string and plurals names in a values folder's strings*.xml. */
    private fun translatableNames(dir: File): Set<String> =
        dir.listFiles { f -> f.name.startsWith("strings") && f.name.endsWith(".xml") }.orEmpty()
            .flatMap { file ->
                val children = parse(file).documentElement.childNodes
                (0 until children.length).mapNotNull { children.item(it) as? Element }
                    .filter { it.tagName == "string" || it.tagName == "plurals" }
                    .filter { it.getAttribute("translatable") != "false" }
                    .map { it.getAttribute("name") }
            }.toSet()

    @Test
    fun `picker and system locale config list the same languages`() {
        assertEquals(AppLanguage.supportedTags.sorted(), declaredLocales().sorted())
    }

    @Test
    fun `the unqualified resources are the English the config declares`() {
        val props = File(resDir, "resources.properties").readText()
        assertTrue(props.contains("unqualifiedResLocale=en-US"))
        assertTrue("en-US" in AppLanguage.supportedTags)
    }

    @Test
    fun `every offered language is translated well enough to ship`() {
        val english = translatableNames(File(resDir, "values"))
        assertTrue("No base strings found under ${resDir.absolutePath}", english.isNotEmpty())
        val problems = AppLanguage.supportedTags.filter { it != "en-US" }.mapNotNull { tag ->
            val dir = folderFor(tag)
            if (!dir.isDirectory) return@mapNotNull "$tag: no ${dir.name} folder"
            val covered = (translatableNames(dir) intersect english).size
            val share = covered.toDouble() / english.size
            if (share < MIN_COVERAGE) "$tag: ${"%.1f".format(share * 100)}% translated" else null
        }
        assertTrue(
            "Offered languages below ${(MIN_COVERAGE * 100).toInt()}% translated — fill them in " +
                "or drop them from AppLanguage.supportedTags and locales_config.xml:\n" +
                problems.joinToString("\n"),
            problems.isEmpty()
        )
    }

    @Test
    fun `languages are named in their own script, system default first`() {
        val options = AppLanguage.options(current = null)
        assertNull(options.first().tag)
        assertEquals(AppLanguage.supportedTags.size + 1, options.size)
        val names = options.associate { it.tag to it.nativeName }
        assertEquals("हिन्दी", names["hi-IN"])
        assertEquals("தமிழ்", names["ta"])
        // Region only where one language has several variants.
        assertTrue(names["pt-BR"]!!.contains("Brasil"))
        assertEquals("Deutsch", names["de-DE"])
    }

    @Test
    fun `a locale matches its offered tag by language when the region differs`() {
        assertEquals("hi-IN", AppLanguage.selectedTag(Locale.forLanguageTag("hi-IN")))
        assertEquals("bn", AppLanguage.selectedTag(Locale.forLanguageTag("bn-BD")))
        assertNull(AppLanguage.selectedTag(null))
        // A language set earlier from System settings but not offered stays visible.
        val nepali = Locale.forLanguageTag("ne-NP")
        assertEquals("ne-NP", AppLanguage.selectedTag(nepali))
        assertTrue(AppLanguage.options(nepali).any { it.tag == "ne-NP" })
    }

    private companion object {
        const val MIN_COVERAGE = 0.55
    }
}
