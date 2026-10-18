#!/usr/bin/env python3
"""Simple WAV pitch estimate for CLEAN captured sine tones (no dependencies).

This is NOT a replacement for laboratory FFT/RMAA/Audio Precision analysis.
It counts rising zero-crossings in a central window; noisy mic captures or
non-sine music can confuse it. For 24-bit PCM, raw samples are sign-extended.

Usage: python analyze_clean_wav_pitch.py captured.wav --expected 440
"""

import argparse
import struct
import wave


def decode_samples(data, width, channels):
    step = width * channels
    frames = len(data) // step
    out = []
    for n in range(frames):
        i = n * step
        if width == 1:
            x = data[i] - 128
        elif width == 2:
            x = struct.unpack_from('<h', data, i)[0]
        elif width == 3:
            x = int.from_bytes(data[i:i + 3], 'little', signed=True)
        elif width == 4:
            x = struct.unpack_from('<i', data, i)[0]
        else:
            raise ValueError(f'Unsupported PCM width {width}')
        out.append(float(x))
    return out


def estimate(samples, sample_rate):
    if len(samples) < sample_rate // 5:
        raise ValueError('At least 0.2s of captured clean tone is needed')
    mean = sum(samples) / len(samples)
    centered = [x - mean for x in samples]
    amplitude = max(abs(x) for x in centered)
    if amplitude <= 0:
        raise ValueError('Silent recording')
    # Debounce zero crossings via a small hysteresis band.
    threshold = amplitude * 0.08
    armed = False
    crossing_times = []
    for i in range(1, len(centered)):
        prev, curr = centered[i - 1], centered[i]
        if prev < -threshold:
            armed = True
        if armed and curr >= threshold:
            # Accept the rising cycle boundary at the approximate zero location.
            crossing_times.append(i / sample_rate)
            armed = False
    if len(crossing_times) < 3:
        raise ValueError('Too few complete cycles')
    periods = len(crossing_times) - 1
    return periods / (crossing_times[-1] - crossing_times[0])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('wav')
    ap.add_argument('--expected', type=float, required=True)
    ap.add_argument('--tolerance-percent', type=float, default=0.5)
    args = ap.parse_args()
    with wave.open(args.wav, 'rb') as w:
        if w.getcomptype() != 'NONE':
            ap.error('Only uncompressed PCM WAV is supported')
        sample_rate, width, channels, total = w.getframerate(), w.getsampwidth(), w.getnchannels(), w.getnframes()
        # Omit first/last ~15% to avoid recorder startup/shutdown transients.
        start = int(total * .15)
        end = int(total * .85)
        w.setpos(start)
        sample_data = w.readframes(end - start)
    freq = estimate(decode_samples(sample_data, width, channels), sample_rate)
    error_pct = abs((freq / args.expected) - 1.0) * 100
    print(f'Estimated tone: {freq:.3f} Hz; expected: {args.expected:.3f} Hz')
    print(f'Absolute deviation: {error_pct:.4f}% (tolerance ±{args.tolerance_percent}%)')
    print('PASS' if error_pct <= args.tolerance_percent else 'FAIL')
    raise SystemExit(0 if error_pct <= args.tolerance_percent else 1)


if __name__ == '__main__':
    main()
