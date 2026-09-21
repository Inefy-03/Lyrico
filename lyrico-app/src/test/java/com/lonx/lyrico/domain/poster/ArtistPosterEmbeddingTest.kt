package com.lonx.lyrico.domain.poster

import com.lonx.audiotag.model.AudioPicture
import com.lonx.audiotag.model.AudioPictureType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistPosterEmbeddingTest {

    private fun artist(description: String, seed: Int = description.hashCode()) = AudioPicture(
        data = byteArrayOf(seed.toByte()),
        mimeType = "image/jpeg",
        description = description,
        pictureType = AudioPictureType.Artist.tagLibName
    )

    private fun cover(seed: Int = 1) = AudioPicture(
        data = byteArrayOf(seed.toByte()),
        mimeType = "image/jpeg",
        description = "",
        pictureType = AudioPictureType.FrontCover.tagLibName
    )

    private val newData = byteArrayOf(7, 7, 7)

    // ------------------------------------------------------------ 描述

    @Test
    fun aSoleArtistKeepsTheDescriptionBlank() {
        // 描述为空在本应用里是「整首歌只有一个艺术家」的既有写法
        assertEquals("", descriptionFor(listOf("A"), "A"))
    }

    @Test
    fun severalArtistsForceTheNameIntoTheDescription() {
        // 描述为空时读取侧只认「整首歌只有一个艺术家」，多艺术家歌曲会变成「对不上」
        assertEquals("A", descriptionFor(listOf("A", "B"), "A"))
        assertEquals("A", descriptionFor(listOf("A", "B", "C"), " A "))
    }

    /** 通过与 [ArtistPosterEmbedding.plan] 相同的输入拿到它会给的描述。 */
    private fun descriptionFor(artistNames: List<String>, artistName: String): String? =
        ArtistPosterEmbedding.plan(
            pictures = emptyList(),
            artistNames = artistNames,
            artistName = artistName,
            data = newData,
            mimeType = "image/jpeg"
        )?.description

    // ------------------------------------------------------------ plan

    @Test
    fun aSongWithoutAPosterNeedsToBeWritten() {
        val binding = ArtistPosterEmbedding.plan(
            pictures = listOf(cover()),
            artistNames = listOf("A"),
            artistName = "A",
            data = newData,
            mimeType = "image/jpeg"
        )

        assertNotNull(binding)
        assertNull("新增时不替换任何图片", binding!!.replaced)
        assertEquals("", binding.description)
    }

    @Test
    fun anExistingPosterOfThatArtistIsReportedAsTheReplacement() {
        val existing = artist("A")
        val binding = ArtistPosterEmbedding.plan(
            pictures = listOf(cover(), existing, artist("B")),
            artistNames = listOf("A", "B"),
            artistName = "A",
            data = newData,
            mimeType = "image/jpeg"
        )

        assertNotNull(binding)
        assertSame("必须原地替换这位艺术家的那一张", existing, binding!!.replaced)
        assertEquals("A", binding.description)
    }

    @Test
    fun theSamePictureAlreadyInTheTagIsSkipped() {
        val existing = AudioPicture(
            data = newData,
            mimeType = "image/jpeg",
            description = "A",
            pictureType = AudioPictureType.Artist.tagLibName
        )

        val binding = ArtistPosterEmbedding.plan(
            pictures = listOf(existing),
            artistNames = listOf("A"),
            artistName = "A",
            data = newData,
            mimeType = "image/jpeg"
        )

        // 跳过是「授权后重跑」幂等的依据：已经写好的歌不会被重复写盘
        assertNull(binding)
    }

    @Test
    fun theSameBytesWithADifferentMimeTypeStillCountsAsADifferentPicture() {
        val existing = AudioPicture(
            data = newData,
            mimeType = "image/png",
            description = "A",
            pictureType = AudioPictureType.Artist.tagLibName
        )

        assertNotNull(
            ArtistPosterEmbedding.plan(
                pictures = listOf(existing),
                artistNames = listOf("A"),
                artistName = "A",
                data = newData,
                mimeType = "image/jpeg"
            )
        )
    }

    @Test
    fun anotherArtistsPosterIsNeverTreatedAsTheTarget() {
        val other = artist("B")
        val binding = ArtistPosterEmbedding.plan(
            pictures = listOf(other),
            artistNames = listOf("A", "B"),
            artistName = "A",
            data = newData,
            mimeType = "image/jpeg"
        )

        assertNotNull(binding)
        assertNull("B 的海报不能当成 A 的来覆盖", binding!!.replaced)
    }

    @Test
    fun aBlankDescriptionIsAmbiguousWithSeveralArtists() {
        val untagged = artist("", seed = 5)
        val binding = ArtistPosterEmbedding.plan(
            pictures = listOf(untagged),
            artistNames = listOf("A", "B"),
            artistName = "A",
            data = newData,
            mimeType = "image/jpeg"
        )

        assertNotNull(binding)
        assertNull("描述为空又多艺术家时归属不明，只能新增", binding!!.replaced)
    }

    @Test
    fun aBlankDescriptionFollowsTheOnlyArtist() {
        val untagged = artist("", seed = 5)
        val binding = ArtistPosterEmbedding.plan(
            pictures = listOf(untagged),
            artistNames = listOf("A"),
            artistName = "A",
            data = newData,
            mimeType = "image/jpeg"
        )

        assertNotNull(binding)
        assertSame(untagged, binding!!.replaced)
    }

    @Test
    fun aReplacedPosterKeepsItsOwnDescription() {
        val existing = artist("A")
        val binding = ArtistPosterEmbedding.plan(
            pictures = listOf(existing),
            artistNames = listOf("A"),
            artistName = "A",
            data = newData,
            mimeType = "image/jpeg"
        )!!

        val replaced = ArtistPosterEdits.replaceData(
            pictures = listOf(existing),
            target = binding.replaced!!,
            data = newData,
            mimeType = "image/jpeg"
        )

        assertEquals(1, replaced.size)
        assertEquals("A", replaced.single().description)
        assertTrue(replaced.single().data.contentEquals(newData))
        assertNotEquals(existing, replaced.single())
    }
}
