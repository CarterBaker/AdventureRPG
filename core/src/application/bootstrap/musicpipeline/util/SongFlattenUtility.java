package application.bootstrap.musicpipeline.util;

import application.bootstrap.musicpipeline.song.SongClipKind;
import application.bootstrap.musicpipeline.song.SongClipStruct;
import application.bootstrap.musicpipeline.song.SongCursorStruct;
import application.bootstrap.musicpipeline.song.SongData;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SongFlattenUtility extends EngineUtility {

    /*
     * Lays a song out flat, as a DAW renders nested sequences: every notes
     * and sound clip that plays, the song clips around it opened in place at
     * any depth, each as a cursor holding the window of the played song's
     * ticks it sounds within, the shift from its source's ticks to the played
     * song's, and the transpose and gain every clip and lane around it add up
     * to. A muted clip, a muted lane and an unsoloed lane while another is
     * soloed play nothing, at every depth. A song clip plays its song from
     * its offset for its length, at the played song's tempo. Playback and
     * MIDI export both play exactly this. Cursors are reused from the list
     * given and only made when it holds too few. Never held — all methods
     * static.
     */

    // Flatten \\

    // The song laid out into the cursors given, one lane of it or SONG_LANE_ALL; how many cursors it filled
    public static int flatten(SongData song, int lane, ObjectArrayList<SongCursorStruct> cursors) {
        return flattenSong(song, lane, EngineSetting.SONG_LANE_ALL, 0, 0, Integer.MAX_VALUE, 0, 1f, cursors, 0);
    }

    private static int flattenSong(
            SongData song,
            int lane,
            int playedLane,
            int base,
            int windowStart,
            int windowEnd,
            int transpose,
            float gain,
            ObjectArrayList<SongCursorStruct> cursors,
            int count) {

        for (int i = 0; i < song.getClipCount(); i++) {

            SongClipStruct clip = song.getClip(i);
            int start = Math.max(windowStart, base + clip.getStart());
            int end = Math.min(windowEnd, base + clip.getEnd());

            if (clip.isMuted() || !song.isLaneHeard(clip.getLane()) || start >= end
                    || (lane != EngineSetting.SONG_LANE_ALL && clip.getLane() != lane))
                continue;

            int clipLane = playedLane != EngineSetting.SONG_LANE_ALL ? playedLane : clip.getLane();
            int shift = base + clip.getStart() - clip.getOffset();
            int clipTranspose = transpose + clip.getTranspose();
            float clipGain = gain * clip.getVolume() * song.getLane(clip.getLane()).getVolume();

            if (clip.getKind() == SongClipKind.SONG) {
                count = flattenSong(clip.getSongHandle().getSongData(), EngineSetting.SONG_LANE_ALL, clipLane, shift,
                        start, end, clipTranspose, clipGain, cursors, count);
                continue;
            }

            if (count == cursors.size())
                cursors.add(new SongCursorStruct());

            cursors.get(count++).begin(clip, clipLane, shift, start, end, clipTranspose, clipGain);
        }

        return count;
    }
}
