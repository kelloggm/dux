#!/bin/bash

# this is a smoke test for dux.

set -eu

export GOOGLE_APPLICATION_CREDENTIALS=../../credentials/GOOGLE_APPLICATION_CREDENTIALS

../../bazel-bin/dux -c make

diff build.dux expected.dux
