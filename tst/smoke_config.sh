#!/bin/bash

# this is a smoke test for dux.

set -eu

echo "running dux -c make"

ls

dux -c make

echo "uploading to server"

gsutil cp build.dux gs://duxserver-test0