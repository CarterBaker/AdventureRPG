"""Converts one SFZ instrument from the owner's library into the engine's music content.

Claude runs this, never the engine or the editor: it reads an SFZ file and the WAV samples it names from
.claude/assets/ and writes, under the project's assets/:

    sounds/notes/<Instrument>/<Note>_V<layer>_R<roundRobin>.ogg   each sample, mono 48 kHz Ogg Vorbis
    sounds/notes/<Instrument>/<...>.arpg                          each sample's companion
    notesets/<Instrument>.arpg                                    the note set
    instruments/<Instrument>.arpg                                 the default instrument, only when none exists

Every .arpg is drafted as text in a scratch folder and sealed by the ARPG tool (.claude/docs/ARPG.md), whose command
is passed with --arpg. ffmpeg runs with Delivery.bat's settings. See .claude/docs/MusicPipeline.md for the rules.

    python3 -I .claude/tools/SfzConvert.py <file.sfz> <Instrument> --arpg "<ARPG tool command>" --scratch <dir>
        [--display-name <name>] [--midi-program <0..127>] [--percussion]
"""

import argparse
import json
import math
import os
import re
import shlex
import shutil
import struct
import subprocess
import sys
from collections import Counter

# Music constants, mirroring EngineSetting's // Music groups
NOTE_NAMES = ["C", "Cs", "D", "Ds", "E", "F", "Fs", "G", "Gs", "A", "As", "B"]
PITCH_MIN, PITCH_MAX, VELOCITY_MIN, VELOCITY_MAX = 0, 127, 1, 127
NOTE_SET_MIN_VOLUME, NOTE_SET_MAX_VOLUME, NOTE_ENVELOPE_MAX_SECONDS = -96.0, 0.0, 100.0
DEFAULT_POLYPHONY, MIDI_PROGRAM_NONE, SAMPLE_RATE = 16, -1, 48000

# Conversion
HEADROOM_DECIBELS, MAX_GAIN_DECIBELS = 1.0, 60.0
SECONDS_PRECISION, DECIBELS_PRECISION = 10000, 1000
SOUND_NOTE_FOLDER = "sounds/notes"
FFMPEG_ARGUMENTS = ["-nostdin", "-v", "error", "-y"]
FFMPEG_STREAM = ["-map", "0:a:0", "-map_metadata", "-1"]
FFMPEG_VORBIS = ["-c:a", "libvorbis", "-q:a", "5", "-ar", str(SAMPLE_RATE)]

# SFZ
TOKEN = re.compile(r"<(\w+)>|([A-Za-z0-9_$]+)=")
BLOCK_COMMENT = re.compile(r"/\*.*?\*/", re.DOTALL)
PLAYED = {"sample", "key", "lokey", "hikey", "pitch_keycenter", "lovel", "hivel", "seq_length", "seq_position",
          "trigger", "lorand", "hirand",
          "tune", "transpose", "volume", "amp_veltrack", "loop_mode", "loop_start", "loop_end", "offset", "end",
          "ampeg_attack", "ampeg_hold", "ampeg_decay", "ampeg_sustain", "ampeg_release"}
APPROXIMATED = {"xfin_lovel", "xfin_hivel", "xfout_lovel", "xfout_hivel"}
RELEASE_TRIGGERS = {"release", "release_key"}
LOOP_MODES = {"no_loop": "none", "one_shot": "one_shot", "loop_sustain": "sustain", "loop_continuous": "continuous"}
ENVELOPE = [("attack", "ampeg_attack", 0.0, 1.0), ("hold", "ampeg_hold", 0.0, 1.0),
            ("decay", "ampeg_decay", 0.0, 1.0), ("sustain", "ampeg_sustain", 1.0, 100.0),
            ("release", "ampeg_release", 0.001, 1.0)]


class Refusal(Exception):
    pass


# SFZ

