package com.lonx.lyrico.domain.poster

import com.lonx.audiotag.model.AudioPicture
import com.lonx.audiotag.model.AudioPictureType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistPosterEditsTest {

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

    // ------------------------------------------------------------ setFor

    @Test
    fun setForReplacesTheArtistPosterInPlace() {
        val a = artist("A")
        val b = artist("B")
        val cover = cover()
        val pictures = listOf(cover, a, b)
        val newA = artist("A", seed = 99)

        val result = ArtistPosterEdits.setFor(pictures, listOf("A", "B"), "A", newA)

        assertEquals(listOf(cover, newA, b), result)
        assertSame("B 的海报必须原样保留", b, result[2])
    }

    @Test
    fun setForAppendsWhenTheArtistHasNoPosterYet() {
        val a = artist("A")
        val pictures = listOf(a)
        val newB = artist("B", seed = 7)

        assertEquals(listOf(a, newB), ArtistPosterEdits.setFor(pictures, listOf("A", "B"), "B", newB))
    }

    @Test
    fun setForAlsoCollapsesLeftoverDuplicatesOfThatArtist() {
        val first = artist("A", seed = 1)
        val duplicate = artist("a", seed = 2)
        val b = artist("B")
        val newA = artist("A", seed = 9)

        val result = ArtistPosterEdits.setFor(listOf(first, duplicate, b), listOf("A", "B"), "A", newA)

        assertEquals(listOf(newA, b), result)
    }

    @Test
    fun setForNeverTouchesTheFrontCover() {
        val cover = cover()
        val pictures = listOf(cover)

        val result = ArtistPosterEdits.setFor(pictures, emptyList(), "", artist("", seed = 5))

        assertSame(cover, result[0])
        assertEquals(2, result.size)
    }

    // ------------------------------------------------------------ remove / replaceData

    @Test
    fun removeAndReplaceDataOnlyTouchTheGivenInstance() {
        val first = artist("A", seed = 1)
        val lookalike = artist("A", seed = 1) // 数据完全相同，但只有首实例是目标
        val pictures = listOf(first, lookalike)

        assertEquals(listOf(lookalike), ArtistPosterEdits.remove(pictures, first))

        val replaced = ArtistPosterEdits.replaceData(pictures, first, byteArrayOf(42), "image/png")
        assertEquals(byteArrayOf(42).toList(), replaced[0].data.toList())
        assertEquals("image/png", replaced[0].mimeType)
        assertSame(lookalike, replaced[1])
    }

    // ------------------------------------------------------------ reassign

    @Test
    fun reassignMovesTheWholeUnknownGroupAndDropsTheTargetsOldPoster() {
        val unknown = artist("C", seed = 1)   // 描述对不上任何现有艺术家
        val sameUnknown = artist("C", seed = 2)
        val existingA = artist("A", seed = 3)
        val pictures = listOf(existingA, unknown, sameUnknown)

        val result = ArtistPosterEdits.reassign(pictures, listOf("A", "B"), unknown, "A")

        assertEquals(1, result.size)
        // 改描述必然产生新实例，但图片数据仍是原来那张
        assertEquals("A", result[0].description)
        assertEquals(unknown.data.toList(), result[0].data.toList())
        assertTrue("A 原来的海报被顶掉", result.none { it === existingA })
        assertTrue("同组的另一张也被合并", result.none { it === sameUnknown })
    }

    @Test
    fun reassignKeepsOtherArtistsUntouched() {
        val b = artist("B")
        val unknown = artist("C", seed = 1)
        val pictures = listOf(b, unknown)

        val result = ArtistPosterEdits.reassign(pictures, listOf("A", "B"), unknown, "A")

        assertSame(b, result[0])
        assertEquals("A", result[1].description)
    }

    // ------------------------------------------------------------ revertAll

    @Test
    fun revertAllRestoresArtistPostersAndKeepsTheCover() {
        val cover = cover()
        val originalA = artist("A", seed = 1)
        val editedA = artist("A", seed = 9)
        val pictures = listOf(cover, editedA)

        val result = ArtistPosterEdits.revertAll(pictures, listOf(cover, originalA))

        assertSame(cover, result[0])
        assertSame(originalA, result[1])
    }

    @Test
    fun revertAllOnAnEmptiedListBringsTheOriginalsBack() {
        val cover = cover()
        val originalA = artist("A", seed = 1)

        val result = ArtistPosterEdits.revertAll(listOf(cover), listOf(cover, originalA))

        assertEquals(2, result.size)
        assertSame(originalA, result[1])
    }

    /**
     * 回归：原来的实现把艺术家图片一律追加到末尾，`[艺术家, 封面]` 会被重排成 `[封面, 艺术家]`，
     * 于是整份列表与原始列表「不相等」，看起来还原了其实没还原。
     */
    @Test
    fun revertAllKeepsTheOriginalPictureOrder() {
        val originalArtist = artist("A", seed = 1)
        val cover = cover()
        val editedArtist = artist("A", seed = 9)
        // 用户把艺术家海报放在封面之前
        val pictures = listOf(editedArtist, cover)
        val original = listOf(originalArtist, cover)

        val result = ArtistPosterEdits.revertAll(pictures, original)

        assertEquals(original, result)
    }

    @Test
    fun revertAllKeepsOrderWhenTheArtistPosterComesLast() {
        val originalArtist = artist("A", seed = 1)
        val cover = cover()
        val original = listOf(cover, originalArtist)

        val result = ArtistPosterEdits.revertAll(listOf(cover, artist("A", seed = 9)), original)

        assertEquals(original, result)
    }

    @Test
    fun revertAllAppendsPicturesTheUserAdded() {
        val originalArtist = artist("A", seed = 1)
        val cover = cover()
        val added = cover(seed = 2)
        val original = listOf(originalArtist, cover)

        val result = ArtistPosterEdits.revertAll(listOf(cover, added, artist("A", seed = 9)), original)

        assertEquals(listOf(originalArtist, cover, added), result)
    }

    @Test
    fun revertAllAfterRemovingTheCoverDropsIt() {
        val originalArtist = artist("A", seed = 1)
        val cover = cover()
        val original = listOf(originalArtist, cover)

        val result = ArtistPosterEdits.revertAll(listOf(artist("A", seed = 9)), original)

        assertEquals(listOf(originalArtist), result)
    }

    @Test
    fun revertAllDoesNotMoveTheBackPictureIntoARemovedFrontCoverSlot() {
        val frontCover = cover(seed = 1)
        val originalArtist = artist("A", seed = 2)
        val backPicture = AudioPicture(
            data = byteArrayOf(3),
            mimeType = "image/jpeg",
            description = "back",
            pictureType = AudioPictureType.BackCover.tagLibName
        )
        val original = listOf(frontCover, originalArtist, backPicture)
        val editedArtist = artist("A", seed = 9)

        val result = ArtistPosterEdits.revertAll(
            pictures = listOf(editedArtist, backPicture),
            original = original
        )

        assertEquals(listOf(originalArtist, backPicture), result)
    }

    @Test
    fun revertAllKeepsAReplacementCoverBeforeAnOriginallyLeadingArtistPoster() {
        val originalArtist = artist("A", seed = 1)
        val originalCover = cover(seed = 2)
        val replacementCover = cover(seed = 3)
        val original = listOf(originalArtist, originalCover)

        val result = ArtistPosterEdits.revertAll(
            pictures = listOf(replacementCover, artist("A", seed = 9)),
            original = original
        )

        assertEquals(listOf(replacementCover, originalArtist), result)
    }

    /**
     * 回归（真实操作链）：A、C 各有一张海报 → 把 C 重挂到 A（A 的原图被顶掉）→ 还原。
     * 逐张启发式还原只能找回 C，A 永远回不来；整组还原必须两张都回来。
     */
    @Test
    fun revertAllBringsBackThePosterThatReassignDropped() {
        val originalA = artist("A", seed = 1)
        val originalC = artist("C", seed = 2)
        val originals = listOf(originalA, originalC)

        val afterReassign = ArtistPosterEdits.reassign(
            pictures = originals,
            artistNames = listOf("A", "C"),
            target = originalC,
            artistName = "A"
        )
        assertEquals("重挂后只剩改挂过来的那一张", 1, afterReassign.size)

        val reverted = ArtistPosterEdits.revertAll(afterReassign, originals)

        assertEquals(originals, reverted)
    }

    /**
     * 回归（真实操作链）：重挂之后再裁剪，两个字段都变了。启发式匹配在这里必然猜错
     * （按描述会命中目标艺术家原来的海报），整组还原则与原图逐字段一致。
     */
    @Test
    fun revertAllAfterReassignThenCropRestoresBothOriginals() {
        val originalA = artist("A", seed = 1)
        val originalC = artist("C", seed = 2)
        val originals = listOf(originalA, originalC)

        val reassigned = ArtistPosterEdits.reassign(originals, listOf("A", "C"), originalC, "A")
        val cropped = ArtistPosterEdits.replaceData(
            pictures = reassigned,
            target = reassigned.single(),
            data = byteArrayOf(88),
            mimeType = "image/jpeg"
        )

        val reverted = ArtistPosterEdits.revertAll(cropped, originals)

        assertEquals(originals, reverted)
    }
}
