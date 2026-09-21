package com.lonx.lyrico.domain.poster

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtistPosterFileNamingTest {

    // ------------------------------------------------------------ stemFor

    @Test
    fun aPlainArtistNameBecomesTheFileName() {
        assertEquals("周杰伦", ArtistPosterFileNaming.stemFor("周杰伦"))
        assertEquals("AC_DC", ArtistPosterFileNaming.stemFor("AC_DC"))
    }

    @Test
    fun charactersAFileNameCannotHoldBecomeTheirFullWidthForm() {
        // 与「重命名文件」用的是同一套规则，写下去的文件名才认得出是同一位艺术家
        assertEquals("AC／DC", ArtistPosterFileNaming.stemFor("AC/DC"))
        assertEquals("Re：Zero", ArtistPosterFileNaming.stemFor("Re:Zero"))
        assertEquals("A｜B", ArtistPosterFileNaming.stemFor("A|B"))
    }

    @Test
    fun surroundingWhitespaceIsDropped() {
        assertEquals("Artist", ArtistPosterFileNaming.stemFor("  Artist  "))
    }

    @Test
    fun aNameThatCleansUpToNothingHasNoFileName() {
        assertNull(ArtistPosterFileNaming.stemFor("   "))
        assertNull(ArtistPosterFileNaming.stemFor(""))
    }

    // ------------------------------------------------------------ extensionFor

    @Test
    fun theMimeTypeDecidesTheExtension() {
        assertEquals("jpg", ArtistPosterFileNaming.extensionFor("image/jpeg"))
        assertEquals("jpg", ArtistPosterFileNaming.extensionFor("image/jpg"))
        assertEquals("png", ArtistPosterFileNaming.extensionFor("image/png"))
        assertEquals("webp", ArtistPosterFileNaming.extensionFor("image/webp"))
        assertEquals("mp4", ArtistPosterFileNaming.extensionFor("video/mp4"))
    }

    @Test
    fun aMimeTypeWithParametersStillResolves() {
        assertEquals("png", ArtistPosterFileNaming.extensionFor("image/png;charset=utf-8"))
        assertEquals("jpg", ArtistPosterFileNaming.extensionFor("IMAGE/JPEG"))
    }

    @Test
    fun anUnknownMimeTypeFallsBackToThePickedFileNameThenToJpeg() {
        assertEquals("png", ArtistPosterFileNaming.extensionFor(null, "poster.PNG"))
        assertEquals("jpg", ArtistPosterFileNaming.extensionFor(null, null))
        assertEquals("jpg", ArtistPosterFileNaming.extensionFor("image/*", null))
        assertEquals("jpg", ArtistPosterFileNaming.extensionFor("application/octet-stream", "noext"))
    }

    // ------------------------------------------------------------ matchesArtist

    @Test
    fun onlyFilesOfThatArtistAreMatched() {
        val files = listOf("A.jpg", "B.jpg", "AC_DC.png", "Artist.gif", "A_cover.jpg")

        assertEquals(listOf("A.jpg", "A_cover.jpg"), ArtistPosterFileNaming.matchesArtist(files, "A"))
    }

    @Test
    fun theClosestMatchComesFirst() {
        val files = listOf("A_live.jpg", "A.JPG", "A_alt.png")

        // 0 级（名字完全一致）在前，然后是后缀变体；读取海报时也是这个顺序
        assertEquals(listOf("A.JPG", "A_alt.png", "A_live.jpg"), ArtistPosterFileNaming.matchesArtist(files, "A"))
    }

    @Test
    fun anArtistWithNoPosterMatchesNothing() {
        assertEquals(emptyList<String>(), ArtistPosterFileNaming.matchesArtist(listOf("B.jpg"), "A"))
        assertEquals(emptyList<String>(), ArtistPosterFileNaming.matchesArtist(emptyList(), "A"))
    }

    @Test
    fun theFullWidthFormOfAnInvalidCharacterMatchesTheArtistName() {
        val files = listOf("AC／DC.jpg")

        assertEquals(listOf("AC／DC.jpg"), ArtistPosterFileNaming.matchesArtist(files, "AC/DC"))
    }
}
