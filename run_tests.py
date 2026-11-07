"""Host JVM regression tests: python run_tests.py [output-directory]."""
import subprocess
import sys
from pathlib import Path


def main():
    root = Path(__file__).resolve().parent
    output = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else root / "test-out"
    output.mkdir(parents=True, exist_ok=True)

    sources = [
        root / "module/src/com/refresh165/RateProfile.java",
        root / "module/src/com/refresh165/AdaptiveState.java",
        root / "module/src/com/refresh165/ImeLatch.java",
        root / "module/src/com/refresh165/RefreshRateOverlay.java",
        root / "tests/PolicyTest.java",
        root / "tests/RefreshRateOverlayTest.java",
    ]
    subprocess.run(
        [
            "javac",
            "--release",
            "11",
            "-encoding",
            "UTF-8",
            "-d",
            str(output),
            *(str(source) for source in sources),
        ],
        check=True,
    )
    subprocess.run(
        ["java", "-cp", str(output), "com.refresh165.PolicyTest"],
        check=True,
    )
    subprocess.run(
        ["java", "-cp", str(output), "com.refresh165.RefreshRateOverlayTest"],
        check=True,
    )


if __name__ == "__main__":
    main()
