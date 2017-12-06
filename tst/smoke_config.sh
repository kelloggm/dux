#!/bin/bash

# this is a smoke test for dux.

set -eu

echo -n "pwd:"
echo `pwd`

echo "running dux -c make"

../../bazel-bin/dux -c make

echo "uploading to server"

gsutil cp build.dux gs://duxserver-test0