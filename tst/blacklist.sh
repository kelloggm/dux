#!/bin/bash

# this tests that .duxignore will ignore files and directories

set -eu

cd shell

export GOOGLE_APPLICATION_CREDENTIALS=../../credentials/GOOGLE_APPLICATION_CREDENTIALS

# no duxignore: make will be a dependency (perhaps a bug, but easy to test)
../../bazel-bin/dux -c make

if [ `../../bazel-bin/dux -d | grep "make" | wc -l` -eq 0 ]; then
    echo "Missing make from dependencies"
    exit 1
fi

# make is now in duxignore, should not appear in dependencies
rm -f build.dux
which make > .duxignore
../../bazel-bin/dux -c make

if [ `../../bazel-bin/dux -d | grep "make" | wc -l` -ne 0 ]; then
    echo "make should no longer appear in dependencies"
    exit 1
fi

# now let's put the parent *folder* of make in .duxignore
# it should still be ignored
rm -f .duxignore
rm -f build.dux

echo "$(which make)/.." > .duxignore
../../bazel-bin/dux -c make

if [ `../../bazel-bin/dux -d | grep "make" | wc -l` -ne 0 ]; then
    echo "make should still not appear in dependencies"
    exit 1
fi

rm -f .duxignore
rm -f build.dux
