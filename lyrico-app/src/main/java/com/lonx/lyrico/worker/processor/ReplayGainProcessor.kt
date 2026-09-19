package com.lonx.lyrico.worker.processor

import com.lonx.audiotag.model.AudioTagData
import com.lonx.lyrico.data.model.entity.BatchTaskEntity
import com.lonx.lyrico.data.model.entity.BatchTaskItemEntity
import com.lonx.lyrico.data.repository.SettingsRepository
import com.lonx.lyrico.data.song.library.SongLibraryRepository
import com.lonx.lyrico.data.song.tag.AudioTagReadOptions
import com.lonx.lyrico.data.song.tag.AudioTagRepository
import com.lonx.lyrico.domain.song.usecase.PatchSongTagsUseCase
import com.lonx.lyrico.domain.song.usecase.SaveAudioTagsResult
import com.lonx.lyrico.utils.ReplayGainCalculateState
import com.lonx.lyrico.utils.ReplayGainScanner
import kotlinx.coroutines.flow.first

class ReplayGainProcessor(
    private val songLibraryRepository: SongLibraryRepository,
    private val patchSongTagsUseCase: PatchSongTagsUseCase,
    private val replayGainScanner: ReplayGainScanner,
    private val settingsRepository: SettingsRepository,
    private val audioTagRepository: AudioTagRepository
) : BatchTaskProcessor {

    override suspend fun process(
        task: BatchTaskEntity,
        item: BatchTaskItemEntity,
        onProgress: suspend (Float) -> Unit
    ): BatchTaskProcessResult {
        val song = songLibraryRepository.getSongByUri(item.songUri)
            ?: throw BatchTaskSkippedException("Song not found")

        // Library metadata can be stale after tags are removed or edited externally.
        // Only the tags currently in the audio file should prevent calculation.
        checkReplayGainTags(audioTagRepository, song.uri)

        var analysisSuccess = false
        var analysisResult: com.lonx.lyrico.utils.ReplayGainAnalysis? = null

        replayGainScanner.analyze(item.songUri).collect { state ->
            when (state) {
                is ReplayGainCalculateState.Success -> {
                    analysisResult = state.analysis
                    analysisSuccess = true
                }
                is ReplayGainCalculateState.Cancelled,
                is ReplayGainCalculateState.Failed -> {
                    analysisSuccess = false
                }
                is ReplayGainCalculateState.Progress -> {
                    onProgress(state.percent)
                }
            }
        }

        if (!analysisSuccess || analysisResult == null) {
            throw Exception("ReplayGain analysis failed")
        }

        val targetLoudness = settingsRepository.replayGainTargetLoudness.first()
        val tagData = AudioTagData(
            replayGainTrackGain = replayGainScanner.formatGain(analysisResult, targetLoudness),
            replayGainTrackPeak = replayGainScanner.formatPeak(analysisResult.peak),
            replayGainReferenceLoudness = replayGainScanner.formatReferenceLoudness(targetLoudness)
        )

        val result = patchSongTagsUseCase(item.songUri, tagData)
        if (result !is SaveAudioTagsResult.Success) {
            throw Exception("Write failed")
        }

        return BatchTaskProcessResult()
    }
}

internal suspend fun checkReplayGainTags(repository: AudioTagRepository, uri: String) {
    val tag = repository.read(uri, AudioTagReadOptions(strict = true))
    if (!tag.replayGainTrackGain.isNullOrBlank() ||
        !tag.replayGainTrackPeak.isNullOrBlank() ||
        !tag.replayGainAlbumGain.isNullOrBlank() ||
        !tag.replayGainAlbumPeak.isNullOrBlank() ||
        !tag.replayGainReferenceLoudness.isNullOrBlank()
    ) {
        throw BatchTaskSkippedException("ReplayGain already exists")
    }
}
