#!/bin/bash

# this is a smoke test for dux.

set -eu

cd shell

export GOOGLE_APPLICATION_CREDENTIALS=../../credentials/GOOGLE_APPLICATION_CREDENTIALS

../../bazel-bin/dux -c make

diff build.dux expected.dux

echo "printing build.dux"

../../bazel-bin/dux -f build.dux

echo "printing expected.dux"

../../bazel-bin/dux -f expected.dux