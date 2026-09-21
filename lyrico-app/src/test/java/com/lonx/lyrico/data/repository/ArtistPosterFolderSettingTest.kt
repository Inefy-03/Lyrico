package com.lonx.lyrico.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class ArtistPosterFolderSettingTest {

    @Test
    fun legacyMultipleFoldersKeepOnlyTheFirstFolder() {
        assertEquals(
            "content://posters/first",
            decodeArtistPosterFolder(
                """["content://posters/first","content://posters/second"]"""
            )
        )
    }

    @Test
    fun selectingAFolderStoresOnlyThatFolder() {
        assertEquals(
            "content://posters/new",
            decodeArtistPosterFolder(encodeArtistPosterFolder("content://posters/new"))
        )
    }

    @Test
    fun clearingTheFolderStoresAnEmptySelection() {
        assertEquals(
            null,
            decodeArtistPosterFolder(encodeArtistPosterFolder(null))
        )
    }
}
