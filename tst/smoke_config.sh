#!/bin/bash

# this is a smoke test for dux.

set -eu

../../bazel-bin/dux -c make

gsutil cp build.dux gs://duxserver-test0