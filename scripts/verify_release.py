#!/usr/bin/env python3
"""Fail-closed validation of a signed TodUno release APK using Android SDK tools."""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path
import re
import shutil
import subprocess
import sys


def run(*args: str) -> str:
    try:
        return subprocess.check_output(
            args, text=True, encoding="utf-8", stderr=subprocess.STDOUT
        )
    except FileNotFoundError as exc:
        raise RuntimeError(f"Required Android SDK tool not found: {args[0]}") from exc
    except subprocess.CalledProcessError as exc:
        raise RuntimeError(
            f"Command failed ({exc.returncode}): {args[0]}: {exc.output}"
        ) from exc


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--expected-package", required=True)
    parser.add_argument("--expected-version", required=True)
    parser.add_argument("--min-version-code", required=True, type=int)
    parser.add_argument("--expected-cert", required=True)
    args = parser.parse_args()

    if not re.fullmatch(r"[0-9a-fA-F]{64}", args.expected_cert):
        parser.error("--expected-cert must be a 64-digit SHA-256 fingerprint")
    if not args.apk.is_file() or args.apk.stat().st_size == 0:
        parser.error("Signed APK is absent or empty")
    if args.min_version_code < 1:
        parser.error("--min-version-code must be positive")

    badging_tool = shutil.which("aapt2") or shutil.which("aapt")
    if not badging_tool:
        raise RuntimeError("Neither aapt2 nor aapt is available")
    badging = run(badging_tool, "dump", "badging", str(args.apk))
    package_line = next((line for line in badging.splitlines() if line.startswith("package:")), None)
    if not package_line:
        raise RuntimeError("Android package identity not found in APK manifest")
    fields = dict(re.findall(r"([a-zA-Z][a-zA-Z0-9]*)='([^']*)'", package_line))
    package_name = fields.get("name")
    version_name = fields.get("versionName")
    version_code = fields.get("versionCode")
    if package_name != args.expected_package:
        raise RuntimeError(f"Unexpected applicationId: {package_name}")
    if version_name != args.expected_version:
        raise RuntimeError(f"Unexpected versionName: {version_name}")
    if not version_code or int(version_code) < args.min_version_code:
        raise RuntimeError(f"versionCode is missing or too low: {version_code}")

    signing = run(
        "apksigner", "verify", "--verbose", "--print-certs",
        "--min-sdk-version", "24", str(args.apk)
    )
    signers = re.findall(
        r"^Signer #([0-9]+) certificate SHA-256 digest: ([0-9a-fA-F]{64})$",
        signing,
        flags=re.MULTILINE,
    )
    if len(signers) != 1 or signers[0][0] != "1":
        raise RuntimeError("Expected exactly one verified APK signer")
    actual_cert = signers[0][1].lower()
    if actual_cert != args.expected_cert.lower():
        raise RuntimeError(
            f"Signer certificate mismatch: {actual_cert}. "
            "Do not rotate the signing key without an approved migration plan."
        )
    if not re.search(r"^Verified using v[234] scheme .*: true$", signing, flags=re.MULTILINE):
        raise RuntimeError("No modern APK signing scheme was reported as verified")

    run("zipalign", "-c", "-P", "16", "-v", "4", str(args.apk))
    with args.apk.open("rb") as source:
        sha = hashlib.file_digest(source, "sha256").hexdigest()

    print(
        f"Verified: {package_name} versionCode={version_code} "
        f"versionName={version_name}"
    )
    print(f"Signer certificate SHA-256: {actual_cert}")
    print(f"APK SHA-256: {sha}")
    print("Signature and 16-KB ZIP alignment: OK")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, RuntimeError) as exc:
        sys.exit(f"Release validation failed: {exc}")