def parse_sfz(path, report):
    """Every region with what its global, master and group set beneath its own, and the sample folder."""
    text = BLOCK_COMMENT.sub(" ", open(path, encoding="latin-1").read())
    unsupported = Counter()
    control, scopes, regions, region, scope = {}, {"global": {}, "master": {}, "group": {}}, [], None, None
    for line in text.splitlines():
        line = line.split("//", 1)[0].strip()
        if not line:
            continue
        if line.startswith("#"):
            unsupported[line.split()[0]] += 1
            continue
        matches = list(TOKEN.finditer(line))
        for index, match in enumerate(matches):
            end = matches[index + 1].start() if index + 1 < len(matches) else len(line)
            header, opcode = match.group(1), match.group(2)
            if header:
                if region is not None:
                    regions.append({**scopes["global"], **scopes["master"], **scopes["group"], **region})
                region = None
                if header == "control":
                    scope = control
                elif header in scopes:
                    for name in list(scopes)[list(scopes).index(header):]:
                        scopes[name] = {}
                    scope = scopes[header]
                elif header == "region":
                    region = scope = {}
                else:
                    unsupported["<" + header + ">"] += 1
                    scope = None
                continue
            if opcode.startswith("sw_"):
                raise Refusal("This is a keyswitch patch (it uses %s): it switches articulations by key, which an "
                              "instrument cannot. Convert each articulation's own SFZ instead." % opcode)
            known = opcode == "default_path" if scope is control else opcode in PLAYED or opcode in APPROXIMATED
            if scope is None or not known:
                unsupported[opcode] += 1
            if scope is not None:
                scope[opcode] = line[match.end():end].strip()
    if region is not None:
        regions.append({**scopes["global"], **scopes["master"], **scopes["group"], **region})
    for name in sorted(unsupported):
        report.append("Left out: %s (written %d times)" % (name, unsupported[name]))
    if not regions:
        raise Refusal("This SFZ file has no regions.")
    folder = os.path.join(os.path.dirname(path), control.get("default_path", "").replace("\\", "/"))
    return regions, folder


def read_pitch(region, opcode, fallback):
    value = region.get(opcode)
    if value is None:
        return fallback
    try:
        return int(value)
    except ValueError:
        match = re.fullmatch(r"([a-gA-G])([#b]?)(-?\d+)", value)
        if not match:
            return fallback
        step = [0, 2, 4, 5, 7, 9, 11]["CDEFGAB".index(match.group(1).upper())]
        step += {"#": 1, "b": -1, "": 0}[match.group(2)]
        return (int(match.group(3)) + 1) * 12 + step


def read_number(region, opcode, fallback):
    try:
        return float(region[opcode]) if opcode in region else fallback
    except ValueError:
        return fallback


def most_common(regions, opcode, fallback, scale, report):
    """The value most regions give an opcode in the note set's units, each differing region counted in the report."""
    counts = Counter(read_number(region, opcode, fallback * scale) / scale for region in regions)
    value, count = counts.most_common(1)[0]
    if len(counts) > 1:
        report.append("Envelope: %s differs in %d regions; every note takes %g, the value most regions give"
                      % (opcode, len(regions) - count, value * scale))
    return value


# WAV

def read_wav(path):
    """Channels, rate, frames, the decoder for its samples and the first smpl loop (end exclusive)."""
    data = open(path, "rb").read()
    if data[:4] != b"RIFF" or data[8:12] != b"WAVE":
        raise Refusal("Not a WAV file: " + path)
    at, info = 12, {"loop": None}
    while at + 8 <= len(data):
        chunk, size = data[at:at + 4], struct.unpack_from("<I", data, at + 4)[0]
        body = at + 8
        if chunk == b"fmt ":
            tag, channels, rate = struct.unpack_from("<HHI", data, body)
            bits = struct.unpack_from("<H", data, body + 14)[0]
            if tag == 0xFFFE and size >= 40:
                tag = struct.unpack_from("<H", data, body + 24)[0]
            if tag not in (1, 3):
                raise Refusal("WAV stores its samples in a format the converter cannot read: " + path)
            info.update(channels=channels, rate=rate, bits=bits, floating=tag == 3)
        elif chunk == b"data":
            info.update(offset=body, size=min(size, len(data) - body))
        elif chunk == b"smpl" and info["loop"] is None and size >= 60 and struct.unpack_from("<I", data, body + 28)[0]:
            start, last = struct.unpack_from("<II", data, body + 36 + 8)
            info["loop"] = (start, last + 1)
        at = body + size + (size & 1)
    info["frames"] = info["size"] // (info["channels"] * info["bits"] // 8)
    if info["loop"] and not (0 <= info["loop"][0] < info["loop"][1] <= info["frames"]):
        info["loop"] = None
    info["data"] = data
    return info


