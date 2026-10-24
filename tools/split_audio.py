"""
Split whole-poem readings into 上の句 and 下の句 clips for Karutasuke.

Each input file is one poem, read the way it is in a match:
    上の句, pause, 下の句, pause, 下の句 again
The script cuts at the first pause. By default it keeps one reading of the 下の句
(use --keep-repeat to keep both):

    a001.mp3  ->  app/src/main/assets/audio/kaminoku/001.mp3
              ->  app/src/main/assets/audio/shimonoku/001.mp3

Clips are cut without re-encoding, so there is no quality loss.

Setup (once):
    pip install imageio-ffmpeg

Usage (from the project folder):
    python tools/split_audio.py "C:/path/to/karuta_otona"
    python tools/split_audio.py "C:/path/to/karuta_otona" --dry-run   # report only, write nothing
"""

import argparse
import re
import subprocess
import sys
from pathlib import Path

import imageio_ffmpeg

FFMPEG = imageio_ffmpeg.get_ffmpeg_exe()
NAME_PATTERN = re.compile(r"^[a-z]?(\d{1,3})\.mp3$", re.IGNORECASE)   # a001.mp3, b001.mp3, 001.mp3
DEFAULT_OUT = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "assets" / "audio"


def find_pauses(path, noise_db, min_silence):
    """Returns (pauses, file_duration): the (start, end) of every pause between spoken parts, in seconds."""
    result = subprocess.run(
        [FFMPEG, "-hide_banner", "-nostats", "-i", str(path),
         "-af", f"silencedetect=noise={noise_db}dB:d={min_silence}", "-f", "null", "-"],
        capture_output=True, text=True, encoding="utf-8", errors="replace")
    log = result.stderr

    match = re.search(r"Duration: (\d+):(\d+):([\d.]+)", log)
    if not match:
        raise RuntimeError(f"ffmpeg could not read {path.name}")
    h, m, s = match.groups()
    duration = int(h) * 3600 + int(m) * 60 + float(s)

    starts = [float(x) for x in re.findall(r"silence_start: (-?[\d.]+)", log)]
    ends = [float(x) for x in re.findall(r"silence_end: ([\d.]+)", log)]

    # Ignore silence at the very start or end of the file; only pauses between spoken parts count.
    pauses = [(st, en) for st, en in zip(starts, ends) if st > 0.5 and en < duration - 0.5]
    return pauses, duration


def cut(src, dest, start, end):
    dest.parent.mkdir(parents=True, exist_ok=True)
    cmd = [FFMPEG, "-hide_banner", "-loglevel", "error", "-y", "-i", str(src), "-ss", f"{start:.3f}"]
    if end is not None:
        cmd += ["-to", f"{end:.3f}"]
    cmd += ["-map", "0:a", "-c:a", "copy", str(dest)]
    subprocess.run(cmd, check=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("input", type=Path, help="folder with the whole-poem mp3 files")
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT, help="output folder (default: app assets/audio)")
    parser.add_argument("--noise", type=float, default=-40, help="silence threshold in dB (default -40)")
    parser.add_argument("--min-silence", type=float, default=1.0, help="shortest pause to count, seconds (default 1.0)")
    parser.add_argument("--tail", type=float, default=0.3, help="silence kept after the 上の句, seconds (default 0.3)")
    parser.add_argument("--lead", type=float, default=0.2, help="silence kept before the 下の句, seconds (default 0.2)")
    parser.add_argument("--keep-repeat", action="store_true", help="keep both readings of the 下の句")
    parser.add_argument("--dry-run", action="store_true", help="only print where each file would be cut")
    args = parser.parse_args()

    files = []
    for f in sorted(args.input.glob("*.mp3")):
        m = NAME_PATTERN.match(f.name)
        if m and 1 <= int(m.group(1)) <= 100:
            files.append((int(m.group(1)), f))
    if not files:
        sys.exit(f"No files like a001.mp3 found in {args.input}")

    print(f"{'No.':>4}  {'file':<10} {'pauses':>6} {'上の句':>7} {'下の句':>7}")
    problems = []
    for number, f in files:
        pauses, duration = find_pauses(f, args.noise, args.min_silence)
        if not pauses:
            print(f"{number:>4}  {f.name:<10}  NO PAUSE FOUND - not split")
            problems.append(f.name)
            continue

        first_start, first_end = pauses[0]
        kami_end = min(first_start + args.tail, first_end)
        shimo_start = max(first_end - args.lead, kami_end)
        shimo_end = None                                   # None = to the end of the file
        if len(pauses) >= 2 and not args.keep_repeat:
            shimo_end = min(pauses[1][0] + args.tail, pauses[1][1])

        flag = ""
        if len(pauses) != 2:
            flag = f"  <- check: expected 2 pauses (上の句 | 下の句 | 下の句), found {len(pauses)}"
        elif not all(1.5 <= en - st <= 5.0 for st, en in pauses):
            flag = "  <- check: unusual pause length"
        if flag:
            problems.append(f.name)
        shimo_length = (shimo_end if shimo_end is not None else duration) - shimo_start
        print(f"{number:>4}  {f.name:<10} {len(pauses):>6} {kami_end:>6.2f}s {shimo_length:>6.2f}s{flag}")

        if not args.dry_run:
            name = f"{number:03d}.mp3"
            cut(f, args.out / "kaminoku" / name, 0.0, kami_end)
            cut(f, args.out / "shimonoku" / name, shimo_start, shimo_end)

    print()
    print(f"{len(files) - len(problems)} of {len(files)} files look normal.")
    if problems:
        print("Listen to these and adjust by hand if needed: " + ", ".join(problems))
    if not args.dry_run:
        print(f"Clips written to {args.out}")


if __name__ == "__main__":
    main()
