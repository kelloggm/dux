#!/bin/bash

# this is a smoke test for dux.

set -eu

cd shell

export GOOGLE_APPLICATION_CREDENTIALS=../../credentials/GOOGLE_APPLICATION_CREDENTIALS

../../bazel-bin/dux -c make

if ! diff build.dux expected.dux; then
    echo "build.dux and expected.dux differ!"
    echo "printing build.dux"
    ../../bazel-bin/dux -f build.dux -v debug > build.dux.readable
    echo "printing expected.dux"
    ../../bazel-bin/dux -f expected.dux -v debug > expected.dux.readable
    diff build.dux.readable expected.dux.readable
    exit 1
fi