#!/usr/bin/env python3
"""Deterministic, offline pitch QA fixtures (standard library only).

Generates stereo PCM WAV tones at known frequencies and sample rates. Place the
files in Music/ then use LuxNote's audio path to play them. Record a *clean*
loopback if possible and compare output frequency against the manifest.

Usage: python generate_reference_tones.py --out ./reference_tones --duration 5
"""

import argparse
import csv
import math
import struct
import wave
from pathlib import Path


def save_tone(path: Path, frequency: float, sample_rate: int, bit_depth: int, duration: float):
    nframes = int(round(sample_rate * duration))
    fade_frames = max(1, int(sample_rate * 0.015))
    path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(path), 'wb') as wav:
        wav.setnchannels(2)
        wav.setsampwidth(bit_depth // 8)
        wav.setframerate(sample_rate)
        block = bytearray()
        for i in range(nframes):
            gain = min(1.0, i / fade_frames, (nframes - 1 - i) / fade_frames)
            sample = 0.22 * gain * math.sin(2.0 * math.pi * frequency * i / sample_rate)
            if bit_depth == 16:
                integer = max(-32768, min(32767, round(sample * 32767)))
                bytes_s = struct.pack('<h', integer)
            elif bit_depth == 24:
                integer = max(-8388608, min(8388607, round(sample * 8388607)))
                bytes_s = integer.to_bytes(3, 'little', signed=True)
            else:
                raise ValueError('Only 16 and 24 bit supported')
            block.extend(bytes_s)
            block.extend(bytes_s)
            if len(block) >= 65536:
                wav.writeframesraw(block)
                block.clear()
        if block:
            wav.writeframesraw(block)
        wav.writeframes(b'')
    return nframes


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--out', type=Path, default=Path('reference_tones'))
    parser.add_argument('--duration', type=float, default=5.0)
    args = parser.parse_args()
    if not 0.5 <= args.duration <= 60.0:
        parser.error('--duration must be between 0.5 and 60 seconds')
    args.out.mkdir(parents=True, exist_ok=True)
    rows = []
    for rate in (44100, 48000, 96000):
        for bit_depth in (16, 24):
            for hz in (440, 1000):
                file = f'tone_{hz}Hz_{rate}Hz_{bit_depth}bit_stereo.wav'
                count = save_tone(args.out / file, hz, rate, bit_depth, args.duration)
                rows.append((file, hz, rate, bit_depth, 2, count / rate))
    with (args.out / 'reference_manifest.csv').open('w', newline='', encoding='utf-8') as fp:
        writer = csv.writer(fp)
        writer.writerow(['filename', 'tone_hz', 'sample_rate_hz', 'bit_depth', 'channels', 'duration_s'])
        writer.writerows(rows)
    print(f'Created {len(rows)} reference WAVs and manifest in {args.out.resolve()}')


if __name__ == '__main__':
    main()
