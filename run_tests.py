"""Host JVM regression tests: python run_tests.py [output-directory]."""
import subprocess
import sys
from pathlib import Path


def main():
    root = Path(__file__).resolve().parent
    output = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else root / "test-out"
    output.mkdir(parents=True, exist_ok=True)

    sources = [
        root / "module/src/com/ltpoxcos/RateProfile.java",
        root / "module/src/com/ltpoxcos/AdaptiveState.java",
        root / "module/src/com/ltpoxcos/ImeLatch.java",
        root / "module/src/com/ltpoxcos/RefreshRateOverlay.java",
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
        ["java", "-cp", str(output), "com.ltpoxcos.PolicyTest"],
        check=True,
    )
    subprocess.run(
        ["java", "-cp", str(output), "com.ltpoxcos.RefreshRateOverlayTest"],
        check=True,
    )


if __name__ == "__main__":
    main()
