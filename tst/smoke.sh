#!/bin/bash

# this is a smoke test for dux.

set -eu

../../bazel-bin/dux -c make

diff build.dux expected.dux