def read_peak(info, start, end):
    """The loudest point, 0 to 1, of the frames from start to end mixed to one channel."""
    channels, width = info["channels"], info["bits"] // 8
    raw = info["data"][info["offset"] + start * channels * width:info["offset"] + end * channels * width]
    if info["floating"]:
        values, scale = struct.unpack("<%df" % (len(raw) // 4), raw), 1.0
    elif width == 2:
        values, scale = struct.unpack("<%dh" % (len(raw) // 2), raw), 32768.0
    elif width == 3:
        values = [int.from_bytes(raw[i:i + 3], "little", signed=True) for i in range(0, len(raw) - 2, 3)]
        scale = 8388608.0
    elif width == 4:
        values, scale = struct.unpack("<%di" % (len(raw) // 4), raw), 2147483648.0
    else:
        values, scale = [value - 128 for value in raw], 128.0
    peak = 0.0
    for frame in range(0, len(values) - channels + 1, channels):
        peak = max(peak, abs(sum(values[frame:frame + channels])) / channels)
    return min(peak / scale, 1.0)


# Conversion

def convert_sample(sample, release, ffmpeg):
    """The sample trimmed, mixed to mono, brought to full scale and encoded; its gain, length and loop come back."""
    info = read_wav(sample["source"])
    offset = min(sample["offset"], info["frames"] - 1)
    end = min(sample["end"], info["frames"]) if sample["end"] > offset else info["frames"]
    loop = sample["loop"] or info["loop"]
    has_loop = loop is not None and offset <= loop[0] < loop[1] <= end
    looped = has_loop and (sample["mode"] is None or sample["mode"] in ("sustain", "continuous"))
    kept_end = min(end, loop[1] + round(release * info["rate"])) if looped else end
    peak = read_peak(info, offset, kept_end)
    gain = max(-MAX_GAIN_DECIBELS, min(MAX_GAIN_DECIBELS, -20 * math.log10(peak) - HEADROOM_DECIBELS)) if peak else 0.0
    graph = "atrim=start_sample=%d:end_sample=%d" % (offset, kept_end)
    if info["channels"] > 1:
        graph += ",pan=mono|c0=" + "+".join("%.6f*c%d" % (1 / info["channels"], c) for c in range(info["channels"]))
    graph += ",volume=%.3fdB" % gain
    os.makedirs(os.path.dirname(sample["target"]), exist_ok=True)
    subprocess.run([ffmpeg] + FFMPEG_ARGUMENTS + ["-i", sample["source"]] + FFMPEG_STREAM + ["-af", graph]
                   + FFMPEG_VORBIS + [sample["target"]], check=True)
    decoded = subprocess.run([ffmpeg] + FFMPEG_ARGUMENTS + ["-i", sample["target"], "-f", "s16le", "-ac", "1", "-"],
                             check=True, capture_output=True).stdout
    length = len(decoded) / 2 / SAMPLE_RATE
    loop_start = (loop[0] - offset) / info["rate"] if looped else 0.0
    loop_end = min((loop[1] - offset) / info["rate"], length) if looped else 0.0
    sample.update(gain=gain, length=length, looped=looped and loop_start < loop_end,
                  loop_start=loop_start if looped else 0.0, loop_end=loop_end if looped else 0.0)


def floor_seconds(seconds):
    return math.floor(seconds * SECONDS_PRECISION) / SECONDS_PRECISION


def round_decibels(decibels):
    return round(decibels * DECIBELS_PRECISION) / DECIBELS_PRECISION


def plan(sfz_path, instrument, assets, report):
    """Every sample to convert and every region of the note set, with its envelope and velocity tracking."""
    regions, folder = parse_sfz(sfz_path, report)
    released = [region for region in regions if region.get("trigger", "attack") in RELEASE_TRIGGERS]
    if released:
        report.append("Left out: %d regions play only on release (trigger=release); a note set plays on attack"
                      % len(released))
        regions = [region for region in regions if region.get("trigger", "attack") not in RELEASE_TRIGGERS]
    roots, layers = [], {}
    for region in regions:
        key = read_pitch(region, "key", None)
        root = read_pitch(region, "pitch_keycenter", 60 if key is None else key) - int(read_number(region, "transpose", 0))
        low = max(VELOCITY_MIN, int(read_number(region, "lovel", 0)))
        high = min(VELOCITY_MAX, int(read_number(region, "hivel", VELOCITY_MAX)))
        if "xfin_lovel" in region or "xfin_hivel" in region:
            low = max(low, min(VELOCITY_MAX, (int(read_number(region, "xfin_lovel", low))
                                              + int(read_number(region, "xfin_hivel", low))) // 2 + 1))
        if "xfout_lovel" in region or "xfout_hivel" in region:
            high = min(high, max(VELOCITY_MIN, (int(read_number(region, "xfout_lovel", high))
                                                + int(read_number(region, "xfout_hivel", high))) // 2))
        roots.append((root, low, high, key))
        layers.setdefault(root, set()).add(low)
    crossfaded = sum(1 for region in regions if any(opcode in region for opcode in APPROXIMATED))
    if crossfaded:
        report.append("Approximated: %d regions crossfade by velocity; each now plays up to or from the middle of "
                      "its crossfade" % crossfaded)
    envelope = {name: min(NOTE_ENVELOPE_MAX_SECONDS, max(0.0, most_common(regions, opcode, fallback, scale, report)))
                for name, opcode, fallback, scale in ENVELOPE}
    tracking = min(1.0, max(0.0, most_common(regions, "amp_veltrack", 1.0, 100.0, report)))
    samples, planned, names = {}, [], set()
    for index, (region, (root, low, high, key)) in enumerate(zip(regions, roots)):
        if not region.get("sample"):
            raise Refusal("Region %d names no sample." % index)
        source = os.path.normpath(os.path.join(folder, region["sample"].replace("\\", "/")))
        if not os.path.isfile(source):
            raise Refusal("Region %d plays %s, which is not there." % (index, region["sample"]))
        mode = LOOP_MODES.get(region.get("loop_mode"))
        if region.get("loop_mode") and mode is None:
            report.append("Left out: region %d names loop_mode %s; its sample decides" % (index, region["loop_mode"]))
        tune = read_number(region, "tune", 0.0)
        steps = round(tune / 100)
        count = max(1, int(read_number(region, "seq_length", 1)))
        turn = min(count - 1, max(0, int(read_number(region, "seq_position", 1)) - 1))
        if count == 1 and ("lorand" in region or "hirand" in region):
            low_rand, high_rand = read_number(region, "lorand", 0.0), read_number(region, "hirand", 1.0)
            count = max(1, round(1.0 / max(high_rand - low_rand, 1e-6)))
            turn = min(count - 1, int(round(low_rand * count)))
        loop = (int(read_number(region, "loop_start", -1)), int(read_number(region, "loop_end", -1)) + 1)
        loop = loop if 0 <= loop[0] < loop[1] - 1 else None
        offset, end = int(read_number(region, "offset", 0)), int(read_number(region, "end", 0))
        identity = (source, offset, end, loop, mode)
        if identity not in samples:
            layer = sorted(layers[root]).index(low) + 1
            base = "%s/%s/%s%d_V%d_R%d" % (SOUND_NOTE_FOLDER, instrument, NOTE_NAMES[root % 12], root // 12 - 1,
                                           layer, turn + 1)
            name, copy = base, 2
            while name in names:
                name, copy = "%s_%d" % (base, copy), copy + 1
            names.add(name)
            samples[identity] = {"source": source, "name": name[len("sounds/"):], "mode": mode, "loop": loop,
                                 "target": os.path.join(assets, name + ".ogg"), "offset": max(0, offset),
                                 "end": end + 1 if end > 0 else -1}
        planned.append({"sample": samples[identity], "root": root - steps,
                        "low": read_pitch(region, "lokey", PITCH_MIN if key is None else key),
                        "high": read_pitch(region, "hikey", PITCH_MAX if key is None else key),
                        "velocity_low": low, "velocity_high": high, "round_robin": turn, "round_robin_count": count,
                        "tune": tune - steps * 100, "volume": read_number(region, "volume", 0.0)})
    return list(samples.values()), planned, envelope, tracking


def build_note_set(planned, envelope, tracking, report):
    regions, held = [], 0
    for region in sorted(planned, key=lambda r: (r["low"], r["velocity_low"], r["round_robin"])):
        sample = region["sample"]
        volume = region["volume"] - sample["gain"]
        held += volume > NOTE_SET_MAX_VOLUME
        mode = sample["mode"] if sample["mode"] and (sample["looped"] or sample["mode"] in ("none", "one_shot")) \
            else ("continuous" if sample["looped"] else "none")
        regions.append({"sound": sample["name"], "root": region["root"], "low": region["low"], "high": region["high"],
                        "velocity_low": region["velocity_low"], "velocity_high": region["velocity_high"],
                        "round_robin": region["round_robin"], "round_robin_count": region["round_robin_count"],
                        "tune": round_decibels(region["tune"]),
                        "volume": round_decibels(min(NOTE_SET_MAX_VOLUME, max(NOTE_SET_MIN_VOLUME, volume))),
                        "loop_mode": mode})
    if held:
        report.append("Volume: %d regions ask to play louder than their sample at full scale and are held there" % held)
    return {"regions": regions, "envelope": {name: round(value, 4) for name, value in envelope.items()},
            "velocity_tracking": round(tracking, 4)}


def build_instrument(planned, instrument, display_name, midi_program, percussion, report):
    """The default instrument, playing the widest run of pitches every one of which some region covers."""
    covered = [False] * (PITCH_MAX + 1)
    for region in planned:
        for pitch in range(max(PITCH_MIN, region["low"]), min(PITCH_MAX, region["high"]) + 1):
            covered[pitch] = True
    runs, start = [], None
    for pitch, has in enumerate(covered + [False]):
        if has and start is None:
            start = pitch
        elif not has and start is not None:
            runs.append((start, pitch - 1))
            start = None
    low, high = max(runs, key=lambda run: run[1] - run[0])
    if len(runs) > 1:
        report.append("Range: the regions leave gaps, so the instrument plays %d to %d, the widest run" % (low, high))
    return {"display_name": display_name, "note_set": instrument, "low": low, "high": high,
            "polyphony": DEFAULT_POLYPHONY, "volume": 1.0, "midi_program": midi_program, "percussion": percussion,
            "rigs": {}}


def seal(drafts, scratch, assets, arpg):
    """Every draft written as text to the scratch folder, sealed by the ARPG tool and moved into assets."""
    shutil.rmtree(scratch, ignore_errors=True)
    for relative, content in drafts.items():
        path = os.path.join(scratch, relative + ".json")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as file:
            json.dump(content, file, indent=2)
    subprocess.run(shlex.split(arpg) + ["convert", scratch], check=True, stdout=subprocess.DEVNULL)
    for relative in drafts:
        target = os.path.join(assets, relative + ".arpg")
        os.makedirs(os.path.dirname(target), exist_ok=True)
        shutil.move(os.path.join(scratch, relative + ".arpg"), target)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("sfz")
    parser.add_argument("instrument")
    parser.add_argument("--arpg", required=True)
    parser.add_argument("--scratch", required=True)
    parser.add_argument("--assets", default="assets")
    parser.add_argument("--display-name")
    parser.add_argument("--midi-program", type=int, default=MIDI_PROGRAM_NONE)
    parser.add_argument("--percussion", action="store_true")
    parser.add_argument("--ffmpeg", default="ffmpeg")
    arguments = parser.parse_args()
    report = []
    instrument = arguments.instrument
    try:
        samples, planned, envelope, tracking = plan(arguments.sfz, instrument, arguments.assets, report)
        for sample in samples:
            convert_sample(sample, envelope["release"], arguments.ffmpeg)
    except Refusal as refusal:
        print("Refused: %s" % refusal)
        return 1
    drafts = {SOUND_NOTE_FOLDER + "/" + instrument + "/" + os.path.basename(s["name"]):
              {"category": "music", "mono": True, "spatial": True, "length": floor_seconds(s["length"]),
               "loopStart": floor_seconds(s["loop_start"]), "loopEnd": floor_seconds(s["loop_end"])}
              for s in samples}
    drafts["notesets/" + instrument] = build_note_set(planned, envelope, tracking, report)
    if not os.path.isfile(os.path.join(arguments.assets, "instruments", instrument + ".arpg")):
        display_name = arguments.display_name or os.path.splitext(os.path.basename(arguments.sfz))[0]
        drafts["instruments/" + instrument] = build_instrument(planned, instrument, display_name,
                                                               arguments.midi_program, arguments.percussion, report)
    else:
        report.append("Instrument: %s already has an instrument file, kept as it was" % instrument)
    seal(drafts, arguments.scratch, arguments.assets, arguments.arpg)
    looped = sum(1 for sample in samples if sample["looped"])
    decoded = sum(sample["length"] for sample in samples) * SAMPLE_RATE * 2
    report.append("Sustain loops: %d of %d samples loop" % (looped, len(samples)))
    report.append("Converted %d regions from %d samples, %.1f MB once decoded (%d bytes)"
                  % (len(planned), len(samples), decoded / 1048576, decoded))
    print("\n".join(report))
    return 0


if __name__ == "__main__":
    sys.exit(main())
