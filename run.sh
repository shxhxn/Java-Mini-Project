#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p build
javac --release 17 -encoding UTF-8 -d build src/attendance/*.java
java -cp build attendance.AttendanceApp
